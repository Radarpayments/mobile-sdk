// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Modern Greek (`el`).
class AppLocalizationsEl extends AppLocalizations {
  AppLocalizationsEl([String locale = 'el']) : super(locale);

  @override
  String get startTitle => 'Επίδειξη πληρωμής';

  @override
  String get startSettings => 'Ρυθμίσεις προγραμματιστή';

  @override
  String get coverFurnitureName => 'Hearth & Co';

  @override
  String get coverCoffeeName => 'Roast Lane';

  @override
  String get coverTicketsName => 'Stage Pass';

  @override
  String get coverFurnitureTagline => 'Έπιπλα · κατάλογος και καλάθι';

  @override
  String get coverCoffeeTagline => 'Καφετέρια · παραγγελία από πριν';

  @override
  String get coverTicketsTagline => 'Εισιτήρια · συναυλίες και παραστάσεις';

  @override
  String get storeBack => 'Πίσω';

  @override
  String get storeCart => 'Καλάθι';

  @override
  String storeCartCount(int count) {
    return 'Καλάθι · $count';
  }

  @override
  String get storeItems => 'Προϊόντα';

  @override
  String get storeTotal => 'Σύνολο';

  @override
  String storePay(String amount) {
    return 'Πληρωμή $amount';
  }

  @override
  String get storeIncrease => 'Περισσότερα';

  @override
  String get storeDecrease => 'Λιγότερα';

  @override
  String storeAddToCart(String price) {
    return 'Στο καλάθι · $price';
  }

  @override
  String get storeAdd => 'Προσθήκη';

  @override
  String get storeRegistering => 'Δημιουργία παραγγελίας…';

  @override
  String get storeErrorCredentials => 'Δεν έχουν οριστεί το όνομα χρήστη και ο κωδικός πρόσβασης του εμπόρου.';

  @override
  String get storeErrorApiKey => 'Δεν έχει οριστεί το κλειδί API.';

  @override
  String storeErrorFailed(String message) {
    return 'Η παραγγελία δεν δημιουργήθηκε: $message';
  }

  @override
  String get storeOpenSettings => 'Άνοιγμα ρυθμίσεων';

  @override
  String scenarioRow(String buyer) {
    return 'Σενάριο: $buyer';
  }

  @override
  String get scenarioTitle => 'Σενάριο πληρωμής';

  @override
  String get scenarioBuyer => 'Αγοραστής';

  @override
  String get scenarioGuest => 'Επισκέπτης';

  @override
  String get scenarioWithCards => 'Με κάρτες';

  @override
  String get scenarioWithCardsHint => 'Οι κάρτες εμφανίζονται μετά από πληρωμή με «Αποθήκευση κάρτας»';

  @override
  String get scenarioReset => 'Επαναφορά';

  @override
  String get scenarioPayment => 'Πληρωμή';

  @override
  String get scenarioWallet => 'Google Pay';

  @override
  String get scenarioScanner => 'Σαρωτής κάρτας';

  @override
  String get scenarioSdkResult => 'Οθόνη αποτελέσματος SDK';

  @override
  String resultPaidAmount(String amount) {
    return 'Πληρώθηκε $amount';
  }

  @override
  String get resultPaymentId => 'Πληρωμή';

  @override
  String get resultAmount => 'Ποσό';

  @override
  String get resultDeclinedTitle => 'Η πληρωμή απορρίφθηκε';

  @override
  String get resultDeclinedBody => 'Η τράπεζα δεν επιβεβαίωσε την πληρωμή.';

  @override
  String get resultUnconfirmedTitle => 'Ελέγχουμε την πληρωμή';

  @override
  String get resultUnconfirmedBody =>
      'Η τράπεζα δεν έχει απαντήσει ακόμη. Μην πληρώσετε ξανά — θα σας ενημερώσουμε μόλις επιβεβαιωθεί η πληρωμή.';

  @override
  String get resultTryAgain => 'Δοκιμάστε ξανά';

  @override
  String get resultBackToStore => 'Επιστροφή στο κατάστημα';

