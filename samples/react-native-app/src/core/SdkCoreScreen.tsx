import Clipboard from '@react-native-clipboard/clipboard';
import React, { useState, type ReactNode } from 'react';
import { ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAppScope, useL10n } from '../AppScope';
import { useBackHandler, type ScreenProps } from '../navigation';
import { font, mono } from '../store/theme/coverStyle';
import { BackButton } from '../ui/BackButton';
import { Icon } from '../ui/Icon';
import { SystemBars } from '../ui/SystemBars';
import { Tap } from '../ui/Tap';
import { CardFieldsGroup } from './CardFieldsGroup';
import { cvcLength, digitsOf, networkOf } from './cardNetwork';
import { useCoreColors, type CoreColors } from './coreColors';
import { coreRequest, generateToken, variantOf, type CoreForm, type CoreInput, type SdkCoreVariant } from './coreToken';
import { errorText } from './errorText';
import { KeyboardAware } from '../ui/KeyboardAware';

/**
 * SDK Core: a card form of the merchant's own, encrypted on the device into the token the merchant's server sends to
 * the gateway. Card data stay in memory; mdOrder, key, bindingId and the variant are remembered between launches.
 */
export function SdkCoreScreen({ navigation }: ScreenProps<'Core'>) {
  const { prefs } = useAppScope();
  const { strings } = useL10n();
  const colors = useCoreColors();
  const insets = useSafeAreaInsets();
  const [variant, setVariant] = useState<SdkCoreVariant>(() => variantOf(prefs.sdkCoreVariant));
  const [form, setForm] = useState<CoreForm>(() => ({
    pan: '',
    expiry: '',
    cvc: '',
    holder: '',
    mdOrder: prefs.mdOrder,
    bindingId: prefs.bindingId,
    pubKey: prefs.pubKeyPem,
  }));
  const [errors, setErrors] = useState<Partial<Record<CoreInput, string>>>({});
  const [token, setToken] = useState<string>();
  // Back from the token goes to the form.
  useBackHandler(token !== undefined, () => setToken(undefined));

  const edit = (input: CoreInput, value: string) => {
    setForm(current => {
      const next = { ...current, [input]: value };
      // A new network may take a shorter CVC (Amex has 4 digits, the others 3).
      if (input === 'pan') next.cvc = next.cvc.slice(0, cvcLength(networkOf(value)));
      return next;
    });
    setErrors(current => {
      const rest = { ...current };
      delete rest[input];
      return rest;
    });
  };

  const remembered: Partial<Record<CoreInput, (value: string) => void>> = {
    mdOrder: prefs.setMdOrder,
    bindingId: prefs.setBindingId,
    pubKey: prefs.setPubKeyPem,
  };
  const editRemembered = (input: CoreInput, value: string) => {
    edit(input, value);
    remembered[input]?.(value);
  };

  const generate = async () => {
    const outcome = await generateToken(coreRequest(variant, form));
    if ('token' in outcome) {
      setErrors({});
      setToken(outcome.token);
    } else {
      setErrors(outcome.errors);
    }
  };

  const pasted = (input: CoreInput, label: string, options: { hint?: string; multiline?: boolean } = {}) => (
    <PastedInput
      input={input}
      label={label}
      value={form[input]}
      onChange={value => editRemembered(input, value)}
      error={errors[input]}
      colors={colors}
      {...options}
    />
  );

  return (
    <View style={[styles.screen, { backgroundColor: colors.background, paddingTop: insets.top }]}>
      <SystemBars lightBackground={!colors.dark} />
      <KeyboardAware>
        <ScrollView
          contentContainerStyle={[styles.page, { paddingBottom: 24 + insets.bottom }]}
          keyboardShouldPersistTaps="handled"
        >
          <View style={styles.header}>
            <BackButton color={colors.text} onPress={() => navigation.goBack()} />
            <View style={styles.grow}>
              <Text style={font('Inter', 22, { weight: 800, color: colors.text })} accessibilityRole="header">
                {strings.coreTitle}
              </Text>
              {token !== undefined && (
                <Text style={font('Inter', 13, { color: colors.muted })}>{strings.coreDoneSubtitle}</Text>
              )}
            </View>
          </View>
          {token !== undefined ? (
            <TokenPanel token={token} onAgain={() => setToken(undefined)} colors={colors} />
          ) : (
            <View style={styles.form}>
              <VariantSwitch
                selected={variant}
                onSelect={next => {
                  setVariant(next);
                  setErrors({});
                  prefs.setSdkCoreVariant(next);
                }}
                colors={colors}
              />
              <Text style={font('Inter', 13, { lineHeight: 18, color: colors.muted })}>
                {variant === 'new'
                  ? strings.coreHintNew
                  : variant === 'instant'
                  ? strings.coreHintInstant
                  : strings.coreHintStored}
              </Text>
              {variant === 'stored' ? (
                <>
                  {pasted('bindingId', strings.coreBinding)}
                  <Labelled label={strings.coreCvcOptional} error={errors.cvc} colors={colors}>
                    <TextInput
                      testID="core_field_cvc"
                      value={form.cvc}
                      onChangeText={text => edit('cvc', digitsOf(text).slice(0, 4))}
                      keyboardType="number-pad"
                      selectionColor={colors.accent}
                      cursorColor={colors.accent}
                      style={[styles.box, styles.cvc, mono(15, { color: colors.text }), boxColors(colors, errors.cvc)]}
                    />
                  </Labelled>
                </>
              ) : (
                <CardFieldsGroup form={form} onChange={edit} errors={errors} colors={colors} />
              )}
              {variant !== 'instant' && pasted('mdOrder', strings.coreMdOrder, { hint: strings.coreMdOrderHint })}
              {pasted('pubKey', strings.corePubKey, { multiline: true })}
              <Tap
                testID="core_generate"
                onPress={generate}
                rippleColor={colors.dark ? '#0000001F' : '#FFFFFF33'}
                style={[styles.button, { backgroundColor: colors.button }]}
              >
                <Text style={font('Inter', 16, { weight: 700, color: colors.onButton })}>{strings.coreGenerate}</Text>
              </Tap>
            </View>
          )}
        </ScrollView>
      </KeyboardAware>
    </View>
  );
}

