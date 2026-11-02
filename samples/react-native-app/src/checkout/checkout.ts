import { PayrdrCheckout, type CheckoutResult, type PayrdrConfig, type PayrdrTheme } from '@payrdr/react-native-sdk';

import { Listenable } from '../listenable';
import type { OrderRegistrar, OrderRegistration, RegisteredOrder } from '../merchant/orderRegistrar';
import type { Scenario, SheetThemeOverride } from '../store/model/scenario';
import type { CoverStyle } from '../store/theme/coverStyle';

/*
 * Everything this demo does with the payment SDK lives in this file. To take payments in your app:
 *
 *   1. Describe the checkout:         const config: PayrdrConfig = { baseUrl, theme, googlePay, applePay, ... }
 *   2. Get the order.                 Your backend registers it at the gateway — an mdOrder through API V1
 *      (register.do), a session id through API V2 (POST /api2/sessions); this demo does it in-app through
 *      OrderRegistrar only so that it runs without a backend.
 *   3. Open the payment sheet:        await new PayrdrCheckout(config).present(mdOrder)
 *                                     or .presentSession(sessionId)
 *   4. Handle the result:             completed, failed, canceled or unknown — see toOutcome().
 *
 * The app needs no native code and no manifest entries for it: MainActivity stays a plain ReactActivity,
 * AppDelegate is the one the React Native template made, and the SDK's Android libraries bring the INTERNET
 * and camera permissions and the Google Pay entry. iOS needs only what the features themselves need: the camera
 * usage text for the scanner and the Apple Pay entitlement.
 */

/**
 * What the store does after the payment sheet closes:
 * - `success`: paid; `paymentId` identifies the payment at the gateway;
 * - `declined`: the bank or the gateway refused the payment;
 * - `cancelled`: the buyer closed the sheet;
 * - `unconfirmed`: the SDK could not learn how the payment ended; the store must not offer to pay again until it knows.
 */
export type CheckoutOutcome =
  | { kind: 'success'; paymentId: string }
  | { kind: 'declined'; message?: string }
  | { kind: 'cancelled' }
  | { kind: 'unconfirmed'; paymentId: string };

/**
 * Where the store's "Pay" button is in the payment path: `idle` — nothing in progress; `registering` — step 2, the
 * order is being registered; `inSheet` — step 3, the payment sheet is open; `registrationFailed` — step 2 failed,
 * the sheet was not opened.
 */
export type CheckoutState =
  | { step: 'idle' }
  | { step: 'registering' }
  | { step: 'inSheet' }
  | { step: 'registrationFailed'; reason: Exclude<OrderRegistration, RegisteredOrder> };

export interface SdkConfigInput {
  baseUrl: string;
  scenario: Scenario;
  cover: CoverStyle;
  themeOverride: SheetThemeOverride;
  locale: string | undefined;
  storeName: string;
  googlePayMerchantId: string;
  googlePayGateway: string;
  applePayMerchantId: string;
}

/**
 * Step 1: the SDK config for the current demo panel, cover theme and developer settings.
 *
 * Each platform reads its own wallet: Google Pay on Android, Apple Pay on iOS. `googlePay: {}` is enough: the SDK
 * takes the rest from the gateway and the order. Here the developer settings may override two values (blank = from
 * the gateway), and the Google Pay sheet names the store the buyer is in (`storeName`) instead of the gateway's
 * merchant name: all the demo stores share one merchant. Apple Pay needs the merchant ID of the app's entitlement;
 * country and currency come from the order.
 */
export function buildSdkConfig(input: SdkConfigInput): PayrdrConfig {
  const { scenario } = input;
  const applePayMerchantId = input.applePayMerchantId.trim();
  return {
    baseUrl: input.baseUrl,
    locale: input.locale,
    theme: input.themeOverride === 'cover' ? paymentTheme(input.cover) : { mode: input.themeOverride },
    googlePay: scenario.wallet
      ? {
          merchantId: blankToUndefined(input.googlePayMerchantId),
          merchantName: input.storeName,
          gateway: blankToUndefined(input.googlePayGateway),
        }
      : undefined,
    applePay: scenario.wallet && applePayMerchantId ? { merchantId: applePayMerchantId } : undefined,
    cardScanner: scenario.scanner,
    showResultScreen: scenario.sdkResultScreen,
  };
}

