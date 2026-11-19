package net.payrdr.mobile.payment.sdk.qa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.payrdr.mobile.payment.sdk.qa.config.GP_AUTH_METHODS
import net.payrdr.mobile.payment.sdk.qa.config.GP_ENVIRONMENTS
import net.payrdr.mobile.payment.sdk.qa.config.GP_NETWORKS
import net.payrdr.mobile.payment.sdk.qa.config.LOCALES
import net.payrdr.mobile.payment.sdk.qa.config.ThemePreset
import net.payrdr.mobile.payment.sdk.qa.config.launchProblems
import net.payrdr.mobile.payment.sdk.qa.theme.DEBUG_LEGEND
import net.payrdr.mobile.payment.sdk.qa.ui.kit.Caption
import net.payrdr.mobile.payment.sdk.qa.ui.kit.InputLook
import net.payrdr.mobile.payment.sdk.qa.ui.kit.ListGroup
import net.payrdr.mobile.payment.sdk.qa.ui.kit.NavRow
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaScreen
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaSize
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaText
import net.payrdr.mobile.payment.sdk.qa.ui.kit.QaType
import net.payrdr.mobile.payment.sdk.qa.ui.kit.qaColors
import java.util.Locale

/** The checkout's theme: one preset, and for Debug the colours it paints each token with. */
@Composable
internal fun ThemeScreen(
    form: ConfigForm,
    onBack: () -> Unit,
) {
    QaScreen("Theme", onBack) {
        Caption("Preset")
        FormMarks(form, Tags.SDK_THEME, ThemePreset.entries.map { it.json to it.label() })
        if (form.config.sdk.theme == ThemePreset.DEBUG) {
            Caption("Debug legend")
            Column(Modifier.testTag(Tags.THEME_LEGEND)) {
                DEBUG_LEGEND.forEach { (token, color) -> LegendRow(token, color.argb) }
            }
        }
    }
}

@Composable
private fun LegendRow(
    token: String,
    argb: Long,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = QaSize.screen, vertical = 6.dp),
    ) {
        Row(Modifier.size(16.dp).background(Color(argb)).border(QaSize.line, qaColors.line)) {}
        QaText("$token  ${String.format(Locale.ROOT, "%08X", argb)}", QaType.journal)
    }
}

/** How the checkout behaves: the locale, its result screen, the card scanner and the key provider. */
@Composable
internal fun BehaviourScreen(
    form: ConfigForm,
    onBack: () -> Unit,
    onLocale: () -> Unit,
) {
    QaScreen("Behaviour", onBack) {
        Caption("Checkout")
        ListGroup {
            val locale = form.text(Tags.SDK_LOCALE)
            NavRow("Locale", locale.ifEmpty { "system" }, Tags.SDK_LOCALE, onLocale, readsAs = locale)
            FormSwitch(form, Tags.SDK_SHOW_RESULT_SCREEN, "Result screen")
            FormSwitch(form, Tags.SDK_SCANNER, "Card scanner")
        }
        FormInput(
            form,
            Tags.SDK_KEY_PROVIDER_URL,
            "Key provider URL",
            help = "Empty: the SDK asks the stand for its key",
            look = InputLook(keyboardType = KeyboardType.Uri, style = QaType.inputSmall),
        )
    }
}

/** The checkout's locale; choosing one goes back. */
@Composable
internal fun LocaleScreen(
    form: ConfigForm,
    onBack: () -> Unit,
) {
    QaScreen("Locale", onBack) {
        Caption("Locale")
        FormMarks(form, Tags.SDK_LOCALE, listOf("" to "System locale") + LOCALES.map { it to it }, listTag = null, onChosen = onBack)
    }
}

/** Google Pay's settings, all of them at once: the sheet reads them only when Google Pay is on. */
@Composable
internal fun GooglePayScreen(
    form: ConfigForm,
    onBack: () -> Unit,
) {
    QaScreen("Google Pay", onBack) {
        Caption("Google Pay")
        ListGroup { FormSwitch(form, Tags.SDK_GOOGLE_PAY_ENABLED, "Google Pay") }
        FormSegments(form, Tags.SDK_GOOGLE_PAY_ENVIRONMENT, "Environment", listOf("" to "None") + GP_ENVIRONMENTS.map { it to it.label() })
        FormInput(form, Tags.SDK_GOOGLE_PAY_MERCHANT_ID, "Merchant id", look = InputLook(style = QaType.inputSmall))
        FormInput(form, Tags.SDK_GOOGLE_PAY_MERCHANT_FULL_NAME, "Merchant name", look = InputLook(style = QaType.inputSmall))
        FormInput(form, Tags.SDK_GOOGLE_PAY_GATEWAY, "Gateway", look = InputLook(style = QaType.inputSmall))
        FormInput(form, Tags.SDK_GOOGLE_PAY_GATEWAY_MERCHANT_ID, "Gateway merchant id", look = InputLook(style = QaType.inputSmall))
        Caption("Card networks")
        FormChips(form, Tags.SDK_GOOGLE_PAY_ALLOWED_CARD_NETWORKS, GP_NETWORKS)
        Caption("Auth methods")
        FormChips(form, Tags.SDK_GOOGLE_PAY_ALLOWED_AUTH_METHODS, GP_AUTH_METHODS)
        FormInput(form, Tags.SDK_GOOGLE_PAY_COUNTRY_CODE, "Country code", look = InputLook(placeholder = "US", style = QaType.inputSmall))
        Caption("Address")
        ListGroup { FormSwitch(form, Tags.SDK_GOOGLE_PAY_BILLING_ADDRESS_REQUIRED, "Billing address required") }
    }
}

/** SSL pinning: on or off, and the pins as `host=pin` lines. */
@Composable
internal fun SslPinningScreen(
    form: ConfigForm,
    onBack: () -> Unit,
) {
    val missing = launchProblems(form.config, null).find { it.field == "sdk.sslPinning.pins" }
    QaScreen("SSL pinning", onBack) {
        Caption("SSL pinning")
        ListGroup { FormSwitch(form, Tags.SDK_SSL_PINNING_ENABLED, "SSL pinning") }
        FormInput(
            form,
            Tags.SDK_SSL_PINNING_PINS,
            "Pins",
            help = "One host=pin per line; the pin is the base64 SHA-256 of the certificate's public key",
            problem = missing,
            look = InputLook(singleLine = false, style = QaType.inputSmall),
        )
    }
}

private fun ThemePreset.label(): String =
    when (this) {
        ThemePreset.SYSTEM -> "System"
        ThemePreset.LIGHT -> "Light"
        ThemePreset.DARK -> "Dark"
        ThemePreset.CONTRAST -> "Contrast"
        ThemePreset.ROUNDED -> "Rounded"
        ThemePreset.SQUARE -> "Square"
        ThemePreset.LARGE_TEXT -> "Large text"
        ThemePreset.DEBUG -> "Debug"
    }

private fun String.label(): String = replaceFirstChar { it.uppercase() }
