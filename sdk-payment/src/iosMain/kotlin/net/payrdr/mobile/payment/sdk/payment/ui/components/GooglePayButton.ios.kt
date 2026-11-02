package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
public actual fun GooglePayButton(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    // Google Pay is Android-only. On iOS we render nothing.
    Spacer(modifier = modifier)
}
