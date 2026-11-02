import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_ar.dart';
import 'app_localizations_bg.dart';
import 'app_localizations_de.dart';
import 'app_localizations_el.dart';
import 'app_localizations_en.dart';
import 'app_localizations_es.dart';
import 'app_localizations_fr.dart';
import 'app_localizations_ro.dart';
import 'app_localizations_ru.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale) : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations)!;
  }

  static const LocalizationsDelegate<AppLocalizations> delegate = _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates = <LocalizationsDelegate<dynamic>>[
    delegate,
    GlobalMaterialLocalizations.delegate,
    GlobalCupertinoLocalizations.delegate,
    GlobalWidgetsLocalizations.delegate,
  ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[
    Locale('ar'),
    Locale('bg'),
    Locale('de'),
    Locale('el'),
    Locale('en'),
    Locale('es'),
    Locale('fr'),
    Locale('ro'),
    Locale('ru'),
  ];

  /// No description provided for @startTitle.
  ///
  /// In en, this message translates to:
  /// **'Payment demo'**
  String get startTitle;

  /// No description provided for @startSettings.
  ///
  /// In en, this message translates to:
  /// **'Developer settings'**
  String get startSettings;

  /// No description provided for @coverFurnitureName.
  ///
  /// In en, this message translates to:
  /// **'Hearth & Co'**
  String get coverFurnitureName;

  /// No description provided for @coverCoffeeName.
  ///
  /// In en, this message translates to:
  /// **'Roast Lane'**
  String get coverCoffeeName;

  /// No description provided for @coverTicketsName.
  ///
  /// In en, this message translates to:
  /// **'Stage Pass'**
  String get coverTicketsName;

  /// No description provided for @coverFurnitureTagline.
  ///
  /// In en, this message translates to:
  /// **'Furniture · catalogue and cart'**
  String get coverFurnitureTagline;

  /// No description provided for @coverCoffeeTagline.
  ///
  /// In en, this message translates to:
  /// **'Coffee shop · order ahead'**
  String get coverCoffeeTagline;

  /// No description provided for @coverTicketsTagline.
  ///
  /// In en, this message translates to:
  /// **'Tickets · concerts and shows'**
  String get coverTicketsTagline;

  /// No description provided for @storeBack.
  ///
  /// In en, this message translates to:
  /// **'Back'**
  String get storeBack;

  /// No description provided for @storeCart.
  ///
  /// In en, this message translates to:
  /// **'Cart'**
  String get storeCart;

  /// No description provided for @storeCartCount.
  ///
  /// In en, this message translates to:
  /// **'Cart · {count}'**
  String storeCartCount(int count);

  /// No description provided for @storeItems.
  ///
  /// In en, this message translates to:
  /// **'Items'**
  String get storeItems;

  /// No description provided for @storeTotal.
  ///
  /// In en, this message translates to:
  /// **'Total'**
  String get storeTotal;

  /// No description provided for @storePay.
  ///
  /// In en, this message translates to:
  /// **'Pay {amount}'**
  String storePay(String amount);

  /// No description provided for @storeIncrease.
  ///
  /// In en, this message translates to:
  /// **'More'**
  String get storeIncrease;

  /// No description provided for @storeDecrease.
  ///
  /// In en, this message translates to:
  /// **'Less'**
  String get storeDecrease;

  /// No description provided for @storeAddToCart.
  ///
  /// In en, this message translates to:
  /// **'Add to cart · {price}'**
  String storeAddToCart(String price);

  /// No description provided for @storeAdd.
  ///
  /// In en, this message translates to:
  /// **'Add'**
  String get storeAdd;

  /// No description provided for @storeRegistering.
  ///
  /// In en, this message translates to:
  /// **'Creating the order…'**
  String get storeRegistering;

  /// No description provided for @storeErrorCredentials.
  ///
  /// In en, this message translates to:
  /// **'Merchant login and password are not set.'**
  String get storeErrorCredentials;

  /// No description provided for @storeErrorApiKey.
  ///
  /// In en, this message translates to:
  /// **'The API key is not set.'**
  String get storeErrorApiKey;

  /// No description provided for @storeErrorFailed.
  ///
  /// In en, this message translates to:
  /// **'The order was not created: {message}'**
  String storeErrorFailed(String message);

  /// No description provided for @storeOpenSettings.
  ///
  /// In en, this message translates to:
  /// **'Open settings'**
  String get storeOpenSettings;

  /// No description provided for @scenarioRow.
  ///
  /// In en, this message translates to:
  /// **'Scenario: {buyer}'**
  String scenarioRow(String buyer);

  /// No description provided for @scenarioTitle.
  ///
  /// In en, this message translates to:
  /// **'Payment scenario'**
  String get scenarioTitle;

  /// No description provided for @scenarioBuyer.
  ///
  /// In en, this message translates to:
  /// **'Buyer'**
  String get scenarioBuyer;

  /// No description provided for @scenarioGuest.
  ///
  /// In en, this message translates to:
  /// **'Guest'**
  String get scenarioGuest;

  /// No description provided for @scenarioWithCards.
  ///
  /// In en, this message translates to:
  /// **'With cards'**
  String get scenarioWithCards;

  /// No description provided for @scenarioWithCardsHint.
  ///
  /// In en, this message translates to:
  /// **'Cards appear after a payment with “Save card”'**
  String get scenarioWithCardsHint;

  /// No description provided for @scenarioReset.
  ///
  /// In en, this message translates to:
  /// **'Reset'**
  String get scenarioReset;

  /// No description provided for @scenarioPayment.
  ///
  /// In en, this message translates to:
  /// **'Payment'**
  String get scenarioPayment;

  /// No description provided for @scenarioWallet.
  ///
  /// In en, this message translates to:
  /// **'Google Pay'**
  String get scenarioWallet;

  /// No description provided for @scenarioScanner.
  ///
  /// In en, this message translates to:
  /// **'Card scanner'**
  String get scenarioScanner;

  /// No description provided for @scenarioSdkResult.
  ///
  /// In en, this message translates to:
  /// **'SDK result screen'**
  String get scenarioSdkResult;

  /// No description provided for @resultPaidAmount.
  ///
  /// In en, this message translates to:
  /// **'Paid {amount}'**
  String resultPaidAmount(String amount);

  /// No description provided for @resultPaymentId.
  ///
  /// In en, this message translates to:
  /// **'Payment'**
  String get resultPaymentId;

  /// No description provided for @resultAmount.
  ///
  /// In en, this message translates to:
  /// **'Amount'**
  String get resultAmount;

  /// No description provided for @resultDeclinedTitle.
  ///
  /// In en, this message translates to:
  /// **'Payment declined'**
  String get resultDeclinedTitle;

  /// No description provided for @resultDeclinedBody.
  ///
  /// In en, this message translates to:
  /// **'The bank did not confirm the payment.'**
  String get resultDeclinedBody;

  /// No description provided for @resultUnconfirmedTitle.
  ///
  /// In en, this message translates to:
  /// **'Checking your payment'**
  String get resultUnconfirmedTitle;

  /// No description provided for @resultUnconfirmedBody.
  ///
  /// In en, this message translates to:
  /// **'The bank has not answered yet. Please don’t pay again — we’ll let you know as soon as the payment is confirmed.'**
  String get resultUnconfirmedBody;

  /// No description provided for @resultTryAgain.
  ///
  /// In en, this message translates to:
  /// **'Try again'**
  String get resultTryAgain;

  /// No description provided for @resultBackToStore.
  ///
  /// In en, this message translates to:
  /// **'Back to the store'**
  String get resultBackToStore;

  /// No description provided for @resultDone.
  ///
  /// In en, this message translates to:
  /// **'Done'**
  String get resultDone;

  /// No description provided for @devTitle.
  ///
  /// In en, this message translates to:
  /// **'Developer settings'**
  String get devTitle;

  /// No description provided for @devGateway.
  ///
  /// In en, this message translates to:
  /// **'Gateway'**
  String get devGateway;

  /// No description provided for @devBaseUrl.
  ///
  /// In en, this message translates to:
  /// **'Base URL'**
  String get devBaseUrl;

  /// No description provided for @devLogin.
  ///
  /// In en, this message translates to:
  /// **'Merchant login'**
  String get devLogin;

  /// No description provided for @devPassword.
  ///
  /// In en, this message translates to:
  /// **'Password'**
  String get devPassword;

  /// No description provided for @devApiV1.
  ///
  /// In en, this message translates to:
  /// **'API V1'**
  String get devApiV1;

  /// No description provided for @devApiV2.
  ///
  /// In en, this message translates to:
  /// **'API V2'**
  String get devApiV2;

  /// No description provided for @devApiVersion.
  ///
  /// In en, this message translates to:
  /// **'Merchant API'**
  String get devApiVersion;

  /// No description provided for @devApiV1Hint.
  ///
  /// In en, this message translates to:
  /// **'Orders are registered with register.do using the merchant login and password.'**
  String get devApiV1Hint;

  /// No description provided for @devApiV2Hint.
  ///
  /// In en, this message translates to:
  /// **'Sessions are created with POST /api2/sessions using the API key.'**
  String get devApiV2Hint;

  /// No description provided for @devApiKey.
  ///
  /// In en, this message translates to:
  /// **'API key'**
  String get devApiKey;

  /// No description provided for @devOrderType.
  ///
  /// In en, this message translates to:
  /// **'Order'**
  String get devOrderType;

  /// No description provided for @devOrderPurchase.
  ///
  /// In en, this message translates to:
  /// **'Payment'**
  String get devOrderPurchase;

  /// No description provided for @devOrderAft.
  ///
  /// In en, this message translates to:
  /// **'AFT transfer'**
  String get devOrderAft;

  /// No description provided for @devOrderAftHint.
  ///
  /// In en, this message translates to:
  /// **'Transfers are registered with registerP2P.do (one-leg AFT): the sheet asks for the sender and recipient details.'**
  String get devOrderAftHint;

  /// No description provided for @devCredentialsNote.
  ///
  /// In en, this message translates to:
  /// **'Stored only on this device. In your app the order is registered by your backend.'**
  String get devCredentialsNote;

  /// No description provided for @devSdkLook.
  ///
  /// In en, this message translates to:
  /// **'SDK appearance'**
  String get devSdkLook;

  /// No description provided for @devSheetTheme.
  ///
  /// In en, this message translates to:
  /// **'Sheet theme'**
  String get devSheetTheme;

  /// No description provided for @devThemeCover.
  ///
  /// In en, this message translates to:
  /// **'As the store'**
  String get devThemeCover;

  /// No description provided for @devThemeLight.
  ///
  /// In en, this message translates to:
  /// **'Light'**
  String get devThemeLight;

  /// No description provided for @devThemeDark.
  ///
  /// In en, this message translates to:
  /// **'Dark'**
  String get devThemeDark;

  /// No description provided for @devThemeSystem.
  ///
  /// In en, this message translates to:
  /// **'System'**
  String get devThemeSystem;

  /// No description provided for @devLanguage.
  ///
  /// In en, this message translates to:
  /// **'Language'**
  String get devLanguage;

  /// No description provided for @devLanguageSystem.
  ///
  /// In en, this message translates to:
  /// **'System'**
  String get devLanguageSystem;

  /// No description provided for @devWallets.
  ///
  /// In en, this message translates to:
  /// **'Google Pay'**
  String get devWallets;

  /// No description provided for @devGpayGateway.
  ///
  /// In en, this message translates to:
  /// **'Gateway'**
  String get devGpayGateway;

  /// No description provided for @devGpayMerchant.
  ///
  /// In en, this message translates to:
  /// **'Merchant ID'**
  String get devGpayMerchant;

  /// No description provided for @devGpayFromGateway.
  ///
  /// In en, this message translates to:
  /// **'From the gateway'**
  String get devGpayFromGateway;

  /// No description provided for @devMore.
  ///
  /// In en, this message translates to:
  /// **'More'**
  String get devMore;

  /// No description provided for @devAdvanced.
  ///
  /// In en, this message translates to:
  /// **'Advanced'**
  String get devAdvanced;

  /// No description provided for @devAdvancedSub.
  ///
  /// In en, this message translates to:
  /// **'State stream, SSL pinning'**
  String get devAdvancedSub;

  /// No description provided for @devLastOrder.
  ///
  /// In en, this message translates to:
  /// **'Last order'**
  String get devLastOrder;

  /// No description provided for @devCopy.
  ///
  /// In en, this message translates to:
  /// **'Copy'**
  String get devCopy;

  /// No description provided for @devNone.
  ///
  /// In en, this message translates to:
  /// **'—'**
  String get devNone;

  /// No description provided for @resultSuccessFurniture.
  ///
  /// In en, this message translates to:
  /// **'Thank you for your order'**
  String get resultSuccessFurniture;

  /// No description provided for @resultSuccessFurnitureBody.
  ///
  /// In en, this message translates to:
  /// **'We will be in touch to arrange delivery.'**
  String get resultSuccessFurnitureBody;

  /// No description provided for @resultSuccessCoffee.
  ///
  /// In en, this message translates to:
  /// **'Your order is being prepared'**
  String get resultSuccessCoffee;

  /// No description provided for @resultSuccessCoffeeBody.
  ///
  /// In en, this message translates to:
  /// **'Show this number to the barista.'**
  String get resultSuccessCoffeeBody;

  /// No description provided for @resultSuccessTickets.
  ///
  /// In en, this message translates to:
  /// **'You are going!'**
  String get resultSuccessTickets;

  /// No description provided for @resultSuccessTicketsBody.
  ///
  /// In en, this message translates to:
  /// **'Tickets are in your account.'**
  String get resultSuccessTicketsBody;

  /// No description provided for @furnitureCollection.
  ///
  /// In en, this message translates to:
  /// **'Autumn\ncollection'**
  String get furnitureCollection;

  /// No description provided for @furnitureAll.
  ///
  /// In en, this message translates to:
  /// **'All'**
  String get furnitureAll;

  /// No description provided for @furnitureChairs.
  ///
  /// In en, this message translates to:
  /// **'Chairs'**
  String get furnitureChairs;

  /// No description provided for @furnitureSofas.
  ///
  /// In en, this message translates to:
  /// **'Sofas'**
  String get furnitureSofas;

  /// No description provided for @furnitureLight.
  ///
  /// In en, this message translates to:
  /// **'Light'**
  String get furnitureLight;

  /// No description provided for @furnitureDelivery.
  ///
  /// In en, this message translates to:
  /// **'Free delivery in 3–5 days. 30 days to return.'**
  String get furnitureDelivery;

  /// No description provided for @furnitureDeliveryLabel.
  ///
  /// In en, this message translates to:
  /// **'Delivery'**
  String get furnitureDeliveryLabel;

  /// No description provided for @furnitureDeliveryFree.
  ///
  /// In en, this message translates to:
  /// **'Free'**
  String get furnitureDeliveryFree;

  /// No description provided for @furnitureColours.
  ///
  /// In en, this message translates to:
  /// **'Colour'**
  String get furnitureColours;

  /// No description provided for @coffeeLocation.
  ///
  /// In en, this message translates to:
  /// **'King St.'**
  String get coffeeLocation;

  /// No description provided for @coffeeAgain.
  ///
  /// In en, this message translates to:
  /// **'Same as last time'**
  String get coffeeAgain;

  /// No description provided for @coffeeRepeat.
  ///
  /// In en, this message translates to:
  /// **'Order again'**
  String get coffeeRepeat;

  /// No description provided for @coffeeTabCoffee.
  ///
  /// In en, this message translates to:
  /// **'Coffee'**
  String get coffeeTabCoffee;

  /// No description provided for @coffeeTabCold.
  ///
  /// In en, this message translates to:
  /// **'Cold'**
  String get coffeeTabCold;

  /// No description provided for @coffeeTabBakery.
  ///
  /// In en, this message translates to:
  /// **'Bakery'**
  String get coffeeTabBakery;

  /// No description provided for @coffeeOrderTitle.
  ///
  /// In en, this message translates to:
  /// **'Your order'**
  String get coffeeOrderTitle;

  /// No description provided for @coffeePickup.
  ///
  /// In en, this message translates to:
  /// **'Pick up'**
  String get coffeePickup;

  /// No description provided for @coffeePickupValue.
  ///
  /// In en, this message translates to:
  /// **'King St. · in 10 min'**
  String get coffeePickupValue;

  /// No description provided for @coffeeAddItem.
  ///
  /// In en, this message translates to:
  /// **'Add {item}'**
  String coffeeAddItem(String item);

  /// No description provided for @coffeeOrderNumberHint.
  ///
  /// In en, this message translates to:
  /// **'Ready in about 6 minutes.'**
  String get coffeeOrderNumberHint;

  /// No description provided for @ticketsLive.
  ///
  /// In en, this message translates to:
  /// **'Live in Hall A'**
  String get ticketsLive;

  /// No description provided for @ticketsTitle.
  ///
  /// In en, this message translates to:
  /// **'NORTHERN\nLIGHTS'**
  String get ticketsTitle;

  /// No description provided for @ticketsDate.
  ///
  /// In en, this message translates to:
  /// **'Sat, 14 Nov · 20:00 · Hall A'**
  String get ticketsDate;

  /// No description provided for @ticketsSection.
  ///
  /// In en, this message translates to:
  /// **'Tickets'**
  String get ticketsSection;

  /// No description provided for @ticketsToPayment.
  ///
  /// In en, this message translates to:
  /// **'Checkout · {amount}'**
  String ticketsToPayment(String amount);

  /// No description provided for @ticketsCheckout.
  ///
  /// In en, this message translates to:
  /// **'Checkout'**
  String get ticketsCheckout;

  /// No description provided for @ticketsEventLine.
  ///
  /// In en, this message translates to:
  /// **'14 Nov · {category}'**
  String ticketsEventLine(String category);

  /// No description provided for @ticketsHold.
  ///
  /// In en, this message translates to:
  /// **'Seats held for {time}'**
  String ticketsHold(String time);

  /// No description provided for @ticketsHoldExpired.
  ///
  /// In en, this message translates to:
  /// **'The hold ran out. Pick your seats again.'**
  String get ticketsHoldExpired;

  /// No description provided for @ticketsSeats.
  ///
  /// In en, this message translates to:
  /// **'Tickets'**
  String get ticketsSeats;

  /// No description provided for @productVelvetSofa.
  ///
  /// In en, this message translates to:
  /// **'Velvet sofa'**
  String get productVelvetSofa;

  /// No description provided for @productCloudChair.
  ///
  /// In en, this message translates to:
  /// **'Cloud chair'**
  String get productCloudChair;

  /// No description provided for @productOnoChair.
  ///
  /// In en, this message translates to:
  /// **'Ono chair'**
  String get productOnoChair;

  /// No description provided for @productBirchStool.
  ///
  /// In en, this message translates to:
  /// **'Birch stool'**
  String get productBirchStool;

  /// No description provided for @productArcLamp.
  ///
  /// In en, this message translates to:
  /// **'Arc floor lamp'**
  String get productArcLamp;

  /// No description provided for @productLumeArmchair.
  ///
  /// In en, this message translates to:
  /// **'Lume armchair'**
  String get productLumeArmchair;

  /// No description provided for @productFlatWhite.
  ///
  /// In en, this message translates to:
  /// **'Flat white'**
  String get productFlatWhite;

  /// No description provided for @productCappuccino.
  ///
  /// In en, this message translates to:
  /// **'Cappuccino'**
  String get productCappuccino;

  /// No description provided for @productEspresso.
  ///
  /// In en, this message translates to:
  /// **'Espresso'**
  String get productEspresso;

  /// No description provided for @productIcedLatte.
  ///
  /// In en, this message translates to:
  /// **'Iced latte'**
  String get productIcedLatte;

  /// No description provided for @productCroissant.
  ///
  /// In en, this message translates to:
  /// **'Butter croissant'**
  String get productCroissant;

  /// No description provided for @productVelvetSofaSub.
  ///
  /// In en, this message translates to:
  /// **'Emerald velvet, oak legs'**
  String get productVelvetSofaSub;

  /// No description provided for @productCloudChairSub.
  ///
  /// In en, this message translates to:
  /// **'Milk-white bouclé'**
  String get productCloudChairSub;

  /// No description provided for @productOnoChairSub.
  ///
  /// In en, this message translates to:
  /// **'Moulded shell, beech legs'**
  String get productOnoChairSub;

  /// No description provided for @productBirchStoolSub.
  ///
  /// In en, this message translates to:
  /// **'Solid birch'**
  String get productBirchStoolSub;

  /// No description provided for @productArcLampSub.
  ///
  /// In en, this message translates to:
  /// **'Matte graphite steel'**
  String get productArcLampSub;

  /// No description provided for @productLumeArmchairSub.
  ///
  /// In en, this message translates to:
  /// **'Mustard wool'**
  String get productLumeArmchairSub;

  /// No description provided for @productFlatWhiteSub.
  ///
  /// In en, this message translates to:
  /// **'Double shot, steamed milk'**
  String get productFlatWhiteSub;

  /// No description provided for @productCappuccinoSub.
  ///
  /// In en, this message translates to:
  /// **'Classic, with foam'**
  String get productCappuccinoSub;

  /// No description provided for @productEspressoSub.
  ///
  /// In en, this message translates to:
  /// **'Single origin'**
  String get productEspressoSub;

  /// No description provided for @productIcedLatteSub.
  ///
  /// In en, this message translates to:
  /// **'Cold milk, ice'**
  String get productIcedLatteSub;

  /// No description provided for @productCroissantSub.
  ///
  /// In en, this message translates to:
  /// **'Baked this morning'**
  String get productCroissantSub;

  /// No description provided for @ticketFloor.
  ///
  /// In en, this message translates to:
  /// **'Standing floor'**
  String get ticketFloor;

  /// No description provided for @ticketFloorSub.
  ///
  /// In en, this message translates to:
  /// **'General admission'**
  String get ticketFloorSub;

  /// No description provided for @ticketBalcony.
  ///
  /// In en, this message translates to:
  /// **'Balcony'**
  String get ticketBalcony;

  /// No description provided for @ticketBalconySub.
  ///
  /// In en, this message translates to:
  /// **'Row 2 · seated'**
  String get ticketBalconySub;

  /// No description provided for @ticketFee.
  ///
  /// In en, this message translates to:
  /// **'Service fee'**
  String get ticketFee;

  /// No description provided for @startSectionPayment.
  ///
  /// In en, this message translates to:
  /// **'SDK Payment'**
  String get startSectionPayment;

  /// No description provided for @startSectionPaymentSub.
  ///
  /// In en, this message translates to:
  /// **'A ready payment sheet in your app — every store has all scenarios'**
  String get startSectionPaymentSub;

  /// No description provided for @startSectionCore.
  ///
  /// In en, this message translates to:
  /// **'SDK Core'**
  String get startSectionCore;

  /// No description provided for @startSectionCoreSub.
  ///
  /// In en, this message translates to:
  /// **'Card encryption for your own form (PCI DSS)'**
  String get startSectionCoreSub;

  /// No description provided for @coreTileTitle.
  ///
  /// In en, this message translates to:
  /// **'Your own card form'**
  String get coreTileTitle;

  /// No description provided for @coreTileSub.
  ///
  /// In en, this message translates to:
  /// **'Card data turns into a token for your server'**
  String get coreTileSub;

  /// No description provided for @coreTitle.
  ///
  /// In en, this message translates to:
  /// **'SDK Core'**
  String get coreTitle;

  /// No description provided for @coreDoneSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Token is ready'**
  String get coreDoneSubtitle;

  /// No description provided for @coreVariantNew.
  ///
  /// In en, this message translates to:
  /// **'New card'**
  String get coreVariantNew;

  /// No description provided for @coreVariantInstant.
  ///
  /// In en, this message translates to:
  /// **'No order'**
  String get coreVariantInstant;

  /// No description provided for @coreVariantStored.
  ///
  /// In en, this message translates to:
  /// **'Saved card'**
  String get coreVariantStored;

  /// No description provided for @coreHintNew.
  ///
  /// In en, this message translates to:
  /// **'Pays an order you registered: card data plus its mdOrder.'**
  String get coreHintNew;

  /// No description provided for @coreHintInstant.
  ///
  /// In en, this message translates to:
  /// **'No order yet: your server creates it and pays in one call.'**
  String get coreHintInstant;

  /// No description provided for @coreHintStored.
  ///
  /// In en, this message translates to:
  /// **'Pays an order with a saved card: its bindingId instead of card data.'**
  String get coreHintStored;

  /// No description provided for @coreCardGroup.
  ///
  /// In en, this message translates to:
  /// **'Card information'**
  String get coreCardGroup;

  /// No description provided for @corePan.
  ///
  /// In en, this message translates to:
  /// **'Card number'**
  String get corePan;

  /// No description provided for @coreExpiryPlaceholder.
  ///
  /// In en, this message translates to:
  /// **'MM/YY'**
  String get coreExpiryPlaceholder;

  /// No description provided for @coreExpiry.
  ///
  /// In en, this message translates to:
  /// **'Expiry'**
  String get coreExpiry;

  /// No description provided for @coreCvc.
  ///
  /// In en, this message translates to:
  /// **'CVC/CVV'**
  String get coreCvc;

  /// No description provided for @coreCvcOptional.
  ///
  /// In en, this message translates to:
  /// **'CVC/CVV (optional)'**
  String get coreCvcOptional;

  /// No description provided for @coreHolder.
  ///
  /// In en, this message translates to:
  /// **'Name on card (optional)'**
  String get coreHolder;

  /// No description provided for @coreBinding.
  ///
  /// In en, this message translates to:
  /// **'bindingId'**
  String get coreBinding;

  /// No description provided for @coreMdOrder.
  ///
  /// In en, this message translates to:
  /// **'mdOrder'**
  String get coreMdOrder;

  /// No description provided for @coreMdOrderHint.
  ///
  /// In en, this message translates to:
  /// **'From your backend'**
  String get coreMdOrderHint;

  /// No description provided for @corePubKey.
  ///
  /// In en, this message translates to:
  /// **'Public key (PEM)'**
  String get corePubKey;

  /// No description provided for @corePaste.
  ///
  /// In en, this message translates to:
  /// **'Paste'**
  String get corePaste;

  /// No description provided for @coreGenerate.
  ///
  /// In en, this message translates to:
  /// **'Get token'**
  String get coreGenerate;

  /// No description provided for @coreErrorRequired.
  ///
  /// In en, this message translates to:
  /// **'Required'**
  String get coreErrorRequired;

  /// No description provided for @coreErrorInvalid.
  ///
  /// In en, this message translates to:
  /// **'Invalid value'**
  String get coreErrorInvalid;

  /// No description provided for @coreErrorFormat.
  ///
  /// In en, this message translates to:
  /// **'Wrong format'**
  String get coreErrorFormat;

  /// No description provided for @coreTokenOk.
  ///
  /// In en, this message translates to:
  /// **'Token received'**
  String get coreTokenOk;

  /// No description provided for @coreCopy.
  ///
  /// In en, this message translates to:
  /// **'Copy'**
  String get coreCopy;

  /// No description provided for @coreCopied.
  ///
  /// In en, this message translates to:
  /// **'Copied'**
  String get coreCopied;

  /// No description provided for @coreStepSend.
  ///
  /// In en, this message translates to:
  /// **'Send the token to your server'**
  String get coreStepSend;

  /// No description provided for @coreStepGateway.
  ///
  /// In en, this message translates to:
  /// **'Your server passes it to the gateway as seToken'**
  String get coreStepGateway;

  /// No description provided for @coreStepPay.
  ///
  /// In en, this message translates to:
  /// **'The gateway processes the payment'**
  String get coreStepPay;

  /// No description provided for @coreAgain.
  ///
  /// In en, this message translates to:
  /// **'Again'**
  String get coreAgain;

  /// No description provided for @investName.
  ///
  /// In en, this message translates to:
  /// **'Northwind Invest'**
  String get investName;

  /// No description provided for @startSectionPaymentAftSub.
  ///
  /// In en, this message translates to:
  /// **'AFT: top up an account from a card'**
  String get startSectionPaymentAftSub;

  /// No description provided for @investTileSub.
  ///
  /// In en, this message translates to:
  /// **'Top up your investment account from a card and watch the balance grow'**
  String get investTileSub;

  /// No description provided for @investAccount.
  ///
  /// In en, this message translates to:
  /// **'Investment account •• {account}'**
  String investAccount(String account);

  /// No description provided for @investPortfolioValue.
  ///
  /// In en, this message translates to:
  /// **'Portfolio value'**
  String get investPortfolioValue;

  /// No description provided for @investChangeMonth.
  ///
  /// In en, this message translates to:
  /// **'this month'**
  String get investChangeMonth;

  /// No description provided for @investPeriod1w.
  ///
  /// In en, this message translates to:
  /// **'1W'**
  String get investPeriod1w;

  /// No description provided for @investPeriod1m.
  ///
  /// In en, this message translates to:
  /// **'1M'**
  String get investPeriod1m;

  /// No description provided for @investPeriod3m.
  ///
  /// In en, this message translates to:
  /// **'3M'**
  String get investPeriod3m;

  /// No description provided for @investPeriod1y.
  ///
  /// In en, this message translates to:
  /// **'1Y'**
  String get investPeriod1y;

  /// No description provided for @investPeriodAll.
  ///
  /// In en, this message translates to:
  /// **'All'**
  String get investPeriodAll;

  /// No description provided for @investTopUp.
  ///
  /// In en, this message translates to:
  /// **'Top up'**
  String get investTopUp;

  /// No description provided for @investInvest.
  ///
  /// In en, this message translates to:
  /// **'Invest'**
  String get investInvest;

  /// No description provided for @investFreeCash.
  ///
  /// In en, this message translates to:
  /// **'Free cash'**
  String get investFreeCash;

  /// No description provided for @investFreeCashSub.
  ///
  /// In en, this message translates to:
  /// **'Ready to invest'**
  String get investFreeCashSub;

  /// No description provided for @investFreeCashJustNow.
  ///
  /// In en, this message translates to:
  /// **'+{amount} just now'**
  String investFreeCashJustNow(String amount);

  /// No description provided for @investMyAssets.
  ///
  /// In en, this message translates to:
  /// **'My assets'**
  String get investMyAssets;

  /// No description provided for @investUnits.
  ///
  /// In en, this message translates to:
  /// **'{units} units · {price}'**
  String investUnits(String units, String price);

  /// No description provided for @investRecent.
  ///
  /// In en, this message translates to:
  /// **'Recent operations'**
  String get investRecent;

  /// No description provided for @investOpTopUp.
  ///
  /// In en, this message translates to:
  /// **'Deposit'**
  String get investOpTopUp;

  /// No description provided for @investOpBuy.
  ///
  /// In en, this message translates to:
  /// **'Buy {asset}'**
  String investOpBuy(String asset);

  /// No description provided for @investAssetGlb.
  ///
  /// In en, this message translates to:
  /// **'Global ETF'**
  String get investAssetGlb;

  /// No description provided for @investAssetTch.
  ///
  /// In en, this message translates to:
  /// **'Tech Leaders'**
  String get investAssetTch;

  /// No description provided for @investNotifications.
  ///
  /// In en, this message translates to:
  /// **'Notifications'**
  String get investNotifications;

  /// No description provided for @topupTitle.
  ///
  /// In en, this message translates to:
  /// **'Top up'**
  String get topupTitle;

  /// No description provided for @topupTo.
  ///
  /// In en, this message translates to:
  /// **'To'**
  String get topupTo;

  /// No description provided for @topupAvailable.
  ///
  /// In en, this message translates to:
  /// **'Available'**
  String get topupAvailable;

  /// No description provided for @topupAmount.
  ///
  /// In en, this message translates to:
  /// **'Amount'**
  String get topupAmount;

  /// No description provided for @topupLimits.
  ///
  /// In en, this message translates to:
  /// **'from {min} to {max}'**
  String topupLimits(String min, String max);

  /// No description provided for @topupContinue.
  ///
  /// In en, this message translates to:
  /// **'Continue · {amount}'**
  String topupContinue(String amount);

  /// No description provided for @topupCreating.
  ///
  /// In en, this message translates to:
  /// **'Creating transfer…'**
  String get topupCreating;

  /// No description provided for @topupFailedTitle.
  ///
  /// In en, this message translates to:
  /// **'Transfer failed'**
  String get topupFailedTitle;

  /// No description provided for @topupFailedBody.
  ///
  /// In en, this message translates to:
  /// **'The card\'s bank declined the operation. No money was taken; try another card.'**
  String get topupFailedBody;

  /// No description provided for @topupFailedDetail.
  ///
  /// In en, this message translates to:
  /// **'Gateway: {message}'**
  String topupFailedDetail(String message);

  /// No description provided for @topupUnknownTitle.
  ///
  /// In en, this message translates to:
  /// **'Status unknown'**
  String get topupUnknownTitle;

  /// No description provided for @topupUnknownBody.
  ///
  /// In en, this message translates to:
  /// **'Check the order in the merchant portal before trying again.'**
  String get topupUnknownBody;

  /// No description provided for @topupTryAgain.
  ///
  /// In en, this message translates to:
  /// **'Try again'**
  String get topupTryAgain;

  /// No description provided for @doneCredited.
  ///
  /// In en, this message translates to:
  /// **'credited to investment account •• {account}'**
  String doneCredited(String account);

  /// No description provided for @doneOperation.
  ///
  /// In en, this message translates to:
  /// **'Operation'**
  String get doneOperation;

  /// No description provided for @doneOperationValue.
  ///
  /// In en, this message translates to:
  /// **'AFT · to own account'**
  String get doneOperationValue;

  /// No description provided for @doneFreeCash.
  ///
  /// In en, this message translates to:
  /// **'Free cash'**
  String get doneFreeCash;

  /// No description provided for @doneOrder.
  ///
  /// In en, this message translates to:
  /// **'Order'**
  String get doneOrder;

  /// No description provided for @doneTime.
  ///
  /// In en, this message translates to:
  /// **'Time'**
  String get doneTime;

  /// No description provided for @doneInvest.
  ///
  /// In en, this message translates to:
  /// **'Invest {amount}'**
  String doneInvest(String amount);

  /// No description provided for @doneToPortfolio.
  ///
  /// In en, this message translates to:
  /// **'To portfolio'**
  String get doneToPortfolio;

  /// No description provided for @devInvestReset.
  ///
  /// In en, this message translates to:
  /// **'Reset Northwind demo account'**
  String get devInvestReset;

  /// No description provided for @devInvestResetDone.
  ///
  /// In en, this message translates to:
  /// **'Demo account reset'**
  String get devInvestResetDone;

  /// No description provided for @scenarioWalletApple.
  ///
  /// In en, this message translates to:
  /// **'Apple Pay'**
  String get scenarioWalletApple;

  /// No description provided for @devApplePay.
  ///
  /// In en, this message translates to:
  /// **'Apple Pay'**
  String get devApplePay;
}

class _AppLocalizationsDelegate extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['ar', 'bg', 'de', 'el', 'en', 'es', 'fr', 'ro', 'ru'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'ar':
      return AppLocalizationsAr();
    case 'bg':
      return AppLocalizationsBg();
    case 'de':
      return AppLocalizationsDe();
    case 'el':
      return AppLocalizationsEl();
    case 'en':
      return AppLocalizationsEn();
    case 'es':
      return AppLocalizationsEs();
    case 'fr':
      return AppLocalizationsFr();
    case 'ro':
      return AppLocalizationsRo();
    case 'ru':
      return AppLocalizationsRu();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}
