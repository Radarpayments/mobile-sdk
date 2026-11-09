import React, { useEffect, useRef, useState } from 'react';
import { Image, ScrollView, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useL10n } from '../../AppScope';
import type { OnOutcome } from '../../navigation';
import { BackButton } from '../../ui/BackButton';
import { Icon } from '../../ui/Icon';
import { SystemBars } from '../../ui/SystemBars';
import type { CartLine } from '../model/cart';
import { photos } from '../model/catalog';
import { StoreCheckoutBar } from '../StoreCheckoutBar';
import { body, coverStyle, heading } from '../theme/coverStyle';
import type { TicketOrder } from './ticketOrder';
import { HOLD_COLOUR } from './TicketsEventScreen';

const style = coverStyle('tickets');
const HOLD_SECONDS = 600;

export interface TicketsCheckoutScreenProps {
  order: TicketOrder;
  onBack: () => void;
  onHoldExpired: () => void;
  onOutcome: OnOutcome;
  onOpenSettings: () => void;
}

/** Checkout: the event, the bill, a seat hold that runs out, and the shared payment bar. */
export function TicketsCheckoutScreen({
  order,
  onBack,
  onHoldExpired,
  onOutcome,
  onOpenSettings,
}: TicketsCheckoutScreenProps) {
  const { strings, eur } = useL10n();
  const secondsLeft = useHold(onHoldExpired);
  const cart = order.toCart();
  const [seats, fee] = cart.lines;
  const time = `${Math.floor(secondsLeft / 60)}:${`${secondsLeft % 60}`.padStart(2, '0')}`;
  return (
    <SafeAreaView style={styles.screen}>
      <SystemBars lightBackground />
      <View style={styles.header}>
        <BackButton color={style.text} onPress={onBack} />
        <Text style={[styles.grow, heading(style, 22, { weight: 800 })]} accessibilityRole="header">
          {strings.ticketsCheckout}
        </Text>
      </View>
      <ScrollView style={styles.grow}>
        <EventCard seats={seats} />
        <View style={styles.bill}>
          <Line label={strings.ticketsSeats} value={eur(seats.product.priceMinor * seats.quantity)} />
          <Line label={fee.product.name(strings)} value={eur(fee.product.priceMinor)} />
          <Line label={strings.storeTotal} value={eur(cart.totalMinor)} bold />
        </View>
        <View style={styles.hold}>
          <Icon name="schedule" color={HOLD_COLOUR} size={18} />
          <Text testID="tickets_hold" style={body(style, 14, { weight: 600, color: HOLD_COLOUR })}>
            {strings.ticketsHold(time)}
          </Text>
        </View>
      </ScrollView>
      <View style={styles.checkout}>
        <StoreCheckoutBar
          cover="tickets"
          amountMinor={cart.totalMinor}
          onOutcome={onOutcome}
          onOpenSettings={onOpenSettings}
        />
      </View>
    </SafeAreaView>
  );
}

/** Seconds left of the seat hold, counting down from the moment the checkout opens; calls `onExpired` at zero. */
function useHold(onExpired: () => void): number {
  const [secondsLeft, setSecondsLeft] = useState(HOLD_SECONDS);
  const expired = useRef(onExpired);
  useEffect(() => {
    expired.current = onExpired;
  });
  useEffect(() => {
    const startedAt = Date.now();
    const timer = setInterval(() => {
      const left = Math.max(HOLD_SECONDS - Math.floor((Date.now() - startedAt) / 1000), 0);
      setSecondsLeft(left);
      if (left === 0) {
        clearInterval(timer);
        expired.current();
      }
    }, 1000);
    return () => clearInterval(timer);
  }, []);
  return secondsLeft;
}

function Line({ label, value, bold = false }: { label: string; value: string; bold?: boolean }) {
  const text = body(style, bold ? 17 : 14, { weight: bold ? 800 : 400, color: bold ? style.text : style.muted });
  return (
    <View style={styles.line}>
      <Text style={text}>{label}</Text>
      <Text style={text}>{value}</Text>
    </View>
  );
}

function EventCard({ seats }: { seats: CartLine }) {
  const { strings, eur } = useL10n();
  return (
    <View style={styles.event}>
      <Image source={photos.ticketsCrowd} resizeMode="cover" style={styles.eventPhoto} />
      <View style={styles.eventText}>
        <Text style={heading(style, 15, { weight: 800 })}>{strings.ticketsTitle.replaceAll('\n', ' ')}</Text>
        <Text style={body(style, 13, { color: style.muted })}>
          {strings.ticketsEventLine(seats.product.name(strings))}
        </Text>
        <Text style={body(style, 13, { color: style.muted })}>{`${seats.quantity} × ${eur(
          seats.product.priceMinor,
        )}`}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, paddingHorizontal: 16, backgroundColor: style.background },
  grow: { flex: 1 },
  header: { flexDirection: 'row', alignItems: 'center', gap: 4, paddingTop: 4 },
  event: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 8,
    borderWidth: 1,
    borderColor: style.border,
    borderRadius: style.radius,
    overflow: 'hidden',
  },
  eventPhoto: { width: 96, height: 96 },
  eventText: { flex: 1, padding: 12 },
  bill: { paddingVertical: 16, gap: 6 },
  line: { flexDirection: 'row', justifyContent: 'space-between' },
  hold: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  checkout: { paddingVertical: 12 },
});
