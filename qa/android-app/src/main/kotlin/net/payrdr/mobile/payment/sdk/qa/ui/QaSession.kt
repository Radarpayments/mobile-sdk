package net.payrdr.mobile.payment.sdk.qa.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerProvider
import net.payrdr.mobile.payment.sdk.qa.QaApplication
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.launchProblem
import net.payrdr.mobile.payment.sdk.qa.intake.Intake
import net.payrdr.mobile.payment.sdk.qa.journal.Journal
import net.payrdr.mobile.payment.sdk.qa.launcher.OpenOrder
import net.payrdr.mobile.payment.sdk.qa.launcher.QaRun
import net.payrdr.mobile.payment.sdk.qa.launcher.RunState
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
 * The state the screens share, kept across a rotation: the form, the stored profiles, the run, and the external input,
 * applied one batch after another in the order it arrived.
 */
internal class QaSession(
    application: Application,
) : AndroidViewModel(application) {
    private val app = application as QaApplication
    private val store: QaStore = app.store

    val journal: Journal = app.journal

    val scanner: CardScannerProvider = app.scanner

    val form = ConfigForm(QaConfig())

    val editor = ProfileEditor()

    /** False until the stored configuration is in the form; the form is not shown before, so no edit is overwritten. */
    var ready by mutableStateOf(false)
        private set

    val profiles: StateFlow<List<StandProfile>> = store.profiles.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** The last rejection of external input, until the next input or launch. */
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

    private var started = 0L

    /** Opens the sheet; set by the checkout while it is on screen. */
    var presenter: ((OpenOrder) -> Unit)? = null

    val run = QaRun(app.merchant, journal, viewModelScope) { order -> checkNotNull(presenter) { "The checkout is not on screen" }(order) }

    private val intakes = Channel<List<Intake>>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            form.set(store.config.first())
            ready = true
            launch {
                snapshotFlow { form.config }.collectLatest { config ->
                    // A refusal is about the config it was given; once QA changes that config it is stale.
                    if (run.state.value is RunState.Invalid && config != refused) run.reset()
                    store.saveConfig(config)
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

    private var refused: QaConfig? = null

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

    fun saveProfile(profile: StandProfile) {
        viewModelScope.launch { store.saveProfile(profile) }
    }

    fun deleteProfile(name: String) {
        viewModelScope.launch { store.deleteProfile(name) }
    }

    private fun start(profiles: List<StandProfile>) {
        if (run.state.value.isBusy()) return
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
                    store.saveProfile(step.profile)
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

internal fun RunState.isBusy(): Boolean = this is RunState.Registering || this is RunState.InSheet || this is RunState.QueryingStatus
