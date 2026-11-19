@file:OptIn(ExperimentalComposeUiApi::class)

package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.RectangleShape
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
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaTheme
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaType
import net.payrdr.mobile.payment.sdk.qa.ui.kit.qaColors

/** The app's screens, the checkout that stays composed under them, and the confirmation of an incoming profile. */
@Composable
internal fun QaNavGraph(
    session: QaSession,
    scanner: CardScannerProvider,
) {
    QaTheme {
        Box(Modifier.fillMaxSize().background(qaColors.bg).semantics { testTagsAsResourceId = true }) {
            Column(Modifier.safeDrawingPadding()) {
                if (session.ready) Screens(session)
            }
            session.launched?.let { Checkout(session, scanner, it) }
            session.pending?.let { ProfileConfirmation(it) }
        }
    }
}

/** The routes of the app's screens; [field] opens one text field of the order on a screen of its own. */
internal object Route {
    const val HOME = "home"
    const val ORDER = "order"
    const val THEME = "theme"
    const val BEHAVIOUR = "behaviour"
    const val LOCALE = "locale"
    const val GOOGLE_PAY = "google_pay"
    const val SSL_PINNING = "ssl_pinning"
    const val STANDS = "stands"
    const val STAND = "stand"
    const val IMPORT_TEXT = "import_text"
    const val RESULT = "result"
    const val JOURNAL = "journal"
    const val FIELD = "field/{tag}"
    const val FIELD_TAG = "tag"

    fun field(tag: String): String = "field/$tag"
}

@Composable
private fun Screens(session: QaSession) {
    val nav = rememberNavController()
    val state by session.run.state.collectAsState()
    val profiles by session.profiles.collectAsState()
    val entries by session.journal.entries.collectAsState()
    val back: () -> Unit = { nav.popBackStack() }
    val open: (String) -> Unit = { route -> nav.navigate(route) { launchSingleTop = true } }
    LaunchedEffect(state) {
        // Once per run: a rotation re-runs this effect, and QA may have gone from the result to the journal since.
        val finished = state as? RunState.Finished ?: return@LaunchedEffect
        if (session.shown === finished) return@LaunchedEffect
        session.shown = finished
        if (nav.currentDestination?.route != Route.RESULT) nav.navigate(Route.RESULT) { popUpTo(Route.HOME) }
    }
    NavHost(nav, startDestination = Route.HOME) {
        composable(Route.HOME) { HomeScreen(session, profiles, state, open) }
        composable(Route.ORDER) { OrderScreen(session, profiles, back, open) }
        composable(Route.FIELD) { entry -> FieldScreen(session.form, entry.arguments?.getString(Route.FIELD_TAG).orEmpty(), back) }
        composable(Route.THEME) { ThemeScreen(session.form, back) }
        composable(Route.BEHAVIOUR) { BehaviourScreen(session.form, back) { open(Route.LOCALE) } }
        composable(Route.LOCALE) { LocaleScreen(session.form, back) }
        composable(Route.GOOGLE_PAY) { GooglePayScreen(session.form, back) }
        composable(Route.SSL_PINNING) { SslPinningScreen(session.form, back) }
        composable(Route.STANDS) { StandsScreen(session, profiles, back, { open(Route.STAND) }) { open(Route.IMPORT_TEXT) } }
        composable(Route.STAND) { StandEditorScreen(session, back) }
        composable(Route.IMPORT_TEXT) { ImportTextScreen(session, back) }
        composable(Route.RESULT) {
            when (val last = session.lastRun) {
                null -> LaunchedEffect(Unit) { nav.home() }
                else ->
                    ResultScreen(
                        last.finished,
                        entries,
                        onBack = back,
                        onRunAgain = {
                            // A run launched since is not this screen's to cancel.
                            if (session.run.state.value is RunState.Finished) session.run.reset()
                            nav.home()
                        },
                        onJournal = { open(Route.JOURNAL) },
                    )
            }
        }
        composable(Route.JOURNAL) { JournalScreen(entries, session.journal::clear, back) }
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
    val colors = qaColors
    // A dialog is a window of its own, so the root's resource-id setting does not reach it.
    AlertDialog(
        onDismissRequest = pending::reject,
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        shape = RectangleShape,
        containerColor = colors.bg,
        title = { Text("Save the stand ${pending.profile.name}?", style = QaType.title, color = colors.ink) },
        text = {
            Column {
                Text(pending.profile.gatewayUrl, style = QaType.value, color = colors.muted)
                if (pending.replaces) Text("It replaces the stored stand ${pending.profile.name}.", style = QaType.help, color = colors.ink)
            }
        },
        confirmButton = {
            TextButton(onClick = pending::confirm, modifier = Modifier.testTag(Tags.PROFILE_CONFIRM)) {
                Text("Save", style = QaType.link, color = colors.ink)
            }
        },
        dismissButton = {
            TextButton(onClick = pending::reject, modifier = Modifier.testTag(Tags.PROFILE_REJECT)) {
                Text("Reject", style = QaType.link, color = colors.ink)
            }
        },
    )
}

private fun NavHostController.home() {
    popBackStack(Route.HOME, inclusive = false)
}
