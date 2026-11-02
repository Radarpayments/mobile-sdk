import 'package:flutter/material.dart';

import '../../app_scope.dart';
import '../model/cart.dart';
import '../model/catalog.dart';
import '../theme/cover_style.dart';
import 'furniture_category.dart';

/// Catalogue: brand header with the cart, collection photo, category chips and a product grid.
class FurnitureCatalogScreen extends StatelessWidget {
  const FurnitureCatalogScreen({
    super.key,
    required this.cart,
    required this.category,
    required this.onCategory,
    required this.onBack,
    required this.onProduct,
    required this.onCart,
  });

  final Cart cart;
  final FurnitureCategory category;
  final ValueChanged<FurnitureCategory> onCategory;
  final VoidCallback onBack;
  final ValueChanged<Product> onProduct;
  final VoidCallback onCart;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.furniture);
    final products = furnitureFilter(category);
    return Scaffold(
      body: SafeArea(
        child: Stack(
          children: [
            ListView(
              // Back from the cart the grid is where it was.
              key: const PageStorageKey('furniture-catalog'),
              padding: EdgeInsets.fromLTRB(16, 0, 16, cart.isEmpty ? 16 : 88),
              children: [
                _Header(count: cart.count, onBack: onBack, onCart: onCart),
                const SizedBox(height: 16),
                const _Hero(),
                const SizedBox(height: 16),
                _Chips(selected: category, onSelect: onCategory),
                for (var i = 0; i < products.length; i += 2)
                  Padding(
                    padding: const EdgeInsets.only(top: 16),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: _Tile(product: products[i], onTap: onProduct),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: i + 1 < products.length
                              ? _Tile(product: products[i + 1], onTap: onProduct)
                              : const SizedBox.shrink(),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
            if (!cart.isEmpty)
              Positioned(
                left: 16,
                right: 16,
                bottom: 16,
                child: Material(
                  color: style.primary,
                  borderRadius: BorderRadius.circular(style.radius),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(style.radius),
                    onTap: onCart,
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(minHeight: 56),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 18),
                        child: Row(
                          children: [
                            Expanded(
                              child: Text(
                                context.l10n.storeCartCount(cart.count),
                                style: style.body(15, weight: FontWeight.w600, color: style.onPrimary),
                              ),
                            ),
                            Text(
                              context.eur(cart.totalMinor),
                              style: style.body(15, weight: FontWeight.w600, color: style.onPrimary),
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

class _Tile extends StatelessWidget {
  const _Tile({required this.product, required this.onTap});

  final Product product;
  final ValueChanged<Product> onTap;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.furniture);
    return InkWell(
      borderRadius: BorderRadius.circular(style.radius),
      onTap: () => onTap(product),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (product.image case final image?)
            Hero(
              tag: 'photo-${product.id}',
              child: ClipRRect(
                borderRadius: BorderRadius.circular(style.radius),
                child: AspectRatio(aspectRatio: 1, child: Image.asset(image, fit: BoxFit.cover)),
              ),
            ),
          const SizedBox(height: 8),
          Text(
            product.name(context.l10n),
            style: style.body(14, weight: FontWeight.w600, color: style.text),
          ),
          Text(context.eur(product.priceMinor), style: style.body(13, color: style.muted)),
        ],
      ),
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.count, required this.onBack, required this.onCart});

  final int count;
  final VoidCallback onBack;
  final VoidCallback onCart;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.furniture);
    final l10n = context.l10n;
    return Padding(
      padding: const EdgeInsets.only(top: 4),
      child: Row(
        children: [
          Transform.translate(
            offset: Offset(Directionality.of(context) == TextDirection.ltr ? -12 : 12, 0),
            child: BackButton(onPressed: onBack, color: style.text),
          ),
          Expanded(child: Text(l10n.coverFurnitureName, style: style.heading(26))),
          Semantics(
            button: true,
            label: l10n.storeCartCount(count),
            excludeSemantics: true,
            child: InkResponse(
              onTap: onCart,
              radius: 24,
              child: SizedBox.square(
                dimension: 48,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(color: style.border),
                      ),
                      child: Icon(Icons.shopping_basket_outlined, color: style.text, size: 20),
                    ),
                    if (count > 0)
                      PositionedDirectional(
                        top: 0,
                        end: 0,
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6),
                          decoration: BoxDecoration(color: style.accent, borderRadius: BorderRadius.circular(999)),
                          child: Text(
                            '$count',
                            style: style.body(11, weight: FontWeight.w700, color: Colors.white),
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Hero extends StatelessWidget {
  const _Hero();

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.furniture);
    return ClipRRect(
      borderRadius: BorderRadius.circular(16),
      child: SizedBox(
        height: 184,
        child: Stack(
          fit: StackFit.expand,
          children: [
            Image.asset(photo('furniture_hero'), fit: BoxFit.cover),
            DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  stops: const [0.45, 1],
                  colors: [Colors.transparent, Colors.black.withValues(alpha: 0.5)],
                ),
              ),
            ),
            PositionedDirectional(
              start: 14,
              bottom: 14,
              child: Text(
                context.l10n.furnitureCollection,
                style: style.heading(26, weight: FontWeight.w600, color: Colors.white, lineHeight: 26),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Chips extends StatelessWidget {
  const _Chips({required this.selected, required this.onSelect});

  final FurnitureCategory selected;
  final ValueChanged<FurnitureCategory> onSelect;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.furniture);
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (final category in FurnitureCategory.values) ...[
            if (category != FurnitureCategory.values.first) const SizedBox(width: 8),
            Semantics(
              selected: category == selected,
              child: GestureDetector(
                onTap: () => onSelect(category),
                child: Container(
                  constraints: const BoxConstraints(minHeight: 40),
                  alignment: Alignment.center,
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                  decoration: BoxDecoration(
                    color: category == selected ? style.primary : Colors.transparent,
                    border: Border.all(color: category == selected ? style.primary : style.border),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    category.label(context.l10n),
                    style: style.body(
                      13,
                      weight: FontWeight.w600,
                      color: category == selected ? style.onPrimary : style.text,
                    ),
                  ),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
