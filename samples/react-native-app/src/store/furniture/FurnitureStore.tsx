import React, { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { useStore } from '../../AppScope';
import { useBackHandler, useNavigation } from '../../navigation';
import { FadeIn } from '../../ui/FadeIn';
import { SystemBars } from '../../ui/SystemBars';
import type { StoreProps } from '../CoverScreen';
import { coverStyle } from '../theme/coverStyle';
import { FurnitureCartScreen } from './FurnitureCartScreen';
import { FurnitureCatalogScreen } from './FurnitureCatalogScreen';
import type { FurnitureCategory } from './furnitureCategory';

const FADE_MS = 240;

/** Hearth & Co: catalogue → product → cart → the shared payment bar. */
export function FurnitureStore({ onOutcome, onOpenSettings }: StoreProps) {
  const navigation = useNavigation();
  const store = useStore();
  const [cartOpen, setCartOpen] = useState(false);
  const [category, setCategory] = useState<FurnitureCategory>('all');
  const cart = store.cart('furniture');
  // An empty cart is never shown: after a paid order the buyer is back at the catalogue.
  const showCart = cartOpen && !cart.isEmpty;
  // Back from the cart goes to the catalogue, not out of the store.
  useBackHandler(showCart, () => setCartOpen(false));

  return (
    <View style={[styles.store, { backgroundColor: coverStyle('furniture').background }]}>
      <SystemBars lightBackground />
      {/* The catalogue stays mounted under the cart, so back from the cart the grid is where it was. */}
      <View style={[styles.store, showCart && styles.hidden]}>
        <FurnitureCatalogScreen
          cart={cart}
          category={category}
          onCategory={setCategory}
          onBack={() => navigation.goBack()}
          onProduct={product => navigation.push('FurnitureProduct', { productId: product.id })}
          onCart={() => setCartOpen(true)}
        />
      </View>
      {showCart && (
        <FadeIn duration={FADE_MS}>
          <FurnitureCartScreen
            cart={cart}
            onBack={() => setCartOpen(false)}
            onQuantity={(id, quantity) => store.setQuantity('furniture', id, quantity)}
            onOutcome={onOutcome}
            onOpenSettings={onOpenSettings}
          />
        </FadeIn>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  store: { flex: 1 },
  hidden: { display: 'none' },
});
