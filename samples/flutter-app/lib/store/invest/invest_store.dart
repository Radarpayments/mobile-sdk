import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:intl/intl.dart' show DateFormat, NumberFormat;

import '../../app_scope.dart';
import '../../checkout/checkout.dart';
import '../../l10n/app_localizations.dart';
import '../model/cart.dart';
import '../model/catalog.dart';
import '../scenario_sheet.dart';
import '../store_checkout_bar.dart';
import '../theme/cover_style.dart';
import 'invest_account.dart';
import 'invest_colors.dart';
import 'invest_model.dart';
import 'northwind_profile.dart';
import 'top_up_amount.dart';

/// Northwind Invest, the AFT demo: portfolio → top-up amount → SDK sheet ("Transfer") → done. The transfer is
/// registered with `registerP2P.do` and the investor's details; the account is credited only on a completed transfer
/// and kept on the device. "Add card" registers a transfer of 0 that only saves the card. The investor is always the
/// demo buyer: an investment app knows its user, and a saved card shows up only for the clientId it was saved for.
class InvestStore extends StatefulWidget {
  const InvestStore({super.key, required this.onOpenSettings});

  final VoidCallback onOpenSettings;

  @override
  State<InvestStore> createState() => _InvestStoreState();
}

enum _Screen { portfolio, topUp, done }

class _InvestStoreState extends State<InvestStore> {
  late final InvestModel _model;
  late final StoreCheckout _checkout;
  _Screen _screen = _Screen.portfolio;
  DateTime _now = DateTime.now();

  /// The sheet is open for "Add card": its result saves a card, it credits nothing.
  bool _addingCard = false;

  @override
  void initState() {
    super.initState();
    final scope = AppScope.of(context);
    _model = InvestModel(load: () => scope.prefs.investAccount, save: scope.prefs.setInvestAccount);
    _checkout = StoreCheckout(onMdOrder: scope.prefs.setLastMdOrder, present: scope.presentSheet);
    // A reset in the developer settings shows at once.
    scope.prefs.addListener(_reload);
  }

  void _reload() => _model.reload();

  @override
  void dispose() {
    AppScope.of(context).prefs.removeListener(_reload);
    _model.dispose();
    _checkout.dispose();
    super.dispose();
  }

  void _toPortfolio() {
    _model.leaveTopUp();
    setState(() {
      _now = DateTime.now();
      _screen = _Screen.portfolio;
    });
  }

  Future<void> _pay() async {
    final amountMinor = _model.amount.minor;
    if (amountMinor == null) return;
    setState(() => _addingCard = false);
    final outcome = await _transfer(amountMinor);
    if (outcome == null || !mounted) return;
    _model.onOutcome(outcome, amountMinor);
    if (_model.status is TopUpDone) setState(() => _screen = _Screen.done);
  }

  Future<void> _addCard() async {
    setState(() => _addingCard = true);
    final outcome = await _transfer(0);
    if (outcome == null || !mounted) return;
    _model.onCardOutcome(outcome);
  }

  Future<CheckoutOutcome?> _transfer(int amountMinor) {
    final scope = AppScope.of(context);
    final prefs = scope.prefs;
    return _checkout.pay(
      registrar: scope.transferRegistrarFactory(prefs, NorthwindProfile.prefill),
      config: buildSdkConfig(
        baseUrl: prefs.baseUrl,
        scenario: prefs.scenario,
        cover: coverStyle(CoverId.invest),
        themeOverride: prefs.sheetTheme,
        locale: prefs.localeOverride,
        storeName: context.l10n.investName,
        googlePayMerchantId: prefs.googlePayMerchantId,
        googlePayGateway: prefs.googlePayGateway,
        applePayMerchantId: prefs.applePayMerchantId,
      ),
      amountMinor: amountMinor,
      clientId: prefs.clientId,
    );
  }

