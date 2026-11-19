package net.payrdr.mobile.payment.sdk.qa.ui

/** The element identifiers of the contract (`qa/fixtures/identifiers.txt`) that Android has, exposed as resource ids. */
internal object Tags {
    const val PROFILE = "qa_profile"
    const val ORDER_SOURCE = "qa_order_source"
    const val ORDER_TYPE = "qa_order_type"
    const val ORDER_AMOUNT = "qa_order_amount"
    const val ORDER_CURRENCY = "qa_order_currency"
    const val ORDER_CLIENT_ID = "qa_order_client_id"
    const val ORDER_FEATURES = "qa_order_features"
    const val ORDER_EXTRA = "qa_order_extra"
    const val ORDER_MD_ORDER = "qa_order_md_order"
    const val ORDER_SESSION_ID = "qa_order_session_id"
    const val SDK_LOCALE = "qa_sdk_locale"
    const val SDK_THEME = "qa_sdk_theme"
    const val SDK_SHOW_RESULT_SCREEN = "qa_sdk_show_result_screen"
    const val SDK_SCANNER = "qa_sdk_scanner"
    const val SDK_KEY_PROVIDER_URL = "qa_sdk_key_provider_url"
    const val SDK_SSL_PINNING_ENABLED = "qa_sdk_ssl_pinning_enabled"
    const val SDK_SSL_PINNING_PINS = "qa_sdk_ssl_pinning_pins"
    const val SDK_GOOGLE_PAY_ENABLED = "qa_sdk_google_pay_enabled"
    const val SDK_GOOGLE_PAY_MERCHANT_ID = "qa_sdk_google_pay_merchant_id"
    const val SDK_GOOGLE_PAY_MERCHANT_FULL_NAME = "qa_sdk_google_pay_merchant_full_name"
    const val SDK_GOOGLE_PAY_GATEWAY = "qa_sdk_google_pay_gateway"
    const val SDK_GOOGLE_PAY_GATEWAY_MERCHANT_ID = "qa_sdk_google_pay_gateway_merchant_id"
    const val SDK_GOOGLE_PAY_ENVIRONMENT = "qa_sdk_google_pay_environment"
    const val SDK_GOOGLE_PAY_ALLOWED_CARD_NETWORKS = "qa_sdk_google_pay_allowed_card_networks"
    const val SDK_GOOGLE_PAY_ALLOWED_AUTH_METHODS = "qa_sdk_google_pay_allowed_auth_methods"
    const val SDK_GOOGLE_PAY_COUNTRY_CODE = "qa_sdk_google_pay_country_code"
    const val SDK_GOOGLE_PAY_BILLING_ADDRESS_REQUIRED = "qa_sdk_google_pay_billing_address_required"

    const val GROUP_ORDER = "qa_group_order"
    const val GROUP_SDK = "qa_group_sdk"
    const val GROUP_THEME = "qa_group_theme"
    const val GROUP_WALLETS = "qa_group_wallets"
    const val GROUP_SECURITY = "qa_group_security"
    const val THEME_LEGEND = "qa_theme_legend"

    const val LAUNCH = "qa_launch"
    const val MENU = "qa_menu"
    const val IMPORT = "qa_import"
    const val IMPORT_QR = "qa_import_qr"
    const val IMPORT_TEXT = "qa_import_text"
    const val IMPORT_TEXT_FIELD = "qa_import_text_field"
    const val IMPORT_TEXT_APPLY = "qa_import_text_apply"
    const val EXPORT = "qa_export"
    const val RESET = "qa_reset"
    const val REPORT = "qa_report"
    const val RUN_AGAIN = "qa_run_again"
    const val PROFILES_OPEN = "qa_profiles_open"

    const val HOME_AMOUNT = "qa_home_amount"
    const val HOME_FEATURES = "qa_home_features"
    const val HOME_LAST_RUN = "qa_home_last_run"
    const val ORDER_FEATURES_OTHER = "qa_order_features_other"
    const val ORDER_FEATURES_OTHER_NAME = "qa_order_features_other_name"
    const val ORDER_FEATURES_OTHER_ADD = "qa_order_features_other_add"
    const val ORDER_CLIENT_ID_OPEN = "qa_order_client_id_open"
    const val ORDER_EXTRA_OPEN = "qa_order_extra_open"
    const val ORDER_MD_ORDER_PASTE = "qa_order_md_order_paste"
    const val ORDER_SESSION_ID_PASTE = "qa_order_session_id_paste"

