import Clipboard from '@react-native-clipboard/clipboard';
import React from 'react';
import { Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useL10n, usePrefs } from '../AppScope';
import { demoLanguages, type LanguageCode } from '../l10n/l10n';
import type { ApiVersion, OrderType } from '../merchant/orderRegistrar';
import type { ScreenProps } from '../navigation';
import { isAftDemo } from '../store/invest/investMode';
import type { SheetThemeOverride } from '../store/model/scenario';
import { font, StartColors } from '../store/theme/coverStyle';
import { BackButton } from '../ui/BackButton';
import { KeyboardAware } from '../ui/KeyboardAware';
import { SystemBars } from '../ui/SystemBars';
import {
  FieldRow,
  LastOrderRow,
  PickerRow,
  RowDivider,
  SegmentRow,
  SettingsBackground,
  SettingsSection,
} from './SettingsRows';

/**
 * Settings for whoever runs the demo: the gateway, the merchant API and credentials used to register orders, the SDK
 * sheet look and language, the wallet of the platform, and the last order. The four sample apps lay it out the same
 * way.
 */
export function DeveloperScreen({ navigation }: ScreenProps<'Settings'>) {
  const prefs = usePrefs();
  const { strings } = useL10n();
  const insets = useSafeAreaInsets();
  return (
    <View style={[styles.screen, { paddingTop: insets.top }]}>
      <SystemBars lightBackground />
      <KeyboardAware>
        <ScrollView
          contentContainerStyle={[styles.page, { paddingBottom: 24 + insets.bottom }]}
          keyboardShouldPersistTaps="handled"
        >
          <View style={styles.header}>
            <BackButton color={StartColors.text} onPress={() => navigation.goBack()} />
            <Text style={font('Inter', 20, { weight: 800, color: StartColors.text })} accessibilityRole="header">
              {strings.devTitle}
            </Text>
          </View>
          <GatewaySection />
          <CredentialsSection />
          <AppearanceSection />
          {Platform.OS === 'ios' ? <ApplePaySection /> : <GooglePaySection />}
          <SettingsSection title={strings.devMore}>
            <LastOrderRow mdOrder={prefs.lastMdOrder} onCopy={mdOrder => Clipboard.setString(mdOrder)} />
          </SettingsSection>
        </ScrollView>
      </KeyboardAware>
    </View>
  );
}

/** Where orders go and which merchant API registers them. */
function GatewaySection() {
  const prefs = usePrefs();
  const { strings } = useL10n();
  const versions: ReadonlyArray<readonly [ApiVersion, string]> = [
    ['V1', strings.devApiV1],
    ['V2', strings.devApiV2],
  ];
  return (
    <SettingsSection
      title={strings.devGateway}
      footer={[prefs.apiVersion === 'V1' ? strings.devApiV1Hint : strings.devApiV2Hint]}
    >
      <FieldRow
        label={strings.devBaseUrl}
        initial={prefs.baseUrl}
        onChange={prefs.setBaseUrl}
        keyboardType="url"
        error={isGatewayUrl(prefs.baseUrl) ? undefined : strings.devBaseUrlError}
      />
      <RowDivider />
      <SegmentRow
        title={strings.devApiVersion}
        options={versions}
        selected={prefs.apiVersion}
        onSelect={prefs.setApiVersion}
      />
    </SettingsSection>
  );
}

