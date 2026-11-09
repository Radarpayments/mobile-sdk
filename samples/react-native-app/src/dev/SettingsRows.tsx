import React, { useEffect, useRef, useState, type ReactNode } from 'react';
import {
  Animated,
  Modal,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  type KeyboardTypeOptions,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useL10n } from '../AppScope';
import { font, mono, StartColors } from '../store/theme/coverStyle';
import { Icon } from '../ui/Icon';
import { toCaps } from '../ui/caps';
import { Tap } from '../ui/Tap';

/** The settings page: grey like the iOS grouped form, which the four sample apps share. */
export const SettingsBackground = '#F2F2F7';
const ErrorRed = '#B91C1C';
const DoneGreen = '#15803D';
const Placeholder = '#9CA3AF';

/** How long "Copied" stays before the row reads as before. */
const DONE_MS = 2000;

/** A titled group of rows on a white card, with an optional explanation under it. */
export function SettingsSection({
  title,
  footer = [],
  children,
}: {
  title: string;
  footer?: ReadonlyArray<string>;
  children: ReactNode;
}) {
  return (
    <>
      <Text style={styles.sectionTitle} accessibilityRole="header">
        {toCaps(title)}
      </Text>
      <View style={styles.card}>{children}</View>
      {footer.map(text => (
        <Text key={text} style={styles.footer}>
          {text}
        </Text>
      ))}
    </>
  );
}

/** The hairline between two rows of a card, indented like the iOS form's. */
export function RowDivider() {
  return <View style={styles.divider} />;
}

interface FieldRowProps {
  label: string;
  initial: string;
  onChange: (value: string) => void;
  keyboardType?: KeyboardTypeOptions;
  secret?: boolean;
  placeholder?: string;
  error?: string;
  testID?: string;
}

/**
 * A text field with its name always above the value. A secret field masks the value and has a button to show it,
 * since a typo in a merchant password is otherwise impossible to spot.
 */
// Uncontrolled: feeding the stored value back on each keystroke would move the cursor.
export function FieldRow({
  label,
  initial,
  onChange,
  keyboardType = 'default',
  secret = false,
  placeholder,
  error,
  testID,
}: FieldRowProps) {
  const { strings } = useL10n();
  const [shown, setShown] = useState(false);
  return (
    <View style={[styles.fieldRow, secret && styles.fieldRowSecret]}>
      <View style={styles.fieldBody}>
        {/* The field below carries the label for screen readers; this caption would be read a second time. */}
        <Text style={styles.fieldLabel} importantForAccessibility="no" accessibilityElementsHidden>
          {label}
        </Text>
        <TextInput
          testID={testID}
          accessibilityLabel={label}
          defaultValue={initial}
          onChangeText={onChange}
          placeholder={placeholder}
          placeholderTextColor={Placeholder}
          keyboardType={keyboardType}
          secureTextEntry={secret && !shown}
          autoCapitalize="none"
          autoCorrect={false}
          spellCheck={false}
          autoComplete="off"
          textContentType={secret ? 'password' : 'none'}
          selectionColor={StartColors.link}
          cursorColor={StartColors.text}
          style={styles.input}
        />
        {error !== undefined && <Text style={styles.error}>{error}</Text>}
      </View>
      {secret && (
        <Tap
          onPress={() => setShown(!shown)}
          accessibilityRole="button"
          accessibilityLabel={shown ? strings.devHideSecret : strings.devShowSecret}
          style={styles.reveal}
        >
          <Icon name={shown ? 'visibility_off_outlined' : 'visibility_outlined'} color={StartColors.muted} />
        </Tap>
      )}
    </View>
  );
}

interface ChoiceRowProps<T> {
  title: string;
  options: ReadonlyArray<readonly [T, string]>;
  selected: T;
  onSelect: (value: T) => void;
}

