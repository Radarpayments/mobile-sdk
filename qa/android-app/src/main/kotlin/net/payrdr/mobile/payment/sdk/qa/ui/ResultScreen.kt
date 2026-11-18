package net.payrdr.mobile.payment.sdk.qa.ui

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.SdkPayment
import net.payrdr.mobile.payment.sdk.qa.journal.JournalEntry
import net.payrdr.mobile.payment.sdk.qa.launcher.RunState
import net.payrdr.mobile.payment.sdk.qa.report.AppInfo
import net.payrdr.mobile.payment.sdk.qa.report.runReport
import net.payrdr.mobile.payment.sdk.qa.report.text

/** What a finished run came to: the checkout's result, the order's status, and the report of the run. */
@Composable
internal fun ResultScreen(
    finished: RunState.Finished,
    entries: List<JournalEntry>,
    onRunAgain: () -> Unit,
    onJournal: () -> Unit,
) {
    val context = LocalContext.current
    val outcome = finished.outcome
    Column(Modifier.fillMaxSize()) {
        ScreenBar("Result", onRunAgain) { JournalButton(onJournal) }
        SelectionContainer(Modifier.weight(1f)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            ) {
                Value("Result", outcome.type.id, Tags.RESULT_TYPE)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Value("mdOrder", outcome.mdOrder ?: MISSING, Tags.RESULT_MD_ORDER) }
                    TextButton(
                        onClick = { outcome.mdOrder?.let { context.copyText("mdOrder", it) } },
                        enabled = outcome.mdOrder != null,
                        modifier = Modifier.testTag(Tags.RESULT_MD_ORDER_COPY),
                    ) { Text("Copy") }
                }
                Value("Session id", outcome.sessionId ?: MISSING, Tags.RESULT_SESSION_ID)
                Value("Order status", outcome.status.text(), Tags.RESULT_ORDER_STATUS)
                Value("Error", outcome.error ?: MISSING, Tags.RESULT_ERROR)
                Value("HTTP status", outcome.httpStatus?.toString() ?: MISSING)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            OutlinedButton(
                onClick = { context.copyText("Report", runReport(context.appInfo(), finished, entries)) },
                modifier = Modifier.weight(1f).testTag(Tags.REPORT),
            ) { Text("Copy report") }
            Button(onClick = onRunAgain, modifier = Modifier.weight(1f).testTag(Tags.RUN_AGAIN)) { Text("Run again") }
        }
    }
}

@Composable
private fun Value(
    label: String,
    value: String,
    tag: String? = null,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = if (tag != null) Modifier.testTag(tag) else Modifier)
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

private const val MISSING = "-"
