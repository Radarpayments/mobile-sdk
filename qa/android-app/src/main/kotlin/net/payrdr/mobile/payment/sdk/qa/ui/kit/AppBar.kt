package net.payrdr.mobile.payment.sdk.qa.ui.kit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A screen's top bar, 56 tall over a line: back, the title, the actions. The back and the actions reach the screen's edge
 * so that their touch areas do, while what they draw keeps the 16 dp margin.
 */
@Composable
internal fun AppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(QaSize.appBar).bottomLine(),
    ) {
        if (onBack != null) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier =
                    Modifier
                        .width(BACK_WIDTH)
                        .fillMaxHeight()
                        .clickable(role = Role.Button, onClick = onBack)
                        .semantics { contentDescription = "Back" }
                        .padding(start = QaSize.screen),
            ) {
                Chevron(Pointing.LEFT, side = 10.dp, stroke = 2.dp, color = qaColors.ink)
            }
        } else {
            Spacer(Modifier.width(QaSize.screen))
        }
        QaText(title, QaType.title, maxLines = 1, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = ACTIONS_END), content = actions)
    }
}

/** A word in the top bar that acts, such as Copy or Add. */
@Composable
internal fun BarAction(
    text: String,
    tag: String,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .height(QaSize.appBar)
                .widthIn(min = QaSize.control)
                .clickable(role = Role.Button, onClick = onClick)
                .testTag(tag)
                .padding(horizontal = ACTION_PADDING),
    ) {
        QaText(text, QaType.action)
    }
}

/** The ⋮ that opens a menu; [menu] is placed in the same box so that the menu drops from it. */
@Composable
internal fun BarMenu(
    tag: String,
    onClick: () -> Unit,
    menu: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier =
            Modifier
                .size(QaSize.control, QaSize.appBar)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "Menu" }
                .testTag(tag)
                .padding(end = ACTION_PADDING),
    ) {
        MenuDots()
        menu()
    }
}

private val BACK_WIDTH = 44.dp
private val ACTION_PADDING = 8.dp
private val ACTIONS_END = 8.dp
