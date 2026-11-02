package net.payrdr.mobile.payment.samples.android.prefs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * Composable helper returning a live [AppPrefs.Snapshot]. Re-reads DataStore on process resume
 * and on any `AppPrefs.setXxx(...)` call elsewhere.
 */
@Composable
fun rememberPrefs(): Pair<State<AppPrefs.Snapshot?>, AppPrefs> {
    val ctx = LocalContext.current
    val prefs = remember(ctx) { AppPrefs(ctx) }
    val state = prefs.flow.collectAsState(initial = null)
    return state to prefs
}

/**
 * Text field value persisted to DataStore. The field edits a local copy and every change is saved in
 * the background: binding the field straight to [AppPrefs.flow] makes each keystroke wait for a DataStore
 * round trip, so fast typing loses characters and the cursor jumps.
 *
 * @param persisted the value loaded from DataStore, used only as the initial text.
 * @return the current text and the change handler to pass to the field.
 */
@Composable
fun rememberPersistedText(
    persisted: String,
    persist: suspend (String) -> Unit,
): Pair<String, (String) -> Unit> {
    val scope = rememberCoroutineScope()
    var text by rememberSaveable { mutableStateOf(persisted) }
    return text to { value ->
        text = value
        scope.launch { persist(value) }
    }
}
