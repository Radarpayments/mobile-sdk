// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Romanian Moldavian Moldovan (`ro`).
class AppLocalizationsRo extends AppLocalizations {
  AppLocalizationsRo([String locale = 'ro']) : super(locale);

  @override
  String get startTitle => 'Demo de plată';

  @override
  String get startSettings => 'Setări pentru dezvoltatori';

  @override
  String get coverFurnitureName => 'Hearth & Co';

  @override
  String get coverCoffeeName => 'Roast Lane';

  @override
  String get coverTicketsName => 'Stage Pass';

  @override
  String get coverFurnitureTagline => 'Mobilă · catalog și coș';

  @override
  String get coverCoffeeTagline => 'Cafenea · comandă în avans';

  @override
  String get coverTicketsTagline => 'Bilete · concerte și spectacole';

  @override
  String get storeBack => 'Înapoi';

  @override
  String get storeCart => 'Coș';

  @override
  String storeCartCount(int count) {
    return 'Coș · $count';
  }

  @override
  String get storeItems => 'Produse';

  @override
  String get storeTotal => 'Total';

  @override
  String storePay(String amount) {
    return 'Plătiți $amount';
  }

  @override
  String get storeIncrease => 'Mai mult';

  @override
  String get storeDecrease => 'Mai puțin';

  @override
  String storeAddToCart(String price) {
    return 'Adăugați în coș · $price';
  }

  @override
  String get storeAdd => 'Adăugați';

  @override
  String get storeRegistering => 'Se creează comanda…';

  @override
  String get storeErrorCredentials => 'Loginul și parola comerciantului nu sunt setate.';

  @override
  String get storeErrorApiKey => 'Cheia API nu este setată.';

  @override
  String storeErrorFailed(String message) {
    return 'Comanda nu a fost creată: $message';
  }

  @override
  String get storeOpenSettings => 'Deschideți setările';

  @override
  String scenarioRow(String buyer) {
    return 'Scenariu: $buyer';
  }

  @override
  String get scenarioTitle => 'Scenariu de plată';

  @override
  String get scenarioBuyer => 'Cumpărător';

  @override
  String get scenarioGuest => 'Vizitator';

  @override
  String get scenarioWithCards => 'Cu carduri';

  @override
  String get scenarioWithCardsHint => 'Cardurile apar după o plată cu „Salvați cardul”';

  @override
  String get scenarioReset => 'Resetați';

  @override
  String get scenarioPayment => 'Plată';

  @override
  String get scenarioWallet => 'Google Pay';

  @override
  String get scenarioScanner => 'Scaner de card';

  @override
  String get scenarioSdkResult => 'Ecranul de rezultat SDK';

  @override
  String resultPaidAmount(String amount) {
    return 'Plătit: $amount';
  }

  @override
  String get resultPaymentId => 'Plată';

  @override
  String get resultAmount => 'Sumă';

  @override
  String get resultDeclinedTitle => 'Plată refuzată';

  @override
  String get resultDeclinedBody => 'Banca nu a confirmat plata.';

  @override
  String get resultUnconfirmedTitle => 'Verificăm plata';

  @override
  String get resultUnconfirmedBody =>
      'Banca nu a răspuns încă. Nu plătiți din nou — vă anunțăm imediat ce plata este confirmată.';

  @override
  String get resultTryAgain => 'Încercați din nou';

  @override
  String get resultBackToStore => 'Înapoi la magazin';

  @override
  String get resultDone => 'Gata';

  @override
  String get devTitle => 'Setări pentru dezvoltatori';

  @override
  String get devGateway => 'Gateway';

  @override
  String get devBaseUrl => 'URL de bază';

  @override
  String get devLogin => 'Login comerciant';

  @override
  String get devPassword => 'Parolă';

  @override
  String get devApiV1 => 'API V1';

  @override
  String get devApiV2 => 'API V2';

  @override
  String get devApiVersion => 'API comerciant';

  @override
  String get devApiV1Hint => 'Comenzile se înregistrează prin register.do cu loginul și parola comerciantului.';

