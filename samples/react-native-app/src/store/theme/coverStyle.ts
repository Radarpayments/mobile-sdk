import { Platform, type TextStyle } from 'react-native';

import { InvestColors } from '../invest/investColors';
import type { CoverId } from '../model/catalog';

/** Weights the bundled static fonts come in (tool/make_fonts.py). */
export type Weight = 400 | 500 | 600 | 700 | 800;

const weightNames: Record<Weight, string> = {
  400: 'Regular',
  500: 'Medium',
  600: 'SemiBold',
  700: 'Bold',
  800: 'ExtraBold',
};

/** Google Fonts (OFL), one static file per weight in assets/fonts. */
export const StoreFonts = {
  cormorant: 'Cormorant',
  montserrat: 'Montserrat',
  nunito: 'Nunito',
  inter: 'Inter',
  plex: 'IBMPlexSans',
  /** Italic only. */
  playfair: 'PlayfairDisplay',
} as const;

export type FontFamily = (typeof StoreFonts)[keyof typeof StoreFonts];

export interface TextOptions {
  weight?: Weight;
  color?: string;
  lineHeight?: number;
  letterSpacing?: number;
  italic?: boolean;
}

/**
 * Text in `family`. The weight picks the file (`Inter-Bold`), never `fontWeight`: on a custom font Android would
 * draw a fake bold over it.
 */
export function font(family: FontFamily, size: number, options: TextOptions = {}): TextStyle {
  const { weight = 400, color, lineHeight, letterSpacing, italic = false } = options;
  return {
    fontFamily: `${family}-${weightNames[weight]}${italic ? 'Italic' : ''}`,
    fontSize: size,
    color,
    lineHeight,
    letterSpacing,
  };
}

/** Ids and keys: the platform's monospace font (Android's "monospace", Menlo on iOS). */
export function mono(size: number, options: { color?: string; lineHeight?: number; bold?: boolean } = {}): TextStyle {
  return {
    fontFamily: Platform.select({ ios: 'Menlo', default: 'monospace' }),
    fontSize: size,
    fontWeight: options.bold ? '500' : undefined,
    color: options.color,
    lineHeight: options.lineHeight,
  };
}

/**
 * The brand of a demo business: its colours, corner radius and fonts. The store screens use all of it; the SDK
 * sheet takes the colours and corners through `paymentTheme` in checkout.ts.
 *
 * `fieldSurface` is the background of input fields and cards inside the SDK sheet; with `pillButtons` buttons are
 * fully rounded (the coffee shop) instead of using `radius`.
 */
export interface CoverStyle {
  id: CoverId;
  dark: boolean;
  primary: string;
  onPrimary: string;
  background: string;
  surface: string;
  fieldSurface: string;
  text: string;
  muted: string;
  border: string;
  accent: string;
  radius: number;
  pillButtons: boolean;
  headingFont: FontFamily;
  bodyFont: FontFamily;
  accentFont?: FontFamily;
}

/** Brand of each cover, from the design tokens picked with the ui-ux-pro-max design system. */
const styles: Record<CoverId, CoverStyle> = {
  furniture: {
    id: 'furniture',
    dark: false,
    primary: '#1C1917',
    onPrimary: '#FFFFFF',
    background: '#FAFAF9',
    surface: '#FFFFFF',
    fieldSurface: '#F5F5F4',
    text: '#0C0A09',
    muted: '#57534E',
    border: '#D6D3D1',
    accent: '#A16207',
    radius: 12,
    pillButtons: false,
    headingFont: StoreFonts.cormorant,
    bodyFont: StoreFonts.montserrat,
  },
  coffee: {
    id: 'coffee',
    dark: true,
    primary: '#F3D9B1',
    onPrimary: '#17110E',
    background: '#17110E',
    surface: '#261C17',
    fieldSurface: '#2E231E',
    text: '#FEF3C7',
    muted: '#D6C3A5',
    border: '#5B4A40',
    accent: '#F3D9B1',
    radius: 18,
    pillButtons: true,
    headingFont: StoreFonts.nunito,
    bodyFont: StoreFonts.nunito,
  },
  tickets: {
    id: 'tickets',
    dark: false,
    primary: '#EA580C',
    onPrimary: '#000000',
    background: '#FFFFFF',
    surface: '#FFF7ED',
    fieldSurface: '#F8FAFC',
    text: '#0F172A',
    muted: '#64748B',
    border: '#E2E8F0',
    accent: '#EA580C',
    radius: 14,
    pillButtons: false,
    headingFont: StoreFonts.inter,
    bodyFont: StoreFonts.inter,
    accentFont: StoreFonts.playfair,
  },
  invest: {
    id: 'invest',
    dark: true,
    primary: InvestColors.accent,
    onPrimary: InvestColors.onAccent,
    background: InvestColors.surface,
    surface: InvestColors.surface,
    fieldSurface: InvestColors.surface2,
    text: InvestColors.text,
    muted: InvestColors.muted,
    border: InvestColors.fieldBorder,
    accent: InvestColors.accent,
    radius: 12,
    pillButtons: false,
    headingFont: StoreFonts.plex,
    bodyFont: StoreFonts.plex,
  },
};

export function coverStyle(id: CoverId): CoverStyle {
  return styles[id];
}

/** Corners of the store's buttons. */
export function buttonRadius(style: CoverStyle): number {
  return style.pillButtons ? 999 : style.radius;
}

/** Text in the store's body font. */
export function body(style: CoverStyle, size: number, options: TextOptions = {}): TextStyle {
  return font(style.bodyFont, size, options);
}

/** Text in the store's heading font, bold and in the text colour unless told otherwise. */
export function heading(style: CoverStyle, size: number, options: TextOptions = {}): TextStyle {
  return font(style.headingFont, size, { weight: 700, color: style.text, ...options });
}

/** `hex` (#RRGGBB) at `alpha`, as #RRGGBBAA. */
export function withAlpha(hex: string, alpha: number): string {
  return `${hex}${Math.round(alpha * 255)
    .toString(16)
    .padStart(2, '0')}`;
}

/** Neutral colours of the start and developer screens: they belong to the demo, not to a store. */
export const StartColors = {
  background: '#F7F7F5',
  surface: '#FFFFFF',
  text: '#111827',
  muted: '#4B5563',
  border: '#E5E7EB',
  link: '#2563EB',
  selected: '#E5E7EB',
} as const;