/** A choice between two values, as a segmented control next to its name. */
export function SegmentRow<T>({ title, options, selected, onSelect }: ChoiceRowProps<T>) {
  return (
    <View style={[styles.row, styles.segmentRow]}>
      <Text style={[styles.grow, styles.rowText]}>{title}</Text>
      <View style={styles.track} accessibilityRole="radiogroup">
        {options.map(([value, label]) => {
          const on = value === selected;
          return (
            <Tap
              key={label}
              accessibilityRole="radio"
              accessibilityState={{ checked: on }}
              onPress={() => onSelect(value)}
              style={[styles.segment, on && styles.segmentOn]}
            >
              <Text numberOfLines={1} style={on ? styles.segmentTextOn : styles.segmentText}>
                {label}
              </Text>
            </Tap>
          );
        })}
      </View>
    </View>
  );
}

/** A choice among several values: the row shows the current one and opens the list in a bottom sheet. */
export function PickerRow<T>({ title, options, selected, onSelect }: ChoiceRowProps<T>) {
  const { rtl } = useL10n();
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);
  return (
    <>
      <Tap onPress={() => setOpen(true)} style={[styles.row, styles.pickerRow]}>
        <Text style={[styles.grow, styles.rowText]}>{title}</Text>
        <Text style={styles.rowValue}>{options.find(([value]) => value === selected)?.[1]}</Text>
        <Icon name="chevron_right" color={Placeholder} size={20} mirrored={rtl} />
      </Tap>
      <Modal
        visible={open}
        transparent
        animationType="fade"
        onRequestClose={close}
        statusBarTranslucent
        navigationBarTranslucent
      >
        {/* Screen readers close it with their escape gesture or Back, so the scrim is for taps only. */}
        <Pressable style={styles.scrim} onPress={close} accessible={false} />
        <PickerSheet
          title={title}
          options={options}
          selected={selected}
          onClose={close}
          onSelect={value => {
            onSelect(value);
            close();
          }}
        />
      </Modal>
    </>
  );
}

function PickerSheet<T>({ title, options, selected, onSelect, onClose }: ChoiceRowProps<T> & { onClose: () => void }) {
  const insets = useSafeAreaInsets();
  const rise = useRef(new Animated.Value(1)).current;
  useEffect(() => {
    Animated.timing(rise, { toValue: 0, duration: 220, useNativeDriver: true }).start();
  }, [rise]);
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
      <Text style={styles.sheetTitle} accessibilityRole="header">
        {title}
      </Text>
      <ScrollView accessibilityRole="radiogroup">
        {options.map(([value, label]) => {
          const on = value === selected;
          return (
            <Tap
              key={label}
              accessibilityRole="radio"
              accessibilityState={{ checked: on }}
              onPress={() => onSelect(value)}
              style={styles.option}
            >
              <View style={[styles.radio, on && styles.radioOn]}>{on && <View style={styles.radioDot} />}</View>
              <Text style={styles.rowText}>{label}</Text>
            </Tap>
          );
        })}
      </ScrollView>
    </Animated.View>
  );
}

/** The last registered order, monospaced, with Copy that turns into "Copied" for a moment. */
export function LastOrderRow({ mdOrder, onCopy }: { mdOrder: string | undefined; onCopy: (mdOrder: string) => void }) {
  const { strings } = useL10n();
  const [copied, flash] = useDoneFlag();
  return (
    <View style={[styles.row, styles.lastOrderRow]}>
      <View style={[styles.grow, styles.lastOrderBody]}>
        <Text style={styles.rowText}>{strings.devLastOrder}</Text>
        <Text testID="dev_last_order" selectable style={mono(12, { color: StartColors.muted })}>
          {mdOrder ?? strings.devNone}
        </Text>
      </View>
      {mdOrder !== undefined &&
        (copied ? (
          <View style={styles.copied}>
            <DoneMark text={strings.devCopied} />
          </View>
        ) : (
          <Tap
            onPress={() => {
              onCopy(mdOrder);
              flash();
            }}
            style={styles.copy}
          >
            <Text style={styles.copyText}>{strings.devCopy}</Text>
          </Tap>
        ))}
    </View>
  );
}

function DoneMark({ text }: { text: string }) {
  return (
    <View style={styles.done}>
      <Text style={styles.doneText}>{text}</Text>
      <Icon name="check" color={DoneGreen} size={18} />
    </View>
  );
}

