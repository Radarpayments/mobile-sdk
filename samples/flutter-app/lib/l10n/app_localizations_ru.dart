// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Russian (`ru`).
class AppLocalizationsRu extends AppLocalizations {
  AppLocalizationsRu([String locale = 'ru']) : super(locale);

  @override
  String get startTitle => 'Демо оплаты';

  @override
  String get startSettings => 'Для разработчиков';

  @override
  String get coverFurnitureName => 'Hearth & Co';

  @override
  String get coverCoffeeName => 'Roast Lane';

  @override
  String get coverTicketsName => 'Stage Pass';

  @override
  String get coverFurnitureTagline => 'Мебель · каталог и корзина';

  @override
  String get coverCoffeeTagline => 'Кофейня · заказ навынос';

  @override
  String get coverTicketsTagline => 'Билеты · концерты и шоу';

  @override
  String get storeBack => 'Назад';

  @override
  String get storeCart => 'Корзина';

  @override
  String storeCartCount(int count) {
    return 'Корзина · $count';
  }

  @override
  String get storeItems => 'Товары';

  @override
  String get storeTotal => 'Итого';

  @override
  String storePay(String amount) {
    return 'Оплатить $amount';
  }

  @override
  String get storeIncrease => 'Больше';

  @override
  String get storeDecrease => 'Меньше';

  @override
  String storeAddToCart(String price) {
    return 'В корзину · $price';
  }

  @override
  String get storeAdd => 'Добавить';

  @override
  String get storeRegistering => 'Создаём заказ…';

  @override
  String get storeErrorCredentials => 'Не заданы логин и пароль мерчанта.';

  @override
  String get storeErrorApiKey => 'Не задан API-ключ.';

  @override
  String storeErrorFailed(String message) {
    return 'Заказ не создан: $message';
  }

  @override
  String get storeOpenSettings => 'Открыть настройки';

  @override
  String scenarioRow(String buyer) {
    return 'Сценарий: $buyer';
  }

  @override
  String get scenarioTitle => 'Сценарий оплаты';

  @override
  String get scenarioBuyer => 'Покупатель';

  @override
  String get scenarioGuest => 'Гость';

  @override
  String get scenarioWithCards => 'С картами';

  @override
  String get scenarioWithCardsHint => 'Карты появятся после оплаты с «Сохранить карту»';

  @override
  String get scenarioReset => 'Сбросить';

  @override
  String get scenarioPayment => 'Оплата';

  @override
  String get scenarioWallet => 'Google Pay';

  @override
  String get scenarioScanner => 'Сканер карты';

  @override
  String get scenarioSdkResult => 'Экран результата SDK';

  @override
  String resultPaidAmount(String amount) {
    return 'Оплачено $amount';
  }

  @override
  String get resultPaymentId => 'Платёж';

  @override
  String get resultAmount => 'Сумма';

  @override
  String get resultDeclinedTitle => 'Платёж отклонён';

  @override
  String get resultDeclinedBody => 'Банк не подтвердил оплату.';

  @override
  String get resultUnconfirmedTitle => 'Проверяем оплату';

  @override
  String get resultUnconfirmedBody =>
      'Банк ещё не ответил. Не оплачивайте повторно — мы сообщим, как только платёж подтвердится.';

  @override
  String get resultTryAgain => 'Попробовать снова';

  @override
  String get resultBackToStore => 'Вернуться в магазин';

  @override
  String get resultDone => 'Готово';

  @override
  String get devTitle => 'Для разработчиков';

  @override
  String get devGateway => 'Шлюз';

  @override
  String get devBaseUrl => 'Base URL';

  @override
  String get devLogin => 'Логин мерчанта';

  @override
  String get devPassword => 'Пароль';

  @override
  String get devApiV1 => 'API V1';

  @override
  String get devApiV2 => 'API V2';

  @override
  String get devApiVersion => 'API мерчанта';

  @override
  String get devApiV1Hint => 'Заказы регистрируются через register.do по логину и паролю мерчанта.';

  @override
  String get devApiV2Hint => 'Сессии создаются через POST /api2/sessions по API-ключу.';

