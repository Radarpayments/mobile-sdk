package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * The overlays open over a screen (a picker sheet, the camera, a confirmation). While any is, what lies under it is
 * hidden from screen readers: a swipe would otherwise leave the overlay for fields the buyer cannot see.
 */
internal class OverlayCover {
    private var open by mutableIntStateOf(0)

    /** Read in composition: the screen recomposes as overlays open and close. */
    val covering: Boolean get() = open > 0

    fun opened() {
        open++
    }

    fun closed() {
        open--
    }
}

/** The cover of the screen the overlays around are drawn over; null where nothing lies under them. */
internal val LocalOverlayCover = staticCompositionLocalOf<OverlayCover?> { null }

/** Marks the screen under this overlay covered while [visible]. */
@Composable
internal fun CoverWhile(visible: Boolean) {
    val cover = LocalOverlayCover.current ?: return
    DisposableEffect(cover, visible) {
        if (visible) cover.opened()
        onDispose { if (visible) cover.closed() }
    }
}

/** Hidden from screen readers while an overlay covers it, see [OverlayCover]. */
internal fun Modifier.hiddenWhileCovered(cover: OverlayCover?): Modifier = if (cover?.covering == true) clearAndSetSemantics {} else this
