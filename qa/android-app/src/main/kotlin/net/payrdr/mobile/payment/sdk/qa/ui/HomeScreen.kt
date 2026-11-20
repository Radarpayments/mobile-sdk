package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.qa.config.OrderSource
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.QaConfigJson
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.launchProblem
import net.payrdr.mobile.payment.sdk.qa.config.launchProblems
import net.payrdr.mobile.payment.sdk.qa.intake.QrScan
import net.payrdr.mobile.payment.sdk.qa.intake.intakeFromText
import net.payrdr.mobile.payment.sdk.qa.intake.scanQr
import net.payrdr.mobile.payment.sdk.qa.launcher.RunState
import net.payrdr.mobile.payment.sdk.qa.ui.kit.BarMenu
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Caption
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Footer
import net.payrdr.mobile.payment.sdk.qa.ui.kit.FooterWhy
import net.payrdr.mobile.payment.sdk.qa.ui.kit.ListGroup
import net.payrdr.mobile.payment.sdk.qa.ui.kit.NavRow
import net.payrdr.mobile.payment.sdk.qa.ui.kit.OutcomeSquare
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaButton
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaMenu
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaMenuItem
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaScreen
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaText
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaType
import net.payrdr.mobile.payment.sdk.qa.ui.kit.RowChevron
import net.payrdr.mobile.payment.sdk.qa.ui.kit.SecondLine
import net.payrdr.mobile.payment.sdk.qa.ui.kit.TwoLineRow
import net.payrdr.mobile.payment.sdk.qa.ui.kit.ValueText
import net.payrdr.mobile.payment.sdk.qa.ui.kit.qaColors

/**
 * The start screen: what a launch would run and where, one row per part of the configuration, each opening the screen
 * that edits it. A problem shows on the row whose screen fixes it, and Launch stays off while there is one.
 */
