// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Spanish Castilian (`es`).
class AppLocalizationsEs extends AppLocalizations {
  AppLocalizationsEs([String locale = 'es']) : super(locale);

  @override
  String get startTitle => 'Demo de pagos';

  @override
  String get startSettings => 'Ajustes de desarrollador';

  @override
  String get coverFurnitureName => 'Hearth & Co';

  @override
  String get coverCoffeeName => 'Roast Lane';

  @override
  String get coverTicketsName => 'Stage Pass';

  @override
  String get coverFurnitureTagline => 'Muebles · catálogo y carrito';

  @override
  String get coverCoffeeTagline => 'Cafetería · pide con antelación';

  @override
  String get coverTicketsTagline => 'Entradas · conciertos y espectáculos';

  @override
  String get storeBack => 'Atrás';

  @override
  String get storeCart => 'Carrito';

  @override
  String storeCartCount(int count) {
    return 'Carrito · $count';
  }

  @override
  String get storeItems => 'Artículos';

  @override
  String get storeTotal => 'Total';

  @override
  String storePay(String amount) {
    return 'Pagar $amount';
  }

  @override
  String get storeIncrease => 'Más';

  @override
  String get storeDecrease => 'Menos';

  @override
  String storeAddToCart(String price) {
    return 'Añadir al carrito · $price';
  }

  @override
  String get storeAdd => 'Añadir';

  @override
  String get storeRegistering => 'Creando el pedido…';

  @override
  String get storeErrorCredentials => 'No se han configurado el usuario y la contraseña del comercio.';

  @override
  String get storeErrorApiKey => 'No se ha configurado la clave API.';

  @override
  String storeErrorFailed(String message) {
    return 'No se ha creado el pedido: $message';
  }

  @override
  String get storeOpenSettings => 'Abrir ajustes';

  @override
  String scenarioRow(String buyer) {
    return 'Escenario: $buyer';
  }

  @override
  String get scenarioTitle => 'Escenario de pago';

  @override
  String get scenarioBuyer => 'Comprador';

  @override
  String get scenarioGuest => 'Invitado';

  @override
  String get scenarioWithCards => 'Con tarjetas';

  @override
  String get scenarioWithCardsHint => 'Las tarjetas aparecen tras un pago con «Guardar tarjeta»';

  @override
  String get scenarioReset => 'Restablecer';

  @override
  String get scenarioPayment => 'Pago';

  @override
  String get scenarioWallet => 'Google Pay';

  @override
  String get scenarioScanner => 'Escáner de tarjetas';

  @override
  String get scenarioSdkResult => 'Pantalla de resultado del SDK';

  @override
  String resultPaidAmount(String amount) {
    return 'Has pagado $amount';
  }

  @override
  String get resultPaymentId => 'Pago';

  @override
  String get resultAmount => 'Importe';

  @override
  String get resultDeclinedTitle => 'Pago rechazado';

  @override
  String get resultDeclinedBody => 'El banco no ha confirmado el pago.';

  @override
  String get resultUnconfirmedTitle => 'Comprobando el pago';

  @override
  String get resultUnconfirmedBody =>
      'El banco aún no ha respondido. No vuelva a pagar: le avisaremos en cuanto se confirme el pago.';

  @override
  String get resultTryAgain => 'Reintentar';

  @override
  String get resultBackToStore => 'Volver a la tienda';

  @override
  String get resultDone => 'Listo';

  @override
  String get devTitle => 'Ajustes de desarrollador';

  @override
  String get devGateway => 'Pasarela';

  @override
  String get devBaseUrl => 'Base URL';

  @override
  String get devLogin => 'Usuario del comercio';

  @override
  String get devPassword => 'Contraseña';

  @override
  String get devApiV1 => 'API V1';

  @override
  String get devApiV2 => 'API V2';

  @override
  String get devApiVersion => 'API del comercio';

  @override
  String get devApiV1Hint => 'Los pedidos se registran con register.do usando el usuario y la contraseña del comercio.';

