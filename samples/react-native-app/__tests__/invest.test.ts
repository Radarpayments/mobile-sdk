import { GatewayOrderRegistrar, merchantRegistrar } from '../src/merchant/orderRegistrar';
import { InvestAccount, seedAccount } from '../src/store/invest/investAccount';
import { coverFits, isAftDemo, reopenCover, visibleCovers } from '../src/store/invest/investMode';
import { InvestModel } from '../src/store/invest/investModel';
import { northwindPrefill } from '../src/store/invest/northwindProfile';
import {
  canContinue,
  parseAmount,
  presetAmount,
  TopUpAmount,
  amountMinor,
  amountValid,
} from '../src/store/invest/topUpAmount';

const now = 1793280000000;
const glb = (account: InvestAccount) => account.holdings.find(h => h.ticker === 'GLB')!.valueMinor;

describe('InvestAccount', () => {
  test('the seed totals 4812.40', () => {
    expect(InvestAccount.total(seedAccount)).toBe(481240);
    expect(seedAccount.freeCashMinor).toBe(1240);
  });

  test('credit adds to free cash and prepends a top-up; the same order credits once', () => {
    const account = InvestAccount.credit(seedAccount, 'md-1', 10000, now);
    expect(account.freeCashMinor).toBe(11240);
    expect(account.operations[0]).toEqual({ kind: 'TOP_UP', amountMinor: 10000, atEpochMillis: now, orderId: 'md-1' });
    const again = InvestAccount.credit(account, 'md-1', 10000, now + 1);
    expect(again).toBe(account);
  });

  test('invest moves cash into GLB, at most the free cash, nothing under a euro', () => {
    const account = InvestAccount.invest(InvestAccount.credit(seedAccount, 'md-1', 10000, now), 10000, now + 1);
    expect(account.freeCashMinor).toBe(1240);
    expect(glb(account)).toBe(300000);
    expect(account.operations[0]).toEqual({ kind: 'BUY', amountMinor: 10000, atEpochMillis: now + 1, ticker: 'GLB' });
    expect(InvestAccount.invest(seedAccount, 1000000, now).freeCashMinor).toBe(0);
    const poor = { ...seedAccount, freeCashMinor: 99 };
    expect(InvestAccount.canInvest(poor)).toBe(false);
    expect(InvestAccount.invest(poor, 99, now)).toBe(poor);
  });

  test('recent operations are newest first and capped', () => {
    let account = seedAccount;
    for (let i = 0; i < 6; i++) account = InvestAccount.credit(account, `md-${i}`, 1000, now + i);
    expect(InvestAccount.recent(account)).toHaveLength(5);
    expect(InvestAccount.recent(account)[0].orderId).toBe('md-5');
  });

  test('json round trip; corrupt json gives the seed', () => {
    const account = InvestAccount.invest(InvestAccount.credit(seedAccount, 'md-1', 10000, now), 5000, now + 1);
    expect(InvestAccount.fromJson(InvestAccount.toJson(account))).toEqual(account);
    for (const json of [undefined, '', '{', '{"freeCashMinor":"x"}', '[]', '{"freeCashMinor":1}']) {
      expect(InvestAccount.fromJson(json)).toEqual(seedAccount);
    }
  });
});

describe('TopUpAmount', () => {
  test('the default is 100 euros and valid', () => {
    const amount: TopUpAmount = '100';
    expect(amountMinor(amount)).toBe(10000);
    expect(amountValid(amount)).toBe(true);
  });

  test('text from the system keyboard is cleaned', () => {
    expect(parseAmount('12,345')).toBe('12.34');
    expect(parseAmount('12.3.4')).toBe('12.34');
    expect(parseAmount('0005')).toBe('5');
    expect(parseAmount(',5')).toBe('0.5');
    expect(parseAmount('.')).toBe('0.');
    expect(parseAmount('-')).toBe('');
    expect(parseAmount('1234567')).toBe('123456');
    expect(amountMinor('10.')).toBe(1000);
  });

  test('limits are 10 to 1000 euros; empty has no amount', () => {
    expect(amountValid('9.99')).toBe(false);
    expect(amountValid('10')).toBe(true);
    expect(amountValid('1000')).toBe(true);
    expect(amountValid('1000.01')).toBe(false);
    expect(amountMinor('')).toBeUndefined();
    expect(amountValid('0')).toBe(false);
  });

  test('presets set whole euros', () => {
    expect(presetAmount(25000)).toBe('250');
    expect(presetAmount(1250)).toBe('12.5');
  });

  test('unconfirmed blocks continue', () => {
    expect(canContinue({ kind: 'unconfirmed' }, '100')).toBe(false);
    expect(canContinue({ kind: 'declined' }, '100')).toBe(true);
    expect(canContinue({ kind: 'editing' }, '100')).toBe(true);
    expect(canContinue({ kind: 'editing' }, '5')).toBe(false);
  });
});

describe('mode', () => {
  test('the AFT demo is API V1 with AFT orders', () => {
    expect(isAftDemo('V1', 'AFT')).toBe(true);
    expect(isAftDemo('V2', 'AFT')).toBe(false);
    expect(isAftDemo('V1', 'PURCHASE')).toBe(false);
  });

  test('the AFT demo shows only Northwind, otherwise the stores; the last business reopens only when shown', () => {
    expect(visibleCovers(true)).toEqual(['invest']);
    expect(visibleCovers(false)).toEqual(['furniture', 'coffee', 'tickets']);
    expect(reopenCover('furniture', true)).toBeUndefined();
    expect(reopenCover('invest', false)).toBeUndefined();
    expect(reopenCover('invest', true)).toBe('invest');
    expect(reopenCover(undefined, true)).toBeUndefined();
  });
});

