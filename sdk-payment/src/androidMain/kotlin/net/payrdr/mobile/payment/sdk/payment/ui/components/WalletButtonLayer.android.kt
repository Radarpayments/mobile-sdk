package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Android's wallet button (Google Pay) is drawn by Compose and moves with its step: nothing to hold here. */
@Composable
internal actual fun WalletButtonLayerHost(
    layer: WalletButtonLayer,
    enabled: Boolean,
    modifier: Modifier,
) = Unit
