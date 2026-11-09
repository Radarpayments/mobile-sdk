// The sample messages in sdk-bridge/contract, which the bridge's own tests check too: what this plugin sends is what
// the bridge reads, and what the bridge sends back is read here. A case added there fails here until it is handled.
import checkoutRequests from '../../../../sdk-bridge/contract/checkout-requests.json';
import checkoutResults from '../../../../sdk-bridge/contract/checkout-results.json';
import coreTokenRequests from '../../../../sdk-bridge/contract/core-token-requests.json';
import coreTokenResults from '../../../../sdk-bridge/contract/core-token-results.json';
import errorCodes from '../../../../sdk-bridge/contract/error-codes.json';
import { calls, native } from './react-native.mock';
import { PayrdrCheckout, PayrdrCore } from '../index';
import type { CheckoutResult, PayrdrConfig } from '../index';

/** What the plugin sends for `call` while the native side answers `reply`. */
async function sentBy(call: () => Promise<unknown>, reply: unknown) {
  calls.length = 0;
  native.reply = reply;
  await call();
  return calls[0]!.request;
}

/** What the plugin makes of `reply` from the native side. */
function readFrom<T>(reply: unknown, call: () => Promise<T>) {
  native.reply = reply;
  return call();
}

const canceled = { status: 'canceled', orderId: 'md-1' };
// The defaults a plugin always sends, so that every plugin sends the same.
const base: PayrdrConfig = { baseUrl: 'https://pay.example.com', cardScanner: false, showResultScreen: false, debugLogging: false };

it('sends every checkout request as the bridge reads it', async () => {
  const sends: Record<string, () => Promise<unknown>> = {
    minimal: () => new PayrdrCheckout(base).present('md-1'),
    session: () => new PayrdrCheckout(base).presentSession('ps_1'),
    allOptions: () =>
      new PayrdrCheckout({
        baseUrl: 'https://pay.example.com',
        locale: 'de',
        theme: { mode: 'dark' },
        cardScanner: true,
        showResultScreen: true,
        keyProviderUrl: 'https://keys.example.com',
        sslPins: [{ host: 'pay.example.com', sha256: 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=' }],
        debugLogging: true,
      }).present('md-1'),
    customTheme: () =>
      new PayrdrCheckout({
        ...base,
        theme: {
          mode: 'custom',
          base: 'dark',
          colors: { primary: '#f3d9b1', onPrimary: '#fff', scrim: '#00000080' },
          cornerRadius: 18,
          buttonCornerRadius: 999,
        },
      }).present('md-1'),
    wallets: () =>
      new PayrdrCheckout({
        ...base,
        googlePay: {
          merchantId: 'BCR2',
          merchantName: 'Hearth & Co',
          gateway: 'bpcpay',
          gatewayMerchantId: 'shop',
          environment: 'TEST',
          countryCode: 'DE',
          cardNetworks: ['VISA', 'MASTERCARD'],
          authMethods: ['PAN_ONLY', 'CRYPTOGRAM_3DS'],
          billingAddressRequired: true,
        },
        applePay: { merchantId: 'merchant.shop', countryCode: 'DE', networks: ['VISA', 'AMEX'], capabilities: ['THREE_D_SECURE', 'DEBIT'] },
      }).present('md-1'),
  };
  const expected: Record<string, unknown> = checkoutRequests;

  expect(Object.keys(sends).sort()).toEqual(Object.keys(expected).sort());
  for (const [name, send] of Object.entries(sends)) {
    expect([name, await sentBy(send, canceled)]).toEqual([name, expected[name]]);
  }
});

it('reads every checkout result of the bridge as meant', async () => {
  const results: Record<string, CheckoutResult> = {
    completed: { status: 'completed', orderId: 'md-1' },
    canceled: { status: 'canceled', orderId: 'md-1' },
    unknown: { status: 'unknown', orderId: 'md-1' },
    unknownAfterError: { status: 'unknown', orderId: 'md-1', error: { code: 'network', message: 'offline' } },
    declined: {
      status: 'failed',
      orderId: 'md-1',
      error: { code: 'declined', message: 'Insufficient funds', declineCode: '116' },
    },
    gatewayError: {
      status: 'failed',
      orderId: 'md-1',
      error: { code: 'api', message: 'System error', httpStatus: 500, gatewayErrorCode: '5' },
    },
    sessionExpired: {
      status: 'failed',
      orderId: 'md-1',
      error: { code: 'session_expired', message: 'The session of order md-1 expired' },
    },
    invalidRequest: {
      status: 'failed',
      orderId: '',
      error: { code: 'invalid_request', message: 'theme.colors.onPrimary must be #RRGGBB or #AARRGGBB, not white' },
    },
    busy: { status: 'failed', orderId: '', error: { code: 'busy', message: 'A checkout is already open' } },
  };
  const sent: Record<string, unknown> = checkoutResults;

  expect(Object.keys(results).sort()).toEqual(Object.keys(sent).sort());
  for (const [name, reply] of Object.entries(sent)) {
    const result = await readFrom(reply, () => new PayrdrCheckout({ baseUrl: 'u' }).present('md-1'));
    expect([name, result]).toEqual([name, results[name]]);
  }
});

it('keeps every error code of the bridge', async () => {
  for (const code of errorCodes) {
    const result = await readFrom({ status: 'failed', orderId: 'md-1', error: { code } }, () =>
      new PayrdrCheckout({ baseUrl: 'u' }).present('md-1'),
    );
    expect(result.status === 'failed' && result.error.code).toBe(code);
  }
});

it('sends every SDK Core request as the bridge reads it', async () => {
  const card = { pan: '4532896701439077', expiry: '12/28', cvc: '444', pubKey: 'PEM' };
  const sends: Record<string, () => Promise<unknown>> = {
    card: () => PayrdrCore.generateToken({ type: 'card', mdOrder: 'md-1', ...card, cardholder: 'JOHN DOE' }),
    cardWithoutOrder: () => PayrdrCore.generateToken({ type: 'cardWithoutOrder', ...card }),
    savedCard: () => PayrdrCore.generateToken({ type: 'savedCard', mdOrder: 'md-1', bindingId: 'b-1', cvc: '444', pubKey: 'PEM' }),
    savedCardWithoutOrder: () => PayrdrCore.generateToken({ type: 'savedCardWithoutOrder', bindingId: 'b-1', pubKey: 'PEM' }),
  };
  const expected: Record<string, unknown> = coreTokenRequests;

  expect(Object.keys(sends).sort()).toEqual(Object.keys(expected).sort());
  for (const [name, send] of Object.entries(sends)) {
    expect([name, await sentBy(send, { token: 'tok' })]).toEqual([name, expected[name]]);
  }
});

it('reads every SDK Core answer of the bridge as meant', async () => {
  const read = (reply: unknown) =>
    readFrom(reply, () => PayrdrCore.generateToken({ type: 'savedCardWithoutOrder', bindingId: 'b-1', pubKey: 'PEM' }));

  expect(Object.keys(coreTokenResults).sort()).toEqual(['fieldErrors', 'invalidRequest', 'token']);
  expect(await read(coreTokenResults.token)).toEqual({ token: 'tok', errors: {} });
  expect(await read(coreTokenResults.fieldErrors)).toEqual({ errors: { pan: 'required' } });
  await expect(read(coreTokenResults.invalidRequest)).rejects.toThrow(coreTokenResults.invalidRequest.error.message);
});
