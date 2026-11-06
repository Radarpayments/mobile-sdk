package net.payrdr.mobile.payment.sdk.bridge

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayMerchantCapability
import net.payrdr.mobile.payment.sdk.payment.applepay.ApplePayNetwork
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.exception.SdkApplePayException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkCryptogramException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkDeclinedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkGooglePayException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkInvalidSessionIdException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNetworkException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkNotConfiguredException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkOrderNotFoundException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkPaymentApiException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionExpiredException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkSessionNotFoundException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkThreeDsChallengeFailedException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkThreeDsTimeoutException
import net.payrdr.mobile.payment.sdk.payment.exception.SdkTransactionException
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayAuthMethod
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayCardNetwork
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayEnvironment
import net.payrdr.mobile.payment.sdk.payment.logger.ConsoleLogTransport
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.scanner.provider.MlKitVisionCardScannerProvider
import net.payrdr.mobile.payment.sdk.payment.ui.theme.DefaultDarkColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import kotlin.test.Test

/**
 * The sample messages in sdk-bridge/contract, which the Flutter and React Native tests check their side against: what the plugins
 * send is read here, and what is sent back here is what they read. A case added on one side fails until both have it.
 */
class ContractTest {
    private fun cases(fixture: String): Map<String, JsonElement> = Json.parseToJsonElement(fixture).jsonObject

    private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    @Test fun `every checkout request the plugins send is read as meant`() {
        val parsed = cases(ContractFixtures.CHECKOUT_REQUESTS).mapValues { (_, request) -> CheckoutRequest.parse(request.toString()) }
        val expected =
            mapOf<String, (CheckoutRequest) -> Unit>(
                "minimal" to {
                    it.config shouldBe SdkPaymentConfig(baseUrl = "https://pay.example.com")
                    it.checkout shouldBe CheckoutConfig.MdOrder("md-1")
                },
                "session" to { it.checkout shouldBe CheckoutConfig.SessionId("ps_1") },
                "allOptions" to {
                    it.config.locale shouldBe "de"
                    it.config.theme shouldBe PaymentTheme.Dark
                    it.config.showResultScreen shouldBe true
                    it.config.cardScannerProvider.shouldBeInstanceOf<MlKitVisionCardScannerProvider>()
                    it.config.keyProviderUrl shouldBe "https://keys.example.com"
                    it.config.sslPinning!!
                        .entries
                        .single()
                        .hostname shouldBe "pay.example.com"
                    it.config.logTransport shouldBe ConsoleLogTransport
                },
                "customTheme" to {
                    it.config.theme shouldBe
                        PaymentTheme.Custom(
                            colors =
                                DefaultDarkColors.copy(
                                    primary = Color(0xFFF3D9B1),
                                    onPrimary = Color(0xFFFFFFFF),
                                    scrim = Color(0x80000000),
                                ),
                            shapes =
                                PaymentShapes.Default.copy(
                                    field = RoundedCornerShape(18.dp),
                                    button = RoundedCornerShape(999.dp),
                                    card = RoundedCornerShape(18.dp),
                                ),
                        )
                },
                "wallets" to {
                    it.config.googlePay shouldBe
                        GooglePayConfig(
                            merchantId = "BCR2",
                            merchantFullName = "Hearth & Co",
                            gateway = "bpcpay",
                            gatewayMerchantId = "shop",
                            environment = GooglePayEnvironment.TEST,
                            allowedCardNetworks = setOf(GooglePayCardNetwork.VISA, GooglePayCardNetwork.MASTERCARD),
                            allowedAuthMethods = setOf(GooglePayAuthMethod.PAN_ONLY, GooglePayAuthMethod.CRYPTOGRAM_3DS),
                            countryCode = "DE",
                            billingAddressRequired = true,
                        )
                    it.config.applePay shouldBe
                        ApplePayConfig(
                            merchantId = "merchant.shop",
                            supportedNetworks = setOf(ApplePayNetwork.VISA, ApplePayNetwork.AMEX),
                            merchantCapabilities = setOf(ApplePayMerchantCapability.THREE_D_SECURE, ApplePayMerchantCapability.DEBIT),
                            countryCode = "DE",
                        )
                },
            )

        parsed.keys shouldBe expected.keys
        parsed.forEach { (name, request) -> expected.getValue(name)(request) }
    }

