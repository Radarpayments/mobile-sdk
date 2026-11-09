package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Pay with Google Pay button. On Android this wraps `PayButton` from
 * `com.google.android.gms.wallet.button`; on iOS it is hidden by the parent composable (the
 * `SdkPaymentConfig.googlePay == null` check happens upstream).
 */
@Composable
internal expect fun GooglePayButton(
    onClick: () -> Unit,
    modifier: Modifier,
)
