import {
  api2Url,
  ApiV2SessionRegistrar,
  GatewayOrderRegistrar,
  merchantRegistrar,
  parseRegisterResponse,
  parseSessionResponse,
  type OrderType,
} from '../src/merchant/orderRegistrar';

/** `text` as UTF-8 bytes. */
const utf8 = (text: string) => Uint8Array.from(unescape(encodeURIComponent(text)), char => char.charCodeAt(0));

/** A fetch that answers `body` (as UTF-8 bytes, with no charset) and records what it was sent. */
function answering(body: string) {
  return jest.fn(async (_url: RequestInfo | URL, _init?: RequestInit) => ({
    arrayBuffer: async () => utf8(body).buffer,
  })) as unknown as jest.Mock & typeof fetch;
}

function registrar(
  fetch: typeof globalThis.fetch,
  options: { login?: string; timeoutMs?: number; orderType?: OrderType } = {},
) {
  return new GatewayOrderRegistrar({
    baseUrl: 'https://gateway.test/payment/',
    login: options.login ?? 'merchant-api',
    password: 'secret',
    fetch,
    timeoutMs: options.timeoutMs,
    orderType: options.orderType,
  });
}

describe('GatewayOrderRegistrar', () => {
  test('posts the order to register.do as a form and reads the orderId', async () => {
    const fetch = answering('{"orderId":"md-1","formUrl":"https://gateway.test/form"}');
    await expect(registrar(fetch).register(1578, 'demo-1234')).resolves.toEqual({
      kind: 'registered',
      mdOrder: 'md-1',
    });
    const [url, init] = fetch.mock.calls[0];
    expect(url).toBe('https://gateway.test/payment/rest/register.do');
    expect(init.method).toBe('POST');
    expect(init.headers['Content-Type']).toMatch(/^application\/x-www-form-urlencoded/);
    const form = new URLSearchParams(init.body);
    expect(Object.fromEntries(form)).toEqual({
      userName: 'merchant-api',
      password: 'secret',
      amount: '1578',
      currency: '978',
      orderNumber: expect.stringMatching(/^demo-[0-9a-f]{20}$/),
      returnUrl: 'sdk://done',
      clientId: 'demo-1234',
    });
  });

  test('a guest order carries no clientId', async () => {
    const fetch = answering('{"orderId":"md-2"}');
    await registrar(fetch).register(100, undefined);
    expect(new URLSearchParams(fetch.mock.calls[0][1].body).has('clientId')).toBe(false);
  });

  test('sends nothing without credentials', async () => {
    const fetch = answering('{}');
    await expect(registrar(fetch, { login: '  ' }).register(100, undefined)).resolves.toEqual({
      kind: 'missingCredentials',
    });
    expect(fetch).not.toHaveBeenCalled();
  });

  test('reads the reply as UTF-8', async () => {
    const fetch = answering('{"errorCode":"1","errorMessage":"Заказ с таким номером уже обработан"}');
    await expect(registrar(fetch).register(100, undefined)).resolves.toEqual({
      kind: 'failed',
      message: 'Заказ с таким номером уже обработан',
    });
  });

  test('a network error becomes its message', async () => {
    const fetch = jest.fn().mockRejectedValue(new TypeError('Network request failed'));
    await expect(registrar(fetch).register(100, undefined)).resolves.toEqual({
      kind: 'failed',
      message: 'Network request failed',
    });
  });

  test('gives up when the gateway does not answer in time', async () => {
    jest.useFakeTimers();
    const fetch = jest.fn(
      (_url: string, init: RequestInit) =>
        new Promise((_, reject) => init.signal?.addEventListener('abort', () => reject(new Error('Aborted')))),
    );
    const result = registrar(fetch as unknown as typeof globalThis.fetch, { timeoutMs: 20_000 }).register(
      100,
      undefined,
    );
    jest.advanceTimersByTime(20_000);
    await expect(result).resolves.toEqual({ kind: 'failed', message: 'The gateway did not answer in 20 s' });
    jest.useRealTimers();
  });
});

describe('parseRegisterResponse', () => {
  test.each([
    ['{"orderId":"abc"}', { kind: 'registered', mdOrder: 'abc' }],
    ['{"errorCode":"5","errorMessage":"Access denied"}', { kind: 'failed', message: 'Access denied' }],
    ['{"errorCode":"7"}', { kind: 'failed', message: 'Gateway error 7' }],
    ['<html>502</html>', { kind: 'failed', message: 'Unexpected gateway response' }],
    ['["orderId"]', { kind: 'failed', message: 'Unexpected gateway response' }],
  ])('%s', (body, expected) => {
    expect(parseRegisterResponse(body)).toEqual(expected);
  });
});

// ── AFT transfers (registerP2P) ──────────────────────────────────────────

