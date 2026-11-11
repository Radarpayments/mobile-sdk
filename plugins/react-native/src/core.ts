import NativePayrdrSdk from './NativePayrdrSdk';

/**
 * What SDK Core encrypts. `pubKey` is the gateway's public key in PEM; `expiry` is `MM/YY`.
 * - `card`: a new card for an order; `cardWithoutOrder`: a new card without one (e.g. to save it);
 * - `savedCard`: a saved card (its bindingId) for an order; `savedCardWithoutOrder`: without one.
 */
export type CoreTokenRequest =
  | { type: 'card'; mdOrder: string; pan: string; expiry: string; cvc: string; cardholder?: string; pubKey: string }
  | { type: 'cardWithoutOrder'; pan: string; expiry: string; cvc: string; cardholder?: string; pubKey: string }
  | { type: 'savedCard'; mdOrder: string; bindingId: string; cvc?: string; pubKey: string }
  | { type: 'savedCardWithoutOrder'; bindingId: string; cvc?: string; pubKey: string };

export type CoreField =
  | 'pan'
  | 'expiry'
  | 'cvc'
  | 'cardholder'
  | 'bindingId'
  | 'mdOrder'
  | 'pubKey'
  | 'storedPaymentId'
  | 'unknown';

/**
 * A token, or sdk-core's error code (`required`, `invalid`, `invalid-format`) for each field that is wrong.
 * The key `unknown` with the code `unknown` means the encryption failed.
 */
export interface CoreTokenResult {
  token?: string;
  errors: Partial<Record<CoreField, string>>;
}

const WIRE_TYPES = {
  card: 'card',
  cardWithoutOrder: 'cardInstant',
  savedCard: 'binding',
  savedCardWithoutOrder: 'bindingInstant',
} as const;

/**
 * SDK Core, for a merchant with PCI DSS who keeps its own card form: encrypts the card data on the device into a token
 * that your server passes to the gateway as `seToken`.
 */
export const PayrdrCore = {
  async generateToken(request: CoreTokenRequest): Promise<CoreTokenResult> {
    const wire = JSON.parse(await NativePayrdrSdk.coreToken(JSON.stringify({ ...request, type: WIRE_TYPES[request.type] }))) as {
      token?: string;
      errors?: Partial<Record<CoreField, string>>;
      error?: { message?: string };
    };
    if (wire.error) throw new Error(wire.error.message ?? 'Invalid SDK Core request');
    return wire.token ? { token: wire.token, errors: {} } : { errors: wire.errors ?? {} };
  },
};
