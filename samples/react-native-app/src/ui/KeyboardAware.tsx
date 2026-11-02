import React, { type ReactNode } from 'react';
import { KeyboardAvoidingView, Platform, StyleSheet } from 'react-native';

/**
 * Keeps a screen with text fields above the keyboard. The app is drawn edge to edge, so Android no longer shrinks the
 * window for the keyboard; this shrinks the screen instead, and the scroll view inside brings the focused field back
 * into sight.
 */
export function KeyboardAware({ children }: { children: ReactNode }) {
  return (
    <KeyboardAvoidingView style={styles.fill} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
      {children}
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({ fill: { flex: 1 } });
