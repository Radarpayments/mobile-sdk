import { PayrdrCheckout, type CheckoutResult } from '@payrdr/react-native-sdk';

import { buildSdkConfig, paymentTheme, StoreCheckout, toOutcome, type SdkConfigInput } from '../src/checkout/checkout';
import type { OrderRegistrar, OrderRegistration } from '../src/merchant/orderRegistrar';
import { coverStyle } from '../src/store/theme/coverStyle';

describe('toOutcome', () => {
  test.each<[CheckoutResult, ReturnType<typeof toOutcome>]>([
    [
      { status: 'completed', orderId: 'md-1' },
      { kind: 'success', paymentId: 'md-1' },
    ],
    [{ status: 'canceled', orderId: 'md-1' }, { kind: 'cancelled' }],
    [
      {
        status: 'failed',
        orderId: 'md-1',
        error: { code: 'declined', message: 'Insufficient funds', declineCode: '51' },
      },
      { kind: 'declined', message: 'Insufficient funds' },
    ],
    [
      { status: 'failed', orderId: 'md-1', error: { code: 'network' } },
      { kind: 'declined', message: undefined },
    ],
    [
      { status: 'unknown', orderId: 'md-1' },
      { kind: 'unconfirmed', paymentId: 'md-1' },
    ],
    [
      { status: 'unknown', orderId: 'md-1', error: { code: 'three_ds_timeout' } },
      { kind: 'unconfirmed', paymentId: 'md-1' },
    ],
  ])('%j', (result, outcome) => {
    expect(toOutcome(result)).toEqual(outcome);
  });
});

describe('buildSdkConfig', () => {
  const input: SdkConfigInput = {
    baseUrl: 'https://gateway.test/payment',
    scenario: { buyerWithCards: false, wallet: true, scanner: true, sdkResultScreen: false },
    cover: coverStyle('coffee'),
    themeOverride: 'cover',
    locale: 'de',
    storeName: 'Roast Lane',
    googlePayMerchantId: ' ',
    googlePayGateway: 'example',
    applePayMerchantId: ' merchant.test ',
  };

  test('the sheet in the brand of the store: the coffee shop has a dark base and pill buttons', () => {
    expect(buildSdkConfig(input)).toEqual({
      baseUrl: 'https://gateway.test/payment',
      locale: 'de',
      theme: {
        mode: 'custom',
        base: 'dark',
        colors: {
          primary: '#F3D9B1',
          onPrimary: '#17110E',
          background: '#17110E',
          onBackground: '#FEF3C7',
          surface: '#2E231E',
          onSurface: '#FEF3C7',
          outline: '#5B4A40',
          onSurfaceVariant: '#D6C3A5',
          placeholder: '#FEF3C780',
          outlineStrong: '#5B4A40',
        },
        cornerRadius: 18,
        buttonCornerRadius: 999,
      },
      googlePay: { merchantId: undefined, merchantName: 'Roast Lane', gateway: 'example' },
      applePay: { merchantId: 'merchant.test' },
      cardScanner: true,
      showResultScreen: false,
    });
  });

  test('the furniture store keeps its own corners on buttons', () => {
    expect(paymentTheme(coverStyle('furniture'))).toMatchObject({
      base: 'light',
      cornerRadius: 12,
      buttonCornerRadius: 12,
    });
  });

  test('a stock theme from the developer settings replaces the brand', () => {
    expect(buildSdkConfig({ ...input, themeOverride: 'system' }).theme).toEqual({ mode: 'system' });
  });

  test('no wallet when the scenario turns it off or Apple Pay has no merchant id', () => {
    const off = buildSdkConfig({ ...input, scenario: { ...input.scenario, wallet: false } });
    expect(off.googlePay).toBeUndefined();
    expect(off.applePay).toBeUndefined();
    expect(buildSdkConfig({ ...input, applePayMerchantId: '' }).applePay).toBeUndefined();
  });
});

