import { Cart } from '../src/store/model/cart';
import { product } from '../src/store/model/catalog';
import { StoreModel } from '../src/store/storeModel';
import { TicketOrder } from '../src/store/tickets/ticketOrder';

const sofa = product('furniture', 'velvet_sofa');
const stool = product('furniture', 'birch_stool');

describe('Cart', () => {
  test('adding the same product twice raises its quantity', () => {
    const cart = Cart.empty.add(sofa).add(stool).add(sofa);
    expect(cart.lines.map(line => [line.product.id, line.quantity])).toEqual([
      ['velvet_sofa', 2],
      ['birch_stool', 1],
    ]);
    expect(cart.count).toBe(3);
    expect(cart.totalMinor).toBe(2 * 124900 + 9900);
  });

  test('a quantity of 0 or less removes the line', () => {
    const cart = Cart.empty.add(sofa).add(stool);
    expect(cart.setQuantity('velvet_sofa', 0).lines.map(line => line.product.id)).toEqual(['birch_stool']);
    expect(cart.setQuantity('birch_stool', -1).setQuantity('velvet_sofa', 0).isEmpty).toBe(true);
    expect(cart.setQuantity('birch_stool', 4).totalMinor).toBe(124900 + 4 * 9900);
  });

  test('changes leave the original cart as it was', () => {
    const cart = Cart.empty.add(sofa);
    cart.add(sofa);
    cart.setQuantity('velvet_sofa', 0);
    expect(cart.count).toBe(1);
  });
});

describe('TicketOrder', () => {
  test('seats plus one service fee per order', () => {
    expect(new TicketOrder().totalMinor).toBe(2 * 6500 + 350);
    expect(new TicketOrder('floor', 3).totalMinor).toBe(3 * 4500 + 350);
  });

  test('keeps between 1 and 6 seats', () => {
    expect(new TicketOrder().withQuantity(0).quantity).toBe(1);
    expect(new TicketOrder().withQuantity(9).quantity).toBe(TicketOrder.maxTickets);
    expect(new TicketOrder('floor', 4).withCategory('balcony').quantity).toBe(4);
  });
});

describe('StoreModel', () => {
  test('a success empties the cart and remembers it for "Order again"', () => {
    const store = new StoreModel();
    store.add('coffee', product('coffee', 'flat_white'));
    store.onOutcome('coffee', { kind: 'success', paymentId: 'p1' }, 390);
    expect(store.cart('coffee').isEmpty).toBe(true);
    expect(store.lastReceipt).toEqual({ outcome: { kind: 'success', paymentId: 'p1' }, amountMinor: 390 });
    expect(store.lastOrderNumber).toBe(42);
    store.repeatLast('coffee');
    expect(store.cart('coffee').totalMinor).toBe(390);
  });

  test('a decline keeps the cart, a cancel leaves no receipt', () => {
    const store = new StoreModel();
    store.add('furniture', sofa);
    store.onOutcome('furniture', { kind: 'declined', message: 'Declined' }, 124900);
    expect(store.cart('furniture').count).toBe(1);
    expect(store.lastReceipt?.outcome.kind).toBe('declined');
    store.onOutcome('furniture', { kind: 'cancelled' }, 124900);
    expect(store.lastReceipt).toBeUndefined();
    expect(store.lastPaid('furniture')).toBeUndefined();
  });
});
