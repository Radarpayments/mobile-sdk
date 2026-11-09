/** A fund in the demo portfolio; prices are fixed, `changeBasisPoints` is the month's change (410 = +4.1%). */
export interface Holding {
  ticker: string;
  valueMinor: number;
  unitPriceMinor: number;
  changeBasisPoints: number;
}

/** One line of the account history; `orderId` is the gateway order of a top-up. */
export interface InvestOperation {
  kind: 'TOP_UP' | 'BUY';
  amountMinor: number;
  atEpochMillis: number;
  orderId?: string;
  ticker?: string;
}

/**
 * The Northwind Invest demo account, kept on the device. Only `credit` brings money in, and only after the SDK
 * reports a completed transfer; `invest` moves it into a fund locally, without a payment.
 */
export interface InvestAccount {
  freeCashMinor: number;
  holdings: Holding[];
  operations: InvestOperation[];
}

export const GLOBAL_ETF = 'GLB';
export const TECH_LEADERS = 'TCH';
const MIN_INVEST_MINOR = 100;

/** The account a fresh install starts with: €4,812.40 in total. Seed operations: 12 Oct 2026. */
export const seedAccount: InvestAccount = {
  freeCashMinor: 1240,
  holdings: [
    { ticker: GLOBAL_ETF, valueMinor: 290000, unitPriceMinor: 23387, changeBasisPoints: 410 },
    { ticker: TECH_LEADERS, valueMinor: 190000, unitPriceMinor: 37255, changeBasisPoints: 200 },
  ],
  operations: [
    { kind: 'BUY', amountMinor: 25000, atEpochMillis: 1791800400000, ticker: GLOBAL_ETF },
    { kind: 'TOP_UP', amountMinor: 25000, atEpochMillis: 1791800100000, orderId: 'seed' },
  ],
};

const isNumber = (value: unknown): value is number => typeof value === 'number' && Number.isFinite(value);

export const InvestAccount = {
  total: (a: InvestAccount) => a.freeCashMinor + a.holdings.reduce((sum, h) => sum + h.valueMinor, 0),

  canInvest: (a: InvestAccount) => a.freeCashMinor >= MIN_INVEST_MINOR,

  /** Units of the fund a holding is worth. */
  quantity: (h: Holding) => h.valueMinor / h.unitPriceMinor,

  /** Adds a completed top-up of order `orderId`; a second result of the same order changes nothing. */
  credit(a: InvestAccount, orderId: string, amountMinor: number, at: number): InvestAccount {
    if (a.operations.some(op => op.kind === 'TOP_UP' && op.orderId === orderId)) return a;
    return {
      ...a,
      freeCashMinor: a.freeCashMinor + amountMinor,
      operations: [{ kind: 'TOP_UP', amountMinor, atEpochMillis: at, orderId }, ...a.operations],
    };
  },

  /** Buys `ticker` for `amountMinor`, at most the free cash; nothing when there is less than a euro to invest. */
  invest(a: InvestAccount, amountMinor: number, at: number, ticker = GLOBAL_ETF): InvestAccount {
    if (!InvestAccount.canInvest(a)) return a;
    const amount = Math.min(amountMinor, a.freeCashMinor);
    if (amount <= 0) return a;
    return {
      freeCashMinor: a.freeCashMinor - amount,
      holdings: a.holdings.map(h => (h.ticker === ticker ? { ...h, valueMinor: h.valueMinor + amount } : h)),
      operations: [{ kind: 'BUY', amountMinor: amount, atEpochMillis: at, ticker }, ...a.operations],
    };
  },

  /** The latest `limit` operations, newest first. */
  recent: (a: InvestAccount, limit = 5) => a.operations.slice(0, limit),

  /** The account as kept in the app settings. */
  toJson: (a: InvestAccount) => JSON.stringify(a),

  /** The account saved with `toJson`; the seed when there is none or it cannot be read. */
  fromJson(json: string | undefined): InvestAccount {
    if (!json) return seedAccount;
    try {
      const o = JSON.parse(json);
      const ok =
        o !== null &&
        typeof o === 'object' &&
        isNumber(o.freeCashMinor) &&
        Array.isArray(o.holdings) &&
        Array.isArray(o.operations) &&
        o.holdings.every(
          (h: Holding) => typeof h.ticker === 'string' && isNumber(h.valueMinor) && isNumber(h.unitPriceMinor),
        ) &&
        o.operations.every(
          (op: InvestOperation) =>
            (op.kind === 'TOP_UP' || op.kind === 'BUY') && isNumber(op.amountMinor) && isNumber(op.atEpochMillis),
        );
      return ok ? (o as InvestAccount) : seedAccount;
    } catch {
      return seedAccount;
    }
  },
};
