// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Bulgarian (`bg`).
class AppLocalizationsBg extends AppLocalizations {
  AppLocalizationsBg([String locale = 'bg']) : super(locale);

  @override
  String get startTitle => 'Демо на плащане';

  @override
  String get startSettings => 'Настройки за разработчици';

  @override
  String get coverFurnitureName => 'Hearth & Co';

  @override
  String get coverCoffeeName => 'Roast Lane';

  @override
  String get coverTicketsName => 'Stage Pass';

  @override
  String get coverFurnitureTagline => 'Мебели · каталог и количка';

  @override
  String get coverCoffeeTagline => 'Кафене · поръчка предварително';

  @override
  String get coverTicketsTagline => 'Билети · концерти и спектакли';

  @override
  String get storeBack => 'Назад';

  @override
  String get storeCart => 'Количка';

  @override
  String storeCartCount(int count) {
    return 'Количка · $count';
  }

  @override
  String get storeItems => 'Артикули';

  @override
  String get storeTotal => 'Общо';

  @override
  String storePay(String amount) {
    return 'Плати $amount';
  }

  @override
  String get storeIncrease => 'Повече';

  @override
  String get storeDecrease => 'По-малко';

  @override
  String storeAddToCart(String price) {
    return 'Добави в количката · $price';
  }

  @override
  String get storeAdd => 'Добави';

  @override
  String get storeRegistering => 'Създаване на поръчката…';

  @override
  String get storeErrorCredentials => 'Не са зададени потребителско име и парола на търговеца.';

  @override
  String get storeErrorApiKey => 'Не е зададен API ключ.';

  @override
  String storeErrorFailed(String message) {
    return 'Поръчката не беше създадена: $message';
  }

  @override
  String get storeOpenSettings => 'Отвори настройките';

  @override
  String scenarioRow(String buyer) {
    return 'Сценарий: $buyer';
  }

  @override
  String get scenarioTitle => 'Сценарий на плащане';

  @override
  String get scenarioBuyer => 'Купувач';

  @override
  String get scenarioGuest => 'Гост';

  @override
  String get scenarioWithCards => 'С карти';

  @override
  String get scenarioWithCardsHint => 'Картите се появяват след плащане с „Запази картата“';

  @override
  String get scenarioReset => 'Нулирай';

  @override
  String get scenarioPayment => 'Плащане';

  @override
  String get scenarioWallet => 'Google Pay';

  @override
  String get scenarioScanner => 'Скенер на карти';

  @override
  String get scenarioSdkResult => 'Екран с резултата от SDK';

  @override
  String resultPaidAmount(String amount) {
    return 'Платено $amount';
  }

  @override
  String get resultPaymentId => 'Плащане';

  @override
  String get resultAmount => 'Сума';

  @override
  String get resultDeclinedTitle => 'Плащането е отхвърлено';

  @override
  String get resultDeclinedBody => 'Банката не потвърди плащането.';

  @override
  String get resultUnconfirmedTitle => 'Проверяваме плащането';

  @override
  String get resultUnconfirmedBody =>
      'Банката все още не е отговорила. Не плащайте отново — ще ви уведомим веднага щом плащането бъде потвърдено.';

  @override
  String get resultTryAgain => 'Опитай отново';

  @override
  String get resultBackToStore => 'Обратно към магазина';

  @override
  String get resultDone => 'Готово';

  @override
  String get devTitle => 'Настройки за разработчици';

  @override
  String get devGateway => 'Шлюз';

  @override
  String get devBaseUrl => 'Base URL';

  @override
  String get devLogin => 'Потребителско име на търговеца';

  @override
  String get devPassword => 'Парола';

  @override
  String get devApiV1 => 'API V1';

  @override
  String get devApiV2 => 'API V2';

  @override
  String get devApiVersion => 'API на търговеца';

  @override
  String get devApiV1Hint => 'Поръчките се регистрират чрез register.do с потребителското име и паролата на търговеца.';

