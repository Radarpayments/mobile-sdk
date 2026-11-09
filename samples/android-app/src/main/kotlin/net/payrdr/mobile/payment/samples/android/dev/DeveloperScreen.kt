package net.payrdr.mobile.payment.samples.android.dev

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronLeft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.samples.android.AppLanguage
import net.payrdr.mobile.payment.samples.android.DemoLanguages
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.findActivity
import net.payrdr.mobile.payment.samples.android.merchant.ApiVersion
import net.payrdr.mobile.payment.samples.android.merchant.OrderType
import net.payrdr.mobile.payment.samples.android.prefs.AppPrefs
import net.payrdr.mobile.payment.samples.android.prefs.rememberPersistedText
import net.payrdr.mobile.payment.samples.android.prefs.rememberPrefs
import net.payrdr.mobile.payment.samples.android.store.StartColors
import net.payrdr.mobile.payment.samples.android.store.invest.isAftDemo
import net.payrdr.mobile.payment.samples.android.store.model.SheetThemeOverride
import net.payrdr.mobile.payment.samples.android.store.theme.StoreFonts
import net.payrdr.mobile.payment.samples.android.store.theme.SystemBarIcons
import net.payrdr.mobile.payment.samples.android.ui.mirrorInRtl
import java.net.URI

internal const val LOGIN_FIELD_TAG = "dev_login"
internal const val PASSWORD_FIELD_TAG = "dev_password"
internal const val API_KEY_FIELD_TAG = "dev_api_key"

/**
 * Settings for whoever runs the demo: the gateway and merchant credentials used to register orders,
 * the SDK sheet look, Google Pay, and the Advanced demo. The four sample apps lay it out the same way.
 */
