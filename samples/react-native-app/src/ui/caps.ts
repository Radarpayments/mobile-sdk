const GREEK_STRESSED = 'ΆΈΉΊΌΎΏ';
const GREEK_PLAIN = 'ΑΕΗΙΟΥΩ';

/**
 * The text in capitals for a heading. Greek capitals carry no stress mark ("ΠΥΛΗ", not "ΠΎΛΗ"), but
 * `toLocaleUpperCase` keeps it on Hermes.
 */
export function toCaps(text: string): string {
  return Array.from(text.toLocaleUpperCase(), (c) => {
    const i = GREEK_STRESSED.indexOf(c);
    return i < 0 ? c : GREEK_PLAIN[i];
  }).join('');
}
