import type { Strings } from '../l10n/l10n';

/** sdk-core reports stable codes; a merchant maps them to its own words, as here. */
export function errorText(strings: Strings, code: string): string {
  switch (code) {
    case 'required':
      return strings.coreErrorRequired;
    case 'invalid-format':
      return strings.coreErrorFormat;
    default:
      return strings.coreErrorInvalid;
  }
}
