import 'package:flutter/material.dart';

import '../app_scope.dart';
import '../checkout/checkout.dart';
import 'model/cart.dart';
import 'model/catalog.dart';
import 'store_checkout_bar.dart';
import 'store_model.dart';
import 'theme/cover_style.dart';

/// The store's own screen after the sheet closes: the merchant app, not the SDK, decides what comes
/// next. [headline] replaces the default title area (the coffee shop shows its order number).
class StoreResultScreen extends StatelessWidget {
  const StoreResultScreen({
    super.key,
    required this.cover,
    required this.receipt,
    required this.onDone,
    required this.onRetry,
    this.headline,
  });

  final CoverId cover;
  final Receipt receipt;
  final VoidCallback onDone;
  final VoidCallback onRetry;
  final Widget? headline;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(cover);
    final l10n = context.l10n;
    final outcome = receipt.outcome;
    return CoverTheme(
      id: cover,
      child: Scaffold(
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Spacer(flex: 6),
                ...switch (outcome) {
                  Success(:final paymentId) => [
                    headline ??
                        Column(
                          children: [
                            _ResultMark(icon: Icons.check, tint: style.accent),
                            _Title(_successTitle(cover)(l10n), style),
                          ],
                        ),
                    _Body(_successBody(cover)(l10n), style),
                    Container(
                      margin: const EdgeInsets.only(top: 24),
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        border: Border.all(color: style.border),
                        borderRadius: BorderRadius.circular(style.radius),
                      ),
                      child: Column(
                        children: [
                          _InfoRow(l10n.resultAmount, context.eur(receipt.amountMinor), style, monospace: false),
                          const SizedBox(height: 6),
                          _InfoRow(l10n.resultPaymentId, paymentId, style, monospace: true),
                        ],
                      ),
                    ),
                  ],
                  Unconfirmed() => [
                    _ResultMark(icon: Icons.schedule, tint: style.muted),
                    _Title(l10n.resultUnconfirmedTitle, style),
                    _Body(l10n.resultUnconfirmedBody, style),
                  ],
                  Declined() || Cancelled() => [
                    const _ResultMark(icon: Icons.close, tint: Color(0xFFDC2626)),
                    _Title(l10n.resultDeclinedTitle, style),
                    _Body(l10n.resultDeclinedBody, style),
                    // The gateway's own words, as they came: useful when showing the demo, secondary for a buyer.
                    if (outcome case Declined(:final message?))
                      Padding(
                        padding: const EdgeInsets.only(top: 12),
                        child: Text(
                          message,
                          textAlign: TextAlign.center,
                          style: style.body(12, lineHeight: 16, color: style.muted),
                        ),
                      ),
                  ],
                },
                const Spacer(flex: 10),
                // Paying again while the first payment may have gone through could charge the buyer twice.
                if (outcome is Declined) ...[
                  PayButton(cover: cover, label: l10n.resultTryAgain, busy: false, enabled: true, onPressed: onRetry),
                  const SizedBox(height: 10),
                ],
                OutlinedButton(
                  onPressed: onDone,
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size.fromHeight(52),
                    side: BorderSide(color: style.border),
                    shape: RoundedRectangleBorder(borderRadius: style.buttonRadius),
                  ),
                  child: Text(
                    outcome is Success ? l10n.resultDone : l10n.resultBackToStore,
                    style: style.body(15, weight: FontWeight.w700, color: style.text),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ResultMark extends StatelessWidget {
  const _ResultMark({required this.icon, required this.tint});

  final IconData icon;
  final Color tint;

  @override
  Widget build(BuildContext context) => Center(
    child: Container(
      width: 72,
      height: 72,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        border: Border.all(color: tint, width: 1.5),
      ),
      child: Icon(icon, color: tint, size: 32),
    ),
  );
}

class _Title extends StatelessWidget {
  const _Title(this.text, this.style);

  final String text;
  final CoverStyle style;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 18),
    child: Text(text, textAlign: TextAlign.center, style: style.heading(28, lineHeight: 32)),
  );
}

class _Body extends StatelessWidget {
  const _Body(this.text, this.style);

  final String text;
  final CoverStyle style;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 8),
    child: Text(
      text,
      textAlign: TextAlign.center,
      style: style.body(15, lineHeight: 21, color: style.muted),
    ),
  );
}

class _InfoRow extends StatelessWidget {
  const _InfoRow(this.label, this.value, this.style, {required this.monospace});

  final String label;
  final String value;
  final CoverStyle style;
  final bool monospace;

  @override
  Widget build(BuildContext context) => Row(
    children: [
      Text(label, style: TextStyle(fontSize: 13, color: style.muted)),
      const SizedBox(width: 12),
      Expanded(
        child: Text(
          value,
          textAlign: TextAlign.end,
          style: monospace
              ? mono(13, weight: FontWeight.w500, color: style.text)
              : style.body(13, weight: FontWeight.w500, color: style.text),
        ),
      ),
    ],
  );
}

LocalText _successTitle(CoverId cover) => switch (cover) {
  CoverId.furniture => (l) => l.resultSuccessFurniture,
  CoverId.coffee => (l) => l.resultSuccessCoffee,
  CoverId.tickets => (l) => l.resultSuccessTickets,
  CoverId.invest => (l) => l.investOpTopUp,
};

LocalText _successBody(CoverId cover) => switch (cover) {
  CoverId.furniture => (l) => l.resultSuccessFurnitureBody,
  CoverId.coffee => (l) => l.resultSuccessCoffeeBody,
  CoverId.tickets => (l) => l.resultSuccessTicketsBody,
  CoverId.invest => (l) => l.doneOperationValue,
};
