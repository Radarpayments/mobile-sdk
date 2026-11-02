import 'package:flutter/material.dart';

import 'app_scope.dart';
import 'checkout/checkout.dart';
import 'core/sdk_core_screen.dart';
import 'dev/developer_screen.dart';
import 'l10n/app_localizations.dart';
import 'store/coffee/coffee_order_number.dart';
import 'store/coffee/coffee_store.dart';
import 'store/furniture/furniture_store.dart';
import 'store/invest/invest_mode.dart';
import 'store/invest/invest_store.dart';
import 'store/model/catalog.dart';
import 'store/start_screen.dart';
import 'store/store_result_screen.dart';
import 'store/tickets/tickets_store.dart';

/// Languages the demo and the SDK are translated into, each named in itself; the setting lists them in this order.
const demoLanguages = [
  ('en', 'English'),
  ('ru', 'Русский'),
  ('ro', 'Română'),
  ('el', 'Ελληνικά'),
  ('bg', 'Български'),
  ('de', 'Deutsch'),
  ('fr', 'Français'),
  ('es', 'Español'),
  ('ar', 'العربية'),
];

/// The demo app. Its language is the one picked in the developer settings (System follows the device);
/// the SDK sheet gets the same one through `PayrdrConfig.locale`.
class SampleApp extends StatelessWidget {
  const SampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    final prefs = context.prefs;
    return ListenableBuilder(
      listenable: prefs,
      builder: (context, _) => MaterialApp(
        title: 'payrdr Flutter Demo',
        debugShowCheckedModeBanner: false,
        locale: switch (prefs.localeOverride) {
          final code? => Locale(code),
          null => null,
        },
        supportedLocales: AppLocalizations.supportedLocales,
        localizationsDelegates: AppLocalizations.localizationsDelegates,
        // The demo's own screens are drawn for a light background; the SDK sheet follows its theme setting.
        theme: StartColors.theme,
        home: const StartScreen(),
      ),
    );
  }
}

/// Opens the business [cover]; it opens again on the next launch until the buyer leaves it.
void openCover(BuildContext context, CoverId cover) {
  context.prefs.setLastCover(cover);
  Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => CoverRoutes(cover: cover)));
}

Future<void> openSettings(BuildContext context) =>
    Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => const DeveloperScreen()));

void openCore(BuildContext context) =>
    Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => const SdkCoreScreen()));

/// How a store hands a finished payment of `amountMinor` to the app.
typedef OnOutcome = void Function(CheckoutOutcome outcome, int amountMinor);

/// The screens of one business; each cover's own flow plugs in here.
class CoverRoutes extends StatelessWidget {
  const CoverRoutes({super.key, required this.cover});

  final CoverId cover;

  @override
  Widget build(BuildContext context) {
    void onOutcome(CheckoutOutcome outcome, int amountMinor) {
      final store = context.store..onOutcome(cover, outcome, amountMinor);
      final receipt = store.lastReceipt;
      if (receipt == null) return;
      Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (context) => StoreResultScreen(
            cover: cover,
            receipt: receipt,
            headline: cover == CoverId.coffee ? CoffeeOrderNumber(number: store.lastOrderNumber) : null,
            onDone: () => Navigator.of(context).pop(),
            onRetry: () => Navigator.of(context).pop(),
          ),
        ),
      );
    }

    // Back from the settings in the other mode: this business no longer fits it, the start screen shows which do.
    Future<void> onOpenSettings() async {
      await openSettings(context);
      if (!context.mounted) return;
      final prefs = context.prefs;
      if (!coverFits(cover, aftDemo: isAftDemo(prefs.apiVersion, prefs.orderType))) Navigator.of(context).pop();
    }

    return PopScope(
      onPopInvokedWithResult: (didPop, _) {
        if (didPop) context.prefs.setLastCover(null);
      },
      child: switch (cover) {
        CoverId.furniture => FurnitureStore(onOutcome: onOutcome, onOpenSettings: onOpenSettings),
        CoverId.coffee => CoffeeStore(onOutcome: onOutcome, onOpenSettings: onOpenSettings),
        CoverId.tickets => TicketsStore(onOutcome: onOutcome, onOpenSettings: onOpenSettings),
        CoverId.invest => InvestStore(onOpenSettings: onOpenSettings),
      },
    );
  }
}
