package net.payrdr.mobile.payment.samples.android.checkout

import android.content.Context
import android.content.Intent
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
 * integration — a [PayrdrCheckout] built in `onCreate`, and [PayrdrCheckout.present] when the buyer taps "Pay".
 * An app with a fixed gateway can build it in a field instead; this one takes the gateway of the developer settings.
 */
class ClassicCheckoutActivity : ComponentActivity() {
    private lateinit var config: SdkPaymentConfig
    private lateinit var checkout: PayrdrCheckout

    private lateinit var status: TextView
    private lateinit var payButton: Button

    // One HTTP client for "your server" calls, however many times Pay is tapped.
    private val http by lazy { defaultHttpClientFactory().create(config) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = SdkPaymentConfig(baseUrl = intent.getStringExtra(EXTRA_BASE_URL) ?: AppPrefs.BASE_URL)
        // Before the screen starts: it registers an activity result, which Android allows only until then.
        checkout = PayrdrCheckout.Builder(config, ::onResult).build(this)
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
                    baseUrl = config.baseUrl,
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

    companion object {
        private const val EXTRA_BASE_URL = "base_url"
        private const val AMOUNT_MINOR = 100L
        private const val PADDING = 48

        /** Opens the screen paying through [baseUrl], the gateway the rest of the app uses. */
        fun intent(
            context: Context,
            baseUrl: String,
        ): Intent = Intent(context, ClassicCheckoutActivity::class.java).putExtra(EXTRA_BASE_URL, baseUrl)
    }
}
