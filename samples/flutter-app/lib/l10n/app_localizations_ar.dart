// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Arabic (`ar`).
class AppLocalizationsAr extends AppLocalizations {
  AppLocalizationsAr([String locale = 'ar']) : super(locale);

  @override
  String get startTitle => 'عرض توضيحي للدفع';

  @override
  String get startSettings => 'إعدادات المطوّر';

  @override
  String get coverFurnitureName => 'Hearth & Co';

  @override
  String get coverCoffeeName => 'Roast Lane';

  @override
  String get coverTicketsName => 'Stage Pass';

  @override
  String get coverFurnitureTagline => 'أثاث · الكتالوج والسلة';

  @override
  String get coverCoffeeTagline => 'مقهى · اطلب مسبقًا';

  @override
  String get coverTicketsTagline => 'تذاكر · حفلات وعروض';

  @override
  String get storeBack => 'رجوع';

  @override
  String get storeCart => 'السلة';

  @override
  String storeCartCount(int count) {
    return 'السلة · $count';
  }

  @override
  String get storeItems => 'المنتجات';

  @override
  String get storeTotal => 'الإجمالي';

  @override
  String storePay(String amount) {
    return 'ادفع $amount';
  }

  @override
  String get storeIncrease => 'زيادة';

  @override
  String get storeDecrease => 'إنقاص';

  @override
  String storeAddToCart(String price) {
    return 'أضف إلى السلة · $price';
  }

  @override
  String get storeAdd => 'إضافة';

  @override
  String get storeRegistering => 'جارٍ إنشاء الطلب…';

  @override
  String get storeErrorCredentials => 'لم يتم تعيين اسم مستخدم التاجر وكلمة المرور.';

  @override
  String get storeErrorApiKey => 'لم يتم تعيين مفتاح API.';

  @override
  String storeErrorFailed(String message) {
    return 'لم يتم إنشاء الطلب: $message';
  }

  @override
  String get storeOpenSettings => 'فتح الإعدادات';

  @override
  String scenarioRow(String buyer) {
    return 'السيناريو: $buyer';
  }

  @override
  String get scenarioTitle => 'سيناريو الدفع';

  @override
  String get scenarioBuyer => 'المشتري';

  @override
  String get scenarioGuest => 'ضيف';

  @override
  String get scenarioWithCards => 'مع بطاقات';

  @override
  String get scenarioWithCardsHint => 'تظهر البطاقات بعد الدفع مع تفعيل «حفظ البطاقة»';

  @override
  String get scenarioReset => 'إعادة تعيين';

  @override
  String get scenarioPayment => 'الدفع';

  @override
  String get scenarioWallet => 'Google Pay';

  @override
  String get scenarioScanner => 'ماسح البطاقة';

  @override
  String get scenarioSdkResult => 'شاشة نتيجة SDK';

  @override
  String resultPaidAmount(String amount) {
    return 'تم دفع $amount';
  }

  @override
  String get resultPaymentId => 'الدفعة';

  @override
  String get resultAmount => 'المبلغ';

  @override
  String get resultDeclinedTitle => 'تم رفض الدفع';

  @override
  String get resultDeclinedBody => 'لم يؤكد البنك عملية الدفع.';

  @override
  String get resultUnconfirmedTitle => 'نتحقق من الدفع';

  @override
  String get resultUnconfirmedBody => 'لم يردّ البنك بعد. لا تدفع مرة أخرى، وسنبلغك فور تأكيد الدفع.';

  @override
  String get resultTryAgain => 'حاول مرة أخرى';

  @override
  String get resultBackToStore => 'العودة إلى المتجر';

  @override
  String get resultDone => 'تم';

  @override
  String get devTitle => 'إعدادات المطوّر';

  @override
  String get devGateway => 'بوابة الدفع';

  @override
  String get devBaseUrl => 'Base URL';

  @override
  String get devLogin => 'اسم مستخدم التاجر';

  @override
  String get devPassword => 'كلمة المرور';

  @override
  String get devApiV1 => 'API V1';

  @override
  String get devApiV2 => 'API V2';

  @override
  String get devApiVersion => 'واجهة API للتاجر';

  @override
  String get devApiV1Hint => 'تُسجَّل الطلبات عبر register.do باستخدام اسم مستخدم التاجر وكلمة المرور.';

  @override
  String get devApiV2Hint => 'تُنشأ الجلسات عبر POST /api2/sessions باستخدام مفتاح API.';

  @override
  String get devApiKey => 'مفتاح API';

