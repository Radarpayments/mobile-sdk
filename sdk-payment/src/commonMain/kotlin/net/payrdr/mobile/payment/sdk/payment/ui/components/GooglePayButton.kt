package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Pay with Google Pay button. On Android this will eventually wrap `PayButton` from
 * `com.google.android.gms.wallet.button`; on iOS it is hidden by the parent composable (the
 * `SdkPaymentConfig.googlePay == null` check happens upstream).
 *
 * In Plan 3 this is a STUB — tapping does nothing useful. Plan 4 wires the real `PayButton` view.
 *
 * @param onClick fired when the user taps
 * @param modifier layout modifier
 */
@Composable
public expect fun GooglePayButton(
    onClick: () -> Unit,
    modifier: Modifier,
)
