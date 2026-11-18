/// The accessibility identifiers of the contract (`qa/fixtures/identifiers.txt`) that an iOS run uses.
nonisolated enum Tags {
    static let profile = "qa_profile"
    static let orderSource = "qa_order_source"
    static let orderType = "qa_order_type"
    static let orderAmount = "qa_order_amount"
    static let orderCurrency = "qa_order_currency"
    static let orderClientId = "qa_order_client_id"
    static let orderFeatures = "qa_order_features"
    static let orderExtra = "qa_order_extra"
    static let orderMdOrder = "qa_order_md_order"
    static let orderSessionId = "qa_order_session_id"
    static let sdkLocale = "qa_sdk_locale"
    static let sdkTheme = "qa_sdk_theme"
    static let sdkShowResultScreen = "qa_sdk_show_result_screen"
    static let sdkScanner = "qa_sdk_scanner"
    static let sdkKeyProviderUrl = "qa_sdk_key_provider_url"
    static let sdkSslPinningEnabled = "qa_sdk_ssl_pinning_enabled"
    static let sdkSslPinningPins = "qa_sdk_ssl_pinning_pins"
    static let sdkApplePayEnabled = "qa_sdk_apple_pay_enabled"
    static let sdkApplePayMerchantId = "qa_sdk_apple_pay_merchant_id"
    static let sdkApplePaySupportedNetworks = "qa_sdk_apple_pay_supported_networks"
    static let sdkApplePayMerchantCapabilities = "qa_sdk_apple_pay_merchant_capabilities"
    static let sdkApplePayCountryCode = "qa_sdk_apple_pay_country_code"

    static let groupOrder = "qa_group_order"
    static let groupSdk = "qa_group_sdk"
    static let groupTheme = "qa_group_theme"
    static let groupWallets = "qa_group_wallets"
    static let groupSecurity = "qa_group_security"
    static let themeLegend = "qa_theme_legend"

    static let launch = "qa_launch"
    static let menu = "qa_menu"
    static let importClipboard = "qa_import"
    static let importQr = "qa_import_qr"
    static let export = "qa_export"
    static let reset = "qa_reset"
    static let report = "qa_report"
    static let runAgain = "qa_run_again"

    static let profilesOpen = "qa_profiles_open"
    static let profileName = "qa_profile_name"
    static let profileGatewayUrl = "qa_profile_gateway_url"
    static let profileApi = "qa_profile_api"
    static let profileLogin = "qa_profile_login"
    static let profilePassword = "qa_profile_password"
    static let profileApiKey = "qa_profile_api_key"
    static let profileAdd = "qa_profile_add"
    static let profileSave = "qa_profile_save"
    static let profileDelete = "qa_profile_delete"
    static let profileConfirm = "qa_profile_confirm"
    static let profileReject = "qa_profile_reject"

    static let validationError = "qa_validation_error"
    static let resultType = "qa_result_type"
    static let resultMdOrder = "qa_result_md_order"
    static let resultMdOrderCopy = "qa_result_md_order_copy"
    static let resultSessionId = "qa_result_session_id"
    static let resultOrderStatus = "qa_result_order_status"
    static let resultError = "qa_result_error"

    static let journalOpen = "qa_journal_open"
    static let journalText = "qa_journal_text"
    static let journalLevel = "qa_journal_level"
    static let journalSearch = "qa_journal_search"
    static let journalClear = "qa_journal_clear"

    /// The configuration fields, in the order of the form: the order `ConfigForm` reports its first problem in.
    static let fields: [String] = [
        profile,
        orderSource,
        orderType,
        orderAmount,
        orderCurrency,
        orderClientId,
        orderMdOrder,
        orderSessionId,
        orderFeatures,
        orderExtra,
        sdkLocale,
        sdkShowResultScreen,
        sdkScanner,
        sdkKeyProviderUrl,
        sdkTheme,
        sdkApplePayEnabled,
        sdkApplePayMerchantId,
        sdkApplePaySupportedNetworks,
        sdkApplePayMerchantCapabilities,
        sdkApplePayCountryCode,
        sdkSslPinningEnabled,
        sdkSslPinningPins,
    ]

    static let all: [String] = fields + [
        groupOrder, groupSdk, groupTheme, groupWallets, groupSecurity, themeLegend,
        launch, menu, importClipboard, importQr, export, reset, report, runAgain,
        profilesOpen, profileName, profileGatewayUrl, profileApi, profileLogin, profilePassword, profileApiKey,
        profileAdd, profileSave, profileDelete, profileConfirm, profileReject,
        validationError, resultType, resultMdOrder, resultMdOrderCopy, resultSessionId, resultOrderStatus, resultError,
        journalOpen, journalText, journalLevel, journalSearch, journalClear,
    ]

    static func profileSelect(_ name: String) -> String {
        "qa_profile_select_\(name)"
    }
}