  @override
  Widget build(BuildContext context) {
    return CoverTheme(
      id: CoverId.invest,
      child: PopScope(
        canPop: _screen == _Screen.portfolio,
        onPopInvokedWithResult: (didPop, _) {
          if (!didPop) _toPortfolio();
        },
        child: Scaffold(
          backgroundColor: InvestColors.background,
          body: SafeArea(
            child: ListenableBuilder(
              listenable: Listenable.merge([_model, _checkout, AppScope.of(context).prefs]),
              builder: (context, _) => AnimatedSwitcher(
                duration: const Duration(milliseconds: 180),
                child: switch (_screen) {
                  _Screen.portfolio => _Portfolio(
                    key: const ValueKey('portfolio'),
                    account: _model.account,
                    now: _now,
                    onTopUp: () => setState(() => _screen = _Screen.topUp),
                    onInvest: () => _model.invest(_model.account.freeCashMinor),
                  ),
                  _Screen.topUp => _TopUp(
                    key: const ValueKey('topUp'),
                    model: _model,
                    checkout: _checkout,
                    addingCard: _addingCard,
                    onBack: _toPortfolio,
                    onContinue: _pay,
                    onAddCard: _addCard,
                    onOpenSettings: widget.onOpenSettings,
                  ),
                  _Screen.done => switch (_model.status) {
                    final TopUpDone done => _Done(
                      key: const ValueKey('done'),
                      done: done,
                      canInvest: _model.account.canInvest,
                      onInvest: () {
                        _model.invest(done.amountMinor);
                        _toPortfolio();
                      },
                      onPortfolio: _toPortfolio,
                    ),
                    _ => const SizedBox.shrink(),
                  },
                },
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Northwind Invest's tile on the start screen: no photo, its own colours and a rising line.
class InvestTile extends StatelessWidget {
  const InvestTile({super.key, required this.onTap});

  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return Material(
      key: const Key('cover_invest'),
      borderRadius: BorderRadius.circular(20),
      clipBehavior: Clip.antiAlias,
      child: Ink(
        decoration: const BoxDecoration(
          gradient: RadialGradient(
            center: Alignment.topRight,
            radius: 1.3,
            colors: [Color(0xFF166534), InvestColors.surface],
          ),
        ),
        child: InkWell(
          onTap: onTap,
          child: Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.trending_up, color: InvestColors.accent, size: 22),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text('Northwind', style: plex(15, weight: FontWeight.w600)),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                      decoration: BoxDecoration(
                        color: const Color(0x2E22C55E),
                        borderRadius: BorderRadius.circular(99),
                      ),
                      child: Text(
                        'AFT',
                        style: plex(12, weight: FontWeight.w600, color: InvestColors.accent),
                      ),
                    ),
                  ],
                ),
                const Expanded(
                  child: Padding(
                    padding: EdgeInsets.symmetric(vertical: 12),
                    child: PortfolioChart(period: ChartPeriod.all),
                  ),
                ),
                Text(l10n.investName, style: plex(24, weight: FontWeight.w700)),
                Text(l10n.investTileSub, style: plex(13, color: const Color(0xFFCBD5E1))),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Periods of the portfolio chart; each shows its own fixed curve (the demo has no price history).
enum ChartPeriod {
  week([0.55, 0.5, 0.58, 0.52, 0.6, 0.57, 0.66]),
  month([0.18, 0.24, 0.21, 0.35, 0.31, 0.44, 0.41, 0.57, 0.52, 0.68, 0.64, 0.8, 0.85]),
  quarter([0.3, 0.22, 0.35, 0.28, 0.45, 0.4, 0.52, 0.6, 0.55, 0.72]),
  year([0.1, 0.2, 0.16, 0.3, 0.42, 0.38, 0.5, 0.47, 0.62, 0.7, 0.66, 0.82]),
  all([0.05, 0.12, 0.1, 0.22, 0.3, 0.28, 0.45, 0.55, 0.5, 0.7, 0.78, 0.9]);

  const ChartPeriod(this.points);

  final List<double> points;

  String label(AppLocalizations l10n) => switch (this) {
    week => l10n.investPeriod1w,
    month => l10n.investPeriod1m,
    quarter => l10n.investPeriod3m,
    year => l10n.investPeriod1y,
    all => l10n.investPeriodAll,
  };
}

/// The portfolio's line with a fading area under it, ending in a dot.
class PortfolioChart extends StatelessWidget {
  const PortfolioChart({super.key, required this.period});

  final ChartPeriod period;

  @override
  Widget build(BuildContext context) => ExcludeSemantics(
    // Time runs left to right in every language.
    child: Directionality(
      textDirection: TextDirection.ltr,
      child: CustomPaint(painter: _ChartPainter(period.points), size: Size.infinite),
    ),
  );
}

class _ChartPainter extends CustomPainter {
  _ChartPainter(this.points);

  final List<double> points;

  @override
  void paint(Canvas canvas, Size size) {
    const pad = 6.0;
    final step = size.width / (points.length - 1);
    Offset at(int i) => Offset(i * step, pad + (1 - points[i]) * (size.height - 2 * pad));
    final line = Path()..moveTo(at(0).dx, at(0).dy);
    for (var i = 1; i < points.length; i++) {
      line.lineTo(at(i).dx, at(i).dy);
    }
    final area = Path.from(line)
      ..lineTo(size.width, size.height)
      ..lineTo(0, size.height)
      ..close();
    canvas.drawPath(
      area,
      Paint()
        ..shader = const LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [Color(0x4722C55E), Color(0x0022C55E)],
        ).createShader(Offset.zero & size),
    );
    canvas.drawPath(
      line,
      Paint()
        ..color = InvestColors.accent
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.2
        ..strokeJoin = StrokeJoin.round,
    );
    canvas.drawCircle(at(points.length - 1), 4, Paint()..color = InvestColors.accent);
  }

  @override
  bool shouldRepaint(_ChartPainter old) => old.points != points;
}

class _Card extends StatelessWidget {
  const _Card({required this.child, this.highlight = false});

  final Widget child;
  final bool highlight;

  @override
  Widget build(BuildContext context) => AnimatedContainer(
    duration: const Duration(milliseconds: 300),
    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
    decoration: BoxDecoration(
      color: InvestColors.surface,
      borderRadius: BorderRadius.circular(18),
      border: Border.all(color: highlight ? const Color(0xB322C55E) : InvestColors.divider),
    ),
    child: child,
  );
}

class _PrimaryButton extends StatelessWidget {
  const _PrimaryButton({super.key, required this.label, required this.onPressed, this.icon, this.busy = false});

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool busy;

  @override
  Widget build(BuildContext context) => FilledButton(
    onPressed: busy ? null : onPressed,
    style: FilledButton.styleFrom(
      backgroundColor: InvestColors.accent,
      foregroundColor: InvestColors.onAccent,
      disabledBackgroundColor: busy ? InvestColors.accent : const Color(0x5922C55E),
      disabledForegroundColor: busy ? InvestColors.onAccent : const Color(0xB3052E16),
      minimumSize: const Size.fromHeight(52),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
    ),
    child: Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (busy)
          const SizedBox.square(
            dimension: 18,
            child: CircularProgressIndicator(strokeWidth: 2, color: InvestColors.onAccent),
          )
        else if (icon != null)
          Icon(icon, size: 18),
        if (busy || icon != null) const SizedBox(width: 8),
        Flexible(
          child: Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: plex(15, weight: FontWeight.w700, color: InvestColors.onAccent),
          ),
        ),
      ],
    ),
  );
}

class _OutlineButton extends StatelessWidget {
  const _OutlineButton({super.key, required this.label, required this.onPressed, this.icon});

  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final color = onPressed == null ? InvestColors.muted : InvestColors.text;
    return OutlinedButton(
      onPressed: onPressed,
      style: OutlinedButton.styleFrom(
        foregroundColor: InvestColors.text,
        minimumSize: const Size.fromHeight(52),
        side: BorderSide(color: onPressed == null ? const Color(0x80334155) : InvestColors.fieldBorder),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[Icon(icon, size: 18, color: color), const SizedBox(width: 6)],
          Flexible(
            child: Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: plex(15, weight: FontWeight.w700, color: color),
            ),
          ),
        ],
      ),
    );
  }
}

class _BackButton extends StatelessWidget {
  const _BackButton({required this.onPressed});

  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) => IconButton(
    onPressed: onPressed,
    tooltip: MaterialLocalizations.of(context).backButtonTooltip,
    style: IconButton.styleFrom(
      fixedSize: const Size.square(44),
      backgroundColor: InvestColors.surface,
      side: const BorderSide(color: InvestColors.divider),
    ),
    icon: const Icon(Icons.chevron_left, color: InvestColors.text),
  );
}

