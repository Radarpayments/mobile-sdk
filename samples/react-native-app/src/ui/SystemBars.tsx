import React from 'react';
import { StatusBar } from 'react-native';

/**
 * Dark status bar icons on a light screen, light icons on a dark one. The screen mounted last wins; when it closes,
 * the one below it takes over again.
 */
export function SystemBars({ lightBackground }: { lightBackground: boolean }) {
  return <StatusBar barStyle={lightBackground ? 'dark-content' : 'light-content'} />;
}
