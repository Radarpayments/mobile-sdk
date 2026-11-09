package net.payrdr.mobile.payment.sdk.payment.ui.loading

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoadingPhasesTest {
    /** Phases with the virtual time each arrived at; the session is ready at [readyAtMs] (null = never). */
    private fun TestScope.timeline(readyAtMs: Long?): List<Pair<Long, LoadingPhase>> {
        val ready = MutableStateFlow(readyAtMs == 0L)
        val seen = mutableListOf<Pair<Long, LoadingPhase>>()
        backgroundScope.launch { loadingPhases(ready).collect { seen += testScheduler.currentTime to it } }
        if (readyAtMs != null && readyAtMs > 0) {
            advanceTimeBy(readyAtMs)
            ready.value = true
        }
        advanceTimeBy(5_000)
        advanceUntilIdle()
        return seen
    }

    @Test
    fun `fast session never shows the skeleton`() =
        runTest {
            timeline(readyAtMs = 200) shouldBe listOf(0L to LoadingPhase.Blank, 200L to LoadingPhase.Content)
        }

    @Test
    fun `slow session shows the skeleton at 300 and holds it for 400`() =
        runTest {
            timeline(readyAtMs = 500) shouldBe
                listOf(0L to LoadingPhase.Blank, 300L to LoadingPhase.Skeleton, 700L to LoadingPhase.Content)
        }

    @Test
    fun `very slow session shows content when ready`() =
        runTest {
            timeline(readyAtMs = 1000) shouldBe
                listOf(0L to LoadingPhase.Blank, 300L to LoadingPhase.Skeleton, 1000L to LoadingPhase.Content)
        }

    @Test
    fun `ready at once`() =
        runTest {
            timeline(readyAtMs = 0) shouldBe listOf(0L to LoadingPhase.Blank, 0L to LoadingPhase.Content)
        }

    @Test
    fun `never ready stays on the skeleton`() =
        runTest {
            timeline(readyAtMs = null) shouldBe listOf(0L to LoadingPhase.Blank, 300L to LoadingPhase.Skeleton)
        }
}