describe('GatewayOrderRegistrar, AFT transfer', () => {
  const transferReply = '{"errorCode":0,"errorMessage":"Successful","orderId":"md-aft","formUrl":"https://x"}';

  test('an AFT order is registered as a one-leg transfer that returns to the SDK', async () => {
    const fetch = answering(transferReply);
    await expect(registrar(fetch, { orderType: 'AFT' }).register(100, 'demo-1234')).resolves.toEqual({
      kind: 'registered',
      mdOrder: 'md-aft',
    });
    const [url, init] = fetch.mock.calls[0];
    expect(url).toBe('https://gateway.test/payment/rest/api/p2p/registerP2P.do');
    expect(init.method).toBe('POST');
    expect(init.headers).toEqual({ 'Content-Type': 'application/json' });
    expect(JSON.parse(init.body)).toEqual({
      username: 'merchant-api',
      password: 'secret',
      amount: 100,
      currency: '978',
      orderNumber: expect.stringMatching(/^demo-[0-9a-f]{20}$/),
      returnUrl: 'sdk://done',
      transactionTypeIndicator: 'A',
      features: { feature: ['WITHOUT_TO_CARD'] },
      clientId: 'demo-1234',
    });
  });

  test('a guest transfer carries no clientId', async () => {
    const fetch = answering(transferReply);
    await registrar(fetch, { orderType: 'AFT' }).register(100, undefined);
    expect(JSON.parse(fetch.mock.calls[0][1].body)).not.toHaveProperty('clientId');
  });

  test('a refused transfer gives the gateway message', async () => {
    const fetch = answering('{"errorCode":14,"errorMessage":"Features are invalid"}');
    await expect(registrar(fetch, { orderType: 'AFT' }).register(100, undefined)).resolves.toEqual({
      kind: 'failed',
      message: 'Features are invalid',
    });
  });

  test('sends nothing without credentials', async () => {
    const fetch = answering(transferReply);
    await expect(registrar(fetch, { login: '', orderType: 'AFT' }).register(100, undefined)).resolves.toEqual({
      kind: 'missingCredentials',
    });
    expect(fetch).not.toHaveBeenCalled();
  });

  test('a payment order stays on register.do', async () => {
    const fetch = answering('{"orderId":"md-1"}');
    await registrar(fetch, { orderType: 'PURCHASE' }).register(100, undefined);
    expect(fetch.mock.calls[0][0]).toBe('https://gateway.test/payment/rest/register.do');
  });
});

// ── API V2 ───────────────────────────────────────────────────────────────

// A session the dev gateway created, and the mdOrder of its paymentUrl.
const sessionId = 'ps_2GRSigK2sUQKFSAZWbg2z7UuQxxjz7wqCjZqYyWRiq22aF8xxCDPua';
const sessionMdOrder = '0180a01a-a8cb-75b8-8f9d-1a5f00ab8a11';
const sessionBody = `{"id":"${sessionId}","amount":1000,"status":"open","paymentUrl":"https://x?mdOrder=${sessionMdOrder}"}`;

function sessionRegistrar(fetch: typeof globalThis.fetch, options: { apiKey?: string; timeoutMs?: number } = {}) {
  return new ApiV2SessionRegistrar({
    baseUrl: 'https://gateway.test/payment/',
    apiKey: options.apiKey ?? 'key-1',
    fetch,
    timeoutMs: options.timeoutMs,
  });
}

describe('ApiV2SessionRegistrar', () => {
  test('a guest order creates a session that returns to the SDK', async () => {
    const fetch = answering(sessionBody);
    await expect(sessionRegistrar(fetch).register(157800, undefined)).resolves.toEqual({
      kind: 'registered',
      mdOrder: sessionMdOrder,
      sessionId,
    });
    const [url, init] = fetch.mock.calls[0];
    expect(url).toBe('https://gateway.test/api2/sessions');
    expect(init.method).toBe('POST');
    expect(init.headers).toEqual({ 'X-API-KEY': 'key-1', 'Content-Type': 'application/json' });
    expect(JSON.parse(init.body)).toEqual({
      amount: 157800,
      currency: 'EUR',
      successUrl: 'sdk://done',
      failureUrl: 'sdk://done',
    });
  });

  test('a buyer with cards is the merchantCustomerId', async () => {
    const fetch = answering(sessionBody);
    await sessionRegistrar(fetch).register(100, 'demo-1234');
    expect(JSON.parse(fetch.mock.calls[0][1].body).merchantCustomerId).toBe('demo-1234');
  });

  test('sends nothing without an API key', async () => {
    const fetch = answering(sessionBody);
    await expect(sessionRegistrar(fetch, { apiKey: ' ' }).register(100, undefined)).resolves.toEqual({
      kind: 'missingApiKey',
    });
    expect(fetch).not.toHaveBeenCalled();
  });

  test('a network error becomes its message', async () => {
    const fetch = jest.fn().mockRejectedValue(new TypeError('Network request failed'));
    await expect(sessionRegistrar(fetch).register(100, undefined)).resolves.toEqual({
      kind: 'failed',
      message: 'Network request failed',
    });
  });

  test('gives up when the gateway does not answer in time', async () => {
    jest.useFakeTimers();
    const fetch = jest.fn(
      (_url: string, init: RequestInit) =>
        new Promise((_, reject) => init.signal?.addEventListener('abort', () => reject(new Error('Aborted')))),
    );
    const result = sessionRegistrar(fetch as unknown as typeof globalThis.fetch, { timeoutMs: 20_000 }).register(
      100,
      undefined,
    );
    jest.advanceTimersByTime(20_000);
    await expect(result).resolves.toEqual({ kind: 'failed', message: 'The gateway did not answer in 20 s' });
    jest.useRealTimers();
  });

  test('the API key shows neither in its text nor in its JSON', () => {
    const sessions = sessionRegistrar(answering(sessionBody), { apiKey: 'key-secret' });
    expect(String(sessions)).toBe('ApiV2SessionRegistrar(baseUrl=https://gateway.test/payment/)');
    expect(JSON.stringify(sessions)).not.toContain('key-secret');
  });
});

