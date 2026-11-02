package net.payrdr.mobile.payment.samples.android.payment.advanced

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.Lock
import net.payrdr.mobile.payment.samples.android.checkout.ClassicCheckoutActivity
import net.payrdr.mobile.payment.samples.android.nav.Routes
import net.payrdr.mobile.payment.samples.android.payment.result.LastPaymentResult
import net.payrdr.mobile.payment.samples.android.prefs.rememberPrefs
import net.payrdr.mobile.payment.samples.android.ui.components.NavTopBar
import net.payrdr.mobile.payment.samples.android.ui.components.PrimaryCta
import net.payrdr.mobile.payment.sdk.payment.android.rememberPayrdrCheckout
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.ssl.SslPinningConfig

@Composable
fun AdvancedScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onPay: () -> Unit,
) {
    val (prefsState, _) = rememberPrefs()
    val snap = prefsState.value ?: return

    Surface {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            NavTopBar(title = "Advanced", subtitle = "4 scenarios", onBack = onBack)
            Column(Modifier.padding(16.dp)) {
                Placeholder(
                    title = "🔤 Custom button text",
                    body = "`buttonText` config is not in 4.x SdkPaymentConfig. Control happens gateway-side via session template.",
                )
                Spacer(Modifier.height(10.dp))
                ActiveTile(
                    title = "🔀 Headless payment flow",
                    body = "Sample collects `SdkPayment.paymentFlow()` stream and renders FSM states live.",
                    buttonText = "Open",
                    onClick = { onNavigate(Routes.HEADLESS) },
                )
                Spacer(Modifier.height(10.dp))
                val context = LocalContext.current
                ActiveTile(
                    title = "🧱 Screen without Compose",
                    body = "A plain Activity: `PayrdrCheckout.Builder(config) { … }.build(this)` in a field, `present(mdOrder)` on tap.",
                    buttonText = "Open",
                    onClick = { context.startActivity(Intent(context, ClassicCheckoutActivity::class.java)) },
                )
                Spacer(Modifier.height(10.dp))
                SslPinningTile(baseUrl = snap.baseUrl, mdOrder = snap.mdOrder, onPay = onPay)
            }
        }
    }
}

@Composable
private fun Placeholder(
    title: String,
    body: String,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F4F6))
            .padding(14.dp),
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF546088))
        Spacer(Modifier.height(4.dp))
        Text(body, fontSize = 10.sp, color = Color(0xFF6C7AA5), lineHeight = 14.sp)
    }
}

@Composable
private fun ActiveTile(
    title: String,
    body: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(body, fontSize = 10.sp, color = Color(0xFF6C7AA5), lineHeight = 14.sp)
        Spacer(Modifier.height(10.dp))
        PrimaryCta(text = buttonText, onClick = onClick)
    }
}

@Composable
private fun SslPinningTile(
    baseUrl: String,
    mdOrder: String,
    onPay: () -> Unit,
) {
    val host =
        runCatching {
            java.net.URI(baseUrl).host ?: "dev.bpcbt.com"
        }.getOrDefault("dev.bpcbt.com")
    val pinning =
        SslPinningConfig
            .builder()
            .addPin(host, "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()
    val sdkConfig = SdkPaymentConfig(baseUrl = baseUrl, sslPinning = pinning)
    val checkout =
        rememberPayrdrCheckout(sdkConfig) { result ->
            LastPaymentResult.set(result)
            onPay()
        }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = TablerIcons.Lock,
                contentDescription = null,
                tint = Color(0xFF1A2341),
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("SSL pinning (hash invalid — intentional)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Checkout запускается с hardcoded-пин, который НЕ соответствует реальному серверу — ожидается сетевая ошибка. Демонстрация fail-fast поведения.",
            fontSize = 10.sp,
            color = Color(0xFF6C7AA5),
            lineHeight = 14.sp,
        )
        Spacer(Modifier.height(10.dp))
        PrimaryCta(
            text = "Buy (expect pin error)",
            onClick = {
                LastPaymentResult.begin("CARD")
                checkout.present(mdOrder)
            },
        )
    }
}