  @override
  String get devApiV2Hint => 'Сесиите се създават чрез POST /api2/sessions с API ключа.';

  @override
  String get devApiKey => 'API ключ';

  @override
  String get devOrderType => 'Поръчка';

  @override
  String get devOrderPurchase => 'Плащане';

  @override
  String get devOrderAft => 'AFT превод';

  @override
  String get devOrderAftHint =>
      'Преводите се регистрират чрез registerP2P.do (еднопосочен AFT): прозорецът иска данните на изпращача и получателя.';

  @override
  String get devCredentialsNote =>
      'Съхраняват се само на това устройство. Във вашето приложение поръчката се регистрира от вашия бекенд.';

  @override
  String get devSdkLook => 'Външен вид на SDK';

  @override
  String get devSheetTheme => 'Тема на панела';

  @override
  String get devThemeCover => 'Като на магазина';

  @override
  String get devThemeLight => 'Светла';

  @override
  String get devThemeDark => 'Тъмна';

  @override
  String get devThemeSystem => 'Системна';

  @override
  String get devLanguage => 'Език';

  @override
  String get devLanguageSystem => 'Системен';

  @override
  String get devWallets => 'Google Pay';

  @override
  String get devGpayGateway => 'Gateway';

  @override
  String get devGpayMerchant => 'Merchant ID';

  @override
  String get devGpayFromGateway => 'От платежния шлюз';

  @override
  String get devMore => 'Още';

  @override
  String get devAdvanced => 'Advanced';

  @override
  String get devAdvancedSub => 'Поток от състояния, SSL pinning';

  @override
  String get devLastOrder => 'Последна поръчка';

  @override
  String get devCopy => 'Копирай';

  @override
  String get devNone => '—';

  @override
  String get resultSuccessFurniture => 'Благодарим ви за поръчката';

  @override
  String get resultSuccessFurnitureBody => 'Ще се свържем с вас, за да уговорим доставката.';

  @override
  String get resultSuccessCoffee => 'Поръчката ви се приготвя';

  @override
  String get resultSuccessCoffeeBody => 'Покажете този номер на баристата.';

  @override
  String get resultSuccessTickets => 'Ще бъдете там!';

  @override
  String get resultSuccessTicketsBody => 'Билетите са във вашия профил.';

  @override
  String get furnitureCollection => 'Есенна\nколекция';

  @override
  String get furnitureAll => 'Всички';

  @override
  String get furnitureChairs => 'Столове';

  @override
  String get furnitureSofas => 'Дивани';

  @override
  String get furnitureLight => 'Осветление';

  @override
  String get furnitureDelivery => 'Безплатна доставка за 3–5 дни. 30 дни за връщане.';

  @override
  String get furnitureDeliveryLabel => 'Доставка';

  @override
  String get furnitureDeliveryFree => 'Безплатно';

  @override
  String get furnitureColours => 'Цвят';

  @override
  String get coffeeLocation => 'King St.';

  @override
  String get coffeeAgain => 'Като миналия път';

  @override
  String get coffeeRepeat => 'Поръчай отново';

  @override
  String get coffeeTabCoffee => 'Кафе';

  @override
  String get coffeeTabCold => 'Студени';

  @override
  String get coffeeTabBakery => 'Печива';

  @override
  String get coffeeOrderTitle => 'Вашата поръчка';

  @override
  String get coffeePickup => 'Вземане';

  @override
  String get coffeePickupValue => 'King St. · след 10 мин';

  @override
  String coffeeAddItem(String item) {
    return 'Добави $item';
  }

  @override
  String get coffeeOrderNumberHint => 'Готова след около 6 минути.';

  @override
  String get ticketsLive => 'Live in Hall A';

  @override
  String get ticketsTitle => 'NORTHERN\nLIGHTS';

  @override
  String get ticketsDate => 'Сб, 14 ноември · 20:00 · Hall A';

  @override
  String get ticketsSection => 'Билети';