describe('StoreCheckout', () => {
  const config = { baseUrl: 'https://gateway.test/payment' };

  function deferred<T>() {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>(done => (resolve = done));
    return { promise, resolve };
  }

  test('registers, opens the sheet and maps the result; a second tap meanwhile does nothing', async () => {
    const registration = deferred<OrderRegistration>();
    const sheet = deferred<CheckoutResult>();
    const registrar: OrderRegistrar = { register: jest.fn(() => registration.promise) };
    const present = jest.fn(() => sheet.promise);
    const onMdOrder = jest.fn();
    const checkout = new StoreCheckout(onMdOrder, present);

    const first = checkout.pay({ registrar, config, amountMinor: 390, clientId: 'demo-1' });
    expect(checkout.state).toEqual({ step: 'registering' });
    await expect(checkout.pay({ registrar, config, amountMinor: 390, clientId: 'demo-1' })).resolves.toBeUndefined();

    registration.resolve({ kind: 'registered', mdOrder: 'md-1' });
    await Promise.resolve();
    expect(checkout.state).toEqual({ step: 'inSheet' });
    expect(checkout.busy).toBe(true);
    await expect(checkout.pay({ registrar, config, amountMinor: 390, clientId: 'demo-1' })).resolves.toBeUndefined();

    sheet.resolve({ status: 'completed', orderId: 'md-1' });
    await expect(first).resolves.toEqual({ kind: 'success', paymentId: 'md-1' });
    expect(checkout.state).toEqual({ step: 'idle' });
    expect(registrar.register).toHaveBeenCalledTimes(1);
    expect(registrar.register).toHaveBeenCalledWith(390, 'demo-1');
    expect(present).toHaveBeenCalledWith(config, { kind: 'registered', mdOrder: 'md-1' });
    expect(onMdOrder).toHaveBeenCalledWith('md-1');
  });

  test('an API V2 session is launched as a session and reported by its mdOrder', async () => {
    const session: OrderRegistration = { kind: 'registered', mdOrder: 'md-1', sessionId: 'ps_1' };
    const present = jest.fn(async () => ({ status: 'completed', orderId: 'md-1' } as const));
    const onMdOrder = jest.fn();
    await new StoreCheckout(onMdOrder, present).pay({
      registrar: { register: async () => session },
      config,
      amountMinor: 100,
      clientId: undefined,
    });
    expect(present).toHaveBeenCalledWith(config, session);
    expect(onMdOrder).toHaveBeenCalledWith('md-1');
  });

  test.each<[string, OrderRegistration, 'present' | 'presentSession', string]>([
    ['an API V1 order opens by its mdOrder', { kind: 'registered', mdOrder: 'md-1' }, 'present', 'md-1'],
    [
      'an API V2 session opens by its id',
      { kind: 'registered', mdOrder: 'md-1', sessionId: 'ps_1' },
      'presentSession',
      'ps_1',
    ],
  ])('the SDK sheet: %s', async (_, registration, entry, argument) => {
    const sheet = {
      present: jest.fn(async () => ({ status: 'canceled', orderId: 'md-1' })),
      presentSession: jest.fn(async () => ({ status: 'canceled', orderId: 'md-1' })),
    };
    jest.mocked(PayrdrCheckout).mockImplementationOnce(() => sheet as unknown as PayrdrCheckout);
    await expect(
      new StoreCheckout(jest.fn()).pay({
        registrar: { register: async () => registration },
        config,
        amountMinor: 100,
        clientId: undefined,
      }),
    ).resolves.toEqual({ kind: 'cancelled' });
    expect(PayrdrCheckout).toHaveBeenLastCalledWith(config);
    expect(sheet[entry]).toHaveBeenCalledWith(argument);
    expect(sheet[entry === 'present' ? 'presentSession' : 'present']).not.toHaveBeenCalled();
  });

  test('a failed registration keeps the sheet closed and says why', async () => {
    const present = jest.fn();
    const checkout = new StoreCheckout(jest.fn(), present);
    const registrar: OrderRegistrar = { register: async () => ({ kind: 'missingCredentials' }) };
    await expect(checkout.pay({ registrar, config, amountMinor: 100, clientId: undefined })).resolves.toBeUndefined();
    expect(checkout.state).toEqual({ step: 'registrationFailed', reason: { kind: 'missingCredentials' } });
    expect(checkout.busy).toBe(false);
    expect(present).not.toHaveBeenCalled();
  });

  test('listeners hear every step', async () => {
    const checkout = new StoreCheckout(jest.fn(), async () => ({ status: 'canceled', orderId: 'md-1' }));
    const steps: string[] = [];
    checkout.subscribe(() => steps.push(checkout.state.step));
    await checkout.pay({
      registrar: { register: async () => ({ kind: 'registered', mdOrder: 'md-1' }) },
      config,
      amountMinor: 100,
      clientId: undefined,
    });
    expect(steps).toEqual(['registering', 'inSheet', 'idle']);
  });
});
