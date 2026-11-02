/**
 * The amount typed in the top-up field, in euros with `.` as the separator whatever the locale shows: at most six
 * whole digits and two decimals, no leading zeros.
 */
export type TopUpAmount = string;

export const MIN_TOP_UP = 1000;
export const MAX_TOP_UP = 100000;
export const PRESETS = [5000, 10000, 25000, 50000] as const;
const DECIMALS = 2;
const WHOLE_DIGITS = 6;

/** Euro cents, or undefined while nothing is typed. */
export function amountMinor(text: TopUpAmount): number | undefined {
  if (!text) return undefined;
  const [whole, cents = ''] = text.split('.');
  return Number(whole || '0') * 100 + Number(cents.padEnd(2, '0'));
}

export function amountValid(text: TopUpAmount): boolean {
  const minor = amountMinor(text);
  return minor !== undefined && minor >= MIN_TOP_UP && minor <= MAX_TOP_UP;
}

/** `c` typed at the end: a digit or `.`; anything that would break the format is ignored. */
function type(text: TopUpAmount, c: string): TopUpAmount {
  const dot = text.indexOf('.');
  if (c === '.') return dot >= 0 ? text : `${text || '0'}.`;
  if (!/^[0-9]$/.test(c)) return text;
  if (dot >= 0) return text.length - dot > DECIMALS ? text : text + c;
  if (text === '0') return c;
  return text.length >= WHOLE_DIGITS ? text : text + c;
}

/**
 * What the system keyboard left in the field, cleaned as if typed key by key: the locale's separator (`,` or the
 * Arabic `٫`) becomes `.`, Arabic-Indic and Persian digits become ASCII ones, anything else that does not fit the
 * format is dropped.
 */
export function parseAmount(input: string): TopUpAmount {
  return [...input].reduce((text, c) => type(text, ascii(c)), '');
}

function ascii(c: string): string {
  if (c === ',' || c === '\u066B') return '.';
  const code = c.charCodeAt(0);
  for (const zero of [0x0660, 0x06f0]) {
    if (code >= zero && code <= zero + 9) return String(code - zero);
  }
  return c;
}

/** The amount of a preset chip. */
export function presetAmount(minor: number): TopUpAmount {
  const cents = minor % 100;
  const whole = `${Math.floor(minor / 100)}`;
  return cents === 0 ? whole : `${whole}.${`${cents}`.padStart(2, '0').replace(/0+$/, '')}`;
}

/**
 * Where the top-up screen is in the transfer: typing (`editing`), refused (`declined`), not known (`unconfirmed`:
 * the money is not credited and the app does not offer to pay again), or credited (`done`).
 */
export type TopUpStatus =
  | { kind: 'editing' }
  | { kind: 'declined'; message?: string }
  | { kind: 'unconfirmed' }
  | { kind: 'done'; orderId: string; amountMinor: number; freeCashBefore: number; freeCashAfter: number; at: number };

/** Whether "Continue" may start a transfer of `amount` now. */
export function canContinue(status: TopUpStatus, amount: TopUpAmount): boolean {
  return amountValid(amount) && (status.kind === 'editing' || status.kind === 'declined');
}
