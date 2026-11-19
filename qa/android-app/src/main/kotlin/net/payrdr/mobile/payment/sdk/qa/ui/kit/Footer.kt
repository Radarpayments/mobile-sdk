package net.payrdr.mobile.payment.sdk.qa.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** The bottom of a screen under a line: an optional reason in red, then the screen's buttons. */
@Composable
internal fun Footer(
    why: @Composable ColumnScope.() -> Unit = {},
    buttons: @Composable RowScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(QaSize.gap),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(qaColors.bg)
                .topLine()
                .padding(start = QaSize.screen, end = QaSize.screen, top = 12.dp, bottom = QaSize.screen),
    ) {
        why()
        Row(horizontalArrangement = Arrangement.spacedBy(QaSize.gap), modifier = Modifier.fillMaxWidth(), content = buttons)
    }
}

/** The reason a footer's button is off, or the problem of the last attempt. */
@Composable
internal fun FooterWhy(
    text: String,
    modifier: Modifier = Modifier,
) {
    QaText(text, QaType.help, color = qaColors.bad, modifier = modifier.fillMaxWidth())
}

internal enum class ButtonKind { PRIMARY, SECONDARY }

/** A footer button, 52 tall: ink on the primary one, an ink frame on the secondary one, grey while off. */
@Composable
internal fun RowScope.QaButton(
    text: String,
    tag: String,
    onClick: () -> Unit,
    kind: ButtonKind = ButtonKind.PRIMARY,
    enabled: Boolean = true,
) {
    val colors = qaColors
    val background =
        when {
            !enabled -> colors.line
            kind == ButtonKind.PRIMARY -> colors.ink
            else -> Color.Transparent
        }
    val content =
        when {
            !enabled -> colors.muted
            kind == ButtonKind.PRIMARY -> colors.bg
            else -> colors.ink
        }
    var modifier =
        Modifier
            .weight(1f)
            .height(QaSize.button)
            .background(background)
    if (enabled && kind == ButtonKind.SECONDARY) modifier = modifier.border(QaSize.line, colors.ink)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag(tag),
    ) {
        QaText(text, QaType.button, color = content, maxLines = 1)
    }
}
