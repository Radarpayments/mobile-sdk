import type { CheckoutOutcome } from '../checkout/checkout';
import { Listenable } from '../listenable';
import { Cart } from './model/cart';
import type { CoverId, Product } from './model/catalog';

/** What the store's result screen shows. */
export interface Receipt {
  outcome: CheckoutOutcome;
  amountMinor: number;
}

const FIRST_ORDER_NUMBER = 42;

/** Carts of the demo businesses and the result of the last payment, shared by all store screens. */
export class StoreModel extends Listenable {
  private carts: Record<CoverId, Cart> = { furniture: Cart.empty, coffee: Cart.empty, tickets: Cart.empty, invest: Cart.empty };
  private paid: Partial<Record<CoverId, Cart>> = {};
  private receipt: Receipt | undefined;
  private orderNumber = FIRST_ORDER_NUMBER - 1;

  cart(cover: CoverId): Cart {
    return this.carts[cover];
  }

  /** The last paid order of `cover`, or undefined when nothing was paid there yet. */
  lastPaid(cover: CoverId): Cart | undefined {
    return this.paid[cover];
  }

  /** The last payment that ended in anything but a cancel; undefined after a cancel. */
  get lastReceipt(): Receipt | undefined {
    return this.receipt;
  }

  /** Number of the last paid order, as the coffee shop calls it out. */
  get lastOrderNumber(): number {
    return this.orderNumber;
  }

  /** Puts the last paid order of `cover` back into its cart, for "Order again". */
  repeatLast(cover: CoverId): void {
    const last = this.lastPaid(cover);
    if (last) this.edit(cover, () => last);
  }

  /** Adds one `product` to the cart of `cover`. */
  add(cover: CoverId, product: Product): void {
    this.edit(cover, cart => cart.add(product));
  }

  /** Sets how many of `productId` are in the cart of `cover`. */
  setQuantity(cover: CoverId, productId: string, quantity: number): void {
    this.edit(cover, cart => cart.setQuantity(productId, quantity));
  }

  /** Replaces the cart of `cover` with `cart`, e.g. the seats picked for a concert. */
  replaceCart(cover: CoverId, cart: Cart): void {
    this.edit(cover, () => cart);
  }

  /** Records how the payment of `amountMinor` for `cover` ended: a success empties the cart. */
  onOutcome(cover: CoverId, outcome: CheckoutOutcome, amountMinor: number): void {
    this.receipt = outcome.kind === 'cancelled' ? undefined : { outcome, amountMinor };
    if (outcome.kind === 'success') {
      this.paid[cover] = this.cart(cover);
      this.orderNumber++;
      this.carts[cover] = Cart.empty;
    }
    this.notify();
  }

  private edit(cover: CoverId, change: (cart: Cart) => Cart): void {
    this.carts[cover] = change(this.cart(cover));
    this.notify();
  }
}