  @override
  String get devOrderType => 'الطلب';

  @override
  String get devOrderPurchase => 'دفع';

  @override
  String get devOrderAft => 'تحويل AFT';

  @override
  String get devOrderAftHint =>
      'تُسجَّل التحويلات عبر registerP2P.do (AFT باتجاه واحد): تطلب النافذة بيانات المرسل والمستلم.';

  @override
  String get devCredentialsNote => 'تُحفظ على هذا الجهاز فقط. في تطبيقك، يسجّل الخادم الخلفي لديك الطلب.';

  @override
  String get devSdkLook => 'مظهر SDK';

  @override
  String get devSheetTheme => 'سمة واجهة الدفع';

  @override
  String get devThemeCover => 'مثل المتجر';

  @override
  String get devThemeLight => 'فاتح';

  @override
  String get devThemeDark => 'داكن';

  @override
  String get devThemeSystem => 'حسب النظام';

  @override
  String get devLanguage => 'اللغة';

  @override
  String get devLanguageSystem => 'حسب النظام';

  @override
  String get devWallets => 'Google Pay';

  @override
  String get devGpayGateway => 'Gateway';

  @override
  String get devGpayMerchant => 'Merchant ID';

  @override
  String get devGpayFromGateway => 'من بوابة الدفع';

  @override
  String get devMore => 'المزيد';

  @override
  String get devAdvanced => 'Advanced';

  @override
  String get devAdvancedSub => 'تدفق الحالات، SSL pinning';

  @override
  String get devLastOrder => 'آخر طلب';

  @override
  String get devCopy => 'نسخ';

  @override
  String get devCredentials => 'بيانات الاعتماد';

  @override
  String get devShowSecret => 'إظهار';

  @override
  String get devHideSecret => 'إخفاء';

  @override
  String get devCopied => 'تم النسخ';

  @override
  String get devBaseUrlError => 'يجب أن يبدأ العنوان بـ https://';

  @override
  String get devNone => '—';

  @override
  String get resultSuccessFurniture => 'شكرًا لطلبك';

  @override
  String get resultSuccessFurnitureBody => 'سنتواصل معك لترتيب التوصيل.';

  @override
  String get resultSuccessCoffee => 'جارٍ تحضير طلبك';

  @override
  String get resultSuccessCoffeeBody => 'اعرض هذا الرقم على الباريستا.';

  @override
  String get resultSuccessTickets => 'حجزت مكانك!';

  @override
  String get resultSuccessTicketsBody => 'التذاكر موجودة في حسابك.';

  @override
  String get furnitureCollection => 'تشكيلة\nالخريف';

  @override
  String get furnitureAll => 'الكل';

  @override
  String get furnitureChairs => 'الكراسي';

  @override
  String get furnitureSofas => 'الأرائك';

  @override
  String get furnitureLight => 'الإضاءة';

  @override
  String get furnitureDelivery => 'توصيل مجاني خلال 3–5 أيام. إرجاع خلال 30 يومًا.';

  @override
  String get furnitureDeliveryLabel => 'التوصيل';

  @override
  String get furnitureDeliveryFree => 'مجاني';

  @override
  String get furnitureColours => 'اللون';

  @override
  String get coffeeLocation => 'King St.';

  @override
  String get coffeeAgain => 'مثل المرة السابقة';

  @override
  String get coffeeRepeat => 'اطلب مجددًا';

  @override
  String get coffeeTabCoffee => 'القهوة';

  @override
  String get coffeeTabCold => 'مشروبات باردة';

  @override
  String get coffeeTabBakery => 'المخبوزات';

  @override
  String get coffeeOrderTitle => 'طلبك';

  @override
  String get coffeePickup => 'الاستلام';

  @override
  String get coffeePickupValue => 'King St.‎ · خلال 10 دقائق';

  @override
  String coffeeAddItem(String item) {
    return 'أضف $item';
  }

  @override
  String get coffeeOrderNumberHint => 'سيكون جاهزًا خلال 6 دقائق تقريبًا.';

  @override
  String get ticketsLive => 'Live in Hall A';

  @override
  String get ticketsTitle => 'NORTHERN\nLIGHTS';

  @override
  String get ticketsDate => 'السبت، 14 نوفمبر · 20:00 · Hall A';

  @override
  String get ticketsSection => 'التذاكر';

  @override
  String ticketsToPayment(String amount) {
    return 'المتابعة للدفع · $amount';
  }

  @override
  String get ticketsCheckout => 'إتمام الشراء';

