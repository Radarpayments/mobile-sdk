package net.payrdr.mobile.payment.samples.android.dev

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.TablerIcons
import compose.icons.tablericons.ChevronLeft
import compose.icons.tablericons.ChevronRight
import kotlinx.coroutines.launch
import net.payrdr.mobile.payment.samples.android.AppLanguage
import net.payrdr.mobile.payment.samples.android.DemoLanguages
import net.payrdr.mobile.payment.samples.android.R
import net.payrdr.mobile.payment.samples.android.findActivity
import net.payrdr.mobile.payment.samples.android.merchant.ApiVersion
import net.payrdr.mobile.payment.samples.android.merchant.OrderType
import net.payrdr.mobile.payment.samples.android.prefs.rememberPersistedText
import net.payrdr.mobile.payment.samples.android.prefs.rememberPrefs
import net.payrdr.mobile.payment.samples.android.store.GroupHeader
import net.payrdr.mobile.payment.samples.android.store.StartColors
import net.payrdr.mobile.payment.samples.android.store.invest.isAftDemo
import net.payrdr.mobile.payment.samples.android.store.model.SheetThemeOverride
import net.payrdr.mobile.payment.samples.android.store.theme.StoreFonts
import net.payrdr.mobile.payment.samples.android.store.theme.SystemBarIcons

internal const val LOGIN_FIELD_TAG = "dev_login"
internal const val PASSWORD_FIELD_TAG = "dev_password"
internal const val API_KEY_FIELD_TAG = "dev_api_key"

