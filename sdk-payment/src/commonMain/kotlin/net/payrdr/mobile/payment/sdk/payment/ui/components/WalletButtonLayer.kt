package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize

/**
 * Where a native wallet button stands in its step: the step lays out an empty slot, and the sheet's [WalletButtonLayer]
 * draws the button over it from outside the moving steps.
 *
 * @param alpha how far the step has faded in or out, the one part of its motion its layout does not tell
 * @param settled false while the step moves: the layer follows the slot frame by frame only then
 */
internal class WalletSlot(
    val alpha: State<Float>,
    val settled: State<Boolean>,
    val onClick: () -> Unit,
) {
    /** The slot's layout, read for where the step's zoom has taken it; null until it is placed. */
    var coordinates: LayoutCoordinates? = null

    /** The slot's own (unzoomed) size. */
    var size by mutableStateOf(IntSize.Zero)

    /** Counts the slot's placements, so that the layer follows it when it moves while its step stands. */
    var placements by mutableIntStateOf(0)
}

/**
 * A layer of the sheet, under the steps and out of their transitions, that holds the native wallet button. A UIKit view
 * inside a Compose transition is hidden for the whole of it (Compose Multiplatform 1.7): here the button stays the real
 * one and moves, zooms and fades with its step.
 *
 * @param parking the main button the wallet button waits under, unseen, until a step first shows it; null when the step
 *   on screen has none
 */
internal class WalletButtonLayer(
    val parking: () -> PrimaryButtonSlot? = { null },
) {
    var slot by mutableStateOf<WalletSlot?>(null)
}

/** The sheet's [WalletButtonLayer]; null outside the checkout, where a wallet button draws itself in place. */
internal val LocalWalletButtonLayer = staticCompositionLocalOf<WalletButtonLayer?> { null }

/** How far the step around has faded in or out (1 when it stands): for what draws outside the step, see [WalletSlot]. */
internal val LocalStepAlpha = staticCompositionLocalOf<State<Float>> { mutableStateOf(1f) }

/**
 * Draws [layer]'s button over its slot; [modifier] gives it the whole area the steps take. [enabled] when a step may show
 * the button (its wallet is set up), so that it is ready before. Nothing where the wallet button is not native.
 */
@Composable
internal expect fun WalletButtonLayerHost(
    layer: WalletButtonLayer,
    enabled: Boolean,
    modifier: Modifier,
)
