import { PayrdrCore, type CoreField, type CoreTokenRequest } from '@payrdr/react-native-sdk';

/** The three kinds of token. */
export type SdkCoreVariant = 'new' | 'instant' | 'stored';

export function variantOf(stored: string): SdkCoreVariant {
  return stored === 'instant' || stored === 'stored' ? stored : 'new';
}

/** Inputs of the SDK Core form; each sdk-core error is shown next to one of them. */
export type CoreInput = 'pan' | 'expiry' | 'cvc' | 'holder' | 'bindingId' | 'mdOrder' | 'pubKey';

/** The form field an sdk-core error belongs to; undefined for errors no field explains. */
export function coreInputOf(field: CoreField): CoreInput | undefined {
  switch (field) {
    case 'cardholder':
      return 'holder';
    case 'storedPaymentId':
    case 'unknown':
      return undefined;
    default:
      return field;
  }
}

/** What is typed into the form. Card data stay in memory only. */
export interface CoreForm {
  pan: string;
  expiry: string;
  cvc: string;
  holder: string;
  mdOrder: string;
  bindingId: string;
  pubKey: string;
}

/** The SDK Core request of `variant` from the form: the number without spaces, an empty name or CVC left out. */
export function coreRequest(variant: SdkCoreVariant, form: CoreForm): CoreTokenRequest {
  const pan = form.pan.replace(/ /g, '');
  const cardholder = form.holder.trim() || undefined;
  switch (variant) {
    case 'new':
      return {
        type: 'card',
        mdOrder: form.mdOrder,
        pan,
        expiry: form.expiry,
        cvc: form.cvc,
        cardholder,
        pubKey: form.pubKey,
      };
    case 'instant':
      return {
        type: 'cardWithoutOrder',
        pan,
        expiry: form.expiry,
        cvc: form.cvc,
        cardholder,
        pubKey: form.pubKey,
      };
    case 'stored':
      return {
        type: 'savedCard',
        mdOrder: form.mdOrder,
        bindingId: form.bindingId,
        cvc: form.cvc || undefined,
        pubKey: form.pubKey,
      };
  }
}

/** A token, or sdk-core's error code (`required`, `invalid`, `invalid-format`) by form input. */
export type CoreOutcome = { token: string } | { errors: Partial<Record<CoreInput, string>> };

/** Encrypts the form into the token the merchant's server sends to the gateway as `seToken`. */
export async function generateToken(request: CoreTokenRequest): Promise<CoreOutcome> {
  try {
    const result = await PayrdrCore.generateToken(request);
    if (result.token) return { token: result.token };
    const errors: Partial<Record<CoreInput, string>> = {};
    for (const [field, code] of Object.entries(result.errors) as Array<[CoreField, string]>) {
      const input = coreInputOf(field);
      if (input) errors[input] = code;
    }
    // An error no field explains (e.g. an unusable key) still needs a place: the key field.
    return { errors: Object.keys(errors).length ? errors : { pubKey: 'invalid' } };
  } catch {
    // The bridge refused the request as a whole, which here can only be the key.
    return { errors: { pubKey: 'invalid' } };
  }
}
