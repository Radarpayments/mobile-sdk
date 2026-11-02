import 'package:flutter/material.dart';

import '../app.dart';
import '../app_scope.dart';
import '../checkout/checkout.dart';
import '../merchant/order_registrar.dart';
import 'model/cart.dart';
import 'model/catalog.dart';
import 'scenario_sheet.dart';
import 'start_screen.dart';
import 'theme/cover_style.dart';

/// The bottom of every cart in every cover: the demo-panel row, the "Pay" button and a registration
/// error, if any. Registers the order for [amountMinor] and opens the SDK sheet in the cover's brand;
/// with [autoPay] it does so as soon as it is shown.
class StoreCheckoutBar extends StatefulWidget {
  const StoreCheckoutBar({
    super.key,
    required this.cover,
    required this.amountMinor,
    required this.onOutcome,
    required this.onOpenSettings,
    this.autoPay = false,
    this.onAutoPayStarted,
  });

  final CoverId cover;
  final int amountMinor;
  final OnOutcome onOutcome;
  final VoidCallback onOpenSettings;
  final bool autoPay;
  final VoidCallback? onAutoPayStarted;

  @override
  State<StoreCheckoutBar> createState() => _StoreCheckoutBarState();
}

class _StoreCheckoutBarState extends State<StoreCheckoutBar> {
  late final StoreCheckout _checkout;

  @override
  void initState() {
    super.initState();
    final scope = AppScope.of(context);
    _checkout = StoreCheckout(onMdOrder: scope.prefs.setLastMdOrder, present: scope.presentSheet);
    // "Order again": go straight to the payment sheet once the screen is up.
    if (widget.autoPay && widget.amountMinor > 0) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        widget.onAutoPayStarted?.call();
        _pay();
      });
    }
  }

  @override
  void dispose() {
    _checkout.dispose();
    super.dispose();
  }

  Future<void> _pay() async {
    final scope = AppScope.of(context);
    final prefs = scope.prefs;
    final amountMinor = widget.amountMinor;
    final scenario = prefs.scenario;
    final outcome = await _checkout.pay(
      registrar: scope.registrarFactory(prefs),
      config: buildSdkConfig(
        baseUrl: prefs.baseUrl,
        scenario: scenario,
        cover: coverStyle(widget.cover),
        themeOverride: prefs.sheetTheme,
        locale: prefs.localeOverride,
        storeName: coverName(widget.cover)(context.l10n),
        googlePayMerchantId: prefs.googlePayMerchantId,
        googlePayGateway: prefs.googlePayGateway,
        applePayMerchantId: prefs.applePayMerchantId,
      ),
      amountMinor: amountMinor,
      clientId: scenario.buyerWithCards ? prefs.clientId : null,
    );
    if (outcome != null) widget.onOutcome(outcome, amountMinor);
  }

  @override
  Widget build(BuildContext context) {
    final prefs = context.prefs;
    return ListenableBuilder(
      listenable: Listenable.merge([_checkout, prefs]),
      builder: (context, _) => Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (_checkout.state case CheckoutRegistrationFailed(:final reason)) ...[
            RegistrationError(cover: widget.cover, reason: reason, onOpenSettings: widget.onOpenSettings),
            const SizedBox(height: 10),
          ],
          ScenarioRow(
            cover: widget.cover,
            buyerWithCards: prefs.scenario.buyerWithCards,
            onTap: () => showScenarioSheet(context),
          ),
          const SizedBox(height: 10),
          PayButton(
            key: const Key('store_pay'),
            cover: widget.cover,
            label: context.l10n.storePay(context.eur(widget.amountMinor)),
            busy: _checkout.busy,
            enabled: widget.amountMinor > 0,
            onPressed: _pay,
          ),
        ],
      ),
    );
  }
}

/// Dashed row above the "Pay" button that opens the demo panel.
class ScenarioRow extends StatelessWidget {
  const ScenarioRow({super.key, required this.cover, required this.buyerWithCards, required this.onTap});

