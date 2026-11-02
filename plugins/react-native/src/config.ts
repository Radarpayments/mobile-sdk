/** What the payment sheet looks like and which gateway it pays through: the React Native face of `SdkPaymentConfig`. */
export interface PayrdrConfig {
  /** Payment gateway base URL, e.g. `https://pay.example.com/payment`. */
  baseUrl: string;
  /** Language of the sheet, a tag such as `de` or `ar`; absent follows the device. */
  locale?: string;
  /** Colours and corners of the sheet; default `{ mode: 'system' }`. */
  theme?: PayrdrTheme;
  /** Shows Google Pay (Android) when the gateway offers it for the order. */
  googlePay?: GooglePayOptions;
  /** Shows Apple Pay (iOS) when the gateway offers it for the order. */
  applePay?: ApplePayOptions;
  /** Puts the camera scan icon on the card form. */
  cardScanner?: boolean;
  /** The sheet shows its own success / declined screen before it returns. */
  showResultScreen?: boolean;
  /** Where the gateway's public keys come from; absent = `{baseUrl}/se/keys.do`. */
  keyProviderUrl?: string;
  /** Certificate pins for the gateway's hosts; absent = the system trust store. */
  sslPins?: SslPin[];
  /** Writes the SDK's log to logcat / the Xcode console. */
  debugLogging?: boolean;
}

/**
 * A stock theme, or `custom`: starts from the `base` palette and changes only the colours given and the corners set (in
 * points; a large `buttonCornerRadius` such as 999 makes pill buttons).
 */
export type PayrdrTheme =
  | { mode: 'system' }
  | { mode: 'light' }
  | { mode: 'dark' }
  | {
      mode: 'custom';
      base?: 'light' | 'dark';
      colors?: PayrdrColors;
      cornerRadius?: number;
      buttonCornerRadius?: number;
    };

/** Colours of a custom theme as CSS hex: `#RGB`, `#RRGGBB` or `#RRGGBBAA`. Absent keeps the base palette's colour. */
export interface PayrdrColors {
  /** The pay button and the selected items. */
  primary?: string;
  onPrimary?: string;
  /** The sheet. */
  background?: string;
  onBackground?: string;
  /** Input fields and cards inside the sheet. */
  surface?: string;
  onSurface?: string;
  error?: string;
  onError?: string;
  outline?: string;
  success?: string;
  /** Secondary text. */
  onSurfaceVariant?: string;
  placeholder?: string;
  outlineStrong?: string;
  /** The dimming behind the sheet. */
  scrim?: string;
}

export type GooglePayCardNetwork = 'AMEX' | 'DISCOVER' | 'INTERAC' | 'JCB' | 'MASTERCARD' | 'VISA';

/** Google Pay on Android. Every field may be left out: the SDK then takes it from the gateway's settings and the order. */
export interface GooglePayOptions {
  merchantId?: string;
  /** Shown on the Google Pay sheet; absent = the merchant name of the order. */
  merchantName?: string;
  gateway?: string;
  gatewayMerchantId?: string;
  environment?: 'TEST' | 'PRODUCTION';
  /** ISO 3166-1 alpha-2, e.g. `DE`; absent = the merchant's country. */
  countryCode?: string;
  /** Absent = Visa and Mastercard. */
  cardNetworks?: GooglePayCardNetwork[];
  /** Absent = both. */
  authMethods?: Array<'PAN_ONLY' | 'CRYPTOGRAM_3DS'>;
  billingAddressRequired?: boolean;
}

export type ApplePayNetwork = 'AMEX' | 'DISCOVER' | 'JCB' | 'MASTERCARD' | 'VISA';

/** Apple Pay on iOS. Only the Apple merchant id is needed; the country comes from the order unless set. */
export interface ApplePayOptions {
  /** `merchant.…`, the id in the app's Apple Pay entitlement. */
  merchantId: string;
  countryCode?: string;
  /** Absent = Visa and Mastercard. */
  networks?: ApplePayNetwork[];
  /** Absent = 3-D Secure, credit and debit. */
  capabilities?: Array<'THREE_D_SECURE' | 'EMV' | 'CREDIT' | 'DEBIT'>;
}

/** A certificate pin: the base64 SHA-256 of the host's public key (44 characters, no `sha256/` prefix). */
export interface SslPin {
  host: string;
  sha256: string;
}

/** The config as the native side reads it: colours as `#AARRGGBB`. */
export function configToWire(config: PayrdrConfig): Record<string, unknown> {
  const theme = config.theme ?? { mode: 'system' };
  return {
    ...config,
    theme:
      theme.mode === 'custom'
        ? {
            ...theme,
            colors: Object.fromEntries(
              Object.entries(theme.colors ?? {}).map(([name, css]) => [name, argbHex(name, css)]),
            ),
          }
        : theme,
  };
}

/** CSS `#RGB` / `#RRGGBB` / `#RRGGBBAA` as `#AARRGGBB`. */
function argbHex(name: string, css: string): string {
  const hex = css.replace(/^#/, '');
  if (!/^[0-9a-fA-F]+$/.test(hex) || ![3, 6, 8].includes(hex.length)) {
    throw new Error(`theme.colors.${name} must be #RGB, #RRGGBB or #RRGGBBAA, not ${css}`);
  }
  const rgb = hex.length === 3 ? [...hex.slice(0, 3)].map((c) => c + c).join('') : hex.slice(0, 6);
  const alpha = hex.length === 8 ? hex.slice(6) : 'FF';
  return `#${alpha}${rgb}`.toUpperCase();
}
