package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/** A text field of the form: shows the draft while one is pending, and its problem under it. */
@Composable
internal fun FormText(
    form: ConfigForm,
    tag: String,
    label: String,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val problem = form.problem(tag)
    OutlinedTextField(
        value = form.text(tag),
        onValueChange = { form.edit(tag, it) },
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        isError = problem != null,
        supportingText = problem?.let { { Text(it.message) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

@Composable
internal fun FormSwitch(
    form: ConfigForm,
    tag: String,
    label: String,
) {
    LabeledSwitch(tag, label, form.text(tag) == "true") { form.edit(tag, it.toString()) }
}

/** A selector of the form; its text is the JSON value of the field, "" for none. */
@Composable
internal fun FormChoice(
    form: ConfigForm,
    tag: String,
    label: String,
    options: List<String>,
) {
    Choice(tag, label, form.text(tag), options, form.problem(tag)?.message) { form.edit(tag, it) }
}

@Composable
internal fun LabeledSwitch(
    tag: String,
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(tag)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                .padding(vertical = 4.dp),
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** A read-only text field that opens a menu of [options]; an empty option reads "none". */
@Composable
internal fun Choice(
    tag: String,
    label: String,
    value: String,
    options: List<String>,
    problem: String? = null,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(NONE) },
            singleLine = true,
            isError = problem != null,
            supportingText = problem?.let { { Text(it) } },
            trailingIcon = { Text("▾") },
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
        )
        // The overlay takes the taps a read-only field would swallow, and covering the field hides it from accessibility,
        // so the overlay is the element a test finds: tagged, carrying the current value as its text.
        Box(
            Modifier
                .matchParentSize()
                .testTag(tag)
                .semantics { text = AnnotatedString(value) }
                .clickable(role = Role.DropdownList) { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.ifEmpty { NONE }) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/** A titled group of the configuration, expanded until QA collapses it. */
@Composable
internal fun Group(
    tag: String,
    title: String,
    content: @Composable () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag(tag)
                    .clickable { expanded = !expanded }
                    .padding(vertical = 12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(if (expanded) "▴" else "▾")
        }
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 12.dp)) { content() }
        }
    }
}

@Composable
internal fun SecretText(
    tag: String,
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    )
}

private const val NONE = "none"
