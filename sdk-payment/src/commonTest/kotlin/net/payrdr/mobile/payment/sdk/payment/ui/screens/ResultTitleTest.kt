package net.payrdr.mobile.payment.sdk.payment.ui.screens

import io.kotest.matchers.shouldBe
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_card_saved_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_failure_title
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.result_success_title
import kotlin.test.Test

class ResultTitleTest {
    @Test
    fun `an order that only saves a card says the card is saved and not paid`() {
        resultTitle(success = true, saveCardOnly = true) shouldBe Res.string.result_card_saved_title
        resultTitle(success = true, saveCardOnly = false) shouldBe Res.string.result_success_title
    }

    @Test
    fun `a failure reads the same whether or not the order only saves a card`() {
        resultTitle(success = false, saveCardOnly = true) shouldBe Res.string.result_failure_title
        resultTitle(success = false, saveCardOnly = false) shouldBe Res.string.result_failure_title
    }
}
