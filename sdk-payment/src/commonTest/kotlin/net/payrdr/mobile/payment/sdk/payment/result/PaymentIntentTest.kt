package net.payrdr.mobile.payment.sdk.payment.result

import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import net.payrdr.mobile.payment.sdk.payment.support.Fixtures
import kotlin.test.Test

class PaymentIntentTest {
    @Test fun `new card toString hides the card and the payer`() {
        val text =
            PaymentIntent
                .SubmitNewCard(
                    pan = Fixtures.TEST_PAN,
                    expiryMMYY = Fixtures.TEST_EXPIRY,
                    cvc = Fixtures.TEST_CVC,
                    cardHolder = Fixtures.TEST_CARDHOLDER,
                    saveCard = true,
                    payerData = mapOf(PayerField.EMAIL to "alex.morgan@example.com"),
                ).toString()

        text shouldNotContain Fixtures.TEST_PAN
        text shouldNotContain Fixtures.TEST_CVC
        text shouldNotContain Fixtures.TEST_EXPIRY
        text shouldNotContain Fixtures.TEST_CARDHOLDER
        text shouldNotContain "alex.morgan@example.com"
        // Enough to tell cards apart in a log, as printed on receipts.
        text shouldContain Fixtures.TEST_PAN.takeLast(4)
        text shouldContain "saveCard=true"
    }

    @Test fun `saved card toString hides the CVV and the payer`() {
        val text =
            PaymentIntent
                .SubmitSavedCard(
                    bindingId = Fixtures.TEST_BINDING_ID,
                    cvc = Fixtures.TEST_CVC,
                    payerData = mapOf(PayerField.MOBILE_PHONE to "+35799000000"),
                ).toString()

        text shouldNotContain Fixtures.TEST_CVC
        text shouldNotContain "+35799000000"
        text shouldContain Fixtures.TEST_BINDING_ID
    }
}
