package net.payrdr.mobile.payment.samples.android.store.invest

import net.payrdr.mobile.payment.samples.android.merchant.ApiVersion
import net.payrdr.mobile.payment.samples.android.merchant.OrderType
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InvestModeTest {
    @Test fun `the AFT demo is API V1 with AFT orders`() {
        assertTrue(isAftDemo(ApiVersion.V1, OrderType.AFT))
        assertFalse(isAftDemo(ApiVersion.V2, OrderType.AFT))
        assertFalse(isAftDemo(ApiVersion.V1, OrderType.PURCHASE))
    }

    @Test fun `the AFT demo shows only Northwind, otherwise the stores`() {
        assertEquals(listOf(CoverId.INVEST), visibleCovers(aftDemo = true))
        assertEquals(listOf(CoverId.FURNITURE, CoverId.COFFEE, CoverId.TICKETS), visibleCovers(aftDemo = false))
    }

    @Test fun `the last business reopens only when the mode shows it`() {
        assertNull(reopenCover(CoverId.FURNITURE, aftDemo = true))
        assertNull(reopenCover(CoverId.INVEST, aftDemo = false))
        assertEquals(CoverId.INVEST, reopenCover(CoverId.INVEST, aftDemo = true))
        assertEquals(CoverId.COFFEE, reopenCover(CoverId.COFFEE, aftDemo = false))
        assertNull(reopenCover(null, aftDemo = true))
    }

    @Test fun `an open business stays only while the mode still shows it`() {
        assertTrue(coverFits(CoverId.INVEST, aftDemo = true))
        assertFalse(coverFits(CoverId.INVEST, aftDemo = false))
        assertFalse(coverFits(CoverId.TICKETS, aftDemo = true))
        assertTrue(coverFits(CoverId.TICKETS, aftDemo = false))
    }
}
