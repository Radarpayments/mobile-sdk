package net.payrdr.mobile.payment.sdk.payment.threeds

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AcsFormTest {
    @Test fun `base64 survives the form - plus slash and equals are escaped`() {
        AcsForm.encode("eJx+a/b=") shouldBe "eJx%2Ba%2Fb%3D"
    }

    @Test fun `separators of the form itself are escaped`() {
        AcsForm.encode("a&b=c d") shouldBe "a%26b%3Dc%20d"
    }

    @Test fun `unreserved characters stay and others go as UTF-8 bytes`() {
        AcsForm.encode("Az09-._~é") shouldBe "Az09-._~%C3%A9"
    }

    @Test fun `the body carries MD PaReq and TermUrl in the bank's order`() {
        AcsForm.body(md = "m+d", paReq = "p/r=", termUrl = "https://pay.example.com/term?x=1") shouldBe
            "MD=m%2Bd&PaReq=p%2Fr%3D&TermUrl=https%3A%2F%2Fpay.example.com%2Fterm%3Fx%3D1"
    }
}
