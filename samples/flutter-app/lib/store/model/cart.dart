import 'package:flutter/widgets.dart';
import 'package:intl/intl.dart';

import 'catalog.dart';

/// One product and how many of it the buyer takes.
class CartLine {
  const CartLine(this.product, this.quantity);

  final Product product;
  final int quantity;
}

/// Immutable shopping cart: every change returns a new cart. Prices are euro cents.
class Cart {
  const Cart([this.lines = const []]);

  final List<CartLine> lines;

  /// Sum of all lines in euro cents.
  int get totalMinor => lines.fold(0, (sum, line) => sum + line.product.priceMinor * line.quantity);

  /// Number of items, counting quantities.
  int get count => lines.fold(0, (sum, line) => sum + line.quantity);

  /// True when there is nothing to pay for.
  bool get isEmpty => lines.isEmpty;

  /// Adds one [product].
  Cart add(Product product) {
    final current = lines.where((line) => line.product.id == product.id).firstOrNull?.quantity ?? 0;
    return current == 0 ? Cart([...lines, CartLine(product, 1)]) : setQuantity(product.id, current + 1);
  }

  /// Sets the quantity of [productId]; 0 or less removes the line.
  Cart setQuantity(String productId, int quantity) => Cart([
    for (final line in lines)
      if (line.product.id != productId) line else if (quantity > 0) CartLine(line.product, quantity),
  ]);
}

/// Formats euro [minor] units for [locale], e.g. "€1,578.00" or "1 578,00 €".
String formatEur(int minor, Locale locale) =>
    NumberFormat.simpleCurrency(locale: locale.toLanguageTag(), name: 'EUR').format(minor / 100);

/// [formatEur] in the language the demo is shown in.
extension Eur on BuildContext {
  String eur(int minor) => formatEur(minor, Localizations.localeOf(this));
}
