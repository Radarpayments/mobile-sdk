import Combine
import RadarpaymentsSDKPayment

/// A profile from outside that waits for the tester's answer before it is saved.
struct PendingProfile: Equatable {
    var profile: StandProfile
    /// A stored profile of the same name would be replaced.
    var replaces: Bool
}

/// Opens the sheet of an order and hands its result to the closure; false when nothing opened.
typealias Opener = @MainActor (OpenOrder, QaConfig, StandProfile, @escaping (CheckoutResult) -> Void) -> Bool

/// The app's one session, as long as the process lives: the form, the store, the journal, the run and the input from
/// outside waiting to be applied. The screens come and go; nothing a run needs lives in them.
@MainActor
final class QaSession: ObservableObject {
    let form: ConfigForm
    let editor = ProfileEditor()
    let store: QaStore
    let journal: Journal
    let run: QaRun

    /// The rejection of the last input from outside, or a save that failed; cleared by the next input or launch.
    @Published private(set) var notice: Problem?
    @Published private(set) var pending: PendingProfile?
    /// The finished run the result screen shows: set when a run finishes, cleared when the next one starts or by Run again.
    @Published private(set) var shown: FinishedRun?

    // The configuration of the last launch the check refused: its problem stays until the configuration changes.
    private var refused: QaConfig?
    // Inputs from outside wait here, in order, while a profile of an earlier one waits for its answer.
    private var queue: [[Intake]] = []
    private var rest: [Intake] = []
    // Input from outside waits while the scene is not active, so that a launch in it finds a key window to open over.
    private var active: Bool
    private var subscriptions: Set<AnyCancellable> = []

    /// `arguments` are the steps of the launch arguments: they come before any other input. With `active` false, input
    /// from outside waits for `setActive(true)`.
    init(
        store: QaStore,
        journal: Journal,
        merchant: Merchant,
        arguments: [Intake] = [],
        active: Bool = true,
        open: @escaping Opener
    ) {
        self.store = store
        self.journal = journal
        self.active = active
        if !arguments.isEmpty { queue.append(arguments) }
        form = ConfigForm(store.config)
        let relay = ResultRelay()
        run = QaRun(merchant: merchant, journal: journal) { order, config, profile in
            open(order, config, profile) { result in relay.run?.onResult(result) }
        }
        relay.run = run

        form.$config.dropFirst().sink { [weak self] config in self?.changed(config) }.store(in: &subscriptions)
        run.$state.sink { [weak self] state in
            if case let .finished(finished) = state { self?.shown = finished } else { self?.shown = nil }
        }.store(in: &subscriptions)
        for publisher in [form.objectWillChange, editor.objectWillChange, store.objectWillChange, run.objectWillChange] {
            publisher.sink { [weak self] _ in self?.objectWillChange.send() }.store(in: &subscriptions)
        }
        drain()
    }

    /// What the validation line shows: the form's problem, else the notice, else the refusal of the last launch.
    var shownProblem: Problem? {
        if let problem = form.problem { return problem }
        if let notice { return notice }
        if case let .invalid(problem) = run.state { return problem }
        return nil
    }

    /// The launch check's problem with the profile, shown under the profile picker.
    var profileProblem: Problem? {
        let problem = launchProblem(config: form.config, profile: selectedProfile)
        return problem?.field == "profile" ? problem : nil
    }

    var canLaunch: Bool {
        form.problem == nil && profileProblem == nil && !run.state.isBusy
    }

    var selectedProfile: StandProfile? {
        guard let name = form.config.profile else { return nil }
        return store.profiles.first { sameText($0.name, name) }
    }

    func launch() {
        start()
    }

    /// Applies the steps of one input from outside, after every input that came before it.
    func accept(_ steps: [Intake]) {
        queue.append(steps)
        drain()
    }

    func setActive(_ active: Bool) {
        self.active = active
        drain()
    }

    func confirmPending() {
        guard let pending else { return }
        self.pending = nil
        if let problem = write("the profile", { try store.save(profile: pending.profile) }) {
            notice = problem
            rest = []
        } else {
            apply(rest)
        }
        drain()
    }

    /// Declining drops the rest of that input and shows no error.
    func rejectPending() {
        guard pending != nil else { return }
        pending = nil
        rest = []
        drain()
    }

    /// Keeps the selected profile; everything else returns to its default.
    func reset() {
        form.set(QaConfig(profile: form.config.profile))
        notice = nil
    }

    func select(_ name: String) {
        form.edit(Tags.profile, name)
    }

    func saveProfile(_ profile: StandProfile) -> Bool {
        if let problem = write("the profile", { try store.save(profile: profile) }) {
            editor.failed(problem)
            return false
        }
        return true
    }

    func deleteProfile(named name: String) -> Bool {
        if let problem = write("the profile", verb: "delete", { try store.deleteProfile(named: name) }) {
            editor.failed(problem)
            return false
        }
        return true
    }

    /// Back from the result screen to the configuration, which is kept as it was.
    func runAgain() {
        run.reset()
    }

    private func changed(_ config: QaConfig) {
        store.save(config: config)
        if case .invalid = run.state, config != refused { run.reset() }
    }

    private func drain() {
        while active, pending == nil, !queue.isEmpty {
            notice = nil
            apply(queue.removeFirst())
        }
    }

    // Stops at a profile that needs an answer and keeps the steps after it in `rest`.
    private func apply(_ steps: [Intake]) {
        for (index, step) in steps.enumerated() {
            switch step {
            case let .rejected(problem):
                notice = problem
            case let .config(config):
                form.set(config)
            case let .profile(profile, needsConfirmation):
                if needsConfirmation {
                    rest = Array(steps[(index + 1)...])
                    pending = PendingProfile(profile: profile, replaces: store.profiles.contains { sameText($0.name, profile.name) })
                    return
                }
                if let problem = write("the profile", { try store.save(profile: profile) }) {
                    notice = problem
                    rest = []
                    return
                }
            case .launch:
                start()
            }
        }
        rest = []
    }

    private func start() {
        if run.state.isBusy || form.problem != nil { return }
        notice = nil
        let config = form.config
        let profile = selectedProfile
        if profile == nil || launchProblem(config: config, profile: profile) != nil { refused = config }
        run.launch(config: config, profile: profile)
    }

    // The message names what could not be saved or deleted and the kind of failure, never a value.
    private func write(_ what: String, verb: String = "save", _ action: () throws -> Void) -> Problem? {
        do {
            try action()
            return nil
        } catch {
            return Problem(field: "store", message: "could not \(verb) \(what) (\(type(of: error)))")
        }
    }
}

@MainActor
private final class ResultRelay {
    weak var run: QaRun?
}