describe('api2Url', () => {
  test.each([
    ['https://gateway.test/payment', 'https://gateway.test/api2'],
    ['https://gateway.test/payment/', 'https://gateway.test/api2'],
    [' https://gateway.test:443/payment ', 'https://gateway.test/api2'],
    ['http://gateway.test:80/payment', 'http://gateway.test/api2'],
    ['https://gateway.test:8443/payment', 'https://gateway.test:8443/api2'],
    ['HTTPS://Gateway.test/payment?x=1', 'https://Gateway.test/api2'],
  ])('%s', (baseUrl, expected) => {
    expect(api2Url(baseUrl)).toBe(expected);
  });

  test('a base URL without a host has no API V2', async () => {
    expect(() => api2Url('gateway.test/payment')).toThrow('gateway.test/payment');
    const fetch = answering(sessionBody);
    const hostless = new ApiV2SessionRegistrar({ baseUrl: 'gateway.test', apiKey: 'key-1', fetch });
    await expect(hostless.register(100, undefined)).resolves.toMatchObject({ kind: 'failed' });
    expect(fetch).not.toHaveBeenCalled();
  });
});

describe('parseSessionResponse', () => {
  test.each([
    [sessionBody, { kind: 'registered', mdOrder: sessionMdOrder, sessionId }],
    [
      '{"error":{"errorCode":"unauthorized","errorMessage":"Unknown user"}}',
      { kind: 'failed', message: 'Unknown user' },
    ],
    ['{"error":{"errorCode":"unauthorized"}}', { kind: 'failed', message: 'Gateway error unauthorized' }],
    ['{"id":"not-a-session"}', { kind: 'failed', message: 'Unexpected session id not-a-session' }],
    ['<html>502</html>', { kind: 'failed', message: 'Unexpected gateway response' }],
  ])('%s', (body, expected) => {
    expect(parseSessionResponse(body)).toEqual(expected);
  });
});

describe('merchantRegistrar', () => {
  const settings = { baseUrl: 'https://gateway.test/payment', login: 'merchant-api', password: 'secret', apiKey: '' };

  test('API V1 registers with the login and password, API V2 with the key', () => {
    expect(merchantRegistrar({ ...settings, apiVersion: 'V1' })).toBeInstanceOf(GatewayOrderRegistrar);
    expect(merchantRegistrar({ ...settings, apiVersion: 'V2' })).toBeInstanceOf(ApiV2SessionRegistrar);
  });

  test('the order type reaches API V1 only', async () => {
    const transfers = answering('{"orderId":"md-aft"}');
    await merchantRegistrar({ ...settings, apiVersion: 'V1', orderType: 'AFT', fetch: transfers }).register(
      100,
      undefined,
    );
    expect(transfers.mock.calls[0][0]).toBe('https://gateway.test/payment/rest/api/p2p/registerP2P.do');

    const sessions = answering(sessionBody);
    const v2 = { ...settings, apiKey: 'key-1', apiVersion: 'V2' as const, orderType: 'AFT' as const, fetch: sessions };
    expect(merchantRegistrar(v2)).toBeInstanceOf(ApiV2SessionRegistrar);
    await merchantRegistrar(v2).register(100, undefined);
    expect(sessions.mock.calls[0][0]).toBe('https://gateway.test/api2/sessions');
  });

  test('API V2 ignores the login and password', async () => {
    const fetch = answering(sessionBody);
    await expect(merchantRegistrar({ ...settings, apiVersion: 'V2', fetch }).register(100, undefined)).resolves.toEqual(
      { kind: 'missingApiKey' },
    );
  });
});
