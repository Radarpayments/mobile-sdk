@testable import QaApp
import RadarpaymentsSDKPayment
import XCTest

@MainActor
final class QaSessionTests: MerchantTestCase {
    private let suite = "qa.session.tests"
    private let sandbox = StandProfile(
        name: "sandbox", gatewayUrl: "https://dev.bpcbt.com/payment", login: "login-1", password: "password-1"
    )
    private let given = QaConfig(profile: "sandbox", order: OrderConfig(source: .mdOrder, mdOrder: "md-1"))
    private var secrets = InMemorySecretStore()
    private var opened: [OpenOrder] = []
    private var deliver: ((CheckoutResult) -> Void)?

    override func tearDown() {
        UserDefaults().removePersistentDomain(forName: suite)
        super.tearDown()
    }

    private func session(
        profiles: [StandProfile]? = nil,
        config: QaConfig? = nil,
        arguments: [Intake] = [],
        active: Bool = true
    ) throws -> QaSession {
        UserDefaults().removePersistentDomain(forName: suite)
        secrets = InMemorySecretStore()
        let store = QaStore(defaults: UserDefaults(suiteName: suite)!, secrets: secrets)
        for profile in profiles ?? [sandbox] {
            try store.save(profile: profile)
        }
        store.save(config: config ?? given)
        return QaSession(
            store: store, journal: journal, merchant: merchant(), arguments: arguments, active: active
        ) { [unowned self] order, _, _, onResult in
            opened.append(order)
            deliver = onResult
            return true
        }
    }

    func test_the_form_starts_with_the_stored_configuration() throws {
        let session = try session()

        XCTAssertEqual(session.form.config, given)
    }

    func test_an_edit_that_parses_is_stored() throws {
        let session = try session()
        session.form.edit(Tags.orderAmount, "700")

        XCTAssertEqual(session.store.config.order.amount, 700)
    }

    func test_launch_is_refused_while_a_field_holds_text_that_does_not_parse() throws {
        let session = try session()
        session.form.edit(Tags.orderAmount, "5x")

        session.launch()

        XCTAssertEqual(session.run.state, .idle)
        XCTAssertEqual(opened, [])
        XCTAssertEqual(session.form.problem?.field, "order.amount")
        XCTAssertEqual(session.shownProblem?.field, "order.amount")
    }

    func test_an_external_launch_is_ignored_while_a_field_holds_text_that_does_not_parse() throws {
        let session = try session()
        session.form.edit(Tags.orderAmount, "5x")

        session.accept([.launch])

        XCTAssertEqual(session.run.state, .idle)
        XCTAssertEqual(opened, [])
        XCTAssertEqual(session.form.problem?.field, "order.amount")
    }

    func test_a_launch_opens_the_sheet_of_the_configured_order() throws {
        let session = try session()

        session.launch()

        XCTAssertEqual(session.run.state, .inSheet(OpenOrder(mdOrder: "md-1", sessionId: nil)))
        XCTAssertEqual(opened, [OpenOrder(mdOrder: "md-1", sessionId: nil)])
    }

    func test_input_arriving_during_a_run_is_applied_its_launch_is_ignored_and_the_runs_journal_is_kept() throws {
        let session = try session()
        session.launch()
        journal.record(.info, tag: "sdk", message: "first run")
        var changed = given
        changed.order.amount = 500

        session.accept([.config(changed), .launch])

        XCTAssertEqual(session.form.config.order.amount, 500)
        XCTAssertEqual(session.run.state, .inSheet(OpenOrder(mdOrder: "md-1", sessionId: nil)))
        XCTAssertEqual(journal.snapshot().map(\.message), ["first run"])
        XCTAssertEqual(opened, [OpenOrder(mdOrder: "md-1", sessionId: nil)])
    }

