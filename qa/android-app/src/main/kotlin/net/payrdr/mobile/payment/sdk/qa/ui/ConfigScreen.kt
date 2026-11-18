package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.qa.config.Problem
import net.payrdr.mobile.payment.sdk.qa.config.QaConfigJson
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.launchProblem
import net.payrdr.mobile.payment.sdk.qa.intake.QrScan
import net.payrdr.mobile.payment.sdk.qa.intake.intakeFromText
import net.payrdr.mobile.payment.sdk.qa.intake.scanQr
import net.payrdr.mobile.payment.sdk.qa.launcher.RunState

/** The start screen: the stand profile, the five groups of the configuration, and Launch pinned under them. */
@Composable
internal fun ConfigScreen(
    session: QaSession,
    profiles: List<StandProfile>,
    state: RunState,
    onProfiles: () -> Unit,
    onJournal: () -> Unit,
) {
    val form = session.form
    val selected = profiles.find { it.name == form.config.profile }
    val profileProblem = launchProblem(form.config, selected)?.takeIf { it.field == PROFILE_FIELD }
    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenBar("PAYRDR QA") {
            TextButton(onClick = onProfiles, modifier = Modifier.testTag(Tags.PROFILES_OPEN)) { Text("Profiles") }
            JournalButton(onJournal)
            ConfigMenu(session)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            Choice(
                tag = Tags.PROFILE,
                label = "Stand profile",
                value = form.text(Tags.PROFILE),
                options = profiles.map { it.name },
                problem = profileProblem?.message,
                onSelect = session::select,
            )
            OrderGroup(form)
            SdkGroup(form)
            ThemeGroup(form)
            WalletsGroup(form)
            SecurityGroup(form)
        }
        HorizontalDivider()
        LaunchBar(
            problem = form.problem ?: session.notice ?: (state as? RunState.Invalid)?.problem,
            busy = state.isBusy(),
            enabled = profileProblem == null && !state.isBusy(),
            onLaunch = session::launch,
        )
    }
}

@Composable
private fun LaunchBar(
    problem: Problem?,
    busy: Boolean,
    enabled: Boolean,
    onLaunch: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (problem != null) {
            Text(
                "${problem.field}: ${problem.message}",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(Tags.VALIDATION_ERROR),
            )
        }
        Button(onClick = onLaunch, enabled = enabled, modifier = Modifier.fillMaxWidth().testTag(Tags.LAUNCH)) {
            Text(if (busy) "Running…" else "Launch")
        }
    }
}

@Composable
private fun ConfigMenu(session: QaSession) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, modifier = Modifier.testTag(Tags.MENU)) { Text("⋮") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuItem(Tags.IMPORT, "Import from clipboard") {
                expanded = false
                session.accept(intakeFromText(context.clipboardText()))
            }
            MenuItem(Tags.IMPORT_QR, "Import from QR code") {
                expanded = false
                scope.launch {
                    when (val scan = scanQr(context)) {
                        is QrScan.Text -> session.accept(intakeFromText(scan.text))
                        QrScan.Cancelled -> Unit
                        QrScan.Unavailable -> context.say("The QR scanner is not available: Google Play services cannot provide it")
                    }
                }
            }
            MenuItem(Tags.EXPORT, "Export to clipboard") {
                expanded = false
                context.copyText("Configuration", QaConfigJson.encode(session.form.config))
            }
            MenuItem(Tags.RESET, "Reset") {
                expanded = false
                session.reset()
            }
        }
    }
}

@Composable
private fun MenuItem(
    tag: String,
    text: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(text = { Text(text) }, onClick = onClick, modifier = Modifier.testTag(tag))
}

private const val PROFILE_FIELD = "profile"
