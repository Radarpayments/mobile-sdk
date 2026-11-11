package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Apple Pay has no button on Android: [WalletAvailability.applePay] is always false here, so this is never shown. */
@Suppress("UnusedParameter")
@Composable
internal fun ApplePayButton(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Spacer(modifier = modifier)
}
