/**
 * Card networks the form tells apart by the leading digits, as the SDK's own form does (`CardNetworkResolver` in
 * sdk-payment).
 */
export type CardNetwork =
  | 'visa'
  | 'mastercard'
  | 'amex'
  | 'unionPay'
  | 'diners'
  | 'jcb'
  | 'discover'
  | 'maestro'
  | 'unknown';

export const networkLabel: Record<CardNetwork, string> = {
  visa: 'Visa',
  mastercard: 'Mastercard',
  amex: 'Amex',
  unionPay: 'UnionPay',
  diners: 'Diners Club',
  jcb: 'JCB',
  discover: 'Discover',
  maestro: 'Maestro',
  unknown: '',
};

export function digitsOf(text: string): string {
  return text.replace(/\D/g, '');
}

/** The network of a partial or complete card number. */
export function networkOf(pan: string): CardNetwork {
  const digits = digitsOf(pan);
  const prefix = (length: number) => (digits.length >= length ? Number(digits.slice(0, length)) : undefined);
  const [two, three, four] = [prefix(2), prefix(3), prefix(4)];
  const within = (value: number | undefined, from: number, to: number) =>
    value !== undefined && value >= from && value <= to;
  if (digits === '') return 'unknown';
  if (digits.startsWith('4')) return 'visa';
  if (two === 34 || two === 37) return 'amex';
  if (within(two, 51, 55) || within(four, 2221, 2720)) return 'mastercard';
  if (two === 62) return 'unionPay';
  if (two === 36 || two === 38 || two === 39 || within(three, 300, 305) || four === 3095) return 'diners';
  if (within(four, 3528, 3589)) return 'jcb';
  if (four === 6011 || within(three, 644, 649) || two === 65) return 'discover';
  if (two === 50 || within(two, 56, 58) || four === 6304 || four === 6759 || within(four, 6761, 6763)) return 'maestro';
  return 'unknown';
}

/** Longest number the network issues; typing beyond it is dropped. */
export function maxPanLength(network: CardNetwork): number {
  return network === 'amex' ? 15 : network === 'mastercard' ? 16 : 19;
}

/** CVC printed on its cards: 4 digits on the front for Amex, 3 on the back otherwise. */
export function cvcLength(network: CardNetwork): number {
  return network === 'amex' ? 4 : 3;
}

/** `digits` in the network's groups: "4111 1111 1111 1111", Amex 4-6-5. */
export function groupPan(digits: string): string {
  const sizes = networkOf(digits) === 'amex' ? [4, 6, 5] : [4, 4, 4, 4, 3];
  const groups: string[] = [];
  let at = 0;
  for (const size of sizes) {
    if (at >= digits.length) break;
    groups.push(digits.slice(at, at + size));
    at += size;
  }
  return groups.join(' ');
}

/** A typed card number: its digits, at most the network's length, in groups. */
export function formatPan(text: string): string {
  const digits = digitsOf(text);
  return groupPan(digits.slice(0, maxPanLength(networkOf(digits))));
}

/** Typed expiry as `MM/YY`: digits only, the slash added after the month. */
export function formatExpiry(text: string): string {
  const digits = digitsOf(text).slice(0, 4);
  return digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;
}
