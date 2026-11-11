package net.payrdr.mobile.payment.sdk.payment.presentation

import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.ui.Step
import net.payrdr.mobile.payment.sdk.payment.ui.components.WalletAvailability

/**
 * Everything a renderer of the checkout draws from, as one value.
 *
 * @property step what is on screen.
 * @property revealKey the key of the step that takes the place of the loading one and so reveals its body in place
 * instead of sliding in; null after any other change.
 * @property forward the last change of [step] went deeper into the flow (the new page comes from the trailing side).
 * @property skeleton the rows the loading step draws.
 * @property skeletonVisible the loading step shows its skeleton (it stays blank for a session that answers fast).
 * @property zoomReveal the first step appears with a zoom: no skeleton was shown before it, so there is nothing to
 * cross-fade from.
 * @property shown false once the result is being handed over and the sheet has to slide away; the renderer calls
 * [CheckoutModel.exitFinished] when it is gone.
 * @property order the amount and the merchant, once the session has them.
 * @property transfer the order is a transfer, not a purchase.
 * @property saveCardOnly the order only saves the card.
 * @property canReturnToForm the failed result on screen offers another attempt.
 * @property wallets the wallets the current form offers on this device.
 * @property walletConfigured the merchant set up this platform's wallet, so room for its button is kept.
 * @property sheetPresentation every step is shown in the sheet; otherwise only the loading and the methods ones.
 * @property reduceMotion the device asks for less motion.
 */
@PayrdrUiApi
public data class CheckoutUi(
    public val step: Step,
    public val revealKey: String?,
    public val forward: Boolean,
    public val skeleton: SkeletonLayout,
    public val skeletonVisible: Boolean,
    public val zoomReveal: Boolean,
    public val shown: Boolean,
    public val order: OrderSummary?,
    public val transfer: Boolean,
    public val saveCardOnly: Boolean,
    public val canReturnToForm: Boolean,
    public val wallets: WalletAvailability,
    public val walletConfigured: Boolean,
    public val sheetPresentation: Boolean,
    public val reduceMotion: Boolean,
)
