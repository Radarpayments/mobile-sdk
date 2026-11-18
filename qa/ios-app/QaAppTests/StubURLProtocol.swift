import Foundation
@testable import QaApp
import XCTest

/// What the stub gateway does with a request.
enum StubReply {
    case answer(status: Int, body: String)
    case fail(Error)
    /// Never answers; the request ends only when it is cancelled.
    case hang
    /// Sends the status line and then nothing.
    case headOnly(status: Int)
    /// Answers with these headers, which may set a cookie.
    case answerWithHeaders(status: Int, headers: [String: String], body: String)
    /// A 30x to [location]: the URL loading system is told of the redirect as a real HTTP stack tells it, and the redirect
    /// response is delivered as the answer for a client that refuses to follow.
    case redirect(status: Int, location: String, body: String)
    /// Reacts only after [seconds], unless the request was cancelled before.
    indirect case after(seconds: TimeInterval, StubReply)
    /// Reacts only once [gate] is signalled, unless the request was cancelled before.
    indirect case gated(DispatchSemaphore, StubReply)
}

/// A gateway inside the test process: records every request and answers as `reply` says.
final class StubURLProtocol: URLProtocol {
    private static let lock = NSLock()
    private static var replyToAll: (URLRequest) -> StubReply = { _ in .answer(status: 200, body: #"{"orderId":"abc"}"#) }
    private static var recorded: [RecordedRequest] = []
    private static var started: [XCTestExpectation] = []

    private var stopped = false
    private let stopLock = NSLock()

    static func reset(_ reply: @escaping (URLRequest) -> StubReply = { _ in .answer(status: 200, body: #"{"orderId":"abc"}"#) }) {
        lock.withLock {
            replyToAll = reply
            recorded = []
            started = []
        }
    }

    static func notifyOnStart(_ expectation: XCTestExpectation) {
        lock.withLock { started.append(expectation) }
    }

    static var requests: [RecordedRequest] {
        lock.withLock { recorded }
    }

    static func session() -> URLSession {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubURLProtocol.self]
        return URLSession(configuration: configuration)
    }

    override class func canInit(with request: URLRequest) -> Bool {
        true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        let (reply, waiting) = Self.lock.withLock { () -> (StubReply, [XCTestExpectation]) in
            Self.recorded.append(RecordedRequest(request))
            let waiting = Self.started
            Self.started = []
            return (Self.replyToAll(request), waiting)
        }
        waiting.forEach { $0.fulfill() }
        perform(reply)
    }

    override func stopLoading() {
        stopLock.withLock { stopped = true }
    }

    private func perform(_ reply: StubReply) {
        guard !stopLock.withLock({ stopped }) else { return }
        switch reply {
        case let .answer(status, body):
            let response = HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: "HTTP/1.1", headerFields: nil)!
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: Data(body.utf8))
            client?.urlProtocolDidFinishLoading(self)
        case let .fail(error):
            client?.urlProtocol(self, didFailWithError: error)
        case .hang:
            break
        case let .answerWithHeaders(status, headers, body):
            let response = HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: "HTTP/1.1", headerFields: headers)!
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: Data(body.utf8))
            client?.urlProtocolDidFinishLoading(self)
        case let .redirect(status, location, body):
            let response = HTTPURLResponse(
                url: request.url!,
                statusCode: status,
                httpVersion: "HTTP/1.1",
                headerFields: ["Location": location]
            )!
            var next = request
            next.url = URL(string: location)
            if status == 301 || status == 302 || status == 303 {
                next.httpMethod = "GET"
                next.httpBody = nil
            }
            client?.urlProtocol(self, wasRedirectedTo: next, redirectResponse: response)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: Data(body.utf8))
            client?.urlProtocolDidFinishLoading(self)
        case let .headOnly(status):
            let response = HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: "HTTP/1.1", headerFields: nil)!
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: Data("{".utf8))
        case let .after(seconds, next):
            DispatchQueue.global().asyncAfter(deadline: .now() + seconds) { self.perform(next) }
        case let .gated(gate, next):
            DispatchQueue.global().async {
                gate.wait()
                self.perform(next)
            }
        }
    }
}

/// A request as the gateway received it.
struct RecordedRequest {
    let method: String?
    let url: String?
    let headers: [String: String]
    let body: Data
    let timeoutInterval: TimeInterval

    init(_ request: URLRequest) {
        method = request.httpMethod
        url = request.url?.absoluteString
        headers = request.allHTTPHeaderFields ?? [:]
        body = request.httpBody ?? Self.read(request.httpBodyStream)
        timeoutInterval = request.timeoutInterval
    }

    var bodyText: String {
        String(decoding: body, as: UTF8.self)
    }

    var json: JSONValue? {
        JSONValue.parse(bodyText)
    }

    var jsonObject: JSONObject? {
        if case let .object(object)? = json { object } else { nil }
    }

    /// The form's names in the order they were sent.
    var formNames: [String] {
        formPairs.map(\.0)
    }

    var form: [String: String] {
        Dictionary(formPairs, uniquingKeysWith: { _, last in last })
    }

    private var formPairs: [(String, String)] {
        bodyText.split(separator: "&", omittingEmptySubsequences: true).map { pair in
            let parts = pair.split(separator: "=", maxSplits: 1, omittingEmptySubsequences: false)
            return (decode(parts[0]), parts.count > 1 ? decode(parts[1]) : "")
        }
    }

    private func decode(_ text: Substring) -> String {
        text.replacingOccurrences(of: "+", with: " ").removingPercentEncoding ?? String(text)
    }

    private static func read(_ stream: InputStream?) -> Data {
        guard let stream else { return Data() }
        stream.open()
        defer { stream.close() }
        var data = Data()
        var buffer = [UInt8](repeating: 0, count: 4096)
        while stream.hasBytesAvailable {
            let count = stream.read(&buffer, maxLength: buffer.count)
            if count <= 0 { break }
            data.append(buffer, count: count)
        }
        return data
    }
}

/// The profiles and the merchant the merchant tests share; the gateway answers `{"orderId":"abc"}` unless told otherwise.
class MerchantTestCase: XCTestCase {
    let v1 = StandProfile(name: "v1", gatewayUrl: "https://dev.bpcbt.com/payment", login: "login-1", password: "password-1")
    let v2 = StandProfile(name: "v2", gatewayUrl: "https://dev.bpcbt.com/payment", api: .v2, apiKey: "key-1")
    var journal = Journal()

    override func setUp() {
        super.setUp()
        StubURLProtocol.reset()
        journal = Journal()
    }

    func merchant(timeout: TimeInterval = 20) -> Merchant {
        Merchant(session: StubURLProtocol.session(), journal: journal, timeout: timeout, orderNumber: { "qa-1" })
    }

    func answer(_ status: Int, _ body: String) {
        StubURLProtocol.reset { _ in .answer(status: status, body: body) }
    }

    func answer(_ body: String) {
        answer(200, body)
    }

    func single(file: StaticString = #filePath, line: UInt = #line) throws -> RecordedRequest {
        let requests = StubURLProtocol.requests
        XCTAssertEqual(requests.count, 1, file: file, line: line)
        return try XCTUnwrap(requests.first, file: file, line: line)
    }

    var journalText: String {
        journal.snapshot().map(\.message).joined(separator: "\n")
    }

    func object(_ text: String) -> JSONObject {
        if case let .object(object)? = JSONValue.parse(text) { return object }
        XCTFail("not a JSON object: \(text)")
        return JSONObject()
    }

    func json(_ text: String) -> JSONValue? {
        JSONValue.parse(text)
    }
}