  @override
  String ticketsToPayment(String amount) {
    return 'Към плащане · $amount';
  }

  @override
  String get ticketsCheckout => 'Плащане';

  @override
  String ticketsEventLine(String category) {
    return '14 ноем. · $category';
  }

  @override
  String ticketsHold(String time) {
    return 'Пазим местата още $time';
  }

  @override
  String get ticketsHoldExpired => 'Резервацията изтече. Изберете местата отново.';

  @override
  String get ticketsSeats => 'Билети';

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
  String get productVelvetSofaSub => 'Изумрудено кадифе, дъбови крака';

  @override
  String get productCloudChairSub => 'Млечнобяло букле';

  @override
  String get productOnoChairSub => 'Формована седалка, крака от бук';

  @override
  String get productBirchStoolSub => 'Масивна бреза';

  @override
  String get productArcLampSub => 'Матова графитена стомана';

  @override
  String get productLumeArmchairSub => 'Вълна в горчичен цвят';

  @override
  String get productFlatWhiteSub => 'Двойно еспресо, мляко на пара';

  @override
  String get productCappuccinoSub => 'Класическо, с млечна пяна';

  @override
  String get productEspressoSub => 'Кафе от един произход';

  @override
  String get productIcedLatteSub => 'Студено мляко, лед';

  @override
  String get productCroissantSub => 'Изпечен тази сутрин';

  @override
  String get ticketFloor => 'Партер';

  @override
  String get ticketFloorSub => 'Правостоящи места';

  @override
  String get ticketBalcony => 'Балкон';

  @override
  String get ticketBalconySub => 'Ред 2 · седящи места';

  @override
  String get ticketFee => 'Такса за обслужване';

  @override
  String get startSectionPayment => 'SDK Payment';

  @override
  String get startSectionPaymentSub =>
      'Готов панел за плащане във вашето приложение — всеки магазин има всички сценарии';

  @override
  String get startSectionCore => 'SDK Core';

  @override
  String get startSectionCoreSub => 'Криптиране на картата за собствена форма (PCI DSS)';

  @override
  String get coreTileTitle => 'Собствена форма за карта';

  @override
  String get coreTileSub => 'Данните за картата се превръщат в токен за вашия сървър';

  @override
  String get coreTitle => 'SDK Core';

  @override
  String get coreDoneSubtitle => 'Токенът е готов';

  @override
  String get coreVariantNew => 'Нова карта';

  @override
  String get coreVariantInstant => 'Без поръчка';

  @override
  String get coreVariantStored => 'Запазена карта';

  @override
  String get coreHintNew => 'Плащане на регистрирана от вас поръчка: данните за картата и нейният mdOrder.';

  @override
  String get coreHintInstant => 'Още няма поръчка: сървърът ви я създава и я плаща с една заявка.';

  @override
  String get coreHintStored => 'Плащане на поръчка със запазена карта: нейният bindingId вместо данните за картата.';

  @override
  String get coreCardGroup => 'Данни за картата';

  @override
  String get corePan => 'Номер на картата';

  @override
  String get coreExpiryPlaceholder => 'ММ/ГГ';

  @override
  String get coreExpiry => 'Валидност';

  @override
  String get coreCvc => 'CVC/CVV';

  @override
  String get coreCvcOptional => 'CVC/CVV (по избор)';

  @override
  String get coreHolder => 'Име върху картата (по избор)';

  @override
  String get coreBinding => 'bindingId';

  @override
  String get coreMdOrder => 'mdOrder';

  @override
  String get coreMdOrderHint => 'От вашия бекенд';

  @override
  String get corePubKey => 'Публичен ключ (PEM)';

  @override
  String get corePaste => 'Постави';

  @override
  String get coreGenerate => 'Получи токен';

  @override
  String get coreErrorRequired => 'Задължително поле';

  @override
  String get coreErrorInvalid => 'Невалидна стойност';

  @override
  String get coreErrorFormat => 'Грешен формат';

