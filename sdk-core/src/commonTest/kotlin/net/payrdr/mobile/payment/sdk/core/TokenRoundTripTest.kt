package net.payrdr.mobile.payment.sdk.core

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import kotlinx.datetime.TimeZone
import net.payrdr.mobile.payment.sdk.core.crypto.RsaCryptogramCipher
import net.payrdr.mobile.payment.sdk.core.crypto.TestRsaKeys
import net.payrdr.mobile.payment.sdk.core.crypto.decryptWithTestKey
import net.payrdr.mobile.payment.sdk.core.model.BindingParams
import net.payrdr.mobile.payment.sdk.core.model.CardInstantParams
import net.payrdr.mobile.payment.sdk.core.model.CardParams
import net.payrdr.mobile.payment.sdk.core.model.MsdkRegisteredFrom
import net.payrdr.mobile.payment.sdk.core.model.NewPaymentMethodStoredCardParams
import net.payrdr.mobile.payment.sdk.core.model.PaymentCardParams
import net.payrdr.mobile.payment.sdk.core.model.SdkCoreConfig
import net.payrdr.mobile.payment.sdk.core.processor.DefaultPaymentStringProcessor
import net.payrdr.mobile.payment.sdk.core.utils.Base58Coder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test

/**
 * The token opened with the private key, as the gateway opens it: the other tests only see that a token comes out,
 * not that it carries what the gateway reads.
 */
@OptIn(ExperimentalEncodingApi::class)
class TokenRoundTripTest {
    private val sdk = SdkCore(DefaultPaymentStringProcessor(TimeZone.UTC), RsaCryptogramCipher())

    @Test
    fun `a new card token opens to the payment string with the card and the order`() {
        val fields =
            open(
                CardParams(
                    mdOrder = "order-abc",
                    pan = PAN,
                    cvc = "444",
                    expiryMMYY = "02/28",
                    cardHolder = "JOHN SMITH",
                    pubKey = TestRsaKeys.PUBLIC_KEY_PEM,
                ),
                MsdkRegisteredFrom.MSDK_PAYMENT,
            )

        fields shouldBe listOf(TIME, UUID, PAN, "444", "202802", "order-abc", "", "JOHN SMITH", "MSDK_PAYMENT")
    }

    @Test
    fun `a saved card token carries the binding and no card details`() {
        val fields = open(BindingParams(mdOrder = "order-abc", bindingID = "binding-1", cvc = "123", pubKey = TestRsaKeys.PUBLIC_KEY_PEM))

        fields shouldBe listOf(TIME, UUID, "", "123", "", "order-abc", "binding-1", "", "MSDK_CORE")
    }

    @Test
    fun `a stored payment method token carries the binding of its pm id and no order`() {
        val fields =
            open(
                NewPaymentMethodStoredCardParams(
                    storedPaymentId = "pm_" + Base58Coder.encode("binding-1"),
                    cvc = null,
                    pubKey = TestRsaKeys.PUBLIC_KEY_PEM,
                ),
            )

        fields shouldBe listOf(TIME, UUID, "", "", "", "", "binding-1", "", "MSDK_CORE")
    }

    @Test
    fun `an instant card token carries the card and no order`() {
        val fields =
            open(CardInstantParams(pan = PAN, cvc = "444", expiryMMYY = "12/28", cardHolder = null, pubKey = TestRsaKeys.PUBLIC_KEY_PEM))

        fields shouldBe listOf(TIME, UUID, PAN, "444", "202812", "", "", "", "MSDK_CORE")
    }

    @Test
    fun `each token has an id of its own`() {
        val params = BindingParams(mdOrder = "order-abc", bindingID = "binding-1", cvc = "123", pubKey = TestRsaKeys.PUBLIC_KEY_PEM)

        decrypt(params)[1] shouldNotBe decrypt(params)[1]
    }

    /** The fields of the opened token, its id checked and replaced by [UUID]. */
    private fun open(
        params: PaymentCardParams,
        registeredFrom: MsdkRegisteredFrom = MsdkRegisteredFrom.MSDK_CORE,
    ): List<String> {
        val fields = decrypt(params, registeredFrom)
        fields[1] shouldMatch UUID_PATTERN
        return fields.toMutableList().apply { set(1, UUID) }
    }

    private fun decrypt(
        params: PaymentCardParams,
        registeredFrom: MsdkRegisteredFrom = MsdkRegisteredFrom.MSDK_CORE,
    ): List<String> {
        val result = sdk.generate(SdkCoreConfig(cardParams = params, timestamp = TIMESTAMP, registeredFrom = registeredFrom))
        result.errors.shouldBeEmpty()
        val token = result.token.shouldNotBeNull()
        return decryptWithTestKey(Base64.decode(token)).decodeToString().split("/")
    }

    private companion object {
        const val PAN = "4532896701439077"
        const val TIMESTAMP = 1594009580806L
        const val TIME = "2020-07-06T04:26:20+00:00"
        const val UUID = "<uuid>"
        val UUID_PATTERN = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }
}
