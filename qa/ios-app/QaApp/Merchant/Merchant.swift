import Foundation
import RadarpaymentsSDKPayment

/// The session the merchant calls go through: no timeout of its own shorter than the merchant's, and nothing kept from
/// one call to the next. A merchant's server sends no cookie back, so a cookie from `register.do` must not reach the status
/// call or the next run.
nonisolated func makeMerchantSession() -> URLSession {
    URLSession(configuration: merchantSessionConfiguration())
}

nonisolated func merchantSessionConfiguration() -> URLSessionConfiguration {
    let configuration = URLSessionConfiguration.ephemeral
    configuration.timeoutIntervalForRequest = idleTimeoutCeiling
    configuration.timeoutIntervalForResource = idleTimeoutCeiling
    configuration.httpCookieStorage = nil
    configuration.httpShouldSetCookies = false
    configuration.httpCookieAcceptPolicy = .never
    configuration.urlCache = nil
    configuration.urlCredentialStorage = nil
    return configuration
}

// The merchant times the whole exchange itself; the stack's own timeouts only stand behind it.
private nonisolated let idleTimeoutCeiling: TimeInterval = 2 * maxTimeout
// Task.sleep takes whole nanoseconds in a UInt64; a day keeps any timeout far inside it.
private nonisolated let maxTimeout: TimeInterval = 24 * 60 * 60

