package net.payrdr.mobile.payment.sdk.qa.ui.kit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A whole screen: the app bar, the content scrolling under it with 24 at its end, and the [footer] fixed at the bottom
 * above the keyboard. With [scroll] off the content gets the height left, for a screen that lays out a list of its own.
 */
@Composable
internal fun QaScreen(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    footer: (@Composable () -> Unit)? = null,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize().background(qaColors.bg).imePadding()) {
        AppBar(title, onBack, actions)
        val body = Modifier.weight(1f).fillMaxWidth()
        if (scroll) {
            Column(body.verticalScroll(rememberScrollState())) {
                content()
                Spacer(Modifier.height(24.dp))
            }
        } else {
            Column(body, content = content)
        }
        footer?.invoke()
    }
}
