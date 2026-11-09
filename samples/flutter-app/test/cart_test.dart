import 'package:flutter/widgets.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:payrdr_flutter_sample/store/model/cart.dart';
import 'package:payrdr_flutter_sample/store/model/catalog.dart';
import 'package:payrdr_flutter_sample/store/tickets/ticket_order.dart';

void main() {
  final [sofa, chair, ...] = Catalog.products(CoverId.furniture);

  group('Cart', () {
    test('adding the same product raises its quantity instead of adding a line', () {
      final cart = const Cart().add(sofa).add(chair).add(sofa);

      expect(cart.lines.map((line) => (line.product.id, line.quantity)), [('velvet_sofa', 2), ('cloud_chair', 1)]);
      expect(cart.count, 3);
      expect(cart.totalMinor, 2 * 124900 + 32900);
    });

    test('quantity 0 removes the line and an emptied cart is empty', () {
      final cart = const Cart().add(sofa).add(chair).setQuantity(sofa.id, 0);

      expect(cart.lines.single.product.id, 'cloud_chair');
      expect(cart.setQuantity(chair.id, -1).isEmpty, isTrue);
    });

    test('setting a quantity keeps the order of the lines', () {
      final cart = const Cart().add(sofa).add(chair).setQuantity(sofa.id, 5);

      expect(cart.lines.map((line) => line.product.id), ['velvet_sofa', 'cloud_chair']);
      expect(cart.totalMinor, 5 * 124900 + 32900);
    });

    test('changes leave the original cart as it was', () {
      final cart = const Cart().add(sofa);
      cart.add(chair).setQuantity(sofa.id, 3);

      expect(cart.count, 1);
    });
  });

  group('TicketOrder', () {
    test('pays the seats plus one service fee', () {
      const order = TicketOrder(categoryId: 'floor', quantity: 3);

      expect(order.totalMinor, 3 * 4500 + 350);
      expect(order.toCart().lines.map((line) => (line.product.id, line.quantity)), [('floor', 3), ('fee', 1)]);
    });

    test('keeps between 1 and 6 seats', () {
      const order = TicketOrder();

      expect(order.withQuantity(0).quantity, 1);
      expect(order.withQuantity(9).quantity, TicketOrder.maxTickets);
      expect(order.withCategory('floor').quantity, 2);
    });
  });

  test('euro amounts follow the language', () {
    expect(formatEur(157800, const Locale('en')), '€1,578.00');
    expect(formatEur(157800, const Locale('de')), '1.578,00 €');
    // Latin digits in Arabic too: card data and the SDK's texts use them.
    expect(formatEur(390, const Locale('ar')), contains('3.90'));
  });
}
