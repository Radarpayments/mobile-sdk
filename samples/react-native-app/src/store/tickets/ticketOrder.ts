import { Cart } from '../model/cart';
import { product, ticketFeeId } from '../model/catalog';

/** Seats picked for the concert: a category, how many (1–`maxTickets`), plus one service fee per order. */
export class TicketOrder {
  static readonly maxTickets = 6;

  constructor(readonly categoryId: string = 'balcony', readonly quantity: number = 2) {}

  /** Tickets and the fee, in euro cents. */
  get totalMinor(): number {
    return this.toCart().totalMinor;
  }

  /** The same order with `quantity` kept between 1 and `maxTickets`. */
  withQuantity(quantity: number): TicketOrder {
    return new TicketOrder(this.categoryId, Math.min(Math.max(quantity, 1), TicketOrder.maxTickets));
  }

  withCategory(categoryId: string): TicketOrder {
    return new TicketOrder(categoryId, this.quantity);
  }

  /** Cart lines the store pays for: the seats and the fee. */
  toCart(): Cart {
    return new Cart([
      { product: product('tickets', this.categoryId), quantity: this.quantity },
      { product: product('tickets', ticketFeeId), quantity: 1 },
    ]);
  }
}
