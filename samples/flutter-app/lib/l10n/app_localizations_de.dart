// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for German (`de`).
class AppLocalizationsDe extends AppLocalizations {
  AppLocalizationsDe([String locale = 'de']) : super(locale);

  @override
  String get startTitle => 'Zahlungs-Demo';

  @override
  String get startSettings => 'Entwicklereinstellungen';

  @override
  String get coverFurnitureName => 'Hearth & Co';

  @override
  String get coverCoffeeName => 'Roast Lane';

  @override
  String get coverTicketsName => 'Stage Pass';

  @override
  String get coverFurnitureTagline => 'Möbel · Katalog und Warenkorb';

  @override
  String get coverCoffeeTagline => 'Café · online vorbestellen';

  @override
  String get coverTicketsTagline => 'Tickets · Konzerte und Shows';

  @override
  String get storeBack => 'Zurück';

  @override
  String get storeCart => 'Warenkorb';

  @override
  String storeCartCount(int count) {
    return 'Warenkorb · $count';
  }

  @override
  String get storeItems => 'Artikel';

  @override
  String get storeTotal => 'Gesamt';

  @override
  String storePay(String amount) {
    return '$amount bezahlen';
  }

  @override
  String get storeIncrease => 'Mehr';

  @override
  String get storeDecrease => 'Weniger';

  @override
  String storeAddToCart(String price) {
    return 'In den Warenkorb · $price';
  }

  @override
  String get storeAdd => 'Hinzufügen';

  @override
  String get storeRegistering => 'Bestellung wird angelegt…';

  @override
  String get storeErrorCredentials => 'Händler-Login und Passwort sind nicht festgelegt.';

  @override
  String get storeErrorApiKey => 'Der API-Schlüssel ist nicht festgelegt.';

  @override
  String storeErrorFailed(String message) {
    return 'Die Bestellung wurde nicht angelegt: $message';
  }

  @override
  String get storeOpenSettings => 'Einstellungen öffnen';

  @override
  String scenarioRow(String buyer) {
    return 'Szenario: $buyer';
  }

  @override
  String get scenarioTitle => 'Zahlungsszenario';

  @override
  String get scenarioBuyer => 'Käufer';

  @override
  String get scenarioGuest => 'Gast';

  @override
  String get scenarioWithCards => 'Mit Karten';

  @override
  String get scenarioWithCardsHint => 'Karten erscheinen nach einer Zahlung mit „Karte speichern“';

  @override
  String get scenarioReset => 'Zurücksetzen';

  @override
  String get scenarioPayment => 'Zahlung';

  @override
  String get scenarioWallet => 'Google Pay';

  @override
  String get scenarioScanner => 'Kartenscanner';

  @override
  String get scenarioSdkResult => 'SDK-Ergebnisbildschirm';

  @override
  String resultPaidAmount(String amount) {
    return '$amount bezahlt';
  }

  @override
  String get resultPaymentId => 'Zahlung';

  @override
  String get resultAmount => 'Betrag';

  @override
  String get resultDeclinedTitle => 'Zahlung abgelehnt';

  @override
  String get resultDeclinedBody => 'Die Bank hat die Zahlung nicht bestätigt.';

  @override
  String get resultUnconfirmedTitle => 'Wir prüfen Ihre Zahlung';

  @override
  String get resultUnconfirmedBody =>
      'Die Bank hat noch nicht geantwortet. Bitte zahlen Sie nicht erneut – wir melden uns, sobald die Zahlung bestätigt ist.';

  @override
  String get resultTryAgain => 'Erneut versuchen';

  @override
  String get resultBackToStore => 'Zurück zum Shop';

  @override
  String get resultDone => 'Fertig';

  @override
  String get devTitle => 'Entwicklereinstellungen';

  @override
  String get devGateway => 'Gateway';

  @override
  String get devBaseUrl => 'Base URL';

  @override
  String get devLogin => 'Händler-Login';

  @override
  String get devPassword => 'Passwort';

  @override
  String get devApiV1 => 'API V1';

  @override
  String get devApiV2 => 'API V2';

  @override
  String get devApiVersion => 'Händler-API';

  @override
  String get devApiV1Hint => 'Bestellungen werden über register.do mit Händler-Login und Passwort angelegt.';

  @override
  String get devApiV2Hint => 'Sessions werden über POST /api2/sessions mit dem API-Schlüssel erstellt.';

