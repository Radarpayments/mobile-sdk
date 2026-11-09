import { decodeUtf8 } from './utf8';

const ALPHABET = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz';
const OUTSIDE_PREFIX = 'ps_';
const INSIDE_PREFIX = 'ps:';

/**
 * The mdOrder an API V2 session id stands for, as the SDK's `SessionIdConverter` reads it: the id is `ps_` and the
 * Base58 (Bitcoin alphabet) of `ps:` and the mdOrder. Undefined when `sessionId` is not one.
 */
export function sessionIdToMdOrder(sessionId: string): string | undefined {
  if (!sessionId.startsWith(OUTSIDE_PREFIX)) return undefined;
  const bytes = decodeBase58(sessionId.slice(OUTSIDE_PREFIX.length));
  const decoded = bytes && decodeUtf8(bytes);
  return decoded?.startsWith(INSIDE_PREFIX) ? decoded.slice(INSIDE_PREFIX.length) : undefined;
}

/** Base58 `text` as bytes, big-endian; undefined for a character outside the alphabet. */
function decodeBase58(text: string): Uint8Array | undefined {
  const leadingOnes = text.length - text.replace(/^1+/, '').length;
  // Little-endian base-256 digits of the number the text spells.
  const digits: number[] = [];
  for (const char of text) {
    let carry = ALPHABET.indexOf(char);
    if (carry < 0) return undefined;
    for (let i = 0; i < digits.length; i++) {
      carry += digits[i] * 58;
      digits[i] = carry % 256;
      carry = Math.floor(carry / 256);
    }
    for (; carry > 0; carry = Math.floor(carry / 256)) digits.push(carry % 256);
  }
  return Uint8Array.from([...new Array<number>(leadingOnes).fill(0), ...digits.reverse()]);
}
