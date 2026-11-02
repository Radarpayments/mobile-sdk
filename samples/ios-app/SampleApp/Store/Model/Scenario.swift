import Foundation

/// The demo panel: what the next payment looks like, the same in every cover.
struct Scenario: Codable, Equatable {
    /// true = the order is registered with the demo buyer's clientId, so cards saved earlier show up.
    var buyerWithCards = false
    /// Show Apple Pay (when the merchant has it set up).
    var wallet = true
    /// Show the card scanner on the card form.
    var scanner = true
    /// Let the SDK show its own success / declined screen before returning.
    var sdkResultScreen = false
}

/// Which theme the SDK sheet uses: the cover's brand colours or a stock one.
enum SheetThemeOverride: String, CaseIterable, Codable {
    case cover, light, dark, system
}
