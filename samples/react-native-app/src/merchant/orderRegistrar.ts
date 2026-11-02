import { randomHex } from '../randomHex';
import { sessionIdToMdOrder } from './sessionId';
import { decodeUtf8 } from './utf8';

/**
 * "Your server": registers an order at the gateway and hands back its mdOrder (API V1) or its session (API V2).
 *
 * In a real integration this code lives on the merchant's backend, which keeps the merchant login and password or API
 * key; the app only receives the order. The demo registers orders from the app so that it works without a backend —
 * never ship merchant credentials inside an app.
 */
export interface OrderRegistrar {
  /** Registers an order for `amountMinor` euro cents; `clientId` links it to a buyer's saved cards, undefined = guest. */
  register(amountMinor: number, clientId: string | undefined): Promise<OrderRegistration>;
}

/**
 * Outcome of `OrderRegistrar.register`:
 * - `registered`: the gateway accepted the order; pass `sessionId` to the SDK when there is one (`presentSession`,
 *   API V2), `mdOrder` otherwise (`present`, API V1). An API V2 session has an mdOrder too, the one its id encodes;
 * - `missingCredentials`: login or password is not set in the developer settings; nothing was sent;
 * - `missingApiKey`: the API V2 key is not set in the developer settings; nothing was sent;
 * - `failed`: the gateway refused the order or could not be reached.
 */
export type OrderRegistration =
  | RegisteredOrder
  | { kind: 'missingCredentials' }
  | { kind: 'missingApiKey' }
  | { kind: 'failed'; message: string };

export interface RegisteredOrder {
  kind: 'registered';
  mdOrder: string;
  sessionId?: string;
}

/** The merchant API a demo order is registered through. */
export const apiVersions = ['V1', 'V2'] as const;

export type ApiVersion = (typeof apiVersions)[number];

/** What a demo order is: a payment, or a one-leg AFT transfer (API V1 `registerP2P.do`). */
export const orderTypes = ['PURCHASE', 'AFT'] as const;

export type OrderType = (typeof orderTypes)[number];

export interface GatewayOrderRegistrarOptions {
  baseUrl: string;
  login: string;
  password: string;
  /** A payment (`register.do`) unless told otherwise. */
  orderType?: OrderType;
  /** Details a transfer is registered with; ignored for a payment. */
  prefill?: TransferPrefill;
  /** Tests pass a fake. */
  fetch?: typeof fetch;
  timeoutMs?: number;
}

/**
 * Details a transfer is registered with so that the sheet does not ask for them: the buyer's `email`, and
 * `billingPayerData` / `billingRecipientData` as `registerP2P.do` takes them (gateway field name → value).
 */
export interface TransferPrefill {
  email?: string;
  billingPayerData?: Record<string, string>;
  billingRecipientData?: Record<string, string>;
}

const EUR = '978';

const isEmpty = (fields: Record<string, string> | undefined) => !fields || Object.keys(fields).length === 0;
const RETURN_URL = 'sdk://done';
const TIMEOUT_MS = 20_000;

/**
 * Registers orders through API V1 of the gateway at `baseUrl`, in euros: a payment with `register.do`, an AFT transfer
 * with `registerP2P.do` (`orderType`); gives up after `timeoutMs`.
 */
export class GatewayOrderRegistrar implements OrderRegistrar {
  private readonly options: Required<Omit<GatewayOrderRegistrarOptions, 'prefill'>>;

  /** Details a transfer is registered with, if any. */
  readonly prefill?: TransferPrefill;

  constructor(options: GatewayOrderRegistrarOptions) {
    this.prefill = options.prefill;
    this.options = {
      ...options,
      fetch: options.fetch ?? ((input, init) => fetch(input, init)),
      orderType: options.orderType ?? 'PURCHASE',
      timeoutMs: options.timeoutMs ?? TIMEOUT_MS,
    };
  }

  async register(amountMinor: number, clientId: string | undefined): Promise<OrderRegistration> {
    const { baseUrl, login, password, orderType } = this.options;
    if (!login.trim() || !password.trim()) return { kind: 'missingCredentials' };
    if (orderType === 'AFT') return this.registerTransfer(amountMinor, clientId);
    const fields: Record<string, string> = {
      userName: login,
      password,
      amount: `${amountMinor}`,
      currency: EUR,
      orderNumber: newOrderNumber(),
      returnUrl: RETURN_URL,
      ...(clientId === undefined ? {} : { clientId }),
    };
    return callGateway(this.options, parseRegisterResponse, () => ({
      url: `${baseUrl.replace(/\/+$/, '')}/rest/register.do`,
      headers: { 'Content-Type': 'application/x-www-form-urlencoded; charset=utf-8' },
      body: Object.entries(fields)
        .map(([name, value]) => `${encodeURIComponent(name)}=${encodeURIComponent(value)}`)
        .join('&'),
    }));
  }

