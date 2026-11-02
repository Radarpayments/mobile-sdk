import 'package:flutter/material.dart';

import '../../app_scope.dart';
import '../model/cart.dart';
import '../model/catalog.dart';
import '../theme/cover_style.dart';

enum _CoffeeTab {
  coffee,
  cold,
  bakery;

  LocalText get label => switch (this) {
    coffee => (l) => l.coffeeTabCoffee,
    cold => (l) => l.coffeeTabCold,
    bakery => (l) => l.coffeeTabBakery,
  };

  static _CoffeeTab of(String productId) => switch (productId) {
    'iced_latte' => cold,
    'croissant' => bakery,
    _ => coffee,
  };
}

/// Menu: the last order to repeat in one tap, tabs, items with "+", and the order pill.
class CoffeeMenuScreen extends StatefulWidget {
  const CoffeeMenuScreen({
    super.key,
    required this.cart,
    required this.lastPaid,
    required this.onBack,
    required this.onAdd,
    required this.onRepeat,
    required this.onOrder,
  });

  final Cart cart;
  final Cart? lastPaid;
  final VoidCallback onBack;
  final ValueChanged<Product> onAdd;
  final VoidCallback onRepeat;
  final VoidCallback onOrder;

  @override
  State<CoffeeMenuScreen> createState() => _CoffeeMenuScreenState();
}

class _CoffeeMenuScreenState extends State<CoffeeMenuScreen> {
  _CoffeeTab _tab = _CoffeeTab.coffee;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.coffee);
    final l10n = context.l10n;
    final cart = widget.cart;
    final lastPaid = widget.lastPaid;
    return Scaffold(
      body: SafeArea(
        child: Stack(
          children: [
            ListView(
              padding: const EdgeInsets.only(bottom: 96),
              children: [
                Padding(
                  padding: const EdgeInsetsDirectional.only(start: 4, end: 16, top: 4),
                  child: Row(
                    children: [
                      BackButton(onPressed: widget.onBack, color: style.text),
                      Expanded(
                        child: Text(l10n.coverCoffeeName, style: style.heading(24, weight: FontWeight.w800)),
                      ),
                      Icon(Icons.place_outlined, color: style.muted, size: 16),
                      const SizedBox(width: 4),
                      Text(l10n.coffeeLocation, style: style.body(13, color: style.muted)),
                    ],
                  ),
                ),
                if (lastPaid != null && !lastPaid.isEmpty) _Again(last: lastPaid, onRepeat: widget.onRepeat),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  child: Row(
                    children: [
                      for (final tab in _CoffeeTab.values)
                        Padding(
                          padding: const EdgeInsetsDirectional.only(end: 20),
                          child: Semantics(
                            selected: tab == _tab,
                            child: GestureDetector(
                              onTap: () => setState(() => _tab = tab),
                              child: ConstrainedBox(
                                constraints: const BoxConstraints(minHeight: 44),
                                child: Column(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      tab.label(l10n),
                                      style: style.body(
                                        15,
                                        weight: FontWeight.w700,
                                        color: tab == _tab ? style.text : style.muted,
                                      ),
                                    ),
                                    Container(
                                      margin: const EdgeInsets.only(top: 4),
                                      width: 28,
                                      height: 2,
                                      color: tab == _tab ? style.primary : style.background,
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
                for (final product in Catalog.products(CoverId.coffee))
                  if (_CoffeeTab.of(product.id) == _tab) _MenuRow(product: product, onAdd: () => widget.onAdd(product)),
              ],
            ),
            if (!cart.isEmpty)
              Positioned(
                left: 16,
                right: 16,
                bottom: 16,
                child: Material(
                  color: style.primary,
                  shape: const StadiumBorder(),
                  child: InkWell(
                    customBorder: const StadiumBorder(),
                    onTap: widget.onOrder,
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(minHeight: 56),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 22),
                        child: Row(
                          children: [
                            Expanded(
                              child: Text(
                                l10n.storeCartCount(cart.count),
                                style: style.body(15, weight: FontWeight.w800, color: style.onPrimary),
                              ),
                            ),
                            Text(
                              context.eur(cart.totalMinor),
                              style: style.body(15, weight: FontWeight.w800, color: style.onPrimary),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _Again extends StatelessWidget {
  const _Again({required this.last, required this.onRepeat});

  final Cart last;
  final VoidCallback onRepeat;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.coffee);
    final l10n = context.l10n;
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      height: 132,
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(color: style.surface, borderRadius: BorderRadius.circular(style.radius)),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Image.asset(photo('coffee_flat_white'), width: 104, fit: BoxFit.cover),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(l10n.coffeeAgain, style: style.body(12, color: style.muted)),
                  Text(
                    last.lines.map((line) => line.product.name(l10n)).join(', '),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: style.body(15, weight: FontWeight.w700, color: style.text),
                  ),
                  Text(context.eur(last.totalMinor), style: style.body(13, color: style.muted)),
                  const SizedBox(height: 8),
                  Material(
                    color: style.primary,
                    borderRadius: BorderRadius.circular(12),
                    child: InkWell(
                      borderRadius: BorderRadius.circular(12),
                      onTap: onRepeat,
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
                        child: Text(
                          l10n.coffeeRepeat,
                          style: style.body(14, weight: FontWeight.w800, color: style.onPrimary),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _MenuRow extends StatelessWidget {
  const _MenuRow({required this.product, required this.onAdd});

  final Product product;
  final VoidCallback onAdd;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.coffee);
    final l10n = context.l10n;
    final name = product.name(l10n);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        children: [
          if (product.image case final image?)
            ClipRRect(
              borderRadius: BorderRadius.circular(16),
              child: Image.asset(image, width: 64, height: 64, fit: BoxFit.cover),
            ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    name,
                    style: style.body(15, weight: FontWeight.w700, color: style.text),
                  ),
                  if (product.subtitle case final subtitle?)
                    Text(
                      subtitle(l10n),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: style.body(13, color: style.muted),
                    ),
                  Text(context.eur(product.priceMinor), style: style.body(13, color: style.muted)),
                ],
              ),
            ),
          ),
          IconButton(
            onPressed: onAdd,
            tooltip: l10n.coffeeAddItem(name),
            icon: Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(color: style.primary, shape: BoxShape.circle),
              child: Icon(Icons.add, color: style.onPrimary, size: 18),
            ),
          ),
        ],
      ),
    );
  }
}