  @override
  String get resultDone => 'Τέλος';

  @override
  String get devTitle => 'Ρυθμίσεις προγραμματιστή';

  @override
  String get devGateway => 'Πύλη πληρωμών';

  @override
  String get devBaseUrl => 'Base URL';

  @override
  String get devLogin => 'Όνομα χρήστη εμπόρου';

  @override
  String get devPassword => 'Κωδικός πρόσβασης';

  @override
  String get devApiV1 => 'API V1';

  @override
  String get devApiV2 => 'API V2';

  @override
  String get devApiVersion => 'API εμπόρου';

  @override
  String get devApiV1Hint =>
      'Οι παραγγελίες καταχωρίζονται μέσω register.do με το όνομα χρήστη και τον κωδικό του εμπόρου.';

  @override
  String get devApiV2Hint => 'Οι συνεδρίες δημιουργούνται μέσω POST /api2/sessions με το κλειδί API.';

  @override
  String get devApiKey => 'Κλειδί API';

  @override
  String get devOrderType => 'Παραγγελία';

  @override
  String get devOrderPurchase => 'Πληρωμή';

  @override
  String get devOrderAft => 'Μεταφορά AFT';

  @override
  String get devOrderAftHint =>
      'Οι μεταφορές καταχωρίζονται μέσω registerP2P.do (AFT μίας κατεύθυνσης): το παράθυρο ζητά τα στοιχεία αποστολέα και παραλήπτη.';

  @override
  String get devCredentialsNote =>
      'Αποθηκεύονται μόνο σε αυτήν τη συσκευή. Στη δική σας εφαρμογή, την παραγγελία την καταχωρίζει το backend σας.';

  @override
  String get devSdkLook => 'Εμφάνιση SDK';

  @override
  String get devSheetTheme => 'Θέμα παραθύρου πληρωμής';

  @override
  String get devThemeCover => 'Όπως το κατάστημα';

  @override
  String get devThemeLight => 'Ανοιχτό';

  @override
  String get devThemeDark => 'Σκούρο';

  @override
  String get devThemeSystem => 'Συστήματος';

  @override
  String get devLanguage => 'Γλώσσα';

  @override
  String get devLanguageSystem => 'Συστήματος';

  @override
  String get devWallets => 'Google Pay';

  @override
  String get devGpayGateway => 'Gateway';

  @override
  String get devGpayMerchant => 'Merchant ID';

  @override
  String get devGpayFromGateway => 'Από την πύλη πληρωμών';

  @override
  String get devMore => 'Περισσότερα';

  @override
  String get devAdvanced => 'Advanced';

  @override
  String get devAdvancedSub => 'Ροή καταστάσεων, SSL pinning';

  @override
  String get devLastOrder => 'Τελευταία παραγγελία';

  @override
  String get devCopy => 'Αντιγραφή';

  @override
  String get devNone => '—';

  @override
  String get resultSuccessFurniture => 'Ευχαριστούμε για την παραγγελία σας';

  @override
  String get resultSuccessFurnitureBody => 'Θα επικοινωνήσουμε μαζί σας για να κανονίσουμε την παράδοση.';

  @override
  String get resultSuccessCoffee => 'Η παραγγελία σας ετοιμάζεται';

  @override
  String get resultSuccessCoffeeBody => 'Δείξτε αυτόν τον αριθμό στον μπαρίστα.';

  @override
  String get resultSuccessTickets => 'Θα είστε εκεί!';

  @override
  String get resultSuccessTicketsBody => 'Τα εισιτήρια βρίσκονται στον λογαριασμό σας.';

  @override
  String get furnitureCollection => 'Φθινοπωρινή\nσυλλογή';

  @override
  String get furnitureAll => 'Όλα';

  @override
  String get furnitureChairs => 'Καρέκλες';

  @override
  String get furnitureSofas => 'Καναπέδες';

  @override
  String get furnitureLight => 'Φωτισμός';