function boxColors(colors: CoreColors, error: string | undefined) {
  return { backgroundColor: colors.surface, borderColor: error !== undefined ? colors.error : colors.border };
}

function VariantSwitch({
  selected,
  onSelect,
  colors,
}: {
  selected: SdkCoreVariant;
  onSelect: (variant: SdkCoreVariant) => void;
  colors: CoreColors;
}) {
  const { strings } = useL10n();
  const variants: ReadonlyArray<readonly [SdkCoreVariant, string]> = [
    ['new', strings.coreVariantNew],
    ['instant', strings.coreVariantInstant],
    ['stored', strings.coreVariantStored],
  ];
  return (
    <View style={[styles.track, { backgroundColor: colors.track }]} accessibilityRole="tablist">
      {variants.map(([variant, label]) => {
        const isSelected = variant === selected;
        return (
          <Tap
            key={variant}
            testID={`core_variant_${variant}`}
            onPress={() => onSelect(variant)}
            accessibilityRole="tab"
            accessibilityState={{ selected: isSelected }}
            style={[styles.segment, isSelected && { backgroundColor: colors.surface }]}
          >
            <Text
              style={font('Inter', 13, {
                weight: isSelected ? 700 : 500,
                color: isSelected ? colors.text : colors.muted,
              })}
            >
              {label}
            </Text>
          </Tap>
        );
      })}
    </View>
  );
}

interface PastedInputProps {
  input: CoreInput;
  label: string;
  value: string;
  onChange: (value: string) => void;
  error: string | undefined;
  colors: CoreColors;
  hint?: string;
  multiline?: boolean;
}

/** A value copied from elsewhere (mdOrder, bindingId, the key): monospace, with "Paste". */
function PastedInput({ input, label, value, onChange, error, colors, hint, multiline = false }: PastedInputProps) {
  const { strings } = useL10n();
  const paste = async () => {
    const text = (await Clipboard.getString()).trim();
    if (text) onChange(text);
  };
  return (
    <Labelled label={label} error={error} colors={colors}>
      {/* In a tall field "Paste" stays by the first line. */}
      <View
        style={[
          styles.box,
          styles.pastedBox,
          multiline ? styles.alignTop : styles.alignCenter,
          boxColors(colors, error),
        ]}
      >
        <TextInput
          testID={`core_field_${input}`}
          value={value}
          onChangeText={onChange}
          placeholder={hint}
          placeholderTextColor={colors.placeholder}
          // A whole 2048-bit PEM key: its 64-character lines wrap in two.
          multiline={multiline}
          numberOfLines={multiline ? 4 : 1}
          autoCapitalize="none"
          autoCorrect={false}
          spellCheck={false}
          selectionColor={colors.accent}
          cursorColor={colors.accent}
          style={[
            styles.pastedText,
            multiline
              ? mono(12, { lineHeight: 16, color: colors.text })
              : mono(15, { lineHeight: 20, color: colors.text }),
            multiline && styles.pem,
          ]}
        />
        <Tap onPress={paste} style={styles.paste}>
          <Text style={font('Inter', 13, { weight: 600, color: colors.accent })}>{strings.corePaste}</Text>
        </Tap>
      </View>
    </Labelled>
  );
}

