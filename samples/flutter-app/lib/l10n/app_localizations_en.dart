// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get startTitle => 'Payment demo';

  @override
  String get startSettings => 'Developer settings';

  @override
  String get coverFurnitureName => 'Hearth & Co';

  @override
  String get coverCoffeeName => 'Roast Lane';

  @override
  String get coverTicketsName => 'Stage Pass';

  @override
  String get coverFurnitureTagline => 'Furniture · catalogue and cart';

  @override
  String get coverCoffeeTagline => 'Coffee shop · order ahead';

  @override
  String get coverTicketsTagline => 'Tickets · concerts and shows';

  @override
  String get storeBack => 'Back';

  @override
  String get storeCart => 'Cart';

  @override
  String storeCartCount(int count) {
    return 'Cart · $count';
  }

  @override
  String get storeItems => 'Items';

  @override
  String get storeTotal => 'Total';

  @override
  String storePay(String amount) {
    return 'Pay $amount';
  }

  @override
  String get storeIncrease => 'More';

  @override
  String get storeDecrease => 'Less';

  @override
  String storeAddToCart(String price) {
    return 'Add to cart · $price';
  }

  @override
  String get storeAdd => 'Add';

  @override
  String get storeRegistering => 'Creating the order…';

  @override
  String get storeErrorCredentials => 'Merchant login and password are not set.';

  @override
  String get storeErrorApiKey => 'The API key is not set.';

  @override
  String storeErrorFailed(String message) {
    return 'The order was not created: $message';
  }

  @override
  String get storeOpenSettings => 'Open settings';

  @override
  String scenarioRow(String buyer) {
    return 'Scenario: $buyer';
  }

  @override
  String get scenarioTitle => 'Payment scenario';

  @override
  String get scenarioBuyer => 'Buyer';

  @override
  String get scenarioGuest => 'Guest';

  @override
  String get scenarioWithCards => 'With cards';

  @override
  String get scenarioWithCardsHint => 'Cards appear after a payment with “Save card”';

  @override
  String get scenarioReset => 'Reset';

  @override
  String get scenarioPayment => 'Payment';

  @override
  String get scenarioWallet => 'Google Pay';

  @override
  String get scenarioScanner => 'Card scanner';

  @override
  String get scenarioSdkResult => 'SDK result screen';

  @override
  String resultPaidAmount(String amount) {
    return 'Paid $amount';
  }

  @override
  String get resultPaymentId => 'Payment';

  @override
  String get resultAmount => 'Amount';

  @override
  String get resultDeclinedTitle => 'Payment declined';

  @override
  String get resultDeclinedBody => 'The bank did not confirm the payment.';

  @override
  String get resultUnconfirmedTitle => 'Checking your payment';

  @override
  String get resultUnconfirmedBody =>
      'The bank has not answered yet. Please don’t pay again — we’ll let you know as soon as the payment is confirmed.';

  @override
  String get resultTryAgain => 'Try again';

  @override
  String get resultBackToStore => 'Back to the store';

  @override
  String get resultDone => 'Done';

  @override
  String get devTitle => 'Developer settings';

  @override
  String get devGateway => 'Gateway';

  @override
  String get devBaseUrl => 'Base URL';

  @override
  String get devLogin => 'Merchant login';

  @override
  String get devPassword => 'Password';

  @override
  String get devApiV1 => 'API V1';

  @override
  String get devApiV2 => 'API V2';

  @override
  String get devApiVersion => 'Merchant API';

  @override
  String get devApiV1Hint => 'Orders are registered with register.do using the merchant login and password.';

  @override
  String get devApiV2Hint => 'Sessions are created with POST /api2/sessions using the API key.';

  @override
  String get devApiKey => 'API key';

  @override
  String get devOrderType => 'Order';

  @override
  String get devOrderPurchase => 'Payment';

  @override
  String get devOrderAft => 'AFT transfer';

  @override
  String get devOrderAftHint =>
      'Transfers are registered with registerP2P.do (one-leg AFT): the sheet asks for the sender and recipient details.';

  @override
  String get devCredentialsNote => 'Stored only on this device. In your app the order is registered by your backend.';

  @override
  String get devSdkLook => 'SDK appearance';

  @override
  String get devSheetTheme => 'Sheet theme';

  @override
  String get devThemeCover => 'As the store';

  @override
  String get devThemeLight => 'Light';

  @override
  String get devThemeDark => 'Dark';

  @override
  String get devThemeSystem => 'System';

  @override
  String get devLanguage => 'Language';

  @override
  String get devLanguageSystem => 'System';

  @override
  String get devWallets => 'Google Pay';

  @override
  String get devGpayGateway => 'Gateway';

  @override
  String get devGpayMerchant => 'Merchant ID';

  @override
  String get devGpayFromGateway => 'From the gateway';

  @override
  String get devMore => 'More';

  @override
  String get devAdvanced => 'Advanced';

  @override
  String get devAdvancedSub => 'State stream, SSL pinning';

  @override
  String get devLastOrder => 'Last order';

  @override
  String get devCopy => 'Copy';

  @override
  String get devNone => '—';

  @override
  String get resultSuccessFurniture => 'Thank you for your order';

  @override
  String get resultSuccessFurnitureBody => 'We will be in touch to arrange delivery.';

  @override
  String get resultSuccessCoffee => 'Your order is being prepared';

  @override
  String get resultSuccessCoffeeBody => 'Show this number to the barista.';

  @override
  String get resultSuccessTickets => 'You are going!';

  @override
  String get resultSuccessTicketsBody => 'Tickets are in your account.';

  @override
  String get furnitureCollection => 'Autumn\ncollection';

  @override
  String get furnitureAll => 'All';

  @override
  String get furnitureChairs => 'Chairs';

  @override
  String get furnitureSofas => 'Sofas';

  @override
  String get furnitureLight => 'Light';

  @override
  String get furnitureDelivery => 'Free delivery in 3–5 days. 30 days to return.';

  @override
  String get furnitureDeliveryLabel => 'Delivery';

  @override
  String get furnitureDeliveryFree => 'Free';

  @override
  String get furnitureColours => 'Colour';

  @override
  String get coffeeLocation => 'King St.';

  @override
  String get coffeeAgain => 'Same as last time';

  @override
  String get coffeeRepeat => 'Order again';

  @override
  String get coffeeTabCoffee => 'Coffee';

  @override
  String get coffeeTabCold => 'Cold';

  @override
  String get coffeeTabBakery => 'Bakery';

  @override
  String get coffeeOrderTitle => 'Your order';

  @override
  String get coffeePickup => 'Pick up';

  @override
  String get coffeePickupValue => 'King St. · in 10 min';

  @override
  String coffeeAddItem(String item) {
    return 'Add $item';
  }

  @override
  String get coffeeOrderNumberHint => 'Ready in about 6 minutes.';

  @override
  String get ticketsLive => 'Live in Hall A';

  @override
  String get ticketsTitle => 'NORTHERN\nLIGHTS';

  @override
  String get ticketsDate => 'Sat, 14 Nov · 20:00 · Hall A';

  @override
  String get ticketsSection => 'Tickets';

  @override
  String ticketsToPayment(String amount) {
    return 'Checkout · $amount';
  }

  @override
  String get ticketsCheckout => 'Checkout';

  @override
  String ticketsEventLine(String category) {
    return '14 Nov · $category';
  }

  @override
  String ticketsHold(String time) {
    return 'Seats held for $time';
  }

  @override
  String get ticketsHoldExpired => 'The hold ran out. Pick your seats again.';

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
  String get productVelvetSofaSub => 'Emerald velvet, oak legs';

  @override
  String get productCloudChairSub => 'Milk-white bouclé';

  @override
  String get productOnoChairSub => 'Moulded shell, beech legs';

  @override
  String get productBirchStoolSub => 'Solid birch';

  @override
  String get productArcLampSub => 'Matte graphite steel';

  @override
  String get productLumeArmchairSub => 'Mustard wool';

  @override
  String get productFlatWhiteSub => 'Double shot, steamed milk';

  @override
  String get productCappuccinoSub => 'Classic, with foam';

  @override
  String get productEspressoSub => 'Single origin';

  @override
  String get productIcedLatteSub => 'Cold milk, ice';

  @override
  String get productCroissantSub => 'Baked this morning';

  @override
  String get ticketFloor => 'Standing floor';

  @override
  String get ticketFloorSub => 'General admission';

  @override
  String get ticketBalcony => 'Balcony';

  @override
  String get ticketBalconySub => 'Row 2 · seated';

  @override
  String get ticketFee => 'Service fee';

  @override
  String get startSectionPayment => 'SDK Payment';

  @override
  String get startSectionPaymentSub => 'A ready payment sheet in your app — every store has all scenarios';

  @override
  String get startSectionCore => 'SDK Core';

  @override
  String get startSectionCoreSub => 'Card encryption for your own form (PCI DSS)';

  @override
  String get coreTileTitle => 'Your own card form';

  @override
  String get coreTileSub => 'Card data turns into a token for your server';

  @override
  String get coreTitle => 'SDK Core';

  @override
  String get coreDoneSubtitle => 'Token is ready';

  @override
  String get coreVariantNew => 'New card';

  @override
  String get coreVariantInstant => 'No order';

  @override
  String get coreVariantStored => 'Saved card';

  @override
  String get coreHintNew => 'Pays an order you registered: card data plus its mdOrder.';

  @override
  String get coreHintInstant => 'No order yet: your server creates it and pays in one call.';

  @override
  String get coreHintStored => 'Pays an order with a saved card: its bindingId instead of card data.';

  @override
  String get coreCardGroup => 'Card information';

  @override
  String get corePan => 'Card number';

  @override
  String get coreExpiryPlaceholder => 'MM/YY';

  @override
  String get coreExpiry => 'Expiry';

  @override
  String get coreCvc => 'CVC/CVV';

  @override
  String get coreCvcOptional => 'CVC/CVV (optional)';

  @override
  String get coreHolder => 'Name on card (optional)';

  @override
  String get coreBinding => 'bindingId';

  @override
  String get coreMdOrder => 'mdOrder';

  @override
  String get coreMdOrderHint => 'From your backend';

  @override
  String get corePubKey => 'Public key (PEM)';

  @override
  String get corePaste => 'Paste';

  @override
  String get coreGenerate => 'Get token';

  @override
  String get coreErrorRequired => 'Required';

  @override
  String get coreErrorInvalid => 'Invalid value';

  @override
  String get coreErrorFormat => 'Wrong format';

  @override
  String get coreTokenOk => 'Token received';

  @override
  String get coreCopy => 'Copy';

  @override
  String get coreCopied => 'Copied';

  @override
  String get coreStepSend => 'Send the token to your server';

  @override
  String get coreStepGateway => 'Your server passes it to the gateway as seToken';

  @override
  String get coreStepPay => 'The gateway processes the payment';

  @override
  String get coreAgain => 'Again';

  @override
  String get investName => 'Northwind Invest';

  @override
  String get startSectionPaymentAftSub => 'AFT: top up an account from a card';

  @override
  String get investTileSub => 'Top up your investment account from a card and watch the balance grow';

  @override
  String investAccount(String account) {
    return 'Investment account •• $account';
  }

  @override
  String get investPortfolioValue => 'Portfolio value';

  @override
  String get investChangeMonth => 'this month';

  @override
  String get investPeriod1w => '1W';

  @override
  String get investPeriod1m => '1M';

  @override
  String get investPeriod3m => '3M';

  @override
  String get investPeriod1y => '1Y';

  @override
  String get investPeriodAll => 'All';

  @override
  String get investTopUp => 'Top up';

  @override
  String get investInvest => 'Invest';

  @override
  String get investFreeCash => 'Free cash';

  @override
  String get investFreeCashSub => 'Ready to invest';

  @override
  String investFreeCashJustNow(String amount) {
    return '+$amount just now';
  }

  @override
  String get investMyAssets => 'My assets';

  @override
  String investUnits(String units, String price) {
    return '$units units · $price';
  }

  @override
  String get investRecent => 'Recent operations';

  @override
  String get investOpTopUp => 'Deposit';

  @override
  String investOpBuy(String asset) {
    return 'Buy $asset';
  }

  @override
  String get investAssetGlb => 'Global ETF';

  @override
  String get investAssetTch => 'Tech Leaders';

  @override
  String get investNotifications => 'Notifications';

  @override
  String get topupTitle => 'Top up';

  @override
  String get topupTo => 'To';

  @override
  String get topupAvailable => 'Available';

  @override
  String get topupAmount => 'Amount';

  @override
  String topupLimits(String min, String max) {
    return 'from $min to $max';
  }

  @override
  String topupContinue(String amount) {
    return 'Continue · $amount';
  }

  @override
  String get topupCreating => 'Creating transfer…';

  @override
  String get topupFailedTitle => 'Transfer failed';

  @override
  String get topupFailedBody => 'The card\'s bank declined the operation. No money was taken; try another card.';

  @override
  String topupFailedDetail(String message) {
    return 'Gateway: $message';
  }

  @override
  String get topupUnknownTitle => 'Status unknown';

  @override
  String get topupUnknownBody => 'Check the order in the merchant portal before trying again.';

  @override
  String get topupTryAgain => 'Try again';

  @override
  String get topupAddCard => 'Add card';

  @override
  String get topupAddingCard => 'Adding card…';

  @override
  String get topupCardAddedTitle => 'Card added';

  @override
  String get topupCardAddedBody => 'Choose it in the payment sheet at your next top-up. No money was taken.';

  @override
  String get topupCardFailedTitle => 'Card not added';

  @override
  String get topupCardFailedBody => 'The card\'s bank did not confirm it. No money was taken; try another card.';

  @override
  String get topupCardUnknownBody =>
      'The bank has not confirmed the card yet. If it has, the card appears at your next top-up.';

  @override
  String doneCredited(String account) {
    return 'credited to investment account •• $account';
  }

  @override
  String get doneOperation => 'Operation';

  @override
  String get doneOperationValue => 'AFT · to own account';

  @override
  String get doneFreeCash => 'Free cash';

  @override
  String get doneOrder => 'Order';

  @override
  String get doneTime => 'Time';

  @override
  String doneInvest(String amount) {
    return 'Invest $amount';
  }

  @override
  String get doneToPortfolio => 'To portfolio';

  @override
  String get devInvestReset => 'Reset Northwind demo account';

  @override
  String get devInvestResetDone => 'Demo account reset';

  @override
  String get scenarioWalletApple => 'Apple Pay';

  @override
  String get devApplePay => 'Apple Pay';
}