  @override
  String get devApiV2Hint => 'Las sesiones se crean con POST /api2/sessions usando la clave API.';

  @override
  String get devApiKey => 'Clave API';

  @override
  String get devOrderType => 'Pedido';

  @override
  String get devOrderPurchase => 'Pago';

  @override
  String get devOrderAft => 'Transferencia AFT';

  @override
  String get devOrderAftHint =>
      'Las transferencias se registran con registerP2P.do (AFT de un solo sentido): la ventana pide los datos del remitente y del destinatario.';

  @override
  String get devCredentialsNote => 'Solo se guardan en este dispositivo. En tu app, el pedido lo registra tu backend.';

  @override
  String get devSdkLook => 'Apariencia del SDK';

  @override
  String get devSheetTheme => 'Tema del panel';

  @override
  String get devThemeCover => 'Como la tienda';

  @override
  String get devThemeLight => 'Claro';

  @override
  String get devThemeDark => 'Oscuro';

  @override
  String get devThemeSystem => 'Sistema';

  @override
  String get devLanguage => 'Idioma';

  @override
  String get devLanguageSystem => 'Sistema';

  @override
  String get devWallets => 'Google Pay';

  @override
  String get devGpayGateway => 'Gateway';

  @override
  String get devGpayMerchant => 'Merchant ID';

  @override
  String get devGpayFromGateway => 'De la pasarela de pago';

  @override
  String get devMore => 'Más';

  @override
  String get devAdvanced => 'Advanced';

  @override
  String get devAdvancedSub => 'Flujo de estados, SSL pinning';

  @override
  String get devLastOrder => 'Último pedido';

  @override
  String get devCopy => 'Copiar';

  @override
  String get devNone => '—';

  @override
  String get resultSuccessFurniture => 'Gracias por tu pedido';

  @override
  String get resultSuccessFurnitureBody => 'Nos pondremos en contacto contigo para organizar la entrega.';

  @override
  String get resultSuccessCoffee => 'Estamos preparando tu pedido';

  @override
  String get resultSuccessCoffeeBody => 'Muestra este número al barista.';

  @override
  String get resultSuccessTickets => '¡Nos vemos allí!';

  @override
  String get resultSuccessTicketsBody => 'Tus entradas están en tu cuenta.';

  @override
  String get furnitureCollection => 'Colección\nde otoño';

  @override
  String get furnitureAll => 'Todo';

  @override
  String get furnitureChairs => 'Sillas y sillones';

  @override
  String get furnitureSofas => 'Sofás';

  @override
  String get furnitureLight => 'Iluminación';

  @override
  String get furnitureDelivery => 'Envío gratis en 3–5 días. 30 días para devoluciones.';

  @override
  String get furnitureDeliveryLabel => 'Envío';

  @override
  String get furnitureDeliveryFree => 'Gratis';

  @override
  String get furnitureColours => 'Color';

  @override
  String get coffeeLocation => 'King St.';

  @override
  String get coffeeAgain => 'Lo mismo que la última vez';

  @override
  String get coffeeRepeat => 'Repetir pedido';

  @override
  String get coffeeTabCoffee => 'Café';

  @override
  String get coffeeTabCold => 'Bebidas frías';

  @override
  String get coffeeTabBakery => 'Bollería';

  @override
  String get coffeeOrderTitle => 'Tu pedido';

  @override
  String get coffeePickup => 'Recogida';

  @override
  String get coffeePickupValue => 'King St. · en 10 min';

  @override
  String coffeeAddItem(String item) {
    return 'Añadir $item';
  }

  @override
  String get coffeeOrderNumberHint => 'Estará listo en unos 6 minutos.';

  @override
  String get ticketsLive => 'Live in Hall A';

  @override
  String get ticketsTitle => 'NORTHERN\nLIGHTS';

  @override
  String get ticketsDate => 'Sáb, 14 nov · 20:00 · Hall A';

  @override
  String get ticketsSection => 'Entradas';

