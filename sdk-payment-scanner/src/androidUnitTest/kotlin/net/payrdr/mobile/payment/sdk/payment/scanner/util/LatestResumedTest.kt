package net.payrdr.mobile.payment.sdk.payment.scanner.util

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LatestResumedTest {
    private class Screen

    @Test
    fun `the screen resumed last is the current one`() {
        val tracker = LatestResumed<Screen>()
        val first = Screen()
        val second = Screen()
        tracker.current() shouldBe null
        tracker.resumed(first)
        tracker.resumed(second)
        tracker.current() shouldBe second
    }

    @Test
    fun `a screen destroyed after another came up does not clear it`() {
        val tracker = LatestResumed<Screen>()
        val leaving = Screen()
        val shown = Screen()
        tracker.resumed(leaving)
        tracker.resumed(shown)
        tracker.destroyed(leaving)
        tracker.current() shouldBe shown
    }

    @Test
    fun `destroying the current screen leaves none`() {
        val tracker = LatestResumed<Screen>()
        val only = Screen()
        tracker.resumed(only)
        tracker.destroyed(only)
        tracker.current() shouldBe null
    }
}
