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
    static let importText = "qa_import_text"
    static let importTextField = "qa_import_text_field"
    static let importTextApply = "qa_import_text_apply"
    static let export = "qa_export"
    static let reset = "qa_reset"
    static let report = "qa_report"
    static let runAgain = "qa_run_again"

    static let profilesOpen = "qa_profiles_open"
    static let homeAmount = "qa_home_amount"
    static let homeFeatures = "qa_home_features"
    static let homeLastRun = "qa_home_last_run"
    static let orderFeaturesOther = "qa_order_features_other"
    static let orderFeaturesOtherName = "qa_order_features_other_name"
    static let orderFeaturesOtherAdd = "qa_order_features_other_add"
    static let orderClientIdOpen = "qa_order_client_id_open"
    static let orderExtraOpen = "qa_order_extra_open"
    static let orderMdOrderPaste = "qa_order_md_order_paste"
    static let orderSessionIdPaste = "qa_order_session_id_paste"

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
    static let profileEnter = "qa_profile_enter"
    static let profilePaste = "qa_profile_paste"
    static let profilePasteText = "qa_profile_paste_text"
    static let profileScan = "qa_profile_scan"
    static let profilePasswordShow = "qa_profile_password_show"
    static let profileApiKeyShow = "qa_profile_api_key_show"

    static let validationError = "qa_validation_error"
    static let resultType = "qa_result_type"
    static let resultMdOrder = "qa_result_md_order"
    static let resultMdOrderCopy = "qa_result_md_order_copy"
    static let resultSessionId = "qa_result_session_id"
    static let resultOrderStatus = "qa_result_order_status"
    static let resultError = "qa_result_error"
    static let resultSessionIdCopy = "qa_result_session_id_copy"
    static let resultStand = "qa_result_stand"
    static let resultOrder = "qa_result_order"
    static let resultTheme = "qa_result_theme"

    static let journalOpen = "qa_journal_open"
    static let journalText = "qa_journal_text"
    static let journalLevel = "qa_journal_level"
    static let journalSearch = "qa_journal_search"
    static let journalClear = "qa_journal_clear"
    static let journalCopy = "qa_journal_copy"

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
        importText, importTextField, importTextApply,
        profilesOpen, homeAmount, homeFeatures, homeLastRun,
        orderFeaturesOther, orderFeaturesOtherName, orderFeaturesOtherAdd, orderClientIdOpen, orderExtraOpen,
        orderMdOrderPaste, orderSessionIdPaste,
        profileName, profileGatewayUrl, profileApi, profileLogin, profilePassword, profileApiKey,
        profileAdd, profileSave, profileDelete, profileConfirm, profileReject,
        profileEnter, profilePaste, profilePasteText, profileScan, profilePasswordShow, profileApiKeyShow,
        validationError, resultType, resultMdOrder, resultMdOrderCopy, resultSessionId, resultOrderStatus, resultError,
        resultSessionIdCopy, resultStand, resultOrder, resultTheme,
        journalOpen, journalText, journalLevel, journalSearch, journalClear, journalCopy,
    ]

    /// The identifiers that end in a value, as `qa/fixtures/identifiers.txt` writes them: an option of a choice, a chip
    /// of a set, a profile's row. The value part is what `option` makes of the value.
    static let patterns: [String] = [
        "\(orderSource)_<value>",
        "\(orderType)_<value>",
        "\(orderCurrency)_<value>",
        "\(orderFeature)_<value>",
        "\(sdkTheme)_<value>",
        "\(sdkLocale)_<value>",
        "\(sdkApplePaySupportedNetworks)_<value>",
        "\(sdkApplePayMerchantCapabilities)_<value>",
        "\(profileSelectPrefix)_<name>",
        "\(profileEditPrefix)_<name>",
        "\(profileApi)_<value>",
        "\(journalFilterPrefix)_<value>",
    ]

    /// The identifier of one option of [field]: the value in snake case (a camel hump becomes `_`), lower case, with
    /// every run of other characters than `a-z` and `0-9` as one `_`, and `none` for no value: `qa_order_currency_eur`,
    /// `qa_sdk_theme_large_text`, `qa_sdk_locale_none`. Works on scalars, as Android's regexes work on chars.
    static func option(_ field: String, _ value: String?) -> String {
        var out = String.UnicodeScalarView()
        var previousLower = false
        var inRun = false
        for scalar in (value ?? "").unicodeScalars {
            let isLower = scalar.value >= 0x61 && scalar.value <= 0x7A
            let isUpper = scalar.value >= 0x41 && scalar.value <= 0x5A
            let isDigit = scalar.value >= 0x30 && scalar.value <= 0x39
            if isUpper && previousLower {
                out.append("_")
            }
            if isLower || isDigit {
                out.append(scalar)
                inRun = false
            } else if isUpper {
                out.append(Unicode.Scalar(scalar.value + 0x20)!)
                inRun = false
            } else if !inRun {
                out.append("_")
                inRun = true
            }
            previousLower = isLower
        }
        let id = String(out)
        return "\(field)_\(id.isEmpty ? "none" : id)"
    }

    static func feature(_ name: String) -> String {
        option(orderFeature, name)
    }

    static func profileSelect(_ name: String) -> String {
        "\(profileSelectPrefix)_\(name)"
    }

    static func profileEdit(_ name: String) -> String {
        "\(profileEditPrefix)_\(name)"
    }

    static func journalFilter(_ id: String) -> String {
        "\(journalFilterPrefix)_\(id)"
    }

    private static let orderFeature = "qa_order_feature"
    private static let profileSelectPrefix = "qa_profile_select"
    private static let profileEditPrefix = "qa_profile_edit"
    private static let journalFilterPrefix = "qa_journal_filter"
}