  @override
  String get devApiKey => 'API-ключ';

  @override
  String get devOrderType => 'Заказ';

  @override
  String get devOrderPurchase => 'Оплата';

  @override
  String get devOrderAft => 'AFT-перевод';

  @override
  String get devOrderAftHint =>
      'Переводы регистрируются через registerP2P.do (AFT в одну сторону): лист попросит данные отправителя и получателя.';

  @override
  String get devCredentialsNote =>
      'Хранятся только на этом устройстве. В вашем приложении заказ регистрирует ваш бэкенд.';

  @override
  String get devSdkLook => 'Оформление SDK';

  @override
  String get devSheetTheme => 'Тема шторки';

  @override
  String get devThemeCover => 'Как у магазина';

  @override
  String get devThemeLight => 'Светлая';

  @override
  String get devThemeDark => 'Тёмная';

  @override
  String get devThemeSystem => 'Системная';

  @override
  String get devLanguage => 'Язык';

  @override
  String get devLanguageSystem => 'Системный';

  @override
  String get devWallets => 'Google Pay';

  @override
  String get devGpayGateway => 'Gateway';

  @override
  String get devGpayMerchant => 'Merchant ID';

  @override
  String get devGpayFromGateway => 'Из платёжного шлюза';

  @override
  String get devMore => 'Ещё';

  @override
  String get devAdvanced => 'Advanced';

  @override
  String get devAdvancedSub => 'Поток состояний, SSL pinning';

  @override
  String get devLastOrder => 'Последний заказ';

  @override
  String get devCopy => 'Копировать';

  @override
  String get devNone => '—';

  @override
  String get resultSuccessFurniture => 'Спасибо за заказ';

  @override
  String get resultSuccessFurnitureBody => 'Мы свяжемся, чтобы согласовать доставку.';

  @override
  String get resultSuccessCoffee => 'Заказ готовится';

  @override
  String get resultSuccessCoffeeBody => 'Покажите номер бариста.';

  @override
  String get resultSuccessTickets => 'Вы идёте!';

  @override
  String get resultSuccessTicketsBody => 'Билеты уже в вашем аккаунте.';

  @override
  String get furnitureCollection => 'Осенняя\nколлекция';

  @override
  String get furnitureAll => 'Всё';

  @override
  String get furnitureChairs => 'Кресла и стулья';

  @override
  String get furnitureSofas => 'Диваны';

  @override
  String get furnitureLight => 'Свет';

  @override
  String get furnitureDelivery => 'Бесплатная доставка за 3–5 дней. 30 дней на возврат.';

  @override
  String get furnitureDeliveryLabel => 'Доставка';

  @override
  String get furnitureDeliveryFree => 'Бесплатно';

  @override
  String get furnitureColours => 'Цвет';

  @override
  String get coffeeLocation => 'King St.';

  @override
  String get coffeeAgain => 'Как в прошлый раз';

  @override
  String get coffeeRepeat => 'Повторить';

  @override
  String get coffeeTabCoffee => 'Кофе';

  @override
  String get coffeeTabCold => 'Холодное';

  @override
  String get coffeeTabBakery => 'Выпечка';

  @override
  String get coffeeOrderTitle => 'Ваш заказ';

  @override
  String get coffeePickup => 'Забрать';

  @override
  String get coffeePickupValue => 'King St. · через 10 мин';

  @override
  String coffeeAddItem(String item) {
    return 'Добавить $item';
  }

  @override
  String get coffeeOrderNumberHint => 'Будет готов примерно через 6 минут.';

  @override
  String get ticketsLive => 'Live in Hall A';

  @override
  String get ticketsTitle => 'NORTHERN\nLIGHTS';

  @override
  String get ticketsDate => 'Сб, 14 ноября · 20:00 · Hall A';

  @override
  String get ticketsSection => 'Билеты';

  @override
  String ticketsToPayment(String amount) {
    return 'К оплате · $amount';
  }

  @override
  String get ticketsCheckout => 'Оформление';

  @override
  String ticketsEventLine(String category) {
    return '14 нояб · $category';
  }

  @override
  String ticketsHold(String time) {
    return 'Места держим ещё $time';
  }