  @override
  String get devApiKey => 'API-Schlüssel';

  @override
  String get devOrderType => 'Bestellung';

  @override
  String get devOrderPurchase => 'Zahlung';

  @override
  String get devOrderAft => 'AFT-Überweisung';

  @override
  String get devOrderAftHint =>
      'Überweisungen werden über registerP2P.do angelegt (einseitiges AFT): Das Fenster fragt nach den Daten von Absender und Empfänger.';

  @override
  String get devCredentialsNote => 'Nur auf diesem Gerät gespeichert. In Ihrer App legt Ihr Backend die Bestellung an.';

  @override
  String get devSdkLook => 'SDK-Darstellung';

  @override
  String get devSheetTheme => 'Design des Zahlungsfensters';

  @override
  String get devThemeCover => 'Wie der Shop';

  @override
  String get devThemeLight => 'Hell';

  @override
  String get devThemeDark => 'Dunkel';

  @override
  String get devThemeSystem => 'System';

  @override
  String get devLanguage => 'Sprache';

  @override
  String get devLanguageSystem => 'System';

  @override
  String get devWallets => 'Google Pay';

  @override
  String get devGpayGateway => 'Gateway';

  @override
  String get devGpayMerchant => 'Merchant ID';

  @override
  String get devGpayFromGateway => 'Vom Zahlungs-Gateway';

  @override
  String get devMore => 'Mehr';

  @override
  String get devAdvanced => 'Advanced';

  @override
  String get devAdvancedSub => 'Zustandsstream, SSL-Pinning';

  @override
  String get devLastOrder => 'Letzte Bestellung';

  @override
  String get devCopy => 'Kopieren';

  @override
  String get devNone => '—';

  @override
  String get resultSuccessFurniture => 'Vielen Dank für Ihre Bestellung';

  @override
  String get resultSuccessFurnitureBody => 'Wir melden uns, um die Lieferung abzustimmen.';

  @override
  String get resultSuccessCoffee => 'Ihre Bestellung wird zubereitet';

  @override
  String get resultSuccessCoffeeBody => 'Zeigen Sie diese Nummer dem Barista.';

  @override
  String get resultSuccessTickets => 'Sie sind dabei!';

  @override
  String get resultSuccessTicketsBody => 'Die Tickets finden Sie in Ihrem Konto.';

  @override
  String get furnitureCollection => 'Herbst-\nkollektion';

  @override
  String get furnitureAll => 'Alle';

  @override
  String get furnitureChairs => 'Stühle';

  @override
  String get furnitureSofas => 'Sofas';

  @override
  String get furnitureLight => 'Leuchten';

  @override
  String get furnitureDelivery => 'Kostenlose Lieferung in 3–5 Tagen. 30 Tage Rückgaberecht.';

  @override
  String get furnitureDeliveryLabel => 'Lieferung';

  @override
  String get furnitureDeliveryFree => 'Kostenlos';

  @override
  String get furnitureColours => 'Farbe';

  @override
  String get coffeeLocation => 'King St.';

  @override
  String get coffeeAgain => 'Wie beim letzten Mal';

  @override
  String get coffeeRepeat => 'Erneut bestellen';

  @override
  String get coffeeTabCoffee => 'Kaffee';

  @override
  String get coffeeTabCold => 'Kalt';

  @override
  String get coffeeTabBakery => 'Gebäck';

  @override
  String get coffeeOrderTitle => 'Ihre Bestellung';

  @override
  String get coffeePickup => 'Abholung';

  @override
  String get coffeePickupValue => 'King St. · in 10 Min.';

  @override
  String coffeeAddItem(String item) {
    return '$item hinzufügen';
  }

  @override
  String get coffeeOrderNumberHint => 'In etwa 6 Minuten fertig.';

  @override
  String get ticketsLive => 'Live in Hall A';

  @override
  String get ticketsTitle => 'NORTHERN\nLIGHTS';

  @override
  String get ticketsDate => 'Sa, 14. Nov. · 20:00 · Hall A';

  @override
  String get ticketsSection => 'Tickets';

  @override
  String ticketsToPayment(String amount) {
    return 'Zur Kasse · $amount';
  }

  @override
  String get ticketsCheckout => 'Kasse';

  @override
  String ticketsEventLine(String category) {
    return '14. Nov. · $category';
  }

