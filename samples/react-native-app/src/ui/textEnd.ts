import { StyleSheet, type TextStyle } from 'react-native';

const styles = StyleSheet.create({ left: { textAlign: 'left' }, right: { textAlign: 'right' } });

/**
 * Text aligned to the end of the line. Needed for a right-to-left layout set with the `direction` style: unlike
 * `I18nManager.forceRTL`, it does not swap `textAlign: 'right'`.
 */
export function textEnd(rtl: boolean): TextStyle {
  return rtl ? styles.left : styles.right;
}
