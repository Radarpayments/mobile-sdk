@file:OptIn(ExperimentalComposeUiApi::class)

package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import net.payrdr.mobile.payment.sdk.payment.android.rememberPayrdrCheckout
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerProvider
import net.payrdr.mobile.payment.sdk.qa.launcher.OpenOrder
import net.payrdr.mobile.payment.sdk.qa.launcher.RunState
import net.payrdr.mobile.payment.sdk.qa.launcher.sdkPaymentConfig

/** The app's screens, the checkout that stays composed under them, and the confirmation of an incoming profile. */
@Composable
internal fun QaNavGraph(
    session: QaSession,
    scanner: CardScannerProvider,
) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }) {
            Column(Modifier.safeDrawingPadding()) {
                if (session.ready) Screens(session)
            }
            session.launched?.let { Checkout(session, scanner, it) }
            session.pending?.let { ProfileConfirmation(it) }
        }
    }
}

@Composable
private fun Screens(session: QaSession) {
    val nav = rememberNavController()
    val state by session.run.state.collectAsState()
    val profiles by session.profiles.collectAsState()
    val entries by session.journal.entries.collectAsState()
    LaunchedEffect(state) {
        // Once per run: a rotation re-runs this effect, and QA may have gone from the result to the journal since.
        val finished = state as? RunState.Finished ?: return@LaunchedEffect
        if (session.shown === finished) return@LaunchedEffect
        session.shown = finished
        if (nav.currentDestination?.route != RESULT) nav.navigate(RESULT) { popUpTo(CONFIG) }
    }
    NavHost(nav, startDestination = CONFIG) {
        composable(CONFIG) {
            ConfigScreen(session, profiles, state, onProfiles = { nav.navigate(PROFILES) }, onJournal = { nav.openJournal() })
        }
        composable(PROFILES) {
            ProfilesScreen(session, profiles, onBack = { nav.popBackStack() }, onJournal = { nav.openJournal() })
        }
        composable(RESULT) {
            val runAgain = {
                session.run.reset()
                nav.popBackStack(CONFIG, inclusive = false)
            }
            BackHandler(onBack = { runAgain() })
            when (val finished = state) {
                is RunState.Finished -> ResultScreen(finished, entries, onRunAgain = { runAgain() }, onJournal = { nav.openJournal() })
                else -> LaunchedEffect(Unit) { nav.popBackStack(CONFIG, inclusive = false) }
            }
        }
        composable(JOURNAL) {
            JournalScreen(session.journal, entries, onBack = { nav.popBackStack() })
        }
    }
}

/**
 * The checkout of the last launch. It stays composed under every screen, so the sheet's result reaches it whichever
 * screen is shown, and it is keyed on the configuration and the profile: two builds of one config with pinning are
 * not equal, so the SDK config cannot be the key.
 */
@Composable
private fun Checkout(
    session: QaSession,
    scanner: CardScannerProvider,
    launch: Launched,
) {
    val config =
        remember(launch.config, launch.profile) {
            sdkPaymentConfig(launch.config, launch.profile, session.journal, scanner)
        }
    val checkout = rememberPayrdrCheckout(config, session.run::onResult)
    DisposableEffect(checkout) {
        val presenter: (OpenOrder) -> Unit = { order ->
            order.sessionId?.let(checkout::presentSession) ?: checkout.present(order.mdOrder.orEmpty())
        }
        session.attach(presenter)
        onDispose { session.detach(presenter) }
    }
    LaunchedEffect(launch) { session.checkoutReady(launch) }
}

@Composable
private fun ProfileConfirmation(pending: PendingProfile) {
    // A dialog is a window of its own, so the root's resource-id setting does not reach it.
    AlertDialog(
        onDismissRequest = pending::reject,
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        title = { Text("Save the profile ${pending.profile.name}?") },
        text = {
            Column {
                Text(pending.profile.gatewayUrl)
                if (pending.replaces) Text("It replaces the existing profile ${pending.profile.name}.")
            }
        },
        confirmButton = { TextButton(onClick = pending::confirm, modifier = Modifier.testTag(Tags.PROFILE_CONFIRM)) { Text("Save") } },
        dismissButton = { TextButton(onClick = pending::reject, modifier = Modifier.testTag(Tags.PROFILE_REJECT)) { Text("Reject") } },
    )
}

private fun NavHostController.openJournal() {
    navigate(JOURNAL) { launchSingleTop = true }
}

private const val CONFIG = "config"
private const val PROFILES = "profiles"
private const val RESULT = "result"
private const val JOURNAL = "journal"