class _Badge extends StatelessWidget {
  const _Badge({required this.color, required this.child});

  final Color color;
  final Widget child;

  @override
  Widget build(BuildContext context) => Container(
    width: 40,
    height: 40,
    alignment: Alignment.center,
    decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(12)),
    child: child,
  );
}

class _TickerBadge extends StatelessWidget {
  const _TickerBadge(this.ticker);

  final String ticker;

  @override
  Widget build(BuildContext context) {
    final glb = ticker == InvestAccount.globalEtf;
    return _Badge(
      color: glb ? InvestColors.glb : InvestColors.tch,
      child: Text(
        ticker,
        style: plex(12, weight: FontWeight.w700, color: glb ? InvestColors.onGlb : InvestColors.onTch),
      ),
    );
  }
}

String _assetName(AppLocalizations l10n, String ticker) =>
    ticker == InvestAccount.globalEtf ? l10n.investAssetGlb : l10n.investAssetTch;

String _time(BuildContext context, int at) =>
    DateFormat.yMMMd(Localizations.localeOf(context).toString())
        .add_Hm()
        .format(DateTime.fromMillisecondsSinceEpoch(at));

String _percent(BuildContext context, double value) => NumberFormat.decimalPercentPattern(
  locale: Localizations.localeOf(context).toString(),
  decimalDigits: 1,
).format(value);

