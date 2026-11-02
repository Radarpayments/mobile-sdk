import 'package:flutter/material.dart';

import '../../app.dart';
import '../../app_scope.dart';
import '../model/cart.dart';
import '../model/catalog.dart';
import '../store_checkout_bar.dart';
import '../theme/cover_style.dart';

/// The order: items with quantities, where to pick it up, and the shared payment bar.
class CoffeeOrderScreen extends StatelessWidget {
  const CoffeeOrderScreen({
    super.key,
    required this.cart,
    required this.autoPay,
    required this.onAutoPayStarted,
    required this.onBack,
    required this.onQuantity,
    required this.onOutcome,
    required this.onOpenSettings,
  });

  final Cart cart;
  final bool autoPay;
  final VoidCallback onAutoPayStarted;
  final VoidCallback onBack;
  final void Function(String productId, int quantity) onQuantity;
  final OnOutcome onOutcome;
  final VoidCallback onOpenSettings;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.coffee);
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
                    Expanded(
                      child: Text(l10n.coffeeOrderTitle, style: style.heading(24, weight: FontWeight.w800)),
                    ),
                  ],
                ),
              ),
              Expanded(
                child: ListView(
                  children: [
                    for (final line in cart.lines) _OrderLine(line: line, onQuantity: onQuantity),
                    Container(
                      margin: const EdgeInsets.only(top: 12),
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: style.surface,
                        borderRadius: BorderRadius.circular(style.radius),
                      ),
                      child: Row(
                        children: [
                          Text(l10n.coffeePickup, style: style.body(14, color: style.muted)),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              l10n.coffeePickupValue,
                              style: style.body(14, weight: FontWeight.w700, color: style.text),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 12),
                child: StoreCheckoutBar(
                  cover: CoverId.coffee,
                  amountMinor: cart.totalMinor,
                  onOutcome: onOutcome,
                  onOpenSettings: onOpenSettings,
                  autoPay: autoPay,
                  onAutoPayStarted: onAutoPayStarted,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _OrderLine extends StatelessWidget {
  const _OrderLine({required this.line, required this.onQuantity});

  final CartLine line;
  final void Function(String productId, int quantity) onQuantity;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.coffee);
    final l10n = context.l10n;
    final product = line.product;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          if (product.image case final image?)
            ClipRRect(
              borderRadius: BorderRadius.circular(14),
              child: Image.asset(image, width: 56, height: 56, fit: BoxFit.cover),
            ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    product.name(l10n),
                    style: style.body(15, weight: FontWeight.w700, color: style.text),
                  ),
                  Text(context.eur(product.priceMinor * line.quantity), style: style.body(13, color: style.muted)),
                ],
              ),
            ),
          ),
          IconButton(
            onPressed: () => onQuantity(product.id, line.quantity - 1),
            tooltip: l10n.storeDecrease,
            icon: Icon(Icons.remove, color: style.text),
          ),
          Text(
            '${line.quantity}',
            style: style.body(14, weight: FontWeight.w700, color: style.text),
          ),
          IconButton(
            onPressed: () => onQuantity(product.id, line.quantity + 1),
            tooltip: l10n.storeIncrease,
            icon: Icon(Icons.add, color: style.text),
          ),
        ],
      ),
    );
  }
}
