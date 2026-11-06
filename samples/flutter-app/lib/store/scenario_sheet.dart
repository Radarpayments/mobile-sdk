import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import '../app_scope.dart';
import 'start_screen.dart';
import 'theme/cover_style.dart';

/// Opens the demo panel. Neutral on purpose — it is a tool for the person showing the demo, not part
/// of the store — and the same in every cover. Every change is saved at once. [buyerChoice] is false where the buyer
/// is always the one with saved cards (Northwind): only "Reset" is left.
Future<void> showScenarioSheet(BuildContext context, {bool buyerChoice = true}) => showModalBottomSheet<void>(
  context: context,
  isScrollControlled: true,
  showDragHandle: true,
  backgroundColor: StartColors.surface,
  builder: (_) => Theme(
    data: StartColors.theme,
    child: ScenarioSheet(buyerChoice: buyerChoice),
  ),
);

class ScenarioSheet extends StatelessWidget {
  const ScenarioSheet({super.key, this.buyerChoice = true});

  final bool buyerChoice;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final prefs = context.prefs;
    return ListenableBuilder(
      listenable: prefs,
      builder: (context, _) {
        // Changes start from the stored scenario, not this build's copy: two quick taps must both count.
        final scenario = prefs.scenario;
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 16),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  l10n.scenarioTitle,
                  style: font(StoreFonts.inter, 20, weight: FontWeight.w800, color: StartColors.text),
                ),
                GroupHeader(l10n.scenarioBuyer),
                if (buyerChoice)
                  SegmentedButton<bool>(
                    segments: [
                      ButtonSegment(value: false, label: Text(l10n.scenarioGuest)),
                      ButtonSegment(
                        value: true,
                        label: Text(l10n.scenarioWithCards, key: const Key('scenario_with_cards')),
                      ),
                    ],
                    selected: {scenario.buyerWithCards},
                    showSelectedIcon: false,
                    onSelectionChanged: (selected) =>
                        prefs.setScenario(prefs.scenario.copyWith(buyerWithCards: selected.first)),
                  ),
                Padding(
                  padding: const EdgeInsets.only(top: 4),
                  child: Row(
                    children: [
                      Expanded(
                        child: Text(
                          l10n.scenarioWithCardsHint,
                          style: font(StoreFonts.inter, 13, lineHeight: 18, color: StartColors.muted),
                        ),
                      ),
                      TextButton(
                        onPressed: prefs.resetClientId,
                        child: Text(l10n.scenarioReset, style: const TextStyle(color: StartColors.link)),
                      ),
                    ],
                  ),
                ),
                GroupHeader(l10n.scenarioPayment),
                _ToggleRow(
                  key: const Key('scenario_wallet'),
                  title: defaultTargetPlatform == TargetPlatform.iOS ? l10n.scenarioWalletApple : l10n.scenarioWallet,
                  value: scenario.wallet,
                  onChanged: (on) => prefs.setScenario(prefs.scenario.copyWith(wallet: on)),
                ),
                const Divider(height: 1, color: StartColors.border),
                _ToggleRow(
                  key: const Key('scenario_scanner'),
                  title: l10n.scenarioScanner,
                  value: scenario.scanner,
                  onChanged: (on) => prefs.setScenario(prefs.scenario.copyWith(scanner: on)),
                ),
                const Divider(height: 1, color: StartColors.border),
                _ToggleRow(
                  key: const Key('scenario_sdk_result'),
                  title: l10n.scenarioSdkResult,
                  value: scenario.sdkResultScreen,
                  onChanged: (on) => prefs.setScenario(prefs.scenario.copyWith(sdkResultScreen: on)),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}

/// Caption of a group of settings, in capitals.
class GroupHeader extends StatelessWidget {
  const GroupHeader(this.title, {super.key});

  final String title;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 20, bottom: 8),
    child: Text(
      title.toUpperCase(),
      style: font(StoreFonts.inter, 12, weight: FontWeight.w700, color: StartColors.muted, letterSpacing: 0.5),
    ),
  );
}

class _ToggleRow extends StatelessWidget {
  const _ToggleRow({super.key, required this.title, required this.value, required this.onChanged});

  final String title;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) => MergeSemantics(
    child: InkWell(
      onTap: () => onChanged(!value),
      child: ConstrainedBox(
        constraints: const BoxConstraints(minHeight: 52),
        child: Row(
          children: [
            Expanded(
              child: Text(title, style: font(StoreFonts.inter, 15, color: StartColors.text)),
            ),
            Switch(value: value, onChanged: onChanged),
          ],
        ),
      ),
    ),
  );
}
