import React from 'react';
import { Platform, Pressable, type PressableProps, type StyleProp, type ViewStyle } from 'react-native';

export interface TapProps extends Omit<PressableProps, 'style'> {
  style?: StyleProp<ViewStyle>;
  /** Colour of the Android ripple; a dark veil by default, pass a light one on dark backgrounds. */
  rippleColor?: string;
}

/** A pressable area: the ripple on Android, a dimmed press on iOS, where there is no ripple. */
export function Tap({ style, rippleColor = '#0000001F', ...props }: TapProps) {
  return (
    <Pressable
      accessibilityRole="button"
      android_ripple={{ color: rippleColor, foreground: true }}
      style={({ pressed }) => [style, pressed && Platform.OS === 'ios' && { opacity: 0.7 }]}
      {...props}
    />
  );
}
