/* eslint-disable no-bitwise -- UTF-8 is made of bit fields. */
const REPLACEMENT = 0xfffd;

/**
 * UTF-8 `bytes` as text; a malformed sequence becomes U+FFFD, as with Dart's `utf8.decode(allowMalformed: true)`.
 * Hand-written because Hermes does not promise a `TextDecoder`.
 */
export function decodeUtf8(bytes: Uint8Array): string {
  const codePoints: number[] = [];
  let at = 0;
  while (at < bytes.length) {
    const lead = bytes[at];
    const [length, initial, min] =
      lead < 0x80
        ? [1, lead, 0]
        : lead >= 0xc2 && lead < 0xe0
        ? [2, lead & 0x1f, 0x80]
        : lead >= 0xe0 && lead < 0xf0
        ? [3, lead & 0x0f, 0x800]
        : lead >= 0xf0 && lead < 0xf5
        ? [4, lead & 0x07, 0x10000]
        : [0, 0, 0];
    let codePoint = initial;
    let read = 1;
    while (read < length && at + read < bytes.length && (bytes[at + read] & 0xc0) === 0x80) {
      codePoint = (codePoint << 6) | (bytes[at + read] & 0x3f);
      read++;
    }
    const valid =
      length > 0 &&
      read === length &&
      codePoint >= min &&
      codePoint <= 0x10ffff &&
      (codePoint < 0xd800 || codePoint > 0xdfff);
    codePoints.push(valid ? codePoint : REPLACEMENT);
    at += Math.max(read, 1);
  }
  let text = '';
  // In slices: String.fromCodePoint takes its arguments on the stack.
  for (let start = 0; start < codePoints.length; start += 4096) {
    text += String.fromCodePoint(...codePoints.slice(start, start + 4096));
  }
  return text;
}