function blankToUndefined(value: string): string | undefined {
  return value.trim() || undefined;
}

/**
 * The SDK sheet in the brand of `style`: colours and corners; typography stays the SDK's own. Error and success
 * colours come from the light or dark base palette.
 */
export function paymentTheme(style: CoverStyle): PayrdrTheme {
  return {
    mode: 'custom',
    base: style.dark ? 'dark' : 'light',
    colors: {
      primary: style.primary,
      onPrimary: style.onPrimary,
      background: style.background,
      onBackground: style.text,
      surface: style.fieldSurface,
      onSurface: style.text,
      outline: style.border,
      onSurfaceVariant: style.muted,
    },
    cornerRadius: style.radius,
    buttonCornerRadius: style.pillButtons ? 999 : style.radius,
  };
}

/** Step 4: turns the SDK result into what the store shows next. */
export function toOutcome(result: CheckoutResult): CheckoutOutcome {
  switch (result.status) {
    case 'completed':
      return { kind: 'success', paymentId: result.orderId };
    case 'canceled':
      return { kind: 'cancelled' };
    case 'failed':
      return { kind: 'declined', message: result.error.message };
    case 'unknown':
      // How the payment ended is not known yet: a real store checks the order on its server (or waits for the
      // gateway's callback) before telling the buyer anything, and never offers to pay again meanwhile.
      return { kind: 'unconfirmed', paymentId: result.orderId };
  }
}

/** Opens the sheet for the registered `order`; tests replace it. */
export type PresentSheet = (config: PayrdrConfig, order: RegisteredOrder) => Promise<CheckoutResult>;

/** An API V2 session by its id, an API V1 order by its mdOrder. */
const presentSheet: PresentSheet = (config, { mdOrder, sessionId }) => {
  const checkout = new PayrdrCheckout(config);
  return sessionId === undefined ? checkout.present(mdOrder) : checkout.presentSession(sessionId);
};

export interface PayRequest {
  registrar: OrderRegistrar;
  config: PayrdrConfig;
  amountMinor: number;
  /** The demo buyer with saved cards; undefined = guest. */
  clientId: string | undefined;
}

/**
 * Steps 2 and 3 for the store's "Pay" button: registers the order, then opens the sheet. A second tap while the
 * order is being registered or the sheet is open does nothing. `onMdOrder` sees the mdOrder of every registered
 * order, an API V2 session's too (the demo shows the last one in the developer settings).
 */
export class StoreCheckout extends Listenable {
  private current: CheckoutState = { step: 'idle' };

  constructor(
    private readonly onMdOrder: (mdOrder: string) => void,
    private readonly present: PresentSheet = presentSheet,
  ) {
    super();
  }

  /** Current step. */
  get state(): CheckoutState {
    return this.current;
  }

  get busy(): boolean {
    return this.current.step === 'registering' || this.current.step === 'inSheet';
  }

  /** Pays `amountMinor` euro cents. Gives the outcome once the sheet closes, undefined when it did not open. */
  async pay({ registrar, config, amountMinor, clientId }: PayRequest): Promise<CheckoutOutcome | undefined> {
    if (this.busy) return undefined;
    this.set({ step: 'registering' });
    const registration = await registrar.register(amountMinor, clientId);
    if (registration.kind !== 'registered') {
      this.set({ step: 'registrationFailed', reason: registration });
      return undefined;
    }
    this.onMdOrder(registration.mdOrder);
    this.set({ step: 'inSheet' });
    try {
      return toOutcome(await this.present(config, registration));
    } finally {
      this.set({ step: 'idle' });
    }
  }

  private set(state: CheckoutState): void {
    this.current = state;
    this.notify();
  }
}
