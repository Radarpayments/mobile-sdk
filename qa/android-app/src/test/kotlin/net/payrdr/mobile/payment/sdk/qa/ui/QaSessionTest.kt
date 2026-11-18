package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.runtime.snapshots.Snapshot
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.payrdr.mobile.payment.sdk.payment.logger.LogLevel
import net.payrdr.mobile.payment.sdk.qa.config.OrderConfig
import net.payrdr.mobile.payment.sdk.qa.config.OrderSource
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.intake.Intake
import net.payrdr.mobile.payment.sdk.qa.launcher.OpenOrder
import net.payrdr.mobile.payment.sdk.qa.launcher.RunState
import net.payrdr.mobile.payment.sdk.qa.merchant.FakeGateway
import net.payrdr.mobile.payment.sdk.qa.storage.QaStore
import net.payrdr.mobile.payment.sdk.qa.storage.SecretBox
import java.io.IOException
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QaSessionTest {
    private val sandbox = StandProfile("sandbox", "https://dev.bpcbt.com/payment", login = "login-1", password = "password-1")
    private val given = QaConfig(profile = "sandbox", order = OrderConfig(source = OrderSource.MD_ORDER, mdOrder = "md-1"))

    private class MemoryDataStore : DataStore<Preferences> {
        var failing = false
        private val stored = MutableStateFlow(emptyPreferences())
        override val data: Flow<Preferences> = stored

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            if (failing) throw IOException("No space left on device")
            return transform(stored.value).also { stored.value = it }
        }
    }

    private class PlainBox : SecretBox {
        var failing = false

        override fun seal(plain: String): String {
            check(!failing) { "cannot seal $plain" }
            return "sealed:$plain"
        }

        override fun open(sealed: String): String = sealed.removePrefix("sealed:")
    }

    private class Fixture(
        val session: QaSession,
        val dataStore: MemoryDataStore,
        val box: PlainBox,
        private val scope: CoroutineScope,
    ) {
        val presented = mutableListOf<OpenOrder>()
        val presenter: (OpenOrder) -> Unit = { presented += it }

        fun close() = scope.cancel()
    }

    private suspend fun TestScope.fixture(
        profiles: List<StandProfile> = listOf(sandbox),
        config: QaConfig = given,
        prepare: (MemoryDataStore, PlainBox) -> Unit = { _, _ -> },
    ): Fixture {
        val dataStore = MemoryDataStore()
        val box = PlainBox()
        val store = QaStore(dataStore, box, StandardTestDispatcher(testScheduler))
        profiles.forEach { store.saveProfile(it) }
        store.saveConfig(config)
        prepare(dataStore, box)
        val gateway = FakeGateway()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val session = QaSession(store, gateway.journal, gateway.merchant(), scope)
        advanceUntilIdle()
        return Fixture(session, dataStore, box, scope)
    }

    private fun TestScope.startRun(fixture: Fixture) {
        fixture.session.launch()
        advanceUntilIdle()
        fixture.session.checkoutReady(checkNotNull(fixture.session.launched))
        advanceUntilIdle()
    }

    @Test
    fun `Launch is refused while a field holds text that does not parse`() =
        runTest {
            val f = fixture()
            f.session.form.edit(Tags.ORDER_AMOUNT, "5x")

            f.session.launch()
            advanceUntilIdle()

            f.session.launched shouldBe null
            f.session.run.state.value shouldBe RunState.Idle
            f.session.form.problem
                ?.field shouldBe "order.amount"
            f.close()
        }

    @Test
    fun `an external launch is ignored while a field holds text that does not parse`() =
        runTest {
            val f = fixture()
            f.session.form.edit(Tags.ORDER_AMOUNT, "5x")

            f.session.accept(listOf(Intake.Launch))
            advanceUntilIdle()

            f.session.launched shouldBe null
            f.session.run.state.value shouldBe RunState.Idle
            f.session.form.problem
                ?.field shouldBe "order.amount"
            f.close()
        }

    @Test
    fun `input arriving during a run is applied, its launch is ignored and the run's journal is kept`() =
        runTest {
            val f = fixture()
            f.session.attach(f.presenter)
            startRun(f)
            f.session.journal.record(LogLevel.INFO, "sdk", "first run")

            f.session.accept(listOf(Intake.Config(given.copy(order = given.order.copy(amount = 500))), Intake.Launch))
            advanceUntilIdle()

            f.session.form.config.order.amount shouldBe 500
            f.session.launched?.id shouldBe 1
            f.session.run.state.value
                .shouldBeInstanceOf<RunState.InSheet>()
            f.session.journal.entries.value
                .map { it.message } shouldBe listOf("first run")
            f.presented shouldBe listOf(OpenOrder("md-1", null))
            f.close()
        }

    @Test
    fun `the sheet waits for the checkout when it is not on screen`() =
        runTest {
            val f = fixture()
            startRun(f)

            f.session.run.state.value
                .shouldBeInstanceOf<RunState.InSheet>()
            f.presented shouldBe emptyList()

            f.session.attach(f.presenter)
            advanceUntilIdle()

            f.presented shouldBe listOf(OpenOrder("md-1", null))
            f.session.run.state.value
                .shouldBeInstanceOf<RunState.InSheet>()
            f.close()
        }

    @Test
    fun `a checkout that left the screen no longer opens the sheet`() =
        runTest {
            val f = fixture()
            val gone: (OpenOrder) -> Unit = { error("the checkout of the destroyed screen was called") }
            f.session.attach(gone)
            f.session.attach(f.presenter)
            f.session.detach(gone)
            startRun(f)

            f.presented shouldBe listOf(OpenOrder("md-1", null))
            f.close()
        }

    @Test
    fun `a configuration that cannot be written is reported instead of crashing`() =
        runTest {
            val f = fixture { dataStore, _ -> dataStore.failing = true }
            f.session.form.edit(Tags.ORDER_AMOUNT, "700")
            // Compose applies snapshot writes on its frame clock; without a composition the test sends them itself.
            Snapshot.sendApplyNotifications()
            advanceUntilIdle()

            f.session.ready shouldBe true
            f.session.notice?.field shouldBe "store"
            f.close()
        }

    @Test
    fun `a profile that cannot be sealed is reported in the editor without its secrets`() =
        runTest {
            val f = fixture { _, box -> box.failing = true }
            var saved = false

            f.session.saveProfile(sandbox.copy(name = "other")) { saved = true }
            advanceUntilIdle()

            saved shouldBe false
            f.session.editor.problem
                ?.field shouldBe "store"
            f.session.editor.problem
                ?.message
                .orEmpty() shouldNotContain "password-1"
            f.close()
        }

    @Test
    fun `a profile that cannot be deleted says so`() =
        runTest {
            val f = fixture { dataStore, _ -> dataStore.failing = true }
            var deleted = false

            f.session.deleteProfile(sandbox.name) { deleted = true }
            advanceUntilIdle()

            deleted shouldBe false
            f.session.editor.problem shouldBe Problem("store", "could not delete the profile (IOException)")
            f.close()
        }

    @Test
    fun `a profile from outside that cannot be saved is reported and the rest of that input is dropped`() =
        runTest {
            val f = fixture(profiles = emptyList(), config = QaConfig()) { _, box -> box.failing = true }

            f.session.accept(listOf(Intake.Profile(sandbox, needsConfirmation = false), Intake.Config(given), Intake.Launch))
            advanceUntilIdle()

            f.session.notice?.field shouldBe "store"
            f.session.notice
                ?.message
                .orEmpty() shouldNotContain "password-1"
            f.session.form.config shouldBe QaConfig()
            f.session.launched shouldBe null
            f.close()
        }

    @Test
    fun `a profile from outside is saved but not selected`() =
        runTest {
            val f = fixture(profiles = emptyList(), config = QaConfig())

            f.session.accept(listOf(Intake.Profile(sandbox, needsConfirmation = false)))
            advanceUntilIdle()

            f.session.profiles.value shouldBe listOf(sandbox)
            f.session.form.config.profile shouldBe null
            f.close()
        }

    @Test
    fun `declining a profile drops the rest of that input and shows no error`() =
        runTest {
            val f = fixture(profiles = emptyList(), config = QaConfig())

            f.session.accept(listOf(Intake.Profile(sandbox, needsConfirmation = true), Intake.Config(given), Intake.Launch))
            advanceUntilIdle()
            checkNotNull(f.session.pending).reject()
            advanceUntilIdle()

            f.session.profiles.value shouldBe emptyList()
            f.session.form.config shouldBe QaConfig()
            f.session.launched shouldBe null
            f.session.notice shouldBe null
            f.close()
        }

    @Test
    fun `a second profile waits until the first confirmation is answered`() =
        runTest {
            val f = fixture(profiles = emptyList(), config = QaConfig())
            val second = sandbox.copy(name = "second")

            f.session.accept(listOf(Intake.Profile(sandbox, needsConfirmation = true)))
            f.session.accept(listOf(Intake.Profile(second, needsConfirmation = true)))
            advanceUntilIdle()
            f.session.pending?.profile shouldBe sandbox
            checkNotNull(f.session.pending).confirm()
            advanceUntilIdle()

            f.session.pending?.profile shouldBe second
            f.session.profiles.value shouldBe listOf(sandbox)
            f.close()
        }
}
