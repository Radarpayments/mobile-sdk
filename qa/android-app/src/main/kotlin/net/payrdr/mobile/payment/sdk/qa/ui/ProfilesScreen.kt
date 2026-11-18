package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.qa.config.ApiVersion
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile

/**
 * The stored stand profiles. Tapping one selects it for the configuration and opens it in the editor under the list;
 * Add clears the editor for a new one. Profiles also arrive by import, through the configuration screen's menu.
 */
@Composable
internal fun ProfilesScreen(
    session: QaSession,
    profiles: List<StandProfile>,
    onBack: () -> Unit,
    onJournal: () -> Unit,
) {
    val editor = session.editor
    val context = LocalContext.current
    val selected = session.form.config.profile
    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenBar("Profiles", onBack) { JournalButton(onJournal) }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        ) {
            if (profiles.isEmpty()) Text("No profiles yet", style = MaterialTheme.typography.bodyMedium)
            profiles.forEach { profile ->
                ProfileRow(profile, profile.name == selected) {
                    session.select(profile.name)
                    editor.load(profile)
                }
            }
            HorizontalDivider()
            ProfileFields(editor)
            editor.problem?.let { problem ->
                Text(
                    "${problem.field}: ${problem.message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag(Tags.VALIDATION_ERROR),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
                OutlinedButton(onClick = { editor.load(null) }, modifier = Modifier.testTag(Tags.PROFILE_ADD)) { Text("Add") }
                Button(
                    onClick = {
                        editor.valid()?.let { profile ->
                            session.saveProfile(profile)
                            context.say("Profile ${profile.name} saved")
                        }
                    },
                    modifier = Modifier.testTag(Tags.PROFILE_SAVE),
                ) { Text("Save") }
                OutlinedButton(
                    onClick = {
                        session.deleteProfile(editor.name)
                        editor.load(null)
                    },
                    enabled = profiles.any { it.name == editor.name },
                    modifier = Modifier.testTag(Tags.PROFILE_DELETE),
                ) { Text("Delete") }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: StandProfile,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .testTag(Tags.profileSelect(profile.name))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
    ) {
        Text(
            (if (selected) "✓ " else "") + profile.name,
            style = MaterialTheme.typography.titleMedium,
        )
        Text("${profile.gatewayUrl} · ${profile.api.json}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProfileFields(editor: ProfileEditor) {
    Field(Tags.PROFILE_NAME, "Name", editor.name) { editor.name = it }
    Field(Tags.PROFILE_GATEWAY_URL, "Gateway URL", editor.gatewayUrl, KeyboardType.Uri) { editor.gatewayUrl = it }
    Choice(Tags.PROFILE_API, "API", editor.api.json, ApiVersion.entries.map { it.json }) { json ->
        editor.api = ApiVersion.entries.first { it.json == json }
    }
    when (editor.api) {
        ApiVersion.V1 -> {
            Field(Tags.PROFILE_LOGIN, "Login", editor.login) { editor.login = it }
            SecretText(Tags.PROFILE_PASSWORD, "Password", editor.password) { editor.password = it }
        }
        ApiVersion.V2 -> SecretText(Tags.PROFILE_API_KEY, "API key", editor.apiKey) { editor.apiKey = it }
    }
}

@Composable
private fun Field(
    tag: String,
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}
