package net.payrdr.mobile.payment.samples.android.store.invest

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvestAccountTest {
    private val seed = InvestAccount.SEED
    private val now = 1_793_280_000_000L

    @Test fun `seed totals 4812_40`() {
        assertEquals(481240, seed.totalMinor)
        assertEquals(1240, seed.freeCashMinor)
    }

    @Test fun `credit adds to free cash and prepends a top-up`() {
        val account = seed.credit("md-1", 10000, now)
        assertEquals(11240, account.freeCashMinor)
        assertEquals(491240, account.totalMinor)
        val op = account.operations.first()
        assertEquals(Operation.Kind.TOP_UP, op.kind)
        assertEquals("md-1", op.orderId)
        assertEquals(10000, op.amountMinor)
        assertEquals(now, op.atEpochMillis)
    }

    @Test fun `credit twice with the same order credits once`() {
        val account = seed.credit("md-1", 10000, now).credit("md-1", 10000, now + 1)
        assertEquals(11240, account.freeCashMinor)
        assertEquals(seed.operations.size + 1, account.operations.size)
    }

    @Test fun `invest moves cash into GLB`() {
        val account = seed.credit("md-1", 10000, now).invest(10000, now + 1)
        assertEquals(1240, account.freeCashMinor)
        assertEquals(300000, account.holdings.single { it.ticker == "GLB" }.valueMinor)
        assertEquals(491240, account.totalMinor)
        val op = account.operations.first()
        assertEquals(Operation.Kind.BUY, op.kind)
        assertEquals(10000, op.amountMinor)
        assertEquals("GLB", op.ticker)
    }

    @Test fun `invest more than free cash invests all free cash`() {
        val account = seed.invest(1_000_000, now)
        assertEquals(0, account.freeCashMinor)
        assertEquals(291240, account.holdings.single { it.ticker == "GLB" }.valueMinor)
    }

    @Test fun `invest with under one euro does nothing`() {
        val poor = seed.copy(freeCashMinor = 99)
        assertFalse(poor.canInvest)
        assertEquals(poor, poor.invest(99, now))
        assertTrue(seed.canInvest)
    }

    @Test fun `recent operations are newest first and capped`() {
        var account = seed
        repeat(6) { account = account.credit("md-$it", 1000, now + it) }
        val recent = account.recent()
        assertEquals(5, recent.size)
        assertEquals("md-5", recent.first().orderId)
    }

    @Test fun `seed operations are a buy and a top-up, newest first`() {
        assertEquals(listOf(Operation.Kind.BUY, Operation.Kind.TOP_UP), seed.operations.map { it.kind })
        assertEquals(listOf(25000L, 25000L), seed.operations.map { it.amountMinor })
    }

    @Test fun `json round trip`() {
        val account = seed.credit("md-1", 10000, now).invest(5000, now + 1)
        assertEquals(account, InvestAccount.fromJson(account.toJson()))
    }

    @Test fun `corrupt json gives the seed`() {
        listOf(null, "", "{", """{"freeCashMinor":"x"}""", "[]", """{"freeCashMinor":1}""").forEach {
            assertEquals(seed, InvestAccount.fromJson(it), "json: $it")
        }
    }
}
