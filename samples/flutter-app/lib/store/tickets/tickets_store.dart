import 'package:flutter/material.dart';

import '../../app.dart';
import '../../app_scope.dart';
import '../model/catalog.dart';
import '../theme/cover_style.dart';
import 'ticket_order.dart';
import 'tickets_checkout_screen.dart';
import 'tickets_event_screen.dart';

/// Stage Pass: the event with seat categories → checkout with a seat hold → the shared payment bar.
class TicketsStore extends StatefulWidget {
  const TicketsStore({super.key, required this.onOutcome, required this.onOpenSettings});

  final OnOutcome onOutcome;
  final VoidCallback onOpenSettings;

  @override
  State<TicketsStore> createState() => _TicketsStoreState();
}

class _TicketsStoreState extends State<TicketsStore> {
  TicketOrder _order = const TicketOrder();
  bool _checkoutOpen = false;
  bool _holdExpired = false;

  @override
  Widget build(BuildContext context) {
    final store = context.store;
    return ListenableBuilder(
      listenable: store,
      builder: (context, _) {
        // After a paid order the cart is empty: the buyer is back at the event.
        final showCheckout = _checkoutOpen && !store.cart(CoverId.tickets).isEmpty;
        return PopScope(
          canPop: !showCheckout,
          onPopInvokedWithResult: (didPop, _) {
            if (!didPop) setState(() => _checkoutOpen = false);
          },
          child: CoverTheme(
            id: CoverId.tickets,
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 180),
              child: showCheckout
                  ? TicketsCheckoutScreen(
                      key: const ValueKey('checkout'),
                      order: _order,
                      onBack: () => setState(() => _checkoutOpen = false),
                      onHoldExpired: () => setState(() {
                        _holdExpired = true;
                        _checkoutOpen = false;
                      }),
                      onOutcome: widget.onOutcome,
                      onOpenSettings: widget.onOpenSettings,
                    )
                  : TicketsEventScreen(
                      key: const ValueKey('event'),
                      order: _order,
                      holdExpired: _holdExpired,
                      onBack: () => Navigator.of(context).maybePop(),
                      onCategory: (id) => setState(() => _order = _order.withCategory(id)),
                      onQuantity: (quantity) => setState(() => _order = _order.withQuantity(quantity)),
                      onNext: () {
                        setState(() {
                          _holdExpired = false;
                          _checkoutOpen = true;
                        });
                        store.replaceCart(CoverId.tickets, _order.toCart());
                      },
                    ),
            ),
          ),
        );
      },
    );
  }
}
