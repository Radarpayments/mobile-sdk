import Combine
import Foundation
import RadarpaymentsSDKPayment

/// How a run ended; the raw value is the `qa_result_type` text of the contract.
enum ResultType: String, CaseIterable {
    case completed
    case failed
    case canceled
    case unknown
    case registrationFailed = "registration_failed"
    case sdkError = "sdk_error"
}

/// The order the sheet is opened with: a session id when there is one, the mdOrder otherwise.
struct OpenOrder: Equatable {
    var mdOrder: String?
    var sessionId: String?
}

/// What a run ended with. `error` is the SDK's error text, the gateway's answer to a failed registration, or the type
/// and message of the error that kept the sheet from opening; `httpStatus` is the gateway's HTTP status when it named one.
struct RunOutcome: Equatable {
    var type: ResultType
    var mdOrder: String?
    var sessionId: String?
    var error: String?
    var httpStatus: Int?
    var status: OrderStatus
}

/// A finished run with the config and the profile it was launched with, so that its report describes this run and not
/// what the form holds by the time QA copies it. Its description prints no credentials (`StandProfile` masks them).
struct FinishedRun: Equatable {
    var outcome: RunOutcome
    var config: QaConfig
    var profile: StandProfile
}

enum RunState: Equatable {
    case idle
    case invalid(Problem)
    case registering
    case inSheet(OpenOrder)
    case queryingStatus
    case finished(FinishedRun)

    var isBusy: Bool {
        switch self {
        case .registering, .inSheet, .queryingStatus: true
        case .idle, .invalid, .finished: false
        }
    }
}

/// One QA run: checks the configuration, clears the journal, registers the order (or takes the configured one), has
/// `present` open the sheet, takes its result in `onResult`, asks the gateway for the order's status and finishes.
/// A launch while a run is under way is ignored.
///
/// `present` opens the sheet for the order with the run's own config and profile and returns whether it opened; the
/// sheet's result must come back through `onResult`, on the main actor.
@MainActor
final class QaRun: ObservableObject {
    typealias Present = @MainActor (OpenOrder, QaConfig, StandProfile) -> Bool

    @Published private(set) var state: RunState = .idle

    private let merchant: Merchant
    private let journal: Journal
    private let present: Present

    // The run allowed to change the state. Its tasks write only while it is still current, so a registration or a
    // status query that answers after a reset cannot overwrite the run started since.
    private var current: Attempt?
    private var tasks: [UUID: Task<Void, Never>] = [:]

    init(merchant: Merchant, journal: Journal, present: @escaping Present) {
        self.merchant = merchant
        self.journal = journal
        self.present = present
    }

    func launch(config: QaConfig, profile: StandProfile?) {
        if state.isBusy { return }
        let problem = launchProblem(config: config, profile: profile)
        guard problem == nil, let profile else {
            state = .invalid(problem ?? Problem(field: "profile", message: "select a stored profile"))
            return
        }
        journal.clear()
        let attempt = Attempt(config: config, profile: profile)
        current = attempt
        if let order = givenOrder(config) {
            state = .inSheet(order)
            open(attempt, order)
        } else {
            state = .registering
            start(attempt) { [weak self] in await self?.register(attempt) }
        }
    }

    /// The sheet's result; ignored unless a sheet of the current run is open.
    func onResult(_ result: CheckoutResult) {
        guard let attempt = current, case let .inSheet(order) = state else { return }
        state = .queryingStatus
        start(attempt) { [weak self] in await self?.finish(attempt, order, result) }
    }

    func reset() {
        current?.task?.cancel()
        current = nil
        state = .idle
    }

    /// Waits until no registration or status query of any run is in flight (tests).
    func waitUntilSettled() async {
        while let task = tasks.values.first {
            await task.value
        }
    }

    private func register(_ attempt: Attempt) async {
        let registration: Registration
        do {
            registration = try await merchant.register(order: attempt.config.order, profile: attempt.profile)
        } catch {
            return
        }
        switch registration {
        case let .registered(mdOrder, sessionId):
            let order = OpenOrder(mdOrder: mdOrder, sessionId: sessionId)
            if move(attempt, to: .inSheet(order)) { open(attempt, order) }
        case let .failed(httpStatus, message):
            let outcome = RunOutcome(
                type: .registrationFailed, mdOrder: nil, sessionId: nil, error: message, httpStatus: httpStatus, status: .notQueried
            )
            move(attempt, to: attempt.finished(outcome))
        }
    }