  @override
  String get furnitureDelivery => 'Δωρεάν παράδοση σε 3–5 ημέρες. 30 ημέρες για επιστροφή.';

  @override
  String get furnitureDeliveryLabel => 'Παράδοση';

  @override
  String get furnitureDeliveryFree => 'Δωρεάν';

  @override
  String get furnitureColours => 'Χρώμα';

  @override
  String get coffeeLocation => 'King St.';

  @override
  String get coffeeAgain => 'Όπως την τελευταία φορά';

  @override
  String get coffeeRepeat => 'Επανάληψη παραγγελίας';

  @override
  String get coffeeTabCoffee => 'Καφές';

  @override
  String get coffeeTabCold => 'Κρύα';

  @override
  String get coffeeTabBakery => 'Αρτοσκευάσματα';

  @override
  String get coffeeOrderTitle => 'Η παραγγελία σας';

  @override
  String get coffeePickup => 'Παραλαβή';

  @override
  String get coffeePickupValue => 'King St. · σε 10 λεπτά';

  @override
  String coffeeAddItem(String item) {
    return 'Προσθήκη $item';
  }

  @override
  String get coffeeOrderNumberHint => 'Έτοιμη σε περίπου 6 λεπτά.';

  @override
  String get ticketsLive => 'Live in Hall A';

  @override
  String get ticketsTitle => 'NORTHERN\nLIGHTS';

  @override
  String get ticketsDate => 'Σάβ, 14 Νοε · 20:00 · Hall A';

  @override
  String get ticketsSection => 'Εισιτήρια';

  @override
  String ticketsToPayment(String amount) {
    return 'Ταμείο · $amount';
  }

  @override
  String get ticketsCheckout => 'Ταμείο';

  @override
  String ticketsEventLine(String category) {
    return '14 Νοε · $category';
  }

  @override
  String ticketsHold(String time) {
    return 'Οι θέσεις κρατούνται για $time';
  }

  @override
  String get ticketsHoldExpired => 'Η κράτηση έληξε. Επιλέξτε ξανά θέσεις.';

  @override
  String get ticketsSeats => 'Εισιτήρια';

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
  String get productVelvetSofaSub => 'Σμαραγδί βελούδο, δρύινα πόδια';

  @override
  String get productCloudChairSub => 'Γαλακτερό λευκό μπουκλέ';

  @override
  String get productOnoChairSub => 'Χυτό κέλυφος, πόδια από οξιά';

  @override
  String get productBirchStoolSub => 'Μασίφ σημύδα';

  @override
  String get productArcLampSub => 'Ματ ατσάλι σε χρώμα γραφίτη';

  @override
  String get productLumeArmchairSub => 'Μάλλινο ύφασμα σε μουσταρδί';

  @override
  String get productFlatWhiteSub => 'Διπλός espresso, ζεστό γάλα';

  @override
  String get productCappuccinoSub => 'Κλασικός, με αφρόγαλα';

  @override
  String get productEspressoSub => 'Μίας προέλευσης';

  @override
  String get productIcedLatteSub => 'Κρύο γάλα, πάγος';

  @override
  String get productCroissantSub => 'Ψημένο σήμερα το πρωί';

  @override
  String get ticketFloor => 'Αρένα';

  @override
  String get ticketFloorSub => 'Γενική είσοδος';

  @override
  String get ticketBalcony => 'Εξώστης';

  @override
  String get ticketBalconySub => 'Σειρά 2 · καθήμενοι';

  @override
  String get ticketFee => 'Χρέωση υπηρεσίας';

  @override
  String get startSectionPayment => 'SDK Payment';

  @override
  String get startSectionPaymentSub =>
      'Έτοιμο παράθυρο πληρωμής στην εφαρμογή σας — κάθε κατάστημα περιλαμβάνει όλα τα σενάρια';

  @override
  String get startSectionCore => 'SDK Core';

  @override
  String get startSectionCoreSub => 'Κρυπτογράφηση κάρτας για τη δική σας φόρμα (PCI DSS)';

  @override
  String get coreTileTitle => 'Η δική σας φόρμα κάρτας';

