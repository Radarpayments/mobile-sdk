// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for French (`fr`).
class AppLocalizationsFr extends AppLocalizations {
  AppLocalizationsFr([String locale = 'fr']) : super(locale);

  @override
  String get startTitle => 'Démo de paiement';

  @override
  String get startSettings => 'Paramètres développeur';

  @override
  String get coverFurnitureName => 'Hearth & Co';

  @override
  String get coverCoffeeName => 'Roast Lane';

  @override
  String get coverTicketsName => 'Stage Pass';

  @override
  String get coverFurnitureTagline => 'Mobilier · catalogue et panier';

  @override
  String get coverCoffeeTagline => 'Café · commandez à l’avance';

  @override
  String get coverTicketsTagline => 'Billetterie · concerts et spectacles';

  @override
  String get storeBack => 'Retour';

  @override
  String get storeCart => 'Panier';

  @override
  String storeCartCount(int count) {
    return 'Panier · $count';
  }

  @override
  String get storeItems => 'Articles';

  @override
  String get storeTotal => 'Total';

  @override
  String storePay(String amount) {
    return 'Payer $amount';
  }

  @override
  String get storeIncrease => 'Plus';

  @override
  String get storeDecrease => 'Moins';

  @override
  String storeAddToCart(String price) {
    return 'Ajouter au panier · $price';
  }

  @override
  String get storeAdd => 'Ajouter';

  @override
  String get storeRegistering => 'Création de la commande…';

  @override
  String get storeErrorCredentials => 'L’identifiant et le mot de passe marchand ne sont pas renseignés.';

  @override
  String get storeErrorApiKey => 'La clé API n’est pas renseignée.';

  @override
  String storeErrorFailed(String message) {
    return 'La commande n’a pas été créée : $message';
  }

  @override
  String get storeOpenSettings => 'Ouvrir les paramètres';

  @override
  String scenarioRow(String buyer) {
    return 'Scénario : $buyer';
  }

  @override
  String get scenarioTitle => 'Scénario de paiement';

  @override
  String get scenarioBuyer => 'Acheteur';

  @override
  String get scenarioGuest => 'Invité';

  @override
  String get scenarioWithCards => 'Avec cartes';

  @override
  String get scenarioWithCardsHint => 'Les cartes apparaissent après un paiement avec « Enregistrer la carte »';

  @override
  String get scenarioReset => 'Réinitialiser';

  @override
  String get scenarioPayment => 'Paiement';

  @override
  String get scenarioWallet => 'Google Pay';

  @override
  String get scenarioScanner => 'Scanner de carte';

  @override
  String get scenarioSdkResult => 'Écran de résultat du SDK';

  @override
  String resultPaidAmount(String amount) {
    return 'Payé : $amount';
  }

  @override
  String get resultPaymentId => 'Paiement';

  @override
  String get resultAmount => 'Montant';

  @override
  String get resultDeclinedTitle => 'Paiement refusé';

  @override
  String get resultDeclinedBody => 'La banque n’a pas confirmé le paiement.';

  @override
  String get resultUnconfirmedTitle => 'Vérification du paiement';

  @override
  String get resultUnconfirmedBody =>
      'La banque n’a pas encore répondu. Ne payez pas une seconde fois : nous vous préviendrons dès que le paiement sera confirmé.';

  @override
  String get resultTryAgain => 'Réessayer';

  @override
  String get resultBackToStore => 'Retour à la boutique';

  @override
  String get resultDone => 'Terminé';

  @override
  String get devTitle => 'Paramètres développeur';

  @override
  String get devGateway => 'Passerelle';

  @override
  String get devBaseUrl => 'Base URL';

  @override
  String get devLogin => 'Identifiant marchand';

  @override
  String get devPassword => 'Mot de passe';

  @override
  String get devApiV1 => 'API V1';

  @override
  String get devApiV2 => 'API V2';

  @override
  String get devApiVersion => 'API marchand';

  @override
  String get devApiV1Hint =>
      'Les commandes sont créées via register.do avec l’identifiant et le mot de passe marchand.';

  @override
  String get devApiV2Hint => 'Les sessions sont créées via POST /api2/sessions avec la clé API.';

  @override
  String get devApiKey => 'Clé API';

