package net.payrdr.mobile.payment.samples.android.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronDown
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutOutcome
import net.payrdr.mobile.payment.samples.android.checkout.CheckoutState
import net.payrdr.mobile.payment.samples.android.checkout.buildSdkConfig
import net.payrdr.mobile.payment.samples.android.checkout.rememberStoreCheckout
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistrar
import net.payrdr.mobile.payment.samples.android.merchant.OrderRegistration
import net.payrdr.mobile.payment.samples.android.merchant.TransferPrefill
import net.payrdr.mobile.payment.samples.android.merchant.merchantRegistrar
import net.payrdr.mobile.payment.samples.android.prefs.AppPrefs
import net.payrdr.mobile.payment.samples.android.prefs.rememberPrefs
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.formatEur
import net.payrdr.mobile.payment.samples.android.store.theme.coverStyle
import net.payrdr.mobile.payment.samples.android.store.theme.paymentTheme
import net.payrdr.mobile.payment.sdk.payment.config.GooglePayConfig
import net.payrdr.mobile.payment.sdk.payment.config.SdkPaymentConfig
import net.payrdr.mobile.payment.sdk.payment.http.defaultHttpClientFactory

internal const val PAY_BUTTON_TAG = "store_pay"
internal const val SCENARIO_ROW_TAG = "store_scenario"

/** Builds the "your server" registrar from the developer settings; tests provide a fake. */
val LocalOrderRegistrarFactory =
    staticCompositionLocalOf<(AppPrefs.Snapshot) -> OrderRegistrar> { { gatewayRegistrar(it, prefill = null) } }

/** "Your server" for Northwind Invest: the same, but a transfer is registered with the investor's details. */
val LocalTransferRegistrarFactory =
    staticCompositionLocalOf<(AppPrefs.Snapshot, TransferPrefill) -> OrderRegistrar> { ::gatewayRegistrar }

private fun gatewayRegistrar(
    snap: AppPrefs.Snapshot,
    prefill: TransferPrefill?,
): OrderRegistrar =
    merchantRegistrar(
        apiVersion = snap.apiVersion,
        baseUrl = snap.baseUrl,
        login = snap.merchantLogin,
        password = snap.merchantPassword,
        apiKey = snap.apiKey,
        http = defaultHttpClientFactory().create(SdkPaymentConfig(baseUrl = snap.baseUrl)),
        orderType = snap.orderType,
        prefill = prefill,
    )

/** Step 1 for [cover]: the SDK config in its brand, with the demo panel and the developer settings applied. */
@Composable
fun coverSdkConfig(
    cover: CoverId,
    snap: AppPrefs.Snapshot,
): SdkPaymentConfig =
    buildSdkConfig(
        baseUrl = snap.baseUrl,
        scenario = snap.scenario,
        coverTheme = coverStyle(cover).paymentTheme(),
        themeOverride = snap.sheetTheme,
        locale = snap.localeOverride,
        // `GooglePayConfig()` is enough: the SDK takes the rest from the gateway and the order. Here the developer settings
        // may override two values, and the Google Pay sheet names the store the buyer is in instead of the gateway's
        // merchant name (all the demo stores share one merchant).
        googlePay =
            GooglePayConfig(
                merchantId = snap.gpayMerchantId.ifBlank { null },
                merchantFullName = stringResource(coverName(cover)),
                gateway = snap.gpayGateway.ifBlank { null },
            ),
    )

/**
 * The bottom of every cart in every cover: the demo-panel row, the "Pay" button and a registration
 * error, if any. Registers the order for [amountMinor] and opens the SDK sheet in the cover's brand;
 * with [autoPay] it does so as soon as it is shown.
 */
