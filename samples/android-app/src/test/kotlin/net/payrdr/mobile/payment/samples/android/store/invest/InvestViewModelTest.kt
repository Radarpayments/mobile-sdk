package net.payrdr.mobile.payment.samples.android.store.invest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutOutcome
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class InvestViewModelTest {
    init {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    private val saved = mutableListOf<InvestAccount>()
    private val vm = InvestViewModel(initial = InvestAccount.SEED, save = { saved += it }, clock = { 1_000L })

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test fun `it starts from the saved account, so a success it already credited is not credited again`() {
        val credited = InvestAccount.SEED.credit("md-1", 10000, 500L)
        val restored = InvestViewModel(initial = credited, save = { saved += it }, clock = { 1_000L })
        restored.onOutcome(CheckoutOutcome.Success("md-1"), 10000)
        assertEquals(credited, restored.account.value)
        assertEquals(emptyList(), saved)
    }

    @Test fun `it saves in its own scope, which outlives the screen that created it`() {
        // `save` suspends (DataStore): the view model launches it, nobody else's scope does.
        val vm =
            InvestViewModel(initial = InvestAccount.SEED, save = {
                yield()
                saved += it
            }, clock = { 1_000L })
        vm.onOutcome(CheckoutOutcome.Success("md-1"), 10000)
        assertEquals(11240, saved.single().freeCashMinor)
    }

    @Test fun `success credits the amount once, saves and shows done`() {
        vm.onOutcome(CheckoutOutcome.Success("md-1"), 10000)
        vm.onOutcome(CheckoutOutcome.Success("md-1"), 10000)
        assertEquals(11240, vm.account.value.freeCashMinor)
        assertEquals(11240, saved.last().freeCashMinor)
        assertEquals(TopUpStatus.Done("md-1", 10000, freeCashBefore = 1240, freeCashAfter = 11240, at = 1_000L), vm.status.value)
    }

    @Test fun `declined keeps the balance`() {
        vm.onOutcome(CheckoutOutcome.Declined("Issuer declined"), 10000)
        assertEquals(1240, vm.account.value.freeCashMinor)
        assertEquals(TopUpStatus.Declined("Issuer declined"), vm.status.value)
        assertEquals(emptyList(), saved)
    }

    @Test fun `unconfirmed keeps the balance and blocks continue`() {
        vm.onOutcome(CheckoutOutcome.Unconfirmed("md-2"), 10000)
        assertEquals(1240, vm.account.value.freeCashMinor)
        assertIs<TopUpStatus.Unconfirmed>(vm.status.value)
        assertEquals(false, vm.status.value.canContinue(vm.amount.value))
    }

    @Test fun `a saved card credits nothing and the top-up may go on`() {
        vm.onCardOutcome(CheckoutOutcome.Success("md-verify"))
        assertEquals(InvestAccount.SEED, vm.account.value)
        assertEquals(emptyList(), saved)
        assertEquals(TopUpStatus.CardAdded, vm.status.value)
        vm.edit("100")
        assertEquals(true, vm.status.value.canContinue(vm.amount.value))
    }

    @Test fun `a card the bank refused or did not confirm leaves the top-up open`() {
        vm.edit("100")
        vm.onCardOutcome(CheckoutOutcome.Declined("Issuer declined"))
        assertEquals(TopUpStatus.CardDeclined("Issuer declined"), vm.status.value)
        assertEquals(true, vm.status.value.canContinue(vm.amount.value))
        vm.onCardOutcome(CheckoutOutcome.Unconfirmed("md-verify"))
        assertEquals(TopUpStatus.CardUnconfirmed, vm.status.value)
        assertEquals(true, vm.status.value.canContinue(vm.amount.value))
        assertEquals(InvestAccount.SEED, vm.account.value)
    }

    @Test fun `cancel goes back to editing`() {
        vm.onOutcome(CheckoutOutcome.Declined(null), 10000)
        vm.onOutcome(CheckoutOutcome.Cancelled, 10000)
        assertEquals(TopUpStatus.Editing, vm.status.value)
    }

    @Test fun `leaving the top-up screen forgets its banner`() {
        vm.onOutcome(CheckoutOutcome.Unconfirmed("md-2"), 10000)
        vm.leaveTopUp()
        assertEquals(TopUpStatus.Editing, vm.status.value)
    }

    @Test fun `invest from done invests the top-up and saves`() {
        vm.onOutcome(CheckoutOutcome.Success("md-1"), 10000)
        vm.invest(10000)
        assertEquals(1240, vm.account.value.freeCashMinor)
        assertEquals(1240, saved.last().freeCashMinor)
    }

    @Test fun `the saved account replaces the shown one`() {
        val other = InvestAccount.SEED.copy(freeCashMinor = 5)
        vm.sync(other)
        assertEquals(other, vm.account.value)
    }

    @Test fun `the field and the presets edit the amount`() {
        vm.preset(25000)
        vm.edit("2505,5")
        assertEquals("2505.5", vm.amount.value.text)
        vm.preset(5000)
        assertEquals(5000L, vm.amount.value.minor)
    }
}
