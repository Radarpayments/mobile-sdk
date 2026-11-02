import React, { useEffect, useRef, useState, type ReactNode } from 'react';
import {
  AccessibilityInfo,
  ActivityIndicator,
  Animated,
  BackHandler,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  type LayoutChangeEvent,
  type TextStyle,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { gatewayTransferRegistrar, useAppScope, useL10n, usePrefs } from '../../AppScope';
import { buildSdkConfig, StoreCheckout } from '../../checkout/checkout';
import { useListenable } from '../../listenable';
import { BackButton } from '../../ui/BackButton';
import { Icon, type IconName } from '../../ui/Icon';
import { KeyboardAware } from '../../ui/KeyboardAware';
import { SystemBars } from '../../ui/SystemBars';
import { Tap } from '../../ui/Tap';
import { ScenarioSheet } from '../ScenarioSheet';
import { RegistrationError, ScenarioRow } from '../StoreCheckoutBar';
import { coverStyle, font } from '../theme/coverStyle';
import { GLOBAL_ETF, InvestAccount, type Holding, type InvestOperation } from './investAccount';
import { InvestColors as C } from './investColors';
import { InvestModel } from './investModel';
import { northwind, northwindPrefill } from './northwindProfile';
import {
  amountMinor,
  amountValid,
  canContinue,
  MAX_TOP_UP,
  MIN_TOP_UP,
  PRESETS,
  type TopUpStatus,
} from './topUpAmount';

type Weight = 400 | 500 | 600 | 700;

/** Northwind's text: IBM Plex Sans; amounts with tabular figures so they do not jump while counting. */
function plex(size: number, options: { weight?: Weight; color?: string; tabular?: boolean } = {}): TextStyle {
  const { weight = 400, color = C.text, tabular = false } = options;
  return { ...font('IBMPlexSans', size, { weight, color }), ...(tabular ? { fontVariant: ['tabular-nums'] } : {}) };
}

type Screen = 'portfolio' | 'topUp' | 'done';

/**
 * Northwind Invest, the AFT demo: portfolio → top-up amount → SDK sheet ("Transfer") → done. The transfer is
 * registered with `registerP2P.do` and the investor's details; the account is credited only on a completed transfer
 * and kept on the device.
 */
export function InvestStore({ onBack, onOpenSettings }: { onBack: () => void; onOpenSettings: () => void }) {
  const scope = useAppScope();
  const prefs = usePrefs();
  const { strings } = useL10n();
  const [model] = useState(
    () =>
      new InvestModel(
        () => scope.prefs.investAccount,
        a => scope.prefs.setInvestAccount(a),
      ),
  );
  const [checkout] = useState(() => new StoreCheckout(scope.prefs.setLastMdOrder, scope.presentSheet));
  useListenable(model);
  const { state, busy } = useListenable(checkout);
  const [screen, setScreen] = useState<Screen>('portfolio');
  const [now, setNow] = useState(Date.now());
  const [scenarioOpen, setScenarioOpen] = useState(false);

  // A reset in the developer settings shows at once.
  useEffect(() => scope.prefs.subscribe(() => model.reload()), [scope.prefs, model]);

  const toPortfolio = () => {
    model.leaveTopUp();
    setNow(Date.now());
    setScreen('portfolio');
  };

  useEffect(() => {
    const sub = BackHandler.addEventListener('hardwareBackPress', () => {
      if (screen === 'portfolio') return false;
      toPortfolio();
      return true;
    });
    return () => sub.remove();
  });

  const pay = async () => {
    const minor = amountMinor(model.amount);
    if (minor === undefined) return;
    const scenario = prefs.scenario;
    const outcome = await checkout.pay({
      registrar: (scope.transferRegistrarFactory ?? gatewayTransferRegistrar)(prefs, northwindPrefill),
      config: buildSdkConfig({
        baseUrl: prefs.baseUrl,
        scenario,
        cover: coverStyle('invest'),
        themeOverride: prefs.sheetTheme,
        locale: prefs.localeOverride,
        storeName: strings.investName,
        googlePayMerchantId: prefs.googlePayMerchantId,
        googlePayGateway: prefs.googlePayGateway,
        applePayMerchantId: prefs.applePayMerchantId,
      }),
      amountMinor: minor,
      clientId: scenario.buyerWithCards ? prefs.clientId : undefined,
    });
    if (!outcome) return;
    model.onOutcome(outcome, minor);
    if (model.status.kind === 'done') setScreen('done');
  };

  const status = model.status;
  return (
    <SafeAreaView style={styles.screen}>
      <SystemBars lightBackground={false} />
      {screen === 'portfolio' && (
        <Portfolio
          account={model.account}
          now={now}
          onBack={onBack}
          onTopUp={() => setScreen('topUp')}
          onInvest={() => model.invest(model.account.freeCashMinor)}
        />
      )}
      {screen === 'topUp' && (
        <KeyboardAware>
          <TopUp model={model} busy={busy} onBack={toPortfolio} onContinue={pay}>
            {state.step === 'registrationFailed' && (
              <RegistrationError cover="invest" state={state} onOpenSettings={onOpenSettings} />
            )}
            <ScenarioRow
              cover="invest"
              buyerWithCards={prefs.scenario.buyerWithCards}
              onPress={() => setScenarioOpen(true)}
            />
          </TopUp>
        </KeyboardAware>
      )}
      {screen === 'done' && status.kind === 'done' && (
        <Done
          status={status}
          canInvest={InvestAccount.canInvest(model.account)}
          onInvest={() => {
            model.invest(status.amountMinor);
            toPortfolio();
          }}
          onPortfolio={toPortfolio}
        />
      )}
      <ScenarioSheet visible={scenarioOpen} onClose={() => setScenarioOpen(false)} />
    </SafeAreaView>
  );
}

/** Northwind Invest's tile on the start screen: no photo, its own colours and a rising line. */
export function InvestTile({ onPress }: { onPress: () => void }) {
  const { strings } = useL10n();
  return (
    <Tap testID="cover_invest" onPress={onPress} rippleColor="#FFFFFF1F" style={styles.tile}>
      <View style={styles.tileGlow} />
      <View style={styles.row}>
        <Icon name="trending_up" color={C.accent} size={22} />
        <Text style={[plex(15, { weight: 600 }), styles.grow, styles.gap8]}>Northwind</Text>
        <Text style={[plex(12, { weight: 600, color: C.accent }), styles.aftPill]}>AFT</Text>
      </View>
      <PortfolioChart period="all" style={styles.tileChart} />
      <Text style={plex(24, { weight: 700 })}>{strings.investName}</Text>
      <Text style={plex(13, { color: '#CBD5E1' })}>{strings.investTileSub}</Text>
    </Tap>
  );
}

// ── Chart ────────────────────────────────────────────────────────────────────────────────────────────────

/** Periods of the portfolio chart; each shows its own fixed curve (the demo has no price history). */
const periods = {
  week: [0.55, 0.5, 0.58, 0.52, 0.6, 0.57, 0.66],
  month: [0.18, 0.24, 0.21, 0.35, 0.31, 0.44, 0.41, 0.57, 0.52, 0.68, 0.64, 0.8, 0.85],
  quarter: [0.3, 0.22, 0.35, 0.28, 0.45, 0.4, 0.52, 0.6, 0.55, 0.72],
  year: [0.1, 0.2, 0.16, 0.3, 0.42, 0.38, 0.5, 0.47, 0.62, 0.7, 0.66, 0.82],
  all: [0.05, 0.12, 0.1, 0.22, 0.3, 0.28, 0.45, 0.55, 0.5, 0.7, 0.78, 0.9],
} as const;

type Period = keyof typeof periods;

/** The portfolio's line, drawn as segments, ending in a dot. Time runs left to right in every language. */
function PortfolioChart({ period, style }: { period: Period; style?: object }) {
  const [size, setSize] = useState({ width: 0, height: 0 });
  const points = periods[period];
  const pad = 6;
  const at = (i: number) => ({
    x: (i * size.width) / (points.length - 1),
    y: pad + (1 - points[i]) * (size.height - 2 * pad),
  });
  return (
    <View
      style={[style, styles.chart]}
      importantForAccessibility="no-hide-descendants"
      accessibilityElementsHidden
      onLayout={(e: LayoutChangeEvent) => setSize(e.nativeEvent.layout)}
    >
      {size.width > 0 &&
        points.slice(1).map((_, i) => {
          const a = at(i);
          const b = at(i + 1);
          const length = Math.hypot(b.x - a.x, b.y - a.y);
          const angle = Math.atan2(b.y - a.y, b.x - a.x);
          return (
            <View
              key={i}
              style={[
                styles.segment,
                {
                  width: length,
                  left: (a.x + b.x) / 2 - length / 2,
                  top: (a.y + b.y) / 2 - 1.1,
                  transform: [{ rotate: `${angle}rad` }],
                },
              ]}
            />
          );
        })}
      {size.width > 0 && (
        <View style={[styles.dot, { left: at(points.length - 1).x - 4, top: at(points.length - 1).y - 4 }]} />
      )}
    </View>
  );
}

// ── Shared pieces ────────────────────────────────────────────────────────────────────────────────────────

function Card({ children, highlight = false }: { children: ReactNode; highlight?: boolean }) {
  return <View style={[styles.card, highlight && styles.cardHighlight]}>{children}</View>;
}

function PrimaryButton({
  label,
  onPress,
  enabled = true,
  busy = false,
  icon,
  testID,
}: {
  label: string;
  onPress: () => void;
  enabled?: boolean;
  busy?: boolean;
  icon?: IconName;
  testID?: string;
}) {
  return (
    <Tap
      testID={testID}
      onPress={onPress}
      disabled={!enabled || busy}
      accessibilityState={{ disabled: !enabled, busy }}
      style={[styles.button, styles.primary, !enabled && !busy && styles.disabled]}
    >
      {busy ? <ActivityIndicator color={C.onAccent} /> : icon && <Icon name={icon} color={C.onAccent} size={18} />}
      <Text numberOfLines={1} style={plex(15, { weight: 700, color: C.onAccent })}>
        {label}
      </Text>
    </Tap>
  );
}

function OutlineButton({
  label,
  onPress,
  enabled = true,
  icon,
}: {
  label: string;
  onPress: () => void;
  enabled?: boolean;
  icon?: IconName;
}) {
  const color = enabled ? C.text : C.muted;
  return (
    <Tap
      onPress={onPress}
      disabled={!enabled}
      accessibilityState={{ disabled: !enabled }}
      rippleColor="#FFFFFF1F"
      style={[styles.button, styles.outline, !enabled && styles.outlineDisabled]}
    >
      {icon && <Icon name={icon} color={color} size={18} />}
      <Text numberOfLines={1} style={plex(15, { weight: 700, color })}>
        {label}
      </Text>
    </Tap>
  );
}

function Badge({ color, children }: { color: string; children: ReactNode }) {
  return <View style={[styles.badge, { backgroundColor: color }]}>{children}</View>;
}

function TickerBadge({ ticker }: { ticker: string }) {
  const glb = ticker === GLOBAL_ETF;
  return (
    <Badge color={glb ? C.glb : C.tch}>
      <Text style={plex(12, { weight: 700, color: glb ? C.onGlb : C.onTch })}>{ticker}</Text>
    </Badge>
  );
}

function useFormat() {
  const { strings, language, eur } = useL10n();
  return {
    strings,
    eur,
    assetName: (ticker: string) => (ticker === GLOBAL_ETF ? strings.investAssetGlb : strings.investAssetTch),
    percent: (value: number) =>
      new Intl.NumberFormat(language, { style: 'percent', minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(
        value,
      ),
    number2: (value: number) =>
      new Intl.NumberFormat(language, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value),
    eurWhole: (minor: number) =>
      new Intl.NumberFormat(language, {
        style: 'currency',
        currency: 'EUR',
        maximumFractionDigits: 0,
        minimumFractionDigits: 0,
      }).format(minor / 100),
    time: (at: number) =>
      new Intl.DateTimeFormat(language, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(at)),
    separator: new Intl.NumberFormat(language).formatToParts(1.5).find(p => p.type === 'decimal')?.value ?? '.',
  };
}

/** `minor` counted up (or down) from the value shown before, so a credit is seen arriving. */
function CountedEur({ minor, style }: { minor: number; style: TextStyle }) {
  const { eur } = useL10n();
  const [shown, setShown] = useState(minor);
  const value = useRef(new Animated.Value(minor)).current;
  useEffect(() => {
    let cancelled = false;
    AccessibilityInfo.isReduceMotionEnabled().then(reduced => {
      if (cancelled) return;
      if (reduced) {
        value.setValue(minor);
        setShown(minor);
        return;
      }
      const id = value.addListener(({ value: v }) => setShown(Math.round(v)));
      Animated.timing(value, { toValue: minor, duration: 600, useNativeDriver: false }).start(() =>
        value.removeListener(id),
      );
    });
    return () => {
      cancelled = true;
    };
  }, [minor, value]);
  return <Text style={style}>{eur(shown)}</Text>;
}

function ListRow({ divider, children }: { divider: boolean; children: ReactNode }) {
  return <View style={[styles.listRow, divider && styles.divider]}>{children}</View>;
}

// ── Portfolio ────────────────────────────────────────────────────────────────────────────────────────────

/**
 * Northwind's home: the portfolio, its free cash and the latest operations. After a top-up the amounts count up to
 * the new values and the free-cash card is outlined while the credit is fresh.
 */
function Portfolio({
  account,
  now,
  onBack,
  onTopUp,
  onInvest,
}: {
  account: InvestAccount;
  now: number;
  onBack: () => void;
  onTopUp: () => void;
  onInvest: () => void;
}) {
  const { strings, eur, assetName, percent, number2, time } = useFormat();
  const [period, setPeriod] = useState<Period>('month');
  const first = account.operations[0];
  const justCredited = first?.kind === 'TOP_UP' && now - first.atEpochMillis < 60_000 ? first : undefined;
  const periodLabels: Record<Period, string> = {
    week: strings.investPeriod1w,
    month: strings.investPeriod1m,
    quarter: strings.investPeriod3m,
    year: strings.investPeriod1y,
    all: strings.investPeriodAll,
  };
  return (
    <ScrollView contentContainerStyle={styles.page}>
      <View style={[styles.row, styles.top8]}>
        <BackButton color={C.text} icon="chevron_left" background={C.surface} onPress={onBack} />
        <View style={styles.avatar}>
          <Text style={plex(14, { weight: 700 })}>{northwind.initials}</Text>
        </View>
        <View style={[styles.grow, styles.gap10]}>
          <Text style={plex(12, { color: C.muted })}>{strings.investAccount(northwind.accountLast4)}</Text>
          <Text style={plex(15, { weight: 600 })}>{northwind.name}</Text>
        </View>
        {/* Decorative: the demo has no notifications, so it is not a button. */}
        <Icon name="notifications_none" color={C.muted} />
      </View>
      <Text style={[plex(13, { color: C.muted }), styles.top18]}>{strings.investPortfolioValue}</Text>
      <CountedEur minor={InvestAccount.total(account)} style={plex(36, { weight: 700, tabular: true })} />
      <View style={[styles.row, styles.top6]}>
        <Text style={[plex(13, { weight: 600, color: C.accent, tabular: true }), styles.pill]}>
          ▲ {eur(14920)} · {percent(0.032)}
        </Text>
        <Text style={[plex(13, { color: C.muted }), styles.gap8]}>{strings.investChangeMonth}</Text>
      </View>
      <PortfolioChart period={period} style={styles.portfolioChart} />
      <View style={styles.periods} accessibilityRole="tablist">
        {(Object.keys(periods) as Period[]).map(p => (
          <Tap
            key={p}
            accessibilityRole="tab"
            accessibilityState={{ selected: p === period }}
            rippleColor="#FFFFFF1F"
            onPress={() => setPeriod(p)}
            style={[styles.period, p === period && styles.periodOn]}
          >
            <Text style={plex(13, { weight: p === period ? 600 : 500, color: p === period ? C.text : C.muted })}>
              {periodLabels[p]}
            </Text>
          </Tap>
        ))}
      </View>
      <View style={[styles.row, styles.top16, styles.buttons]}>
        <View style={styles.grow}>
          <PrimaryButton testID="invest_top_up" label={strings.investTopUp} icon="add" onPress={onTopUp} />
        </View>
        <View style={styles.grow}>
          <OutlineButton
            label={strings.investInvest}
            icon="trending_up"
            enabled={InvestAccount.canInvest(account)}
            onPress={onInvest}
          />
        </View>
      </View>
      <View style={styles.top12}>
        <Card highlight={justCredited !== undefined}>
          <View style={styles.row}>
            <Badge color={C.accentSoft}>
              <Icon name="account_balance_wallet_outlined" color={C.accent} size={20} />
            </Badge>
            <View style={[styles.grow, styles.gap12]}>
              <Text style={plex(13, { color: C.muted })}>{strings.investFreeCash}</Text>
              {justCredited ? (
                <Text style={plex(12, { color: C.accent, tabular: true })}>
                  {strings.investFreeCashJustNow(eur(justCredited.amountMinor))}
                </Text>
              ) : (
                <Text style={plex(12, { color: C.muted })}>{strings.investFreeCashSub}</Text>
              )}
            </View>
            <CountedEur minor={account.freeCashMinor} style={plex(18, { weight: 700, tabular: true })} />
          </View>
        </Card>
      </View>
      <SectionTitle title={strings.investMyAssets} trailing={`${account.holdings.length}`} />
      <Card>
        {account.holdings.map((h: Holding, i) => (
          <ListRow key={h.ticker} divider={i > 0}>
            <TickerBadge ticker={h.ticker} />
            <View style={[styles.grow, styles.gap12]}>
              <Text style={plex(15, { weight: 600 })}>{assetName(h.ticker)}</Text>
              <Text style={plex(13, { color: C.muted, tabular: true })}>
                {strings.investUnits(number2(InvestAccount.quantity(h)), eur(h.unitPriceMinor))}
              </Text>
            </View>
            <View style={styles.end}>
              <Text style={plex(15, { weight: 600, tabular: true })}>{eur(h.valueMinor)}</Text>
              <Text style={plex(13, { color: C.accent, tabular: true })}>+{percent(h.changeBasisPoints / 10000)}</Text>
            </View>
          </ListRow>
        ))}
      </Card>
      <SectionTitle title={strings.investRecent} />
      <Card>
        {InvestAccount.recent(account).map((op: InvestOperation, i) => (
          <ListRow key={`${op.atEpochMillis}-${i}`} divider={i > 0}>
            {op.kind === 'TOP_UP' ? (
              <Badge color={C.accentSoft}>
                <Icon name="arrow_upward" color={C.accent} size={18} />
              </Badge>
            ) : (
              <TickerBadge ticker={op.ticker ?? GLOBAL_ETF} />
            )}
            <View style={[styles.grow, styles.gap12]}>
              <Text style={plex(15, { weight: 600 })}>
                {op.kind === 'TOP_UP' ? strings.investOpTopUp : strings.investOpBuy(assetName(op.ticker ?? GLOBAL_ETF))}
              </Text>
              <Text style={plex(13, { color: C.muted })}>{time(op.atEpochMillis)}</Text>
            </View>
            <Text style={plex(15, { weight: 600, color: op.kind === 'TOP_UP' ? C.accent : C.text, tabular: true })}>
              {op.kind === 'TOP_UP' ? '+' : '−'}
              {eur(op.amountMinor)}
            </Text>
          </ListRow>
        ))}
      </Card>
    </ScrollView>
  );
}

function SectionTitle({ title, trailing }: { title: string; trailing?: string }) {
  return (
    <View style={[styles.row, styles.section]}>
      <Text style={[plex(16, { weight: 700 }), styles.grow]} accessibilityRole="header">
        {title}
      </Text>
      {trailing !== undefined && <Text style={plex(13, { color: C.muted })}>{trailing}</Text>}
    </View>
  );
}

// ── Top-up ───────────────────────────────────────────────────────────────────────────────────────────────

/**
 * The amount to top up the account with, typed on the system keyboard. "Continue" registers the transfer and the
 * SDK sheet takes it from there; `children` is the demo panel row and a registration error, if any.
 */
function TopUp({
  model,
  busy,
  onBack,
  onContinue,
  children,
}: {
  model: InvestModel;
  busy: boolean;
  onBack: () => void;
  onContinue: () => void;
  children: ReactNode;
}) {
  const { strings, eur, eurWhole, separator } = useFormat();
  const amount = model.amount;
  const status = model.status;
  const shown = amount.replace('.', separator);
  const label = busy
    ? strings.topupCreating
    : status.kind === 'declined'
    ? strings.topupTryAgain
    : strings.topupContinue(amountMinor(amount) === undefined ? '—' : eur(amountMinor(amount)!));
  return (
    <View style={styles.fill}>
      <ScrollView contentContainerStyle={styles.page} keyboardShouldPersistTaps="handled" keyboardDismissMode="on-drag">
        <View style={styles.titleBar}>
          <View style={styles.titleBack}>
            <BackButton color={C.text} icon="chevron_left" background={C.surface} onPress={onBack} />
          </View>
          <Text style={plex(17, { weight: 600 })} accessibilityRole="header">
            {strings.topupTitle}
          </Text>
        </View>
        <Banner status={status} />
        <View style={styles.top12}>
          <Card>
            <View style={styles.row}>
              <Badge color={C.accentSoft}>
                <Icon name="trending_up" color={C.accent} size={20} />
              </Badge>
              <View style={[styles.grow, styles.gap12]}>
                <Text style={plex(12, { color: C.muted })}>{strings.topupTo}</Text>
                <Text numberOfLines={1} style={plex(15, { weight: 600 })}>
                  {strings.investAccount(northwind.accountLast4)}
                </Text>
              </View>
              <View style={styles.end}>
                <Text style={plex(12, { color: C.muted })}>{strings.topupAvailable}</Text>
                <Text style={plex(15, { weight: 600, tabular: true })}>{eur(model.account.freeCashMinor)}</Text>
              </View>
            </View>
          </Card>
        </View>
        <Text style={[plex(13, { color: C.muted }), styles.center, styles.top22]}>{strings.topupAmount}</Text>
        <View style={[styles.row, styles.amountRow]}>
          <Text style={plex(52, { weight: 700 })}>€</Text>
          <TextInput
            testID="invest_amount"
            value={shown}
            onChangeText={model.edit.bind(model)}
            autoFocus
            keyboardType="decimal-pad"
            returnKeyType="done"
            placeholder="0"
            placeholderTextColor={C.muted}
            selectionColor={C.accent}
            cursorColor={C.accent}
            style={[plex(52, { weight: 700, tabular: true }), styles.amountInput]}
            accessibilityLabel={strings.topupAmount}
          />
        </View>
        <Text
          style={[
            plex(13, { color: amountValid(amount) || amount === '' ? C.muted : C.error, tabular: true }),
            styles.center,
          ]}
        >
          {strings.topupLimits(eur(MIN_TOP_UP), eur(MAX_TOP_UP))}
        </Text>
        <View style={[styles.row, styles.top16, styles.presets]}>
          {PRESETS.map(preset => {
            const on = amountMinor(amount) === preset;
            return (
              <Tap
                key={preset}
                rippleColor="#FFFFFF1F"
                accessibilityState={{ selected: on }}
                onPress={() => model.preset(preset)}
                style={[styles.preset, on && styles.presetOn]}
              >
                <Text style={plex(14, { weight: 600, color: on ? C.accent : C.text, tabular: true })}>
                  {eurWhole(preset)}
                </Text>
              </Tap>
            );
          })}
        </View>
      </ScrollView>
      <View style={styles.footer}>
        {children}
        <PrimaryButton
          testID="invest_continue"
          label={label}
          busy={busy}
          enabled={canContinue(status, amount)}
          onPress={onContinue}
        />
      </View>
    </View>
  );
}

function Banner({ status }: { status: TopUpStatus }) {
  const { strings } = useL10n();
  if (status.kind !== 'declined' && status.kind !== 'unconfirmed') return null;
  const title = status.kind === 'declined' ? strings.topupFailedTitle : strings.topupUnknownTitle;
  const body = status.kind === 'declined' ? strings.topupFailedBody : strings.topupUnknownBody;
  return (
    <View style={styles.banner} accessibilityLiveRegion="polite">
      <Icon name="error_outline" color={C.error} size={22} />
      <View style={[styles.grow, styles.gap10]}>
        <Text style={plex(15, { weight: 600 })}>{title}</Text>
        <Text style={plex(13, { color: C.muted })}>{body}</Text>
        {status.kind === 'declined' && status.message !== undefined && (
          <Text style={[plex(12, { color: C.muted }), styles.top6]}>{strings.topupFailedDetail(status.message)}</Text>
        )}
      </View>
    </View>
  );
}

// ── Done ─────────────────────────────────────────────────────────────────────────────────────────────────

/** The transfer went through: what was credited, to where, and the order to find it by at the gateway. */
function Done({
  status,
  canInvest,
  onInvest,
  onPortfolio,
}: {
  status: Extract<TopUpStatus, { kind: 'done' }>;
  canInvest: boolean;
  onInvest: () => void;
  onPortfolio: () => void;
}) {
  const { strings, eur, time } = useFormat();
  const id = status.orderId;
  return (
    <View style={styles.fill}>
      <ScrollView contentContainerStyle={styles.page}>
        <View style={styles.checkOuter}>
          <View style={styles.checkInner}>
            <Icon name="check" color={C.onAccent} size={30} />
          </View>
        </View>
        <Text style={[plex(40, { weight: 700, tabular: true }), styles.center, styles.top22]}>
          +{eur(status.amountMinor)}
        </Text>
        <Text style={[plex(15, { color: C.muted }), styles.center]}>
          {strings.doneCredited(northwind.accountLast4)}
        </Text>
        <View style={styles.top28}>
          <Card>
            <Detail label={strings.doneOperation} divider={false}>
              <Text style={plex(14)}>{strings.doneOperationValue}</Text>
            </Detail>
            <Detail label={strings.doneFreeCash}>
              <Text style={plex(14, { weight: 600, color: C.accent, tabular: true })}>
                {eur(status.freeCashBefore)} → {eur(status.freeCashAfter)}
              </Text>
            </Detail>
            <Detail label={strings.doneOrder}>
              <Text numberOfLines={1} style={plex(14, { tabular: true })}>
                {id.length <= 20 ? id : `${id.slice(0, 8)}…${id.slice(-8)}`}
              </Text>
            </Detail>
            <Detail label={strings.doneTime}>
              <Text style={plex(14)}>{time(status.at)}</Text>
            </Detail>
          </Card>
        </View>
      </ScrollView>
      <View style={styles.footer}>
        <PrimaryButton label={strings.doneInvest(eur(status.amountMinor))} enabled={canInvest} onPress={onInvest} />
        <OutlineButton label={strings.doneToPortfolio} onPress={onPortfolio} />
      </View>
    </View>
  );
}

function Detail({ label, divider = true, children }: { label: string; divider?: boolean; children: ReactNode }) {
  return (
    <View style={[styles.detail, divider && styles.divider]}>
      <Text style={plex(14, { color: C.muted })}>{label}</Text>
      <View style={styles.detailValue}>{children}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: C.background },
  fill: { flex: 1 },
  page: { paddingHorizontal: 20, paddingBottom: 24 },
  row: { flexDirection: 'row', alignItems: 'center' },
  grow: { flex: 1 },
  end: { alignItems: 'flex-end' },
  center: { textAlign: 'center' },
  gap8: { marginStart: 8 },
  gap10: { marginStart: 10 },
  gap12: { marginHorizontal: 12 },
  top6: { marginTop: 6 },
  top8: { marginTop: 8 },
  top12: { marginTop: 12 },
  top16: { marginTop: 16 },
  top18: { marginTop: 18 },
  top22: { marginTop: 22 },
  top28: { marginTop: 28 },
  tile: { flex: 1, minHeight: 224, borderRadius: 20, overflow: 'hidden', padding: 18, backgroundColor: C.surface },
  tileGlow: {
    position: 'absolute',
    top: -160,
    right: -160,
    width: 420,
    height: 420,
    borderRadius: 210,
    backgroundColor: '#16653480',
  },
  tileChart: { flex: 1, marginVertical: 12, minHeight: 80 },
  aftPill: {
    backgroundColor: '#22C55E2E',
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 99,
    overflow: 'hidden',
  },
  chart: { overflow: 'hidden' },
  portfolioChart: { height: 96, marginTop: 10, marginBottom: 6 },
  segment: { position: 'absolute', height: 2.2, borderRadius: 1.1, backgroundColor: C.accent },
  dot: { position: 'absolute', width: 8, height: 8, borderRadius: 4, backgroundColor: C.accent },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#15803D',
    alignItems: 'center',
    justifyContent: 'center',
    marginStart: 12,
  },
  pill: {
    backgroundColor: C.accentSoft,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 99,
    overflow: 'hidden',
  },
  periods: {
    flexDirection: 'row',
    padding: 3,
    backgroundColor: C.surface,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: C.divider,
  },
  period: { flex: 1, minHeight: 36, borderRadius: 9, alignItems: 'center', justifyContent: 'center' },
  periodOn: { backgroundColor: C.surface2 },
  buttons: { gap: 10 },
  button: {
    minHeight: 52,
    borderRadius: 14,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    paddingHorizontal: 12,
    overflow: 'hidden',
  },
  primary: { backgroundColor: C.accent },
  disabled: { opacity: 0.35 },
  outline: { borderWidth: 1, borderColor: C.fieldBorder },
  outlineDisabled: { borderColor: '#33415580' },
  card: {
    backgroundColor: C.surface,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: C.divider,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  cardHighlight: { borderColor: '#22C55EB3' },
  badge: { width: 40, height: 40, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  section: { marginTop: 20, marginBottom: 8 },
  listRow: { flexDirection: 'row', alignItems: 'center', paddingVertical: 11 },
  divider: { borderTopWidth: 1, borderTopColor: C.divider },
  titleBar: { minHeight: 52, alignItems: 'center', justifyContent: 'center', marginTop: 4 },
  titleBack: { position: 'absolute', start: 0 },
  amountRow: { justifyContent: 'center' },
  amountInput: { minWidth: 40, padding: 0, color: C.text },
  presets: { gap: 8 },
  preset: {
    flex: 1,
    minHeight: 44,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: C.fieldBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  presetOn: { borderColor: C.accent, backgroundColor: C.accentSoft },
  banner: {
    flexDirection: 'row',
    marginTop: 12,
    padding: 12,
    paddingHorizontal: 14,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#F8717173',
    backgroundColor: C.errorSoft,
  },
  footer: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 12, gap: 10 },
  checkOuter: {
    marginTop: 64,
    alignSelf: 'center',
    width: 88,
    height: 88,
    borderRadius: 44,
    backgroundColor: C.accentSoft,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkInner: {
    width: 60,
    height: 60,
    borderRadius: 30,
    backgroundColor: C.accent,
    alignItems: 'center',
    justifyContent: 'center',
  },
  detail: { flexDirection: 'row', alignItems: 'center', paddingVertical: 10, gap: 12 },
  detailValue: { flex: 1, alignItems: 'flex-end' },
});
