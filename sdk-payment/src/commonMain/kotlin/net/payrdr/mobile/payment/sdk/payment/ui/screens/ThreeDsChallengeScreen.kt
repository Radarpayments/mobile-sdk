package net.payrdr.mobile.payment.sdk.payment.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.payment.result.OrderSummary
import net.payrdr.mobile.payment.sdk.payment.ui.components.ButtonKind
import net.payrdr.mobile.payment.sdk.payment.ui.components.CheckoutButton
import net.payrdr.mobile.payment.sdk.payment.ui.components.PaymentIcons
import net.payrdr.mobile.payment.sdk.payment.ui.components.ProgressRing
import net.payrdr.mobile.payment.sdk.payment.ui.components.SIDE_PADDING
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentColors
import net.payrdr.mobile.payment.sdk.payment.ui.theme.LocalPaymentTypography
import net.payrdr.mobile.payment.sdk.payment.ui.theme.tabular
import net.payrdr.mobile.payment.sdk.payment.ui.util.ltr
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.Res
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.threeds_cancel
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.threeds_hint
import net.payrdr.mobile.payment.sdk.sdk.payment.generated.resources.threeds_title
import org.jetbrains.compose.resources.stringResource

/**
 * Shown while the platform WebView (wrapped by [net.payrdr.mobile.payment.sdk.payment.threeds.WebChallengeHost])
 * is displaying the ACS challenge. This composable itself does NOT host the WebView — the host
 * presents it as a modal dialog/controller outside the Compose tree, and emits the result back
 * into the FSM when the redirect fires.
 *
 * @param onCancel user taps Cancel → dispatch `PaymentIntent.Cancel` upstream
 * @param order amount shown under the headline; null hides it
 */
@Composable
public fun ThreeDsChallengeScreen(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    order: OrderSummary? = null,
) = ThreeDsChallengeContent(onCancel = onCancel, order = order, bottomPadding = 18.dp, modifier = modifier)

/**
 * [ThreeDsChallengeScreen] with [bottomPadding] under the Cancel button: none in the checkout sheet, which keeps that
 * space itself, so the button stands where the sheet's other buttons do.
 */
@Composable
internal fun ThreeDsChallengeContent(
    onCancel: () -> Unit,
    order: OrderSummary?,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPaymentColors.current
    val typography = LocalPaymentTypography.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(start = SIDE_PADDING, end = SIDE_PADDING, top = 18.dp, bottom = bottomPadding)
                .testTag("ThreeDsChallengeScreen"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ProgressRing(icon = PaymentIcons.Shield)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(Res.string.threeds_title),
                style = typography.headline.copy(color = colors.onBackground),
                textAlign = TextAlign.Center,
            )
            order?.formattedAmount?.let {
                Text(
                    text = ltr(it),
                    style = typography.body.tabular().copy(color = colors.onSurfaceVariant, fontWeight = typography.title.fontWeight),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Text(
                text = stringResource(Res.string.threeds_hint),
                style = typography.body.copy(color = colors.onSurfaceVariant),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp).widthIn(max = 280.dp),
            )
        }
        CheckoutButton(
            text = stringResource(Res.string.threeds_cancel),
            onClick = onCancel,
            kind = ButtonKind.Secondary,
        )
    }
}