  @override
  String get devOrderType => 'Commande';

  @override
  String get devOrderPurchase => 'Paiement';

  @override
  String get devOrderAft => 'Transfert AFT';

  @override
  String get devOrderAftHint =>
      'Les transferts sont créés via registerP2P.do (AFT à sens unique) : la fenêtre demande les coordonnées de l’expéditeur et du destinataire.';

  @override
  String get devCredentialsNote =>
      'Stockés uniquement sur cet appareil. Dans votre app, la commande est créée par votre backend.';

  @override
  String get devSdkLook => 'Apparence du SDK';

  @override
  String get devSheetTheme => 'Thème du volet';

  @override
  String get devThemeCover => 'Comme la boutique';

  @override
  String get devThemeLight => 'Clair';

  @override
  String get devThemeDark => 'Sombre';

  @override
  String get devThemeSystem => 'Système';

  @override
  String get devLanguage => 'Langue';

  @override
  String get devLanguageSystem => 'Système';

  @override
  String get devWallets => 'Google Pay';

  @override
  String get devGpayGateway => 'Gateway';

  @override
  String get devGpayMerchant => 'Merchant ID';

  @override
  String get devGpayFromGateway => 'De la passerelle de paiement';

  @override
  String get devMore => 'Plus';

  @override
  String get devAdvanced => 'Advanced';

  @override
  String get devAdvancedSub => 'Flux d’états, SSL pinning';

  @override
  String get devLastOrder => 'Dernière commande';

  @override
  String get devCopy => 'Copier';

  @override
  String get devCredentials => 'Identifiants';

  @override
  String get devShowSecret => 'Afficher';

  @override
  String get devHideSecret => 'Masquer';

  @override
  String get devCopied => 'Copié';

  @override
  String get devBaseUrlError => 'L\'adresse doit commencer par https://';

  @override
  String get devNone => '—';

  @override
  String get resultSuccessFurniture => 'Merci pour votre commande';

  @override
  String get resultSuccessFurnitureBody => 'Nous vous contacterons pour organiser la livraison.';

  @override
  String get resultSuccessCoffee => 'Votre commande est en préparation';

  @override
  String get resultSuccessCoffeeBody => 'Montrez ce numéro au barista.';

  @override
  String get resultSuccessTickets => 'Vous y allez !';

  @override
  String get resultSuccessTicketsBody => 'Les billets sont dans votre compte.';

  @override
  String get furnitureCollection => 'Collection\nautomne';

  @override
  String get furnitureAll => 'Tout';

  @override
  String get furnitureChairs => 'Chaises';

  @override
  String get furnitureSofas => 'Canapés';

  @override
  String get furnitureLight => 'Luminaires';

  @override
  String get furnitureDelivery => 'Livraison gratuite sous 3 à 5 jours. Retours sous 30 jours.';

  @override
  String get furnitureDeliveryLabel => 'Livraison';

  @override
  String get furnitureDeliveryFree => 'Gratuite';

  @override
  String get furnitureColours => 'Couleur';

  @override
  String get coffeeLocation => 'King St.';

  @override
  String get coffeeAgain => 'Comme la dernière fois';

  @override
  String get coffeeRepeat => 'Commander à nouveau';

  @override
  String get coffeeTabCoffee => 'Café';

  @override
  String get coffeeTabCold => 'Boissons froides';

  @override
  String get coffeeTabBakery => 'Viennoiseries';

  @override
  String get coffeeOrderTitle => 'Votre commande';

  @override
  String get coffeePickup => 'Retrait';

  @override
  String get coffeePickupValue => 'King St. · dans 10 min';

  @override
  String coffeeAddItem(String item) {
    return 'Ajouter $item';
  }

  @override
  String get coffeeOrderNumberHint => 'Prête dans environ 6 minutes.';

  @override
  String get ticketsLive => 'Live in Hall A';

  @override
  String get ticketsTitle => 'NORTHERN\nLIGHTS';

  @override
  String get ticketsDate => 'Sam. 14 nov. · 20 h · Hall A';

  @override
  String get ticketsSection => 'Billets';

  @override
  String ticketsToPayment(String amount) {
    return 'Passer au paiement · $amount';
  }

  @override
  String get ticketsCheckout => 'Paiement';

