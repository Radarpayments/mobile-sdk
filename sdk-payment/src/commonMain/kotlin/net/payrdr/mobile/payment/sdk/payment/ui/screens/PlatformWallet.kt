package net.payrdr.mobile.payment.sdk.payment.ui.screens

import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState

/** The wallet of this platform is configured: Google Pay on Android, Apple Pay on iOS. */
internal expect fun SdkPaymentConfig.hasPlatformWallet(): Boolean

/** The order of this card form offers the wallet of this platform. */
internal expect fun PaymentState.CardFormShown.offersPlatformWallet(): Boolean
