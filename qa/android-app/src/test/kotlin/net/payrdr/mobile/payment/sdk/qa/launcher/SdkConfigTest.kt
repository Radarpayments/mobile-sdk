package net.payrdr.mobile.payment.sdk.qa.launcher

import androidx.compose.runtime.Composable
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayAuthMethod
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayCardNetwork
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayEnvironment
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerConfig
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerProvider
import net.payrdr.mobile.payment.sdk.payment.scanner.CardScannerResult
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme
import net.payrdr.mobile.payment.sdk.qa.config.GooglePayOptions
import net.payrdr.mobile.payment.sdk.qa.config.Pin
import net.payrdr.mobile.payment.sdk.qa.config.QaConfig
import net.payrdr.mobile.payment.sdk.qa.config.SdkOptions
import net.payrdr.mobile.payment.sdk.qa.config.SslPinningOptions
import net.payrdr.mobile.payment.sdk.qa.config.StandProfile
import net.payrdr.mobile.payment.sdk.qa.config.ThemePreset
import net.payrdr.mobile.payment.sdk.qa.journal.Journal
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SdkConfigTest {
    private val profile = StandProfile("sandbox", "https://dev.bpcbt.com/payment", login = "login-1", password = "password-1")
    private val journal = Journal()
    private val scanner =
        object : CardScannerProvider {
            @Composable
            override fun CardScannerScreen(
                config: CardScannerConfig,
                onResult: (CardScannerResult) -> Unit,
            ) = Unit
        }
    private val pin = Pin("dev.bpcbt.com", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")

    private fun build(sdk: SdkOptions = SdkOptions()) =
        sdkPaymentConfig(QaConfig(profile = "sandbox", sdk = sdk), profile, journal, scanner)

    @Test
    fun `the default config opens the profile's gateway with nothing optional`() {
        val config = build()
        config.baseUrl shouldBe "https://dev.bpcbt.com/payment"
        config.locale.shouldBeNull()
        config.keyProviderUrl.shouldBeNull()
        config.sslPinning.shouldBeNull()
        config.googlePay.shouldBeNull()
        config.applePay.shouldBeNull()
        config.cardScannerProvider.shouldBeNull()
        config.theme shouldBe PaymentTheme.System
        config.showResultScreen shouldBe false
        config.logTransport shouldBeSameInstanceAs journal
    }

    @Test
    fun `the sdk options are passed as set`() {
        val config =
            build(
                SdkOptions(
                    locale = "ro",
                    theme = ThemePreset.DARK,
                    showResultScreen = true,
                    keyProviderUrl = "https://dev.bpcbt.com/payment/se/keys.do",
                ),
            )
        config.locale shouldBe "ro"
        config.theme shouldBe PaymentTheme.Dark
        config.showResultScreen shouldBe true
        config.keyProviderUrl shouldBe "https://dev.bpcbt.com/payment/se/keys.do"
    }

    @Test
    fun `google pay is passed only when enabled`() {
        val options = GooglePayOptions(enabled = true, environment = "test", allowedCardNetworks = listOf("VISA"))
        build(SdkOptions(googlePay = options)).googlePay shouldBe
            GooglePayConfig(
                environment = GooglePayEnvironment.TEST,
                allowedCardNetworks = setOf(GooglePayCardNetwork.VISA),
                allowedAuthMethods = setOf(GooglePayAuthMethod.PAN_ONLY, GooglePayAuthMethod.CRYPTOGRAM_3DS),
            )
        build(SdkOptions(googlePay = options.copy(enabled = false))).googlePay.shouldBeNull()
    }

    @Test
    fun `every google pay field is passed and an unset one stays null`() {
        val options =
            GooglePayOptions(
                enabled = true,
                merchantId = "merchant-1",
                merchantFullName = "QA store",
                gateway = "bpcpay",
                gatewayMerchantId = "gateway-merchant-1",
                environment = "production",
                allowedCardNetworks = listOf("AMEX", "DISCOVER", "INTERAC", "JCB", "MASTERCARD", "VISA"),
                allowedAuthMethods = listOf("CRYPTOGRAM_3DS"),
                countryCode = "CY",
                billingAddressRequired = true,
            )
        build(SdkOptions(googlePay = options)).googlePay shouldBe
            GooglePayConfig(
                merchantId = "merchant-1",
                merchantFullName = "QA store",
                gateway = "bpcpay",
                gatewayMerchantId = "gateway-merchant-1",
                environment = GooglePayEnvironment.PRODUCTION,
                allowedCardNetworks = GooglePayCardNetwork.entries.toSet(),
                allowedAuthMethods = setOf(GooglePayAuthMethod.CRYPTOGRAM_3DS),
                countryCode = "CY",
                billingAddressRequired = true,
            )
        build(SdkOptions(googlePay = GooglePayOptions(enabled = true))).googlePay shouldBe
            GooglePayConfig(environment = null)
    }

    @Test
    fun `a google pay value the validation lets through is a contract violation`() {
        assertFailsWith<IllegalStateException> {
            build(SdkOptions(googlePay = GooglePayOptions(enabled = true, environment = "staging")))
        }
        assertFailsWith<IllegalStateException> {
            build(SdkOptions(googlePay = GooglePayOptions(enabled = true, allowedCardNetworks = listOf("MIR"))))
        }
        assertFailsWith<IllegalStateException> {
            build(SdkOptions(googlePay = GooglePayOptions(enabled = true, allowedAuthMethods = listOf("PIN"))))
        }
    }

    @Test
    fun `the scanner is passed only when asked for`() {
        build(SdkOptions(scanner = true)).cardScannerProvider shouldBeSameInstanceAs scanner
        build(SdkOptions(scanner = false)).cardScannerProvider.shouldBeNull()
    }

    @Test
    fun `pinning is passed only when enabled`() {
        build(SdkOptions(sslPinning = SslPinningOptions(enabled = true, pins = listOf(pin)))).sslPinning.shouldNotBeNull()
        build(SdkOptions(sslPinning = SslPinningOptions(enabled = false, pins = listOf(pin)))).sslPinning.shouldBeNull()
    }

    @Test
    fun `equal configs build equal sdk configs`() {
        val sdk =
            SdkOptions(
                theme = ThemePreset.LARGE_TEXT,
                scanner = true,
                googlePay = GooglePayOptions(enabled = true, environment = "test"),
            )
        build(sdk) shouldBe build(sdk.copy())
        ThemePreset.entries.forEach { build(SdkOptions(theme = it)) shouldBe build(SdkOptions(theme = it)) }
    }

    // SslPinningConfig has no equals: two builds of one pin list are different objects, so an sdk config with pinning
    // never equals another, and the caller has to key the checkout on the QaConfig rather than on this result.
    @Test
    fun `with pinning only the pinning config differs between two builds`() {
        val sdk = SdkOptions(sslPinning = SslPinningOptions(enabled = true, pins = listOf(pin)))
        val first = build(sdk)
        val second = build(sdk)
        first.sslPinning shouldNotBe second.sslPinning
        first.copy(sslPinning = null) shouldBe second.copy(sslPinning = null)
    }
}
