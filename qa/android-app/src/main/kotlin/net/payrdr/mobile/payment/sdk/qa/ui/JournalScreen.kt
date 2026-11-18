package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.logger.LogLevel
import net.payrdr.mobile.payment.sdk.qa.journal.Journal
import net.payrdr.mobile.payment.sdk.qa.journal.JournalEntry
import net.payrdr.mobile.payment.sdk.qa.journal.filtered
import net.payrdr.mobile.payment.sdk.qa.journal.line

/** The SDK's log and the merchant's gateway exchanges of the current run, as one selectable text. */
@Composable
internal fun JournalScreen(
    journal: Journal,
    entries: List<JournalEntry>,
    onBack: () -> Unit,
) {
    var level by rememberSaveable { mutableStateOf(LogLevel.DEBUG) }
    var query by rememberSaveable { mutableStateOf("") }
    val text = entries.filtered(level, query).joinToString("\n") { it.line() }
    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenBar("Journal", onBack) {
            TextButton(onClick = journal::clear, modifier = Modifier.testTag(Tags.JOURNAL_CLEAR)) { Text("Clear") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Column(Modifier.weight(2f)) {
                Choice(Tags.JOURNAL_LEVEL, "Level", level.name, LogLevel.entries.map { it.name }) { name ->
                    level = LogLevel.valueOf(name)
                }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.weight(3f).testTag(Tags.JOURNAL_SEARCH),
            )
        }
        SelectionContainer(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.testTag(Tags.JOURNAL_TEXT),
            )
        }
    }
}