  @override
  String get ticketsHoldExpired => 'Бронь истекла. Выберите места заново.';

  @override
  String get ticketsSeats => 'Билеты';

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
  String get productVelvetSofaSub => 'Изумрудный велюр, дубовые ножки';

  @override
  String get productCloudChairSub => 'Молочный букле';

  @override
  String get productOnoChairSub => 'Литой пластик, ножки из бука';

  @override
  String get productBirchStoolSub => 'Массив берёзы';

  @override
  String get productArcLampSub => 'Матовая графитовая сталь';

  @override
  String get productLumeArmchairSub => 'Горчичная шерсть';

  @override
  String get productFlatWhiteSub => 'Двойной эспрессо, молоко';

  @override
  String get productCappuccinoSub => 'Классический, с пенкой';

  @override
  String get productEspressoSub => 'Моносорт';

  @override
  String get productIcedLatteSub => 'Холодное молоко, лёд';

  @override
  String get productCroissantSub => 'Испекли сегодня утром';

  @override
  String get ticketFloor => 'Танцпол';

  @override
  String get ticketFloorSub => 'Стоячие места';

  @override
  String get ticketBalcony => 'Балкон';

  @override
  String get ticketBalconySub => 'Ряд 2 · сидячие';

  @override
  String get ticketFee => 'Сервисный сбор';

  @override
  String get startSectionPayment => 'SDK Payment';

  @override
  String get startSectionPaymentSub => 'Готовая шторка оплаты в вашем приложении — в каждом магазине все сценарии';

  @override
  String get startSectionCore => 'SDK Core';

  @override
  String get startSectionCoreSub => 'Шифрование карты для своей формы (PCI DSS)';

  @override
  String get coreTileTitle => 'Своя форма карты';

  @override
  String get coreTileSub => 'Данные карты превращаются в токен для вашего сервера';

  @override
  String get coreTitle => 'SDK Core';

  @override
  String get coreDoneSubtitle => 'Токен готов';

  @override
  String get coreVariantNew => 'Новая карта';

  @override
  String get coreVariantInstant => 'Без заказа';

  @override
  String get coreVariantStored => 'Сохранённая';

  @override
  String get coreHintNew => 'Оплата зарегистрированного заказа: данные карты и его mdOrder.';

  @override
  String get coreHintInstant => 'Заказа ещё нет: сервер создаёт его и оплачивает одним запросом.';

  @override
  String get coreHintStored => 'Оплата заказа сохранённой картой: bindingId вместо данных карты.';

  @override
  String get coreCardGroup => 'Данные карты';

  @override
  String get corePan => 'Номер карты';

  @override
  String get coreExpiryPlaceholder => 'ММ/ГГ';

  @override
  String get coreExpiry => 'Срок';

  @override
  String get coreCvc => 'CVC/CVV';

  @override
  String get coreCvcOptional => 'CVC/CVV (необязательно)';

  @override
  String get coreHolder => 'Имя на карте (необязательно)';

  @override
  String get coreBinding => 'bindingId';

  @override
  String get coreMdOrder => 'mdOrder';

  @override
  String get coreMdOrderHint => 'Из вашего бэкенда';

  @override
  String get corePubKey => 'Публичный ключ (PEM)';

  @override
  String get corePaste => 'Вставить';

  @override
  String get coreGenerate => 'Получить токен';

  @override
  String get coreErrorRequired => 'Обязательное поле';

  @override
  String get coreErrorInvalid => 'Неверное значение';

  @override
  String get coreErrorFormat => 'Неверный формат';

  @override
  String get coreTokenOk => 'Токен получен';

  @override
  String get coreCopy => 'Копировать';

  @override
  String get coreCopied => 'Скопировано';

  @override
  String get coreStepSend => 'Отправьте токен на свой сервер';

  @override
  String get coreStepGateway => 'Сервер передаёт его в шлюз в параметре seToken';

  @override
  String get coreStepPay => 'Шлюз проводит оплату';

  @override
  String get coreAgain => 'Ещё раз';

  @override
  String get investName => 'Northwind Invest';