  @override
  String ticketsHold(String time) {
    return 'Plätze noch $time reserviert';
  }

  @override
  String get ticketsHoldExpired => 'Die Reservierung ist abgelaufen. Wählen Sie Ihre Plätze erneut.';

  @override
  String get ticketsSeats => 'Tickets';

  @override
  String get productVelvetSofa => 'Velvet sofa';

  @override
  String get productCloudChair => 'Cloud chair';

  @override
  String get productOnoChair => 'Ono chair';

  @override
  String get productBirchStool => 'Birch stool';

  @override
  String get productArcLamp => 'Arc floor lamp';

  @override
  String get productLumeArmchair => 'Lume armchair';

  @override
  String get productFlatWhite => 'Flat white';

  @override
  String get productCappuccino => 'Cappuccino';

  @override
  String get productEspresso => 'Espresso';

  @override
  String get productIcedLatte => 'Iced latte';

  @override
  String get productCroissant => 'Butter croissant';

  @override
  String get productVelvetSofaSub => 'Smaragdgrüner Samt, Eichenbeine';

  @override
  String get productCloudChairSub => 'Milchweißes Bouclé';

  @override
  String get productOnoChairSub => 'Formschale, Buchenbeine';

  @override
  String get productBirchStoolSub => 'Massive Birke';

  @override
  String get productArcLampSub => 'Matter Stahl in Graphit';

  @override
  String get productLumeArmchairSub => 'Senfgelbe Wolle';

  @override
  String get productFlatWhiteSub => 'Doppelter Espresso, aufgeschäumte Milch';

  @override
  String get productCappuccinoSub => 'Klassisch, mit Milchschaum';

  @override
  String get productEspressoSub => 'Single Origin';

  @override
  String get productIcedLatteSub => 'Kalte Milch, Eis';

  @override
  String get productCroissantSub => 'Heute früh gebacken';

  @override
  String get ticketFloor => 'Innenraum';

  @override
  String get ticketFloorSub => 'Stehplätze';

  @override
  String get ticketBalcony => 'Balkon';

  @override
  String get ticketBalconySub => 'Reihe 2 · Sitzplätze';

  @override
  String get ticketFee => 'Servicegebühr';

  @override
  String get startSectionPayment => 'SDK Payment';

  @override
  String get startSectionPaymentSub => 'Ein fertiges Zahlungsfenster für Ihre App — jeder Shop enthält alle Szenarien';

  @override
  String get startSectionCore => 'SDK Core';

  @override
  String get startSectionCoreSub => 'Kartenverschlüsselung für Ihr eigenes Formular (PCI DSS)';

  @override
  String get coreTileTitle => 'Eigenes Kartenformular';

  @override
  String get coreTileSub => 'Kartendaten werden zu einem Token für Ihren Server';

  @override
  String get coreTitle => 'SDK Core';

  @override
  String get coreDoneSubtitle => 'Token ist bereit';

  @override
  String get coreVariantNew => 'Neue Karte';

  @override
  String get coreVariantInstant => 'Ohne Bestellung';

  @override
  String get coreVariantStored => 'Gespeichert';

  @override
  String get coreHintNew => 'Zahlung einer registrierten Bestellung: Kartendaten plus deren mdOrder.';

  @override
  String get coreHintInstant => 'Noch keine Bestellung: Ihr Server legt sie an und bezahlt in einem Aufruf.';

  @override
  String get coreHintStored => 'Zahlung einer Bestellung mit gespeicherter Karte: bindingId statt Kartendaten.';

  @override
  String get coreCardGroup => 'Kartendaten';

  @override
  String get corePan => 'Kartennummer';

  @override
  String get coreExpiryPlaceholder => 'MM/JJ';

  @override
  String get coreExpiry => 'Ablaufdatum';

  @override
  String get coreCvc => 'CVC/CVV';

  @override
  String get coreCvcOptional => 'CVC/CVV (optional)';

  @override
  String get coreHolder => 'Name auf der Karte (optional)';

  @override
  String get coreBinding => 'bindingId';

  @override
  String get coreMdOrder => 'mdOrder';

  @override
  String get coreMdOrderHint => 'Von Ihrem Backend';

  @override
  String get corePubKey => 'Öffentlicher Schlüssel (PEM)';

  @override
  String get corePaste => 'Einfügen';

  @override
  String get coreGenerate => 'Token abrufen';