    const val PROFILE_NAME = "qa_profile_name"
    const val PROFILE_GATEWAY_URL = "qa_profile_gateway_url"
    const val PROFILE_API = "qa_profile_api"
    const val PROFILE_LOGIN = "qa_profile_login"
    const val PROFILE_PASSWORD = "qa_profile_password"
    const val PROFILE_API_KEY = "qa_profile_api_key"
    const val PROFILE_ADD = "qa_profile_add"
    const val PROFILE_SAVE = "qa_profile_save"
    const val PROFILE_DELETE = "qa_profile_delete"
    const val PROFILE_CONFIRM = "qa_profile_confirm"
    const val PROFILE_REJECT = "qa_profile_reject"
    const val PROFILE_ENTER = "qa_profile_enter"
    const val PROFILE_PASTE = "qa_profile_paste"
    const val PROFILE_PASTE_TEXT = "qa_profile_paste_text"
    const val PROFILE_SCAN = "qa_profile_scan"
    const val PROFILE_PASSWORD_SHOW = "qa_profile_password_show"
    const val PROFILE_API_KEY_SHOW = "qa_profile_api_key_show"

    const val VALIDATION_ERROR = "qa_validation_error"
    const val RESULT_TYPE = "qa_result_type"
    const val RESULT_MD_ORDER = "qa_result_md_order"
    const val RESULT_MD_ORDER_COPY = "qa_result_md_order_copy"
    const val RESULT_SESSION_ID = "qa_result_session_id"
    const val RESULT_ORDER_STATUS = "qa_result_order_status"
    const val RESULT_ERROR = "qa_result_error"
    const val RESULT_SESSION_ID_COPY = "qa_result_session_id_copy"
    const val RESULT_STAND = "qa_result_stand"
    const val RESULT_ORDER = "qa_result_order"
    const val RESULT_THEME = "qa_result_theme"
    const val JOURNAL_OPEN = "qa_journal_open"
    const val JOURNAL_TEXT = "qa_journal_text"
    const val JOURNAL_LEVEL = "qa_journal_level"
    const val JOURNAL_SEARCH = "qa_journal_search"
    const val JOURNAL_CLEAR = "qa_journal_clear"
    const val JOURNAL_COPY = "qa_journal_copy"

    /** The configuration fields, in the order of the form: the order [ConfigForm] reports its first problem in. */
    val fields: List<String> =
        listOf(
            PROFILE,
            ORDER_SOURCE,
            ORDER_TYPE,
            ORDER_AMOUNT,
            ORDER_CURRENCY,
            ORDER_CLIENT_ID,
            ORDER_MD_ORDER,
            ORDER_SESSION_ID,
            ORDER_FEATURES,
            ORDER_EXTRA,
            SDK_LOCALE,
            SDK_SHOW_RESULT_SCREEN,
            SDK_SCANNER,
            SDK_KEY_PROVIDER_URL,
            SDK_THEME,
            SDK_GOOGLE_PAY_ENABLED,
            SDK_GOOGLE_PAY_MERCHANT_ID,
            SDK_GOOGLE_PAY_MERCHANT_FULL_NAME,
            SDK_GOOGLE_PAY_GATEWAY,
            SDK_GOOGLE_PAY_GATEWAY_MERCHANT_ID,
            SDK_GOOGLE_PAY_ENVIRONMENT,
            SDK_GOOGLE_PAY_ALLOWED_CARD_NETWORKS,
            SDK_GOOGLE_PAY_ALLOWED_AUTH_METHODS,
            SDK_GOOGLE_PAY_COUNTRY_CODE,
            SDK_GOOGLE_PAY_BILLING_ADDRESS_REQUIRED,
            SDK_SSL_PINNING_ENABLED,
            SDK_SSL_PINNING_PINS,
        )

