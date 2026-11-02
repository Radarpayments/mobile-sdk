import NativePayrdrSdk from './NativePayrdrSdk';
import { configToWire, type PayrdrConfig } from './config';
import { resultFromWire, type CheckoutResult } from './result';

/**
 * Opens the native payment sheet over the app and gives back how it ended.
 *
 * ```ts
 * const checkout = new PayrdrCheckout({ baseUrl: 'https://pay.example.com/payment' });
 * const result = await checkout.present(mdOrder);
 * switch (result.status) {
 *   case 'completed': showPaid(); break;
 *   case 'canceled': break;
 *   case 'failed': showError(result.error.message); break;
 *   case 'unknown': checkOnServer(result.orderId); break;
 * }
 * ```
 */
export class PayrdrCheckout {
  constructor(readonly config: PayrdrConfig) {}

  /**
   * Pays an order your server registered at the gateway (its mdOrder). Resolves once the sheet has closed — also when
   * it could not open (`failed` with `busy`, `no_screen` or `invalid_request`); rejects only for a colour in the config
   * that is not hex, a mistake in the app's code.
   */
  present(mdOrder: string): Promise<CheckoutResult> {
    return this.open({ mdOrder });
  }

  /** Pays an API v2 payment session (`ps_…`). */
  presentSession(sessionId: string): Promise<CheckoutResult> {
    return this.open({ sessionId });
  }

  private async open(order: { mdOrder: string } | { sessionId: string }): Promise<CheckoutResult> {
    const request = JSON.stringify({ config: configToWire(this.config), ...order });
    return resultFromWire(await NativePayrdrSdk.present(request));
  }
}
