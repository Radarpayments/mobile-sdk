import 'package:flutter/material.dart';

import '../../app.dart';
import '../../app_scope.dart';
import '../model/catalog.dart';
import '../theme/cover_style.dart';
import 'coffee_menu_screen.dart';
import 'coffee_order_screen.dart';

/// Roast Lane: menu → order → the shared payment bar; "Order again" pays the last order right away.
class CoffeeStore extends StatefulWidget {
  const CoffeeStore({super.key, required this.onOutcome, required this.onOpenSettings});

  final OnOutcome onOutcome;
  final VoidCallback onOpenSettings;

  @override
  State<CoffeeStore> createState() => _CoffeeStoreState();
}

class _CoffeeStoreState extends State<CoffeeStore> {
  bool _orderOpen = false;
  bool _payNow = false;

  @override
  Widget build(BuildContext context) {
    final store = context.store;
    return ListenableBuilder(
      listenable: store,
      builder: (context, _) {
        final cart = store.cart(CoverId.coffee);
        // An empty order is never shown: after a paid order the buyer is back at the menu, with "Order again".
        final showOrder = _orderOpen && !cart.isEmpty;
        return PopScope(
          canPop: !showOrder,
          onPopInvokedWithResult: (didPop, _) {
            if (!didPop) setState(() => _orderOpen = false);
          },
          child: CoverTheme(
            id: CoverId.coffee,
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 180),
              child: showOrder
                  ? CoffeeOrderScreen(
                      key: const ValueKey('order'),
                      cart: cart,
                      autoPay: _payNow,
                      onAutoPayStarted: () => _payNow = false,
                      onBack: () => setState(() => _orderOpen = false),
                      onQuantity: (id, quantity) => store.setQuantity(CoverId.coffee, id, quantity),
                      onOutcome: widget.onOutcome,
                      onOpenSettings: widget.onOpenSettings,
                    )
                  : CoffeeMenuScreen(
                      key: const ValueKey('menu'),
                      cart: cart,
                      lastPaid: store.lastPaid(CoverId.coffee),
                      onBack: () => Navigator.of(context).maybePop(),
                      onAdd: (product) => store.add(CoverId.coffee, product),
                      onRepeat: () {
                        setState(() {
                          _payNow = true;
                          _orderOpen = true;
                        });
                        store.repeatLast(CoverId.coffee);
                      },
                      onOrder: () => setState(() => _orderOpen = true),
                    ),
            ),
          ),
        );
      },
    );
  }
}
