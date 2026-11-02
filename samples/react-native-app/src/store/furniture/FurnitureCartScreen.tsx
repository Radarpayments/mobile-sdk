import React from 'react';
import { Image, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useL10n } from '../../AppScope';
import type { OnOutcome } from '../../navigation';
import { BackButton } from '../../ui/BackButton';
import { Icon } from '../../ui/Icon';
import { Tap } from '../../ui/Tap';
import type { Cart, CartLine } from '../model/cart';
import { StoreCheckoutBar } from '../StoreCheckoutBar';
import { body, coverStyle, heading, withAlpha } from '../theme/coverStyle';

const style = coverStyle('furniture');

export interface FurnitureCartScreenProps {
  cart: Cart;
  onBack: () => void;
  onQuantity: (productId: string, quantity: number) => void;
  onOutcome: OnOutcome;
  onOpenSettings: () => void;
}

/** The cart: lines with a quantity stepper, totals, then the shared scenario row and "Pay". */
export function FurnitureCartScreen({ cart, onBack, onQuantity, onOutcome, onOpenSettings }: FurnitureCartScreenProps) {
  const { strings, eur } = useL10n();
  return (
    <SafeAreaView style={styles.screen}>
      <View style={styles.header}>
        <BackButton color={style.text} onPress={onBack} />
        <Text style={[styles.grow, heading(style, 26)]} accessibilityRole="header">
          {strings.storeCart}
        </Text>
      </View>
      <ScrollView style={styles.grow}>
        {cart.lines.map(line => (
          <View key={line.product.id} style={styles.divided}>
            <CartRow line={line} onQuantity={onQuantity} />
          </View>
        ))}
        <View style={styles.sums}>
          <SumRow label={strings.storeItems} value={eur(cart.totalMinor)} />
          <SumRow label={strings.furnitureDeliveryLabel} value={strings.furnitureDeliveryFree} />
          <SumRow label={strings.storeTotal} value={eur(cart.totalMinor)} bold />
        </View>
      </ScrollView>
      <View style={styles.checkout}>
        <StoreCheckoutBar
          cover="furniture"
          amountMinor={cart.totalMinor}
          onOutcome={onOutcome}
          onOpenSettings={onOpenSettings}
        />
      </View>
    </SafeAreaView>
  );
}

function CartRow({ line, onQuantity }: { line: CartLine; onQuantity: (productId: string, quantity: number) => void }) {
  const { strings } = useL10n();
  const { product, quantity } = line;
  return (
    <View style={styles.row}>
      {product.image && <Image source={product.image} resizeMode="cover" style={styles.photo} />}
      <View style={styles.rowText}>
        <Text style={body(style, 14, { weight: 600, color: style.text })}>{product.name(strings)}</Text>
        {product.subtitle && (
          <Text numberOfLines={1} style={body(style, 12, { color: style.muted })}>
            {product.subtitle(strings)}
          </Text>
        )}
      </View>
      <View style={styles.stepper}>
        <Tap
          onPress={() => onQuantity(product.id, quantity - 1)}
          accessibilityLabel={strings.storeDecrease}
          style={styles.step}
        >
          <Icon name="remove" color={style.text} size={16} />
        </Tap>
        <Text style={body(style, 14, { weight: 600, color: style.text })}>{quantity}</Text>
        <Tap
          onPress={() => onQuantity(product.id, quantity + 1)}
          accessibilityLabel={strings.storeIncrease}
          style={styles.step}
        >
          <Icon name="add" color={style.text} size={16} />
        </Tap>
      </View>
    </View>
  );
}

function SumRow({ label, value, bold = false }: { label: string; value: string; bold?: boolean }) {
  const text = body(style, bold ? 16 : 14, { weight: bold ? 700 : 400, color: bold ? style.text : style.muted });
  return (
    <View style={styles.sumRow}>
      <Text style={text}>{label}</Text>
      <Text style={text}>{value}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, paddingHorizontal: 16, backgroundColor: style.background },
  grow: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', paddingTop: 4, gap: 4 },
  divided: { borderBottomWidth: 1, borderBottomColor: withAlpha(style.border, 0.6) },
  row: { flexDirection: 'row', alignItems: 'center', paddingVertical: 10 },
  photo: { width: 64, height: 64, borderRadius: 10 },
  rowText: { flex: 1, paddingHorizontal: 12 },
  stepper: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: style.border,
    borderRadius: 10,
  },
  step: {
    width: 40,
    height: 40,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 10,
    overflow: 'hidden',
  },
  sums: { paddingVertical: 14, gap: 6 },
  sumRow: { flexDirection: 'row', justifyContent: 'space-between' },
  checkout: { paddingVertical: 12 },
});