/** `children` with a `label` above it and the `error`, if any, below. */
function Labelled({
  label,
  error,
  colors,
  children,
}: {
  label: string;
  error: string | undefined;
  colors: CoreColors;
  children: ReactNode;
}) {
  const { strings } = useL10n();
  return (
    <View>
      <Text style={[styles.label, font('Inter', 13, { weight: 600, color: colors.muted })]}>{label}</Text>
      {children}
      {error !== undefined && (
        <Text style={[styles.fieldError, font('Inter', 12, { color: colors.error })]}>{errorText(strings, error)}</Text>
      )}
    </View>
  );
}

function TokenPanel({ token, onAgain, colors }: { token: string; onAgain: () => void; colors: CoreColors }) {
  const { strings } = useL10n();
  const [copied, setCopied] = useState(false);
  return (
    <View style={styles.form}>
      <View style={[styles.tokenCard, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <View style={[styles.badge, { backgroundColor: colors.successSoft }]}>
          <Icon name="check" color={colors.success} size={14} />
          <Text style={font('Inter', 12, { weight: 700, color: colors.success })}>{strings.coreTokenOk}</Text>
        </View>
        <View style={[styles.tokenBox, { backgroundColor: colors.track }]}>
          <Text testID="core_token" selectable style={mono(11, { lineHeight: 16, color: colors.text })}>
            {token}
          </Text>
        </View>
        <Tap
          onPress={() => {
            Clipboard.setString(token);
            setCopied(true);
          }}
          style={styles.copy}
        >
          <Text style={font('Inter', 13, { weight: 700, color: colors.accent })}>
            {copied ? strings.coreCopied : strings.coreCopy}
          </Text>
        </Tap>
      </View>
      <View style={styles.steps}>
        {[strings.coreStepSend, strings.coreStepGateway, strings.coreStepPay].map((step, index) => (
          <View key={step} style={styles.step}>
            <View style={[styles.stepNumber, { backgroundColor: colors.stepSoft }]}>
              <Text style={font('Inter', 12, { weight: 700, color: colors.step })}>{index + 1}</Text>
            </View>
            <Text style={[styles.grow, font('Inter', 14, { lineHeight: 19, color: colors.text })]}>{step}</Text>
          </View>
        ))}
      </View>
      <Tap onPress={onAgain} style={[styles.button, { backgroundColor: colors.track }]}>
        <Text style={font('Inter', 16, { weight: 700, color: colors.text })}>{strings.coreAgain}</Text>
      </Tap>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  page: { paddingHorizontal: 16 },
  grow: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingTop: 4, paddingBottom: 12 },
  form: { gap: 12 },
  track: { flexDirection: 'row', padding: 3, borderRadius: 12 },
  segment: {
    flex: 1,
    minHeight: 40,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 10,
    overflow: 'hidden',
  },
  label: { paddingBottom: 6 },
  fieldError: { paddingTop: 4 },
  box: { minHeight: 48, borderWidth: 1, borderRadius: 12 },
  cvc: { paddingHorizontal: 14 },
  pastedBox: { flexDirection: 'row', paddingStart: 14, paddingEnd: 4 },
  alignTop: { alignItems: 'flex-start' },
  alignCenter: { alignItems: 'center' },
  pastedText: { flex: 1, paddingVertical: 12, paddingHorizontal: 0 },
  pem: { minHeight: 4 * 16 + 24, maxHeight: 18 * 16 + 24, textAlignVertical: 'top' },
  paste: { minHeight: 48, paddingHorizontal: 12, justifyContent: 'center', borderRadius: 12, overflow: 'hidden' },
  button: {
    marginTop: 4,
    minHeight: 52,
    borderRadius: 14,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  tokenCard: { padding: 16, borderWidth: 1, borderRadius: 16 },
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    gap: 4,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
  },
  tokenBox: { marginTop: 12, padding: 12, borderRadius: 10 },
  copy: {
    alignSelf: 'flex-end',
    minHeight: 40,
    paddingHorizontal: 12,
    justifyContent: 'center',
    borderRadius: 20,
    overflow: 'hidden',
  },
  steps: { paddingTop: 4, gap: 10 },
  step: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  stepNumber: { width: 24, height: 24, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
});
