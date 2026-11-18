import SwiftUI

/// The five groups of the configuration form; each field carries its identifier from `Tags`.
struct ConfigGroups: View {
    @ObservedObject var form: ConfigForm

    var body: some View {
        FormGroup(tag: Tags.groupOrder, title: "Order") {
            FormChoice(form: form, tag: Tags.orderSource, label: "Source", options: OrderSource.allCases.map(\.rawValue))
            FormChoice(form: form, tag: Tags.orderType, label: "Type", options: OrderType.allCases.map(\.rawValue))
            FormText(form: form, tag: Tags.orderAmount, label: "Amount, minor units", keyboard: .numberPad)
            FormChoice(form: form, tag: Tags.orderCurrency, label: "Currency", options: Currencies.codes)
            FormText(form: form, tag: Tags.orderClientId, label: "Client id")
            FormText(form: form, tag: Tags.orderMdOrder, label: "mdOrder")
            FormText(form: form, tag: Tags.orderSessionId, label: "Session id")
            FormText(form: form, tag: Tags.orderFeatures, label: "Features, comma separated")
            FormText(form: form, tag: Tags.orderExtra, label: "Extra parameters, JSON object", multiline: true)
        }
        FormGroup(tag: Tags.groupSdk, title: "SDK") {
            FormChoice(form: form, tag: Tags.sdkLocale, label: "Locale", options: [""] + QaConfigJson.locales)
            FormSwitch(form: form, tag: Tags.sdkShowResultScreen, label: "Result screen")
            FormSwitch(form: form, tag: Tags.sdkScanner, label: "Card scanner")
            FormText(form: form, tag: Tags.sdkKeyProviderUrl, label: "Key provider URL", keyboard: .URL)
        }
        FormGroup(tag: Tags.groupTheme, title: "Theme") {
            FormChoice(form: form, tag: Tags.sdkTheme, label: "Preset", options: ThemePreset.allCases.map(\.rawValue))
            if form.config.sdk.theme == .debug {
                ThemeLegend()
            }
        }
        FormGroup(tag: Tags.groupWallets, title: "Wallets") {
            FormSwitch(form: form, tag: Tags.sdkApplePayEnabled, label: "Apple Pay")
            FormText(form: form, tag: Tags.sdkApplePayMerchantId, label: "Merchant id")
            FormText(form: form, tag: Tags.sdkApplePaySupportedNetworks, label: "Supported networks, comma separated")
            FormText(form: form, tag: Tags.sdkApplePayMerchantCapabilities, label: "Merchant capabilities, comma separated")
            FormText(form: form, tag: Tags.sdkApplePayCountryCode, label: "Country code")
        }
        FormGroup(tag: Tags.groupSecurity, title: "Security") {
            FormSwitch(form: form, tag: Tags.sdkSslPinningEnabled, label: "SSL pinning")
            FormText(form: form, tag: Tags.sdkSslPinningPins, label: "Pins, host=pin per line", multiline: true)
        }
    }
}

/// Which token of the `debug` theme each colour on the sheet stands for.
private struct ThemeLegend: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            ForEach(debugLegend, id: \.token) { entry in
                HStack(spacing: 8) {
                    RoundedRectangle(cornerRadius: 3)
                        .fill(color(entry.color.argb))
                        .overlay(RoundedRectangle(cornerRadius: 3).stroke(.secondary, lineWidth: 0.5))
                        .frame(width: 20, height: 20)
                    Text(entry.token)
                    Spacer()
                    Text(String(format: "%08llX", entry.color.argb)).font(.system(.caption, design: .monospaced))
                }
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityIdentifier(Tags.themeLegend)
    }

    private func color(_ argb: Int64) -> Color {
        func channel(_ shift: Int64) -> Double { Double((argb >> shift) & 0xFF) / 255 }
        return Color(.sRGB, red: channel(16), green: channel(8), blue: channel(0), opacity: channel(24))
    }
}
