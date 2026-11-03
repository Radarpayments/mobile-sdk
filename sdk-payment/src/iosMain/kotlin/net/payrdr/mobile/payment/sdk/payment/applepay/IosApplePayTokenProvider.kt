package net.payrdr.mobile.payment.sdk.payment.applepay

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import net.payrdr.mobile.payment.sdk.payment.config.ApplePayConfig
import platform.Foundation.NSData
import platform.Foundation.base64EncodedStringWithOptions
import platform.PassKit.PKPayment
import platform.PassKit.PKPaymentAuthorizationController
import platform.PassKit.PKPaymentAuthorizationControllerDelegateProtocol
import platform.PassKit.PKPaymentAuthorizationResult
import platform.PassKit.PKPaymentAuthorizationStatus
import platform.darwin.NSObject
import kotlin.concurrent.AtomicReference

/**
 * iOS actual using [PKPaymentAuthorizationController]. Presents the Apple Pay sheet, captures
 * the raw encrypted token bytes from `PKPayment.token.paymentData`, and base64-encodes them
 * as the gateway expects them: the controller sends the token to `applepay/paymentOrder.do`,
 * or for a transfer to `p2p/perform.do`.
 *
 * Construct once per screen; `requestPaymentToken` may be called multiple times
 * (sequentially — Apple Pay sheet is exclusive).
 *
 * Concurrency: only one Apple Pay request may be in flight at a time. A second concurrent
 * call returns [ApplePayTokenResult.Failure] with message "Apple Pay request already in
 * flight" instead of clobbering the first deferred.
 */
internal class IosApplePayTokenProvider : ApplePayTokenProvider {
    private val pending: AtomicReference<CompletableDeferred<ApplePayTokenResult>?> =
        AtomicReference(null)

    // The open sheet's controller and delegate, until the sheet is gone. PassKit holds its delegate weakly, and the
    // token arrives before the sheet finishes: released any earlier, Kotlin/Native may collect the delegate, the sheet
    // never hears "finished" and stays on screen over the checkout.
    private val openSheet: AtomicReference<Pair<PKPaymentAuthorizationController, Any>?> = AtomicReference(null)

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun requestPaymentToken(
        config: ApplePayConfig,
        amountMinorUnits: Long,
        currencyCode: String,
        countryCode: String,
        merchantDisplayName: String,
    ): ApplePayTokenResult {
        if (!PKPaymentAuthorizationController.canMakePayments()) {
            return ApplePayTokenResult.NotAvailable
        }

        val deferred = CompletableDeferred<ApplePayTokenResult>()
        if (!pending.compareAndSet(null, deferred)) {
            return ApplePayTokenResult.Failure("Apple Pay request already in flight")
        }
        // A tap while the previous sheet is still animating away: nothing opens, the buyer stays on the form.
        if (openSheet.value != null) {
            pending.compareAndSet(deferred, null)
            return ApplePayTokenResult.Cancelled
        }

        return try {
            val request =
                ApplePayRequestBuilder.build(
                    config = config,
                    amountMinorUnits = amountMinorUnits,
                    currencyCode = currencyCode,
                    countryCode = countryCode,
                    merchantDisplayName = merchantDisplayName,
                )

            val controller = PKPaymentAuthorizationController(paymentRequest = request)
            val delegate =
                object : NSObject(), PKPaymentAuthorizationControllerDelegateProtocol {
                    override fun paymentAuthorizationController(
                        controller: PKPaymentAuthorizationController,
                        didAuthorizePayment: PKPayment,
                        handler: (PKPaymentAuthorizationResult?) -> Unit,
                    ) {
                        val data: NSData? = didAuthorizePayment.token.paymentData
                        if (data == null) {
                            handler(
                                PKPaymentAuthorizationResult(
                                    status = PKPaymentAuthorizationStatus.PKPaymentAuthorizationStatusFailure,
                                    errors = null,
                                ),
                            )
                            if (!deferred.isCompleted) deferred.complete(ApplePayTokenResult.Failure("PKPayment.token.paymentData was nil"))
                            return
                        }
                        val base64: String = data.base64EncodedStringWithOptions(0u.convert())
                        handler(
                            PKPaymentAuthorizationResult(
                                status = PKPaymentAuthorizationStatus.PKPaymentAuthorizationStatusSuccess,
                                errors = null,
                            ),
                        )
                        if (!deferred.isCompleted) deferred.complete(ApplePayTokenResult.Success(base64))
                    }

                    override fun paymentAuthorizationControllerDidFinish(controller: PKPaymentAuthorizationController) {
                        controller.dismissWithCompletion { openSheet.value = null }
                        if (!deferred.isCompleted) deferred.complete(ApplePayTokenResult.Cancelled)
                    }
                }
            openSheet.value = controller to delegate
            controller.delegate = delegate

            controller.presentWithCompletion { presented ->
                if (!presented) {
                    openSheet.value = null
                    if (!deferred.isCompleted) {
                        deferred.complete(ApplePayTokenResult.Failure("PKPaymentAuthorizationController.present returned false"))
                    }
                }
            }

            try {
                deferred.await()
            } catch (e: CancellationException) {
                // Dismiss the sheet so we don't leave the merchant app with a stuck modal.
                controller.dismissWithCompletion { openSheet.value = null }
                throw e
            }
        } finally {
            pending.compareAndSet(deferred, null)
        }
    }
}
