import Clipboard from '@react-native-clipboard/clipboard';
import React from 'react';
import { Platform, ScrollView, StyleSheet, Text, TextInput, View, type KeyboardTypeOptions } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useL10n, usePrefs } from '../AppScope';
import { demoLanguages, type LanguageCode } from '../l10n/l10n';
import type { ApiVersion, OrderType } from '../merchant/orderRegistrar';
import type { ScreenProps } from '../navigation';
import type { SheetThemeOverride } from '../store/model/scenario';
import { GroupHeader } from '../store/ScenarioSheet';
import { font, mono, StartColors } from '../store/theme/coverStyle';
import { BackButton } from '../ui/BackButton';
import { Icon } from '../ui/Icon';
import { SystemBars } from '../ui/SystemBars';
import { Tap } from '../ui/Tap';
import { KeyboardAware } from '../ui/KeyboardAware';

/**
 * Settings for whoever runs the demo: the gateway, the merchant API and credentials used to register orders, the SDK
 * sheet look and language, the wallet of the platform, and the last order.
 */
export function DeveloperScreen({ navigation }: ScreenProps<'Settings'>) {
  const prefs = usePrefs();
  const { strings } = useL10n();
  const insets = useSafeAreaInsets();
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
          <GroupHeader title={strings.devGateway} />
          <Field label={strings.devBaseUrl} initial={prefs.baseUrl} onChange={prefs.setBaseUrl} keyboardType="url" />
          <ApiSection />
          <Note text={strings.devCredentialsNote} />
          <GroupHeader title={strings.devSdkLook} />
          <Label text={strings.devSheetTheme} />
          <Chips options={themes} selected={prefs.sheetTheme} onSelect={prefs.setSheetTheme} />
          <Label text={strings.devLanguage} />
          {/* The SDK reads it from the settings with each payment; the demo's own screens switch at once. */}
          <Chips options={languages} selected={prefs.localeOverride} onSelect={prefs.setLocaleOverride} />
          {Platform.OS === 'ios' ? (
            <>
              <GroupHeader title={strings.devApplePay} />
              <Field
                label={strings.devGpayMerchant}
                initial={prefs.applePayMerchantId}
                onChange={prefs.setApplePayMerchantId}
              />
            </>
          ) : (
            <>
              <GroupHeader title={strings.devWallets} />
              <Field
                label={strings.devGpayGateway}
                initial={prefs.googlePayGateway}
                onChange={gateway => prefs.setGooglePay({ gateway })}
                placeholder={strings.devGpayFromGateway}
              />
              <Field
                label={strings.devGpayMerchant}
                initial={prefs.googlePayMerchantId}
                onChange={merchantId => prefs.setGooglePay({ merchantId })}
                placeholder={strings.devGpayFromGateway}
              />
            </>
          )}
          <GroupHeader title={strings.devMore} />
          <LastOrder mdOrder={prefs.lastMdOrder} />
        </ScrollView>
      </KeyboardAware>
    </View>
  );
}

/** API V1 (login and password) or API V2 (API key): how the demo registers its orders. */
function ApiSection() {
  const prefs = usePrefs();
  const { strings } = useL10n();
  const versions: ReadonlyArray<readonly [ApiVersion, string]> = [
    ['V1', strings.devApiV1],
    ['V2', strings.devApiV2],
  ];
  const orderTypes: ReadonlyArray<readonly [OrderType, string]> = [
    ['PURCHASE', strings.devOrderPurchase],
    ['AFT', strings.devOrderAft],
  ];
  return (
    <>
      <Label text={strings.devApiVersion} />
      <Chips options={versions} selected={prefs.apiVersion} onSelect={prefs.setApiVersion} />
      {prefs.apiVersion === 'V1' ? (
        <>
          <Note text={strings.devApiV1Hint} hint />
          {/* Transfers are registered through API V1 (registerP2P.do). */}
          <Label text={strings.devOrderType} />
          <Chips options={orderTypes} selected={prefs.orderType} onSelect={prefs.setOrderType} />
          {prefs.orderType === 'AFT' && <Note text={strings.devOrderAftHint} hint />}
          {prefs.orderType === 'AFT' && <InvestReset onReset={() => prefs.setInvestAccount(undefined)} />}
          <Field
            testID="dev_login"
            label={strings.devLogin}
            initial={prefs.merchantLogin}
            onChange={login => prefs.setCredentials({ login })}
          />
          <Field
            testID="dev_password"
            label={strings.devPassword}
            initial={prefs.merchantPassword}
            onChange={password => prefs.setCredentials({ password })}
            secret
          />
        </>
      ) : (
        <>
          <Note text={strings.devApiV2Hint} hint />
          <Field
            testID="dev_api_key"
            label={strings.devApiKey}
            initial={prefs.apiKey}
            onChange={prefs.setApiKey}
            secret
          />
        </>
      )}
    </>
  );
}