  @override
  String get startSectionPaymentAftSub => 'AFT: пополнение счёта переводом с карты';

  @override
  String get investTileSub => 'Пополните инвест-счёт с карты и посмотрите, как растёт баланс';

  @override
  String investAccount(String account) {
    return 'Инвест-счёт •• $account';
  }

  @override
  String get investPortfolioValue => 'Стоимость портфеля';

  @override
  String get investChangeMonth => 'за месяц';

  @override
  String get investPeriod1w => '1Н';

  @override
  String get investPeriod1m => '1М';

  @override
  String get investPeriod3m => '3М';

  @override
  String get investPeriod1y => '1Г';

  @override
  String get investPeriodAll => 'Всё';

  @override
  String get investTopUp => 'Пополнить';

  @override
  String get investInvest => 'Инвестировать';

  @override
  String get investFreeCash => 'Свободные деньги';

  @override
  String get investFreeCashSub => 'Готовы к инвестициям';

  @override
  String investFreeCashJustNow(String amount) {
    return '+$amount только что';
  }

  @override
  String get investMyAssets => 'Мои активы';

  @override
  String investUnits(String units, String price) {
    return '$units шт · $price';
  }

  @override
  String get investRecent => 'Последние операции';

  @override
  String get investOpTopUp => 'Пополнение';

  @override
  String investOpBuy(String asset) {
    return 'Покупка $asset';
  }

  @override
  String get investAssetGlb => 'Global ETF';

  @override
  String get investAssetTch => 'Tech Leaders';

  @override
  String get investNotifications => 'Уведомления';

  @override
  String get topupTitle => 'Пополнение';

  @override
  String get topupTo => 'Куда';

  @override
  String get topupAvailable => 'Свободно';

  @override
  String get topupAmount => 'Сумма';

  @override
  String topupLimits(String min, String max) {
    return 'от $min до $max';
  }

  @override
  String topupContinue(String amount) {
    return 'Продолжить · $amount';
  }

  @override
  String get topupCreating => 'Создаём перевод…';

  @override
  String get topupFailedTitle => 'Перевод не прошёл';

  @override
  String get topupFailedBody => 'Банк карты отклонил операцию. Деньги не списаны, попробуйте другую карту.';

  @override
  String topupFailedDetail(String message) {
    return 'Шлюз: $message';
  }

  @override
  String get topupUnknownTitle => 'Статус неизвестен';

  @override
  String get topupUnknownBody => 'Проверьте заказ в личном кабинете мерчанта, прежде чем пробовать снова.';

  @override
  String get topupTryAgain => 'Попробовать снова';

  @override
  String get topupAddCard => 'Добавить карту';

  @override
  String get topupAddingCard => 'Добавляем карту…';

  @override
  String get topupCardAddedTitle => 'Карта добавлена';

  @override
  String get topupCardAddedBody => 'Выберите её в окне оплаты при следующем пополнении. Деньги не списаны.';

  @override
  String get topupCardFailedTitle => 'Карта не добавлена';

  @override
  String get topupCardFailedBody => 'Банк карты её не подтвердил. Деньги не списаны, попробуйте другую карту.';

  @override
  String get topupCardUnknownBody =>
      'Банк ещё не подтвердил карту. Если подтвердит, она появится при следующем пополнении.';

  @override
  String doneCredited(String account) {
    return 'зачислено на инвест-счёт •• $account';
  }

  @override
  String get doneOperation => 'Операция';

  @override
  String get doneOperationValue => 'AFT · на свой счёт';

  @override
  String get doneFreeCash => 'Свободные деньги';

  @override
  String get doneOrder => 'Заказ';

  @override
  String get doneTime => 'Время';

  @override
  String doneInvest(String amount) {
    return 'Инвестировать $amount';
  }

  @override
  String get doneToPortfolio => 'В портфель';

  @override
  String get devInvestReset => 'Сбросить демо-счёт Northwind';

  @override
  String get devInvestResetDone => 'Демо-счёт сброшен';

  @override
  String get scenarioWalletApple => 'Apple Pay';

  @override
  String get devApplePay => 'Apple Pay';
}
