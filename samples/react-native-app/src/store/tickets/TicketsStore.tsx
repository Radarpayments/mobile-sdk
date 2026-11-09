import React, { useState } from 'react';
import { StyleSheet, View } from 'react-native';

import { useStore } from '../../AppScope';
import { useBackHandler, useNavigation } from '../../navigation';
import { FadeIn } from '../../ui/FadeIn';
import type { StoreProps } from '../CoverScreen';
import { coverStyle } from '../theme/coverStyle';
import { TicketOrder } from './ticketOrder';
import { TicketsCheckoutScreen } from './TicketsCheckoutScreen';
import { TicketsEventScreen } from './TicketsEventScreen';

/** Stage Pass: the event with seat categories → checkout with a seat hold → the shared payment bar. */
export function TicketsStore({ onOutcome, onOpenSettings }: StoreProps) {
  const navigation = useNavigation();
  const store = useStore();
  const [order, setOrder] = useState(() => new TicketOrder());
  const [checkoutOpen, setCheckoutOpen] = useState(false);
  const [holdExpired, setHoldExpired] = useState(false);
  // After a paid order the cart is empty: the buyer is back at the event.
  const showCheckout = checkoutOpen && !store.cart('tickets').isEmpty;
  useBackHandler(showCheckout, () => setCheckoutOpen(false));

  return (
    <View style={[styles.store, { backgroundColor: coverStyle('tickets').background }]}>
      {showCheckout ? (
        <FadeIn key="checkout">
          <TicketsCheckoutScreen
            order={order}
            onBack={() => setCheckoutOpen(false)}
            onHoldExpired={() => {
              setHoldExpired(true);
              setCheckoutOpen(false);
            }}
            onOutcome={onOutcome}
            onOpenSettings={onOpenSettings}
          />
        </FadeIn>
      ) : (
        <FadeIn key="event">
          <TicketsEventScreen
            order={order}
            holdExpired={holdExpired}
            onBack={() => navigation.goBack()}
            onCategory={id => setOrder(order.withCategory(id))}
            onQuantity={quantity => setOrder(order.withQuantity(quantity))}
            onNext={() => {
              setHoldExpired(false);
              setCheckoutOpen(true);
              store.replaceCart('tickets', order.toCart());
            }}
          />
        </FadeIn>
      )}
    </View>
  );
}

const styles = StyleSheet.create({ store: { flex: 1 } });