    private func open(_ attempt: Attempt, _ order: OpenOrder) {
        if present(order, attempt.config, attempt.profile) { return }
        // The present call may have reset this run and launched another one, or delivered a result before giving up.
        guard current === attempt else { return }
        let error = sheetError(PayrdrCheckoutError.notPresented)
        journal.record(.error, tag: Self.tag, message: "The sheet threw \(error)")
        if case .inSheet = state {
            let outcome = RunOutcome(
                type: .sdkError, mdOrder: order.mdOrder, sessionId: order.sessionId, error: error, httpStatus: nil, status: .notQueried
            )
            state = attempt.finished(outcome)
        }
    }

    private func finish(_ attempt: Attempt, _ order: OpenOrder, _ result: CheckoutResult) async {
        // After a session the app knows the order only from the result; an empty orderId means the sheet closed first.
        let mdOrder = order.mdOrder ?? (isBlank(result.orderId) ? nil : result.orderId)
        let status: OrderStatus
        do {
            status = try await merchant.status(orderId: mdOrder, profile: attempt.profile)
        } catch {
            return
        }
        let (type, error) = Self.typeAndError(of: result)
        let outcome = RunOutcome(
            type: type,
            mdOrder: mdOrder,
            sessionId: order.sessionId,
            error: error?.message,
            httpStatus: (error as? SdkPaymentApiException)?.httpStatus.map { Int(truncating: $0) },
            status: status
        )
        move(attempt, to: attempt.finished(outcome))
    }

    private func start(_ attempt: Attempt, _ body: @escaping @MainActor () async -> Void) {
        let id = UUID()
        let task = Task { @MainActor [weak self] in
            await body()
            self?.tasks[id] = nil
        }
        attempt.task = task
        tasks[id] = task
    }

    @discardableResult
    private func move(_ attempt: Attempt, to next: RunState) -> Bool {
        guard current === attempt else { return false }
        state = next
        return true
    }

    private func givenOrder(_ config: QaConfig) -> OpenOrder? {
        switch config.order.source {
        case .register: nil
        case .mdOrder: OpenOrder(mdOrder: config.order.mdOrder, sessionId: nil)
        case .sessionId: OpenOrder(mdOrder: nil, sessionId: config.order.sessionId)
        }
    }

    private static func typeAndError(of result: CheckoutResult) -> (ResultType, SdkException?) {
        switch onEnum(of: result) {
        case .completed: (.completed, nil)
        case let .failed(failed): (.failed, failed.error)
        case .canceled: (.canceled, nil)
        case let .unknown(unknown): (.unknown, unknown.cause)
        }
    }

    private static let tag = "qa"

    private final class Attempt {
        let config: QaConfig
        let profile: StandProfile
        var task: Task<Void, Never>?

        init(config: QaConfig, profile: StandProfile) {
            self.config = config
            self.profile = profile
        }

        func finished(_ outcome: RunOutcome) -> RunState {
            .finished(FinishedRun(outcome: outcome, config: config, profile: profile))
        }
    }
}

/// `<TypeName>: <message>`, or the type name alone when there is no message, as Android names a throwable.
///
/// An error from a Kotlin `@Throws` function arrives as an `NSError` holding the Kotlin exception, named the way the
/// journal names Kotlin causes. A Swift error is named by its own type: bridged to an object it would be
/// `__SwiftNativeNSError`. Its message is what it states itself (`LocalizedError.errorDescription`, an `NSError`'s
/// `NSLocalizedDescriptionKey`), never `localizedDescription`, which is localized and invents a text when there is none.
nonisolated func sheetError(_ error: any Error) -> String {
    let name: String
    let message: String?
    let bridged = error as NSError
    if let kotlin = bridged.userInfo["KotlinException"] as? KotlinThrowable {
        name = causeName(of: kotlin)
        message = kotlin.message
    } else {
        name = String(describing: type(of: error))
        message = (error as? LocalizedError)?.errorDescription ?? bridged.userInfo[NSLocalizedDescriptionKey] as? String
    }
    guard let message, !isBlank(message) else { return name }
    return "\(name): \(message)"
}
