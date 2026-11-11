package net.payrdr.mobile.payment.sdk.payment.ui.loading

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** What the sheet shows while it opens. */
internal enum class LoadingPhase {
    /** Header only, the body laid out but not drawn: the sheet rises at its final height. */
    Blank,

    /** The pulsing skeleton. */
    Skeleton,

    /** The real content may come in. */
    Content,
}

/**
 * The opening of the sheet against [ready] (the session is in): [LoadingPhase.Blank] at once; content ready within
 * [skeletonDelay] goes straight to [LoadingPhase.Content], so a fast session never flashes a skeleton. Otherwise the
 * skeleton shows at [skeletonDelay] and stays at least [minSkeleton], however soon the content comes after it.
 * Completes after [LoadingPhase.Content]; never ready stays on the skeleton.
 */
internal fun loadingPhases(
    ready: Flow<Boolean>,
    skeletonDelay: Duration = SkeletonTiming.DELAY_MS.milliseconds,
    minSkeleton: Duration = SkeletonTiming.MIN_MS.milliseconds,
): Flow<LoadingPhase> =
    flow {
        emit(LoadingPhase.Blank)
        if (withTimeoutOrNull(skeletonDelay) { ready.first { it } } != null) {
            emit(LoadingPhase.Content)
            return@flow
        }
        emit(LoadingPhase.Skeleton)
        coroutineScope {
            val held = async { delay(minSkeleton) }
            ready.first { it }
            held.await()
        }
        emit(LoadingPhase.Content)
    }

/** When the skeleton appears, and how long it stays once shown. */
internal object SkeletonTiming {
    const val DELAY_MS = 300
    const val MIN_MS = 400
}