/// [minor] counted up (or down) from the value shown before, so a credit is seen arriving.
class _CountedEur extends StatelessWidget {
  const _CountedEur(this.minor, {required this.style});

  final int minor;
  final TextStyle style;

  @override
  Widget build(BuildContext context) {
    final reduced = MediaQuery.of(context).disableAnimations;
    return TweenAnimationBuilder<double>(
      tween: Tween(end: minor.toDouble()),
      duration: reduced ? Duration.zero : const Duration(milliseconds: 600),
      curve: Curves.easeOut,
      builder: (context, value, _) => Text(context.eur(value.round()), style: style),
    );
  }
}

class _ListRow extends StatelessWidget {
  const _ListRow({required this.divider, required this.children});

  final bool divider;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) => Container(
    decoration: BoxDecoration(
      border: divider ? const Border(top: BorderSide(color: InvestColors.divider)) : null,
    ),
    padding: const EdgeInsets.symmetric(vertical: 11),
    child: Row(children: children),
  );
}

/// Northwind's home: the portfolio, its free cash and the latest operations. After a top-up the amounts count up
/// to the new values and the free-cash card is outlined while the credit is fresh.
class _Portfolio extends StatefulWidget {
  const _Portfolio({
    super.key,
    required this.account,
    required this.now,
    required this.onTopUp,
    required this.onInvest,
  });

  final InvestAccount account;
  final DateTime now;
  final VoidCallback onTopUp;
  final VoidCallback onInvest;

  @override
  State<_Portfolio> createState() => _PortfolioState();
}

