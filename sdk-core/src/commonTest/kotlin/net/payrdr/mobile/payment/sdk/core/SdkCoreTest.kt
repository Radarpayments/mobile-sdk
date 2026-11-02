package net.payrdr.mobile.payment.sdk.core

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import net.payrdr.mobile.payment.sdk.core.model.CardParams
import net.payrdr.mobile.payment.sdk.core.model.MsdkRegisteredFrom
import net.payrdr.mobile.payment.sdk.core.model.ParamField
import net.payrdr.mobile.payment.sdk.core.model.SdkCoreConfig
import kotlin.test.Test

class SdkCoreTest {
    private val sdk = SdkCore()

    @Test
    fun `generate returns token on fully-valid new card`() {
        val result =
            sdk.generate(
                SdkCoreConfig(
                    cardParams =
                        CardParams(
                            mdOrder = "order-abc",
                            pan = "4532896701439077",
                            cvc = "444",
                            expiryMMYY = "12/28",
                            cardHolder = "JOHN SMITH",
                            pubKey = TEST_PUBLIC_KEY_PEM,
                        ),
                    timestamp = 1594009580806L,
                    registeredFrom = MsdkRegisteredFrom.MSDK_CORE,
                ),
            )

        result.token.shouldNotBeNull()
        result.token!!.shouldNotBeBlank()
        result.errors.shouldBeEmpty()
        result.isSuccessful shouldBe true
    }

    @Test
    fun `generate returns errors on invalid PAN`() {
        val result =
            sdk.generate(
                SdkCoreConfig(
                    cardParams =
                        CardParams(
                            mdOrder = "order-abc",
                            pan = "INVALID-CARD",
                            cvc = "444",
                            expiryMMYY = "12/28",
                            cardHolder = "JOHN SMITH",
                            pubKey = TEST_PUBLIC_KEY_PEM,
                        ),
                    timestamp = 1594009580806L,
                ),
            )

        result.token.shouldBeNull()
        result.errors.shouldNotBeEmpty()
        result.errors shouldContain (ParamField.PAN to "invalid-format")
    }

    @Test
    fun `generate returns errors on missing CVV`() {
        val result =
            sdk.generate(
                SdkCoreConfig(
                    cardParams =
                        CardParams(
                            mdOrder = "order-abc",
                            pan = "4532896701439077",
                            cvc = "",
                            expiryMMYY = "12/28",
                            cardHolder = "JOHN SMITH",
                            pubKey = TEST_PUBLIC_KEY_PEM,
                        ),
                    timestamp = 1594009580806L,
                ),
            )

        result.errors shouldContain (ParamField.CVC to "required")
    }

    @Test
    fun `empty fields are each reported as themselves with the card number first`() {
        // Several empty fields share one value (""); each must still be checked by its own validator.
        val result =
            sdk.generate(
                SdkCoreConfig(
                    cardParams =
                        CardParams(
                            mdOrder = "order-abc",
                            pan = "",
                            cvc = "",
                            expiryMMYY = "",
                            cardHolder = null,
                            pubKey = TEST_PUBLIC_KEY_PEM,
                        ),
                    timestamp = 1594009580806L,
                ),
            )

        result.errors shouldBe mapOf(ParamField.PAN to "required")
    }

    @Test
    fun `an empty card number is not checked as a CVC`() {
        val result =
            sdk.generate(
                SdkCoreConfig(
                    cardParams =
                        CardParams(
                            mdOrder = "order-abc",
                            pan = "",
                            cvc = "",
                            expiryMMYY = "12/28",
                            cardHolder = "JOHN SMITH",
                            pubKey = TEST_PUBLIC_KEY_PEM,
                        ),
                    timestamp = 1594009580806L,
                ),
            )

        result.errors shouldBe mapOf(ParamField.PAN to "required")
    }

    private companion object {
        // Same fixture RSA-2048 public key used in RsaCryptogramCipherTest — keep in sync.
        // Generated via: openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out /tmp/test_rsa.pem
        //               openssl rsa -pubout -in /tmp/test_rsa.pem -out /tmp/test_rsa_pub.pem
        const val TEST_PUBLIC_KEY_PEM = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvjG7qd50WLuBmw7rUpQo
xN44zA/XUddLxYAN22fJE9GbLMwKcT0fVE//KjAUDDy3s8p4QuNuxLfUsINM4DlV
IbQZx3ZOXUaQILYCqmqqvbibEk4ulITdMS6QjEsHXdcKXpr+1ZyVG90RI7oxyQf8
YKTxqOQF7lYqicEb6ucPJwYxLA1IoPu9K5PAysLrFiEwuq+z6dNFrvVMXSDAefry
Hc/MnjXhGLc3qcfH9sJczKQ8we6I7zIDgRcIjsZ8xI8ME+X36UJpYPvrU41Xssi1
6MCObHbZpZTEjShPnfmwTIDMbjnqwitXKaC0C+vJ+DvnL5+iqsHN+HT+z8NcQXco
xQIDAQAB
-----END PUBLIC KEY-----"""
    }
}