/** API V1: the order type, login and password; API V2: the API key. */
function CredentialsSection() {
  const prefs = usePrefs();
  const { strings } = useL10n();
  const orderTypes: ReadonlyArray<readonly [OrderType, string]> = [
    ['PURCHASE', strings.devOrderPurchase],
    ['AFT', strings.devOrderAft],
  ];
  const footer = isAftDemo(prefs.apiVersion, prefs.orderType)
    ? [strings.devOrderAftHint, strings.devCredentialsNote]
    : [strings.devCredentialsNote];
  return (
    <SettingsSection title={strings.devCredentials} footer={footer}>
      {prefs.apiVersion === 'V1' ? (
        <>
          {/* Transfers are registered through API V1 (registerP2P.do). */}
          <SegmentRow
            title={strings.devOrderType}
            options={orderTypes}
            selected={prefs.orderType}
            onSelect={prefs.setOrderType}
          />
          <RowDivider />
          <FieldRow
            testID="dev_login"
            label={strings.devLogin}
            initial={prefs.merchantLogin}
            onChange={login => prefs.setCredentials({ login })}
          />
          <RowDivider />
          <FieldRow
            testID="dev_password"
            label={strings.devPassword}
            initial={prefs.merchantPassword}
            onChange={password => prefs.setCredentials({ password })}
            secret
          />
        </>
      ) : (
        <FieldRow
          testID="dev_api_key"
          label={strings.devApiKey}
          initial={prefs.apiKey}
          onChange={prefs.setApiKey}
          secret
        />
      )}
    </SettingsSection>
  );
}

/** The SDK sheet's theme and language. */
function AppearanceSection() {
  const prefs = usePrefs();
  const { strings } = useL10n();
  const themes: ReadonlyArray<readonly [SheetThemeOverride, string]> = [
    ['cover', strings.devThemeCover],
    ['light', strings.devThemeLight],
    ['dark', strings.devThemeDark],
    ['system', strings.devThemeSystem],
  ];
  const languages: ReadonlyArray<readonly [LanguageCode | undefined, string]> = [
    [undefined, strings.devLanguageSystem],
    ...demoLanguages,
  ];
  return (
    <SettingsSection title={strings.devSdkLook}>
      <PickerRow
        title={strings.devSheetTheme}
        options={themes}
        selected={prefs.sheetTheme}
        onSelect={prefs.setSheetTheme}
      />
      <RowDivider />
      {/* The SDK reads it from the settings with each payment; the demo's own screens switch at once. */}
      <PickerRow
        title={strings.devLanguage}
        options={languages}
        selected={prefs.localeOverride}
        onSelect={prefs.setLocaleOverride}
      />
    </SettingsSection>
  );
}

function ApplePaySection() {
  const prefs = usePrefs();
  const { strings } = useL10n();
  return (
    <SettingsSection title={strings.devApplePay}>
      <FieldRow
        label={strings.devGpayMerchant}
        initial={prefs.applePayMerchantId}
        onChange={prefs.setApplePayMerchantId}
      />
    </SettingsSection>
  );
}

/** Google Pay gateway and merchant; empty means what the gateway says. */
function GooglePaySection() {
  const prefs = usePrefs();
  const { strings } = useL10n();
  return (
    <SettingsSection title={strings.devWallets}>
      <FieldRow
        label={strings.devGpayGateway}
        initial={prefs.googlePayGateway}
        onChange={gateway => prefs.setGooglePay({ gateway })}
        placeholder={strings.devGpayFromGateway}
      />
      <RowDivider />
      <FieldRow
        label={strings.devGpayMerchant}
        initial={prefs.googlePayMerchantId}
        onChange={merchantId => prefs.setGooglePay({ merchantId })}
        placeholder={strings.devGpayFromGateway}
      />
    </SettingsSection>
  );
}

/**
 * The gateway is reached over HTTPS only; anything else would fail on the first order with a less telling error.
 * A pattern rather than `URL`: React Native's `URL` guesses the host with a loose pattern of its own, and Node's,
 * which the tests get, is a different parser.
 */
export function isGatewayUrl(url: string): boolean {
  return /^https:\/\/(?:[^\s/?#@]*@)?(?:\[[\dA-Fa-f:.]+\]|[A-Za-z\d](?:[A-Za-z\d.-]*[A-Za-z\d])?)(?::\d*)?(?:[/?#]\S*)?$/.test(
    url.trim(),
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: SettingsBackground },
  page: { paddingHorizontal: 16 },
  header: { flexDirection: 'row', alignItems: 'center', gap: 4 },
});
