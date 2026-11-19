package net.payrdr.mobile.payment.sdk.qa.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.SdkPayment
import net.payrdr.mobile.payment.sdk.qa.journal.JournalEntry
import net.payrdr.mobile.payment.sdk.qa.launcher.RunState
import net.payrdr.mobile.payment.sdk.qa.report.AppInfo
import net.payrdr.mobile.payment.sdk.qa.report.runReport
import net.payrdr.mobile.payment.sdk.qa.report.text
import net.payrdr.mobile.payment.sdk.qa.ui.kit.ButtonKind
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Caption
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Footer
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Link
import net.payrdr.mobile.payment.sdk.qa.ui.kit.ListGroup
import net.payrdr.mobile.payment.sdk.qa.ui.kit.ListRow
import net.payrdr.mobile.payment.sdk.qa.ui.kit.NavRow
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaButton
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaScreen
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaSize
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaText
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaType
import net.payrdr.mobile.payment.sdk.qa.ui.kit.RowValue
import net.payrdr.mobile.payment.sdk.qa.ui.kit.TwoLineRow
import net.payrdr.mobile.payment.sdk.qa.ui.kit.ValueText
import net.payrdr.mobile.payment.sdk.qa.ui.kit.qaColors

/**
 * What a finished run came to: the outcome, the ids to copy, what it was launched with and the journal. Rows with
 * nothing to show are left out.
 */
@Composable
internal fun ResultScreen(
    finished: RunState.Finished,
    entries: List<JournalEntry>,
    onBack: () -> Unit,
    onRunAgain: () -> Unit,
    onJournal: () -> Unit,
) {
    val context = LocalContext.current
    val outcome = finished.outcome
    QaScreen(
        title = "Result",
        onBack = onBack,
        footer = {
            Footer {
                QaButton(
                    "Copy report",
                    Tags.REPORT,
                    { context.copyText("Report", runReport(context.appInfo(), finished, entries)) },
                    ButtonKind.SECONDARY,
                )
                QaButton("Run again", Tags.RUN_AGAIN, onRunAgain)
            }
        },
    ) {
        Banner(finished)
        val ids = listOfNotNull(outcome.mdOrder, outcome.sessionId, outcome.error)
        if (ids.isNotEmpty()) {
            Caption("Order")
            ListGroup {
                outcome.mdOrder?.let { CopyRow("mdOrder", it, Tags.RESULT_MD_ORDER, Tags.RESULT_MD_ORDER_COPY) }
                outcome.sessionId?.let { CopyRow("Session id", it, Tags.RESULT_SESSION_ID, Tags.RESULT_SESSION_ID_COPY) }
                outcome.error?.let { error ->
                    TwoLineRow(
                        text = {
                            QaText(outcome.httpStatus?.let { "Error · HTTP $it" } ?: "Error", QaType.body, maxLines = 1)
                            ValueText(error, error, Tags.RESULT_ERROR, QaType.value, qaColors.bad)
                        },
                    )
                }
            }
        }
        Caption("Launched with")
        ListGroup {
            LaunchedRow("Stand", "${finished.profile.name} · API ${finished.profile.api.json}", Tags.RESULT_STAND)
            LaunchedRow("Order", launchedOrderSummary(finished.config.order), Tags.RESULT_ORDER)
            LaunchedRow("Theme", finished.config.sdk.theme.json, Tags.RESULT_THEME)
        }
        Caption("Evidence")
        ListGroup { NavRow("Journal", entriesSummary(entries.size), Tags.JOURNAL_OPEN, onJournal) }
    }
}

/**
 * The outcome in its colour, the only coloured block of the app. The title reads as the contract's id
 * (`qa_result_type`) and the detail as the contract's status text (`qa_result_order_status`).
 */
@Composable
private fun Banner(finished: RunState.Finished) {
    val outcome = finished.outcome
    val onColor = qaColors.onOutcome
    Column(
        Modifier
            .fillMaxWidth()
            .background(toneColor(outcomeTone(outcome.type)))
            .padding(horizontal = QaSize.screen, vertical = 24.dp),
    ) {
        ValueText(outcomeTitle(outcome.type), outcome.type.id, Tags.RESULT_TYPE, QaType.outcome, onColor)
        Spacer(Modifier.height(QaSize.gap))
        ValueText(statusDetail(outcome.status).joinToString("\n"), outcome.status.text(), Tags.RESULT_ORDER_STATUS, QaType.value, onColor)
    }
}

@Composable
private fun CopyRow(
    label: String,
    value: String,
    tag: String,
    copyTag: String,
) {
    val context = LocalContext.current
    TwoLineRow(
        text = {
            QaText(label, QaType.body, maxLines = 1)
            ValueText(value, value, tag, QaType.value, qaColors.muted)
        },
        trailing = { Link("Copy", copyTag, { context.copyText(label, value) }) },
    )
}

@Composable
private fun LaunchedRow(
    label: String,
    value: String,
    tag: String,
) {
    ListRow {
        QaText(label, QaType.body, maxLines = 1)
        RowValue(value, tag = tag)
    }
}

private fun Context.appInfo(): AppInfo {
    val info = packageManager.getPackageInfo(packageName, 0)
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else versionCodeBefore28(info)
    return AppInfo(
        appVersion = "${info.versionName} ($code)",
        sdkVersion = SdkPayment.VERSION,
        device = "${Build.MANUFACTURER} ${Build.MODEL}",
        os = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    )
}

@Suppress("DEPRECATION")
private fun versionCodeBefore28(info: android.content.pm.PackageInfo): Long = info.versionCode.toLong()
