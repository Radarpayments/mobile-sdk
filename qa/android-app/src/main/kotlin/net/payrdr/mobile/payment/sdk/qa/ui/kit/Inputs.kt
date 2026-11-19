package net.payrdr.mobile.payment.sdk.qa.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * A text input in a [FieldFrame]: monospace, as [look] says, with [trailing] on the right (a unit, Paste, Show). [tag]
 * goes on the editable text itself, so a test reads and types the value through it.
 */
@Composable
internal fun TextInput(
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    isProblem: Boolean = false,
    look: InputLook = InputLook(),
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val colors = qaColors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    FieldFrame(
        focused = focused,
        isProblem = isProblem,
        modifier = modifier,
        minHeight = if (look.singleLine) QaSize.field else MULTILINE_HEIGHT,
    ) {
        Row(
            verticalAlignment = if (look.singleLine) Alignment.CenterVertically else Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(QaSize.screen),
            modifier = Modifier.padding(start = QaSize.screen),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = look.singleLine,
                textStyle = look.style.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.ink),
                interactionSource = interaction,
                visualTransformation = if (look.hidden) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = if (look.hidden) KeyboardType.Password else look.keyboardType),
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(vertical = 14.dp)
                        .testTag(tag),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty() &&
                            look.placeholder.isNotEmpty()
                        ) {
                            QaText(look.placeholder, look.style, color = colors.muted, maxLines = 1)
                        }
                        inner()
                    }
                },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = QaSize.screen - TRAILING_TOUCH_INSET),
                content = trailing,
            )
        }
    }
}

/**
 * The unit inside a field that opens a picker, such as the currency: the code and a small chevron. [tag] goes on it and it
 * reads as the code, so the field's identifier keeps carrying the value.
 */
@Composable
internal fun FieldUnit(
    text: String,
    tag: String,
    onClick: () -> Unit,
    menu: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .height(QaSize.control)
                .widthIn(min = QaSize.control)
                .clickable(role = Role.DropdownList, onClick = onClick)
                .semantics { this.text = AnnotatedString(text) }
                .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QaText(text, QaType.label, modifier = Modifier.clearAndSetSemantics {})
            Chevron(Pointing.DOWN, side = 7.dp, stroke = 1.5.dp, color = qaColors.ink)
        }
        menu()
    }
}

private val MULTILINE_HEIGHT = 104.dp

// A trailing link's touch area is wider than its word; this much of it overlaps the field's inner padding so the word
// keeps the 16 margin from the frame.
private val TRAILING_TOUCH_INSET = 8.dp
