import React from 'react';
import { Image, StyleSheet, Text, useWindowDimensions, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useAppScope, useL10n } from '../../AppScope';
import type { ScreenProps } from '../../navigation';
import { BackButton } from '../../ui/BackButton';
import { SystemBars } from '../../ui/SystemBars';
import { Tap } from '../../ui/Tap';
import { product as findProduct } from '../model/catalog';
import { body, coverStyle, heading } from '../theme/coverStyle';
import { photoFill } from '../../ui/photoFill';

const style = coverStyle('furniture');
const PHOTO_SHARE = 0.46;
const swatches = ['#1C1917', '#E7E5E4', style.accent];

/** One product: a large photo, the name, colours, delivery terms and "Add to cart". */
export function FurnitureProductScreen({ navigation, route }: ScreenProps<'FurnitureProduct'>) {
  const { store } = useAppScope();
  const { strings, eur } = useL10n();
  const insets = useSafeAreaInsets();
  const { height } = useWindowDimensions();
  const product = findProduct('furniture', route.params.productId);
  return (
    <View style={styles.screen}>
      <SystemBars lightBackground />
      <View style={{ height: height * PHOTO_SHARE }}>
        {product.image && <Image source={product.image} resizeMode="cover" style={photoFill} />}
        <View style={[styles.back, { top: insets.top + 12 }]}>
          <BackButton
            icon="chevron_left"
            color={style.text}
            background="rgba(255, 255, 255, 0.92)"
            onPress={() => navigation.goBack()}
          />
        </View>
      </View>
      <View style={[styles.details, { paddingBottom: 16 + insets.bottom }]}>
        <Text style={heading(style, 32, { weight: 600, lineHeight: 34 })} accessibilityRole="header">
          {product.name(strings)}
        </Text>
        {product.subtitle && <Text style={body(style, 14, { color: style.muted })}>{product.subtitle(strings)}</Text>}
        <Text style={[styles.label, body(style, 13, { weight: 600, color: style.text })]}>
          {strings.furnitureColours}
        </Text>
        <View style={styles.swatches}>
          {swatches.map((colour, index) => (
            <View key={colour} style={[styles.swatch, index === 0 ? styles.swatchSelected : styles.swatchIdle]}>
              <View style={[styles.swatchFill, { backgroundColor: colour }]} />
            </View>
          ))}
        </View>
        <Text style={[styles.delivery, body(style, 14, { lineHeight: 20, color: style.muted })]}>
          {strings.furnitureDelivery}
        </Text>
        <View style={styles.grow} />
        <Tap
          testID="furniture_add"
          rippleColor="#FFFFFF33"
          onPress={() => {
            store.add('furniture', product);
            navigation.goBack();
          }}
          style={styles.add}
        >
          <Text style={body(style, 15, { weight: 600, color: style.onPrimary })}>
            {strings.storeAddToCart(eur(product.priceMinor))}
          </Text>
        </Tap>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: style.background },
  back: { position: 'absolute', start: 12 },
  details: { flex: 1, paddingHorizontal: 20, paddingTop: 16 },
  grow: { flex: 1 },
  label: { paddingTop: 16, paddingBottom: 8 },
  swatches: { flexDirection: 'row', gap: 10 },
  swatch: { width: 28, height: 28, borderRadius: 14, borderWidth: 1, padding: 4 },
  swatchSelected: { borderColor: style.text, borderWidth: 2 },
  swatchIdle: { borderColor: style.border },
  swatchFill: { flex: 1, borderRadius: 10 },
  delivery: { paddingTop: 16 },
  add: {
    minHeight: 52,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: style.radius,
    backgroundColor: style.primary,
    overflow: 'hidden',
  },
});
