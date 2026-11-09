import React, { useEffect, useRef } from 'react';
import { Animated, Modal, Platform, Pressable, StyleSheet, Switch, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useL10n, usePrefs } from '../AppScope';
import { toCaps } from '../ui/caps';
import { Tap } from '../ui/Tap';
import { font, StartColors } from './theme/coverStyle';

/**
 * The demo panel. Neutral on purpose — it is a tool for the person showing the demo, not part of the store — and the
 * same in every cover. Every change is saved at once. `buyerChoice` is false where the buyer is always the one with
 * saved cards (Northwind): only "Reset" is left.
 */
export function ScenarioSheet({
  visible,
  onClose,
  buyerChoice = true,
}: {
  visible: boolean;
  onClose: () => void;
  buyerChoice?: boolean;
}) {
  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onClose}
      statusBarTranslucent
      navigationBarTranslucent
    >
      {/* Screen readers close it with their escape gesture or Back, so the scrim is for taps only. */}
      <Pressable style={styles.scrim} onPress={onClose} accessible={false} />
      <SheetBody onClose={onClose} buyerChoice={buyerChoice} />
    </Modal>
  );
}

function SheetBody({ onClose, buyerChoice }: { onClose: () => void; buyerChoice: boolean }) {
  const prefs = usePrefs();
  const { strings } = useL10n();
  const insets = useSafeAreaInsets();
  const rise = useRef(new Animated.Value(1)).current;
  useEffect(() => {
    Animated.timing(rise, { toValue: 0, duration: 220, useNativeDriver: true }).start();
  }, [rise]);
  // Changes start from the stored scenario, not this render's copy: two quick taps must both count.
  const scenario = prefs.scenario;
  const update = (change: Partial<typeof scenario>) => prefs.setScenario({ ...prefs.scenario, ...change });
  return (
    <Animated.View
      accessibilityViewIsModal
      onAccessibilityEscape={onClose}
      style={[
        styles.sheet,
        { paddingBottom: 16 + insets.bottom },
        { transform: [{ translateY: rise.interpolate({ inputRange: [0, 1], outputRange: [0, 400] }) }] },
      ]}
    >
      <View style={styles.handle} />
      <Text style={styles.title} accessibilityRole="header">
        {strings.scenarioTitle}
      </Text>
      <GroupHeader title={strings.scenarioBuyer} />
      {buyerChoice && (
        <View style={styles.segments} accessibilityRole="radiogroup">
          {[
            { value: false, label: strings.scenarioGuest, testID: 'scenario_guest' },
            { value: true, label: strings.scenarioWithCards, testID: 'scenario_with_cards' },
          ].map(({ value, label, testID }, index) => (
            <Tap
              key={testID}
              testID={testID}
              accessibilityRole="radio"
              accessibilityState={{ selected: scenario.buyerWithCards === value }}
              onPress={() => update({ buyerWithCards: value })}
              style={[
                styles.segment,
                index > 0 && styles.segmentNext,
                scenario.buyerWithCards === value && styles.segmentSelected,
              ]}
            >
              <Text style={styles.segmentLabel}>{label}</Text>
            </Tap>
          ))}
        </View>
      )}
      <View style={styles.hintRow}>
        <Text style={styles.hint}>{strings.scenarioWithCardsHint}</Text>
        <Tap onPress={prefs.resetClientId} style={styles.reset}>
          <Text style={styles.link}>{strings.scenarioReset}</Text>
        </Tap>
      </View>
      <GroupHeader title={strings.scenarioPayment} />
      <ToggleRow
        testID="scenario_wallet"
        title={Platform.OS === 'ios' ? strings.scenarioWalletApple : strings.scenarioWallet}
        value={scenario.wallet}
        onChange={wallet => update({ wallet })}
      />
      <View style={styles.divider} />
      <ToggleRow
        testID="scenario_scanner"
        title={strings.scenarioScanner}
        value={scenario.scanner}
        onChange={scanner => update({ scanner })}
      />
      <View style={styles.divider} />
      <ToggleRow
        testID="scenario_sdk_result"
        title={strings.scenarioSdkResult}
        value={scenario.sdkResultScreen}
        onChange={sdkResultScreen => update({ sdkResultScreen })}
      />
    </Animated.View>
  );
}

/** Caption of a group of settings, in capitals. */
export function GroupHeader({ title }: { title: string }) {
  return <Text style={styles.groupHeader}>{toCaps(title)}</Text>;
}

interface ToggleRowProps {
  title: string;
  value: boolean;
  onChange: (value: boolean) => void;
  testID: string;
}

function ToggleRow({ title, value, onChange, testID }: ToggleRowProps) {
  return (
    <Tap
      testID={testID}
      accessibilityRole="switch"
      accessibilityState={{ checked: value }}
      onPress={() => onChange(!value)}
      style={styles.toggleRow}
    >
      <Text style={styles.toggleTitle}>{title}</Text>
      <Switch
        value={value}
        onValueChange={onChange}
        trackColor={{ true: StartColors.text, false: StartColors.border }}
        thumbColor={Platform.OS === 'android' ? StartColors.surface : undefined}
        accessible={false}
      />
    </Tap>
  );
}

const styles = StyleSheet.create({
  scrim: { ...StyleSheet.absoluteFill, backgroundColor: 'rgba(0, 0, 0, 0.32)' },
  sheet: {
    position: 'absolute',
    start: 0,
    end: 0,
    bottom: 0,
    paddingHorizontal: 20,
    backgroundColor: StartColors.surface,
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
  },
  handle: {
    alignSelf: 'center',
    width: 32,
    height: 4,
    borderRadius: 2,
    marginVertical: 16,
    backgroundColor: '#CAC4D0',
  },
  title: font('Inter', 20, { weight: 800, color: StartColors.text }),
  groupHeader: {
    paddingTop: 20,
    paddingBottom: 8,
    ...font('Inter', 12, { weight: 700, color: StartColors.muted, letterSpacing: 0.5 }),
  },
  segments: { flexDirection: 'row', borderWidth: 1, borderColor: '#79747E', borderRadius: 20, overflow: 'hidden' },
  segment: { flex: 1, minHeight: 40, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 12 },
  segmentNext: { borderStartWidth: 1, borderStartColor: '#79747E' },
  segmentSelected: { backgroundColor: StartColors.selected },
  segmentLabel: font('Inter', 14, { weight: 500, color: StartColors.text }),
  hintRow: { flexDirection: 'row', alignItems: 'center', paddingTop: 4, gap: 8 },
  hint: { flex: 1, ...font('Inter', 13, { lineHeight: 18, color: StartColors.muted }) },
  reset: { minHeight: 40, paddingHorizontal: 12, justifyContent: 'center', borderRadius: 20, overflow: 'hidden' },
  link: font('Inter', 14, { weight: 500, color: StartColors.link }),
  toggleRow: { flexDirection: 'row', alignItems: 'center', minHeight: 52 },
  toggleTitle: { flex: 1, ...font('Inter', 15, { color: StartColors.text }) },
  divider: { height: StyleSheet.hairlineWidth, backgroundColor: StartColors.border },
});
