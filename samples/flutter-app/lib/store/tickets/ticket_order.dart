import '../model/cart.dart';
import '../model/catalog.dart';

/// Seats picked for the concert: a category, how many (1–[maxTickets]), plus one service fee per order.
class TicketOrder {
  const TicketOrder({this.categoryId = 'balcony', this.quantity = 2});

  /// Most seats one order can hold.
  static const maxTickets = 6;

  final String categoryId;
  final int quantity;

  /// Tickets and the fee, in euro cents.
  int get totalMinor => toCart().totalMinor;

  /// The same order with [quantity] kept between 1 and [maxTickets].
  TicketOrder withQuantity(int quantity) =>
      TicketOrder(categoryId: categoryId, quantity: quantity.clamp(1, maxTickets));

  TicketOrder withCategory(String categoryId) => TicketOrder(categoryId: categoryId, quantity: quantity);

  /// Cart lines the store pays for: the seats and the fee.
  Cart toCart() {
    final products = Catalog.products(CoverId.tickets);
    return Cart([
      CartLine(products.firstWhere((product) => product.id == categoryId), quantity),
      CartLine(products.firstWhere((product) => product.id == ticketFeeId), 1),
    ]);
  }
}