class _PortfolioState extends State<_Portfolio> {
  ChartPeriod _period = ChartPeriod.month;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final account = widget.account;
    final first = account.operations.firstOrNull;
    final justCredited =
        first != null &&
            first.kind == OperationKind.topUp &&
            widget.now.millisecondsSinceEpoch - first.atEpochMillis < 60000
        ? first
        : null;
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
      children: [
        Row(
          children: [
            _BackButton(onPressed: () => Navigator.of(context).maybePop()),
            const SizedBox(width: 12),
            Container(
              width: 40,
              height: 40,
              alignment: Alignment.center,
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                gradient: LinearGradient(colors: [Color(0xFF166534), Color(0xFF0F766E)]),
              ),
              child: Text(NorthwindProfile.initials, style: plex(14, weight: FontWeight.w700)),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(l10n.investAccount(NorthwindProfile.accountLast4), style: plex(12, color: InvestColors.muted)),
                  Text(NorthwindProfile.name, style: plex(15, weight: FontWeight.w600)),
                ],
              ),
            ),
            // Decorative: the demo has no notifications, so it is not a button.
            const ExcludeSemantics(child: Icon(Icons.notifications_none, color: InvestColors.muted)),
          ],
        ),
        const SizedBox(height: 18),
        Text(l10n.investPortfolioValue, style: plex(13, color: InvestColors.muted)),
        _CountedEur(account.totalMinor, style: plex(36, weight: FontWeight.w700, tabular: true)),
        const SizedBox(height: 6),
        Row(
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
              decoration: BoxDecoration(color: InvestColors.accentSoft, borderRadius: BorderRadius.circular(99)),
              child: Text(
                '▲ ${context.eur(14920)} · ${_percent(context, 0.032)}',
                style: plex(13, weight: FontWeight.w600, color: InvestColors.accent, tabular: true),
              ),
            ),
            const SizedBox(width: 8),
            Text(l10n.investChangeMonth, style: plex(13, color: InvestColors.muted)),
          ],
        ),
        Padding(
          padding: const EdgeInsets.only(top: 10, bottom: 6),
          child: SizedBox(height: 96, child: PortfolioChart(period: _period)),
        ),
        Container(
          padding: const EdgeInsets.all(3),
          decoration: BoxDecoration(
            color: InvestColors.surface,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: InvestColors.divider),
          ),
          child: Row(
            children: [
              for (final p in ChartPeriod.values)
                Expanded(
                  child: Semantics(
                    selected: p == _period,
                    child: InkWell(
                      borderRadius: BorderRadius.circular(9),
                      onTap: () => setState(() => _period = p),
                      child: Container(
                        height: 36,
                        alignment: Alignment.center,
                        decoration: BoxDecoration(
                          color: p == _period ? InvestColors.surface2 : null,
                          borderRadius: BorderRadius.circular(9),
                        ),
                        child: Text(
                          p.label(l10n),
                          style: plex(
                            13,
                            weight: p == _period ? FontWeight.w600 : FontWeight.w500,
                            color: p == _period ? InvestColors.text : InvestColors.muted,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: _PrimaryButton(
                key: const Key('invest_top_up'),
                label: l10n.investTopUp,
                icon: Icons.add,
                onPressed: widget.onTopUp,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: _OutlineButton(
                label: l10n.investInvest,
                icon: Icons.trending_up,
                onPressed: account.canInvest ? widget.onInvest : null,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        _Card(
          highlight: justCredited != null,
          child: Row(
            children: [
              const _Badge(
                color: InvestColors.accentSoft,
                child: Icon(Icons.account_balance_wallet_outlined, color: InvestColors.accent, size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(l10n.investFreeCash, style: plex(13, color: InvestColors.muted)),
                    if (justCredited != null)
                      Text(
                        l10n.investFreeCashJustNow(context.eur(justCredited.amountMinor)),
                        style: plex(12, color: InvestColors.accent, tabular: true),
                      )
                    else
                      Text(l10n.investFreeCashSub, style: plex(12, color: InvestColors.muted)),
                  ],
                ),
              ),
              _CountedEur(account.freeCashMinor, style: plex(18, weight: FontWeight.w700, tabular: true)),
            ],
          ),
        ),
        _SectionTitle(l10n.investMyAssets, trailing: '${account.holdings.length}'),
        _Card(
          child: Column(
            children: [
              for (final (i, h) in account.holdings.indexed)
                _ListRow(
                  divider: i > 0,
                  children: [
                    _TickerBadge(h.ticker),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(_assetName(l10n, h.ticker), style: plex(15, weight: FontWeight.w600)),
                          Text(
                            l10n.investUnits(
                              NumberFormat('0.00', Localizations.localeOf(context).toString()).format(h.quantity),
                              context.eur(h.unitPriceMinor),
                            ),
                            style: plex(13, color: InvestColors.muted, tabular: true),
                          ),
                        ],
                      ),
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(context.eur(h.valueMinor), style: plex(15, weight: FontWeight.w600, tabular: true)),
                        Text(
                          '+${_percent(context, h.changeBasisPoints / 10000)}',
                          style: plex(13, color: InvestColors.accent, tabular: true),
                        ),
                      ],
                    ),
                  ],
                ),
            ],
          ),
        ),
        _SectionTitle(l10n.investRecent),
        _Card(
          child: Column(
            children: [
              for (final (i, op) in account.recent().indexed)
                _ListRow(
                  divider: i > 0,
                  children: [
                    if (op.kind == OperationKind.topUp)
                      const _Badge(
                        color: InvestColors.accentSoft,
                        child: Icon(Icons.arrow_upward, color: InvestColors.accent, size: 18),
                      )
                    else
                      _TickerBadge(op.ticker ?? InvestAccount.globalEtf),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            op.kind == OperationKind.topUp
                                ? l10n.investOpTopUp
                                : l10n.investOpBuy(_assetName(l10n, op.ticker ?? InvestAccount.globalEtf)),
                            style: plex(15, weight: FontWeight.w600),
                          ),
                          Text(_time(context, op.atEpochMillis), style: plex(13, color: InvestColors.muted)),
                        ],
                      ),
                    ),
                    Text(
                      '${op.kind == OperationKind.topUp ? '+' : '−'}${context.eur(op.amountMinor)}',
                      style: plex(
                        15,
                        weight: FontWeight.w600,
                        color: op.kind == OperationKind.topUp ? InvestColors.accent : InvestColors.text,
                        tabular: true,
                      ),
                    ),
                  ],
                ),
            ],
          ),
        ),
      ],
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.title, {this.trailing});

  final String title;
  final String? trailing;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 20, bottom: 8),
    child: Row(
      children: [
        Expanded(
          child: Semantics(
            header: true,
            child: Text(title, style: plex(16, weight: FontWeight.w700)),
          ),
        ),
        if (trailing != null) Text(trailing!, style: plex(13, color: InvestColors.muted)),
      ],
    ),
  );
}