/**
 * Settings for whoever runs the demo: the gateway and merchant credentials used to register orders,
 * the SDK sheet look, Google Pay, and the Advanced demo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperScreen(
    onBack: () -> Unit,
    onAdvanced: () -> Unit,
) {
    val (prefsState, prefs) = rememberPrefs()
    val snap = prefsState.value ?: return
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val (baseUrl, onBaseUrl) = rememberPersistedText(snap.baseUrl) { prefs.setBaseUrl(it) }
    val (login, onLogin) = rememberPersistedText(snap.merchantLogin) { prefs.setCredentials(login = it) }
    val (password, onPassword) = rememberPersistedText(snap.merchantPassword) { prefs.setCredentials(password = it) }
    val (apiKey, onApiKey) = rememberPersistedText(snap.apiKey) { prefs.setApiKey(it) }
    val (gateway, onGateway) = rememberPersistedText(snap.gpayGateway) { prefs.setGooglePay(gateway = it) }
    val (merchantId, onMerchantId) = rememberPersistedText(snap.gpayMerchantId) { prefs.setGooglePay(merchantId = it) }

    SystemBarIcons(lightBackground = true)
    MaterialTheme(colorScheme = StartColors.scheme) {
        Column(
            Modifier
                .fillMaxSize()
                .background(StartColors.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(TablerIcons.ChevronLeft, contentDescription = stringResource(R.string.store_back), tint = StartColors.text)
                }
                Text(
                    stringResource(R.string.dev_title),
                    style = TextStyle(fontFamily = StoreFonts.inter, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
                    color = StartColors.text,
                )
            }

            GroupHeader(R.string.dev_gateway)
            Field(R.string.dev_base_url, baseUrl, onBaseUrl, KeyboardType.Uri)
            ApiSection(
                apiVersion = snap.apiVersion,
                onApiVersion = { scope.launch { prefs.setApiVersion(it) } },
                orderType = snap.orderType,
                onOrderType = { scope.launch { prefs.setOrderType(it) } },
                login = login to onLogin,
                password = password to onPassword,
                apiKey = apiKey to onApiKey,
            )
            Note(stringResource(R.string.dev_credentials_note))
            if (isAftDemo(snap.apiVersion, snap.orderType)) InvestReset { scope.launch { prefs.setInvestAccount(null) } }

            val activity = LocalContext.current.findActivity()
            SdkLookSection(snap.sheetTheme, snap.localeOverride, onTheme = { scope.launch { prefs.setSheetTheme(it) } }) { code ->
                scope.launch {
                    // The SDK reads it from the settings with each payment; the demo's own screens on recreation.
                    prefs.setLocaleOverride(code)
                    activity?.let { AppLanguage.set(it, code) }
                }
            }

            GroupHeader(R.string.dev_wallets)
            Field(R.string.dev_gpay_gateway, gateway, onGateway, KeyboardType.Text, placeholder = R.string.dev_gpay_from_gateway)
            Field(R.string.dev_gpay_merchant, merchantId, onMerchantId, KeyboardType.Text, placeholder = R.string.dev_gpay_from_gateway)

            GroupHeader(R.string.dev_more)
            LinkRow(R.string.dev_advanced, R.string.dev_advanced_sub, onAdvanced)
            HorizontalDivider(color = StartColors.border)
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.dev_last_order), style = TextStyle(fontSize = 15.sp), color = StartColors.text)
                    Text(
                        snap.lastMdOrder ?: stringResource(R.string.dev_none),
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        color = StartColors.muted,
                    )
                }
                snap.lastMdOrder?.let { md ->
                    TextButton(onClick = { clipboard.setText(AnnotatedString(md)) }) {
                        Text(stringResource(R.string.dev_copy), color = StartColors.link)
                    }
                }
            }
        }
    }
}

/** API V1 (login and password) or API V2 (API key): how the demo registers its orders. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ApiSection(
    apiVersion: ApiVersion,
    onApiVersion: (ApiVersion) -> Unit,
    orderType: OrderType,
    onOrderType: (OrderType) -> Unit,
    login: Pair<String, (String) -> Unit>,
    password: Pair<String, (String) -> Unit>,
    apiKey: Pair<String, (String) -> Unit>,
) {
    Column {
        Label(R.string.dev_api_version)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ApiVersion.V1 to R.string.dev_api_v1, ApiVersion.V2 to R.string.dev_api_v2).forEach { (value, label) ->
                FilterChip(
                    selected = apiVersion == value,
                    onClick = { onApiVersion(value) },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        when (apiVersion) {
            ApiVersion.V1 -> {
                Note(stringResource(R.string.dev_api_v1_hint))
                // Transfers are registered through API V1 (registerP2P.do).
                Label(R.string.dev_order_type)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val orderTypes = listOf(OrderType.PURCHASE to R.string.dev_order_purchase, OrderType.AFT to R.string.dev_order_aft)
                    orderTypes.forEach { (value, label) ->
                        FilterChip(
                            selected = orderType == value,
                            onClick = { onOrderType(value) },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
                if (orderType == OrderType.AFT) Note(stringResource(R.string.dev_order_aft_hint))
                Field(R.string.dev_login, login.first, login.second, KeyboardType.Text, Modifier.testTag(LOGIN_FIELD_TAG))
                Field(
                    R.string.dev_password,
                    password.first,
                    password.second,
                    KeyboardType.Password,
                    Modifier.testTag(PASSWORD_FIELD_TAG),
                    secret = true,
                )
            }
            ApiVersion.V2 -> {
                Note(stringResource(R.string.dev_api_v2_hint))
                Field(
                    R.string.dev_api_key,
                    apiKey.first,
                    apiKey.second,
                    KeyboardType.Password,
                    Modifier.testTag(API_KEY_FIELD_TAG),
                    secret = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SdkLookSection(
    sheetTheme: SheetThemeOverride,
    locale: String?,
    onTheme: (SheetThemeOverride) -> Unit,
    onLocale: (String?) -> Unit,
) {
    Column {
        GroupHeader(R.string.dev_sdk_look)
        Label(R.string.dev_sheet_theme)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                SheetThemeOverride.COVER to R.string.dev_theme_cover,
                SheetThemeOverride.LIGHT to R.string.dev_theme_light,
                SheetThemeOverride.DARK to R.string.dev_theme_dark,
                SheetThemeOverride.SYSTEM to R.string.dev_theme_system,
            ).forEach { (value, label) ->
                FilterChip(
                    selected = sheetTheme == value,
                    onClick = { onTheme(value) },
                    label = { Text(stringResource(label)) },
                )
            }
        }
        Label(R.string.dev_language)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val languages = listOf<Pair<String?, String>>(null to stringResource(R.string.dev_language_system)) + DemoLanguages
            languages.forEach { (code, label) ->
                FilterChip(
                    selected = locale == code,
                    onClick = { onLocale(code) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun Field(
    label: Int,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType,
    modifier: Modifier = Modifier,
    secret: Boolean = false,
    placeholder: Int? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        placeholder = placeholder?.let { { Text(stringResource(it)) } },
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, autoCorrectEnabled = false),
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
}

@Composable
private fun Label(text: Int) {
    Text(
        stringResource(text),
        modifier = Modifier.padding(top = 4.dp),
        style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 14.sp),
        color = StartColors.text,
    )
}

/** Puts the Northwind Invest demo account back as a fresh install has it. */
@Composable
private fun InvestReset(onReset: () -> Unit) {
    var reset by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = {
        onReset()
        reset = true
    }) { Text(stringResource(R.string.dev_invest_reset)) }
    if (reset) Note(stringResource(R.string.dev_invest_reset_done))
}

@Composable
private fun Note(text: String) {
    Text(text, style = TextStyle(fontFamily = StoreFonts.inter, fontSize = 13.sp, lineHeight = 18.sp), color = StartColors.muted)
}

@Composable
private fun LinkRow(
    title: Int,
    subtitle: Int,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = TextStyle(fontSize = 15.sp), color = StartColors.text)
            Text(stringResource(subtitle), style = TextStyle(fontSize = 13.sp), color = StartColors.muted)
        }
        Icon(TablerIcons.ChevronRight, contentDescription = null, tint = StartColors.muted)
    }
}
