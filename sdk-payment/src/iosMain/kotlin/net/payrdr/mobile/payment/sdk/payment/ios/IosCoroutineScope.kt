package net.payrdr.mobile.payment.sdk.payment.ios

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

/**
 * Swift-callable factory for a `CoroutineScope` bound to Dispatchers.Main.
 *
 * Kotlin/Native does not re-export `kotlinx.coroutines.MainScope()` in the generated
 * Obj-C header, so Swift consumers that need to pass a `scope` argument into
 * [SdkPayment.paymentFlow], [SdkPayment.completePayment], or
 * [SdkPaymentViewControllerFactory.make] would have no idiomatic way to obtain one.
 *
 * Usage from Swift: `IosCoroutineScopeKt.iosMainScope()`.
 */
public fun iosMainScope(): CoroutineScope = MainScope()
