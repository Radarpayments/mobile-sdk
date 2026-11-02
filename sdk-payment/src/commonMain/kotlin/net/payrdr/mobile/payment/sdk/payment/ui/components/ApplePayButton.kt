package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Pay with Apple Pay button. On iOS this wraps `PKPaymentButton`; on Android it is hidden by the
 * parent composable (the `SdkPaymentConfig.applePay == null` check happens upstream).
 *
 * In Plan 3 this is a STUB — tapping does nothing useful on either platform. Plan 4 (platform
 * integrations) wires:
 * - iOS: `PKPaymentRequest` + `PKPaymentAuthorizationViewController` + `applePayProcessForm` call
 * - Android: N/A (the button is hidden)
 *
 * @param onClick fired when the user taps; in Plan 3 this is a no-op hook
 * @param modifier layout modifier (fillMaxWidth + height = 48.dp is the typical setup)
 */
@Composable
public expect fun ApplePayButton(
    onClick: () -> Unit,
    modifier: Modifier,
)
