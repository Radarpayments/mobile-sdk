import React, { useEffect, useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { gatewayRegistrar, type AppScopeValue } from './src/AppScope';
import { AppPrefs } from './src/prefs/appPrefs';
import { SampleApp } from './src/SampleApp';
import { StoreModel } from './src/store/storeModel';
import { StartColors } from './src/store/theme/coverStyle';

/** Reads the settings, then shows the demo; until then the start screen's background. */
export default function App() {
  const [scope, setScope] = useState<AppScopeValue>();
  useEffect(() => {
    AppPrefs.load().then(prefs => setScope({ prefs, store: new StoreModel(), registrarFactory: gatewayRegistrar }));
  }, []);
  return scope ? <SampleApp scope={scope} /> : <View style={styles.loading} />;
}

const styles = StyleSheet.create({ loading: { flex: 1, backgroundColor: StartColors.background } });