    func test_the_sheets_result_finishes_the_run_and_the_result_is_shown_once() async throws {
        let session = try session()
        session.launch()

        try XCTUnwrap(deliver)(CheckoutResultCanceled(orderId: "md-1"))
        await session.run.waitUntilSettled()

        XCTAssertEqual(session.shown?.outcome.type, .canceled)
        session.runAgain()
        XCTAssertNil(session.shown)
        XCTAssertEqual(session.run.state, .idle)
        XCTAssertEqual(session.form.config, given)
    }

    func test_a_launch_the_check_refuses_shows_its_problem_until_the_configuration_changes() throws {
        let session = try session(config: QaConfig(profile: "missing"))

        session.launch()

        XCTAssertEqual(session.shownProblem?.field, "profile")
        session.form.edit(Tags.orderAmount, "700")
        XCTAssertEqual(session.run.state, .idle)
        XCTAssertNil(session.shownProblem)
    }

    func test_reset_keeps_the_selected_profile() throws {
        var config = given
        config.order.amount = 700
        let session = try session(config: config)

        session.reset()

        XCTAssertEqual(session.form.config, QaConfig(profile: "sandbox"))
    }

    func test_selecting_a_profile_puts_it_in_the_configuration() throws {
        let session = try session(config: QaConfig())

        session.select("sandbox")

        XCTAssertEqual(session.form.config.profile, "sandbox")
    }

    func test_a_profile_that_cannot_be_saved_from_the_editor_is_reported_there_without_its_secrets() throws {
        let session = try session()
        secrets.failsWrites = true

        let saved = session.saveProfile(StandProfile(
            name: "other", gatewayUrl: "https://dev.bpcbt.com/payment", login: "login-1", password: "password-1"
        ))

        XCTAssertFalse(saved)
        XCTAssertEqual(session.editor.problem?.field, "store")
        XCTAssertFalse(session.editor.problem?.message.contains("password-1") ?? true)
        XCTAssertEqual(session.store.profiles, [sandbox])
    }

    func test_a_profile_that_cannot_be_deleted_is_reported_in_the_editor() throws {
        let session = try session()
        secrets.failsWrites = true

        XCTAssertFalse(session.deleteProfile(named: "sandbox"))
        XCTAssertEqual(session.editor.problem?.field, "store")
        XCTAssertTrue(session.editor.problem?.message.hasPrefix("could not delete the profile") ?? false)
    }

    func test_a_profile_from_outside_that_cannot_be_saved_is_reported_and_the_rest_of_that_input_is_dropped() throws {
        let session = try session(profiles: [], config: QaConfig())
        secrets.failsWrites = true

        session.accept([.profile(sandbox, needsConfirmation: false), .config(given), .launch])

        XCTAssertEqual(session.notice?.field, "store")
        XCTAssertFalse(session.notice?.message.contains("password-1") ?? true)
        XCTAssertEqual(session.form.config, QaConfig())
        XCTAssertEqual(opened, [])
    }

    func test_a_profile_from_a_launch_argument_is_saved_without_confirmation_but_not_selected() throws {
        let session = try session(profiles: [], config: QaConfig())

        session.accept([.profile(sandbox, needsConfirmation: false)])

        XCTAssertNil(session.pending)
        XCTAssertEqual(session.store.profiles, [sandbox])
        XCTAssertNil(session.form.config.profile)
    }

    func test_a_confirmed_profile_is_saved_and_the_rest_of_the_input_applies() throws {
        let session = try session(profiles: [], config: QaConfig())

        session.accept([.profile(sandbox, needsConfirmation: true), .config(given), .launch])
        XCTAssertEqual(session.pending, PendingProfile(profile: sandbox, replaces: false))
        XCTAssertEqual(session.store.profiles, [])
        session.confirmPending()

        XCTAssertNil(session.pending)
        XCTAssertEqual(session.store.profiles, [sandbox])
        XCTAssertEqual(session.form.config, given)
        XCTAssertEqual(opened, [OpenOrder(mdOrder: "md-1", sessionId: nil)])
    }

