package net.payrdr.mobile.payment.sdk.payment.ui.loading

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SavedCardCountCacheTest {
    private class MemoryStore : KeyValueStore {
        val values = mutableMapOf<String, Int>()

        override fun getInt(key: String): Int? = values[key]

        override fun putInt(
            key: String,
            value: Int,
        ) {
            values[key] = value
        }
    }

    private val store = MemoryStore()
    private val cache = SavedCardCountCache(store, "https://pay.test")

    @Test
    fun `no cards before the first session`() {
        cache.read() shouldBe 0
    }

    @Test
    fun `remembers the count of the last session`() {
        cache.write(2)
        cache.read() shouldBe 2
    }

    @Test
    fun `counts no more cards than the sheet lists`() {
        cache.write(5)
        cache.read() shouldBe 3
    }

    @Test
    fun `a negative count reads as none`() {
        cache.write(-1)
        cache.read() shouldBe 0
    }

    @Test
    fun `gateways keep their own counts`() {
        cache.write(2)
        SavedCardCountCache(store, "https://other.test").read() shouldBe 0
    }

    @Test
    fun `the wallet is not known before the first session`() {
        cache.readWalletOffered() shouldBe null
    }

    @Test
    fun `remembers whether the last session offered the wallet`() {
        cache.writeWalletOffered(false)
        cache.readWalletOffered() shouldBe false
        cache.writeWalletOffered(true)
        cache.readWalletOffered() shouldBe true
    }
}
