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
import { body, coverStyle, heading } from '../theme/coverStyle';

const style = coverStyle('coffee');
const RIPPLE = '#FFFFFF1F';

export interface CoffeeOrderScreenProps {
  cart: Cart;
  autoPay: boolean;
  onAutoPayStarted: () => void;
  onBack: () => void;
  onQuantity: (productId: string, quantity: number) => void;
  onOutcome: OnOutcome;
  onOpenSettings: () => void;
}

/** The order: items with quantities, where to pick it up, and the shared payment bar. */
export function CoffeeOrderScreen(props: CoffeeOrderScreenProps) {
  const { cart, autoPay, onAutoPayStarted, onBack, onQuantity, onOutcome, onOpenSettings } = props;
  const { strings } = useL10n();
  return (
    <SafeAreaView style={styles.screen}>
      <View style={styles.header}>
        <BackButton color={style.text} onPress={onBack} />
        <Text style={[styles.grow, heading(style, 24, { weight: 800 })]} accessibilityRole="header">
          {strings.coffeeOrderTitle}
        </Text>
      </View>
      <ScrollView style={styles.grow}>
        {cart.lines.map(line => (
          <OrderLine key={line.product.id} line={line} onQuantity={onQuantity} />
        ))}
        <View style={styles.pickup}>
          <Text style={body(style, 14, { color: style.muted })}>{strings.coffeePickup}</Text>
          <Text style={[styles.grow, body(style, 14, { weight: 700, color: style.text })]}>
            {strings.coffeePickupValue}
          </Text>
        </View>
      </ScrollView>
      <View style={styles.checkout}>
        <StoreCheckoutBar
          cover="coffee"
          amountMinor={cart.totalMinor}
          onOutcome={onOutcome}
          onOpenSettings={onOpenSettings}
          autoPay={autoPay}
          onAutoPayStarted={onAutoPayStarted}
        />
      </View>
    </SafeAreaView>
  );
}

function OrderLine({
  line,
  onQuantity,
}: {
  line: CartLine;
  onQuantity: (productId: string, quantity: number) => void;
}) {
  const { strings, eur } = useL10n();
  const { product, quantity } = line;
  return (
    <View style={styles.line}>
      {product.image && <Image source={product.image} resizeMode="cover" style={styles.photo} />}
      <View style={styles.lineText}>
        <Text style={body(style, 15, { weight: 700, color: style.text })}>{product.name(strings)}</Text>
        <Text style={body(style, 13, { color: style.muted })}>{eur(product.priceMinor * quantity)}</Text>
      </View>
      <Tap
        onPress={() => onQuantity(product.id, quantity - 1)}
        accessibilityLabel={strings.storeDecrease}
        rippleColor={RIPPLE}
        style={styles.step}
      >
        <Icon name="remove" color={style.text} />
      </Tap>
      <Text style={body(style, 14, { weight: 700, color: style.text })}>{quantity}</Text>
      <Tap
        onPress={() => onQuantity(product.id, quantity + 1)}
        accessibilityLabel={strings.storeIncrease}
        rippleColor={RIPPLE}
        style={styles.step}
      >
        <Icon name="add" color={style.text} />
      </Tap>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, paddingHorizontal: 16, backgroundColor: style.background },
  grow: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingTop: 4 },
  line: { flexDirection: 'row', alignItems: 'center', paddingVertical: 8 },
  photo: { width: 56, height: 56, borderRadius: 14 },
  lineText: { flex: 1, paddingHorizontal: 12 },
  step: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  pickup: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 12,
    padding: 14,
    borderRadius: style.radius,
    backgroundColor: style.surface,
  },
  checkout: { paddingVertical: 12 },
});
