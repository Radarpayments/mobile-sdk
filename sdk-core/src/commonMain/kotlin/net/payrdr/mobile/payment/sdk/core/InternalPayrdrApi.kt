package net.payrdr.mobile.payment.sdk.core

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HidesFromObjC

/**
 * Marks declarations that the SDK's own modules share with each other. They are not part of the public API: they
 * can change or disappear in any release, and Swift does not see them.
 */
@OptIn(ExperimentalObjCRefinement::class)
@HidesFromObjC
@RequiresOptIn(message = "Internal to the Payrdr SDK modules; not for use outside the SDK.", level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class InternalPayrdrApi
