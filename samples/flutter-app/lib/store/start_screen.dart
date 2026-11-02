import 'package:flutter/material.dart';

import '../app.dart';
import '../app_scope.dart';
import '../l10n/app_localizations.dart';
import 'invest/invest_mode.dart';
import 'invest/invest_store.dart';
import 'model/catalog.dart';
import 'theme/cover_style.dart';

/// First screen: the SDK Payment stores, SDK Core's own card form, and the developer settings.
class StartScreen extends StatefulWidget {
  const StartScreen({super.key});

  @override
  State<StartScreen> createState() => _StartScreenState();
}

class _StartScreenState extends State<StartScreen> {
  static const _tileHeight = 112.0;

  @override
  void initState() {
    super.initState();
    // The last business opens straight away, once per launch.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final prefs = context.prefs;
      final last = reopenCover(prefs.lastCover, aftDemo: isAftDemo(prefs.apiVersion, prefs.orderType));
      if (mounted && last != null) openCover(context, last);
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final prefs = context.prefs;
    return ListenableBuilder(
      listenable: prefs,
      builder: (context, _) => _page(context, l10n, isAftDemo(prefs.apiVersion, prefs.orderType)),
    );
  }

  Widget _page(BuildContext context, AppLocalizations l10n, bool aftDemo) {
    return SystemBarIcons(
      lightBackground: true,
      child: Scaffold(
        backgroundColor: StartColors.background,
        body: SafeArea(
          child: LayoutBuilder(
            // At least a screen tall: the store tiles share what the rest leaves, so SDK Core sits at the bottom.
            // On a short screen the tiles keep their minimum height and the page scrolls instead.
            builder: (context, constraints) => SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: ConstrainedBox(
                constraints: BoxConstraints(minHeight: constraints.maxHeight),
                child: IntrinsicHeight(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: Row(
                          children: [
                            Expanded(
                              child: Text(
                                l10n.startTitle,
                                style: font(StoreFonts.inter, 26, weight: FontWeight.w800, color: StartColors.text),
                              ),
                            ),
                            IconButton(
                              onPressed: () => openSettings(context),
                              tooltip: l10n.startSettings,
                              icon: const Icon(Icons.settings_outlined, color: StartColors.text),
                            ),
                          ],
                        ),
                      ),
                      // AFT orders top up an account: shown in a business where that is what happens, not in the stores.
                      _Section(
                        title: l10n.startSectionPayment,
                        subtitle: aftDemo ? l10n.startSectionPaymentAftSub : l10n.startSectionPaymentSub,
                      ),
                      if (aftDemo)
                        Expanded(
                          child: ConstrainedBox(
                            constraints: const BoxConstraints(minHeight: _tileHeight * 2),
                            child: InvestTile(onTap: () => openCover(context, CoverId.invest)),
                          ),
                        )
                      else
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              for (final (index, (cover, image, tagline)) in [
                                (CoverId.furniture, 'furniture_hero', l10n.coverFurnitureTagline),
                                (CoverId.coffee, 'coffee_hero', l10n.coverCoffeeTagline),
                                (CoverId.tickets, 'tickets_hero', l10n.coverTicketsTagline),
                              ].indexed) ...[
                                if (index > 0) const SizedBox(height: 10),
                                Expanded(
                                  child: ConstrainedBox(
                                    constraints: const BoxConstraints(minHeight: _tileHeight),
                                    child: _PhotoTile(
                                      key: Key('cover_${cover.name}'),
                                      image: image,
                                      name: coverName(cover)(l10n),
                                      nameStyle: coverStyle(cover).heading(22),
                                      tagline: tagline,
                                      onTap: () => openCover(context, cover),
                                    ),
                                  ),
                                ),
                              ],
                            ],
                          ),
                        ),
                      _Section(title: l10n.startSectionCore, subtitle: l10n.startSectionCoreSub),
                      SizedBox(
                        height: _tileHeight,
                        child: _PhotoTile(
                          key: const Key('core_tile'),
                          image: 'core_hero',
                          name: l10n.coreTileTitle,
                          nameStyle: font(StoreFonts.inter, 22, weight: FontWeight.w800),
                          tagline: l10n.coreTileSub,
                          onTap: () => openCore(context),
                        ),
                      ),
                      const SizedBox(height: 16),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Brand name of [cover].
LocalText coverName(CoverId cover) => switch (cover) {
  CoverId.furniture => (l) => l.coverFurnitureName,
  CoverId.coffee => (l) => l.coverCoffeeName,
  CoverId.tickets => (l) => l.coverTicketsName,
  CoverId.invest => (l) => l.investName,
};

/// Heading of one SDK on the start screen.
class _Section extends StatelessWidget {
  const _Section({required this.title, required this.subtitle});

  final String title;
  final String subtitle;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 20, bottom: 10),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: font(StoreFonts.inter, 17, weight: FontWeight.w800, color: StartColors.text),
        ),
        const SizedBox(height: 2),
        Text(subtitle, style: font(StoreFonts.inter, 13, lineHeight: 18, color: StartColors.muted)),
      ],
    ),
  );
}

/// Photo with a dark fade at the bottom, the name and a tagline over it.
class _PhotoTile extends StatelessWidget {
  const _PhotoTile({
    super.key,
    required this.image,
    required this.name,
    required this.nameStyle,
    required this.tagline,
    required this.onTap,
  });

  final String image;
  final String name;
  final TextStyle nameStyle;
  final String tagline;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Semantics(
    button: true,
    child: ClipRRect(
      borderRadius: BorderRadius.circular(20),
      // Every layer is positioned: the tile's height comes from the layout, never from the photo.
      child: Stack(
        fit: StackFit.expand,
        children: [
          Positioned.fill(child: Image.asset(photo(image), fit: BoxFit.cover)),
          Positioned.fill(
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  stops: const [0.3, 1],
                  colors: [Colors.transparent, Colors.black.withValues(alpha: 0.68)],
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
                Text(name, style: nameStyle.copyWith(color: Colors.white)),
                Text(tagline, style: font(StoreFonts.inter, 13, color: Colors.white.withValues(alpha: 0.92))),
              ],
            ),
          ),
          Positioned.fill(
            child: Material(
              type: MaterialType.transparency,
              child: InkWell(onTap: onTap),
            ),
          ),
        ],
      ),
    ),
  );
}

/// Neutral colours of the start and developer screens: they belong to the demo, not to a store.
abstract final class StartColors {
  static const background = Color(0xFFF7F7F5);
  static const surface = Color(0xFFFFFFFF);
  static const text = Color(0xFF111827);
  static const muted = Color(0xFF4B5563);
  static const border = Color(0xFFE5E7EB);
  static const link = Color(0xFF2563EB);
  static const selected = Color(0xFFE5E7EB);

  /// Material theme of the neutral screens and the demo panel: grey selection instead of the default purple.
  static final theme = ThemeData(
    colorScheme: ColorScheme.fromSeed(seedColor: text).copyWith(
      primary: text,
      surface: surface,
      onSurface: text,
      secondaryContainer: selected,
      onSecondaryContainer: text,
    ),
    fontFamily: StoreFonts.inter,
    scaffoldBackgroundColor: background,
  );
}