  /**
   * A one-leg AFT transfer (`WITHOUT_TO_CARD`, transaction type `A`): the buyer's card is debited, the money goes to
   * the recipient; the sheet asks for the sender and recipient details the gateway needs.
   */
  private registerTransfer(amountMinor: number, clientId: string | undefined): Promise<OrderRegistration> {
    const { baseUrl, login, password } = this.options;
    const order = {
      username: login,
      password,
      amount: amountMinor,
      currency: EUR,
      orderNumber: newOrderNumber(),
      returnUrl: RETURN_URL,
      transactionTypeIndicator: 'A',
      features: { feature: ['WITHOUT_TO_CARD'] },
      ...(clientId === undefined ? {} : { clientId }),
      ...(this.prefill?.email === undefined ? {} : { email: this.prefill.email }),
      ...(isEmpty(this.prefill?.billingPayerData) ? {} : { billingPayerData: this.prefill?.billingPayerData }),
      ...(isEmpty(this.prefill?.billingRecipientData) ? {} : { billingRecipientData: this.prefill?.billingRecipientData }),
    };
    return callGateway(this.options, parseRegisterResponse, () => ({
      url: `${baseUrl.replace(/\/+$/, '')}/rest/api/p2p/registerP2P.do`,
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(order),
    }));
  }
}

export interface ApiV2SessionRegistrarOptions {
  baseUrl: string;
  apiKey: string;
  /** Tests pass a fake. */
  fetch?: typeof fetch;
  timeoutMs?: number;
}

/**
 * Creates API V2 sessions (`POST /api2/sessions`) at the gateway of `baseUrl` with the merchant's API key, in euros;
 * gives up after `timeoutMs`. The buyer comes back from the bank page to `sdk://done`, which the SDK expects (the
 * merchant's own pages work too).
 */
export class ApiV2SessionRegistrar implements OrderRegistrar {
  private readonly options: Required<Omit<ApiV2SessionRegistrarOptions, 'apiKey'>>;
  // Private to the class: neither a log of the registrar nor its JSON shows the key.
  readonly #apiKey: string;

  constructor({ apiKey, ...options }: ApiV2SessionRegistrarOptions) {
    this.#apiKey = apiKey;
    this.options = {
      ...options,
      fetch: options.fetch ?? ((input, init) => fetch(input, init)),
      timeoutMs: options.timeoutMs ?? TIMEOUT_MS,
    };
  }