@Composable
internal fun HomeScreen(
    session: QaSession,
    profiles: List<StandProfile>,
    state: RunState,
    open: (String) -> Unit,
) {
    val form = session.form
    val config = form.config
    val profile = profiles.find { it.name == config.profile }
    val launchCheck = launchProblem(config, profile)
    val problems = listOfNotNull(form.problem) + launchProblems(config, profile)
    val problemOf = { row: HomeRow -> problems.firstOrNull { homeRow(it.field) == row } }
    val why = form.problem ?: session.notice ?: (state as? RunState.Invalid)?.problem ?: launchCheck
    val busy = state.isBusy()
    QaScreen(
        title = "QA Sample App",
        actions = { HomeMenu(session, open) },
        footer = {
            Footer(why = { why?.let { FooterWhy("${it.field}: ${it.message}", Modifier.testTag(Tags.VALIDATION_ERROR)) } }) {
                QaButton(
                    text = if (busy) "Running…" else "Launch",
                    tag = Tags.LAUNCH,
                    onClick = session::launch,
                    enabled = form.problem == null && launchCheck == null && !busy,
                )
            }
        },
    ) {
        Caption("Stand")
        ListGroup { StandRow(form.text(Tags.PROFILE), profile, problemOf(HomeRow.STAND)) { open(Route.STANDS) } }
        Caption("Order")
        ListGroup {
            SummaryRow("Order", orderSummary(config.order), Tags.GROUP_ORDER, problemOf(HomeRow.ORDER)) { open(Route.ORDER) }
            if (config.order.source == OrderSource.REGISTER) {
                SummaryRow("Amount", amountSummary(config.order), Tags.HOME_AMOUNT, problemOf(HomeRow.AMOUNT)) { open(Route.ORDER) }
                SummaryRow("Features", featuresSummary(config.order), Tags.HOME_FEATURES, problemOf(HomeRow.FEATURES)) {
                    open(Route.ORDER)
                }
            }
        }
        Caption("Checkout")
        ListGroup {
            SummaryRow("Theme", config.sdk.theme.json, Tags.GROUP_THEME, problemOf(HomeRow.THEME)) { open(Route.THEME) }
            SummaryRow("Behaviour", behaviourSummary(config.sdk), Tags.GROUP_SDK, problemOf(HomeRow.BEHAVIOUR)) { open(Route.BEHAVIOUR) }
            SummaryRow("Google Pay", googlePaySummary(config.sdk), Tags.GROUP_WALLETS, problemOf(HomeRow.GOOGLE_PAY)) {
                open(Route.GOOGLE_PAY)
            }
            SummaryRow("SSL pinning", sslPinningSummary(config.sdk), Tags.GROUP_SECURITY, problemOf(HomeRow.SSL_PINNING)) {
                open(Route.SSL_PINNING)
            }
        }
        session.lastRun?.let { last ->
            Caption("Last run")
            ListGroup {
                TwoLineRow(
                    modifier = Modifier.clickable(role = Role.Button) { open(Route.RESULT) }.testTag(Tags.HOME_LAST_RUN),
                    text = {
                        val type = last.finished.outcome.type
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutcomeSquare(toneColor(outcomeTone(type)))
                            Spacer(Modifier.width(10.dp))
                            QaText(outcomeTitle(type), QaType.bodyStrong, maxLines = 1)
                        }
                        SecondLine(lastRunSummary(last.timeMillis, last.finished.outcome.status))
                    },
                    trailing = { RowChevron() },
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    summary: String,
    tag: String,
    problem: Problem?,
    onClick: () -> Unit,
) {
    NavRow(label, problem?.let(::rowProblem) ?: summary, tag, onClick, valueIsProblem = problem != null)
}

/** The stand: its name, which `qa_profile` reads as the configuration's value, over its host and API or its problem. */
@Composable
private fun StandRow(
    name: String,
    profile: StandProfile?,
    problem: Problem?,
    onClick: () -> Unit,
) {
    TwoLineRow(
        modifier = Modifier.clickable(role = Role.Button, onClick = onClick).testTag(Tags.PROFILES_OPEN),
        text = {
            ValueText(name.ifEmpty { "No stand" }, name, Tags.PROFILE, QaType.bodyStrong, maxLines = 1)
            when {
                problem != null -> SecondLine(rowProblem(problem), color = qaColors.bad)
                profile != null -> SecondLine(standSummary(profile))
            }
        },
        trailing = { RowChevron() },
    )
}

@Composable
private fun HomeMenu(
    session: QaSession,
    open: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    val chosen = { action: () -> Unit ->
        expanded = false
        action()
    }
    BarMenu(Tags.MENU, onClick = { expanded = true }) {
        QaMenu(expanded, onDismiss = { expanded = false }) {
            QaMenuItem("Import from clipboard", Tags.IMPORT, { chosen { session.accept(intakeFromText(context.clipboardText())) } })
            QaMenuItem("Import from text", Tags.IMPORT_TEXT, {
                chosen {
                    session.textImport.clear()
                    open(Route.IMPORT_TEXT)
                }
            })
            QaMenuItem("Import from QR code", Tags.IMPORT_QR, {
                chosen {
                    scope.launch {
                        when (val scan = scanQr(context)) {
                            is QrScan.Text -> session.accept(intakeFromText(scan.text))
                            QrScan.Cancelled -> Unit
                            QrScan.Unavailable -> context.say(QR_UNAVAILABLE)
                        }
                    }
                }
            })
            QaMenuItem("Export to clipboard", Tags.EXPORT, {
                chosen { context.copyText("Configuration", QaConfigJson.encode(session.form.config)) }
            })
            QaMenuItem("Reset", Tags.RESET, { chosen(session::reset) })
            QaMenuItem("Journal", Tags.JOURNAL_OPEN, { chosen { open(Route.JOURNAL) } })
        }
    }
}

/** The colour of an outcome: green, red, or grey for one that is neither. */
@Composable
internal fun toneColor(tone: Tone): Color =
    when (tone) {
        Tone.OK -> qaColors.ok
        Tone.BAD -> qaColors.bad
        Tone.NEUTRAL -> qaColors.muted
    }

internal const val QR_UNAVAILABLE = "The QR scanner is not available: Google Play services cannot provide it"
