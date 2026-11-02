/// The demo panel: what the next payment looks like, the same in every cover.
///
/// [buyerWithCards]: true = the order is registered with the demo buyer's clientId, so cards saved
/// in earlier payments show up; false = a guest without saved cards. [wallet] shows Google Pay /
/// Apple Pay (when the merchant has it set up), [scanner] the card scanner on the card form, and
/// [sdkResultScreen] lets the SDK show its own success / declined screen before returning.
class Scenario {
  const Scenario({this.buyerWithCards = false, this.wallet = true, this.scanner = true, this.sdkResultScreen = false});

  final bool buyerWithCards;
  final bool wallet;
  final bool scanner;
  final bool sdkResultScreen;

  Scenario copyWith({bool? buyerWithCards, bool? wallet, bool? scanner, bool? sdkResultScreen}) => Scenario(
    buyerWithCards: buyerWithCards ?? this.buyerWithCards,
    wallet: wallet ?? this.wallet,
    scanner: scanner ?? this.scanner,
    sdkResultScreen: sdkResultScreen ?? this.sdkResultScreen,
  );

  @override
  bool operator ==(Object other) =>
      other is Scenario &&
      other.buyerWithCards == buyerWithCards &&
      other.wallet == wallet &&
      other.scanner == scanner &&
      other.sdkResultScreen == sdkResultScreen;

  @override
  int get hashCode => Object.hash(buyerWithCards, wallet, scanner, sdkResultScreen);
}

/// Which theme the SDK sheet uses: the cover's brand colours or a stock one.
enum SheetThemeOverride { cover, light, dark, system }
