import 'package:flutter/material.dart';

import '../../app.dart';
import '../../app_scope.dart';
import '../model/cart.dart';
import '../model/catalog.dart';
import '../store_checkout_bar.dart';
import '../theme/cover_style.dart';

/// The cart: lines with a quantity stepper, totals, then the shared scenario row and "Pay".
class FurnitureCartScreen extends StatelessWidget {
  const FurnitureCartScreen({
    super.key,
    required this.cart,
    required this.onBack,
    required this.onQuantity,
    required this.onOutcome,
    required this.onOpenSettings,
  });

  final Cart cart;
  final VoidCallback onBack;
  final void Function(String productId, int quantity) onQuantity;
  final OnOutcome onOutcome;
  final VoidCallback onOpenSettings;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.furniture);
    final l10n = context.l10n;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Row(
                  children: [
                    BackButton(onPressed: onBack, color: style.text),
                    Expanded(child: Text(l10n.storeCart, style: style.heading(26))),
                  ],
                ),
              ),
              Expanded(
                child: ListView(
                  children: [
                    for (final line in cart.lines) ...[
                      _CartRow(line: line, onQuantity: onQuantity),
                      Divider(height: 1, color: style.border.withValues(alpha: 0.6)),
                    ],
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      child: Column(
                        children: [
                          _SumRow(l10n.storeItems, context.eur(cart.totalMinor), bold: false),
                          const SizedBox(height: 6),
                          _SumRow(l10n.furnitureDeliveryLabel, l10n.furnitureDeliveryFree, bold: false),
                          const SizedBox(height: 6),
                          _SumRow(l10n.storeTotal, context.eur(cart.totalMinor), bold: true),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 12),
                child: StoreCheckoutBar(
                  cover: CoverId.furniture,
                  amountMinor: cart.totalMinor,
                  onOutcome: onOutcome,
                  onOpenSettings: onOpenSettings,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _CartRow extends StatelessWidget {
  const _CartRow({required this.line, required this.onQuantity});

  final CartLine line;
  final void Function(String productId, int quantity) onQuantity;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.furniture);
    final l10n = context.l10n;
    final product = line.product;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Row(
        children: [
          if (product.image case final image?)
            ClipRRect(
              borderRadius: BorderRadius.circular(10),
              child: Image.asset(image, width: 64, height: 64, fit: BoxFit.cover),
            ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    product.name(l10n),
                    style: style.body(14, weight: FontWeight.w600, color: style.text),
                  ),
                  if (product.subtitle case final subtitle?)
                    Text(
                      subtitle(l10n),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: style.body(12, color: style.muted),
                    ),
                ],
              ),
            ),
          ),
          DecoratedBox(
            decoration: BoxDecoration(
              border: Border.all(color: style.border),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Row(
              children: [
                IconButton(
                  onPressed: () => onQuantity(product.id, line.quantity - 1),
                  tooltip: l10n.storeDecrease,
                  iconSize: 16,
                  icon: Icon(Icons.remove, color: style.text),
                ),
                Text(
                  '${line.quantity}',
                  style: style.body(14, weight: FontWeight.w600, color: style.text),
                ),
                IconButton(
                  onPressed: () => onQuantity(product.id, line.quantity + 1),
                  tooltip: l10n.storeIncrease,
                  iconSize: 16,
                  icon: Icon(Icons.add, color: style.text),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _SumRow extends StatelessWidget {
  const _SumRow(this.label, this.value, {required this.bold});

  final String label;
  final String value;
  final bool bold;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.furniture);
    final text = style.body(
      bold ? 16 : 14,
      weight: bold ? FontWeight.w700 : FontWeight.w400,
      color: bold ? style.text : style.muted,
    );
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: text),
        Text(value, style: text),
      ],
    );
  }
}
