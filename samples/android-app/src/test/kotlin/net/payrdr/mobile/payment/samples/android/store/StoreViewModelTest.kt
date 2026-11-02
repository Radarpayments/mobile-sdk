package net.payrdr.mobile.payment.samples.android.store

import net.payrdr.mobile.payment.samples.android.checkout.CheckoutOutcome
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.Product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoreViewModelTest {
    private val mug = Product(id = "mug", name = 0, subtitle = null, priceMinor = 1800, image = null)

    @Test fun `carts are separate per cover`() {
        val vm = StoreViewModel()
        vm.add(CoverId.FURNITURE, mug)
        assertEquals(1, vm.cart(CoverId.FURNITURE).count)
        assertTrue(vm.cart(CoverId.COFFEE).isEmpty)
    }

    @Test fun `success clears the cart and keeps the receipt`() {
        val vm = StoreViewModel()
        vm.add(CoverId.FURNITURE, mug)
        vm.onOutcome(CoverId.FURNITURE, CheckoutOutcome.Success("p-1"), amountMinor = 1800)
        assertTrue(vm.cart(CoverId.FURNITURE).isEmpty)
        assertEquals(StoreViewModel.Receipt(CheckoutOutcome.Success("p-1"), 1800), vm.lastReceipt.value)
    }

    @Test fun `cancel keeps the cart and shows no result`() {
        val vm = StoreViewModel()
        vm.add(CoverId.FURNITURE, mug)
        vm.onOutcome(CoverId.FURNITURE, CheckoutOutcome.Cancelled, amountMinor = 1800)
        assertEquals(1, vm.cart(CoverId.FURNITURE).count)
        assertNull(vm.lastReceipt.value)
    }

    @Test fun `decline keeps the cart for a retry`() {
        val vm = StoreViewModel()
        vm.add(CoverId.TICKETS, mug)
        vm.onOutcome(CoverId.TICKETS, CheckoutOutcome.Declined("no"), amountMinor = 1800)
        assertEquals(1, vm.cart(CoverId.TICKETS).count)
    }

    @Test fun `a paid order can be repeated`() {
        val vm = StoreViewModel()
        vm.add(CoverId.COFFEE, mug)
        assertNull(vm.lastPaid(CoverId.COFFEE))
        vm.onOutcome(CoverId.COFFEE, CheckoutOutcome.Success("p-1"), amountMinor = 1800)
        assertEquals(1, vm.lastPaid(CoverId.COFFEE)?.count)
        vm.repeatLast(CoverId.COFFEE)
        assertEquals(1, vm.cart(CoverId.COFFEE).count)
    }

    @Test fun `order numbers start at 42 and grow with each paid order`() {
        val vm = StoreViewModel()
        vm.add(CoverId.COFFEE, mug)
        vm.onOutcome(CoverId.COFFEE, CheckoutOutcome.Success("p-1"), amountMinor = 1800)
        assertEquals(42, vm.lastOrderNumber.value)
        vm.repeatLast(CoverId.COFFEE)
        vm.onOutcome(CoverId.COFFEE, CheckoutOutcome.Success("p-2"), amountMinor = 1800)
        assertEquals(43, vm.lastOrderNumber.value)
    }

    @Test fun `replaceCart puts a whole order in the cart`() {
        val vm = StoreViewModel()
        vm.add(CoverId.TICKETS, mug)
        val order =
            net.payrdr.mobile.payment.samples.android.store.model
                .Cart()
                .add(mug)
                .add(mug)
        vm.replaceCart(CoverId.TICKETS, order)
        assertEquals(order, vm.cart(CoverId.TICKETS))
    }
}
