package net.payrdr.mobile.payment.samples.android.payment.composition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.payrdr.mobile.payment.sdk.payment.SdkPayment
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig

/**
 * Composable helper that remembers an `SdkPayment` instance keyed by the config's stable identity
 * (baseUrl + theme + gpay + applePay + locale + pinning). Re-creates when any of these change.
 */
@Composable
fun rememberSdkPayment(config: SdkPaymentConfig): SdkPayment =
    remember(
        config.baseUrl,
        config.theme,
        config.googlePay,
        config.applePay,
        config.locale,
        config.sslPinning,
    ) {
        SdkPayment.create(config)
    }
