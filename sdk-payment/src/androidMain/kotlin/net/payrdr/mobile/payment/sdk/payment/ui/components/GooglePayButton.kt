package net.payrdr.mobile.payment.sdk.payment.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.wallet.button.ButtonConstants
import com.google.android.gms.wallet.button.ButtonOptions
import com.google.android.gms.wallet.button.PayButton
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayAvailability
import net.payrdr.mobile.payment.sdk.payment.gpay.GooglePayRequestBuilder
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentShapes
import net.payrdr.mobile.payment.sdk.payment.ui.theme.colorsAreDark
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.pay_with_google_pay
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.stringResource

/**
 * Android actual: real Google Pay [PayButton] from play-services-wallet if available;
 * Material fallback otherwise.
 */
@Composable
internal fun GooglePayButton(
    onClick: () -> Unit,
    modifier: Modifier,
) {
    if (GooglePayAvailability.isAvailable) {
        // The view is built once per look: its listener calls the latest onClick, not the one of the first composition.
        val latestClick = rememberUpdatedState(onClick)
        // Same corners and height as the other checkout buttons; the light variant on dark themes,
        // as Google's brand guidelines ask.
        val density = LocalDensity.current
        val shape = LocalPaymentShapes.current.button
        val dark = colorsAreDark(LocalPaymentColors.current)
        val radiusPx = remember(shape, density) { shape.walletButtonCornerRadius()?.let { with(density) { it.roundToPx() } } }
        key(radiusPx, dark) {
            AndroidView(
                modifier = modifier.fillMaxWidth().height(WalletButtonHeight),
                factory = { context ->
                    PayButton(context).apply {
                        initialize(
                            ButtonOptions
                                .newBuilder()
                                .setAllowedPaymentMethods(GooglePayRequestBuilder.buildButtonPaymentMethods(defaultButtonProbeConfig()))
                                .setButtonTheme(if (dark) ButtonConstants.ButtonTheme.LIGHT else ButtonConstants.ButtonTheme.DARK)
                                .apply { radiusPx?.let { setCornerRadius(it) } }
                                .build(),
                        )
                        setOnClickListener { latestClick.value() }
                    }
                },
            )
        }
    } else {
        // Fallback — consumer app forgot to add play-services-wallet runtime dep.
        val shapes = LocalPaymentShapes.current
        Button(
            onClick = onClick,
            shape = shapes.button,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ),
            modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(text = stringResource(Res.string.pay_with_google_pay))
        }
    }
}

/**
 * The `PayButton` only needs the card networks and auth methods at construction time; the request parameters are
 * supplied at tap-time.
 */
private fun defaultButtonProbeConfig(): net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig =
    net.payrdr.mobile.payment.sdk.payment.config
        .GooglePayConfig()