  @override
  String get coreTileSub => 'Η κάρτα γίνεται token για τον διακομιστή σας';

  @override
  String get coreTitle => 'SDK Core';

  @override
  String get coreDoneSubtitle => 'Το token είναι έτοιμο';

  @override
  String get coreVariantNew => 'Νέα κάρτα';

  @override
  String get coreVariantInstant => 'Χωρίς παραγγελία';

  @override
  String get coreVariantStored => 'Αποθηκευμένη κάρτα';

  @override
  String get coreHintNew => 'Πληρώνει μια παραγγελία που καταχωρίσατε: στοιχεία κάρτας και το mdOrder της.';

  @override
  String get coreHintInstant =>
      'Δεν υπάρχει ακόμη παραγγελία: ο διακομιστής σας τη δημιουργεί και την πληρώνει με μία κλήση.';

  @override
  String get coreHintStored =>
      'Πληρώνει μια παραγγελία με αποθηκευμένη κάρτα: το bindingId της αντί για τα στοιχεία κάρτας.';

  @override
  String get coreCardGroup => 'Στοιχεία κάρτας';

  @override
  String get corePan => 'Αριθμός κάρτας';

  @override
  String get coreExpiryPlaceholder => 'ΜΜ/ΕΕ';

  @override
  String get coreExpiry => 'Λήξη';

  @override
  String get coreCvc => 'CVC/CVV';

  @override
  String get coreCvcOptional => 'CVC/CVV (προαιρετικό)';

  @override
  String get coreHolder => 'Όνομα στην κάρτα (προαιρετικό)';

  @override
  String get coreBinding => 'bindingId';

  @override
  String get coreMdOrder => 'mdOrder';

  @override
  String get coreMdOrderHint => 'Από το backend σας';

  @override
  String get corePubKey => 'Δημόσιο κλειδί (PEM)';

  @override
  String get corePaste => 'Επικόλληση';

  @override
  String get coreGenerate => 'Λήψη token';

  @override
  String get coreErrorRequired => 'Υποχρεωτικό πεδίο';

  @override
  String get coreErrorInvalid => 'Μη έγκυρη τιμή';

  @override
  String get coreErrorFormat => 'Λάθος μορφή';

  @override
  String get coreTokenOk => 'Το token ελήφθη';

  @override
  String get coreCopy => 'Αντιγραφή';

  @override
  String get coreCopied => 'Αντιγράφηκε';

  @override
  String get coreStepSend => 'Στείλτε το token στον διακομιστή σας';

  @override
  String get coreStepGateway => 'Ο διακομιστής σας το διαβιβάζει στην πύλη πληρωμών ως seToken';

  @override
  String get coreStepPay => 'Η πύλη πληρωμών επεξεργάζεται την πληρωμή';

  @override
  String get coreAgain => 'Ξανά';

  @override
  String get investName => 'Northwind Invest';

  @override
  String get startSectionPaymentAftSub => 'AFT: φόρτωση λογαριασμού από κάρτα';

  @override
  String get investTileSub => 'Φορτώστε τον επενδυτικό σας λογαριασμό από κάρτα και δείτε το υπόλοιπο να μεγαλώνει';

  @override
  String investAccount(String account) {
    return 'Επενδυτικός λογαριασμός •• $account';
  }

  @override
  String get investPortfolioValue => 'Αξία χαρτοφυλακίου';

  @override
  String get investChangeMonth => 'αυτόν τον μήνα';

  @override
  String get investPeriod1w => '1Ε';

  @override
  String get investPeriod1m => '1Μ';

  @override
  String get investPeriod3m => '3Μ';

  @override
  String get investPeriod1y => '1Χ';

  @override
  String get investPeriodAll => 'Όλα';

  @override
  String get investTopUp => 'Φόρτωση';

  @override
  String get investInvest => 'Επένδυση';

  @override
  String get investFreeCash => 'Διαθέσιμα μετρητά';

  @override
  String get investFreeCashSub => 'Έτοιμα για επένδυση';