/// The amount to top up the account with, typed on the system keyboard. "Continue" registers the transfer and the
/// SDK sheet takes it from there; "Add card" ([onAddCard]) saves a card for later top-ups without moving money
/// ([addingCard] while it runs).
class _TopUp extends StatefulWidget {
  const _TopUp({
    super.key,
    required this.model,
    required this.checkout,
    required this.addingCard,
    required this.onBack,
    required this.onContinue,
    required this.onAddCard,
    required this.onOpenSettings,
  });

  final InvestModel model;
  final StoreCheckout checkout;
  final bool addingCard;
  final VoidCallback onBack;
  final VoidCallback onContinue;
  final VoidCallback onAddCard;
  final VoidCallback onOpenSettings;

  @override
  State<_TopUp> createState() => _TopUpState();
}

class _TopUpState extends State<_TopUp> {
  late final TextEditingController _field;

  String get _separator => NumberFormat.decimalPattern(Localizations.localeOf(context).toString()).symbols.DECIMAL_SEP;

  String _shown(TopUpAmount amount) => amount.text.replaceAll('.', _separator);

  @override
  void initState() {
    super.initState();
    _field = TextEditingController();
    widget.model.addListener(_sync);
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (_field.text.isEmpty) _field.text = _shown(widget.model.amount);
  }

  /// A preset chip, or a character the format dropped: show what the amount really is, caret at the end.
  void _sync() {
    final shown = _shown(widget.model.amount);
    if (_field.text == shown) return;
    _field.value = TextEditingValue(
      text: shown,
      selection: TextSelection.collapsed(offset: shown.length),
    );
  }

