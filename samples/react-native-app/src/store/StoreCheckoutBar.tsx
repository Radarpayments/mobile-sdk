import React, { useEffect, useState } from 'react';
import { ActivityIndicator, StyleSheet, Text, View } from 'react-native';

import { useAppScope, useL10n, usePrefs } from '../AppScope';
import { buildSdkConfig, StoreCheckout, type CheckoutState } from '../checkout/checkout';
import { useListenable } from '../listenable';
import type { OnOutcome } from '../navigation';
import { Icon } from '../ui/Icon';
import { Tap } from '../ui/Tap';
import type { CoverId } from './model/catalog';
import { ScenarioSheet } from './ScenarioSheet';
import { coverName } from './StartScreen';
import { body, buttonRadius, coverStyle, withAlpha } from './theme/coverStyle';

export interface StoreCheckoutBarProps {
  cover: CoverId;
  amountMinor: number;
  onOutcome: OnOutcome;
  onOpenSettings: () => void;
  /** Pays as soon as the bar is shown ("Order again"). */
  autoPay?: boolean;
  onAutoPayStarted?: () => void;
}

/**
 * The bottom of every cart in every cover: the demo-panel row, the "Pay" button and a registration error, if any.
 * Registers the order for `amountMinor` and opens the SDK sheet in the cover's brand.
 */
export function StoreCheckoutBar({
  cover,
  amountMinor,
  onOutcome,
  onOpenSettings,
  autoPay = false,
  onAutoPayStarted,
}: StoreCheckoutBarProps) {
  const scope = useAppScope();
  const prefs = usePrefs();
  const { strings, eur } = useL10n();
  const [checkout] = useState(() => new StoreCheckout(scope.prefs.setLastMdOrder, scope.presentSheet));
  const { state, busy } = useListenable(checkout);
  const [scenarioOpen, setScenarioOpen] = useState(false);

  const pay = async () => {
    const scenario = prefs.scenario;
    const outcome = await checkout.pay({
      registrar: scope.registrarFactory(prefs),
      config: buildSdkConfig({
        baseUrl: prefs.baseUrl,
        scenario,
        cover: coverStyle(cover),
        themeOverride: prefs.sheetTheme,
        locale: prefs.localeOverride,
        storeName: coverName[cover](strings),
        googlePayMerchantId: prefs.googlePayMerchantId,
        googlePayGateway: prefs.googlePayGateway,
        applePayMerchantId: prefs.applePayMerchantId,
      }),
      amountMinor,
      clientId: scenario.buyerWithCards ? prefs.clientId : undefined,
    });
    if (outcome) onOutcome(outcome, amountMinor);
  };

  // "Order again": straight to the payment sheet once the screen is up.
  useEffect(() => {
    if (autoPay && amountMinor > 0) {
      onAutoPayStarted?.();
      pay();
    }
    // Once, when the bar appears.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <View style={styles.bar}>
      {state.step === 'registrationFailed' && (
        <RegistrationError cover={cover} state={state} onOpenSettings={onOpenSettings} />
      )}
      <ScenarioRow cover={cover} buyerWithCards={prefs.scenario.buyerWithCards} onPress={() => setScenarioOpen(true)} />
      <PayButton
        testID="store_pay"
        cover={cover}
        label={strings.storePay(eur(amountMinor))}
        busy={busy}
        enabled={amountMinor > 0}
        onPress={pay}
      />
      <ScenarioSheet visible={scenarioOpen} onClose={() => setScenarioOpen(false)} />
    </View>
  );
}

/** Dashed row above the "Pay" button that opens the demo panel. */
export function ScenarioRow({
  cover,
  buyerWithCards,
  onPress,
}: {
  cover: CoverId;
  buyerWithCards: boolean;
  onPress: () => void;
}) {
  const style = coverStyle(cover);
  const { strings } = useL10n();
  const buyer = buyerWithCards ? strings.scenarioWithCards : strings.scenarioGuest;
  return (
    <Tap
      testID="store_scenario"
      onPress={onPress}
      rippleColor={style.dark ? '#FFFFFF1F' : undefined}
      style={[styles.scenario, { borderColor: style.border }]}
    >
      <Text style={[styles.grow, body(style, 13, { weight: 500, color: style.muted })]}>
        {strings.scenarioRow(buyer)}
      </Text>
      <Icon name="keyboard_arrow_down" color={style.muted} size={18} />
    </Tap>
  );
}

export interface PayButtonProps {
  cover: CoverId;
  label: string;
  busy: boolean;
  enabled: boolean;
  onPress: () => void;
  testID?: string;
}

/** The store's own "Pay" button in the cover's brand; shows progress while the order is registered. */
export function PayButton({ cover, label, busy, enabled, onPress, testID }: PayButtonProps) {
  const style = coverStyle(cover);
  const active = enabled && !busy;
  return (
    <Tap
      testID={testID}
      onPress={onPress}
      disabled={!active}
      accessibilityLabel={label}
      accessibilityState={{ disabled: !active, busy }}
      rippleColor={style.dark ? '#0000001F' : '#FFFFFF33'}
      style={[
        styles.pay,
        {
          borderRadius: buttonRadius(style),
          backgroundColor: active || busy ? style.primary : withAlpha(style.primary, 0.4),
        },
      ]}
    >
      {busy ? (
        <ActivityIndicator color={style.onPrimary} />
      ) : (
        <Text
          style={body(style, 15, {
            weight: 700,
            color: active ? style.onPrimary : withAlpha(style.onPrimary, 0.7),
          })}
        >
          {label}
        </Text>
      )}
    </Tap>
  );
}

/** Why the order was not created, with a way to the developer settings. */
export function RegistrationError({
  cover,
  state,
  onOpenSettings,
}: {
  cover: CoverId;
  state: Extract<CheckoutState, { step: 'registrationFailed' }>;
  onOpenSettings: () => void;
}) {
  const style = coverStyle(cover);
  const { strings } = useL10n();
  const text =
    state.reason.kind === 'failed'
      ? strings.storeErrorFailed(state.reason.message)
      : state.reason.kind === 'missingApiKey'
      ? strings.storeErrorApiKey
      : strings.storeErrorCredentials;
  return (
    <View style={[styles.error, { backgroundColor: style.surface }]} accessibilityLiveRegion="polite">
      <Text style={body(style, 13, { lineHeight: 18, color: style.text })}>{text}</Text>
      <Tap onPress={onOpenSettings} style={styles.errorAction}>
        <Text style={body(style, 13, { weight: 700, color: style.dark ? style.primary : style.accent })}>
          {strings.storeOpenSettings}
        </Text>
      </Tap>
    </View>
  );
}

const styles = StyleSheet.create({
  bar: { gap: 10 },
  grow: { flex: 1 },
  scenario: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: 48,
    paddingHorizontal: 14,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderRadius: 12,
    overflow: 'hidden',
  },
  pay: { minHeight: 52, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24, overflow: 'hidden' },
  error: { borderRadius: 12, paddingStart: 14, paddingEnd: 4, paddingTop: 10 },
  errorAction: { alignSelf: 'flex-start', minHeight: 40, justifyContent: 'center' },
});
