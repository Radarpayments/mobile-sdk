import React from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { useL10n } from '../../AppScope';
import { body, coverStyle, heading } from '../theme/coverStyle';

const style = coverStyle('coffee');

/** The coffee shop's success headline: the order number in large type, to show the barista. */
export function CoffeeOrderNumber({ number }: { number: number }) {
  const { strings } = useL10n();
  return (
    <View style={styles.headline}>
      <Text style={body(style, 17, { weight: 700, color: style.muted })}>{strings.resultSuccessCoffee}</Text>
      <Text testID="coffee_order_number" style={heading(style, 88, { weight: 800, lineHeight: 96 })}>
        {number}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({ headline: { alignItems: 'center' } });
