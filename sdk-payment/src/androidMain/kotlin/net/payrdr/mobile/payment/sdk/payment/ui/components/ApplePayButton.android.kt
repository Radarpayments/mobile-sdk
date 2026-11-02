package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
public actual fun ApplePayButton(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    // Apple Pay is iOS-only. On Android we render nothing.
    Spacer(modifier = modifier)
}