  final CoverId cover;
  final bool buyerWithCards;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(cover);
    final l10n = context.l10n;
    final buyer = buyerWithCards ? l10n.scenarioWithCards : l10n.scenarioGuest;
    final shape = BorderRadius.circular(12);
    return CustomPaint(
      painter: _DashedBorder(color: style.border, radius: 12),
      child: Material(
        type: MaterialType.transparency,
        child: InkWell(
          key: const Key('store_scenario'),
          borderRadius: shape,
          onTap: onTap,
          child: ConstrainedBox(
            constraints: const BoxConstraints(minHeight: 48),
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 14),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      l10n.scenarioRow(buyer),
                      style: style.body(13, weight: FontWeight.w500, color: style.muted),
                    ),
                  ),
                  Icon(Icons.keyboard_arrow_down, color: style.muted, size: 18),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _DashedBorder extends CustomPainter {
  const _DashedBorder({required this.color, required this.radius});

  final Color color;
  final double radius;

  static const _dash = 5.0;
  static const _gap = 4.0;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1;
    final outline = Path()..addRRect(RRect.fromRectAndRadius(Offset.zero & size, Radius.circular(radius)).deflate(0.5));
    for (final metric in outline.computeMetrics()) {
      for (var at = 0.0; at < metric.length; at += _dash + _gap) {
        canvas.drawPath(metric.extractPath(at, at + _dash), paint);
      }
    }
  }

  @override
  bool shouldRepaint(_DashedBorder oldDelegate) => oldDelegate.color != color || oldDelegate.radius != radius;
}

/// The store's own "Pay" button in the cover's brand; shows progress while the order is registered.
class PayButton extends StatelessWidget {
  const PayButton({
    super.key,
    required this.cover,
    required this.label,
    required this.busy,
    required this.enabled,
    required this.onPressed,
  });

  final CoverId cover;
  final String label;
  final bool busy;
  final bool enabled;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(cover);
    return FilledButton(
      onPressed: enabled && !busy ? onPressed : null,
      style: FilledButton.styleFrom(
        backgroundColor: style.primary,
        foregroundColor: style.onPrimary,
        disabledBackgroundColor: style.primary.withValues(alpha: busy ? 1 : 0.4),
        disabledForegroundColor: style.onPrimary.withValues(alpha: busy ? 1 : 0.7),
        minimumSize: const Size.fromHeight(52),
        shape: RoundedRectangleBorder(borderRadius: style.buttonRadius),
      ),
      child: busy
          ? SizedBox.square(dimension: 20, child: CircularProgressIndicator(color: style.onPrimary, strokeWidth: 2))
          : Text(label, style: style.body(15, weight: FontWeight.w700)),
    );
  }
}

/// Why the order was not created, with a way to the developer settings.
class RegistrationError extends StatelessWidget {
  const RegistrationError({super.key, required this.cover, required this.reason, required this.onOpenSettings});

  final CoverId cover;
  final OrderRegistration reason;
  final VoidCallback onOpenSettings;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(cover);
    final l10n = context.l10n;
    final text = switch (reason) {
      RegistrationFailed(:final message) => l10n.storeErrorFailed(message),
      MissingApiKey() => l10n.storeErrorApiKey,
      _ => l10n.storeErrorCredentials,
    };
    return DecoratedBox(
      decoration: BoxDecoration(color: style.surface, borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsetsDirectional.only(start: 14, end: 4, top: 10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(text, style: style.body(13, lineHeight: 18, color: style.text)),
            TextButton(
              onPressed: onOpenSettings,
              style: TextButton.styleFrom(padding: EdgeInsets.zero),
              child: Text(
                l10n.storeOpenSettings,
                style: style.body(13, weight: FontWeight.w700, color: style.dark ? style.primary : style.accent),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
