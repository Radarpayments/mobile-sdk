import { decodeUtf8 } from '../src/merchant/utf8';

/** `text` as UTF-8 bytes. */
const utf8 = (text: string) => Uint8Array.from(unescape(encodeURIComponent(text)), char => char.charCodeAt(0));

test('decodes UTF-8 of every length', () => {
  const text = 'A é € 𝄞 Ελληνικά العربية';
  expect(decodeUtf8(utf8(text))).toBe(text);
});

test('a malformed sequence becomes U+FFFD, as with allowMalformed', () => {
  expect(decodeUtf8(Uint8Array.from([0x61, 0xff, 0x62, 0xe2, 0x82]))).toBe('a�b�');
  // An overlong "/" is not a slash.
  expect(decodeUtf8(Uint8Array.from([0xc0, 0xaf]))).toBe('��');
});
