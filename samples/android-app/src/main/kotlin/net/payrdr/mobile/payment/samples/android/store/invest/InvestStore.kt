package net.payrdr.mobile.payment.samples.android.store.invest

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutState
import net.payrdr.mobile.payment.samples.android.checkout.rememberStoreCheckout
import net.payrdr.mobile.payment.samples.android.prefs.AppPrefs
import net.payrdr.mobile.payment.samples.android.prefs.rememberPrefs
import net.payrdr.mobile.payment.samples.android.store.LocalTransferRegistrarFactory
import net.payrdr.mobile.payment.samples.android.store.RegistrationError
import net.payrdr.mobile.payment.samples.android.store.ScenarioRow
import net.payrdr.mobile.payment.samples.android.store.ScenarioSheet
import net.payrdr.mobile.payment.samples.android.store.coverSdkConfig
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.theme.CoverTheme

/**
 * Northwind Invest, the AFT demo: portfolio → top-up amount → SDK sheet ("Transfer") → done. The transfer is
 * registered with `registerP2P.do` and the investor's details; the account is credited only on a completed transfer
 * and kept on the device. "Add card" registers a transfer of 0 that only saves the card. The investor is always the
 * demo buyer: an investment app knows its user, and a saved card shows up only for the clientId it was saved for.
 */
@Composable
fun InvestStore(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val (prefsState, prefs) = rememberPrefs()
    val snap = prefsState.value ?: return
    val scope = rememberCoroutineScope()
    val vm: InvestViewModel = viewModel { InvestViewModel(initial = snap.investAccount, save = { prefs.setInvestAccount(it) }) }
    LaunchedEffect(snap.investAccount) { vm.sync(snap.investAccount) }
    val account by vm.account.collectAsState()
    val status by vm.status.collectAsState()
    var screen by rememberSaveable { mutableStateOf(Screen.PORTFOLIO) }
    // The amount of the transfer in the sheet: the keypad may not change it meanwhile, but keep what was sent.
    var sentMinor by rememberSaveable { mutableLongStateOf(0L) }
    // The sheet is open for "Add card": its result saves a card, it credits nothing.
    var addingCard by rememberSaveable { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val factory = LocalTransferRegistrarFactory.current
    val registrar =
        remember(snap.baseUrl, snap.apiVersion, snap.orderType, snap.merchantLogin, snap.merchantPassword) {
            factory(snap, NorthwindProfile.prefill)
        }
    val checkout =
        rememberStoreCheckout(
            config = coverSdkConfig(CoverId.INVEST, snap),
            registrar = registrar,
            onMdOrder = { md -> scope.launch { prefs.setLastMdOrder(md) } },
            onOutcome = { outcome ->
                if (addingCard) vm.onCardOutcome(outcome) else vm.onOutcome(outcome, sentMinor)
                if (vm.status.value is TopUpStatus.Done) screen = Screen.DONE
            },
        )
    val checkoutState by checkout.state.collectAsState()

    fun toPortfolio() {
        vm.leaveTopUp()
        now = System.currentTimeMillis()
        screen = Screen.PORTFOLIO
    }
    BackHandler(enabled = screen != Screen.PORTFOLIO) { toPortfolio() }

    CoverTheme(CoverId.INVEST) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS)) },
            label = "invest",
        ) { target ->
            when (target) {
                Screen.PORTFOLIO ->
                    PortfolioScreen(
                        account = account,
                        now = now,
                        onBack = onBack,
                        onTopUp = { screen = Screen.TOP_UP },
                        onInvest = { vm.invest(account.freeCashMinor) },
                    )
                Screen.TOP_UP ->
                    TopUpScreen(
                        vm = vm,
                        checkout = checkoutState,
                        addingCard = addingCard,
                        onBack = ::toPortfolio,
                        onContinue = {
                            val minor = vm.amount.value.minor ?: return@TopUpScreen
                            sentMinor = minor
                            addingCard = false
                            checkout.pay(minor, snap.clientId)
                        },
                        onAddCard = {
                            addingCard = true
                            checkout.pay(0, snap.clientId)
                        },
                    ) { TopUpFooter(snap, prefs, checkoutState, onOpenSettings) }
                Screen.DONE ->
                    (status as? TopUpStatus.Done)?.let { done ->
                        TopUpDoneScreen(
                            done = done,
                            canInvest = account.canInvest,
                            onInvest = {
                                vm.invest(done.amountMinor)
                                toPortfolio()
                            },
                            onPortfolio = ::toPortfolio,
                        )
                    } ?: LaunchedEffect(Unit) { screen = Screen.PORTFOLIO }
            }
        }
    }
}

/** The demo panel row and why the order was not created, if it was not: as above every store's "Pay" button. */
@Composable
private fun TopUpFooter(
    snap: AppPrefs.Snapshot,
    prefs: AppPrefs,
    checkout: CheckoutState,
    onOpenSettings: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    (checkout as? CheckoutState.RegistrationFailed)?.let { RegistrationError(CoverId.INVEST, it.reason, onOpenSettings) }
    ScenarioRow(CoverId.INVEST, buyerWithCards = true) { sheetOpen = true }
    if (sheetOpen) {
        ScenarioSheet(
            scenario = snap.scenario,
            buyerChoice = false,
            onChange = { scope.launch { prefs.setScenario(it) } },
            onResetBuyer = { scope.launch { prefs.resetClientId() } },
            onDismiss = { sheetOpen = false },
        )
    }
}

private enum class Screen { PORTFOLIO, TOP_UP, DONE }

private const val FADE_MS = 180
