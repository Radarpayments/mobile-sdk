package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.sdk.qa.config.ApiVersion
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.intake.QrScan
import net.payrdr.mobile.payment.sdk.qa.intake.intakeFromText
import net.payrdr.mobile.payment.sdk.qa.intake.scanQr
import net.payrdr.mobile.payment.sdk.qa.ui.kit.BarAction
import net.payrdr.mobile.payment.sdk.qa.ui.kit.ButtonKind
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Caption
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Footer
import net.payrdr.mobile.payment.sdk.qa.ui.kit.FooterWhy
import net.payrdr.mobile.payment.sdk.qa.ui.kit.InputLook
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Link
import net.payrdr.mobile.payment.sdk.qa.ui.kit.ListGroup
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Mark
import net.payrdr.mobile.payment.sdk.qa.ui.kit.NavRow
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Note
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaButton
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaScreen
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaText
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaType
import net.payrdr.mobile.payment.sdk.qa.ui.kit.SecondLine
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Segment
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Segmented
import net.payrdr.mobile.payment.sdk.qa.ui.kit.TextInput
import net.payrdr.mobile.payment.sdk.qa.ui.kit.TwoLineRow

/**
 * The stored stands. The mark chooses the one the configuration uses, Edit opens it in the editor; a stand is added by
 * hand, from the clipboard or from a QR code, and an imported one is confirmed before it is stored.
 */
@Composable
internal fun StandsScreen(
    session: QaSession,
    profiles: List<StandProfile>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val selected = session.form.config.profile
    val edit = { profile: StandProfile? ->
        session.editor.load(profile)
        onEdit()
    }
    QaScreen("Stands", onBack, actions = { BarAction("Add", Tags.PROFILE_ADD) { edit(null) } }) {
        Caption("Saved")
        if (profiles.isEmpty()) {
            Note("No stands yet")
        } else {
            ListGroup {
                profiles.forEach { profile ->
                    StandRow(profile, profile.name == selected, onSelect = { session.select(profile.name) }, onEdit = { edit(profile) })
                }
            }
        }
        Caption("Add a stand")
        ListGroup {
            NavRow("Enter by hand", "", Tags.PROFILE_ENTER, { edit(null) })
            NavRow("Paste from clipboard", "", Tags.PROFILE_PASTE, { session.accept(intakeFromText(context.clipboardText())) })
            NavRow("Scan a QR code", "", Tags.PROFILE_SCAN, {
                scope.launch {
                    when (val scan = scanQr(context)) {
                        is QrScan.Text -> session.accept(intakeFromText(scan.text))
                        QrScan.Cancelled -> Unit
                        QrScan.Unavailable -> context.say(QR_UNAVAILABLE)
                    }
                }
            })
        }
    }
}

@Composable
private fun StandRow(
    profile: StandProfile,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
) {
    TwoLineRow(
        modifier = Modifier.selectable(selected, role = Role.RadioButton, onClick = onSelect).testTag(Tags.profileSelect(profile.name)),
        leading = { Mark(selected) },
        text = {
            QaText(profile.name, if (selected) QaType.bodyStrong else QaType.body, maxLines = 1)
            SecondLine(standSummary(profile))
        },
        trailing = { Link("Edit", Tags.profileEdit(profile.name), onEdit) },
    )
}

/** One stand: its name, gateway and API, and the login and password of v1 or the key of v2. */
@Composable
internal fun StandEditorScreen(
    session: QaSession,
    onBack: () -> Unit,
) {
    val editor = session.editor
    val context = LocalContext.current
    QaScreen(
        title = if (editor.original == null) "New stand" else "Stand",
        onBack = onBack,
        footer = {
            Footer(why = { editor.problem?.let { FooterWhy("${it.field}: ${it.message}", Modifier.testTag(Tags.VALIDATION_ERROR)) } }) {
                editor.original?.let { original ->
                    QaButton("Delete", Tags.PROFILE_DELETE, { session.deleteProfile(original, onBack) }, ButtonKind.SECONDARY)
                }
                QaButton("Save", Tags.PROFILE_SAVE, {
                    editor.valid()?.let { profile ->
                        session.saveProfile(profile) {
                            context.say("Stand ${profile.name} saved")
                            onBack()
                        }
                    }
                })
            }
        },
    ) {
        Caption("Name")
        TextInput(editor.name, { editor.name = it }, Tags.PROFILE_NAME)
        Caption("Gateway URL")
        TextInput(editor.gatewayUrl, {
            editor.gatewayUrl = it
        }, Tags.PROFILE_GATEWAY_URL, look = InputLook(keyboardType = KeyboardType.Uri, style = QaType.inputSmall))
        Caption("API")
        Segmented(
            segments = ApiVersion.entries.map { Segment(it.json, it.json, Tags.option(Tags.PROFILE_API, it.json)) },
            selected = editor.api.json,
            tag = Tags.PROFILE_API,
            onSelect = { json -> editor.api = ApiVersion.entries.first { it.json == json } },
        )
        when (editor.api) {
            ApiVersion.V1 -> {
                Caption("Login")
                TextInput(editor.login, { editor.login = it }, Tags.PROFILE_LOGIN)
                Caption("Password")
                Secret(editor.password, { editor.password = it }, Tags.PROFILE_PASSWORD, Tags.PROFILE_PASSWORD_SHOW)
            }
            ApiVersion.V2 -> {
                Caption("API key")
                Secret(editor.apiKey, { editor.apiKey = it }, Tags.PROFILE_API_KEY, Tags.PROFILE_API_KEY_SHOW)
            }
        }
    }
}

/** A secret field, hidden until Show; it is shown on this screen only and hidden again when the screen is left. */
@Composable
private fun Secret(
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
    showTag: String,
) {
    // Not rememberSaveable: whether a secret is visible is not worth writing into the saved state.
    var shown by remember { mutableStateOf(false) }
    TextInput(value, onValueChange, tag, look = InputLook(hidden = !shown, keyboardType = KeyboardType.Password)) {
        Link(if (shown) "Hide" else "Show", showTag, { shown = !shown })
    }
}