  @override
  String ticketsEventLine(String category) {
    return '14 nov. · $category';
  }

  @override
  String ticketsHold(String time) {
    return 'Places réservées encore $time';
  }

  @override
  String get ticketsHoldExpired => 'La réservation a expiré. Choisissez à nouveau vos places.';

  @override
  String get ticketsSeats => 'Billets';

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
  String get productVelvetSofaSub => 'Velours émeraude, pieds en chêne';

  @override
  String get productCloudChairSub => 'Bouclette blanc lait';

  @override
  String get productOnoChairSub => 'Coque moulée, pieds en hêtre';

  @override
  String get productBirchStoolSub => 'Bouleau massif';

  @override
  String get productArcLampSub => 'Acier graphite mat';

  @override
  String get productLumeArmchairSub => 'Laine moutarde';

  @override
  String get productFlatWhiteSub => 'Double espresso, lait chaud';

  @override
  String get productCappuccinoSub => 'Classique, avec mousse de lait';

  @override
  String get productEspressoSub => 'Pure origine';

  @override
  String get productIcedLatteSub => 'Lait froid, glaçons';

  @override
  String get productCroissantSub => 'Sorti du four ce matin';

  @override
  String get ticketFloor => 'Fosse';

  @override
  String get ticketFloorSub => 'Places debout';

  @override
  String get ticketBalcony => 'Balcon';

  @override
  String get ticketBalconySub => 'Rang 2 · places assises';

  @override
  String get ticketFee => 'Frais de service';

  @override
  String get startSectionPayment => 'SDK Payment';

  @override
  String get startSectionPaymentSub =>
      'Un volet de paiement prêt à l’emploi dans votre app — chaque boutique propose tous les scénarios';

  @override
  String get startSectionCore => 'SDK Core';

  @override
  String get startSectionCoreSub => 'Chiffrement de la carte pour votre propre formulaire (PCI DSS)';

  @override
  String get coreTileTitle => 'Votre formulaire de carte';

  @override
  String get coreTileSub => 'La carte devient un jeton pour votre serveur';

  @override
  String get coreTitle => 'SDK Core';

  @override
  String get coreDoneSubtitle => 'Le jeton est prêt';

  @override
  String get coreVariantNew => 'Nouvelle carte';

  @override
  String get coreVariantInstant => 'Sans commande';

  @override
  String get coreVariantStored => 'Carte enregistrée';

  @override
  String get coreHintNew => 'Paie une commande déjà créée : données de la carte et son mdOrder.';

  @override
  String get coreHintInstant => 'Pas encore de commande : votre serveur la crée et la paie en un seul appel.';

  @override
  String get coreHintStored =>
      'Paie une commande avec une carte enregistrée : son bindingId au lieu des données de la carte.';

  @override
  String get coreCardGroup => 'Informations de la carte';

  @override
  String get corePan => 'Numéro de carte';

  @override
  String get coreExpiryPlaceholder => 'MM/AA';

  @override
  String get coreExpiry => 'Expiration';

  @override
  String get coreCvc => 'CVC/CVV';

  @override
  String get coreCvcOptional => 'CVC/CVV (facultatif)';

  @override
  String get coreHolder => 'Nom sur la carte (facultatif)';

  @override
  String get coreBinding => 'bindingId';

  @override
  String get coreMdOrder => 'mdOrder';

  @override
  String get coreMdOrderHint => 'Fourni par votre backend';

  @override
  String get corePubKey => 'Clé publique (PEM)';

  @override
  String get corePaste => 'Coller';

  @override
  String get coreGenerate => 'Obtenir le jeton';

  @override
  String get coreErrorRequired => 'Obligatoire';

  @override
  String get coreErrorInvalid => 'Valeur non valide';

  @override
  String get coreErrorFormat => 'Format incorrect';

  @override
  String get coreTokenOk => 'Jeton reçu';

  @override
  String get coreCopy => 'Copier';

  @override
  String get coreCopied => 'Copié';

  @override
  String get coreStepSend => 'Envoyez le jeton à votre serveur';

  @override
  String get coreStepGateway => 'Votre serveur le transmet à la passerelle dans le paramètre seToken';

  @override
  String get coreStepPay => 'La passerelle traite le paiement';

