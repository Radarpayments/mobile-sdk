import type { CheckoutOutcome } from '../../checkout/checkout';
import { Listenable } from '../../listenable';
import { InvestAccount } from './investAccount';
import { parseAmount, presetAmount, type TopUpAmount, type TopUpStatus } from './topUpAmount';

/**
 * Northwind Invest: the account shown (the saved one), the amount in the top-up field and how the last transfer
 * ended. Money is credited only when the SDK reports the transfer completed.
 */
export class InvestModel extends Listenable {
  private current: InvestAccount;
  private typed: TopUpAmount = '100';
  private state: TopUpStatus = { kind: 'editing' };

  constructor(
    private readonly load: () => InvestAccount,
    private readonly save: (account: InvestAccount) => void,
    private readonly clock: () => number = Date.now,
  ) {
    super();
    this.current = load();
  }

  get account(): InvestAccount {
    return this.current;
  }

  get amount(): TopUpAmount {
    return this.typed;
  }

  get status(): TopUpStatus {
    return this.state;
  }

  /** The account as saved on the device (it changes after a reset in the developer settings). */
  reload(): void {
    this.current = this.load();
    this.notify();
  }

  /** The amount field changed. */
  edit(input: string): void {
    this.typed = parseAmount(input);
    this.notify();
  }

  /** A preset chip. */
  preset(minor: number): void {
    this.typed = presetAmount(minor);
    this.notify();
  }

  /** The sheet closed after a transfer of `amountMinor`. */
  onOutcome(outcome: CheckoutOutcome, amountMinor: number): void {
    switch (outcome.kind) {
      case 'success': {
        const before = this.current;
        const after = InvestAccount.credit(before, outcome.paymentId, amountMinor, this.clock());
        if (after === before) return;
        this.update(after);
        this.state = {
          kind: 'done',
          orderId: outcome.paymentId,
          amountMinor,
          freeCashBefore: before.freeCashMinor,
          freeCashAfter: after.freeCashMinor,
          at: this.clock(),
        };
        break;
      }
      case 'declined':
        this.state = { kind: 'declined', message: outcome.message };
        break;
      case 'unconfirmed':
        this.state = { kind: 'unconfirmed' };
        break;
      case 'cancelled':
        this.state = { kind: 'editing' };
        break;
    }
    this.notify();
  }

  /** Buys the Global ETF for `amountMinor` (at most the free cash), without a payment. */
  invest(amountMinor: number): void {
    this.update(InvestAccount.invest(this.current, amountMinor, this.clock()));
    this.notify();
  }

  /** The buyer left the top-up screen: its banner goes, the amount stays. */
  leaveTopUp(): void {
    this.state = { kind: 'editing' };
    this.notify();
  }

  private update(next: InvestAccount): void {
    if (next === this.current) return;
    this.current = next;
    this.save(next);
  }
}
