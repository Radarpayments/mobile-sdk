import { demoLanguages, formatEur, isRtl, stringsFor } from '../src/l10n/l10n';
import { languageCodes, messages, placeholders } from '../src/l10n/messages';

test('every language has every text with the same placeholders as English', () => {
  for (const language of languageCodes) {
    expect(Object.keys(messages[language])).toEqual(Object.keys(messages.en));
    for (const [key, names] of Object.entries(placeholders)) {
      for (const name of names ?? []) {
        expect(messages[language][key as keyof typeof messages.en]).toContain(`{${name}}`);
      }
    }
  }
});

test('placeholders are filled in', () => {
  expect(stringsFor('en').storePay('€3.90')).toBe('Pay €3.90');
  expect(stringsFor('ru').storeCartCount(2)).toBe('Корзина · 2');
  expect(stringsFor('de').coverFurnitureName).toBe('Hearth & Co');
});

test('the settings list the nine languages in their own names; Arabic is right to left', () => {
  expect(demoLanguages.map(([code]) => code)).toEqual(languageCodes);
  expect(languageCodes.filter(isRtl)).toEqual(['ar']);
});

test('euros in the language of the demo', () => {
  expect(formatEur(157800, 'en')).toBe('€1,578.00');
  expect(formatEur(390, 'de')).toMatch(/^3,90\s€$/);
});