  async register(amountMinor: number, clientId: string | undefined): Promise<OrderRegistration> {
    if (!this.#apiKey.trim()) return { kind: 'missingApiKey' };
    const session = {
      amount: amountMinor,
      currency: 'EUR',
      successUrl: RETURN_URL,
      failureUrl: RETURN_URL,
      // API V2's name for the buyer whose cards the gateway keeps (API V1's clientId).
      ...(clientId === undefined ? {} : { merchantCustomerId: clientId }),
    };
    return callGateway(this.options, parseSessionResponse, () => ({
      url: `${api2Url(this.options.baseUrl)}/sessions`,
      headers: { 'X-API-KEY': this.#apiKey, 'Content-Type': 'application/json' },
      body: JSON.stringify(session),
    }));
  }

  toString(): string {
    return `ApiV2SessionRegistrar(baseUrl=${this.options.baseUrl})`;
  }
}

export interface MerchantRegistrarOptions {
  apiVersion: ApiVersion;
  baseUrl: string;
  login: string;
  password: string;
  apiKey: string;
  orderType?: OrderType;
  /** A transfer's details, see `TransferPrefill`. */
  prefill?: TransferPrefill;
  /** Tests pass a fake. */
  fetch?: typeof fetch;
}

/**
 * The registrar for the API version and order type chosen in the developer settings; transfers go through API V1 and
 * carry `prefill` when there is one.
 */
export function merchantRegistrar({
  apiVersion,
  baseUrl,
  login,
  password,
  apiKey,
  orderType,
  prefill,
  fetch,
}: MerchantRegistrarOptions): OrderRegistrar {
  switch (apiVersion) {
    case 'V1':
      return new GatewayOrderRegistrar({ baseUrl, login, password, orderType, prefill, fetch });
    case 'V2':
      return new ApiV2SessionRegistrar({ baseUrl, apiKey, fetch });
  }
}

/**
 * API V2 is served next to the payment page: `https://host/payment` → `https://host/api2`. Throws for a base URL
 * without a scheme and host.
 */
export function api2Url(baseUrl: string): string {
  // React Native's URL has no getters for the parts, hence by hand.
  const match = /^([a-z][a-z\d+.-]*):\/\/(?:[^@/?#]*@)?(\[[^\]]*\]|[^:/?#]+)(?::(\d*))?(?:[/?#]|$)/i.exec(
    baseUrl.trim(),
  );
  if (!match) throw new Error(`Not a gateway URL: ${baseUrl}`);
  const [, scheme, host, port] = match;
  const protocol = scheme.toLowerCase();
  const defaultPort = protocol === 'https' ? '443' : protocol === 'http' ? '80' : undefined;
  return `${protocol}://${host}${port && port !== defaultPort ? `:${port}` : ''}/api2`;
}

/** One request of a gateway call: built inside the call, so that a mistake in it fails the order and nothing else. */
interface GatewayRequest {
  url: string;
  headers: Record<string, string>;
  body: string;
}

/** One gateway call of "your server", given up after `timeoutMs`: a stalled DNS lookup must not keep "Pay" busy. */
async function callGateway(
  { fetch, timeoutMs }: { fetch: typeof globalThis.fetch; timeoutMs: number },
  parse: (body: string) => OrderRegistration,
  request: () => GatewayRequest,
): Promise<OrderRegistration> {
  const abort = new AbortController();
  const timer = setTimeout(() => abort.abort(), timeoutMs);
  try {
    const { url, headers, body } = request();
    const response = await fetch(url, { method: 'POST', headers, body, signal: abort.signal });
    // The gateway answers in UTF-8 but does not always say so; `text()` would follow a wrong charset.
    return parse(decodeUtf8(new Uint8Array(await response.arrayBuffer())));
  } catch (error) {
    if (abort.signal.aborted) return { kind: 'failed', message: `The gateway did not answer in ${timeoutMs / 1000} s` };
    return { kind: 'failed', message: error instanceof Error ? error.message : String(error) };
  } finally {
    clearTimeout(timer);
  }
}

// Gateways limit orderNumber to 32 characters.
function newOrderNumber(): string {
  return `demo-${randomHex(20)}`;
}

/** Reads a `register.do` / `registerP2P.do` response: `orderId` on success, `errorMessage` otherwise. */
export function parseRegisterResponse(body: string): OrderRegistration {
  const reply = parseObject(body);
  if (!reply) return { kind: 'failed', message: 'Unexpected gateway response' };
  if (reply.orderId != null) return { kind: 'registered', mdOrder: `${reply.orderId}` };
  if (reply.errorMessage != null) return { kind: 'failed', message: `${reply.errorMessage}` };
  return { kind: 'failed', message: `Gateway error ${reply.errorCode ?? ''}` };
}

/** Reads a `POST /api2/sessions` response: the session on success, `error.errorMessage` otherwise. */
export function parseSessionResponse(body: string): OrderRegistration {
  const reply = parseObject(body);
  if (!reply) return { kind: 'failed', message: 'Unexpected gateway response' };
  if (reply.id != null) {
    const sessionId = `${reply.id}`;
    const mdOrder = sessionIdToMdOrder(sessionId);
    return mdOrder === undefined
      ? { kind: 'failed', message: `Unexpected session id ${sessionId}` }
      : { kind: 'registered', mdOrder, sessionId };
  }
  const error = typeof reply.error === 'object' && reply.error !== null ? (reply.error as Record<string, unknown>) : {};
  if (error.errorMessage != null) return { kind: 'failed', message: `${error.errorMessage}` };
  return { kind: 'failed', message: `Gateway error ${error.errorCode ?? ''}` };
}

/** `body` as a JSON object; undefined for anything else. */
function parseObject(body: string): Record<string, unknown> | undefined {
  let json: unknown;
  try {
    json = JSON.parse(body);
  } catch {
    return undefined;
  }
  return typeof json === 'object' && json !== null && !Array.isArray(json)
    ? (json as Record<string, unknown>)
    : undefined;
}
