package net.payrdr.mobile.payment.samples.android.checkout

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistration
import net.payrdr.mobile.payment.samples.android.merchant.merchantRegistrar
import net.payrdr.mobile.payment.samples.android.prefs.AppPrefs
import net.payrdr.mobile.payment.sdk.payment.android.PayrdrCheckout
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.http.defaultHttpClientFactory
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult

/**
 * The SDK from a screen that is not written in Compose: a plain Activity with a button. This is the whole
 * integration — a [PayrdrCheckout] built in a field, and [PayrdrCheckout.present] when the buyer taps "Pay".
 */
class ClassicCheckoutActivity : ComponentActivity() {
    // Built in a field: it registers an activity result, which Android allows only before the screen starts.
    private val checkout = PayrdrCheckout.Builder(SdkPaymentConfig(baseUrl = BASE_URL), ::onResult).build(this)

    private lateinit var status: TextView
    private lateinit var payButton: Button

    // One HTTP client for "your server" calls, however many times Pay is tapped.
    private val http by lazy { defaultHttpClientFactory().create(SdkPaymentConfig(baseUrl = BASE_URL)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { text = "Tap Pay to open the payment sheet." }
        payButton = Button(this).apply { text = "Pay €1.00" }
        payButton.setOnClickListener { pay() }
        setContentView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(PADDING, PADDING, PADDING, PADDING)
                addView(status)
                addView(payButton)
            },
        )
    }

    private fun pay() {
        // "Your server" registers the order; a real app gets the mdOrder from its backend. One order per tap.
        payButton.isEnabled = false
        lifecycleScope.launch {
            val snap = AppPrefs(this@ClassicCheckoutActivity).flow.first()
            val registrar =
                merchantRegistrar(
                    apiVersion = snap.apiVersion,
                    baseUrl = BASE_URL,
                    login = snap.merchantLogin,
                    password = snap.merchantPassword,
                    apiKey = snap.apiKey,
                    http = http,
                    orderType = snap.orderType,
                )
            // The developer-settings buyer, as in the stores: saved cards when the scenario has them.
            val clientId = snap.clientId.takeIf { snap.scenario.buyerWithCards }
            when (val registration = registrar.register(AMOUNT_MINOR, clientId)) {
                is OrderRegistration.Registered ->
                    registration.sessionId?.let(checkout::presentSession) ?: checkout.present(registration.mdOrder)
                else -> {
                    status.text = "Order not registered: $registration"
                    payButton.isEnabled = true
                }
            }
        }
    }

    private fun onResult(result: CheckoutResult) {
        payButton.isEnabled = true
        status.text =
            when (result) {
                is CheckoutResult.Completed -> "Paid, order ${result.orderId}"
                is CheckoutResult.Canceled -> "Canceled"
                is CheckoutResult.Failed -> "Failed: ${result.error.message}"
                is CheckoutResult.Unknown -> "Outcome unknown, order ${result.orderId}"
            }
    }

    private companion object {
        const val BASE_URL = AppPrefs.BASE_URL
        const val AMOUNT_MINOR = 100L
        const val PADDING = 48
    }
}
