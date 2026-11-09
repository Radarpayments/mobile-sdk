package net.payrdr.mobile.payment.sdk.payment.ui.screens

import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState

internal actual fun SdkPaymentConfig.hasPlatformWallet(): Boolean = googlePay != null

internal actual fun PaymentState.CardFormShown.offersPlatformWallet(): Boolean = googlePay != null
