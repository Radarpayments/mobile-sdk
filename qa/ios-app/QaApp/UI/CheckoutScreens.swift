import SwiftUI

/// The theme preset; with `debug` the legend of the colours it paints.
struct ThemeScreen: View {
    @ObservedObject var form: ConfigForm
    let back: () -> Void

    var body: some View {
        QaScreen(title: "Theme", back: back) {
            Caption("Preset")
            FormMarks(form: form, tag: Tags.sdkTheme, options: ThemePreset.allCases.map { ($0.rawValue, themeLabel($0)) }, listTag: Tags.sdkTheme)
            if form.config.sdk.theme == .debug {
                Caption("Debug legend")
                ThemeLegend()
            }
        }
    }
}

private func themeLabel(_ preset: ThemePreset) -> String {
    switch preset {
    case .system: "System"
    case .light: "Light"
    case .dark: "Dark"
    case .contrast: "Contrast"
    case .rounded: "Rounded"
    case .square: "Square"
    case .largeText: "Large text"
    case .debug: "Debug"
    }
}

/// Which token of the `debug` theme each colour on the sheet stands for.
private struct ThemeLegend: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(debugLegend, id: \.token) { entry in
                HStack(spacing: 12) {
                    Rectangle()
                        .fill(color(entry.color.argb))
                        .overlay { Rectangle().strokeBorder(QaColor.line, lineWidth: QaSize.line) }
                        .frame(width: 16, height: 16)
                    Text("\(entry.token)  \(String(format: "%08llX", entry.color.argb))")
                        .qaStyle(QaType.journal)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .padding(.horizontal, QaSize.screen)
                .padding(.vertical, 6)
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

/// How the checkout behaves: the locale, the result screen, the scanner, the key provider.
struct BehaviourScreen: View {
    @ObservedObject var form: ConfigForm
    let back: () -> Void
    let go: (Route) -> Void

    var body: some View {
        let locale = form.config.sdk.locale
        QaScreen(title: "Behaviour", back: back) {
            Caption("Checkout")
            ListGroup {
                NavRow(label: "Locale", value: locale ?? "system", tag: Tags.sdkLocale, readsAs: locale ?? "") { go(.locale) }
                FormSwitch(form: form, tag: Tags.sdkShowResultScreen, label: "Result screen")
                FormSwitch(form: form, tag: Tags.sdkScanner, label: "Card scanner")
            }
            FormInput(
                form: form,
                tag: Tags.sdkKeyProviderUrl,
                caption: "Key provider URL",
                help: "Empty: the SDK asks the stand for its key",
                look: InputLook(keyboard: .URL, style: QaType.inputSmall)
            )
        }
    }
}

/// The locale of the sheet, or the system's; choosing one returns to Behaviour.
struct LocaleScreen: View {
    @ObservedObject var form: ConfigForm
    let back: () -> Void

    var body: some View {
        QaScreen(title: "Locale", back: back) {
            Caption("Locale")
            FormMarks(
                form: form,
                tag: Tags.sdkLocale,
                options: [("", "System locale")] + QaConfigJson.locales.map { ($0, $0) },
                onChosen: back
            )
        }
    }
}

/// Apple Pay on the sheet: whether it is offered, the merchant, the networks and capabilities, the country.
struct ApplePayScreen: View {
    @ObservedObject var form: ConfigForm
    let back: () -> Void

    var body: some View {
        QaScreen(title: "Apple Pay", back: back) {
            Caption("Apple Pay")
            ListGroup {
                FormSwitch(form: form, tag: Tags.sdkApplePayEnabled, label: "Apple Pay")
            }
            FormInput(form: form, tag: Tags.sdkApplePayMerchantId, caption: "Merchant id", look: .small)
            Caption("Card networks")
            FormChips(form: form, tag: Tags.sdkApplePaySupportedNetworks, known: QaConfigJson.applePayNetworks) {
                Tags.option(Tags.sdkApplePaySupportedNetworks, $0)
            }
            Caption("Merchant capabilities")
            FormChips(form: form, tag: Tags.sdkApplePayMerchantCapabilities, known: QaConfigJson.applePayCapabilities) {
                Tags.option(Tags.sdkApplePayMerchantCapabilities, $0)
            }
            FormInput(
                form: form,
                tag: Tags.sdkApplePayCountryCode,
                caption: "Country code",
                look: InputLook(placeholder: "US", style: QaType.inputSmall)
            )
        }
    }
}

/// SSL pinning of the SDK's connections: the switch and the pins.
struct SslPinningScreen: View {
    @ObservedObject var session: QaSession
    @ObservedObject var form: ConfigForm
    let back: () -> Void

    var body: some View {
        let problem = launchProblems(config: form.config, profile: session.selectedProfile)
            .first { sameText($0.field, "sdk.sslPinning.pins") }
        QaScreen(title: "SSL pinning", back: back) {
            Caption("SSL pinning")
            ListGroup {
                FormSwitch(form: form, tag: Tags.sdkSslPinningEnabled, label: "SSL pinning")
            }
            FormInput(
                form: form,
                tag: Tags.sdkSslPinningPins,
                caption: "Pins",
                help: "One host=pin per line; the pin is the base64 SHA-256 of the certificate's public key",
                problem: problem,
                look: InputLook(singleLine: false, style: QaType.inputSmall)
            )
        }
    }
}
