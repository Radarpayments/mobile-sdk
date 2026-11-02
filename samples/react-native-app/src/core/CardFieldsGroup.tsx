import React, { useState } from 'react';
import { StyleSheet, Text, TextInput, View, type KeyboardTypeOptions, type ViewStyle } from 'react-native';

import { useL10n } from '../AppScope';
import { font } from '../store/theme/coverStyle';
import { cvcLength, digitsOf, formatExpiry, formatPan, networkLabel, networkOf } from './cardNetwork';
import type { CoreColors } from './coreColors';
import type { CoreForm, CoreInput } from './coreToken';
import { errorText } from './errorText';

const RADIUS = 12;
const STROKE = 1.5;
const CELL_HEIGHT = 48;

type CardInput = 'pan' | 'expiry' | 'cvc' | 'holder';

const cardInputs: readonly CardInput[] = ['pan', 'expiry', 'cvc', 'holder'];

export interface CardFieldsGroupProps {
  form: CoreForm;
  onChange: (input: CardInput, value: string) => void;
  errors: Partial<Record<CoreInput, string>>;
  colors: CoreColors;
}

/**
 * The card fields as one group, the way the SDK's own form shows them: the number across the top with the network,
 * expiry and CVC side by side, the name below, split by hairlines in one frame. The focused cell is outlined in the
 * accent colour, a cell with an error in red; the error itself goes under the group.
 */
export function CardFieldsGroup({ form, onChange, errors, colors }: CardFieldsGroupProps) {
  const { strings } = useL10n();
  const [focused, setFocused] = useState<CardInput>();
  const network = networkOf(form.pan);
  const failed = cardInputs.find(input => errors[input] !== undefined);

  const cell = (input: CardInput, placeholder: string, corners: ViewStyle, extra: Partial<CellProps> = {}) => (
    <Cell
      input={input}
      value={form[input]}
      placeholder={placeholder}
      corners={corners}
      highlight={errors[input] !== undefined ? colors.error : focused === input ? colors.accent : undefined}
      onFocus={() => setFocused(input)}
      onBlur={() => setFocused(current => (current === input ? undefined : current))}
      onChangeText={value => onChange(input, value)}
      colors={colors}
      {...extra}
    />
  );

  return (
    <View>
      <Text style={[styles.label, font('Inter', 13, { weight: 600, color: colors.muted })]}>
        {strings.coreCardGroup}
      </Text>
      <View style={[styles.frame, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        {cell('pan', strings.corePan, styles.top, {
          keyboardType: 'number-pad',
          format: formatPan,
          // The network the number belongs to, as the SDK's form shows its logo there.
          trailing: network === 'unknown' ? undefined : networkLabel[network],
        })}
        <View style={[styles.hairline, { backgroundColor: colors.border }]} />
        <View style={styles.row}>
          <View style={styles.grow}>
            {cell('expiry', strings.coreExpiryPlaceholder, styles.square, {
              keyboardType: 'number-pad',
              format: formatExpiry,
            })}
          </View>
          <View style={[styles.verticalHairline, { backgroundColor: colors.border }]} />
          <View style={styles.grow}>
            {cell('cvc', strings.coreCvc, styles.square, {
              keyboardType: 'number-pad',
              format: text => digitsOf(text).slice(0, cvcLength(network)),
            })}
          </View>
        </View>
        <View style={[styles.hairline, { backgroundColor: colors.border }]} />
        {cell('holder', strings.coreHolder, styles.bottom, { autoCapitalize: 'characters' })}
      </View>
      {failed && (
        <Text testID={`core_error_${failed}`} style={[styles.error, font('Inter', 12, { color: colors.error })]}>
          {errorText(strings, errors[failed] ?? '')}
        </Text>
      )}
    </View>
  );
}

interface CellProps {
  input: CardInput;
  value: string;
  placeholder: string;
  corners: ViewStyle;
  highlight: string | undefined;
  onFocus: () => void;
  onBlur: () => void;
  onChangeText: (value: string) => void;
  colors: CoreColors;
  keyboardType?: KeyboardTypeOptions;
  autoCapitalize?: 'characters' | 'none';
  /** Keeps what may be typed and shows it formatted, e.g. the number in groups. */
  format?: (text: string) => string;
  trailing?: string;
}

function Cell(props: CellProps) {
  const { input, value, placeholder, corners, highlight, colors, format = (text: string) => text, trailing } = props;
  return (
    <View style={styles.cell}>
      <TextInput
        testID={`core_field_${input}`}
        value={value}
        onChangeText={text => props.onChangeText(format(text))}
        onFocus={props.onFocus}
        onBlur={props.onBlur}
        placeholder={placeholder}
        placeholderTextColor={colors.placeholder}
        keyboardType={props.keyboardType ?? 'default'}
        autoCapitalize={props.autoCapitalize ?? 'none'}
        autoCorrect={false}
        spellCheck={false}
        selectionColor={colors.accent}
        cursorColor={colors.accent}
        // One font for the placeholder and the value, with even digits, so nothing jumps while typing.
        style={[styles.input, font('Inter', 15, { color: colors.text }), styles.tabular]}
      />
      {trailing !== undefined && (
        <Text style={font('Inter', 12, { weight: 700, color: colors.muted })}>{trailing}</Text>
      )}
      {highlight !== undefined && (
        <View
          pointerEvents="none"
          style={[StyleSheet.absoluteFill, styles.outline, corners, { borderColor: highlight }]}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  label: { paddingBottom: 6 },
  frame: { borderWidth: STROKE, borderRadius: RADIUS, overflow: 'hidden' },
  row: { flexDirection: 'row' },
  grow: { flex: 1 },
  hairline: { height: STROKE },
  verticalHairline: { width: STROKE },
  cell: { height: CELL_HEIGHT, flexDirection: 'row', alignItems: 'center', paddingStart: 14, paddingEnd: 10 },
  input: { flex: 1, padding: 0, alignSelf: 'stretch' },
  tabular: { fontVariant: ['tabular-nums'] },
  outline: { borderWidth: STROKE },
  top: { borderTopLeftRadius: RADIUS - STROKE, borderTopRightRadius: RADIUS - STROKE },
  bottom: { borderBottomLeftRadius: RADIUS - STROKE, borderBottomRightRadius: RADIUS - STROKE },
  square: {},
  error: { paddingTop: 6 },
});
