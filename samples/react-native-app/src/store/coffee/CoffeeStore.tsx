import React, { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { useStore } from '../../AppScope';
import { useBackHandler, useNavigation } from '../../navigation';
import { FadeIn } from '../../ui/FadeIn';
import { SystemBars } from '../../ui/SystemBars';
import type { StoreProps } from '../CoverScreen';
import { coverStyle } from '../theme/coverStyle';
import { CoffeeMenuScreen } from './CoffeeMenuScreen';
import { CoffeeOrderScreen } from './CoffeeOrderScreen';

/** Roast Lane: menu → order → the shared payment bar; "Order again" pays the last order right away. */
export function CoffeeStore({ onOutcome, onOpenSettings }: StoreProps) {
  const navigation = useNavigation();
  const store = useStore();
  const [orderOpen, setOrderOpen] = useState(false);
  const [payNow, setPayNow] = useState(false);
  const cart = store.cart('coffee');
  // An empty order is never shown: after a paid order the buyer is back at the menu, with "Order again".
  const showOrder = orderOpen && !cart.isEmpty;
  useBackHandler(showOrder, () => setOrderOpen(false));

  return (
    <View style={[styles.store, { backgroundColor: coverStyle('coffee').background }]}>
      <SystemBars lightBackground={false} />
      {showOrder ? (
        <FadeIn key="order">
          <CoffeeOrderScreen
            cart={cart}
            autoPay={payNow}
            onAutoPayStarted={() => setPayNow(false)}
            onBack={() => setOrderOpen(false)}
            onQuantity={(id, quantity) => store.setQuantity('coffee', id, quantity)}
            onOutcome={onOutcome}
            onOpenSettings={onOpenSettings}
          />
        </FadeIn>
      ) : (
        <FadeIn key="menu">
          <CoffeeMenuScreen
            cart={cart}
            lastPaid={store.lastPaid('coffee')}
            onBack={() => navigation.goBack()}
            onAdd={product => store.add('coffee', product)}
            onRepeat={() => {
              setPayNow(true);
              setOrderOpen(true);
              store.repeatLast('coffee');
            }}
            onOrder={() => setOrderOpen(true)}
          />
        </FadeIn>
      )}
    </View>
  );
}

const styles = StyleSheet.create({ store: { flex: 1 } });