  @override
  String investFreeCashJustNow(String amount) {
    return '+$amount μόλις τώρα';
  }

  @override
  String get investMyAssets => 'Τα στοιχεία μου';

  @override
  String investUnits(String units, String price) {
    return '$units μερίδια · $price';
  }

  @override
  String get investRecent => 'Πρόσφατες κινήσεις';

  @override
  String get investOpTopUp => 'Φόρτωση';

  @override
  String investOpBuy(String asset) {
    return 'Αγορά $asset';
  }

  @override
  String get investAssetGlb => 'Global ETF';

  @override
  String get investAssetTch => 'Tech Leaders';

  @override
  String get investNotifications => 'Ειδοποιήσεις';

  @override
  String get topupTitle => 'Φόρτωση';

  @override
  String get topupTo => 'Προς';

  @override
  String get topupAvailable => 'Διαθέσιμα';

  @override
  String get topupAmount => 'Ποσό';

  @override
  String topupLimits(String min, String max) {
    return 'από $min έως $max';
  }

  @override
  String topupContinue(String amount) {
    return 'Συνέχεια · $amount';
  }

  @override
  String get topupCreating => 'Δημιουργία μεταφοράς…';

  @override
  String get topupFailedTitle => 'Η μεταφορά απέτυχε';

  @override
  String get topupFailedBody =>
      'Η τράπεζα της κάρτας απέρριψε τη συναλλαγή. Δεν χρεώθηκε τίποτα· δοκιμάστε άλλη κάρτα.';

  @override
  String topupFailedDetail(String message) {
    return 'Πύλη: $message';
  }

  @override
  String get topupUnknownTitle => 'Άγνωστη κατάσταση';

  @override
  String get topupUnknownBody => 'Ελέγξτε την παραγγελία στην πύλη εμπόρου πριν δοκιμάσετε ξανά.';

  @override
  String get topupTryAgain => 'Δοκιμάστε ξανά';

  @override
  String get topupAddCard => 'Προσθήκη κάρτας';

  @override
  String get topupAddingCard => 'Προσθήκη κάρτας…';

  @override
  String get topupCardAddedTitle => 'Η κάρτα προστέθηκε';

  @override
  String get topupCardAddedBody => 'Επιλέξτε την στο παράθυρο πληρωμής στην επόμενη φόρτωση. Δεν χρεώθηκε κανένα ποσό.';

  @override
  String get topupCardFailedTitle => 'Η κάρτα δεν προστέθηκε';

  @override
  String get topupCardFailedBody =>
      'Η τράπεζα της κάρτας δεν την επιβεβαίωσε. Δεν χρεώθηκε κανένα ποσό· δοκιμάστε άλλη κάρτα.';

  @override
  String get topupCardUnknownBody =>
      'Η τράπεζα δεν έχει επιβεβαιώσει ακόμη την κάρτα. Αν την επιβεβαιώσει, θα εμφανιστεί στην επόμενη φόρτωση.';

  @override
  String doneCredited(String account) {
    return 'πιστώθηκαν στον επενδυτικό λογαριασμό •• $account';
  }

  @override
  String get doneOperation => 'Συναλλαγή';

  @override
  String get doneOperationValue => 'AFT · σε δικό σας λογαριασμό';

  @override
  String get doneFreeCash => 'Διαθέσιμα μετρητά';

  @override
  String get doneOrder => 'Παραγγελία';

  @override
  String get doneTime => 'Ώρα';

  @override
  String doneInvest(String amount) {
    return 'Επένδυση $amount';
  }

  @override
  String get doneToPortfolio => 'Στο χαρτοφυλάκιο';

  @override
  String get devInvestReset => 'Επαναφορά δοκιμαστικού λογαριασμού Northwind';

  @override
  String get devInvestResetDone => 'Ο δοκιμαστικός λογαριασμός επαναφέρθηκε';

  @override
  String get scenarioWalletApple => 'Apple Pay';

  @override
  String get devApplePay => 'Apple Pay';
}