  @override
  String get devApiV2Hint => 'Sesiunile se creează prin POST /api2/sessions cu cheia API.';

  @override
  String get devApiKey => 'Cheie API';

  @override
  String get devOrderType => 'Comandă';

  @override
  String get devOrderPurchase => 'Plată';

  @override
  String get devOrderAft => 'Transfer AFT';

  @override
  String get devOrderAftHint =>
      'Transferurile se înregistrează prin registerP2P.do (AFT într-un singur sens): fereastra cere datele expeditorului și ale destinatarului.';

  @override
  String get devCredentialsNote =>
      'Se păstrează doar pe acest dispozitiv. În aplicația dvs., comanda este înregistrată de backend.';

  @override
  String get devSdkLook => 'Aspectul SDK';

  @override
  String get devSheetTheme => 'Tema ferestrei de plată';

  @override
  String get devThemeCover => 'Ca magazinul';

  @override
  String get devThemeLight => 'Luminoasă';

  @override
  String get devThemeDark => 'Întunecată';

  @override
  String get devThemeSystem => 'Sistem';

  @override
  String get devLanguage => 'Limbă';

  @override
  String get devLanguageSystem => 'Sistem';

  @override
  String get devWallets => 'Google Pay';

  @override
  String get devGpayGateway => 'Gateway';

  @override
  String get devGpayMerchant => 'Merchant ID';

  @override
  String get devGpayFromGateway => 'De la gateway-ul de plată';

  @override
  String get devMore => 'Mai multe';

  @override
  String get devAdvanced => 'Avansat';

  @override
  String get devAdvancedSub => 'Flux de stări, SSL pinning';

  @override
  String get devLastOrder => 'Ultima comandă';

  @override
  String get devCopy => 'Copiați';

  @override
  String get devNone => '—';

  @override
  String get resultSuccessFurniture => 'Vă mulțumim pentru comandă';

  @override
  String get resultSuccessFurnitureBody => 'Vă vom contacta pentru a stabili livrarea.';

  @override
  String get resultSuccessCoffee => 'Comanda dvs. se pregătește';

  @override
  String get resultSuccessCoffeeBody => 'Arătați acest număr la bar.';

  @override
  String get resultSuccessTickets => 'Ne vedem la concert!';

  @override
  String get resultSuccessTicketsBody => 'Biletele sunt deja în contul dvs.';

  @override
  String get furnitureCollection => 'Colecția\nde toamnă';

  @override
  String get furnitureAll => 'Toate';

  @override
  String get furnitureChairs => 'Scaune';

  @override
  String get furnitureSofas => 'Canapele';

  @override
  String get furnitureLight => 'Iluminat';

  @override
  String get furnitureDelivery => 'Livrare gratuită în 3–5 zile. 30 de zile pentru retur.';

  @override
  String get furnitureDeliveryLabel => 'Livrare';

  @override
  String get furnitureDeliveryFree => 'Gratuit';

  @override
  String get furnitureColours => 'Culoare';

  @override
  String get coffeeLocation => 'King St.';

  @override
  String get coffeeAgain => 'Ca data trecută';

  @override
  String get coffeeRepeat => 'Comandați din nou';

  @override
  String get coffeeTabCoffee => 'Cafea';

  @override
  String get coffeeTabCold => 'Reci';

  @override
  String get coffeeTabBakery => 'Patiserie';

  @override
  String get coffeeOrderTitle => 'Comanda dvs.';

  @override
  String get coffeePickup => 'Ridicare';

  @override
  String get coffeePickupValue => 'King St. · în 10 min';

  @override
  String coffeeAddItem(String item) {
    return 'Adăugați $item';
  }

  @override
  String get coffeeOrderNumberHint => 'Gata în aproximativ 6 minute.';

  @override
  String get ticketsLive => 'Live in Hall A';

  @override
  String get ticketsTitle => 'NORTHERN\nLIGHTS';

  @override
  String get ticketsDate => 'Sâm., 14 nov. · 20:00 · Hall A';

  @override
  String get ticketsSection => 'Bilete';