interface FieldProps {
  label: string;
  /** The stored value when the screen opens; the field then edits its own text and saves every change. */
  initial: string;
  onChange: (value: string) => void;
  keyboardType?: KeyboardTypeOptions;
  secret?: boolean;
  placeholder?: string;
  testID?: string;
}

// Uncontrolled: feeding the stored value back on each keystroke would move the cursor.
function Field({
  label,
  initial,
  onChange,
  keyboardType = 'default',
  secret = false,
  placeholder,
  testID,
}: FieldProps) {
  return (
    <View style={styles.field}>
      <Text style={font('Inter', 12, { weight: 500, color: StartColors.muted })}>{label}</Text>
      <TextInput
        testID={testID}
        accessibilityLabel={label}
        defaultValue={initial}
        onChangeText={onChange}
        placeholder={placeholder}
        placeholderTextColor="#9CA3AF"
        keyboardType={keyboardType}
        secureTextEntry={secret}
        autoCapitalize="none"
        autoCorrect={false}
        spellCheck={false}
        autoComplete="off"
        textContentType={secret ? 'password' : 'none'}
        selectionColor={StartColors.link}
        cursorColor={StartColors.text}
        style={[styles.input, font('Inter', 16, { color: StartColors.text })]}
      />
    </View>
  );
}

function Label({ text }: { text: string }) {
  return <Text style={[styles.label, font('Inter', 14, { color: StartColors.text })]}>{text}</Text>;
}

/** A muted line of explanation; `hint` sets it off from the chips above it. */
/** Puts the Northwind Invest demo account back as a fresh install has it. */
function InvestReset({ onReset }: { onReset: () => void }) {
  const { strings } = useL10n();
  const [reset, setReset] = React.useState(false);
  return (
    <>
      <Tap
        testID="dev_invest_reset"
        onPress={() => {
          onReset();
          setReset(true);
        }}
        style={styles.investReset}
      >
        <Text style={font('Inter', 14, { weight: 600, color: StartColors.text })}>{strings.devInvestReset}</Text>
      </Tap>
      {reset && <Note text={strings.devInvestResetDone} hint />}
    </>
  );
}

function Note({ text, hint = false }: { text: string; hint?: boolean }) {
  return (
    <Text style={[hint && styles.hint, font('Inter', 13, { lineHeight: 18, color: StartColors.muted })]}>{text}</Text>
  );
}

interface ChipsProps<T> {
  options: ReadonlyArray<readonly [T, string]>;
  selected: T;
  onSelect: (value: T) => void;
}

function Chips<T>({ options, selected, onSelect }: ChipsProps<T>) {
  return (
    <View style={styles.chips}>
      {options.map(([value, label]) => {
        const isSelected = value === selected;
        return (
          <Tap
            key={label}
            onPress={() => onSelect(value)}
            accessibilityRole="radio"
            accessibilityState={{ checked: isSelected }}
            style={[styles.chip, isSelected ? styles.chipSelected : styles.chipIdle]}
          >
            {isSelected && <Icon name="check" color={StartColors.text} size={18} />}
            <Text style={font('Inter', 14, { weight: 500, color: StartColors.text })}>{label}</Text>
          </Tap>
        );
      })}
    </View>
  );
}

function LastOrder({ mdOrder }: { mdOrder: string | undefined }) {
  const { strings } = useL10n();
  return (
    <View style={styles.lastOrder}>
      <View style={styles.grow}>
        <Text style={font('Inter', 15, { color: StartColors.text })}>{strings.devLastOrder}</Text>
        <Text testID="dev_last_order" selectable style={mono(12, { color: StartColors.muted })}>
          {mdOrder ?? strings.devNone}
        </Text>
      </View>
      {mdOrder !== undefined && (
        <Tap onPress={() => Clipboard.setString(mdOrder)} style={styles.copy}>
          <Text style={font('Inter', 14, { weight: 500, color: StartColors.link })}>{strings.devCopy}</Text>
        </Tap>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  investReset: { minHeight: 44, justifyContent: 'center', alignSelf: 'flex-start' },
  screen: { flex: 1, backgroundColor: StartColors.background },
  page: { paddingHorizontal: 16 },
  grow: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  field: {
    marginBottom: 8,
    paddingHorizontal: 12,
    paddingTop: 8,
    borderWidth: 1,
    borderColor: '#79747E',
    borderRadius: 4,
    backgroundColor: StartColors.surface,
  },
  input: { minHeight: 40, paddingVertical: 6, paddingHorizontal: 0 },
  label: { paddingTop: 4, paddingBottom: 4 },
  hint: { paddingVertical: 8 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    minHeight: 32,
    paddingHorizontal: 12,
    borderRadius: 8,
    borderWidth: 1,
    overflow: 'hidden',
  },
  chipSelected: { backgroundColor: StartColors.selected, borderColor: StartColors.selected },
  chipIdle: { borderColor: '#79747E' },
  lastOrder: { flexDirection: 'row', alignItems: 'center', minHeight: 56 },
  copy: { minHeight: 40, paddingHorizontal: 12, justifyContent: 'center', borderRadius: 20, overflow: 'hidden' },
});
