package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.launchProblem
import net.payrdr.mobile.payment.sdk.qa.intake.Intake
import net.payrdr.mobile.payment.sdk.qa.journal.Journal
import net.payrdr.mobile.payment.sdk.qa.launcher.OpenOrder
import net.payrdr.mobile.payment.sdk.qa.launcher.QaRun
import net.payrdr.mobile.payment.sdk.qa.launcher.RunState
import net.payrdr.mobile.payment.sdk.qa.merchant.Merchant
import net.payrdr.mobile.payment.sdk.qa.storage.QaStore

/** A launch the checkout is built for; [id] tells two launches of the same configuration apart. */
internal data class Launched(
    val config: QaConfig,
    val profile: StandProfile,
    val id: Long,
)

/** A profile from a link, the clipboard or a QR code that waits for QA to confirm it. */
internal class PendingProfile(
    val profile: StandProfile,
    val replaces: Boolean,
    private val answer: CompletableDeferred<Boolean>,
) {
    fun confirm() {
        answer.complete(true)
    }

    fun reject() {
        answer.complete(false)
    }
}

/**
 * The app's one session: the form, the stored profiles, the run, and the external input, applied one batch after
 * another in the order it arrived. It belongs to the process, not to a screen: an input that starts a second screen
 * above the open sheet must reach the run that is under way, not start another one.
 */
internal class QaSession(
    private val store: QaStore,
    val journal: Journal,
    merchant: Merchant,
    private val scope: CoroutineScope,
) {
    val form = ConfigForm(QaConfig())

    val editor = ProfileEditor()

    /** False until the stored configuration is in the form; the form is not shown before, so no edit is overwritten. */
    var ready by mutableStateOf(false)
        private set

    val profiles: StateFlow<List<StandProfile>> = store.profiles.stateIn(scope, SharingStarted.Eagerly, emptyList())

    /** The last rejection of external input or failed write, until the next input or launch. */
    var notice: Problem? by mutableStateOf(null)
        private set

    var pending: PendingProfile? by mutableStateOf(null)
        private set

    /**
     * The last launch that passed the checks. The checkout is composed for it before the run starts ([checkoutReady]),
     * because the checkout reads its configuration from composition and would otherwise open with the previous one.
     */
    var launched: Launched? by mutableStateOf(null)
        private set

    /** The finished run the result screen was opened for, so that it is opened once per run. */
    var shown: RunState.Finished? = null

    private var started = 0L

    private var refused: QaConfig? = null

    // The checkouts on screen, the newest last. A run waits for one instead of failing while none is composed.
    private val presenters = MutableStateFlow<List<(OpenOrder) -> Unit>>(emptyList())

    val run = QaRun(merchant, journal, scope) { order -> presenters.map { it.lastOrNull() }.filterNotNull().first()(order) }

    private val intakes = Channel<List<Intake>>(Channel.UNLIMITED)

    init {
        scope.launch {
            form.set(store.config.first())
            ready = true
            launch {
                snapshotFlow { form.config }.collectLatest { config ->
                    // A refusal is about the config it was given; once QA changes that config it is stale.
                    if (run.state.value is RunState.Invalid && config != refused) run.reset()
                    write("the configuration") { store.saveConfig(config) }?.let { notice = it }
                }
            }
            for (steps in intakes) apply(steps)
        }
    }

    fun accept(steps: List<Intake>) {
        intakes.trySend(steps)
    }

    fun launch() {
        start(profiles.value)
    }

    fun attach(presenter: (OpenOrder) -> Unit) {
        presenters.update { it + presenter }
    }

    fun detach(presenter: (OpenOrder) -> Unit) {
        presenters.update { it - presenter }
    }

    fun checkoutReady(launch: Launched) {
        if (launch.id <= started) return
        started = launch.id
        run.launch(launch.config, launch.profile)
    }

    fun reset() {
        form.set(QaConfig(profile = form.config.profile))
        notice = null
    }

    fun select(name: String) {
        form.edit(Tags.PROFILE, name)
    }

    fun saveProfile(
        profile: StandProfile,
        onSaved: () -> Unit,
    ) {
        scope.launch {
            val problem = write("the profile") { store.saveProfile(profile) }
            if (problem == null) onSaved() else editor.failed(problem)
        }
    }

    fun deleteProfile(
        name: String,
        onDeleted: () -> Unit,
    ) {
        scope.launch {
            val problem = write("the profile") { store.deleteProfile(name) }
            if (problem == null) onDeleted() else editor.failed(problem)
        }
    }

    private fun start(profiles: List<StandProfile>) {
        // A launch whose checkout is not composed yet is under way too.
        if (run.state.value.isBusy() || (launched?.id ?: 0) > started) return
        // The field shows text that is not in the config; launching the last good value would test something else.
        if (form.problem != null) return
        notice = null
        val config = form.config
        val profile = profiles.find { it.name == config.profile }
        if (profile == null || launchProblem(config, profile) != null) {
            refused = config
            run.launch(config, profile)
        } else {
            launched = Launched(config, profile, (launched?.id ?: 0) + 1)
        }
    }

    private suspend fun apply(steps: List<Intake>) {
        notice = null
        for (step in steps) {
            when (step) {
                is Intake.Rejected -> notice = step.problem
                is Intake.Config -> form.set(step.config)
                is Intake.Profile -> {
                    if (step.needsConfirmation && !confirmed(step.profile)) return
                    val problem = write("the profile") { store.saveProfile(step.profile) }
                    if (problem != null) {
                        notice = problem
                        return
                    }
                }
                Intake.Launch -> start(store.profiles.first())
            }
        }
    }

    private suspend fun confirmed(profile: StandProfile): Boolean {
        val answer = CompletableDeferred<Boolean>()
        pending = PendingProfile(profile, store.profiles.first().any { it.name == profile.name }, answer)
        return try {
            answer.await()
        } finally {
            pending = null
        }
    }
}

/**
 * Runs a store write and returns what went wrong instead of throwing: a full disk, a Keystore that refuses to seal.
 * The message names the exception's class only, since a message could carry what was being sealed.
 */
@Suppress("TooGenericExceptionCaught")
private suspend fun write(
    what: String,
    block: suspend () -> Unit,
): Problem? =
    try {
        block()
        null
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Problem("store", "could not save $what (${error.javaClass.simpleName})")
    }

internal fun RunState.isBusy(): Boolean = this is RunState.Registering || this is RunState.InSheet || this is RunState.QueryingStatus
