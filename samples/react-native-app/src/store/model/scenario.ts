/**
 * The demo panel: what the next payment looks like, the same in every cover.
 *
 * `buyerWithCards`: true = the order is registered with the demo buyer's clientId, so cards saved in earlier
 * payments show up; false = a guest without saved cards. `wallet` shows Google Pay / Apple Pay (when the merchant
 * has it set up), `scanner` the card scanner on the card form, and `sdkResultScreen` lets the SDK show its own
 * success / declined screen before returning.
 */
export interface Scenario {
  readonly buyerWithCards: boolean;
  readonly wallet: boolean;
  readonly scanner: boolean;
  readonly sdkResultScreen: boolean;
}

/** Which theme the SDK sheet uses: the cover's brand colours or a stock one. */
export type SheetThemeOverride = 'cover' | 'light' | 'dark' | 'system';

export const sheetThemeOverrides: readonly SheetThemeOverride[] = ['cover', 'light', 'dark', 'system'];
