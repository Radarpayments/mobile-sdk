package net.payrdr.mobile.payment.sdk.payment.flow

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.MerchantCountry
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.MerchantInfo
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.PaymentSettings
import net.payrdr.mobile.payment.sdk.payment.api.dto.response.SessionStatusResponse
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayEnvironment
import kotlin.test.Test

class WalletResolverTest {
    private val session =
        SessionStatusResponse(
            remainingSecs = 1200,
            amount = "99.00",
            currencyAlphaCode = "EUR",
            merchantInfo =
                MerchantInfo(
                    merchantLogin = "shop-login",
                    merchantFullName = "Hearth & Co",
                    merchantCountry = MerchantCountry(countryCode = "276", countryCodeAlpha2 = "DE"),
                ),
            merchantOptions = listOf("CARD", "GOOGLEPAY", "APPLEPAY"),
        )

    private val settings =
        PaymentSettings(
            mapOf(
                "googlePay.environment" to "PRODUCTION",
                "googlePay.merchantId" to "BCR2DN4T",
                "googlePay.gateway" to "bpcpay",
            ),
        )

    @Test fun `google pay takes everything from the gateway and the order`() {
        WalletResolver.googlePay(GooglePayConfig(), session, settings) shouldBe
            GooglePayConfig(
                merchantId = "BCR2DN4T",
                merchantFullName = "Hearth & Co",
                gateway = "bpcpay",
                gatewayMerchantId = "shop-login",
                environment = GooglePayEnvironment.PRODUCTION,
                countryCode = "DE",
            )
    }

    @Test fun `what the merchant sets wins over the gateway and the order`() {
        val config =
            GooglePayConfig(
                merchantId = "own-id",
                merchantFullName = "Own name",
                gateway = "own-gateway",
                gatewayMerchantId = "own-gateway-id",
                environment = GooglePayEnvironment.TEST,
                countryCode = "FR",
            )

        WalletResolver.googlePay(config, session, settings) shouldBe config
    }

    @Test fun `google pay is off when the gateway has not enabled it for the merchant`() {
        WalletResolver.googlePay(GooglePayConfig(), session.copy(merchantOptions = listOf("CARD")), settings).shouldBeNull()
    }

    @Test fun `google pay is off when the gateway hides it`() {
        val hidden = PaymentSettings(settings.values + ("ecom2.hidden.payments" to "SBP_PAY, GOOGLE_PAY"))

        WalletResolver.googlePay(GooglePayConfig(), session, hidden).shouldBeNull()
    }

    @Test fun `without the gateway settings google pay is off unless the merchant set them`() {
        WalletResolver.googlePay(GooglePayConfig(), session, settings = null).shouldBeNull()
        WalletResolver
            .googlePay(
                GooglePayConfig(merchantId = "own-id", gateway = "bpcpay", environment = GooglePayEnvironment.TEST),
                session,
                settings = null,
            )?.gatewayMerchantId shouldBe "shop-login"
    }

    @Test fun `google pay is off when no country is known instead of guessing one`() {
        val noCountry = session.copy(merchantInfo = session.merchantInfo.copy(merchantCountry = null))

        WalletResolver.googlePay(GooglePayConfig(), noCountry, settings).shouldBeNull()
        WalletResolver.googlePay(GooglePayConfig(countryCode = "DE"), noCountry, settings)?.countryCode shouldBe "DE"
    }

    @Test fun `google pay is off when the gateway names an environment it does not know`() {
        val odd = PaymentSettings(settings.values + ("googlePay.environment" to "STAGING"))

        WalletResolver.googlePay(GooglePayConfig(), session, odd).shouldBeNull()
    }

    @Test fun `apple pay takes the country from the order`() {
        WalletResolver.applePay(ApplePayConfig(merchantId = "merchant.shop"), session, settings) shouldBe
            ApplePayConfig(merchantId = "merchant.shop", countryCode = "DE")
    }

    @Test fun `apple pay is off when the gateway has not enabled or hides it`() {
        val config = ApplePayConfig(merchantId = "merchant.shop")

        WalletResolver.applePay(config, session.copy(merchantOptions = listOf("GOOGLEPAY")), settings).shouldBeNull()
        WalletResolver.applePay(config, session, PaymentSettings(mapOf("ecom2.hidden.payments" to "APPLE_PAY"))).shouldBeNull()
    }

    @Test fun `apple pay does not need the gateway settings`() {
        WalletResolver.applePay(ApplePayConfig(merchantId = "merchant.shop"), session, settings = null)?.countryCode shouldBe "DE"
    }

    @Test fun `a wallet is off when the order names no currency`() {
        val noCurrency = session.copy(currencyAlphaCode = null)

        WalletResolver.googlePay(GooglePayConfig(), noCurrency, settings).shouldBeNull()
        WalletResolver.applePay(ApplePayConfig(merchantId = "merchant.shop"), noCurrency, settings).shouldBeNull()
    }

    @Test fun `a blank value in the config falls back to the gateway and the order`() {
        val resolved = WalletResolver.googlePay(GooglePayConfig(merchantId = " ", gateway = "", countryCode = ""), session, settings)

        resolved?.merchantId shouldBe "BCR2DN4T"
        resolved?.gateway shouldBe "bpcpay"
        resolved?.countryCode shouldBe "DE"
        WalletResolver.applePay(ApplePayConfig(merchantId = "merchant.shop", countryCode = " "), session, settings)?.countryCode shouldBe
            "DE"
    }

    @Test fun `values a wallet would reject hide it instead of failing on the tap`() {
        WalletResolver.googlePay(GooglePayConfig(), session.copy(currencyAlphaCode = "EURO"), settings).shouldBeNull()
        WalletResolver.googlePay(GooglePayConfig(countryCode = "DEU"), session, settings).shouldBeNull()
        WalletResolver.googlePay(GooglePayConfig(allowedCardNetworks = emptySet()), session, settings).shouldBeNull()
        WalletResolver.googlePay(GooglePayConfig(allowedAuthMethods = emptySet()), session, settings).shouldBeNull()
        WalletResolver
            .applePay(
                ApplePayConfig(merchantId = "merchant.shop", supportedNetworks = emptySet()),
                session,
                settings,
            ).shouldBeNull()
        WalletResolver.applePay(ApplePayConfig(merchantId = "merchant.shop", countryCode = "DEU"), session, settings).shouldBeNull()
    }
}
