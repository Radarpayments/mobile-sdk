import type { Product } from './catalog';

/** One product and how many of it the buyer takes. */
export interface CartLine {
  readonly product: Product;
  readonly quantity: number;
}

/** Immutable shopping cart: every change returns a new cart. Prices are euro cents. */
export class Cart {
  static readonly empty = new Cart();

  constructor(readonly lines: readonly CartLine[] = []) {}

  /** Sum of all lines in euro cents. */
  get totalMinor(): number {
    return this.lines.reduce((sum, line) => sum + line.product.priceMinor * line.quantity, 0);
  }

  /** Number of items, counting quantities. */
  get count(): number {
    return this.lines.reduce((sum, line) => sum + line.quantity, 0);
  }

  get isEmpty(): boolean {
    return this.lines.length === 0;
  }

  add(product: Product): Cart {
    const current = this.lines.find(line => line.product.id === product.id)?.quantity ?? 0;
    return current === 0
      ? new Cart([...this.lines, { product, quantity: 1 }])
      : this.setQuantity(product.id, current + 1);
  }

  /** Sets the quantity of `productId`; 0 or less removes the line. */
  setQuantity(productId: string, quantity: number): Cart {
    return new Cart(
      this.lines.flatMap(line =>
        line.product.id !== productId ? [line] : quantity > 0 ? [{ product: line.product, quantity }] : [],
      ),
    );
  }
}