describe('InvestModel', () => {
  test('a success credits once, saves and shows done', () => {
    const saved: InvestAccount[] = [];
    const model = new InvestModel(
      () => seedAccount,
      a => saved.push(a),
      () => 1000,
    );
    model.onOutcome({ kind: 'success', paymentId: 'md-1' }, 10000);
    model.onOutcome({ kind: 'success', paymentId: 'md-1' }, 10000);
    expect(model.account.freeCashMinor).toBe(11240);
    expect(saved[saved.length - 1].freeCashMinor).toBe(11240);
    expect(model.status).toEqual({
      kind: 'done',
      orderId: 'md-1',
      amountMinor: 10000,
      freeCashBefore: 1240,
      freeCashAfter: 11240,
      at: 1000,
    });
  });

  test('declined and unconfirmed keep the balance; cancel goes back to editing', () => {
    const model = new InvestModel(
      () => seedAccount,
      () => {
        throw new Error('nothing to save');
      },
      () => 1000,
    );
    model.onOutcome({ kind: 'declined', message: 'Issuer declined' }, 10000);
    expect(model.status).toEqual({ kind: 'declined', message: 'Issuer declined' });
    model.onOutcome({ kind: 'unconfirmed', paymentId: 'md-2' }, 10000);
    expect(model.status).toEqual({ kind: 'unconfirmed' });
    expect(model.account.freeCashMinor).toBe(1240);
    model.onOutcome({ kind: 'cancelled' }, 10000);
    expect(model.status).toEqual({ kind: 'editing' });
  });

  test('the field and the presets edit the amount', () => {
    const model = new InvestModel(
      () => seedAccount,
      () => {},
      () => 1000,
    );
    model.edit('2505,5');
    expect(model.amount).toBe('2505.5');
    model.preset(5000);
    expect(amountMinor(model.amount)).toBe(5000);
  });
});

describe('registrar prefill', () => {
  function answering() {
    return jest.fn(async (_url: RequestInfo | URL, _init?: RequestInit) => ({
      arrayBuffer: async () => Uint8Array.from('{"orderId":"md-aft"}', c => c.charCodeAt(0)).buffer,
    })) as unknown as jest.Mock & typeof fetch;
  }

  test('an AFT order carries the prefill the app passes', async () => {
    const fetch = answering();
    await new GatewayOrderRegistrar({
      baseUrl: 'https://gateway.test/payment/',
      login: 'merchant-api',
      password: 'secret',
      orderType: 'AFT',
      prefill: northwindPrefill,
      fetch,
    }).register(10000, undefined);
    const body = JSON.parse(fetch.mock.calls[0][1].body);
    expect(body.email).toBe('alex.kim@example.com');
    expect(body.billingPayerData).toMatchObject({
      payerFirstName: 'Alex',
      payerBirthday: '19900115',
      billingCountry: 'CY',
    });
    expect(body.billingRecipientData).toMatchObject({
      recipientAccount: 'NW00007781',
      recipientAccountNumberType: '100',
    });
    expect(body.transactionTypeIndicator).toBe('A');
  });

  test('an AFT order without prefill sends no payer data; a payment ignores the prefill', async () => {
    const aft = answering();
    await new GatewayOrderRegistrar({
      baseUrl: 'https://gateway.test/payment/',
      login: 'l',
      password: 'p',
      orderType: 'AFT',
      fetch: aft,
    }).register(100, undefined);
    const body = JSON.parse(aft.mock.calls[0][1].body);
    expect(body).not.toHaveProperty('email');
    expect(body).not.toHaveProperty('billingPayerData');
    expect(body).not.toHaveProperty('billingRecipientData');
    const purchase = answering();
    await new GatewayOrderRegistrar({
      baseUrl: 'https://gateway.test/payment/',
      login: 'l',
      password: 'p',
      prefill: northwindPrefill,
      fetch: purchase,
    }).register(100, undefined);
    expect(new URLSearchParams(purchase.mock.calls[0][1].body).has('email')).toBe(false);
  });

  test('the factory passes the prefill to API V1', () => {
    const v1 = merchantRegistrar({
      apiVersion: 'V1',
      baseUrl: 'https://gateway.test/payment/',
      login: 'l',
      password: 'p',
      apiKey: '',
      orderType: 'AFT',
      prefill: northwindPrefill,
    });
    expect((v1 as GatewayOrderRegistrar).prefill).toBe(northwindPrefill);
  });
});

describe('Arabic input', () => {
  it('parse reads the Arabic decimal separator', () => {
    expect(parseAmount('12٫5')).toBe('12.5');
  });

  it('parse reads Arabic-Indic and Persian digits as ASCII', () => {
    expect(parseAmount('١٢٫٥')).toBe('12.5');
    expect(parseAmount('۳۰')).toBe('30');
  });
});

describe('mode switched under an open business', () => {
  it('an open business stays only while the mode still shows it', () => {
    expect(coverFits('invest', true)).toBe(true);
    expect(coverFits('invest', false)).toBe(false);
    expect(coverFits('tickets', true)).toBe(false);
    expect(coverFits('tickets', false)).toBe(true);
  });
});
