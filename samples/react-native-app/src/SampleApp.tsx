import React from 'react';
import { StyleSheet, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';

import { AppScope, useL10n, type AppScopeValue } from './AppScope';
import { SdkCoreScreen } from './core/SdkCoreScreen';
import { DeveloperScreen } from './dev/DeveloperScreen';
import { StackNavigator, type Screens } from './navigation';
import { CoverScreen } from './store/CoverScreen';
import { FurnitureProductScreen } from './store/furniture/FurnitureProductScreen';
import { StartScreen } from './store/StartScreen';
import { StoreResultScreen } from './store/StoreResultScreen';

const screens: Screens = {
  Start: StartScreen,
  Cover: CoverScreen,
  FurnitureProduct: FurnitureProductScreen,
  StoreResult: StoreResultScreen,
  Settings: DeveloperScreen,
  Core: SdkCoreScreen,
};

/**
 * The demo app. Its language is the one picked in the developer settings (System follows the device); the SDK sheet
 * gets the same one through `PayrdrConfig.locale`. Arabic lays the screens out right to left at once, through the
 * `direction` style, without the restart `I18nManager.forceRTL` needs.
 */
export function SampleApp({ scope }: { scope: AppScopeValue }) {
  return (
    <AppScope value={scope}>
      <SafeAreaProvider>
        <Screens />
      </SafeAreaProvider>
    </AppScope>
  );
}

function Screens() {
  const { rtl } = useL10n();
  return (
    <View style={[styles.root, rtl ? styles.rtl : styles.ltr]}>
      <StackNavigator screens={screens} initial="Start" transitions={{ FurnitureProduct: 'fade' }} rtl={rtl} />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  ltr: { direction: 'ltr' },
  rtl: { direction: 'rtl' },
});
