import React, { type ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { useL10n } from '../AppScope';
import type { ScreenProps } from '../navigation';
import { Icon, type IconName } from '../ui/Icon';
import { SystemBars } from '../ui/SystemBars';
import { Tap } from '../ui/Tap';
import { textEnd } from '../ui/textEnd';
import { CoffeeOrderNumber } from './coffee/CoffeeOrderNumber';
import type { CoverId, LocalText } from './model/catalog';
import { PayButton } from './StoreCheckoutBar';
import { body, buttonRadius, coverStyle, heading, mono, type CoverStyle } from './theme/coverStyle';

const successTitle: Record<CoverId, LocalText> = {
  furniture: s => s.resultSuccessFurniture,
  coffee: s => s.resultSuccessCoffee,
  tickets: s => s.resultSuccessTickets,
  invest: s => s.investOpTopUp,
};

const successBody: Record<CoverId, LocalText> = {
  furniture: s => s.resultSuccessFurnitureBody,
  coffee: s => s.resultSuccessCoffeeBody,
  tickets: s => s.resultSuccessTicketsBody,
  invest: s => s.doneOperationValue,
};

/**
 * The store's own screen after the sheet closes: the merchant app, not the SDK, decides what comes next. The coffee
 * shop shows its order number instead of the default title.
 */
export function StoreResultScreen({ navigation, route }: ScreenProps<'StoreResult'>) {
  const { cover, receipt, orderNumber } = route.params;
  const style = coverStyle(cover);
  const { strings, eur } = useL10n();
  const { outcome } = receipt;
  const back = () => navigation.goBack();

  let content: ReactNode;
  switch (outcome.kind) {
    case 'success':
      content = (
        <>
          {cover === 'coffee' ? (
            <CoffeeOrderNumber number={orderNumber} />
          ) : (
            <>
              <ResultMark icon="check" tint={style.accent} />
              <Title text={successTitle[cover](strings)} style={style} />
            </>
          )}
          <Body text={successBody[cover](strings)} style={style} />
          <View style={[styles.receipt, { borderColor: style.border, borderRadius: style.radius }]}>
            <InfoRow label={strings.resultAmount} value={eur(receipt.amountMinor)} style={style} />
            <InfoRow label={strings.resultPaymentId} value={outcome.paymentId} style={style} monospace />
          </View>
        </>
      );
      break;
    case 'unconfirmed':
      content = (
        <>
          <ResultMark icon="schedule" tint={style.muted} />
          <Title text={strings.resultUnconfirmedTitle} style={style} />
          <Body text={strings.resultUnconfirmedBody} style={style} />
        </>
      );
      break;
    case 'declined':
    case 'cancelled':
      content = (
        <>
          <ResultMark icon="close" tint="#DC2626" />
          <Title text={strings.resultDeclinedTitle} style={style} />
          <Body text={strings.resultDeclinedBody} style={style} />
          {/* The gateway's own words, as they came: useful when showing the demo, secondary for a buyer. */}
          {outcome.kind === 'declined' && outcome.message !== undefined && (
            <Text style={[styles.gatewayMessage, body(style, 12, { lineHeight: 16, color: style.muted })]}>
              {outcome.message}
            </Text>
          )}
        </>
      );
      break;
  }

  return (
    <SafeAreaView style={[styles.screen, { backgroundColor: style.background }]}>
      <SystemBars lightBackground={!style.dark} />
      <View style={styles.top} />
      {content}
      <View style={styles.bottom} />
      {/* Paying again while the first payment may have gone through could charge the buyer twice. */}
      {outcome.kind === 'declined' && (
        <PayButton cover={cover} label={strings.resultTryAgain} busy={false} enabled onPress={back} />
      )}
      <Tap
        testID="result_done"
        onPress={back}
        rippleColor={style.dark ? '#FFFFFF1F' : undefined}
        style={[styles.done, { borderColor: style.border, borderRadius: buttonRadius(style) }]}
      >
        <Text style={body(style, 15, { weight: 700, color: style.text })}>
          {outcome.kind === 'success' ? strings.resultDone : strings.resultBackToStore}
        </Text>
      </Tap>
    </SafeAreaView>
  );
}

function ResultMark({ icon, tint }: { icon: IconName; tint: string }) {
  return (
    <View style={[styles.mark, { borderColor: tint }]}>
      <Icon name={icon} color={tint} size={32} />
    </View>
  );
}

function Title({ text, style }: { text: string; style: CoverStyle }) {
  return (
    <Text accessibilityRole="header" style={[styles.title, heading(style, 28, { lineHeight: 32 })]}>
      {text}
    </Text>
  );
}

function Body({ text, style }: { text: string; style: CoverStyle }) {
  return <Text style={[styles.body, body(style, 15, { lineHeight: 21, color: style.muted })]}>{text}</Text>;
}

function InfoRow({
  label,
  value,
  style,
  monospace = false,
}: {
  label: string;
  value: string;
  style: CoverStyle;
  monospace?: boolean;
}) {
  const { rtl } = useL10n();
  return (
    <View style={styles.infoRow}>
      <Text style={body(style, 13, { color: style.muted })}>{label}</Text>
      <Text
        selectable
        style={[
          styles.infoValue,
          textEnd(rtl),
          monospace ? mono(13, { color: style.text, bold: true }) : body(style, 13, { weight: 500, color: style.text }),
        ]}
      >
        {value}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, padding: 20, gap: 0 },
  top: { flex: 6 },
  bottom: { flex: 10 },
  mark: {
    alignSelf: 'center',
    width: 72,
    height: 72,
    borderRadius: 36,
    borderWidth: 1.5,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: { paddingTop: 18, textAlign: 'center' },
  body: { paddingTop: 8, textAlign: 'center' },
  gatewayMessage: { paddingTop: 12, textAlign: 'center' },
  receipt: { marginTop: 24, padding: 14, borderWidth: 1, gap: 6 },
  infoRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  infoValue: { flex: 1 },
  done: {
    marginTop: 10,
    minHeight: 52,
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
});