  @override
  String ticketsToPayment(String amount) {
    return 'Spre plată · $amount';
  }

  @override
  String get ticketsCheckout => 'Finalizarea comenzii';

  @override
  String ticketsEventLine(String category) {
    return '14 nov. · $category';
  }

  @override
  String ticketsHold(String time) {
    return 'Locurile sunt rezervate încă $time';
  }

  @override
  String get ticketsHoldExpired => 'Rezervarea a expirat. Alegeți din nou locurile.';

  @override
  String get ticketsSeats => 'Bilete';

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
  String get productVelvetSofaSub => 'Catifea verde smarald, picioare din stejar';

  @override
  String get productCloudChairSub => 'Bouclé alb-lăptos';

  @override
  String get productOnoChairSub => 'Carcasă turnată, picioare din fag';

  @override
  String get productBirchStoolSub => 'Lemn masiv de mesteacăn';

  @override
  String get productArcLampSub => 'Oțel mat, culoare grafit';

  @override
  String get productLumeArmchairSub => 'Lână culoarea muștarului';

  @override
  String get productFlatWhiteSub => 'Espresso dublu, lapte spumat';

  @override
  String get productCappuccinoSub => 'Clasic, cu spumă';

  @override
  String get productEspressoSub => 'Origine unică';

  @override
  String get productIcedLatteSub => 'Lapte rece, gheață';

  @override
  String get productCroissantSub => 'Copt azi-dimineață';

  @override
  String get ticketFloor => 'Zona în picioare';

  @override
  String get ticketFloorSub => 'Acces general';

  @override
  String get ticketBalcony => 'Balcon';

  @override
  String get ticketBalconySub => 'Rândul 2 · locuri pe scaun';

  @override
  String get ticketFee => 'Taxă de serviciu';

  @override
  String get startSectionPayment => 'SDK Payment';

  @override
  String get startSectionPaymentSub =>
      'O fereastră de plată gata de folosit în aplicația dvs. — fiecare magazin include toate scenariile';

  @override
  String get startSectionCore => 'SDK Core';

  @override
  String get startSectionCoreSub => 'Criptarea cardului pentru propriul formular (PCI DSS)';

  @override
  String get coreTileTitle => 'Propriul formular de card';

  @override
  String get coreTileSub => 'Datele cardului devin un token pentru serverul dvs.';

  @override
  String get coreTitle => 'SDK Core';

  @override
  String get coreDoneSubtitle => 'Tokenul este gata';

  @override
  String get coreVariantNew => 'Card nou';

  @override
  String get coreVariantInstant => 'Fără comandă';

  @override
  String get coreVariantStored => 'Card salvat';

  @override
  String get coreHintNew => 'Plata unei comenzi înregistrate de dvs.: datele cardului și mdOrder-ul comenzii.';

  @override
  String get coreHintInstant =>
      'Încă nu există o comandă: serverul dvs. o creează și o plătește printr-un singur apel.';

  @override
  String get coreHintStored => 'Plata unei comenzi cu un card salvat: bindingId-ul cardului în locul datelor acestuia.';

  @override
  String get coreCardGroup => 'Datele cardului';

  @override
  String get corePan => 'Numărul cardului';

  @override
  String get coreExpiryPlaceholder => 'LL/AA';

  @override
  String get coreExpiry => 'Data expirării';

  @override
  String get coreCvc => 'CVC/CVV';

  @override
  String get coreCvcOptional => 'CVC/CVV (opțional)';

  @override
  String get coreHolder => 'Numele de pe card (opțional)';

  @override
  String get coreBinding => 'bindingId';

  @override
  String get coreMdOrder => 'mdOrder';

  @override
  String get coreMdOrderHint => 'De la backendul dvs.';

  @override
  String get corePubKey => 'Cheie publică (PEM)';

  @override
  String get corePaste => 'Lipiți';

  @override
  String get coreGenerate => 'Obțineți tokenul';

  @override
  String get coreErrorRequired => 'Câmp obligatoriu';

  @override
  String get coreErrorInvalid => 'Valoare nevalidă';

