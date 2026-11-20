package net.payrdr.mobile.payment.sdk.qa.launcher

import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayAuthMethod
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayCardNetwork
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayEnvironment
import net.payrdr.mobile.payment.sdk.payment.logger.LogTransport
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerProvider
import net.payrdr.mobile.payment.sdk.payment.ssl.SslPinningConfig
import net.payrdr.mobile.payment.sdk.qa.config.GooglePayOptions
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.SslPinningOptions
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.theme.paymentTheme

/**
 * The SDK's config for a validated [config] on [profile]'s gateway, with the SDK's log going to [journal].
 *
 * SslPinningConfig has no equals, so two builds of one config with pinning are not equal: the checkout has to be keyed
 * on the [QaConfig], not on this result.
 */
internal fun sdkPaymentConfig(
    config: QaConfig,
    profile: StandProfile,
    journal: LogTransport,
    scanner: CardScannerProvider,
): SdkPaymentConfig {
    val sdk = config.sdk
    return SdkPaymentConfig(
        baseUrl = profile.gatewayUrl,
        keyProviderUrl = sdk.keyProviderUrl,
        sslPinning = sdk.sslPinning.takeIf { it.enabled }?.sslPinningConfig(),
        logTransport = journal,
        locale = sdk.locale,
        theme = sdk.theme.paymentTheme(),
        googlePay = sdk.googlePay.takeIf { it.enabled }?.googlePayConfig(),
        applePay = null,
        cardScannerProvider = scanner.takeIf { sdk.scanner },
        showResultScreen = sdk.showResultScreen,
    )
}

private fun SslPinningOptions.sslPinningConfig(): SslPinningConfig =
    pins.fold(SslPinningConfig.builder()) { builder, pin -> builder.addPin(pin.host, pin.pin) }.build()

private fun GooglePayOptions.googlePayConfig() =
    GooglePayConfig(
        merchantId = merchantId,
        merchantFullName = merchantFullName,
        gateway = gateway,
        gatewayMerchantId = gatewayMerchantId,
        environment = environment?.let(::googlePayEnvironment),
        allowedCardNetworks =
            allowedCardNetworks.mapTo(mutableSetOf()) { GooglePayCardNetwork.entries.byValue(it) { network -> network.value } },
        allowedAuthMethods =
            allowedAuthMethods.mapTo(mutableSetOf()) { GooglePayAuthMethod.entries.byValue(it) { method -> method.value } },
        countryCode = countryCode,
        billingAddressRequired = billingAddressRequired,
    )

private fun googlePayEnvironment(name: String) =
    when (name) {
        "test" -> GooglePayEnvironment.TEST
        "production" -> GooglePayEnvironment.PRODUCTION
        else -> error("Google Pay environment '$name' passed the validation")
    }

private fun <T> List<T>.byValue(
    name: String,
    value: (T) -> String,
): T = firstOrNull { value(it) == name } ?: error("Google Pay value '$name' passed the validation")
