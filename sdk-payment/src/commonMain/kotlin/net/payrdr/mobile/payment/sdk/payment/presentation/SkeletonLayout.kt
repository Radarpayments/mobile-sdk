package net.payrdr.mobile.payment.sdk.payment.presentation

import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.ui.screens.hasPlatformWallet

/** How many saved cards the payment sheet lists; the rest are behind "All cards". */
@PayrdrUiApi
public const val SHEET_CARD_LIMIT: Int = 3

/** What the skeleton can know before the session: whether a wallet is configured, how many cards came last time. */
@PayrdrUiApi
public data class SkeletonLayout(
    public val wallet: Boolean,
    public val savedCards: Int,
)

/** Nothing to choose between: the first step will be the card form (as [rootRoute][net.payrdr.mobile.payment.sdk.payment.ui.rootRoute]). */
@PayrdrUiApi
public val SkeletonLayout.isCardFormRoot: Boolean get() = !wallet && savedCards == 0

/**
 * The skeleton for [config]: as many card rows as the last session had, and a wallet row when the wallet is configured
 * and the last session did not leave it out ([walletOffered] null: no session yet, so it is expected).
 */
internal fun skeletonLayout(
    config: SdkPaymentConfig,
    cachedCards: Int,
    walletOffered: Boolean? = null,
): SkeletonLayout =
    SkeletonLayout(
        wallet = config.hasPlatformWallet() && walletOffered != false,
        savedCards = cachedCards.coerceIn(0, SHEET_CARD_LIMIT),
    )
