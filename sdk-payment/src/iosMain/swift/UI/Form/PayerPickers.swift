import SwiftUI

extension View {
    /// Raises the list the payer is picking from over the checkout.
    func payerPickers(store: PayerStore) -> some View {
        sheetOverlay(id: store.picker?.id, onDismiss: { store.picker = nil }, content: {
            if let picker = store.picker {
                PayerPickerContent(store: store, picker: picker)
            }
        })
    }
}

private struct PayerPickerContent: View {
    let store: PayerStore
    let picker: PayerPicker

    @Environment(\.checkoutStrings) private var strings

    var body: some View {
        let form = store.form
        switch picker {
        case .country(let field):
            CountryListSheet(
                title: text("payer_country_label"),
                searchHint: text("payer_country_search"),
                countries: form.countries(),
                suggested: [],
                selected: form.value(field: field),
                flag: form.flag(iso:),
                onSelect: { code in
                    store.change { $0.selectCountry(field: field, code: code) }
                    store.picker = nil
                }
            )
        case .phoneCode:
            let all = form.phoneCountries()
            CountryListSheet(
                title: text("phone_code_title"),
                searchHint: text("phone_code_search"),
                countries: all,
                suggested: form.suggestedPhoneCountries.compactMap { iso in all.first { $0.code == iso } },
                selected: form.phoneIso,
                flag: form.flag(iso:),
                onSelect: { iso in
                    store.change { $0.selectPhoneCountry(iso: iso) }
                    store.picker = nil
                }
            )
        case .option(let field):
            OptionListSheet(
                title: form.baseLabel(field: field),
                options: form.options(field: field),
                selected: form.value(field: field),
                label: form.optionLabel(code:),
                onSelect: { code in
                    store.change { $0.selectOption(field: field, code: code) }
                    store.picker = nil
                }
            )
        }
    }

    private func text(_ key: String) -> String { strings?.text(key: key) ?? "" }
}

/// A searchable list of countries; it spans the screen whatever the search leaves of it.
private struct CountryListSheet: View {
    let title: String
    let searchHint: String
    let countries: [CountryOptionSpec]
    let suggested: [CountryOptionSpec]
    let selected: String?
    let flag: (String) -> String
    let onSelect: (String) -> Void

    @State private var query = ""
    @State private var searchFocused = false
    @Environment(\.checkoutTheme) private var theme
    @Environment(\.checkoutStrings) private var strings
    @EnvironmentObject private var insets: SheetInsets

    var body: some View {
        let needle = query.trimmingCharacters(in: .whitespaces)
        let shown = countries.filter { $0.matches(query: needle) }
        Text(title).line(theme.headline, color: theme.colors.onBackground).accessibilityAddTraits(.isHeader)
        FieldBox(focused: searchFocused) {
            IconView(icon: .search, size: 20, color: theme.colors.onSurfaceVariant)
            FormTextField(
                text: query, placeholder: searchHint, label: searchHint, onChange: { query = $0 }, onFocus: { searchFocused = $0 }
            )
                .padding(.leading, 6)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        ScrollView(.vertical, showsIndicators: false) {
            LazyVStack(alignment: .leading, spacing: 0) {
                if shown.isEmpty {
                    Text(strings?.text(key: "payer_country_not_found") ?? "")
                        .line(theme.body, color: theme.colors.onSurfaceVariant)
                        .padding(.horizontal, 4)
                        .padding(.vertical, 16)
                }
                if needle.isEmpty && !suggested.isEmpty {
                    ListHeader(text: strings?.text(key: "picker_suggested") ?? "")
                    ForEach(suggested, id: \.code) { row($0) }
                    ListHeader(text: strings?.text(key: "picker_all") ?? "")
                }
                ForEach(shown, id: \.code) { row($0) }
            }
        }
        .frame(height: max(listHeight, 0))
        // The rows would run on over the sheet's bottom padding, where Compose's list ends.
        .clipped()
    }

    /// What is left of the screen under the title and the search field, inside the overlay's own paddings.
    private var listHeight: CGFloat {
        let sheet = UIScreen.main.bounds.height - insets.top - 24
        return sheet - 8 - 14 - 14 - theme.headline.singleLineHeight - 14 - fieldHeight - 14 - insets.sheetBottom - 12
    }

    private func row(_ country: CountryOptionSpec) -> some View {
        let isSelected = country.code == selected
        return Button(action: { onSelect(country.code) }, label: {
            HStack(spacing: 12) {
                FlagIcon(emoji: flag(country.code))
                Text(country.name)
                    .line(theme.body, color: theme.colors.onBackground)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if let dialCode = country.dialCode {
                    Text(PayerForms.shared.leftToRight(text: "+\(dialCode)"))
                        .line(theme.body.tabular(), color: theme.colors.onSurfaceVariant)
                }
                SelectedMark(selected: isSelected)
            }
            .padding(.horizontal, 4)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        })
        .buttonStyle(PlainPress())
        .accessibilityElement(children: .ignore)
        .accessibility(label: Text(country.dialCode.map { "\(country.name), +\($0)" } ?? country.name))
        .accessibility(addTraits: isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

private struct ListHeader: View {
    let text: String

    private static let spacing: CGFloat = 0.6
    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        let font = MatchedFont.font(theme.caption.uiFont)
        Text(text.uppercased())
            .font(Font(font))
            .kerning(Self.spacing - MatchedFont.tracking(of: font))
            .foregroundColor(theme.colors.onSurfaceVariant)
            .lineLimit(1)
            .frame(height: theme.caption.singleLineHeight)
            .padding(EdgeInsets(top: 14, leading: 4, bottom: 4, trailing: 0))
            .accessibilityAddTraits(.isHeader)
    }
}

private struct SelectedMark: View {
    let selected: Bool

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        ZStack {
            if selected {
                IconView(icon: .check, size: 20, color: theme.colors.primary)
            }
        }
        .frame(width: 22, height: 22)
    }
}

/// A short list of named options, e.g. the kinds of an identity document.
private struct OptionListSheet: View {
    let title: String
    let options: [String]
    let selected: String
    let label: (String) -> String
    let onSelect: (String) -> Void

    @Environment(\.checkoutTheme) private var theme

    var body: some View {
        Text(title).line(theme.headline, color: theme.colors.onBackground).accessibilityAddTraits(.isHeader)
        VStack(spacing: 0) {
            ForEach(options, id: \.self) { code in
                Button(action: { onSelect(code) }, label: {
                    HStack(spacing: 12) {
                        Text(label(code))
                            .line(theme.body, color: theme.colors.onBackground)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        SelectedMark(selected: code == selected)
                    }
                    .padding(.horizontal, 4)
                    .padding(.vertical, 12)
                    .contentShape(Rectangle())
                })
                .buttonStyle(PlainPress())
                .accessibility(identifier: "transfer_option_\(code)")
                .accessibility(addTraits: code == selected ? [.isButton, .isSelected] : .isButton)
            }
        }
    }
}