  @override
  String ticketsToPayment(String amount) {
    return 'Finalizar compra · $amount';
  }

  @override
  String get ticketsCheckout => 'Finalizar compra';

  @override
  String ticketsEventLine(String category) {
    return '14 nov · $category';
  }

  @override
  String ticketsHold(String time) {
    return 'Asientos reservados durante $time';
  }

  @override
  String get ticketsHoldExpired => 'La reserva ha caducado. Vuelve a elegir tus asientos.';

  @override
  String get ticketsSeats => 'Entradas';

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
  String get productVelvetSofaSub => 'Terciopelo esmeralda, patas de roble';

  @override
  String get productCloudChairSub => 'Bouclé blanco roto';

  @override
  String get productOnoChairSub => 'Carcasa moldeada, patas de haya';

  @override
  String get productBirchStoolSub => 'Abedul macizo';

  @override
  String get productArcLampSub => 'Acero grafito mate';

  @override
  String get productLumeArmchairSub => 'Lana color mostaza';

  @override
  String get productFlatWhiteSub => 'Doble de espresso, leche vaporizada';

  @override
  String get productCappuccinoSub => 'Clásico, con espuma';

  @override
  String get productEspressoSub => 'Origen único';

  @override
  String get productIcedLatteSub => 'Leche fría, hielo';

  @override
  String get productCroissantSub => 'Horneado esta mañana';

  @override
  String get ticketFloor => 'Pista';

  @override
  String get ticketFloorSub => 'Entrada general';

  @override
  String get ticketBalcony => 'Anfiteatro';

  @override
  String get ticketBalconySub => 'Fila 2 · con asiento';

  @override
  String get ticketFee => 'Gastos de gestión';

  @override
  String get startSectionPayment => 'SDK Payment';

  @override
  String get startSectionPaymentSub => 'Un panel de pago listo en tu app — cada tienda incluye todos los escenarios';

  @override
  String get startSectionCore => 'SDK Core';

  @override
  String get startSectionCoreSub => 'Cifrado de tarjetas para tu propio formulario (PCI DSS)';

  @override
  String get coreTileTitle => 'Tu formulario de tarjeta';

  @override
  String get coreTileSub => 'La tarjeta se convierte en un token para tu servidor';

  @override
  String get coreTitle => 'SDK Core';

  @override
  String get coreDoneSubtitle => 'El token está listo';

  @override
  String get coreVariantNew => 'Nueva tarjeta';

  @override
  String get coreVariantInstant => 'Sin pedido';

  @override
  String get coreVariantStored => 'Tarjeta guardada';

  @override
  String get coreHintNew => 'Paga un pedido que has registrado: datos de la tarjeta y su mdOrder.';

  @override
  String get coreHintInstant => 'Aún no hay pedido: tu servidor lo crea y lo paga en una sola llamada.';

  @override
  String get coreHintStored =>
      'Paga un pedido con una tarjeta guardada: su bindingId en lugar de los datos de la tarjeta.';

  @override
  String get coreCardGroup => 'Datos de la tarjeta';

  @override
  String get corePan => 'Número de tarjeta';

  @override
  String get coreExpiryPlaceholder => 'MM/AA';

  @override
  String get coreExpiry => 'Caducidad';

  @override
  String get coreCvc => 'CVC/CVV';

  @override
  String get coreCvcOptional => 'CVC/CVV (opcional)';

  @override
  String get coreHolder => 'Nombre en la tarjeta (opcional)';

  @override
  String get coreBinding => 'bindingId';

  @override
  String get coreMdOrder => 'mdOrder';

  @override
  String get coreMdOrderHint => 'Desde tu backend';

  @override
  String get corePubKey => 'Clave pública (PEM)';

  @override
  String get corePaste => 'Pegar';

  @override
  String get coreGenerate => 'Obtener token';

  @override
  String get coreErrorRequired => 'Obligatorio';

  @override
  String get coreErrorInvalid => 'Valor no válido';

  @override
  String get coreErrorFormat => 'Formato incorrecto';

