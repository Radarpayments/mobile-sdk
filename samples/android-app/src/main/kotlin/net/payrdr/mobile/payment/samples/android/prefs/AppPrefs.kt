package net.payrdr.mobile.payment.samples.android.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import net.payrdr.mobile.payment.samples.android.merchant.ApiVersion
import net.payrdr.mobile.payment.samples.android.merchant.OrderType
import net.payrdr.mobile.payment.samples.android.store.invest.InvestAccount
import net.payrdr.mobile.payment.samples.android.store.model.CoverId
import net.payrdr.mobile.payment.samples.android.store.model.Scenario
import net.payrdr.mobile.payment.samples.android.store.model.SheetThemeOverride
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "sample_prefs")

/**
 * Typed read/write wrapper over DataStore Preferences. Default values are hard-coded valid test
 * data from `android-sdk/app_kotlin_core`.
 */
class AppPrefs(
    private val context: Context,
) {
    /** Values used until the user changes them. */
    companion object Defaults {
        const val BASE_URL = "https://dev.bpcbt.com/payment"
        const val MD_ORDER = "c400b41a-aa3d-43db-8727-ac4ca9e8f701"
        const val BINDING_ID = "513b17f4-e32e-414f-8c74-936fd7027baa"
        const val SDK_CORE_VARIANT = "new"

        /** Empty: Google Pay takes its gateway and merchant id from the gateway's settings for the merchant. */
        const val GPAY_GATEWAY = ""
        const val GPAY_MERCHANT_ID = ""

        /** No merchant login ships with the app: it is typed in the developer settings. */
        const val MERCHANT_LOGIN = ""

        /** No merchant password ships with the app: it is typed in the developer settings. */
        const val MERCHANT_PASSWORD = ""

        /** No API V2 key ships with the app either. */
        const val API_KEY = ""
        const val PUB_KEY_PEM = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAij/G3JVV3TqYCFZTPmwi
4JduQMsZ2HcFLBBA9fYAvApv3FtA+zKdUGgKh/OPbtpsxe1C57gIaRclbzMoafTb
0eOdj+jqSEJMlVJYSiZ8Hn6g67evhu9wXh5ZKBQ1RUpqL36LbhYnIrP+TEGR/Vyj
bC6QTfaktcRfa8zRqJczHFsyWxnlfwKLfqKz5wSqXkShcrwcfRJCyDRjZX6OFUEC
HsWVK3WMcOV3WZREwbCkh/o5R5Vl6xoyLvSqVEKQiHupJcZu9UEOJiP3yNCn9YPg
yFs2vrCeg6qxDPFnCfetcDCLjjLenGF7VyZzBJ9G2NP3k/mNVtD8Kl7lpiurwY7E
ZwIDAQAB
-----END PUBLIC KEY-----"""
    }

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val MD_ORDER = stringPreferencesKey("md_order")
        val PUB_KEY_PEM = stringPreferencesKey("pub_key_pem")
        val BINDING_ID = stringPreferencesKey("core_bid")
        val SDK_CORE_VARIANT = stringPreferencesKey("core_var")
        val GPAY_GATEWAY = stringPreferencesKey("gpay_gw")
        val GPAY_MERCHANT_ID = stringPreferencesKey("gpay_mid")
        val LOGIN = stringPreferencesKey("store_login")
        val PASSWORD = stringPreferencesKey("store_password")
        val API_VERSION = stringPreferencesKey("store_api_version")
        val API_KEY = stringPreferencesKey("store_api_key")
        val ORDER_TYPE = stringPreferencesKey("store_order_type")
        val CLIENT_ID = stringPreferencesKey("store_client_id")
        val BUYER_WITH_CARDS = booleanPreferencesKey("store_buyer_cards")
        val WALLET = booleanPreferencesKey("store_wallet")
        val SCANNER = booleanPreferencesKey("store_scanner")
        val SDK_RESULT = booleanPreferencesKey("store_sdk_result")
        val LAST_COVER = stringPreferencesKey("store_last_cover")
        val LAST_MD_ORDER = stringPreferencesKey("store_last_md_order")
        val SHEET_THEME = stringPreferencesKey("store_sheet_theme")
        val LOCALE = stringPreferencesKey("store_locale")
        val INVEST_ACCOUNT = stringPreferencesKey("invest_account")
    }

    /** Everything stored, read at once. */
    data class Snapshot(
        val baseUrl: String,
        val mdOrder: String,
        val pubKeyPem: String,
        val bindingId: String,
        val sdkCoreVariant: String,
        val gpayGateway: String,
        val gpayMerchantId: String,
        val merchantLogin: String,
        val merchantPassword: String,
        val apiVersion: ApiVersion,
        val apiKey: String,
        val orderType: OrderType,
        val clientId: String,
        val scenario: Scenario,
        val lastCover: CoverId?,
        val lastMdOrder: String?,
        val sheetTheme: SheetThemeOverride,
        val localeOverride: String?,
        val investAccount: InvestAccount,
    ) {
        override fun toString(): String =
            "Snapshot(baseUrl=$baseUrl, apiVersion=$apiVersion, orderType=$orderType, merchantLogin=$merchantLogin, clientId=$clientId)"
    }

    val flow: Flow<Snapshot> =
        context.dataStore.data
            .map { p ->
                Snapshot(
                    baseUrl = p[Keys.BASE_URL] ?: Defaults.BASE_URL,
                    mdOrder = p[Keys.MD_ORDER] ?: Defaults.MD_ORDER,
                    pubKeyPem = p[Keys.PUB_KEY_PEM] ?: Defaults.PUB_KEY_PEM,
                    bindingId = p[Keys.BINDING_ID] ?: Defaults.BINDING_ID,
                    sdkCoreVariant = p[Keys.SDK_CORE_VARIANT] ?: Defaults.SDK_CORE_VARIANT,
                    gpayGateway = p[Keys.GPAY_GATEWAY] ?: Defaults.GPAY_GATEWAY,
                    gpayMerchantId = p[Keys.GPAY_MERCHANT_ID] ?: Defaults.GPAY_MERCHANT_ID,
                    merchantLogin = p[Keys.LOGIN] ?: Defaults.MERCHANT_LOGIN,
                    merchantPassword = p[Keys.PASSWORD] ?: Defaults.MERCHANT_PASSWORD,
                    apiVersion = p[Keys.API_VERSION]?.let { name -> ApiVersion.entries.find { it.name == name } } ?: ApiVersion.V1,
                    apiKey = p[Keys.API_KEY] ?: Defaults.API_KEY,
                    orderType = p[Keys.ORDER_TYPE]?.let { name -> OrderType.entries.find { it.name == name } } ?: OrderType.PURCHASE,
                    clientId = p[Keys.CLIENT_ID].orEmpty(),
                    scenario =
                        Scenario(
                            buyerWithCards = p[Keys.BUYER_WITH_CARDS] ?: false,
                            wallet = p[Keys.WALLET] ?: true,
                            scanner = p[Keys.SCANNER] ?: true,
                            sdkResultScreen = p[Keys.SDK_RESULT] ?: false,
                        ),
                    lastCover = p[Keys.LAST_COVER]?.let { name -> CoverId.entries.find { it.name == name } },
                    lastMdOrder = p[Keys.LAST_MD_ORDER],
                    sheetTheme =
                        p[Keys.SHEET_THEME]?.let { name -> SheetThemeOverride.entries.find { it.name == name } }
                            ?: SheetThemeOverride.COVER,
                    localeOverride = p[Keys.LOCALE],
                    investAccount = InvestAccount.fromJson(p[Keys.INVEST_ACCOUNT]),
                )
            }.onStart { ensureClientId() }

    /** Gateway base URL for the SDK and order registration. */
    suspend fun setBaseUrl(v: String) = context.dataStore.edit { it[Keys.BASE_URL] = v }

    /** mdOrder used by the SDK Core demo. */
    suspend fun setMdOrder(v: String) = context.dataStore.edit { it[Keys.MD_ORDER] = v }

    /** Public key used by the SDK Core demo. */
    suspend fun setPubKeyPem(v: String) = context.dataStore.edit { it[Keys.PUB_KEY_PEM] = v }

    suspend fun setBindingId(v: String) = context.dataStore.edit { it[Keys.BINDING_ID] = v }

    suspend fun setSdkCoreVariant(v: String) = context.dataStore.edit { it[Keys.SDK_CORE_VARIANT] = v }

    /** Google Pay gateway name and/or merchant id; null leaves a value as it is. */
    suspend fun setGooglePay(
        gateway: String? = null,
        merchantId: String? = null,
    ) = context.dataStore.edit {
        if (gateway != null) it[Keys.GPAY_GATEWAY] = gateway
        if (merchantId != null) it[Keys.GPAY_MERCHANT_ID] = merchantId
    }

    /** Stores the merchant login and/or password typed in the developer settings; they never leave the device. */
    suspend fun setCredentials(
        login: String? = null,
        password: String? = null,
    ) = context.dataStore.edit {
        if (login != null) it[Keys.LOGIN] = login
        if (password != null) it[Keys.PASSWORD] = password
    }

    /** Chooses the merchant API demo orders are registered through. */
    suspend fun setApiVersion(v: ApiVersion) = context.dataStore.edit { it[Keys.API_VERSION] = v.name }

    /** Chooses what demo orders are: payments or AFT transfers (API V1). */
    suspend fun setOrderType(v: OrderType) = context.dataStore.edit { it[Keys.ORDER_TYPE] = v.name }

    /** Stores the API V2 key typed in the developer settings; it never leaves the device. */
    suspend fun setApiKey(v: String) = context.dataStore.edit { it[Keys.API_KEY] = v }

    /** Saves the demo panel. */
    suspend fun setScenario(s: Scenario) =
        context.dataStore.edit {
            it[Keys.BUYER_WITH_CARDS] = s.buyerWithCards
            it[Keys.WALLET] = s.wallet
            it[Keys.SCANNER] = s.scanner
            it[Keys.SDK_RESULT] = s.sdkResultScreen
        }

    /** Replaces the demo buyer with a new one (no saved cards yet) and returns its clientId. */
    suspend fun resetClientId(): String {
        val id = newClientId()
        context.dataStore.edit { it[Keys.CLIENT_ID] = id }
        return id
    }

    /** Saves the Northwind Invest demo account; null puts it back as a fresh install has it. */
    suspend fun setInvestAccount(account: InvestAccount?) =
        context.dataStore.edit { if (account == null) it.remove(Keys.INVEST_ACCOUNT) else it[Keys.INVEST_ACCOUNT] = account.toJson() }

    /** Remembers the cover to open on the next launch; null opens the start screen. */
    suspend fun setLastCover(c: CoverId?) =
        context.dataStore.edit { if (c == null) it.remove(Keys.LAST_COVER) else it[Keys.LAST_COVER] = c.name }

    /** Remembers the mdOrder of the last registered order, shown in the developer settings. */
    suspend fun setLastMdOrder(md: String) = context.dataStore.edit { it[Keys.LAST_MD_ORDER] = md }

    suspend fun setSheetTheme(t: SheetThemeOverride) = context.dataStore.edit { it[Keys.SHEET_THEME] = t.name }

    /** Sets the SDK language; null follows the system. */
    suspend fun setLocaleOverride(code: String?) =
        context.dataStore.edit { if (code == null) it.remove(Keys.LOCALE) else it[Keys.LOCALE] = code }

    private suspend fun ensureClientId() {
        context.dataStore.edit { if (it[Keys.CLIENT_ID].isNullOrBlank()) it[Keys.CLIENT_ID] = newClientId() }
    }

    // Short on purpose: with a 41-character id the test gateway registered the order but declined
    // the payment that saves the card (71015).
    private fun newClientId() = "demo-${UUID.randomUUID().toString().take(CLIENT_ID_RANDOM_CHARS)}"
}

private const val CLIENT_ID_RANDOM_CHARS = 13
