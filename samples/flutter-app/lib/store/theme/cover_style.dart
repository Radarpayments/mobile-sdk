import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../invest/invest_colors.dart';
import '../model/catalog.dart';

/// The brand of a demo business: its colours, corner radius and fonts. The store screens use all of it;
/// the SDK sheet takes the colours and corners through `paymentTheme` in checkout.dart.
///
/// [fieldSurface] is the background of input fields and cards inside the SDK sheet; with [pillButtons]
/// buttons are fully rounded (the coffee shop) instead of using [radius].
class CoverStyle {
  const CoverStyle({
    required this.id,
    required this.dark,
    required this.primary,
    required this.onPrimary,
    required this.background,
    required this.surface,
    required this.fieldSurface,
    required this.text,
    required this.muted,
    required this.border,
    required this.accent,
    required this.radius,
    required this.pillButtons,
    required this.headingFont,
    required this.bodyFont,
    this.accentFont,
  });

  final CoverId id;
  final bool dark;
  final Color primary;
  final Color onPrimary;
  final Color background;
  final Color surface;
  final Color fieldSurface;
  final Color text;
  final Color muted;
  final Color border;
  final Color accent;
  final double radius;
  final bool pillButtons;
  final String headingFont;
  final String bodyFont;
  final String? accentFont;

  /// Corners of the store's buttons.
  BorderRadius get buttonRadius => BorderRadius.circular(pillButtons ? 999 : radius);

  /// Text in the store's body font.
  TextStyle body(double size, {FontWeight weight = FontWeight.w400, Color? color, double? lineHeight}) =>
      font(bodyFont, size, weight: weight, color: color, lineHeight: lineHeight);

  /// Text in the store's heading font.
  TextStyle heading(double size, {FontWeight weight = FontWeight.w700, Color? color, double? lineHeight}) =>
      font(headingFont, size, weight: weight, color: color ?? text, lineHeight: lineHeight);
}

/// Brand of [id], from the design tokens picked with the ui-ux-pro-max design system.
CoverStyle coverStyle(CoverId id) => switch (id) {
  CoverId.furniture => const CoverStyle(
    id: CoverId.furniture,
    dark: false,
    primary: Color(0xFF1C1917),
    onPrimary: Color(0xFFFFFFFF),
    background: Color(0xFFFAFAF9),
    surface: Color(0xFFFFFFFF),
    fieldSurface: Color(0xFFF5F5F4),
    text: Color(0xFF0C0A09),
    muted: Color(0xFF57534E),
    border: Color(0xFFD6D3D1),
    accent: Color(0xFFA16207),
    radius: 12,
    pillButtons: false,
    headingFont: StoreFonts.cormorant,
    bodyFont: StoreFonts.montserrat,
  ),
  CoverId.coffee => const CoverStyle(
    id: CoverId.coffee,
    dark: true,
    primary: Color(0xFFF3D9B1),
    onPrimary: Color(0xFF17110E),
    background: Color(0xFF17110E),
    surface: Color(0xFF261C17),
    fieldSurface: Color(0xFF2E231E),
    text: Color(0xFFFEF3C7),
    muted: Color(0xFFD6C3A5),
    border: Color(0xFF5B4A40),
    accent: Color(0xFFF3D9B1),
    radius: 18,
    pillButtons: true,
    headingFont: StoreFonts.nunito,
    bodyFont: StoreFonts.nunito,
  ),
  CoverId.tickets => const CoverStyle(
    id: CoverId.tickets,
    dark: false,
    primary: Color(0xFFEA580C),
    onPrimary: Color(0xFF000000),
    background: Color(0xFFFFFFFF),
    surface: Color(0xFFFFF7ED),
    fieldSurface: Color(0xFFF8FAFC),
    text: Color(0xFF0F172A),
    muted: Color(0xFF64748B),
    border: Color(0xFFE2E8F0),
    accent: Color(0xFFEA580C),
    radius: 14,
    pillButtons: false,
    headingFont: StoreFonts.inter,
    bodyFont: StoreFonts.inter,
    accentFont: StoreFonts.playfair,
  ),
  CoverId.invest => const CoverStyle(
    id: CoverId.invest,
    dark: true,
    primary: InvestColors.accent,
    onPrimary: InvestColors.onAccent,
    background: InvestColors.surface,
    surface: InvestColors.surface,
    fieldSurface: InvestColors.surface2,
    text: InvestColors.text,
    muted: InvestColors.muted,
    border: InvestColors.fieldBorder,
    accent: InvestColors.accent,
    radius: 12,
    pillButtons: false,
    headingFont: StoreFonts.plex,
    bodyFont: StoreFonts.plex,
  ),
};

/// The store screens of cover [id]: Material colours, the body font and system bar icons.
class CoverTheme extends StatelessWidget {
  const CoverTheme({super.key, required this.id, required this.child});

  final CoverId id;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final style = coverStyle(id);
    final scheme =
        ColorScheme.fromSeed(
          seedColor: style.primary,
          brightness: style.dark ? Brightness.dark : Brightness.light,
        ).copyWith(
          primary: style.primary,
          onPrimary: style.onPrimary,
          surface: style.background,
          onSurface: style.text,
          surfaceContainerHighest: style.surface,
          onSurfaceVariant: style.muted,
          outline: style.border,
        );
    return Theme(
      data: ThemeData(colorScheme: scheme, fontFamily: style.bodyFont, scaffoldBackgroundColor: style.background),
      child: SystemBarIcons(lightBackground: !style.dark, child: child),
    );
  }
}

/// Dark status and navigation bar icons on a light screen, light icons on a dark one.
class SystemBarIcons extends StatelessWidget {
  const SystemBarIcons({super.key, required this.lightBackground, required this.child});

  final bool lightBackground;
  final Widget child;

  @override
  Widget build(BuildContext context) => AnnotatedRegion<SystemUiOverlayStyle>(
    value: (lightBackground ? SystemUiOverlayStyle.dark : SystemUiOverlayStyle.light).copyWith(
      statusBarColor: Colors.transparent,
      systemNavigationBarColor: Colors.transparent,
    ),
    child: child,
  );
}

/// Variable Google Fonts (OFL), declared in pubspec.yaml.
abstract final class StoreFonts {
  static const cormorant = 'Cormorant';
  static const montserrat = 'Montserrat';
  static const nunito = 'Nunito';
  static const inter = 'Inter';
  static const plex = 'IBMPlexSans';

  /// Italic only.
  static const playfair = 'PlayfairDisplay';
}

/// Text in [family]. The weight is also set as the font's `wght` axis: each family is one variable
/// file, which otherwise draws every weight the same.
TextStyle font(
  String family,
  double size, {
  FontWeight weight = FontWeight.w400,
  Color? color,
  double? lineHeight,
  FontStyle? style,
  double? letterSpacing,
}) => TextStyle(
  fontFamily: family,
  fontSize: size,
  fontWeight: weight,
  fontVariations: [FontVariation.weight(weight.value.toDouble())],
  fontStyle: style,
  color: color,
  height: lineHeight == null ? null : lineHeight / size,
  letterSpacing: letterSpacing,
);

/// Ids and keys: the platform's monospace font (Android's "monospace", Menlo on iOS).
TextStyle mono(double size, {Color? color, double? lineHeight, FontWeight? weight}) => TextStyle(
  fontFamily: 'monospace',
  fontFamilyFallback: const ['Menlo', 'Courier'],
  fontSize: size,
  fontWeight: weight,
  color: color,
  height: lineHeight == null ? null : lineHeight / size,
);
