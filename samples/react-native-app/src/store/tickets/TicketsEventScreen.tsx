import React from 'react';
import { Image, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { useL10n } from '../../AppScope';
import { BackButton } from '../../ui/BackButton';
import { Icon } from '../../ui/Icon';
import { SystemBars } from '../../ui/SystemBars';
import { Tap } from '../../ui/Tap';
import { textEnd } from '../../ui/textEnd';
import { photos, products, ticketFeeId, type Product } from '../model/catalog';
import { body, coverStyle, font, heading } from '../theme/coverStyle';
import type { TicketOrder } from './ticketOrder';
import { photoFill } from '../../ui/photoFill';

const style = coverStyle('tickets');

/** Orange-700: the hold warning, 4.5:1 on white. */
export const HOLD_COLOUR = '#C2410C';

export interface TicketsEventScreenProps {
  order: TicketOrder;
  holdExpired: boolean;
  onBack: () => void;
  onCategory: (id: string) => void;
  onQuantity: (quantity: number) => void;
  onNext: () => void;
}

/** The concert: a full-bleed poster, seat categories, how many seats and "Checkout". */
export function TicketsEventScreen({
  order,
  holdExpired,
  onBack,
  onCategory,
  onQuantity,
  onNext,
}: TicketsEventScreenProps) {
  const { strings, eur, rtl } = useL10n();
  const insets = useSafeAreaInsets();
  const seats = order.toCart().lines[0];
  return (
    <View style={styles.screen}>
      {/* The poster runs under the status bar: light icons on the dark photo. */}
      <SystemBars lightBackground={false} />
      <ScrollView style={styles.grow}>
        <Poster onBack={onBack} />
        {holdExpired && (
          <Text style={[styles.expired, body(style, 14, { weight: 600, color: HOLD_COLOUR })]}>
            {strings.ticketsHoldExpired}
          </Text>
        )}
        <Text style={[styles.section, heading(style, 18, { weight: 800 })]} accessibilityRole="header">
          {strings.ticketsSection}
        </Text>
        {products('tickets')
          .filter(category => category.id !== ticketFeeId)
          .map(category => (
            <CategoryRow
              key={category.id}
              category={category}
              selected={category.id === order.categoryId}
              onPress={() => onCategory(category.id)}
            />
          ))}
      </ScrollView>
      <View style={[styles.bar, { marginBottom: 16 + insets.bottom }]}>
        <Tap
          onPress={() => onQuantity(order.quantity - 1)}
          accessibilityLabel={strings.storeDecrease}
          rippleColor="#0000002E"
          style={styles.step}
        >
          <Icon name="remove" color={style.onPrimary} />
        </Tap>
        <Text style={body(style, 16, { weight: 800, color: style.onPrimary })}>{order.quantity}</Text>
        <Tap
          onPress={() => onQuantity(order.quantity + 1)}
          accessibilityLabel={strings.storeIncrease}
          rippleColor="#0000002E"
          style={styles.step}
        >
          <Icon name="add" color={style.onPrimary} />
        </Tap>
        <Tap testID="tickets_next" onPress={onNext} rippleColor="#0000002E" style={styles.next}>
          <Text style={[textEnd(rtl), body(style, 16, { weight: 800, color: style.onPrimary })]}>
            {strings.ticketsToPayment(eur(seats.product.priceMinor * seats.quantity))}
          </Text>
        </Tap>
      </View>
    </View>
  );
}

function CategoryRow({ category, selected, onPress }: { category: Product; selected: boolean; onPress: () => void }) {
  const { strings, eur } = useL10n();
  return (
    <Tap
      onPress={onPress}
      accessibilityRole="radio"
      accessibilityState={{ checked: selected }}
      style={[styles.category, selected ? styles.categorySelected : styles.categoryIdle]}
    >
      <View style={styles.grow}>
        <Text style={body(style, 16, { weight: 600, color: style.text })}>{category.name(strings)}</Text>
        {category.subtitle && <Text style={body(style, 13, { color: style.muted })}>{category.subtitle(strings)}</Text>}
      </View>
      <Text style={body(style, 16, { weight: 700, color: style.text })}>{eur(category.priceMinor)}</Text>
    </Tap>
  );
}

function Poster({ onBack }: { onBack: () => void }) {
  const { strings } = useL10n();
  const insets = useSafeAreaInsets();
  return (
    <View style={styles.poster}>
      <Image source={photos.ticketsHero} resizeMode="cover" style={photoFill} />
      <View style={[StyleSheet.absoluteFill, styles.posterFade]} />
      <View style={[styles.posterBack, { top: insets.top + 12 }]}>
        <BackButton icon="chevron_left" color="#FFFFFF" background="rgba(0, 0, 0, 0.35)" onPress={onBack} />
      </View>
      <View style={styles.posterText}>
        <Text
          style={font(style.accentFont ?? style.headingFont, 18, {
            weight: 600,
            italic: true,
            color: '#FFFFFF',
          })}
        >
          {strings.ticketsLive}
        </Text>
        <Text style={heading(style, 40, { weight: 800, color: '#FFFFFF', lineHeight: 40 })} accessibilityRole="header">
          {strings.ticketsTitle}
        </Text>
        <Text style={[styles.date, body(style, 14, { color: 'rgba(255, 255, 255, 0.9)' })]}>{strings.ticketsDate}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: style.background },
  grow: { flex: 1 },
  poster: { height: 340 },
  posterFade: { backgroundImage: 'linear-gradient(to bottom, rgba(0, 0, 0, 0.25), rgba(0, 0, 0, 0.8))' },
  posterBack: { position: 'absolute', start: 12 },
  posterText: { position: 'absolute', start: 16, end: 16, bottom: 16 },
  date: { marginTop: 6 },
  expired: { paddingHorizontal: 16, paddingTop: 12 },
  section: { paddingStart: 16, paddingTop: 16, paddingBottom: 8 },
  category: {
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: 16,
    marginVertical: 5,
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderRadius: style.radius,
    overflow: 'hidden',
  },
  categorySelected: { backgroundColor: style.surface, borderColor: style.primary, borderWidth: 2 },
  categoryIdle: { backgroundColor: style.background, borderColor: style.border, borderWidth: 1 },
  bar: {
    flexDirection: 'row',
    alignItems: 'center',
    marginHorizontal: 16,
    marginTop: 16,
    minHeight: 56,
    paddingStart: 6,
    borderRadius: style.radius,
    backgroundColor: style.primary,
    overflow: 'hidden',
  },
  step: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  next: { flex: 1, alignSelf: 'stretch', justifyContent: 'center', paddingHorizontal: 12, paddingVertical: 18 },
});