/** True for `DONE_MS` after `flash()`. */
function useDoneFlag(): [boolean, () => void] {
  const [done, setDone] = useState(false);
  useEffect(() => {
    if (!done) return;
    const timer = setTimeout(() => setDone(false), DONE_MS);
    return () => clearTimeout(timer);
  }, [done]);
  return [done, () => setDone(true)];
}

const styles = StyleSheet.create({
  sectionTitle: {
    paddingStart: 4,
    paddingTop: 24,
    paddingBottom: 8,
    ...font('Inter', 12, { weight: 600, color: StartColors.muted, letterSpacing: 0.6 }),
  },
  card: { borderRadius: 14, overflow: 'hidden', backgroundColor: StartColors.surface },
  footer: {
    paddingHorizontal: 4,
    paddingTop: 8,
    ...font('Inter', 13, { lineHeight: 18, color: StartColors.muted }),
  },
  divider: { marginStart: 16, height: StyleSheet.hairlineWidth, backgroundColor: StartColors.border },
  grow: { flex: 1 },
  row: { flexDirection: 'row', alignItems: 'center', minHeight: 56, paddingHorizontal: 16 },
  rowText: font('Inter', 16, { color: StartColors.text }),
  rowValue: font('Inter', 16, { color: StartColors.muted }),
  fieldRow: { flexDirection: 'row', alignItems: 'center', minHeight: 56, paddingHorizontal: 16 },
  fieldRowSecret: { paddingEnd: 4 },
  fieldBody: { flex: 1, paddingVertical: 10 },
  fieldLabel: font('Inter', 12, { weight: 500, color: StartColors.muted }),
  input: {
    minHeight: 32,
    paddingVertical: 4,
    paddingHorizontal: 0,
    ...font('Inter', 16, { color: StartColors.text }),
  },
  error: { paddingTop: 4, ...font('Inter', 12, { color: ErrorRed }) },
  reveal: {
    width: 44,
    height: 44,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 22,
    overflow: 'hidden',
  },
  segmentRow: { paddingVertical: 8, gap: 12 },
  track: { flexDirection: 'row', padding: 2, borderRadius: 10, backgroundColor: StartColors.selected },
  segment: {
    minHeight: 36,
    paddingHorizontal: 12,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 8,
    // Clipping keeps the Android ripple round; on iOS it would also clip the selected segment's shadow.
    overflow: Platform.OS === 'android' ? 'hidden' : 'visible',
  },
  segmentOn: { backgroundColor: StartColors.surface, boxShadow: '0px 1px 2px rgba(0, 0, 0, 0.16)' },
  segmentText: font('Inter', 14, { color: StartColors.text }),
  segmentTextOn: font('Inter', 14, { weight: 600, color: StartColors.text }),
  pickerRow: { paddingEnd: 12, gap: 4 },
  scrim: { ...StyleSheet.absoluteFill, backgroundColor: 'rgba(0, 0, 0, 0.32)' },
  sheet: {
    position: 'absolute',
    start: 0,
    end: 0,
    bottom: 0,
    maxHeight: '85%',
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
  sheetTitle: {
    paddingHorizontal: 24,
    paddingVertical: 8,
    ...font('Inter', 17, { weight: 600, color: StartColors.text }),
  },
  option: { flexDirection: 'row', alignItems: 'center', gap: 16, minHeight: 52, paddingHorizontal: 24 },
  radio: {
    width: 20,
    height: 20,
    borderRadius: 10,
    borderWidth: 2,
    borderColor: StartColors.muted,
    alignItems: 'center',
    justifyContent: 'center',
  },
  radioOn: { borderColor: StartColors.text },
  radioDot: { width: 10, height: 10, borderRadius: 5, backgroundColor: StartColors.text },
  lastOrderRow: { paddingEnd: 8 },
  lastOrderBody: { paddingVertical: 10 },
  copy: { minHeight: 48, paddingHorizontal: 12, justifyContent: 'center', borderRadius: 24, overflow: 'hidden' },
  copyText: font('Inter', 15, { weight: 500, color: StartColors.link }),
  copied: { paddingHorizontal: 8 },
  done: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  doneText: font('Inter', 15, { weight: 500, color: DoneGreen }),
});
