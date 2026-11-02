package net.payrdr.mobile.payment.samples.android.checkout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistrar
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistration
import net.payrdr.mobile.payment.samples.android.store.model.Scenario
import net.payrdr.mobile.payment.samples.android.store.model.SheetThemeOverride
import net.payrdr.mobile.payment.sdk.payment.android.rememberPayrdrCheckout
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult
import net.payrdr.mobile.payment.sdk.payment.scanner.provider.MlKitVisionCardScannerProvider
import net.payrdr.mobile.payment.sdk.payment.ui.theme.PaymentTheme

/*
 * Everything this demo does with the payment SDK lives in this file. To take payments in your app:
 *
 *   1. Describe the checkout:         SdkPaymentConfig(baseUrl = ..., theme = ..., googlePay = ...)
 *   2. Get the order.                 Your backend registers it at the gateway — an mdOrder through API V1
 *      (register.do), a session id through API V2 (POST /api2/sessions); this demo does it in-app through
 *      OrderRegistrar only so that it runs without a backend.
 *   3. Open the payment sheet:        rememberPayrdrCheckout(config) { result -> ... }.present(mdOrder)
 *                                     or .presentSession(sessionId)
 *                                     (a screen without Compose: PayrdrCheckout.Builder(config) { ... }.build(this),
 *                                     see ClassicCheckoutActivity)
 *   4. Handle the result:             completed, failed, canceled or unknown — see toOutcome().
 */

/** What the store does after the payment sheet closes. */
sealed interface CheckoutOutcome {
    /** Paid; [paymentId] identifies the payment at the gateway. */
    data class Success(
        val paymentId: String,
    ) : CheckoutOutcome

    /** The bank or the gateway refused the payment. */
    data class Declined(
        val message: String?,
    ) : CheckoutOutcome

    /** The buyer closed the sheet. */
    data object Cancelled : CheckoutOutcome

    /** The SDK could not learn how the payment ended; the store must not offer to pay again until it knows. */
    data class Unconfirmed(
        val paymentId: String,
    ) : CheckoutOutcome
}

/** Where the store's "Pay" button is in the payment path. */
sealed interface CheckoutState {
    data object Idle : CheckoutState

    /** Step 2: the order is being registered. */
    data object Registering : CheckoutState

    /** Step 3: the payment sheet is open. */
    data object InSheet : CheckoutState

    /** Step 2 failed; the sheet was not opened. */
    data class RegistrationFailed(
        val reason: OrderRegistration,
    ) : CheckoutState
}

/** Step 1: the SDK config for the current demo panel, cover theme and developer settings. */
fun buildSdkConfig(
    baseUrl: String,
    scenario: Scenario,
    coverTheme: PaymentTheme,
    themeOverride: SheetThemeOverride,
    locale: String?,
    googlePay: GooglePayConfig?,
): SdkPaymentConfig =
    SdkPaymentConfig(
        baseUrl = baseUrl,
        locale = locale,
        theme =
            when (themeOverride) {
                SheetThemeOverride.COVER -> coverTheme
                SheetThemeOverride.LIGHT -> PaymentTheme.Light
                SheetThemeOverride.DARK -> PaymentTheme.Dark
                SheetThemeOverride.SYSTEM -> PaymentTheme.System
            },
        googlePay = googlePay.takeIf { scenario.wallet },
        cardScannerProvider = cardScanner.takeIf { scenario.scanner },
        showResultScreen = scenario.sdkResultScreen,
    )

// One provider for every config, so configs that differ only in other fields still compare equal.
private val cardScanner by lazy { MlKitVisionCardScannerProvider() }

/** Step 4: turns the SDK result into what the store shows next. */
fun CheckoutResult.toOutcome(): CheckoutOutcome =
    when (this) {
        is CheckoutResult.Completed -> CheckoutOutcome.Success(orderId)
        is CheckoutResult.Canceled -> CheckoutOutcome.Cancelled
        is CheckoutResult.Failed -> CheckoutOutcome.Declined(error.message)
        // How the payment ended is not known yet: a real store checks the order on its server (or waits for the
        // gateway's callback) before telling the buyer anything, and never offers to pay again meanwhile.
        is CheckoutResult.Unknown -> CheckoutOutcome.Unconfirmed(orderId)
    }

/**
 * Steps 2 and 3 for the store's "Pay" button: registers the order, then opens the sheet. A second tap
 * while the order is being registered or the sheet is open does nothing.
 */
class StoreCheckout internal constructor(
    private val registrar: OrderRegistrar,
    private val scope: CoroutineScope,
    private val launch: (OrderRegistration.Registered) -> Unit,
    private val onMdOrder: (String) -> Unit,
) {
    private val mutableState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)

    val state: StateFlow<CheckoutState> = mutableState.asStateFlow()

    /** Pays [amountMinor] euro cents; [clientId] = the demo buyer with saved cards, null = guest. */
    fun pay(
        amountMinor: Long,
        clientId: String?,
    ) {
        if (mutableState.value == CheckoutState.Registering || mutableState.value == CheckoutState.InSheet) return
        mutableState.value = CheckoutState.Registering
        scope.launch {
            when (val registration = registrar.register(amountMinor, clientId)) {
                is OrderRegistration.Registered -> {
                    onMdOrder(registration.mdOrder)
                    mutableState.value = CheckoutState.InSheet
                    launch(registration)
                }
                else -> mutableState.value = CheckoutState.RegistrationFailed(registration)
            }
        }
    }

    /** The sheet returned a result. */
    fun onSheetClosed() {
        mutableState.value = CheckoutState.Idle
    }
}

/**
 * Wires [StoreCheckout] to the SDK. [onMdOrder] sees every registered order (the demo shows the last one
 * in the developer settings); [onOutcome] gets the result of each payment.
 */
@Composable
fun rememberStoreCheckout(
    config: SdkPaymentConfig,
    registrar: OrderRegistrar,
    onMdOrder: (String) -> Unit,
    onOutcome: (CheckoutOutcome) -> Unit,
): StoreCheckout {
    val scope = rememberCoroutineScope()
    val currentRegistrar = rememberUpdatedState(registrar)
    val currentOnMdOrder = rememberUpdatedState(onMdOrder)
    val currentOnOutcome = rememberUpdatedState(onOutcome)
    lateinit var checkout: StoreCheckout
    // Steps 1, 3 and 4.
    val payrdr =
        rememberPayrdrCheckout(config) { result ->
            checkout.onSheetClosed()
            currentOnOutcome.value(result.toOutcome())
        }
    checkout =
        remember {
            StoreCheckout(
                registrar = { amount, clientId -> currentRegistrar.value.register(amount, clientId) },
                scope = scope,
                launch = { order -> order.sessionId?.let(payrdr::presentSession) ?: payrdr.present(order.mdOrder) },
                onMdOrder = { currentOnMdOrder.value(it) },
            )
        }
    return checkout
}
