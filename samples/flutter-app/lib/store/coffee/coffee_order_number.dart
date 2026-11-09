import 'package:flutter/material.dart';

import '../../app_scope.dart';
import '../model/catalog.dart';
import '../theme/cover_style.dart';

/// The coffee shop's success headline: the order number in large type, to show the barista.
class CoffeeOrderNumber extends StatelessWidget {
  const CoffeeOrderNumber({super.key, required this.number});

  final int number;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(CoverId.coffee);
    return Column(
      children: [
        Text(
          context.l10n.resultSuccessCoffee,
          style: style.body(17, weight: FontWeight.w700, color: style.muted),
        ),
        Text('$number', style: style.heading(88, weight: FontWeight.w800, lineHeight: 96)),
      ],
    );
  }
}