  @override
  String get coreAgain => 'Recommencer';

  @override
  String get investName => 'Northwind Invest';

  @override
  String get startSectionPaymentAftSub => 'AFT : alimenter un compte par carte';

  @override
  String get investTileSub => 'Alimentez votre compte d\'investissement par carte et regardez le solde grimper';

  @override
  String investAccount(String account) {
    return 'Compte d\'investissement •• $account';
  }

  @override
  String get investPortfolioValue => 'Valeur du portefeuille';

  @override
  String get investChangeMonth => 'ce mois-ci';

  @override
  String get investPeriod1w => '1S';

  @override
  String get investPeriod1m => '1M';

  @override
  String get investPeriod3m => '3M';

  @override
  String get investPeriod1y => '1A';

  @override
  String get investPeriodAll => 'Tout';

  @override
  String get investTopUp => 'Alimenter';

  @override
  String get investInvest => 'Investir';

  @override
  String get investFreeCash => 'Liquidités';

  @override
  String get investFreeCashSub => 'Prêtes à investir';

  @override
  String investFreeCashJustNow(String amount) {
    return '+$amount à l\'instant';
  }

  @override
  String get investMyAssets => 'Mes actifs';

  @override
  String investUnits(String units, String price) {
    return '$units parts · $price';
  }

  @override
  String get investRecent => 'Opérations récentes';

  @override
  String get investOpTopUp => 'Alimentation';

  @override
  String investOpBuy(String asset) {
    return 'Achat $asset';
  }

  @override
  String get investAssetGlb => 'Global ETF';

  @override
  String get investAssetTch => 'Tech Leaders';

  @override
  String get investNotifications => 'Notifications';

  @override
  String get topupTitle => 'Alimenter';

  @override
  String get topupTo => 'Vers';

  @override
  String get topupAvailable => 'Disponible';

  @override
  String get topupAmount => 'Montant';

  @override
  String topupLimits(String min, String max) {
    return 'de $min à $max';
  }

  @override
  String topupContinue(String amount) {
    return 'Continuer · $amount';
  }

  @override
  String get topupCreating => 'Création du virement…';

  @override
  String get topupFailedTitle => 'Le virement a échoué';

  @override
  String get topupFailedBody =>
      'La banque de la carte a refusé l\'opération. Aucun débit n\'a eu lieu ; essayez une autre carte.';

  @override
  String topupFailedDetail(String message) {
    return 'Passerelle : $message';
  }

  @override
  String get topupUnknownTitle => 'Statut inconnu';

  @override
  String get topupUnknownBody => 'Vérifiez la commande dans l\'espace marchand avant de réessayer.';

  @override
  String get topupTryAgain => 'Réessayer';

  @override
  String get topupAddCard => 'Ajouter une carte';

  @override
  String get topupAddingCard => 'Ajout de la carte…';

  @override
  String get topupCardAddedTitle => 'Carte ajoutée';

  @override
  String get topupCardAddedBody =>
      'Choisissez-la dans la fenêtre de paiement lors de votre prochaine alimentation. Aucun montant n\'a été débité.';

  @override
  String get topupCardFailedTitle => 'Carte non ajoutée';

  @override
  String get topupCardFailedBody =>
      'La banque de la carte ne l\'a pas confirmée. Aucun montant n\'a été débité ; essayez une autre carte.';

  @override
  String get topupCardUnknownBody =>
      'La banque n\'a pas encore confirmé la carte. Si elle le fait, la carte apparaîtra lors de la prochaine alimentation.';

  @override
  String doneCredited(String account) {
    return 'crédité sur le compte d\'investissement •• $account';
  }

  @override
  String get doneOperation => 'Opération';

  @override
  String get doneOperationValue => 'AFT · vers son propre compte';

  @override
  String get doneFreeCash => 'Liquidités';

  @override
  String get doneOrder => 'Commande';

  @override
  String get doneTime => 'Heure';

  @override
  String doneInvest(String amount) {
    return 'Investir $amount';
  }

  @override
  String get doneToPortfolio => 'Vers le portefeuille';

  @override
  String get scenarioWalletApple => 'Apple Pay';

  @override
  String get devApplePay => 'Apple Pay';
}
