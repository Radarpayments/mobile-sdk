package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Pay with Apple Pay button. On iOS this wraps `PKPaymentButton`; on Android it is hidden by the
 * parent composable (the `SdkPaymentConfig.applePay == null` check happens upstream).
 */
@Composable
internal expect fun ApplePayButton(
    onClick: () -> Unit,
    modifier: Modifier,
)
