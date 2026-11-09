import 'package:flutter/material.dart';

import '../../app_scope.dart';
import '../model/cart.dart';
import '../model/catalog.dart';
import '../theme/cover_style.dart';
import 'ticket_order.dart';

/// Orange-700: the hold warning, 4.5:1 on white.
const holdColour = Color(0xFFC2410C);

/// The concert: a full-bleed poster, seat categories, how many seats and "Checkout".
class TicketsEventScreen extends StatelessWidget {
  const TicketsEventScreen({
    super.key,
    required this.order,
    required this.holdExpired,
    required this.onBack,
    required this.onCategory,
    required this.onQuantity,
    required this.onNext,
  });

  final TicketOrder order;
  final bool holdExpired;
  final VoidCallback onBack;
  final ValueChanged<String> onCategory;
  final ValueChanged<int> onQuantity;
  final VoidCallback onNext;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.tickets);
    final l10n = context.l10n;
    final seats = order.toCart().lines.first;
    // The poster runs under the status bar: light icons on the dark photo.
    return SystemBarIcons(
      lightBackground: false,
      child: Scaffold(
        body: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Expanded(
              child: ListView(
                padding: EdgeInsets.zero,
                children: [
                  _Poster(onBack: onBack),
                  if (holdExpired)
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                      child: Text(
                        l10n.ticketsHoldExpired,
                        style: style.body(14, weight: FontWeight.w600, color: holdColour),
                      ),
                    ),
                  Padding(
                    padding: const EdgeInsetsDirectional.only(start: 16, top: 16, bottom: 8),
                    child: Text(l10n.ticketsSection, style: style.heading(18, weight: FontWeight.w800)),
                  ),
                  for (final category in Catalog.products(CoverId.tickets))
                    if (category.id != ticketFeeId)
                      _CategoryRow(
                        category: category,
                        selected: category.id == order.categoryId,
                        onTap: () => onCategory(category.id),
                      ),
                ],
              ),
            ),
            SafeArea(
              top: false,
              child: Container(
                margin: const EdgeInsets.all(16),
                constraints: const BoxConstraints(minHeight: 56),
                padding: const EdgeInsetsDirectional.only(start: 6),
                decoration: BoxDecoration(color: style.primary, borderRadius: BorderRadius.circular(style.radius)),
                child: Row(
                  children: [
                    IconButton(
                      onPressed: () => onQuantity(order.quantity - 1),
                      tooltip: l10n.storeDecrease,
                      icon: Icon(Icons.remove, color: style.onPrimary),
                    ),
                    Text(
                      '${order.quantity}',
                      style: style.body(16, weight: FontWeight.w800, color: style.onPrimary),
                    ),
                    IconButton(
                      onPressed: () => onQuantity(order.quantity + 1),
                      tooltip: l10n.storeIncrease,
                      icon: Icon(Icons.add, color: style.onPrimary),
                    ),
                    Expanded(
                      child: Semantics(
                        button: true,
                        child: InkWell(
                          borderRadius: BorderRadius.circular(style.radius),
                          onTap: onNext,
                          child: Padding(
                            padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 12),
                            child: Text(
                              l10n.ticketsToPayment(context.eur(seats.product.priceMinor * seats.quantity)),
                              textAlign: TextAlign.end,
                              style: style.body(16, weight: FontWeight.w800, color: style.onPrimary),
                            ),
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
      ),
    );
  }
}

class _CategoryRow extends StatelessWidget {
  const _CategoryRow({required this.category, required this.selected, required this.onTap});

  final Product category;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.tickets);
    final l10n = context.l10n;
    final shape = BorderRadius.circular(style.radius);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 5),
      child: Semantics(
        inMutuallyExclusiveGroup: true,
        checked: selected,
        child: Material(
          color: selected ? style.surface : style.background,
          shape: RoundedRectangleBorder(
            borderRadius: shape,
            side: BorderSide(color: selected ? style.primary : style.border, width: selected ? 2 : 1),
          ),
          child: InkWell(
            borderRadius: shape,
            onTap: onTap,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          category.name(l10n),
                          style: style.body(16, weight: FontWeight.w600, color: style.text),
                        ),
                        if (category.subtitle case final subtitle?)
                          Text(subtitle(l10n), style: style.body(13, color: style.muted)),
                      ],
                    ),
                  ),
                  Text(
                    context.eur(category.priceMinor),
                    style: style.body(16, weight: FontWeight.w700, color: style.text),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Poster extends StatelessWidget {
  const _Poster({required this.onBack});

  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.tickets);
    final l10n = context.l10n;
    return SizedBox(
      height: 340,
      child: Stack(
        fit: StackFit.expand,
        children: [
          Image.asset(photo('tickets_hero'), fit: BoxFit.cover),
          DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [Colors.black.withValues(alpha: 0.25), Colors.black.withValues(alpha: 0.8)],
              ),
            ),
          ),
          SafeArea(
            child: Align(
              alignment: AlignmentDirectional.topStart,
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: IconButton.filled(
                  onPressed: onBack,
                  tooltip: l10n.storeBack,
                  style: IconButton.styleFrom(backgroundColor: Colors.black.withValues(alpha: 0.35)),
                  icon: const Icon(Icons.chevron_left, color: Colors.white),
                ),
              ),
            ),
          ),
          PositionedDirectional(
            start: 16,
            end: 16,
            bottom: 16,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  l10n.ticketsLive,
                  style: font(
                    style.accentFont!,
                    18,
                    weight: FontWeight.w600,
                    style: FontStyle.italic,
                    color: Colors.white,
                  ),
                ),
                Text(
                  l10n.ticketsTitle,
                  style: style.heading(40, weight: FontWeight.w800, color: Colors.white, lineHeight: 40),
                ),
                const SizedBox(height: 6),
                Text(l10n.ticketsDate, style: style.body(14, color: Colors.white.withValues(alpha: 0.9))),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