@Composable
fun DeveloperScreen(
    onBack: () -> Unit,
    onAdvanced: () -> Unit,
) {
    val (prefsState, prefs) = rememberPrefs()
    val snap = prefsState.value ?: return
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    SystemBarIcons(lightBackground = true)
    MaterialTheme(colorScheme = StartColors.scheme) {
        Column(
            Modifier
                .fillMaxSize()
                .background(SettingsBackground)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(TablerIcons.ChevronLeft, stringResource(R.string.store_back), Modifier.mirrorInRtl(), tint = StartColors.text)
                }
                Text(
                    stringResource(R.string.dev_title),
                    Modifier.semantics { heading() },
                    style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                    color = StartColors.text,
                )
            }
            GatewaySection(snap, prefs, scope)
            CredentialsSection(snap, prefs, scope)
            AppearanceSection(snap, prefs, scope)
            GooglePaySection(snap, prefs)
            SettingsSection(stringResource(R.string.dev_more)) {
                LinkRow(stringResource(R.string.dev_advanced), stringResource(R.string.dev_advanced_sub), onAdvanced)
                RowDivider()
                LastOrderRow(snap.lastMdOrder) { md ->
                    scope.launch { clipboard.setClipEntry(ClipData.newPlainText("mdOrder", md).toClipEntry()) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Where orders go and which merchant API registers them. */
@Composable
private fun GatewaySection(
    snap: AppPrefs.Snapshot,
    prefs: AppPrefs,
    scope: CoroutineScope,
) {
    val (baseUrl, onBaseUrl) = rememberPersistedText(snap.baseUrl) { prefs.setBaseUrl(it) }
    val v1 = snap.apiVersion == ApiVersion.V1
    SettingsSection(
        stringResource(R.string.dev_gateway),
        footer = listOf(stringResource(if (v1) R.string.dev_api_v1_hint else R.string.dev_api_v2_hint)),
    ) {
        FieldRow(
            stringResource(R.string.dev_base_url),
            baseUrl,
            onBaseUrl,
            KeyboardType.Uri,
            error = if (isGatewayUrl(baseUrl)) null else stringResource(R.string.dev_base_url_error),
        )
        RowDivider()
        SegmentRow(
            stringResource(R.string.dev_api_version),
            listOf(ApiVersion.V1 to stringResource(R.string.dev_api_v1), ApiVersion.V2 to stringResource(R.string.dev_api_v2)),
            snap.apiVersion,
        ) { scope.launch { prefs.setApiVersion(it) } }
    }
}

/** API V1: the order type, login and password; API V2: the API key. */
@Composable
private fun CredentialsSection(
    snap: AppPrefs.Snapshot,
    prefs: AppPrefs,
    scope: CoroutineScope,
) {
    val (login, onLogin) = rememberPersistedText(snap.merchantLogin) { prefs.setCredentials(login = it) }
    val (password, onPassword) = rememberPersistedText(snap.merchantPassword) { prefs.setCredentials(password = it) }
    val (apiKey, onApiKey) = rememberPersistedText(snap.apiKey) { prefs.setApiKey(it) }
    val aft = isAftDemo(snap.apiVersion, snap.orderType)
    SettingsSection(
        stringResource(R.string.dev_credentials),
        footer = listOfNotNull(stringResource(R.string.dev_order_aft_hint).takeIf { aft }, stringResource(R.string.dev_credentials_note)),
    ) {
        if (snap.apiVersion == ApiVersion.V1) {
            // Transfers are registered through API V1 (registerP2P.do).
            SegmentRow(
                stringResource(R.string.dev_order_type),
                listOf(
                    OrderType.PURCHASE to stringResource(R.string.dev_order_purchase),
                    OrderType.AFT to stringResource(R.string.dev_order_aft),
                ),
                snap.orderType,
            ) { scope.launch { prefs.setOrderType(it) } }
            RowDivider()
            FieldRow(stringResource(R.string.dev_login), login, onLogin, modifier = Modifier.testTag(LOGIN_FIELD_TAG))
            RowDivider()
            FieldRow(
                stringResource(R.string.dev_password),
                password,
                onPassword,
                KeyboardType.Password,
                Modifier.testTag(PASSWORD_FIELD_TAG),
            )
        } else {
            FieldRow(stringResource(R.string.dev_api_key), apiKey, onApiKey, KeyboardType.Password, Modifier.testTag(API_KEY_FIELD_TAG))
        }
    }
}

/** The SDK sheet's theme and language. */
@Composable
private fun AppearanceSection(
    snap: AppPrefs.Snapshot,
    prefs: AppPrefs,
    scope: CoroutineScope,
) {
    val activity = LocalContext.current.findActivity()
    SettingsSection(stringResource(R.string.dev_sdk_look)) {
        PickerRow(
            stringResource(R.string.dev_sheet_theme),
            listOf(
                SheetThemeOverride.COVER to stringResource(R.string.dev_theme_cover),
                SheetThemeOverride.LIGHT to stringResource(R.string.dev_theme_light),
                SheetThemeOverride.DARK to stringResource(R.string.dev_theme_dark),
                SheetThemeOverride.SYSTEM to stringResource(R.string.dev_theme_system),
            ),
            snap.sheetTheme,
            onSelect = { scope.launch { prefs.setSheetTheme(it) } },
        )
        RowDivider()
        PickerRow(
            stringResource(R.string.dev_language),
            listOf<Pair<String?, String>>(null to stringResource(R.string.dev_language_system)) + DemoLanguages,
            snap.localeOverride,
            onSelect = { code ->
                scope.launch {
                    // The SDK reads it from the settings with each payment; the demo's own screens on recreation.
                    prefs.setLocaleOverride(code)
                    activity?.let { AppLanguage.set(it, code) }
                }
            },
        )
    }
}

/** Google Pay gateway and merchant; empty means what the gateway says. */
@Composable
private fun GooglePaySection(
    snap: AppPrefs.Snapshot,
    prefs: AppPrefs,
) {
    val (gateway, onGateway) = rememberPersistedText(snap.gpayGateway) { prefs.setGooglePay(gateway = it) }
    val (merchantId, onMerchantId) = rememberPersistedText(snap.gpayMerchantId) { prefs.setGooglePay(merchantId = it) }
    val fromGateway = stringResource(R.string.dev_gpay_from_gateway)
    SettingsSection(stringResource(R.string.dev_wallets)) {
        FieldRow(stringResource(R.string.dev_gpay_gateway), gateway, onGateway, placeholder = fromGateway)
        RowDivider()
        FieldRow(stringResource(R.string.dev_gpay_merchant), merchantId, onMerchantId, placeholder = fromGateway)
    }
}

/** The gateway is reached over HTTPS only; anything else would fail on the first order with a less telling error. */
internal fun isGatewayUrl(url: String): Boolean =
    runCatching { URI(url.trim()) }.getOrNull()?.let { it.scheme == "https" && !it.host.isNullOrEmpty() } == true