  @override
  String get coreTokenOk => 'Токенът е получен';

  @override
  String get coreCopy => 'Копирай';

  @override
  String get coreCopied => 'Копирано';

  @override
  String get coreStepSend => 'Изпратете токена на вашия сървър';

  @override
  String get coreStepGateway => 'Сървърът ви го предава на шлюза като seToken';

  @override
  String get coreStepPay => 'Шлюзът обработва плащането';

  @override
  String get coreAgain => 'Отново';

  @override
  String get investName => 'Northwind Invest';

  @override
  String get startSectionPaymentAftSub => 'AFT: захранване на сметка от карта';

  @override
  String get investTileSub => 'Захранете инвестиционната си сметка от карта и вижте как расте салдото';

  @override
  String investAccount(String account) {
    return 'Инвестиционна сметка •• $account';
  }

  @override
  String get investPortfolioValue => 'Стойност на портфейла';

  @override
  String get investChangeMonth => 'този месец';

  @override
  String get investPeriod1w => '1С';

  @override
  String get investPeriod1m => '1М';

  @override
  String get investPeriod3m => '3М';

  @override
  String get investPeriod1y => '1Г';

  @override
  String get investPeriodAll => 'Всички';

  @override
  String get investTopUp => 'Захрани';

  @override
  String get investInvest => 'Инвестирай';

  @override
  String get investFreeCash => 'Свободни средства';

  @override
  String get investFreeCashSub => 'Готови за инвестиране';

  @override
  String investFreeCashJustNow(String amount) {
    return '+$amount току-що';
  }

  @override
  String get investMyAssets => 'Моите активи';

  @override
  String investUnits(String units, String price) {
    return '$units бр. · $price';
  }

  @override
  String get investRecent => 'Последни операции';

  @override
  String get investOpTopUp => 'Захранване';

  @override
  String investOpBuy(String asset) {
    return 'Покупка на $asset';
  }

  @override
  String get investAssetGlb => 'Global ETF';

  @override
  String get investAssetTch => 'Tech Leaders';

  @override
  String get investNotifications => 'Известия';

  @override
  String get topupTitle => 'Захранване';

  @override
  String get topupTo => 'Към';

  @override
  String get topupAvailable => 'Налични';

  @override
  String get topupAmount => 'Сума';

  @override
  String topupLimits(String min, String max) {
    return 'от $min до $max';
  }

  @override
  String topupContinue(String amount) {
    return 'Напред · $amount';
  }

  @override
  String get topupCreating => 'Създаваме превода…';

  @override
  String get topupFailedTitle => 'Преводът не мина';

  @override
  String get topupFailedBody => 'Банката на картата отказа операцията. Не са удържани пари; опитайте с друга карта.';

  @override
  String topupFailedDetail(String message) {
    return 'Шлюз: $message';
  }

  @override
  String get topupUnknownTitle => 'Неизвестен статус';

  @override
  String get topupUnknownBody => 'Проверете поръчката в портала на търговеца, преди да опитате отново.';

  @override
  String get topupTryAgain => 'Опитай отново';

  @override
  String doneCredited(String account) {
    return 'заверени по инвестиционна сметка •• $account';
  }

  @override
  String get doneOperation => 'Операция';

  @override
  String get doneOperationValue => 'AFT · към собствена сметка';

  @override
  String get doneFreeCash => 'Свободни средства';

  @override
  String get doneOrder => 'Поръчка';

  @override
  String get doneTime => 'Час';

  @override
  String doneInvest(String amount) {
    return 'Инвестирай $amount';
  }

  @override
  String get doneToPortfolio => 'Към портфейла';

  @override
  String get devInvestReset => 'Нулирай демо сметката на Northwind';

  @override
  String get devInvestResetDone => 'Демо сметката е нулирана';

  @override
  String get scenarioWalletApple => 'Apple Pay';

  @override
  String get devApplePay => 'Apple Pay';
}
