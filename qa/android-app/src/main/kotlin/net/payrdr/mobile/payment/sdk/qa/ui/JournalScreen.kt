package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.logger.LogLevel
import net.payrdr.mobile.payment.sdk.qa.journal.ClockPrecision
import net.payrdr.mobile.payment.sdk.qa.journal.JournalEntry
import net.payrdr.mobile.payment.sdk.qa.journal.line
import net.payrdr.mobile.payment.sdk.qa.journal.timeOfDay
import net.payrdr.mobile.payment.sdk.qa.ui.kit.BarAction
import net.payrdr.mobile.payment.sdk.qa.ui.kit.InputLook
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Note
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaScreen
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaSize
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaText
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaType
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Segment
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Segmented
import net.payrdr.mobile.payment.sdk.qa.ui.kit.TextInput
import net.payrdr.mobile.payment.sdk.qa.ui.kit.bottomLine
import net.payrdr.mobile.payment.sdk.qa.ui.kit.qaColors

/**
 * The journal of the current run, oldest first: a gateway request and its answer as one item that opens to the bodies,
 * every other entry as a line. `qa_journal_text` reads as the shown entries in the journal's line format, so the
 * pairing on screen does not change what automation reads or what Copy puts on the clipboard.
 */
@Composable
internal fun JournalScreen(
    entries: List<JournalEntry>,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(JournalFilter.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    val items = remember(entries) { journalItems(entries) }
    val shown = items.shown(filter, query)
    val text = shown.flatMap { it.entries }.joinToString("\n") { it.line() }
    QaScreen(
        title = "Journal",
        onBack = onBack,
        actions = {
            BarAction("Copy", Tags.JOURNAL_COPY) { context.copyText("Journal", text) }
            BarAction("Clear", Tags.JOURNAL_CLEAR, onClear)
        },
        scroll = false,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().bottomLine().padding(vertical = 12.dp),
        ) {
            TextInput(query, { query = it }, Tags.JOURNAL_SEARCH, look = InputLook(placeholder = "Search", style = QaType.inputSmall))
            Segmented(
                segments = JournalFilter.entries.map { Segment(it.id, it.label, Tags.journalFilter(it.id)) },
                selected = filter.id,
                tag = Tags.JOURNAL_LEVEL,
                onSelect = { id -> filter = JournalFilter.entries.first { it.id == id } },
            )
        }
        SelectionContainer(Modifier.weight(1f)) {
            LazyColumn(Modifier.fillMaxWidth().semantics { this.text = AnnotatedString(text) }.testTag(Tags.JOURNAL_TEXT)) {
                if (shown.isEmpty()) item { Note(if (entries.isEmpty()) "The journal is empty" else "Nothing matches") }
                items(shown) { item ->
                    when (item) {
                        is JournalItem.Exchange -> ExchangeItem(item)
                        is JournalItem.Line -> LineItem(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExchangeItem(item: JournalItem.Exchange) {
    var open by rememberSaveable { mutableStateOf(false) }
    val statusColor = if (item.failed) qaColors.bad else qaColors.ink
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { open = !open }
            .bottomLine()
            .padding(horizontal = QaSize.screen, vertical = 12.dp),
    ) {
        QaText(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(item.method) }
                append(" ")
                append(item.path)
            },
            QaType.journalHead,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) {
            QaText(
                "${timeOfDay(item.request.timeMillis, precision = ClockPrecision.SECONDS)} · ${item.host}",
                QaType.journal,
                color = qaColors.muted,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            QaText(item.outcome, QaType.journal, color = statusColor, maxLines = 1)
        }
        if (open) {
            val body =
                listOfNotNull("→$NBSP${item.requestDetail.trim()}", item.responseDetail?.let { "←$NBSP$it" }).joinToString("\n\n")
            QaText(
                body,
                QaType.journal,
                modifier =
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .background(qaColors.fill)
                        .padding(12.dp),
            )
        }
    }
}

@Composable
private fun LineItem(item: JournalItem.Line) {
    val entry = item.entry
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().bottomLine().padding(horizontal = QaSize.screen, vertical = 8.dp),
    ) {
        QaText(
            timeOfDay(entry.timeMillis, precision = ClockPrecision.SECONDS),
            QaType.journal,
            color = qaColors.muted,
            modifier = Modifier.width(60.dp),
        )
        QaText(item.source, QaType.journal, color = qaColors.muted, modifier = Modifier.width(28.dp))
        QaText(entry.message, QaType.journal, color = if (entry.isProblem()) qaColors.bad else qaColors.ink, modifier = Modifier.weight(1f))
    }
}

private fun JournalEntry.isProblem(): Boolean = level >= LogLevel.WARNING

// Keeps the arrow on the line of a long unbroken body, which then wraps by character as in the mockup.
private const val NBSP = '\u00A0'