    func test_a_profile_that_replaces_one_says_so() throws {
        let session = try session()

        session.accept([.profile(sandbox, needsConfirmation: true)])

        XCTAssertEqual(session.pending?.replaces, true)
    }

    func test_declining_a_profile_drops_the_rest_of_that_input_and_shows_no_error() throws {
        let session = try session(profiles: [], config: QaConfig())

        session.accept([.profile(sandbox, needsConfirmation: true), .config(given), .launch])
        session.rejectPending()

        XCTAssertEqual(session.store.profiles, [])
        XCTAssertEqual(session.form.config, QaConfig())
        XCTAssertEqual(opened, [])
        XCTAssertNil(session.notice)
        XCTAssertNil(session.shownProblem)
    }

    func test_a_second_profile_waits_until_the_first_confirmation_is_answered() throws {
        let session = try session(profiles: [], config: QaConfig())
        var second = sandbox
        second.name = "second"

        session.accept([.profile(sandbox, needsConfirmation: true)])
        session.accept([.profile(second, needsConfirmation: true)])
        XCTAssertEqual(session.pending?.profile, sandbox)
        session.confirmPending()

        XCTAssertEqual(session.pending?.profile, second)
        XCTAssertEqual(session.store.profiles, [sandbox])
    }

    func test_a_rejected_input_is_the_notice_until_the_next_input() throws {
        let session = try session()

        session.accept([.rejected(Problem(field: "data", message: "must be UTF-8 JSON"))])
        XCTAssertEqual(session.shownProblem?.field, "data")
        session.accept([.config(given)])

        XCTAssertNil(session.notice)
    }

    func test_a_config_naming_a_profile_that_arrives_in_the_same_input_selects_it() throws {
        let session = try session(profiles: [], config: QaConfig())

        session.accept([.profile(sandbox, needsConfirmation: false), .config(given)])

        XCTAssertEqual(session.selectedProfile, sandbox)
    }

    func test_a_config_naming_a_profile_that_arrived_earlier_selects_it() throws {
        let session = try session(profiles: [], config: QaConfig())

        session.accept([.profile(sandbox, needsConfirmation: true)])
        session.confirmPending()
        XCTAssertNil(session.selectedProfile)
        session.accept([.config(given)])

        XCTAssertEqual(session.selectedProfile, sandbox)
    }

    func test_input_and_reset_while_the_sheet_is_up_leave_the_run_and_its_journal() throws {
        let session = try session()
        session.launch()
        journal.record(.info, tag: "sdk", message: "first run")

        session.accept([.config(QaConfig(profile: "sandbox"))])
        session.reset()

        XCTAssertEqual(session.run.state, .inSheet(OpenOrder(mdOrder: "md-1", sessionId: nil)))
        XCTAssertEqual(journal.snapshot().map(\.message), ["first run"])
    }

    func test_input_waits_until_the_scene_is_active_and_the_launch_arguments_come_first() throws {
        var fromArguments = given
        fromArguments.order.amount = 500
        var fromLink = given
        fromLink.order.amount = 700
        let session = try session(
            profiles: [],
            config: QaConfig(),
            arguments: [.profile(sandbox, needsConfirmation: false), .config(fromArguments)],
            active: false
        )

        session.accept([.config(fromLink), .launch])
        XCTAssertEqual(session.form.config, QaConfig())
        XCTAssertEqual(session.store.profiles, [])
        session.setActive(true)

        XCTAssertEqual(session.store.profiles, [sandbox])
        XCTAssertEqual(session.form.config, fromLink)
        XCTAssertEqual(opened, [OpenOrder(mdOrder: "md-1", sessionId: nil)])
    }

    func test_launch_arguments_apply_at_once_in_an_active_session() throws {
        var fromArguments = given
        fromArguments.order.amount = 500

        let session = try session(config: QaConfig(), arguments: [.config(fromArguments)])

        XCTAssertEqual(session.form.config, fromArguments)
    }
}
