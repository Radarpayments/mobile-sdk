import React from 'react';
import { Image, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useL10n } from '../../AppScope';
import { BackButton } from '../../ui/BackButton';
import { Icon } from '../../ui/Icon';
import { Tap } from '../../ui/Tap';
import type { Cart } from '../model/cart';
import { photos, type Product } from '../model/catalog';
import { body, coverStyle, heading } from '../theme/coverStyle';
import { categoryLabel, furnitureCategories, furnitureFilter, type FurnitureCategory } from './furnitureCategory';
import { photoFill } from '../../ui/photoFill';

const style = coverStyle('furniture');

export interface FurnitureCatalogScreenProps {
  cart: Cart;
  category: FurnitureCategory;
  onCategory: (category: FurnitureCategory) => void;
  onBack: () => void;
  onProduct: (product: Product) => void;
  onCart: () => void;
}

/** Catalogue: brand header with the cart, collection photo, category chips and a product grid. */
export function FurnitureCatalogScreen({
  cart,
  category,
  onCategory,
  onBack,
  onProduct,
  onCart,
}: FurnitureCatalogScreenProps) {
  const { strings, eur } = useL10n();
  const insets = useSafeAreaInsets();
  const items = furnitureFilter(category);
  const rows = Array.from({ length: Math.ceil(items.length / 2) }, (_, row) => items.slice(row * 2, row * 2 + 2));
  return (
    <View style={[styles.screen, { paddingTop: insets.top }]}>
      <ScrollView contentContainerStyle={[styles.page, { paddingBottom: insets.bottom + (cart.isEmpty ? 16 : 88) }]}>
        <Header count={cart.count} onBack={onBack} onCart={onCart} />
        <Hero />
        <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.chips}>
          {furnitureCategories.map(item => {
            const selected = item === category;
            return (
              <Tap
                key={item}
                onPress={() => onCategory(item)}
                accessibilityRole="tab"
                accessibilityState={{ selected }}
                style={[styles.chip, selected ? styles.chipSelected : styles.chipIdle]}
              >
                <Text
                  style={body(style, 13, {
                    weight: 600,
                    color: selected ? style.onPrimary : style.text,
                  })}
                >
                  {categoryLabel[item](strings)}
                </Text>
              </Tap>
            );
          })}
        </ScrollView>
        {rows.map(row => (
          <View key={row[0].id} style={styles.row}>
            {row.map(product => (
              <Tap
                key={product.id}
                testID={`furniture_${product.id}`}
                onPress={() => onProduct(product)}
                accessibilityLabel={`${product.name(strings)}, ${eur(product.priceMinor)}`}
                style={styles.tile}
              >
                {product.image && <Image source={product.image} resizeMode="cover" style={styles.tilePhoto} />}
                <Text style={[styles.tileName, body(style, 14, { weight: 600, color: style.text })]}>
                  {product.name(strings)}
                </Text>
                <Text style={body(style, 13, { color: style.muted })}>{eur(product.priceMinor)}</Text>
              </Tap>
            ))}
            {row.length === 1 && <View style={styles.tile} />}
          </View>
        ))}
      </ScrollView>
      {!cart.isEmpty && (
        <Tap
          testID="furniture_cart_bar"
          onPress={onCart}
          rippleColor="#FFFFFF33"
          style={[styles.cartBar, { bottom: 16 + insets.bottom }]}
        >
          <Text style={[styles.grow, body(style, 15, { weight: 600, color: style.onPrimary })]}>
            {strings.storeCartCount(cart.count)}
          </Text>
          <Text style={body(style, 15, { weight: 600, color: style.onPrimary })}>{eur(cart.totalMinor)}</Text>
        </Tap>
      )}
    </View>
  );
}

function Header({ count, onBack, onCart }: { count: number; onBack: () => void; onCart: () => void }) {
  const { strings } = useL10n();
  return (
    <View style={styles.header}>
      <View style={styles.backOffset}>
        <BackButton color={style.text} onPress={onBack} />
      </View>
      <Text style={[styles.grow, heading(style, 26)]} accessibilityRole="header">
        {strings.coverFurnitureName}
      </Text>
      <Tap onPress={onCart} accessibilityLabel={strings.storeCartCount(count)} style={styles.basket}>
        <View style={styles.basketCircle}>
          <Icon name="shopping_basket_outlined" color={style.text} size={20} />
        </View>
        {count > 0 && (
          <View style={styles.badge}>
            <Text style={body(style, 11, { weight: 700, color: '#FFFFFF' })}>{count}</Text>
          </View>
        )}
      </Tap>
    </View>
  );
}

function Hero() {
  const { strings } = useL10n();
  return (
    <View style={styles.hero}>
      <Image source={photos.furnitureHero} resizeMode="cover" style={photoFill} />
      <View style={[StyleSheet.absoluteFill, styles.heroFade]} />
      <Text style={[styles.heroText, heading(style, 26, { weight: 600, color: '#FFFFFF', lineHeight: 26 })]}>
        {strings.furnitureCollection}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: style.background },
  page: { paddingHorizontal: 16 },
  grow: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', paddingTop: 4 },
  // The arrow lines up with the content edge, not its touch area.
  backOffset: { marginStart: -10 },
  basket: { width: 48, height: 48, alignItems: 'center', justifyContent: 'center' },
  basketCircle: {
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: style.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badge: {
    position: 'absolute',
    top: 0,
    end: 0,
    paddingHorizontal: 6,
    borderRadius: 999,
    backgroundColor: style.accent,
  },
  hero: { marginTop: 16, height: 184, borderRadius: 16, overflow: 'hidden' },
  heroFade: { backgroundImage: 'linear-gradient(to bottom, transparent 45%, rgba(0, 0, 0, 0.5) 100%)' },
  heroText: { position: 'absolute', start: 14, bottom: 14 },
  chips: { gap: 8, paddingTop: 16 },
  chip: {
    minHeight: 40,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 20,
    borderWidth: 1,
    justifyContent: 'center',
  },
  chipSelected: { backgroundColor: style.primary, borderColor: style.primary },
  chipIdle: { borderColor: style.border },
  row: { flexDirection: 'row', alignItems: 'flex-start', gap: 12, paddingTop: 16 },
  tile: { flex: 1, borderRadius: style.radius },
  // height: 'auto' replaces the file's pixel height React Native gives a bundled image, so the aspect ratio applies.
  tilePhoto: { width: '100%', height: 'auto', aspectRatio: 1, borderRadius: style.radius },
  tileName: { marginTop: 8 },
  cartBar: {
    position: 'absolute',
    start: 16,
    end: 16,
    minHeight: 56,
    paddingHorizontal: 18,
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: style.radius,
    backgroundColor: style.primary,
    overflow: 'hidden',
  },
});
