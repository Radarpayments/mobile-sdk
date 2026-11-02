import 'package:flutter/material.dart';

import '../theme/cover_style.dart';

/// Northwind Invest's tokens: dark OLED navy with green for growth (ui-ux-pro-max "fintech investing").
abstract final class InvestColors {
  static const background = Color(0xFF020617);
  static const surface = Color(0xFF0B1222);
  static const surface2 = Color(0xFF111A2E);
  static const divider = Color(0xFF1E293B);
  static const fieldBorder = Color(0xFF334155);
  static const text = Color(0xFFF8FAFC);
  static const muted = Color(0xFF94A3B8);
  static const accent = Color(0xFF22C55E);
  static const onAccent = Color(0xFF052E16);
  static const accentSoft = Color(0x1F22C55E);
  static const error = Color(0xFFF87171);
  static const errorSoft = Color(0x1FF87171);
  static const glb = Color(0xFF1E3A8A);
  static const onGlb = Color(0xFFBFDBFE);
  static const tch = Color(0xFF4C1D95);
  static const onTch = Color(0xFFDDD6FE);
}

/// Northwind's text: IBM Plex Sans; amounts with tabular figures so they do not jump while counting.
TextStyle plex(
  double size, {
  FontWeight weight = FontWeight.w400,
  Color color = InvestColors.text,
  bool tabular = false,
}) {
  final style = font(StoreFonts.plex, size, weight: weight, color: color);
  return tabular ? style.copyWith(fontFeatures: const [FontFeature.tabularFigures()]) : style;
}
