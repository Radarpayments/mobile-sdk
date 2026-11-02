import { StyleSheet } from 'react-native';

/**
 * An image that covers its parent. `StyleSheet.absoluteFill` alone is not enough for a bundled image: React Native gives
 * it the file's pixel size as width and height, which win over the insets, so only its top-left corner would show.
 */
export const photoFill = StyleSheet.create({
  fill: { position: 'absolute', top: 0, left: 0, width: '100%', height: '100%' },
}).fill;