    val all: List<String> =
        fields +
            listOf(
                GROUP_ORDER,
                GROUP_SDK,
                GROUP_THEME,
                GROUP_WALLETS,
                GROUP_SECURITY,
                THEME_LEGEND,
                LAUNCH,
                MENU,
                IMPORT,
                IMPORT_QR,
                IMPORT_TEXT,
                IMPORT_TEXT_FIELD,
                IMPORT_TEXT_APPLY,
                EXPORT,
                RESET,
                REPORT,
                RUN_AGAIN,
                PROFILES_OPEN,
                HOME_AMOUNT,
                HOME_FEATURES,
                HOME_LAST_RUN,
                ORDER_FEATURES_OTHER,
                ORDER_FEATURES_OTHER_NAME,
                ORDER_FEATURES_OTHER_ADD,
                ORDER_CLIENT_ID_OPEN,
                ORDER_EXTRA_OPEN,
                ORDER_MD_ORDER_PASTE,
                ORDER_SESSION_ID_PASTE,
                PROFILE_NAME,
                PROFILE_GATEWAY_URL,
                PROFILE_API,
                PROFILE_LOGIN,
                PROFILE_PASSWORD,
                PROFILE_API_KEY,
                PROFILE_ADD,
                PROFILE_SAVE,
                PROFILE_DELETE,
                PROFILE_CONFIRM,
                PROFILE_REJECT,
                PROFILE_ENTER,
                PROFILE_PASTE,
                PROFILE_PASTE_TEXT,
                PROFILE_SCAN,
                PROFILE_PASSWORD_SHOW,
                PROFILE_API_KEY_SHOW,
                VALIDATION_ERROR,
                RESULT_TYPE,
                RESULT_MD_ORDER,
                RESULT_MD_ORDER_COPY,
                RESULT_SESSION_ID,
                RESULT_ORDER_STATUS,
                RESULT_ERROR,
                RESULT_SESSION_ID_COPY,
                RESULT_STAND,
                RESULT_ORDER,
                RESULT_THEME,
                JOURNAL_OPEN,
                JOURNAL_TEXT,
                JOURNAL_LEVEL,
                JOURNAL_SEARCH,
                JOURNAL_CLEAR,
                JOURNAL_COPY,
            )

    /**
     * The identifiers that end in a value, as `qa/fixtures/identifiers.txt` writes them: an option of a choice, a chip of a
     * set, a profile's row. The value part is what [option] makes of the value.
     */
    val patterns: List<String> =
        listOf(
            "${ORDER_SOURCE}_$VALUE",
            "${ORDER_TYPE}_$VALUE",
            "${ORDER_CURRENCY}_$VALUE",
            "${ORDER_FEATURE}_$VALUE",
            "${SDK_THEME}_$VALUE",
            "${SDK_LOCALE}_$VALUE",
            "${SDK_GOOGLE_PAY_ENVIRONMENT}_$VALUE",
            "${SDK_GOOGLE_PAY_ALLOWED_CARD_NETWORKS}_$VALUE",
            "${SDK_GOOGLE_PAY_ALLOWED_AUTH_METHODS}_$VALUE",
            "${PROFILE_SELECT}_$NAME",
            "${PROFILE_EDIT}_$NAME",
            "${PROFILE_API}_$VALUE",
            "${JOURNAL_FILTER}_$VALUE",
        )

    /**
     * The identifier of one option of [field]: the value in snake case (a camel hump becomes `_`), lower case, with every
     * run of other characters than `a-z` and `0-9` as one `_`, and `none` for no value: `qa_order_currency_eur`,
     * `qa_sdk_theme_large_text`, `qa_sdk_locale_none`.
     */
    fun option(
        field: String,
        value: String?,
    ): String =
        "${field}_${value
            ?.replace(CAMEL_HUMP, "$1_$2")
            ?.lowercase()
            ?.replace(NOT_ID, "_")
            ?.ifEmpty { null } ?: NONE}"

    fun feature(name: String): String = option(ORDER_FEATURE, name)

    fun profileSelect(name: String): String = "${PROFILE_SELECT}_$name"

    fun profileEdit(name: String): String = "${PROFILE_EDIT}_$name"

    fun journalFilter(id: String): String = "${JOURNAL_FILTER}_$id"

    private const val ORDER_FEATURE = "qa_order_feature"
    private const val PROFILE_SELECT = "qa_profile_select"
    private const val PROFILE_EDIT = "qa_profile_edit"
    private const val JOURNAL_FILTER = "qa_journal_filter"
    private const val VALUE = "<value>"
    private const val NAME = "<name>"
    private const val NONE = "none"
    private val NOT_ID = Regex("[^a-z0-9]+")
    private val CAMEL_HUMP = Regex("([a-z])([A-Z])")
}
