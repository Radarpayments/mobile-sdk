import { I18nManager, Platform, Settings } from 'react-native';

import { languageCodes, messages, placeholders, type LanguageCode, type MessageKey, type Strings } from './messages';

export type { LanguageCode, Strings };

/** Languages the demo and the SDK are translated into, each named in itself; the setting lists them in this order. */
export const demoLanguages: ReadonlyArray<readonly [LanguageCode, string]> = [
  ['en', 'English'],
  ['ru', 'Русский'],
  ['ro', 'Română'],
  ['el', 'Ελληνικά'],
  ['bg', 'Български'],
  ['de', 'Deutsch'],
  ['fr', 'Français'],
  ['es', 'Español'],
  ['ar', 'العربية'],
];

export function isLanguageCode(code: string | undefined): code is LanguageCode {
  return (languageCodes as readonly string[]).includes(code ?? '');
}

const cache = new Map<LanguageCode, Strings>();

export function stringsFor(language: LanguageCode): Strings {
  const cached = cache.get(language);
  if (cached) return cached;
  const strings = Object.fromEntries(
    (Object.entries(messages[language]) as Array<[MessageKey, string]>).map(([key, text]) => {
      const names = placeholders[key];
      return [
        key,
        names
          ? (...values: unknown[]) =>
              names.reduce((result, name, i) => result.replaceAll(`{${name}}`, String(values[i])), text)
          : text,
      ];
    }),
  ) as unknown as Strings;
  cache.set(language, strings);
  return strings;
}

/**
 * The device language when the demo has it, English otherwise. iOS narrows the app's current locale to the
 * localizations its bundle holds (it has no .lproj folders, so that is English), hence the device's own list there.
 */
export function systemLanguage(): LanguageCode {
  const tag =
    Platform.OS === 'ios'
      ? (Settings.get('AppleLanguages') as string[] | undefined)?.[0]
      : I18nManager.getConstants().localeIdentifier;
  const language = tag?.split(/[-_]/)[0];
  return isLanguageCode(language) ? language : 'en';
}

/** Arabic is written right to left. */
export function isRtl(language: LanguageCode): boolean {
  return language === 'ar';
}

/** Euro `minor` units for `language`, e.g. "€1,578.00" or "1.578,00 €". */
export function formatEur(minor: number, language: LanguageCode): string {
  return new Intl.NumberFormat(language, { style: 'currency', currency: 'EUR' }).format(minor / 100);
}