  @override
  String ticketsEventLine(String category) {
    return '14 نوفمبر · $category';
  }

  @override
  String ticketsHold(String time) {
    return 'المقاعد محجوزة لمدة $time';
  }

  @override
  String get ticketsHoldExpired => 'انتهت مدة الحجز. اختر مقاعدك مرة أخرى.';

  @override
  String get ticketsSeats => 'التذاكر';

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
  String get productVelvetSofaSub => 'مخمل زمردي، أرجل من خشب البلوط';

  @override
  String get productCloudChairSub => 'قماش بوكليه أبيض حليبي';

  @override
  String get productOnoChairSub => 'هيكل مقولب، أرجل من خشب الزان';

  @override
  String get productBirchStoolSub => 'خشب البتولا الصلب';

  @override
  String get productArcLampSub => 'فولاذ مطفأ بلون الغرافيت';

  @override
  String get productLumeArmchairSub => 'صوف بلون الخردل';

  @override
  String get productFlatWhiteSub => 'جرعة إسبريسو مزدوجة، حليب مبخّر';

  @override
  String get productCappuccinoSub => 'كلاسيكي، مع رغوة';

  @override
  String get productEspressoSub => 'بنّ أحادي المصدر';

  @override
  String get productIcedLatteSub => 'حليب بارد، ثلج';

  @override
  String get productCroissantSub => 'مخبوز هذا الصباح';

  @override
  String get ticketFloor => 'منطقة الوقوف';

  @override
  String get ticketFloorSub => 'دخول عام';

  @override
  String get ticketBalcony => 'الشرفة';

  @override
  String get ticketBalconySub => 'الصف 2 · مقاعد جلوس';

  @override
  String get ticketFee => 'رسوم الخدمة';

  @override
  String get startSectionPayment => 'SDK Payment';

  @override
  String get startSectionPaymentSub => 'واجهة دفع جاهزة داخل تطبيقك — كل متجر يتضمن جميع السيناريوهات';

  @override
  String get startSectionCore => 'SDK Core';

  @override
  String get startSectionCoreSub => 'تشفير البطاقة لنموذجك الخاص (PCI DSS)';

  @override
  String get coreTileTitle => 'نموذج البطاقة الخاص بك';

  @override
  String get coreTileSub => 'تتحوّل بيانات البطاقة إلى رمز مميز لخادمك';

  @override
  String get coreTitle => 'SDK Core';

  @override
  String get coreDoneSubtitle => 'الرمز المميز جاهز';

  @override
  String get coreVariantNew => 'بطاقة جديدة';

  @override
  String get coreVariantInstant => 'بدون طلب';

  @override
  String get coreVariantStored => 'بطاقة محفوظة';

  @override
  String get coreHintNew => 'يدفع طلبًا سجّلته: بيانات البطاقة مع mdOrder الخاص به.';

  @override
  String get coreHintInstant => 'لا يوجد طلب بعد: ينشئه خادمك ويدفعه في استدعاء واحد.';

  @override
  String get coreHintStored => 'يدفع طلبًا ببطاقة محفوظة: bindingId الخاص بها بدلًا من بيانات البطاقة.';

  @override
  String get coreCardGroup => 'بيانات البطاقة';

  @override
  String get corePan => 'رقم البطاقة';

  @override
  String get coreExpiryPlaceholder => 'MM/YY';

  @override
  String get coreExpiry => 'تاريخ الانتهاء';

  @override
  String get coreCvc => 'CVC/CVV';

  @override
  String get coreCvcOptional => 'CVC/CVV (اختياري)';

  @override
  String get coreHolder => 'الاسم على البطاقة (اختياري)';

  @override
  String get coreBinding => 'bindingId';

  @override
  String get coreMdOrder => 'mdOrder';

  @override
  String get coreMdOrderHint => 'من الخادم الخلفي لديك';

  @override
  String get corePubKey => 'المفتاح العام (PEM)';

  @override
  String get corePaste => 'لصق';

  @override
  String get coreGenerate => 'الحصول على الرمز المميز';

  @override
  String get coreErrorRequired => 'حقل مطلوب';

  @override
  String get coreErrorInvalid => 'قيمة غير صالحة';

  @override
  String get coreErrorFormat => 'تنسيق غير صحيح';

  @override
  String get coreTokenOk => 'تم استلام الرمز المميز';

  @override
  String get coreCopy => 'نسخ';

  @override
  String get coreCopied => 'تم النسخ';

  @override
  String get coreStepSend => 'أرسل الرمز المميز إلى خادمك';

