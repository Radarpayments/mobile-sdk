import React from 'react';
import { StyleSheet } from 'react-native';

import { useL10n } from '../AppScope';
import { Icon, type IconName } from './Icon';
import { Tap } from './Tap';

export interface BackButtonProps {
  color: string;
  onPress: () => void;
  icon?: IconName;
  /** A filled circle behind the icon, for a back button over a photo. */
  background?: string;
}

/** Back, pointing the way the language reads. */
export function BackButton({ color, onPress, icon = 'arrow_back', background }: BackButtonProps) {
  const { strings, rtl } = useL10n();
  return (
    <Tap
      onPress={onPress}
      accessibilityLabel={strings.storeBack}
      hitSlop={4}
      style={[styles.button, background !== undefined && { backgroundColor: background }]}
    >
      <Icon name={icon} color={color} mirrored={rtl} />
    </Tap>
  );
}

const styles = StyleSheet.create({
  button: {
    width: 44,
    height: 44,
    borderRadius: 22,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
});