  @override
  String get coreErrorFormat => 'Format greșit';

  @override
  String get coreTokenOk => 'Token primit';

  @override
  String get coreCopy => 'Copiați';

  @override
  String get coreCopied => 'Copiat';

  @override
  String get coreStepSend => 'Trimiteți tokenul la serverul dvs.';

  @override
  String get coreStepGateway => 'Serverul dvs. îl transmite gateway-ului ca seToken';

  @override
  String get coreStepPay => 'Gateway-ul procesează plata';

  @override
  String get coreAgain => 'Din nou';

  @override
  String get investName => 'Northwind Invest';

  @override
  String get startSectionPaymentAftSub => 'AFT: alimentarea unui cont cu cardul';

  @override
  String get investTileSub => 'Alimentați contul de investiții cu cardul și urmăriți cum crește soldul';

  @override
  String investAccount(String account) {
    return 'Cont de investiții •• $account';
  }

  @override
  String get investPortfolioValue => 'Valoarea portofoliului';

  @override
  String get investChangeMonth => 'luna aceasta';

  @override
  String get investPeriod1w => '1S';

  @override
  String get investPeriod1m => '1L';

  @override
  String get investPeriod3m => '3L';

  @override
  String get investPeriod1y => '1A';

  @override
  String get investPeriodAll => 'Tot';

  @override
  String get investTopUp => 'Alimentează';

  @override
  String get investInvest => 'Investește';

  @override
  String get investFreeCash => 'Numerar disponibil';

  @override
  String get investFreeCashSub => 'Gata de investit';

  @override
  String investFreeCashJustNow(String amount) {
    return '+$amount chiar acum';
  }

  @override
  String get investMyAssets => 'Activele mele';

  @override
  String investUnits(String units, String price) {
    return '$units unități · $price';
  }

  @override
  String get investRecent => 'Operațiuni recente';

  @override
  String get investOpTopUp => 'Alimentare';

  @override
  String investOpBuy(String asset) {
    return 'Cumpărare $asset';
  }

  @override
  String get investAssetGlb => 'Global ETF';

  @override
  String get investAssetTch => 'Tech Leaders';

  @override
  String get investNotifications => 'Notificări';

  @override
  String get topupTitle => 'Alimentare';

  @override
  String get topupTo => 'În';

  @override
  String get topupAvailable => 'Disponibil';

  @override
  String get topupAmount => 'Sumă';

  @override
  String topupLimits(String min, String max) {
    return 'de la $min la $max';
  }

  @override
  String topupContinue(String amount) {
    return 'Continuă · $amount';
  }

  @override
  String get topupCreating => 'Se creează transferul…';

  @override
  String get topupFailedTitle => 'Transferul nu a reușit';

  @override
  String get topupFailedBody => 'Banca cardului a refuzat operațiunea. Nu s-a debitat nimic; încercați alt card.';

  @override
  String topupFailedDetail(String message) {
    return 'Gateway: $message';
  }

  @override
  String get topupUnknownTitle => 'Stare necunoscută';

  @override
  String get topupUnknownBody => 'Verificați comanda în portalul comerciantului înainte de a încerca din nou.';

  @override
  String get topupTryAgain => 'Încearcă din nou';

  @override
  String doneCredited(String account) {
    return 'creditat în contul de investiții •• $account';
  }

  @override
  String get doneOperation => 'Operațiune';

  @override
  String get doneOperationValue => 'AFT · în contul propriu';

  @override
  String get doneFreeCash => 'Numerar disponibil';

  @override
  String get doneOrder => 'Comandă';

  @override
  String get doneTime => 'Ora';

  @override
  String doneInvest(String amount) {
    return 'Investește $amount';
  }

  @override
  String get doneToPortfolio => 'La portofoliu';

  @override
  String get devInvestReset => 'Resetează contul demo Northwind';

  @override
  String get devInvestResetDone => 'Cont demo resetat';

  @override
  String get scenarioWalletApple => 'Apple Pay';

  @override
  String get devApplePay => 'Apple Pay';
}
