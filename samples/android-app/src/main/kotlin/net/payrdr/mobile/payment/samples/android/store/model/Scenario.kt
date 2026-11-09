package net.payrdr.mobile.payment.samples.android.store.model

/**
 * The demo panel: what the next payment looks like, the same in every cover.
 *
 * @property buyerWithCards true = the order is registered with the demo buyer's clientId, so cards saved
 *   in earlier payments show up; false = a guest without saved cards.
 * @property wallet show Google Pay (when the merchant has it set up).
 * @property scanner show the card scanner on the card form.
 * @property sdkResultScreen let the SDK show its own success / declined screen before returning.
 */
data class Scenario(
    val buyerWithCards: Boolean = false,
    val wallet: Boolean = true,
    val scanner: Boolean = true,
    val sdkResultScreen: Boolean = false,
)

/** Which theme the SDK sheet uses: the cover's brand colours or a stock one. */
enum class SheetThemeOverride { COVER, LIGHT, DARK, SYSTEM }
