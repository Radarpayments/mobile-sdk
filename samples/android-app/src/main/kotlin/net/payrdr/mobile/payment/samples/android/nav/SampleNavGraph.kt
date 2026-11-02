package net.payrdr.mobile.payment.samples.android.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutOutcome
import net.payrdr.mobile.payment.samples.android.core.SdkCoreScreen
import net.payrdr.mobile.payment.samples.android.dev.DeveloperScreen
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistrar
import net.payrdr.mobile.payment.samples.android.payment.advanced.AdvancedScreen
import net.payrdr.mobile.payment.samples.android.payment.advanced.HeadlessFlowScreen
import net.payrdr.mobile.payment.samples.android.payment.result.PaymentResultScreen
import net.payrdr.mobile.payment.samples.android.prefs.AppPrefs
import net.payrdr.mobile.payment.samples.android.prefs.rememberPrefs
import net.payrdr.mobile.payment.samples.android.store.LocalOrderRegistrarFactory
import net.payrdr.mobile.payment.samples.android.store.StartScreen
import net.payrdr.mobile.payment.samples.android.store.StoreResultScreen
import net.payrdr.mobile.payment.samples.android.store.StoreViewModel
import net.payrdr.mobile.payment.samples.android.store.coffee.CoffeeOrderNumber
import net.payrdr.mobile.payment.samples.android.store.coffee.CoffeeStore
import net.payrdr.mobile.payment.samples.android.store.furniture.FurnitureStore
import net.payrdr.mobile.payment.samples.android.store.invest.InvestStore
import net.payrdr.mobile.payment.samples.android.store.invest.coverFits
import net.payrdr.mobile.payment.samples.android.store.invest.isAftDemo
import net.payrdr.mobile.payment.samples.android.store.invest.reopenCover
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.tickets.TicketsStore

/** The demo app's navigation; [registrarFactory] lets tests replace "your server". */
@Composable
fun SampleNavHost(registrarFactory: ((AppPrefs.Snapshot) -> OrderRegistrar)? = null) {
    if (registrarFactory != null) {
        CompositionLocalProvider(LocalOrderRegistrarFactory provides registrarFactory) { SampleNavGraph() }
    } else {
        SampleNavGraph()
    }
}

@Composable
private fun SampleNavGraph() {
    val nav: NavHostController = rememberNavController()
    val store: StoreViewModel = viewModel()
    val (prefsState, prefs) = rememberPrefs()
    val scope = rememberCoroutineScope()
    var reopened by rememberSaveable { mutableStateOf(false) }
    NavHost(navController = nav, startDestination = Routes.START) {
        composable(Routes.START) {
            // The last business opens straight away, once per launch.
            val snap = prefsState.value
            LaunchedEffect(snap != null) {
                if (snap != null && !reopened) {
                    reopened = true
                    reopenCover(snap.lastCover, isAftDemo(snap.apiVersion, snap.orderType))?.let { nav.navigate(Routes.cover(it)) }
                }
            }
            StartScreen(
                aftDemo = snap?.let { isAftDemo(it.apiVersion, it.orderType) } ?: false,
                onCover = { cover ->
                    scope.launch { prefs.setLastCover(cover) }
                    nav.navigate(Routes.cover(cover))
                },
                onSettings = { nav.navigate(Routes.DEV) },
                onCore = { nav.navigate(Routes.CORE) },
            )
        }
        composable(Routes.COVER) { entry ->
            val cover = entry.coverArg()
            // Back from the settings in the other mode: this business no longer fits it, the start screen shows which do.
            val snap = prefsState.value
            val fits = snap == null || coverFits(cover, isAftDemo(snap.apiVersion, snap.orderType))
            LaunchedEffect(fits) { if (!fits) nav.popBackStack(Routes.START, inclusive = false) }
            if (fits) {
                CoverRoutes(
                    cover = cover,
                    store = store,
                    nav = nav,
                    onBackToStart = {
                        scope.launch { prefs.setLastCover(null) }
                        nav.popBackStack(Routes.START, inclusive = false)
                    },
                )
            }
        }
        composable(Routes.RESULT) { entry ->
            val cover = entry.coverArg()
            val receipt by store.lastReceipt.collectAsState()
            val orderNumber by store.lastOrderNumber.collectAsState()
            receipt?.let {
                StoreResultScreen(
                    cover = cover,
                    receipt = it,
                    headline = if (cover == CoverId.COFFEE) ({ CoffeeOrderNumber(orderNumber) }) else null,
                    onDone = { nav.popBackStack(Routes.cover(cover), inclusive = false) },
                    onRetry = { nav.popBackStack() },
                )
            }
        }
        composable(Routes.DEV) {
            DeveloperScreen(
                onBack = { nav.popBackStack() },
                onAdvanced = { nav.navigate(Routes.ADVANCED) },
            )
        }
        composable(Routes.CORE) { SdkCoreScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.ADVANCED) {
            AdvancedScreen(
                onBack = { nav.popBackStack() },
                onNavigate = { route -> nav.navigate(route) },
                onPay = { nav.navigate(Routes.DEV_RESULT) },
            )
        }
        composable(Routes.HEADLESS) { HeadlessFlowScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.DEV_RESULT) { PaymentResultScreen(onBack = { nav.popBackStack() }) }
    }
}

/** The screens of one business; each cover's own flow plugs in here. */
@Composable
private fun CoverRoutes(
    cover: CoverId,
    store: StoreViewModel,
    nav: NavHostController,
    onBackToStart: () -> Unit,
) {
    val onOutcome: (CheckoutOutcome, Long) -> Unit = { outcome, amount ->
        store.onOutcome(cover, outcome, amount)
        if (outcome !is CheckoutOutcome.Cancelled) nav.navigate(Routes.result(cover))
    }
    val onOpenSettings = { nav.navigate(Routes.DEV) }
    when (cover) {
        CoverId.FURNITURE -> FurnitureStore(store, onBackToStart, onOutcome, onOpenSettings)
        CoverId.COFFEE -> CoffeeStore(store, onBackToStart, onOutcome, onOpenSettings)
        CoverId.TICKETS -> TicketsStore(store, onBackToStart, onOutcome, onOpenSettings)
        CoverId.INVEST -> InvestStore(onBackToStart, onOpenSettings)
    }
}

private fun NavBackStackEntry.coverArg(): CoverId = CoverId.valueOf(arguments?.getString("cover") ?: CoverId.FURNITURE.name)
