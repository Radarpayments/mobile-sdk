package net.payrdr.mobile.payment.sdk.payment.ui.loading

import net.payrdr.mobile.payment.sdk.payment.presentation.SHEET_CARD_LIMIT

/** A small on-device store of numbers. */
internal interface KeyValueStore {
    fun getInt(key: String): Int?

    fun putInt(
        key: String,
        value: Int,
    )
}

/**
 * How many saved cards the last session on this device had, and whether it offered the wallet, so the skeleton can draw
 * the same rows. Only the count and the flag are kept, per gateway; at most as many cards as the sheet lists.
 */
internal class SavedCardCountCache(
    private val store: KeyValueStore,
    baseUrl: String,
) {
    private val key = "payrdr.savedCardCount.$baseUrl"
    private val walletKey = "payrdr.walletOffered.$baseUrl"

    fun read(): Int = (store.getInt(key) ?: 0).coerceIn(0, SHEET_CARD_LIMIT)

    fun write(count: Int) = store.putInt(key, count.coerceIn(0, SHEET_CARD_LIMIT))

    /** Whether the last session offered this platform's wallet; null before the first session. */
    fun readWalletOffered(): Boolean? = store.getInt(walletKey)?.let { it != 0 }

    fun writeWalletOffered(offered: Boolean) = store.putInt(walletKey, if (offered) 1 else 0)
}