  @override
  String get coreStepGateway => 'يمرّره خادمك إلى بوابة الدفع في المعامل seToken';

  @override
  String get coreStepPay => 'تعالج بوابة الدفع عملية الدفع';

  @override
  String get coreAgain => 'مرة أخرى';

  @override
  String get investName => 'Northwind Invest';

  @override
  String get startSectionPaymentAftSub => 'AFT: تعبئة حساب من بطاقة';

  @override
  String get investTileSub => 'اشحن حسابك الاستثماري من بطاقتك وشاهد رصيدك ينمو';

  @override
  String investAccount(String account) {
    return 'الحساب الاستثماري •• $account';
  }

  @override
  String get investPortfolioValue => 'قيمة المحفظة';

  @override
  String get investChangeMonth => 'هذا الشهر';

  @override
  String get investPeriod1w => 'أسبوع';

  @override
  String get investPeriod1m => 'شهر';

  @override
  String get investPeriod3m => '3 أشهر';

  @override
  String get investPeriod1y => 'سنة';

  @override
  String get investPeriodAll => 'الكل';

  @override
  String get investTopUp => 'شحن';

  @override
  String get investInvest => 'استثمار';

  @override
  String get investFreeCash => 'النقد المتاح';

  @override
  String get investFreeCashSub => 'جاهز للاستثمار';

  @override
  String investFreeCashJustNow(String amount) {
    return '+$amount الآن';
  }

  @override
  String get investMyAssets => 'أصولي';

  @override
  String investUnits(String units, String price) {
    return '$units وحدة · $price';
  }

  @override
  String get investRecent => 'آخر العمليات';

  @override
  String get investOpTopUp => 'شحن';

  @override
  String investOpBuy(String asset) {
    return 'شراء $asset';
  }

  @override
  String get investAssetGlb => 'Global ETF';

  @override
  String get investAssetTch => 'Tech Leaders';

  @override
  String get investNotifications => 'الإشعارات';

  @override
  String get topupTitle => 'شحن';

  @override
  String get topupTo => 'إلى';

  @override
  String get topupAvailable => 'المتاح';

  @override
  String get topupAmount => 'المبلغ';

  @override
  String topupLimits(String min, String max) {
    return 'من $min إلى $max';
  }

  @override
  String topupContinue(String amount) {
    return 'متابعة · $amount';
  }

  @override
  String get topupCreating => 'جارٍ إنشاء التحويل…';

  @override
  String get topupFailedTitle => 'لم يتم التحويل';

  @override
  String get topupFailedBody => 'رفض بنك البطاقة العملية. لم يُخصم أي مبلغ؛ جرّب بطاقة أخرى.';

  @override
  String topupFailedDetail(String message) {
    return 'البوابة: $message';
  }

  @override
  String get topupUnknownTitle => 'الحالة غير معروفة';

  @override
  String get topupUnknownBody => 'تحقق من الطلب في بوابة التاجر قبل المحاولة مرة أخرى.';

  @override
  String get topupTryAgain => 'حاول مرة أخرى';

  @override
  String get topupAddCard => 'إضافة بطاقة';

  @override
  String get topupAddingCard => 'جارٍ إضافة البطاقة…';

  @override
  String get topupCardAddedTitle => 'تمت إضافة البطاقة';

  @override
  String get topupCardAddedBody => 'اخترها في نافذة الدفع عند الشحن التالي. لم يُخصم أي مبلغ.';

  @override
  String get topupCardFailedTitle => 'لم تُضف البطاقة';

  @override
  String get topupCardFailedBody => 'لم يؤكد بنك البطاقة البطاقة. لم يُخصم أي مبلغ؛ جرّب بطاقة أخرى.';

  @override
  String get topupCardUnknownBody => 'لم يؤكد البنك البطاقة بعد. إن أكدها، ستظهر عند الشحن التالي.';

  @override
  String doneCredited(String account) {
    return 'أُودع في الحساب الاستثماري •• $account';
  }

  @override
  String get doneOperation => 'العملية';

  @override
  String get doneOperationValue => 'AFT · إلى حسابك';

  @override
  String get doneFreeCash => 'النقد المتاح';

  @override
  String get doneOrder => 'الطلب';

  @override
  String get doneTime => 'الوقت';

  @override
  String doneInvest(String amount) {
    return 'استثمار $amount';
  }

  @override
  String get doneToPortfolio => 'إلى المحفظة';

  @override
  String get scenarioWalletApple => 'Apple Pay';

  @override
  String get devApplePay => 'Apple Pay';
}
