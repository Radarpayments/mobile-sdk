import 'package:flutter/material.dart';

import '../../app_scope.dart';
import '../model/cart.dart';
import '../model/catalog.dart';
import '../theme/cover_style.dart';

/// One product: a large photo, the name, colours, delivery terms and "Add to cart".
class FurnitureProductScreen extends StatelessWidget {
  const FurnitureProductScreen({super.key, required this.product, required this.onBack, required this.onAdd});

  final Product product;
  final VoidCallback onBack;
  final ValueChanged<Product> onAdd;

  static const _photoShare = 0.46;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.furniture);
    final l10n = context.l10n;
    return Scaffold(
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SizedBox(
            height: MediaQuery.sizeOf(context).height * _photoShare,
            child: Stack(
              fit: StackFit.expand,
              children: [
                if (product.image case final image?)
                  Hero(
                    tag: 'photo-${product.id}',
                    child: Image.asset(image, fit: BoxFit.cover),
                  ),
                SafeArea(
                  child: Align(
                    alignment: AlignmentDirectional.topStart,
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: IconButton.filled(
                        onPressed: onBack,
                        tooltip: l10n.storeBack,
                        style: IconButton.styleFrom(backgroundColor: Colors.white.withValues(alpha: 0.92)),
                        icon: Icon(Icons.chevron_left, color: style.text),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: SafeArea(
              top: false,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(product.name(l10n), style: style.heading(32, weight: FontWeight.w600, lineHeight: 34)),
                    if (product.subtitle case final subtitle?)
                      Text(subtitle(l10n), style: style.body(14, color: style.muted)),
                    Padding(
                      padding: const EdgeInsets.only(top: 16, bottom: 8),
                      child: Text(
                        l10n.furnitureColours,
                        style: style.body(13, weight: FontWeight.w600, color: style.text),
                      ),
                    ),
                    Row(
                      children: [
                        for (final (index, colour) in [
                          const Color(0xFF1C1917),
                          const Color(0xFFE7E5E4),
                          style.accent,
                        ].indexed)
                          Container(
                            width: 28,
                            height: 28,
                            margin: const EdgeInsetsDirectional.only(end: 10),
                            padding: const EdgeInsets.all(4),
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              border: Border.all(
                                color: index == 0 ? style.text : style.border,
                                width: index == 0 ? 2 : 1,
                              ),
                            ),
                            child: DecoratedBox(
                              decoration: BoxDecoration(color: colour, shape: BoxShape.circle),
                            ),
                          ),
                      ],
                    ),
                    Padding(
                      padding: const EdgeInsets.only(top: 16),
                      child: Text(l10n.furnitureDelivery, style: style.body(14, lineHeight: 20, color: style.muted)),
                    ),
                    const Spacer(),
                    FilledButton(
                      onPressed: () => onAdd(product),
                      style: FilledButton.styleFrom(
                        backgroundColor: style.primary,
                        foregroundColor: style.onPrimary,
                        minimumSize: const Size.fromHeight(52),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(style.radius)),
                      ),
                      child: Text(
                        l10n.storeAddToCart(context.eur(product.priceMinor)),
                        style: style.body(15, weight: FontWeight.w600),
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
