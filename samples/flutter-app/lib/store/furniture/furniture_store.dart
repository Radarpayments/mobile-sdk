import 'package:flutter/material.dart';

import '../../app.dart';
import '../../app_scope.dart';
import '../model/catalog.dart';
import '../theme/cover_style.dart';
import 'furniture_cart_screen.dart';
import 'furniture_catalog_screen.dart';
import 'furniture_category.dart';
import 'furniture_product_screen.dart';

/// Hearth & Co: catalogue → product → cart → the shared payment bar.
class FurnitureStore extends StatefulWidget {
  const FurnitureStore({super.key, required this.onOutcome, required this.onOpenSettings});

  final OnOutcome onOutcome;
  final VoidCallback onOpenSettings;

  @override
  State<FurnitureStore> createState() => _FurnitureStoreState();
}

class _FurnitureStoreState extends State<FurnitureStore> {
  static const _fade = Duration(milliseconds: 240);

  bool _cartOpen = false;
  FurnitureCategory _category = FurnitureCategory.all;

  /// The product page is a route of its own, so the photo flies between the tile and the page.
  void _openProduct(Product product) => Navigator.of(context).push(
    PageRouteBuilder<void>(
      transitionDuration: _fade,
      reverseTransitionDuration: _fade,
      pageBuilder: (context, _, _) => CoverTheme(
        id: CoverId.furniture,
        child: FurnitureProductScreen(
          product: product,
          onBack: () => Navigator.of(context).pop(),
          onAdd: (product) {
            this.context.store.add(CoverId.furniture, product);
            Navigator.of(context).pop();
          },
        ),
      ),
      transitionsBuilder: (_, animation, _, child) => FadeTransition(opacity: animation, child: child),
    ),
  );

  @override
  Widget build(BuildContext context) {
    final store = context.store;
    return ListenableBuilder(
      listenable: store,
      builder: (context, _) {
        final cart = store.cart(CoverId.furniture);
        // An empty cart is never shown: after a paid order the buyer is back at the catalogue.
        final showCart = _cartOpen && !cart.isEmpty;
        return PopScope(
          canPop: !showCart,
          onPopInvokedWithResult: (didPop, _) {
            if (!didPop) setState(() => _cartOpen = false);
          },
          child: CoverTheme(
            id: CoverId.furniture,
            child: AnimatedSwitcher(
              duration: _fade,
              child: showCart
                  ? FurnitureCartScreen(
                      key: const ValueKey('cart'),
                      cart: cart,
                      onBack: () => setState(() => _cartOpen = false),
                      onQuantity: (id, quantity) => store.setQuantity(CoverId.furniture, id, quantity),
                      onOutcome: widget.onOutcome,
                      onOpenSettings: widget.onOpenSettings,
                    )
                  : FurnitureCatalogScreen(
                      key: const ValueKey('catalog'),
                      cart: cart,
                      category: _category,
                      onCategory: (category) => setState(() => _category = category),
                      onBack: () => Navigator.of(context).maybePop(),
                      onProduct: _openProduct,
                      onCart: () => setState(() => _cartOpen = true),
                    ),
            ),
          ),
        );
      },
    );
  }
}