    @Test fun `every checkout result the plugins read is what the bridge sends`() {
        val badTheme = """{"mode":"custom","colors":{"onPrimary":"white"}}"""
        val badColour =
            shouldThrow<InvalidBridgeRequest> { CheckoutRequest.parse("""{"config":{"baseUrl":"u","theme":$badTheme},"mdOrder":"md-1"}""") }
        val sent =
            mapOf(
                "completed" to CheckoutResultJson.encode(CheckoutResult.Completed("md-1")),
                "canceled" to CheckoutResultJson.encode(CheckoutResult.Canceled("md-1")),
                "unknown" to CheckoutResultJson.encode(CheckoutResult.Unknown("md-1")),
                "unknownAfterError" to CheckoutResultJson.encode(CheckoutResult.Unknown("md-1", SdkNetworkException("offline"))),
                "declined" to
                    CheckoutResultJson.encode(
                        CheckoutResult.Failed("md-1", SdkDeclinedException(declineCode = "116", message = "Insufficient funds")),
                    ),
                "gatewayError" to
                    CheckoutResultJson.encode(
                        CheckoutResult.Failed("md-1", SdkPaymentApiException(httpStatus = 500, errorCode = "5", message = "System error")),
                    ),
                "sessionExpired" to CheckoutResultJson.encode(CheckoutResult.Failed("md-1", SdkSessionExpiredException("md-1"))),
                "invalidRequest" to CheckoutResultJson.invalidRequest(badColour.message!!),
                "busy" to CheckoutResultJson.failure(NotOpened.BUSY, "A checkout is already open"),
            ).mapValues { (_, text) -> json(text) }

        sent shouldBe cases(ContractFixtures.CHECKOUT_RESULTS)
    }

    @Test fun `the plugins know every error code the bridge sends`() {
        val errors =
            listOf(
                SdkDeclinedException(),
                SdkPaymentApiException(),
                SdkNetworkException(),
                SdkTransactionException(),
                SdkSessionNotFoundException("ps_1"),
                SdkOrderNotFoundException("md-1"),
                SdkSessionExpiredException("md-1"),
                SdkInvalidSessionIdException("x"),
                SdkCryptogramException(),
                SdkNotConfiguredException("no gateway"),
                SdkThreeDsChallengeFailedException("denied"),
                SdkThreeDsTimeoutException(),
                SdkGooglePayException(),
                SdkApplePayException(),
                SdkException(),
            )
        val sent =
            errors.map { error ->
                json(CheckoutResultJson.encode(CheckoutResult.Failed("md-1", error)))["error"]!!.jsonObject["code"]!!.jsonPrimitive.content
            } + listOf(CheckoutResultJson.INVALID_REQUEST, NotOpened.BUSY, NotOpened.NO_SCREEN)
        val known = Json.parseToJsonElement(ContractFixtures.ERROR_CODES).jsonArray.map { it.jsonPrimitive.content }

        sent.toSet() shouldBe known.toSet()
        known.size shouldBe known.toSet().size
    }

    @Test fun `every SDK Core request the plugins send gives a token`() {
        val requests = cases(ContractFixtures.CORE_TOKEN_REQUESTS)

        requests.keys shouldBe setOf("card", "cardWithoutOrder", "savedCard", "savedCardWithoutOrder")
        requests.forEach { (name, request) ->
            val answer = json(PayrdrBridge.coreToken(withRealKey(request.jsonObject).toString()))
            (name to answer.keys) shouldBe (name to setOf("token"))
        }
    }

    @Test fun `every SDK Core answer the plugins read is what the bridge sends`() {
        val expected = cases(ContractFixtures.CORE_TOKEN_RESULTS)
        val card = withRealKey(cases(ContractFixtures.CORE_TOKEN_REQUESTS).getValue("card").jsonObject)
        val token = json(PayrdrBridge.coreToken(card.toString()))
        val noPan = JsonObject(card + ("pan" to JsonPrimitive("")))

        expected.keys shouldBe setOf("token", "fieldErrors", "invalidRequest")
        token.keys shouldBe expected.getValue("token").jsonObject.keys
        token.getValue("token").jsonPrimitive.isString shouldBe true
        json(PayrdrBridge.coreToken(noPan.toString())) shouldBe expected.getValue("fieldErrors")
        json(PayrdrBridge.coreToken("""{"type":"wallet","pubKey":"PEM"}""")) shouldBe expected.getValue("invalidRequest")
    }

    /** The fixtures carry a stand-in key, which sdk-core rightly refuses. */
    private fun withRealKey(request: JsonObject) = JsonObject(request + ("pubKey" to JsonPrimitive(TEST_PUBLIC_KEY_PEM)))
}
