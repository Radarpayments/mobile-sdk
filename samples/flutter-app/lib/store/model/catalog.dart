import '../../l10n/app_localizations.dart';

/// The demo businesses. Every cover runs the same payment path and scenarios.
enum CoverId { furniture, coffee, tickets, invest }

/// A text of the demo in the current language.
typedef LocalText = String Function(AppLocalizations l10n);

/// A thing the buyer can put in the cart; [priceMinor] is in euro cents.
class Product {
  const Product(this.id, this.name, this.subtitle, this.priceMinor, this.image);

  final String id;
  final LocalText name;
  final LocalText? subtitle;
  final int priceMinor;

  /// Asset path of the photo.
  final String? image;
}

/// Id of the per-order service fee line in [CoverId.tickets].
const ticketFeeId = 'fee';

/// What each demo business sells. Prices are euro cents.
abstract final class Catalog {
  /// Products of [cover], in display order.
  static List<Product> products(CoverId cover) => switch (cover) {
    CoverId.furniture => _furniture,
    CoverId.coffee => _coffee,
    CoverId.tickets => _tickets,
    // Northwind Invest sells nothing: the buyer tops up an account.
    CoverId.invest => const [],
  };

  static final _furniture = [
    Product('velvet_sofa', (l) => l.productVelvetSofa, (l) => l.productVelvetSofaSub, 124900, photo('furniture_sofa')),
    Product(
      'cloud_chair',
      (l) => l.productCloudChair,
      (l) => l.productCloudChairSub,
      32900,
      photo('furniture_cloud_chair'),
    ),
    Product('ono_chair', (l) => l.productOnoChair, (l) => l.productOnoChairSub, 18900, photo('furniture_ono_chair')),
    Product('birch_stool', (l) => l.productBirchStool, (l) => l.productBirchStoolSub, 9900, photo('furniture_stool')),
    Product('arc_lamp', (l) => l.productArcLamp, (l) => l.productArcLampSub, 21900, photo('furniture_lamp')),
    Product(
      'lume_armchair',
      (l) => l.productLumeArmchair,
      (l) => l.productLumeArmchairSub,
      45900,
      photo('furniture_hero'),
    ),
  ];

  static final _coffee = [
    Product('flat_white', (l) => l.productFlatWhite, (l) => l.productFlatWhiteSub, 390, photo('coffee_flat_white')),
    Product('cappuccino', (l) => l.productCappuccino, (l) => l.productCappuccinoSub, 360, photo('coffee_cappuccino')),
    Product('espresso', (l) => l.productEspresso, (l) => l.productEspressoSub, 260, photo('coffee_hero')),
    Product('iced_latte', (l) => l.productIcedLatte, (l) => l.productIcedLatteSub, 420, photo('coffee_hero')),
    Product('croissant', (l) => l.productCroissant, (l) => l.productCroissantSub, 350, photo('coffee_croissant')),
  ];

  static final _tickets = [
    Product('floor', (l) => l.ticketFloor, (l) => l.ticketFloorSub, 4500, null),
    Product('balcony', (l) => l.ticketBalcony, (l) => l.ticketBalconySub, 6500, null),
    Product(ticketFeeId, (l) => l.ticketFee, null, 350, null),
  ];
}

/// Asset path of the demo photo [name].
String photo(String name) => 'assets/images/$name.webp';