@Composable
fun StoreCheckoutBar(
    cover: CoverId,
    amountMinor: Long,
    onOutcome: (CheckoutOutcome) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    autoPay: Boolean = false,
    onAutoPayStarted: () -> Unit = {},
) {
    val (prefsState, prefs) = rememberPrefs()
    val snap = prefsState.value
    val style = coverStyle(cover)
    val scope = rememberCoroutineScope()
    val factory = LocalOrderRegistrarFactory.current
    val registrar =
        remember(snap?.baseUrl, snap?.apiVersion, snap?.orderType, snap?.merchantLogin, snap?.merchantPassword, snap?.apiKey) {
            snap?.let(factory) ?: OrderRegistrar { _, _ -> OrderRegistration.MissingCredentials }
        }
    val config = coverSdkConfig(cover, snap ?: return)
    val checkout =
        rememberStoreCheckout(
            config = config,
            registrar = registrar,
            onMdOrder = { md -> scope.launch { prefs.setLastMdOrder(md) } },
            onOutcome = onOutcome,
        )
    val state by checkout.state.collectAsState()
    val clientId = snap.clientId.takeIf { snap.scenario.buyerWithCards }
    // "Order again": go straight to the payment sheet once the screen is up.
    LaunchedEffect(autoPay) {
        if (autoPay && amountMinor > 0) {
            onAutoPayStarted()
            checkout.pay(amountMinor, clientId)
        }
    }
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    val locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: java.util.Locale.getDefault()

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        (state as? CheckoutState.RegistrationFailed)?.let { RegistrationError(cover, it.reason, onOpenSettings) }
        ScenarioRow(cover, snap.scenario.buyerWithCards) { sheetOpen = true }
        PayButton(
            cover = cover,
            label = stringResource(R.string.store_pay, formatEur(amountMinor, locale)),
            busy = state == CheckoutState.Registering || state == CheckoutState.InSheet,
            enabled = amountMinor > 0,
            onClick = { checkout.pay(amountMinor, clientId) },
        )
    }
    if (sheetOpen) {
        ScenarioSheet(
            scenario = snap.scenario,
            onChange = { scope.launch { prefs.setScenario(it) } },
            onResetBuyer = { scope.launch { prefs.resetClientId() } },
            onDismiss = { sheetOpen = false },
        )
    }
}

/** Dashed row above the "Pay" button that opens the demo panel. */
@Composable
fun ScenarioRow(
    cover: CoverId,
    buyerWithCards: Boolean,
    onClick: () -> Unit,
) {
    val style = coverStyle(cover)
    val buyer = stringResource(if (buyerWithCards) R.string.scenario_with_cards else R.string.scenario_guest)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRoundRect(
                    color = style.border,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))),
                )
            }.clickable(role = Role.Button, onClick = onClick)
            .testTag(SCENARIO_ROW_TAG)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.scenario_row, buyer),
            modifier = Modifier.weight(1f),
            style = TextStyle(fontFamily = style.bodyFont, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            color = style.muted,
        )
        Icon(TablerIcons.ChevronDown, contentDescription = null, tint = style.muted, modifier = Modifier.size(18.dp))
    }
}

/** The store's own "Pay" button in the cover's brand; shows progress while the order is registered. */
@Composable
fun PayButton(
    cover: CoverId,
    label: String,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = coverStyle(cover)
    Button(
        onClick = onClick,
        enabled = enabled && !busy,
        shape = if (style.pillButtons) RoundedCornerShape(percent = 50) else RoundedCornerShape(style.radius),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = style.primary,
                contentColor = style.onPrimary,
                disabledContainerColor = style.primary.copy(alpha = if (busy) 1f else 0.4f),
                disabledContentColor = style.onPrimary.copy(alpha = if (busy) 1f else 0.7f),
            ),
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(PAY_BUTTON_TAG),
    ) {
        if (busy) {
            CircularProgressIndicator(color = style.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(
                label,
                style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.Bold, fontSize = 15.sp),
            )
        }
    }
}

/** Why the order was not created, with a way to the developer settings. */
@Composable
fun RegistrationError(
    cover: CoverId,
    reason: OrderRegistration,
    onOpenSettings: () -> Unit,
) {
    val style = coverStyle(cover)
    val text =
        when (reason) {
            is OrderRegistration.Failed -> stringResource(R.string.store_error_failed, reason.message)
            OrderRegistration.MissingApiKey -> stringResource(R.string.store_error_api_key)
            else -> stringResource(R.string.store_error_credentials)
        }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(style.surface)
            .padding(start = 14.dp, end = 4.dp, top = 10.dp),
    ) {
        Text(text, style = TextStyle(fontFamily = style.bodyFont, fontSize = 13.sp, lineHeight = 18.sp), color = style.text)
        TextButton(onClick = onOpenSettings) {
            Text(
                stringResource(R.string.store_open_settings),
                style = TextStyle(fontFamily = style.bodyFont, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                color = style.accent.takeIf { !style.dark } ?: style.primary,
            )
        }
    }
}

/** Brand name of [cover]. */
fun coverName(cover: CoverId): Int =
    when (cover) {
        CoverId.FURNITURE -> R.string.cover_furniture_name
        CoverId.COFFEE -> R.string.cover_coffee_name
        CoverId.TICKETS -> R.string.cover_tickets_name
        CoverId.INVEST -> R.string.invest_name
    }
