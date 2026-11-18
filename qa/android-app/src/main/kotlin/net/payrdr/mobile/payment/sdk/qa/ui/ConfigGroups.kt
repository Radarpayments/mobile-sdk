package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.qa.config.Currencies
import net.payrdr.mobile.payment.sdk.qa.config.GP_ENVIRONMENTS
import net.payrdr.mobile.payment.sdk.qa.config.LOCALES
import net.payrdr.mobile.payment.sdk.qa.config.OrderSource
import net.payrdr.mobile.payment.sdk.qa.config.OrderType
import net.payrdr.mobile.payment.sdk.qa.config.ThemePreset
import net.payrdr.mobile.payment.sdk.qa.theme.DEBUG_LEGEND
import java.util.Locale

@Composable
internal fun OrderGroup(form: ConfigForm) {
    Group(Tags.GROUP_ORDER, "Order") {
        FormChoice(form, Tags.ORDER_SOURCE, "Source", OrderSource.entries.map { it.json })
        FormChoice(form, Tags.ORDER_TYPE, "Type", OrderType.entries.map { it.json })
        FormText(form, Tags.ORDER_AMOUNT, "Amount, minor units", keyboardType = KeyboardType.Number)
        FormChoice(form, Tags.ORDER_CURRENCY, "Currency", Currencies.codes)
        FormText(form, Tags.ORDER_CLIENT_ID, "Client id")
        FormText(form, Tags.ORDER_MD_ORDER, "mdOrder")
        FormText(form, Tags.ORDER_SESSION_ID, "Session id")
        FormText(form, Tags.ORDER_FEATURES, "Features, comma separated")
        FormText(form, Tags.ORDER_EXTRA, "Extra parameters, JSON object", singleLine = false)
    }
}

@Composable
internal fun SdkGroup(form: ConfigForm) {
    Group(Tags.GROUP_SDK, "SDK") {
        FormChoice(form, Tags.SDK_LOCALE, "Locale", listOf("") + LOCALES)
        FormSwitch(form, Tags.SDK_SHOW_RESULT_SCREEN, "Result screen")
        FormSwitch(form, Tags.SDK_SCANNER, "Card scanner")
        FormText(form, Tags.SDK_KEY_PROVIDER_URL, "Key provider URL", keyboardType = KeyboardType.Uri)
    }
}

@Composable
internal fun ThemeGroup(form: ConfigForm) {
    Group(Tags.GROUP_THEME, "Theme") {
        FormChoice(form, Tags.SDK_THEME, "Preset", ThemePreset.entries.map { it.json })
        if (form.config.sdk.theme == ThemePreset.DEBUG) ThemeLegend()
    }
}

@Composable
internal fun WalletsGroup(form: ConfigForm) {
    Group(Tags.GROUP_WALLETS, "Wallets") {
        FormSwitch(form, Tags.SDK_GOOGLE_PAY_ENABLED, "Google Pay")
        FormText(form, Tags.SDK_GOOGLE_PAY_MERCHANT_ID, "Merchant id")
        FormText(form, Tags.SDK_GOOGLE_PAY_MERCHANT_FULL_NAME, "Merchant name")
        FormText(form, Tags.SDK_GOOGLE_PAY_GATEWAY, "Gateway")
        FormText(form, Tags.SDK_GOOGLE_PAY_GATEWAY_MERCHANT_ID, "Gateway merchant id")
        FormChoice(form, Tags.SDK_GOOGLE_PAY_ENVIRONMENT, "Environment", listOf("") + GP_ENVIRONMENTS)
        FormText(form, Tags.SDK_GOOGLE_PAY_ALLOWED_CARD_NETWORKS, "Card networks, comma separated")
        FormText(form, Tags.SDK_GOOGLE_PAY_ALLOWED_AUTH_METHODS, "Auth methods, comma separated")
        FormText(form, Tags.SDK_GOOGLE_PAY_COUNTRY_CODE, "Country code")
        FormSwitch(form, Tags.SDK_GOOGLE_PAY_BILLING_ADDRESS_REQUIRED, "Billing address required")
    }
}

@Composable
internal fun SecurityGroup(form: ConfigForm) {
    Group(Tags.GROUP_SECURITY, "Security") {
        FormSwitch(form, Tags.SDK_SSL_PINNING_ENABLED, "SSL pinning")
        FormText(form, Tags.SDK_SSL_PINNING_PINS, "Pins, host=pin per line", singleLine = false)
    }
}

@Composable
private fun ThemeLegend() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.testTag(Tags.THEME_LEGEND)) {
        DEBUG_LEGEND.forEach { (token, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(20.dp)
                        .background(Color(color.argb))
                        .border(1.dp, MaterialTheme.colorScheme.outline),
                )
                Text("$token  ${String.format(Locale.ROOT, "%08X", color.argb)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
