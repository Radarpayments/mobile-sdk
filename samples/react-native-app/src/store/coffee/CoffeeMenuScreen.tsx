import React, { useState } from 'react';
import { Image, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useL10n } from '../../AppScope';
import { BackButton } from '../../ui/BackButton';
import { Icon } from '../../ui/Icon';
import { Tap } from '../../ui/Tap';
import type { Cart } from '../model/cart';
import { photos, products, type LocalText, type Product } from '../model/catalog';
import { body, coverStyle, heading } from '../theme/coverStyle';

const style = coverStyle('coffee');
const RIPPLE = '#FFFFFF1F';

type CoffeeTab = 'coffee' | 'cold' | 'bakery';

const tabs: ReadonlyArray<readonly [CoffeeTab, LocalText]> = [
  ['coffee', s => s.coffeeTabCoffee],
  ['cold', s => s.coffeeTabCold],
  ['bakery', s => s.coffeeTabBakery],
];

function tabOf(productId: string): CoffeeTab {
  switch (productId) {
    case 'iced_latte':
      return 'cold';
    case 'croissant':
      return 'bakery';
    default:
      return 'coffee';
  }
}

export interface CoffeeMenuScreenProps {
  cart: Cart;
  lastPaid: Cart | undefined;
  onBack: () => void;
  onAdd: (product: Product) => void;
  onRepeat: () => void;
  onOrder: () => void;
}

/** Menu: the last order to repeat in one tap, tabs, items with "+", and the order pill. */
export function CoffeeMenuScreen({ cart, lastPaid, onBack, onAdd, onRepeat, onOrder }: CoffeeMenuScreenProps) {
  const { strings, eur } = useL10n();
  const insets = useSafeAreaInsets();
  const [tab, setTab] = useState<CoffeeTab>('coffee');
  return (
    <View style={[styles.screen, { paddingTop: insets.top }]}>
      <ScrollView contentContainerStyle={{ paddingBottom: 96 + insets.bottom }}>
        <View style={styles.header}>
          <BackButton color={style.text} onPress={onBack} />
          <Text style={[styles.grow, heading(style, 24, { weight: 800 })]} accessibilityRole="header">
            {strings.coverCoffeeName}
          </Text>
          <Icon name="place_outlined" color={style.muted} size={16} />
          <Text style={body(style, 13, { color: style.muted })}>{strings.coffeeLocation}</Text>
        </View>
        {lastPaid && !lastPaid.isEmpty && <Again last={lastPaid} onRepeat={onRepeat} />}
        <View style={styles.tabs} accessibilityRole="tablist">
          {tabs.map(([id, label]) => (
            <Tap
              key={id}
              onPress={() => setTab(id)}
              accessibilityRole="tab"
              accessibilityState={{ selected: id === tab }}
              rippleColor={RIPPLE}
              style={styles.tab}
            >
              <Text style={body(style, 15, { weight: 700, color: id === tab ? style.text : style.muted })}>
                {label(strings)}
              </Text>
              <View style={[styles.tabMark, { backgroundColor: id === tab ? style.primary : style.background }]} />
            </Tap>
          ))}
        </View>
        {products('coffee')
          .filter(product => tabOf(product.id) === tab)
          .map(product => (
            <MenuRow key={product.id} product={product} onAdd={() => onAdd(product)} />
          ))}
      </ScrollView>
      {!cart.isEmpty && (
        <Tap testID="coffee_order_pill" onPress={onOrder} style={[styles.pill, { bottom: 16 + insets.bottom }]}>
          <Text style={[styles.grow, body(style, 15, { weight: 800, color: style.onPrimary })]}>
            {strings.storeCartCount(cart.count)}
          </Text>
          <Text style={body(style, 15, { weight: 800, color: style.onPrimary })}>{eur(cart.totalMinor)}</Text>
        </Tap>
      )}
    </View>
  );
}

function Again({ last, onRepeat }: { last: Cart; onRepeat: () => void }) {
  const { strings, eur } = useL10n();
  return (
    <View style={styles.again}>
      <Image source={photos.coffeeFlatWhite} resizeMode="cover" style={styles.againPhoto} />
      <View style={styles.againText}>
        <Text style={body(style, 12, { color: style.muted })}>{strings.coffeeAgain}</Text>
        <Text numberOfLines={1} style={body(style, 15, { weight: 700, color: style.text })}>
          {last.lines.map(line => line.product.name(strings)).join(', ')}
        </Text>
        <Text style={body(style, 13, { color: style.muted })}>{eur(last.totalMinor)}</Text>
        <Tap testID="coffee_repeat" onPress={onRepeat} style={styles.repeat}>
          <Text style={body(style, 14, { weight: 800, color: style.onPrimary })}>{strings.coffeeRepeat}</Text>
        </Tap>
      </View>
    </View>
  );
}

function MenuRow({ product, onAdd }: { product: Product; onAdd: () => void }) {
  const { strings, eur } = useL10n();
  const name = product.name(strings);
  return (
    <View style={styles.menuRow}>
      {product.image && <Image source={product.image} resizeMode="cover" style={styles.menuPhoto} />}
      <View style={styles.menuText}>
        <Text style={body(style, 15, { weight: 700, color: style.text })}>{name}</Text>
        {product.subtitle && (
          <Text numberOfLines={1} style={body(style, 13, { color: style.muted })}>
            {product.subtitle(strings)}
          </Text>
        )}
        <Text style={body(style, 13, { color: style.muted })}>{eur(product.priceMinor)}</Text>
      </View>
      <Tap
        testID={`coffee_add_${product.id}`}
        onPress={onAdd}
        accessibilityLabel={strings.coffeeAddItem(name)}
        style={styles.addTarget}
      >
        <View style={styles.add}>
          <Icon name="add" color={style.onPrimary} size={18} />
        </View>
      </Tap>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: style.background },
  grow: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingStart: 4, paddingEnd: 16, paddingTop: 4 },
  again: {
    flexDirection: 'row',
    height: 132,
    marginHorizontal: 16,
    marginVertical: 8,
    borderRadius: style.radius,
    backgroundColor: style.surface,
    overflow: 'hidden',
  },
  againPhoto: { width: 104, height: '100%' },
  againText: { flex: 1, padding: 12 },
  repeat: {
    alignSelf: 'flex-start',
    marginTop: 8,
    paddingHorizontal: 18,
    paddingVertical: 10,
    borderRadius: 12,
    backgroundColor: style.primary,
    overflow: 'hidden',
  },
  tabs: { flexDirection: 'row', gap: 20, paddingHorizontal: 16, paddingVertical: 8 },
  tab: { minHeight: 44, justifyContent: 'center' },
  tabMark: { marginTop: 4, width: 28, height: 2 },
  menuRow: { flexDirection: 'row', alignItems: 'center', paddingHorizontal: 16, paddingVertical: 8 },
  menuPhoto: { width: 64, height: 64, borderRadius: 16 },
  menuText: { flex: 1, paddingHorizontal: 12 },
  addTarget: {
    width: 48,
    height: 48,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 24,
    overflow: 'hidden',
  },
  add: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: style.primary,
  },
  pill: {
    position: 'absolute',
    start: 16,
    end: 16,
    minHeight: 56,
    paddingHorizontal: 22,
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 999,
    backgroundColor: style.primary,
    overflow: 'hidden',
  },
});
