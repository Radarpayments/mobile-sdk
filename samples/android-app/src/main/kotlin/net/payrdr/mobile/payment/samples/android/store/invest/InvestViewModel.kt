package net.payrdr.mobile.payment.samples.android.store.invest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutOutcome

/**
 * Northwind Invest: the account shown (the saved one, see [sync]), the amount on the top-up keypad and how the last
 * transfer ended. Money is credited only when the SDK reports the transfer completed.
 *
 * @param initial the account saved on the device: a result the sheet delivers right after the process restarts is
 *   credited to it, not to the seed
 * @param save writes the account; run in this view model's scope, which outlives the screens that come and go with
 *   rotation or a theme change
 */
class InvestViewModel(
    initial: InvestAccount,
    private val save: suspend (InvestAccount) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val mutableAccount = MutableStateFlow(initial)
    private val mutableAmount = MutableStateFlow(TopUpAmount())
    private val mutableStatus = MutableStateFlow<TopUpStatus>(TopUpStatus.Editing)

    val account: StateFlow<InvestAccount> = mutableAccount.asStateFlow()
    val amount: StateFlow<TopUpAmount> = mutableAmount.asStateFlow()
    val status: StateFlow<TopUpStatus> = mutableStatus.asStateFlow()

    /** The account as saved on the device (it changes after a reset in the developer settings). */
    fun sync(saved: InvestAccount) {
        mutableAccount.value = saved
    }

    /** The amount field changed. */
    fun edit(input: String) {
        mutableAmount.value = TopUpAmount.parse(input)
    }

    /** A preset chip. */
    fun preset(minor: Long) {
        mutableAmount.value = TopUpAmount.preset(minor)
    }

    /** The sheet closed after a transfer of [amountMinor]. */
    fun onOutcome(
        outcome: CheckoutOutcome,
        amountMinor: Long,
    ) {
        mutableStatus.value =
            when (outcome) {
                is CheckoutOutcome.Success -> {
                    val before = mutableAccount.value
                    val after = before.credit(outcome.paymentId, amountMinor, clock())
                    if (after == before) return
                    update(after)
                    TopUpStatus.Done(outcome.paymentId, amountMinor, before.freeCashMinor, after.freeCashMinor, clock())
                }
                is CheckoutOutcome.Declined -> TopUpStatus.Declined(outcome.message)
                is CheckoutOutcome.Unconfirmed -> TopUpStatus.Unconfirmed
                CheckoutOutcome.Cancelled -> TopUpStatus.Editing
            }
    }

    /** Buys the Global ETF for [amountMinor] (at most the free cash), without a payment. */
    fun invest(amountMinor: Long) {
        update(mutableAccount.value.invest(amountMinor, clock()))
    }

    /** The buyer left the top-up screen: its banner goes, the amount stays. */
    fun leaveTopUp() {
        mutableStatus.value = TopUpStatus.Editing
    }

    private fun update(account: InvestAccount) {
        if (account == mutableAccount.value) return
        mutableAccount.value = account
        viewModelScope.launch { save(account) }
    }
}
