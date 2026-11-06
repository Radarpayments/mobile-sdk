import React from 'react';
import { StyleSheet, Text } from 'react-native';

/** Material Icons, as Flutter's `Icons` draws them; tool/make_fonts.py bundles these glyphs and no others. */
const glyphs = {
  arrow_back: 0xe092,
  chevron_left: 0xe15e,
  settings_outlined: 0xf36e,
  shopping_basket_outlined: 0xf37e,
  place_outlined: 0xf2ac,
  add: 0xe047,
  remove: 0xe516,
  check: 0xe156,
  close: 0xe16a,
  schedule: 0xe556,
  keyboard_arrow_down: 0xe353,
  trending_up: 0xe67f,
  account_balance_wallet_outlined: 0xee33,
  arrow_upward: 0xe0a0,
  notifications_none: 0xe451,
  error_outline: 0xe238,
  check_circle_outline: 0xe15a,
  credit_card: 0xe19f,
} as const;

export type IconName = keyof typeof glyphs;

export interface IconProps {
  name: IconName;
  color: string;
  size?: number;
  /** Turns a direction icon (back) around for a right-to-left layout. */
  mirrored?: boolean;
}

/** A decorative glyph: the control it sits in carries the label for screen readers. */
export function Icon({ name, color, size = 24, mirrored = false }: IconProps) {
  return (
    <Text
      accessible={false}
      allowFontScaling={false}
      style={[
        styles.glyph,
        { color, fontSize: size, lineHeight: size, width: size, height: size },
        mirrored && styles.mirrored,
      ]}
    >
      {String.fromCodePoint(glyphs[name])}
    </Text>
  );
}

const styles = StyleSheet.create({
  // The font's em square is its line: ascent 512 of 512 units, no descent.
  glyph: { fontFamily: 'MaterialIcons-Regular', textAlign: 'center', includeFontPadding: false },
  mirrored: { transform: [{ scaleX: -1 }] },
});