  @override
  String get coreTokenOk => 'Token recibido';

  @override
  String get coreCopy => 'Copiar';

  @override
  String get coreCopied => 'Copiado';

  @override
  String get coreStepSend => 'Envía el token a tu servidor';

  @override
  String get coreStepGateway => 'Tu servidor lo envía a la pasarela como seToken';

  @override
  String get coreStepPay => 'La pasarela procesa el pago';

  @override
  String get coreAgain => 'Repetir';

  @override
  String get investName => 'Northwind Invest';

  @override
  String get startSectionPaymentAftSub => 'AFT: recargar una cuenta con tarjeta';

  @override
  String get investTileSub => 'Recargue su cuenta de inversión con tarjeta y vea crecer el saldo';

  @override
  String investAccount(String account) {
    return 'Cuenta de inversión •• $account';
  }

  @override
  String get investPortfolioValue => 'Valor de la cartera';

  @override
  String get investChangeMonth => 'este mes';

  @override
  String get investPeriod1w => '1S';

  @override
  String get investPeriod1m => '1M';

  @override
  String get investPeriod3m => '3M';

  @override
  String get investPeriod1y => '1A';

  @override
  String get investPeriodAll => 'Todo';

  @override
  String get investTopUp => 'Recargar';

  @override
  String get investInvest => 'Invertir';

  @override
  String get investFreeCash => 'Efectivo disponible';

  @override
  String get investFreeCashSub => 'Listo para invertir';

  @override
  String investFreeCashJustNow(String amount) {
    return '+$amount ahora mismo';
  }

  @override
  String get investMyAssets => 'Mis activos';

  @override
  String investUnits(String units, String price) {
    return '$units uds. · $price';
  }

  @override
  String get investRecent => 'Últimas operaciones';

  @override
  String get investOpTopUp => 'Recarga';

  @override
  String investOpBuy(String asset) {
    return 'Compra de $asset';
  }

  @override
  String get investAssetGlb => 'Global ETF';

  @override
  String get investAssetTch => 'Tech Leaders';

  @override
  String get investNotifications => 'Notificaciones';

  @override
  String get topupTitle => 'Recargar';

  @override
  String get topupTo => 'A';

  @override
  String get topupAvailable => 'Disponible';

  @override
  String get topupAmount => 'Importe';

  @override
  String topupLimits(String min, String max) {
    return 'de $min a $max';
  }

  @override
  String topupContinue(String amount) {
    return 'Continuar · $amount';
  }

  @override
  String get topupCreating => 'Creando la transferencia…';

  @override
  String get topupFailedTitle => 'La transferencia no se realizó';

  @override
  String get topupFailedBody =>
      'El banco de la tarjeta rechazó la operación. No se cobró nada; pruebe con otra tarjeta.';

  @override
  String topupFailedDetail(String message) {
    return 'Pasarela: $message';
  }

  @override
  String get topupUnknownTitle => 'Estado desconocido';

  @override
  String get topupUnknownBody => 'Compruebe el pedido en el portal del comercio antes de volver a intentarlo.';

  @override
  String get topupTryAgain => 'Intentar de nuevo';

  @override
  String doneCredited(String account) {
    return 'abonado en la cuenta de inversión •• $account';
  }

  @override
  String get doneOperation => 'Operación';

  @override
  String get doneOperationValue => 'AFT · a cuenta propia';

  @override
  String get doneFreeCash => 'Efectivo disponible';

  @override
  String get doneOrder => 'Pedido';

  @override
  String get doneTime => 'Hora';

  @override
  String doneInvest(String amount) {
    return 'Invertir $amount';
  }

  @override
  String get doneToPortfolio => 'A la cartera';

  @override
  String get devInvestReset => 'Restablecer la cuenta demo de Northwind';

  @override
  String get devInvestResetDone => 'Cuenta demo restablecida';

  @override
  String get scenarioWalletApple => 'Apple Pay';

  @override
  String get devApplePay => 'Apple Pay';
}
