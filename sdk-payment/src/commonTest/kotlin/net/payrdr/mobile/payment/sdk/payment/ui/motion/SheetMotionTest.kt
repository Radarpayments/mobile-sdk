package net.payrdr.mobile.payment.sdk.payment.ui.motion

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SheetMotionTest {
    @Test
    fun `receipt lines follow the mark one after another`() {
        cascadeDelayMs(0) shouldBe 320
        cascadeDelayMs(3) shouldBe 455
    }
}
