/**
 * How a checkout ended. Every result names the order (`orderId`, the gateway's mdOrder): your server, not this result,
 * decides whether to release the goods.
 *
 * - `completed`: the gateway confirmed the payment.
 * - `failed`: the payment did not happen (declined, expired), or the sheet could not open (`busy`, `no_screen`,
 *   `invalid_request`).
 * - `canceled`: the buyer closed the sheet.
 * - `unknown`: the payment was sent but how it ended is not known; check the order on your server or wait for the
 *   gateway's callback, and do not offer to pay again meanwhile.
 */
export type CheckoutResult =
  | { status: 'completed'; orderId: string }
  | { status: 'failed'; orderId: string; error: CheckoutError }
  | { status: 'canceled'; orderId: string }
  | { status: 'unknown'; orderId: string; error?: CheckoutError };

/** What went wrong. `message` is the gateway's or the bank's text when there is one. */
export interface CheckoutError {
  code: CheckoutErrorCode;
  message?: string;
  /** The bank's code of a `declined` payment. */
  declineCode?: string;
  /** HTTP status and the gateway's own code of an `api` error. */
  httpStatus?: number;
  gatewayErrorCode?: string;
}

const ERROR_CODES = [
  'declined',
  'api',
  'network',
  'transaction',
  'session_not_found',
  'order_not_found',
  'session_expired',
  'invalid_session_id',
  'cryptogram',
  'not_configured',
  'three_ds_failed',
  'three_ds_timeout',
  'google_pay',
  'apple_pay',
  'invalid_request',
  'busy',
  'no_screen',
  'unknown',
] as const;

export type CheckoutErrorCode = (typeof ERROR_CODES)[number];

/** The result JSON of the native side; codes this version does not know become `unknown`. */
export function resultFromWire(json: string): CheckoutResult {
  const wire = JSON.parse(json) as { status?: string; orderId?: string; error?: Record<string, unknown> };
  const orderId = wire.orderId ?? '';
  const error = wire.error ? errorFromWire(wire.error) : undefined;
  switch (wire.status) {
    case 'completed':
      return { status: 'completed', orderId };
    case 'canceled':
      return { status: 'canceled', orderId };
    case 'failed':
      return { status: 'failed', orderId, error: error ?? { code: 'unknown' } };
    default:
      return error ? { status: 'unknown', orderId, error } : { status: 'unknown', orderId };
  }
}

function errorFromWire(wire: Record<string, unknown>): CheckoutError {
  const code = (ERROR_CODES as readonly string[]).includes(wire.code as string) ? (wire.code as CheckoutErrorCode) : 'unknown';
  const error: CheckoutError = { code };
  if (typeof wire.message === 'string') error.message = wire.message;
  if (typeof wire.declineCode === 'string') error.declineCode = wire.declineCode;
  if (typeof wire.httpStatus === 'number') error.httpStatus = wire.httpStatus;
  if (typeof wire.gatewayErrorCode === 'string') error.gatewayErrorCode = wire.gatewayErrorCode;
  return error;
}