  @override
  void dispose() {
    widget.model.removeListener(_sync);
    _field.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final model = widget.model;
    final amount = model.amount;
    final status = model.status;
    final busy = widget.checkout.busy;
    final addingCard = widget.addingCard;
    final String label;
    if (busy && !addingCard) {
      label = l10n.topupCreating;
    } else if (status is TopUpDeclined) {
      label = l10n.topupTryAgain;
    } else {
      label = l10n.topupContinue(amount.minor == null ? '—' : context.eur(amount.minor!));
    }
    return Column(
      children: [
        Expanded(
          child: ListView(
            keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
            padding: const EdgeInsets.fromLTRB(20, 4, 20, 0),
            children: [
              Stack(
                alignment: Alignment.center,
                children: [
                  Align(
                    alignment: AlignmentDirectional.centerStart,
                    child: _BackButton(onPressed: widget.onBack),
                  ),
                  Text(l10n.topupTitle, style: plex(17, weight: FontWeight.w600)),
                ],
              ),
              _Banner(status: status),
              const SizedBox(height: 12),
              _Card(
                child: Row(
                  children: [
                    const _Badge(
                      color: InvestColors.accentSoft,
                      child: Icon(Icons.trending_up, color: InvestColors.accent, size: 20),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(l10n.topupTo, style: plex(12, color: InvestColors.muted)),
                          Text(
                            l10n.investAccount(NorthwindProfile.accountLast4),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: plex(15, weight: FontWeight.w600),
                          ),
                        ],
                      ),
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(l10n.topupAvailable, style: plex(12, color: InvestColors.muted)),
                        Text(
                          context.eur(model.account.freeCashMinor),
                          style: plex(15, weight: FontWeight.w600, tabular: true),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 10),
              _OutlineButton(
                key: const Key('topup_add_card'),
                label: busy && addingCard ? l10n.topupAddingCard : l10n.topupAddCard,
                icon: Icons.credit_card,
                onPressed: busy ? null : widget.onAddCard,
              ),
              const SizedBox(height: 22),
              Text(
                l10n.topupAmount,
                textAlign: TextAlign.center,
                style: plex(13, color: InvestColors.muted),
              ),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text('€', style: plex(52, weight: FontWeight.w700)),
                  IntrinsicWidth(
                    child: TextField(
                      key: const Key('invest_amount'),
                      controller: _field,
                      autofocus: true,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      textInputAction: TextInputAction.done,
                      inputFormatters: [FilteringTextInputFormatter.allow(amountCharacters)],
                      onChanged: model.edit,
                      cursorColor: InvestColors.accent,
                      style: plex(52, weight: FontWeight.w700, tabular: true),
                      decoration: InputDecoration(
                        isCollapsed: true,
                        border: InputBorder.none,
                        hintText: '0',
                        hintStyle: plex(52, weight: FontWeight.w700, color: InvestColors.muted),
                      ),
                    ),
                  ),
                ],
              ),
              Text(
                l10n.topupLimits(context.eur(TopUpAmount.min), context.eur(TopUpAmount.max)),
                textAlign: TextAlign.center,
                style: plex(
                  13,
                  color: amount.valid || amount.text.isEmpty ? InvestColors.muted : InvestColors.error,
                  tabular: true,
                ),
              ),
              const SizedBox(height: 16),
              Row(
                children: [
                  for (final (i, preset) in TopUpAmount.presets.indexed) ...[
                    if (i > 0) const SizedBox(width: 8),
                    Expanded(
                      child: _Preset(minor: preset, on: amount.minor == preset, onTap: () => model.preset(preset)),
                    ),
                  ],
                ],
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (widget.checkout.state case CheckoutRegistrationFailed(:final reason)) ...[
                RegistrationError(cover: CoverId.invest, reason: reason, onOpenSettings: widget.onOpenSettings),
                const SizedBox(height: 10),
              ],
              ScenarioRow(
                cover: CoverId.invest,
                buyerWithCards: true,
                onTap: () => showScenarioSheet(context, buyerChoice: false),
              ),
              const SizedBox(height: 10),
              _PrimaryButton(
                key: const Key('invest_continue'),
                label: label,
                busy: busy && !addingCard,
                onPressed: status.canContinue(amount) && !busy ? widget.onContinue : null,
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _Preset extends StatelessWidget {
  const _Preset({required this.minor, required this.on, required this.onTap});

  final int minor;
  final bool on;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final whole = NumberFormat.simpleCurrency(
      locale: Localizations.localeOf(context).toString(),
      name: 'EUR',
      decimalDigits: 0,
    );
    return Material(
      color: on ? InvestColors.accentSoft : Colors.transparent,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: on ? InvestColors.accent : InvestColors.fieldBorder),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: SizedBox(
          height: 44,
          child: Center(
            child: Text(
              whole.format(minor / 100),
              style: plex(
                14,
                weight: FontWeight.w600,
                color: on ? InvestColors.accent : InvestColors.text,
                tabular: true,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Banner extends StatelessWidget {
  const _Banner({required this.status});

  final TopUpStatus status;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final (title, body) = switch (status) {
      TopUpDeclined() => (l10n.topupFailedTitle, l10n.topupFailedBody),
      TopUpUnconfirmed() => (l10n.topupUnknownTitle, l10n.topupUnknownBody),
      TopUpCardAdded() => (l10n.topupCardAddedTitle, l10n.topupCardAddedBody),
      TopUpCardDeclined() => (l10n.topupCardFailedTitle, l10n.topupCardFailedBody),
      TopUpCardUnconfirmed() => (l10n.topupUnknownTitle, l10n.topupCardUnknownBody),
      _ => (null, null),
    };
    if (title == null || body == null) return const SizedBox.shrink();
    final added = status is TopUpCardAdded;
    final tint = added ? InvestColors.accent : InvestColors.error;
    return Semantics(
      liveRegion: true,
      child: Container(
        margin: const EdgeInsets.only(top: 12),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: added ? InvestColors.accentSoft : InvestColors.errorSoft,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: tint.withValues(alpha: 0.45)),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(added ? Icons.check_circle_outline : Icons.error_outline, color: tint, size: 22),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: plex(15, weight: FontWeight.w600)),
                  Text(body, style: plex(13, color: InvestColors.muted)),
                  if (status case TopUpDeclined(:final message?) || TopUpCardDeclined(:final message?))
                    Padding(
                      padding: const EdgeInsets.only(top: 6),
                      child: Text(l10n.topupFailedDetail(message), style: plex(12, color: InvestColors.muted)),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// The transfer went through: what was credited, to where, and the order to find it by at the gateway.
class _Done extends StatelessWidget {
  const _Done({
    super.key,
    required this.done,
    required this.canInvest,
    required this.onInvest,
    required this.onPortfolio,
  });

  final TopUpDone done;
  final bool canInvest;
  final VoidCallback onInvest;
  final VoidCallback onPortfolio;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final id = done.orderId;
    return Column(
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            children: [
              const SizedBox(height: 64),
              Center(
                child: Container(
                  width: 88,
                  height: 88,
                  decoration: const BoxDecoration(color: InvestColors.accentSoft, shape: BoxShape.circle),
                  alignment: Alignment.center,
                  child: Container(
                    width: 60,
                    height: 60,
                    decoration: const BoxDecoration(color: InvestColors.accent, shape: BoxShape.circle),
                    child: const Icon(Icons.check, color: InvestColors.onAccent, size: 30),
                  ),
                ),
              ),
              const SizedBox(height: 22),
              Text(
                '+${context.eur(done.amountMinor)}',
                textAlign: TextAlign.center,
                style: plex(40, weight: FontWeight.w700, tabular: true),
              ),
              Text(
                l10n.doneCredited(NorthwindProfile.accountLast4),
                textAlign: TextAlign.center,
                style: plex(15, color: InvestColors.muted),
              ),
              const SizedBox(height: 28),
              _Card(
                child: Column(
                  children: [
                    _DetailRow(l10n.doneOperation, Text(l10n.doneOperationValue, style: plex(14)), divider: false),
                    _DetailRow(
                      l10n.doneFreeCash,
                      Text(
                        '${context.eur(done.freeCashBefore)} → ${context.eur(done.freeCashAfter)}',
                        style: plex(14, weight: FontWeight.w600, color: InvestColors.accent, tabular: true),
                      ),
                    ),
                    _DetailRow(
                      l10n.doneOrder,
                      Text(
                        id.length <= 20 ? id : '${id.substring(0, 8)}…${id.substring(id.length - 8)}',
                        style: plex(14, tabular: true),
                      ),
                    ),
                    _DetailRow(l10n.doneTime, Text(_time(context, done.at), style: plex(14))),
                  ],
                ),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _PrimaryButton(
                label: l10n.doneInvest(context.eur(done.amountMinor)),
                onPressed: canInvest ? onInvest : null,
              ),
              const SizedBox(height: 10),
              _OutlineButton(label: l10n.doneToPortfolio, onPressed: onPortfolio),
            ],
          ),
        ),
      ],
    );
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow(this.label, this.value, {this.divider = true});

  final String label;
  final Widget value;
  final bool divider;

  @override
  Widget build(BuildContext context) => Container(
    decoration: BoxDecoration(
      border: divider ? const Border(top: BorderSide(color: InvestColors.divider)) : null,
    ),
    padding: const EdgeInsets.symmetric(vertical: 10),
    child: Row(
      children: [
        Text(label, style: plex(14, color: InvestColors.muted)),
        const SizedBox(width: 12),
        Expanded(
          child: Align(alignment: AlignmentDirectional.centerEnd, child: value),
        ),
      ],
    ),
  );
}
