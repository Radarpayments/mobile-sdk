package net.payrdr.mobile.payment.sdk.core

import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.core.model.ParamField
import kotlin.test.Test

class TokenResultTest {
    @Test
    fun `withToken sets token and empty errors`() {
        val result = TokenResult.withToken("token-abc")
        result.token shouldBe "token-abc"
        result.errors.shouldBeEmpty()
        result.isSuccessful shouldBe true
    }

    @Test
    fun `withErrors sets errors and null token`() {
        val errors = mapOf(ParamField.PAN to "invalid")
        val result = TokenResult.withErrors(errors)
        result.token.shouldBeNull()
        result.errors shouldBe errors
        result.isSuccessful shouldBe false
    }
}
