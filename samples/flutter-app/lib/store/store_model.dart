import 'package:flutter/foundation.dart';

import '../checkout/checkout.dart';
import 'model/cart.dart';
import 'model/catalog.dart';

/// What the store's result screen shows.
class Receipt {
  const Receipt(this.outcome, this.amountMinor);

  final CheckoutOutcome outcome;
  final int amountMinor;
}

/// Carts of the demo businesses and the result of the last payment, shared by all store screens.
class StoreModel extends ChangeNotifier {
  final Map<CoverId, Cart> _carts = {for (final cover in CoverId.values) cover: const Cart()};
  final Map<CoverId, Cart> _paid = {};
  Receipt? _receipt;
  int _orderNumber = _firstOrderNumber - 1;

  static const _firstOrderNumber = 42;

  Cart cart(CoverId cover) => _carts[cover]!;

  /// The last paid order of [cover], or null when nothing was paid there yet.
  Cart? lastPaid(CoverId cover) => _paid[cover];

  /// The last payment that ended in success or decline; null after a cancel.
  Receipt? get lastReceipt => _receipt;

  /// Number of the last paid order, as the coffee shop calls it out.
  int get lastOrderNumber => _orderNumber;

  /// Puts the last paid order of [cover] back into its cart, for "Order again".
  void repeatLast(CoverId cover) {
    final last = lastPaid(cover);
    if (last != null) _edit(cover, (_) => last);
  }

  /// Adds one [product] to the cart of [cover].
  void add(CoverId cover, Product product) => _edit(cover, (cart) => cart.add(product));

  /// Sets how many of [productId] are in the cart of [cover].
  void setQuantity(CoverId cover, String productId, int quantity) =>
      _edit(cover, (cart) => cart.setQuantity(productId, quantity));

  /// Replaces the cart of [cover] with [cart], e.g. the seats picked for a concert.
  void replaceCart(CoverId cover, Cart cart) => _edit(cover, (_) => cart);

  /// Records how the payment of [amountMinor] for [cover] ended: a success empties the cart.
  void onOutcome(CoverId cover, CheckoutOutcome outcome, int amountMinor) {
    _receipt = outcome is Cancelled ? null : Receipt(outcome, amountMinor);
    if (outcome is Success) {
      _paid[cover] = cart(cover);
      _orderNumber++;
      _carts[cover] = const Cart();
    }
    notifyListeners();
  }

  void _edit(CoverId cover, Cart Function(Cart) change) {
    _carts[cover] = change(cart(cover));
    notifyListeners();
  }
}
