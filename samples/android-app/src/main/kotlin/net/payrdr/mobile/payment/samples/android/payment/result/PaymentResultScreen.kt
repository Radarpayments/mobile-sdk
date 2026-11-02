package net.payrdr.mobile.payment.samples.android.payment.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.samples.android.ui.components.NavTopBar
import net.payrdr.mobile.payment.samples.android.ui.components.PrimaryCta
import net.payrdr.mobile.payment.samples.android.ui.components.ResultHero
import net.payrdr.mobile.payment.sdk.payment.result.CheckoutResult

@Composable
fun PaymentResultScreen(onBack: () -> Unit) {
    val captured = remember { LastPaymentResult.readAndClear() }

    Surface {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            NavTopBar(title = "Result", subtitle = "Payment outcome", onBack = null)
            Column(Modifier.padding(16.dp)) {
                if (captured == null) {
                    ResultHero(
                        isSuccess = false,
                        title = "No result",
                        subtitle = "Start a payment from one of the sub-screens",
                        rows = emptyList(),
                    )
                } else {
                    val (result, durationMs, method) = captured
                    val completed = result is CheckoutResult.Completed
                    val rows =
                        listOf(
                            "Status" to resultStatus(result!!),
                            "Payment ID" to result.orderId,
                            "Method" to method,
                            "Duration" to "${durationMs}ms",
                        )
                    ResultHero(
                        isSuccess = completed,
                        title = resultTitle(result),
                        subtitle =
                            when (result) {
                                is CheckoutResult.Completed -> "Completed"
                                is CheckoutResult.Canceled -> "The buyer closed the checkout"
                                is CheckoutResult.Unknown -> "The checkout could not learn how the payment ended"
                                is CheckoutResult.Failed -> result.error.message ?: "Failed"
                            },
                        rows = rows,
                    )
                }
                Spacer(Modifier.height(14.dp))
                PrimaryCta(text = "Start another payment", onClick = onBack)
            }
        }
    }
}

private fun resultStatus(result: CheckoutResult): String =
    when (result) {
        is CheckoutResult.Completed -> "APPROVED"
        is CheckoutResult.Failed -> "DECLINED"
        is CheckoutResult.Canceled -> "CANCELED"
        is CheckoutResult.Unknown -> "UNKNOWN"
    }

private fun resultTitle(result: CheckoutResult): String =
    when (result) {
        is CheckoutResult.Completed -> "Payment approved"
        is CheckoutResult.Canceled -> "Payment cancelled"
        is CheckoutResult.Unknown -> "Outcome unknown — check the order status"
        is CheckoutResult.Failed -> "Payment failed (${result.error::class.simpleName})"
    }