/// The merchant's backend, played from the device: registers the order the SDK then pays, and asks its status
/// afterwards, with the stand profile's credentials. Every exchange is journaled under `merchant`, the password and the
/// API key masked. Throws only `CancellationError`, when the caller is cancelled: for a gateway or network problem the
/// result says what went wrong.
nonisolated struct Merchant {
    private static let tag = "merchant"
    private static let messageLimit = 300

    private let session: URLSession
    private let journal: Journal
    private let timeout: TimeInterval
    private let orderNumber: () -> String

    init(
        session: URLSession,
        journal: Journal,
        timeout: TimeInterval = 20,
        orderNumber: @escaping () -> String = newOrderNumber
    ) {
        self.session = session
        self.journal = journal
        self.timeout = timeout > 0 ? min(timeout, maxTimeout) : 0
        self.orderNumber = orderNumber
    }

    func register(order: OrderConfig, profile: StandProfile) async throws -> Registration {
        let secrets = GatewaySecrets(profile: profile)
        let failed = { (message: String) in Registration.failed(httpStatus: nil, message: message) }
        let request: GatewayRequest
        if profile.api == .v2, order.type == .aft {
            return failed("An AFT order is registered with registerP2P.do of API v1, and \(profile.name) is an API v2 profile")
        } else if profile.api == .v2 {
            guard let url = sessionUrl(profile) else { return failed(notAUrl(profile, secrets)) }
            request = .json(url: url, body: sessionJson(order), apiKey: profile.apiKey)
        } else if let currency = Currencies.numeric(order.currency) {
            request =
                order.type == .aft
                    ? .json(url: transferUrl(profile), body: transferJson(order, profile, orderNumber: orderNumber(), numericCurrency: currency))
                    : .form(url: registerUrl(profile), parameters: registerForm(order, profile, orderNumber: orderNumber(), numericCurrency: currency))
        } else {
            return failed("Unknown currency \(order.currency)")
        }
        let answer = profile.api == .v2 ? sessionAnswer : registerAnswer
        return try await exchange(request, profile, secrets, failed) { answer($0, $1, secrets) }
    }

    /// Asks API v1 for the order's status; `notQueried` for API v2, without credentials or without [orderId].
    func status(orderId: String?, profile: StandProfile) async throws -> OrderStatus {
        guard profile.api == .v1, !isBlank(profile.login), !isBlank(profile.password), let orderId, !isBlank(orderId)
        else { return .notQueried }
        let secrets = GatewaySecrets(profile: profile)
        let request = GatewayRequest.form(url: statusUrl(profile), parameters: statusForm(orderId, profile))
        return try await exchange(request, profile, secrets, { .failed(httpStatus: nil, message: $0) }) {
            statusAnswer($0, $1, secrets)
        }
    }

    private func exchange<T>(
        _ request: GatewayRequest,
        _ profile: StandProfile,
        _ secrets: GatewaySecrets,
        _ failed: (String) -> T,
        _ answer: (Int, String) -> T
    ) async throws -> T {
        journal.record(.info, tag: Self.tag, message: request.printed(secrets))
        switch try await call(request, profile, secrets) {
        case let .reply(status, body):
            let level: LogLevel = (200 ..< 300).contains(status) ? .info : .warning
            let line = secrets.bounded("← \(status) \(body)", limit: GatewayRequest.journalLimit, cutMarker: GatewayRequest.cutMarker)
            journal.record(level, tag: Self.tag, message: line)
            return answer(status, body)
        case let .noAnswer(message):
            journal.record(.warning, tag: Self.tag, message: "← \(message)")
            return failed(message)
        }
    }

    // The body is read inside the timeout too, since a gateway may send the headers and then stall.
    private func call(_ request: GatewayRequest, _ profile: StandProfile, _ secrets: GatewaySecrets) async throws -> Outcome {
        guard let urlRequest = urlRequest(request) else { return .noAnswer(notAUrl(profile, secrets)) }
        let session = session
        let timeout = timeout
        do {
            return try await withThrowingTaskGroup(of: Outcome?.self) { group in
                group.addTask {
                    let (data, response) = try await session.data(for: urlRequest, delegate: NoRedirect())
                    guard let http = response as? HTTPURLResponse else { throw URLError(.badServerResponse) }
                    return .reply(status: http.statusCode, body: text(of: data, http))
                }
                group.addTask {
                    try await Task.sleep(nanoseconds: UInt64(timeout * 1_000_000_000))
                    return nil
                }
                // Leaving the group cancels and awaits the loser, whose error (URLError.cancelled) is dropped.
                defer { group.cancelAll() }
                return try await group.next()! ?? .noAnswer("The gateway did not answer in \(Int(timeout)) s")
            }
        } catch {
            // URLSession ends a cancelled call with URLError.cancelled; when the caller is cancelled, that is
            // cancellation, not a network failure.
            if Task.isCancelled { throw CancellationError() }
            return .noAnswer(noAnswerText(error, secrets))
        }
    }

    private func urlRequest(_ request: GatewayRequest) -> URLRequest? {
        guard let url = URL(string: request.url) else { return nil }
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.timeoutInterval = idleTimeoutCeiling
        switch request {
        case let .form(_, parameters):
            urlRequest.setValue("application/x-www-form-urlencoded; charset=UTF-8", forHTTPHeaderField: "Content-Type")
            urlRequest.httpBody = formBody(parameters)
        case let .json(_, body, apiKey):
            urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
            if let apiKey { urlRequest.setValue(apiKey, forHTTPHeaderField: GatewayRequest.apiKeyHeader) }
            urlRequest.httpBody = Data(JSONValue.object(body).text.utf8)
        }
        return urlRequest
    }

    // A URLError's description is a sentence about its code; the failing URL, which may carry credentials, stays in
    // its userInfo and out of the text.
    private func noAnswerText(_ error: Error, _ secrets: GatewaySecrets) -> String {
        let name = error is URLError ? "URLError" : String(describing: type(of: error))
        return secrets.bounded("\(name): \(error.localizedDescription)", limit: Self.messageLimit)
    }

    private func notAUrl(_ profile: StandProfile, _ secrets: GatewaySecrets) -> String {
        secrets.bounded("The gateway URL of \(profile.name) is not a URL: \(profile.gatewayUrl)", limit: Self.messageLimit)
    }

    // The body may quote the credentials back: a print of the outcome shows only its status.
    private enum Outcome: CustomStringConvertible, CustomReflectable {
        case reply(status: Int, body: String)
        case noAnswer(String)

        var description: String {
            switch self {
            case let .reply(status, _): "Reply(\(status))"
            case .noAnswer: "NoAnswer"
            }
        }

        var customMirror: Mirror {
            Mirror(self, children: [], displayStyle: .enum)
        }
    }
}

// The charset the gateway names, UTF-8 otherwise, as Ktor's bodyAsText reads it.
private nonisolated func text(of data: Data, _ response: HTTPURLResponse) -> String {
    if let name = response.textEncodingName {
        let encoding = CFStringConvertIANACharSetNameToEncoding(name as CFString)
        if encoding != kCFStringEncodingInvalidId,
           let text = String(data: data, encoding: String.Encoding(rawValue: CFStringConvertEncodingToNSStringEncoding(encoding)))
        {
            return text
        }
    }
    return String(decoding: data, as: UTF8.self)
}

// A 307 or 308 would post the password again and a 301/302/303 would carry the API key to the `Location` host, so the
// redirect is read as the answer, like any other that is not 2xx. On the task rather than the session: the caller may
// pass any session.
private nonisolated final class NoRedirect: NSObject, URLSessionTaskDelegate, Sendable {
    func urlSession(
        _: URLSession,
        task _: URLSessionTask,
        willPerformHTTPRedirection _: HTTPURLResponse,
        newRequest _: URLRequest,
        completionHandler: @escaping (URLRequest?) -> Void
    ) {
        completionHandler(nil)
    }
}
