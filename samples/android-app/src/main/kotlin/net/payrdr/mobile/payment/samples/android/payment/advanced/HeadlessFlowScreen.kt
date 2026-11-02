package net.payrdr.mobile.payment.samples.android.payment.advanced

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistration
import net.payrdr.mobile.payment.samples.android.payment.composition.rememberSdkPayment
import net.payrdr.mobile.payment.samples.android.prefs.rememberPrefs
import net.payrdr.mobile.payment.samples.android.store.LocalOrderRegistrarFactory
import net.payrdr.mobile.payment.samples.android.ui.components.NavTopBar
import net.payrdr.mobile.payment.samples.android.ui.components.PrimaryCta
import net.payrdr.mobile.payment.sdk.payment.config.CheckoutConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.result.PaymentState
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsChallenge
import net.payrdr.mobile.payment.sdk.payment.threeds.ThreeDsResult
import net.payrdr.mobile.payment.sdk.payment.threeds.WebChallengeHost

@Composable
fun HeadlessFlowScreen(onBack: () -> Unit) {
    val (prefsState, _) = rememberPrefs()
    val snap = prefsState.value ?: return
    val sdkConfig = SdkPaymentConfig(baseUrl = snap.baseUrl)
    val sdk = rememberSdkPayment(sdkConfig)
    val factory = LocalOrderRegistrarFactory.current
    val registrar =
        remember(snap.baseUrl, snap.apiVersion, snap.orderType, snap.merchantLogin, snap.merchantPassword, snap.apiKey) { factory(snap) }

    var trace by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Surface {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            NavTopBar(title = "Headless flow", subtitle = "Live PaymentState stream", onBack = onBack)
            Column(Modifier.padding(16.dp)) {
                PrimaryCta(text = "Start headless flow", onClick = {
                    trace = emptyList()
                    scope.launch {
                        // A fresh order each run: an mdOrder can be paid only once.
                        val order =
                            when (val registration = registrar.register(DEMO_AMOUNT_MINOR, null)) {
                                is OrderRegistration.Registered -> registration
                                else -> {
                                    trace = listOf("Order not registered: $registration")
                                    return@launch
                                }
                            }
                        val host = NoopWebChallengeHost()
                        val flow: Flow<PaymentState> =
                            sdk.paymentFlow(
                                checkout = order.sessionId?.let { CheckoutConfig.SessionId(it) } ?: CheckoutConfig.MdOrder(order.mdOrder),
                                webChallengeHost = host,
                                scope = MainScope(),
                            )
                        flow.collect { s ->
                            trace = trace + s::class.simpleName.orEmpty()
                        }
                    }
                })
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0B1220))
                        .padding(12.dp),
                ) {
                    Text("PaymentState trace", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    if (trace.isEmpty()) {
                        Text("(no events yet)", color = Color(0xFF64748B), fontSize = 10.sp)
                    } else {
                        trace.forEachIndexed { i, name ->
                            Text(
                                "${i + 1}. $name",
                                color = Color(0xFFA5B4FC),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Stub host — headless demo does not actually present a 3DS challenge UI.
 * Returns [ThreeDsResult.Cancelled] so the FSM advances to a terminal state without touching a WebView.
 */
private class NoopWebChallengeHost : WebChallengeHost {
    override suspend fun performChallenge(challenge: ThreeDsChallenge): ThreeDsResult = ThreeDsResult.Cancelled
}

private const val DEMO_AMOUNT_MINOR = 100L
