/**
 * `length` random lowercase hex digits, as in a UUID. Not secret: an order number and a demo buyer id only need to
 * be unlikely to repeat.
 */
export function randomHex(length: number): string {
  return Array.from({ length }, () => Math.floor(Math.random() * 16).toString(16)).join('');
}
