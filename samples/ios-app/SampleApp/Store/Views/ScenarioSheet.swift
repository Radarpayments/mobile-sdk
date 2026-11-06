import SwiftUI

/// The demo panel. Neutral on purpose — it is a tool for the person showing the demo, not part of the
/// store — and the same in every cover.
struct ScenarioSheet: View {
    @Binding var scenario: Scenario
    /// False where the buyer is always the one with saved cards (Northwind): only "Reset" is left.
    var buyerChoice = true

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    if buyerChoice {
                        Picker("Buyer", selection: $scenario.buyerWithCards) {
                            Text("Guest").tag(false)
                            Text("With cards").tag(true).accessibilityIdentifier("scenario_with_cards")
                        }
                        .pickerStyle(.segmented)
                    }
                    HStack {
                        Text("Cards appear after a payment with “Save card”")
                            .font(.footnote)
                            .foregroundColor(.secondary)
                        Spacer()
                        Button("Reset") { StorePrefs().resetClientId() }
                    }
                } header: {
                    Text("Buyer")
                }
                Section {
                    Toggle("Apple Pay", isOn: $scenario.wallet)
                    Toggle("Card scanner", isOn: $scenario.scanner)
                    Toggle("SDK result screen", isOn: $scenario.sdkResultScreen)
                } header: {
                    Text("Payment")
                }
            }
            .navigationTitle("Payment scenario")
            .navigationBarTitleDisplayMode(.inline)
        }
        .presentationDetents([.medium, .large])
        .environment(\.colorScheme, .light)
    }
}