  @override
  String get coreErrorRequired => 'Pflichtfeld';

  @override
  String get coreErrorInvalid => 'Ungültiger Wert';

  @override
  String get coreErrorFormat => 'Falsches Format';

  @override
  String get coreTokenOk => 'Token erhalten';

  @override
  String get coreCopy => 'Kopieren';

  @override
  String get coreCopied => 'Kopiert';

  @override
  String get coreStepSend => 'Senden Sie den Token an Ihren Server';

  @override
  String get coreStepGateway => 'Ihr Server übergibt ihn als seToken an das Gateway';

  @override
  String get coreStepPay => 'Das Gateway verarbeitet die Zahlung';

  @override
  String get coreAgain => 'Noch einmal';

  @override
  String get investName => 'Northwind Invest';

  @override
  String get startSectionPaymentAftSub => 'AFT: ein Konto per Karte aufladen';

  @override
  String get investTileSub => 'Laden Sie Ihr Anlagekonto per Karte auf und sehen Sie, wie der Saldo wächst';

  @override
  String investAccount(String account) {
    return 'Anlagekonto •• $account';
  }

  @override
  String get investPortfolioValue => 'Depotwert';

  @override
  String get investChangeMonth => 'diesen Monat';

  @override
  String get investPeriod1w => '1W';

  @override
  String get investPeriod1m => '1M';

  @override
  String get investPeriod3m => '3M';

  @override
  String get investPeriod1y => '1J';

  @override
  String get investPeriodAll => 'Alle';

  @override
  String get investTopUp => 'Aufladen';

  @override
  String get investInvest => 'Investieren';

  @override
  String get investFreeCash => 'Freies Guthaben';

  @override
  String get investFreeCashSub => 'Bereit zum Investieren';

  @override
  String investFreeCashJustNow(String amount) {
    return '+$amount gerade eben';
  }

  @override
  String get investMyAssets => 'Meine Anlagen';

  @override
  String investUnits(String units, String price) {
    return '$units Anteile · $price';
  }

  @override
  String get investRecent => 'Letzte Vorgänge';

  @override
  String get investOpTopUp => 'Aufladung';

  @override
  String investOpBuy(String asset) {
    return 'Kauf $asset';
  }

  @override
  String get investAssetGlb => 'Global ETF';

  @override
  String get investAssetTch => 'Tech Leaders';

  @override
  String get investNotifications => 'Benachrichtigungen';

  @override
  String get topupTitle => 'Aufladen';

  @override
  String get topupTo => 'Auf';

  @override
  String get topupAvailable => 'Verfügbar';

  @override
  String get topupAmount => 'Betrag';

  @override
  String topupLimits(String min, String max) {
    return 'von $min bis $max';
  }

  @override
  String topupContinue(String amount) {
    return 'Weiter · $amount';
  }

  @override
  String get topupCreating => 'Überweisung wird erstellt…';

  @override
  String get topupFailedTitle => 'Überweisung fehlgeschlagen';

  @override
  String get topupFailedBody =>
      'Die Bank der Karte hat den Vorgang abgelehnt. Es wurde nichts abgebucht; versuchen Sie eine andere Karte.';

  @override
  String topupFailedDetail(String message) {
    return 'Gateway: $message';
  }

  @override
  String get topupUnknownTitle => 'Status unbekannt';

  @override
  String get topupUnknownBody => 'Prüfen Sie die Bestellung im Händlerportal, bevor Sie es erneut versuchen.';

  @override
  String get topupTryAgain => 'Erneut versuchen';

  @override
  String doneCredited(String account) {
    return 'dem Anlagekonto •• $account gutgeschrieben';
  }

  @override
  String get doneOperation => 'Vorgang';

  @override
  String get doneOperationValue => 'AFT · auf eigenes Konto';

  @override
  String get doneFreeCash => 'Freies Guthaben';

  @override
  String get doneOrder => 'Bestellung';

  @override
  String get doneTime => 'Zeit';

  @override
  String doneInvest(String amount) {
    return '$amount investieren';
  }

  @override
  String get doneToPortfolio => 'Zum Depot';

  @override
  String get devInvestReset => 'Northwind-Demokonto zurücksetzen';

  @override
  String get devInvestResetDone => 'Demokonto zurückgesetzt';

  @override
  String get scenarioWalletApple => 'Apple Pay';

  @override
  String get devApplePay => 'Apple Pay';
}
