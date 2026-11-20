@file:OptIn(ExperimentalComposeUiApi::class)

package net.payrdr.mobile.payment.sdk.qa.ui.kit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp

/** A menu in the app's look: square, a 1 dp frame, no shadow. */
@Composable
internal fun QaMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = qaColors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RectangleShape,
        containerColor = colors.bg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(QaSize.line, colors.line),
        // A menu is a window of its own, so the root's resource-id setting does not reach its items.
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        content = content,
    )
}

/** An item of a [QaMenu]; a chosen one of a choice shows its mark. */
@Composable
internal fun QaMenuItem(
    text: String,
    tag: String,
    onClick: () -> Unit,
    selected: Boolean? = null,
) {
    DropdownMenuItem(
        text = { QaText(text, if (selected == true) QaType.bodyStrong else QaType.body, maxLines = 1) },
        leadingIcon = selected?.let { { Mark(it) } },
        onClick = onClick,
        modifier = Modifier.testTag(tag),
    )
}
