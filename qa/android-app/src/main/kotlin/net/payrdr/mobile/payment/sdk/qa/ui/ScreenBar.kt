package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** The title row of a screen: back when there is somewhere to go back to, the title, the screen's actions. */
@Composable
internal fun ScreenBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        if (onBack != null) TextButton(onClick = onBack) { Text("‹ Back") }
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        actions()
    }
}

/** The way to the journal, on every screen that is not the journal. */
@Composable
internal fun JournalButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.testTag(Tags.JOURNAL_OPEN)) { Text("Journal") }
}
