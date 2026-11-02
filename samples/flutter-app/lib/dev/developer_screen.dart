import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app.dart';
import '../app_scope.dart';
import '../merchant/order_registrar.dart';
import '../store/model/scenario.dart';
import '../store/scenario_sheet.dart' show GroupHeader;
import '../store/start_screen.dart';
import '../store/theme/cover_style.dart';

/// Settings for whoever runs the demo: the gateway and merchant credentials used to register orders,
/// the SDK sheet look and language, the wallet of the platform, and the last order.
class DeveloperScreen extends StatefulWidget {
  const DeveloperScreen({super.key});

  @override
  State<DeveloperScreen> createState() => _DeveloperScreenState();
}

class _DeveloperScreenState extends State<DeveloperScreen> {
  // The fields edit their own text and save every change: rebuilding them from the settings on each
  // keystroke would move the cursor.
  late final TextEditingController _baseUrl;
  late final TextEditingController _login;
  late final TextEditingController _password;
  late final TextEditingController _apiKey;
  late final TextEditingController _googlePayGateway;
  late final TextEditingController _googlePayMerchantId;
  late final TextEditingController _applePayMerchantId;

  @override
  void initState() {
    super.initState();
    final prefs = context.prefs;
    _baseUrl = TextEditingController(text: prefs.baseUrl);
    _login = TextEditingController(text: prefs.merchantLogin);
    _password = TextEditingController(text: prefs.merchantPassword);
    _apiKey = TextEditingController(text: prefs.apiKey);
    _googlePayGateway = TextEditingController(text: prefs.googlePayGateway);
    _googlePayMerchantId = TextEditingController(text: prefs.googlePayMerchantId);
    _applePayMerchantId = TextEditingController(text: prefs.applePayMerchantId);
  }

  @override
  void dispose() {
    for (final controller in [
      _baseUrl,
      _login,
      _password,
      _apiKey,
      _googlePayGateway,
      _googlePayMerchantId,
      _applePayMerchantId,
    ]) {
      controller.dispose();
    }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final prefs = context.prefs;
    final iOS = defaultTargetPlatform == TargetPlatform.iOS;
    return SystemBarIcons(
      lightBackground: true,
      child: Theme(
        data: StartColors.theme,
        child: Scaffold(
          body: SafeArea(
            child: ListenableBuilder(
              listenable: prefs,
              builder: (context, _) => ListView(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                children: [
                  Row(
                    children: [
                      const BackButton(color: StartColors.text),
                      Expanded(
                        child: Text(
                          l10n.devTitle,
                          style: font(StoreFonts.inter, 20, weight: FontWeight.w800, color: StartColors.text),
                        ),
                      ),
                    ],
                  ),
                  GroupHeader(l10n.devGateway),
                  _Field(
                    label: l10n.devBaseUrl,
                    controller: _baseUrl,
                    onChanged: prefs.setBaseUrl,
                    keyboard: TextInputType.url,
                  ),
                  // API V1 (login and password) or API V2 (API key): how the demo registers its orders.
                  _Label(l10n.devApiVersion),
                  _Chips(
                    options: [(ApiVersion.v1, l10n.devApiV1), (ApiVersion.v2, l10n.devApiV2)],
                    selected: prefs.apiVersion,
                    onSelect: prefs.setApiVersion,
                  ),
                  ...switch (prefs.apiVersion) {
                    ApiVersion.v1 => [
                      _Note(l10n.devApiV1Hint),
                      // Transfers are registered through API V1 (registerP2P.do).
                      _Label(l10n.devOrderType),
                      _Chips(
                        options: [(OrderType.purchase, l10n.devOrderPurchase), (OrderType.aft, l10n.devOrderAft)],
                        selected: prefs.orderType,
                        onSelect: prefs.setOrderType,
                      ),
                      if (prefs.orderType == OrderType.aft) ...[_Note(l10n.devOrderAftHint), const _InvestReset()],
                      _Field(
                        key: const Key('dev_login'),
                        label: l10n.devLogin,
                        controller: _login,
                        onChanged: (value) => prefs.setCredentials(login: value),
                      ),
                      _Field(
                        key: const Key('dev_password'),
                        label: l10n.devPassword,
                        controller: _password,
                        onChanged: (value) => prefs.setCredentials(password: value),
                        secret: true,
                      ),
                    ],
                    ApiVersion.v2 => [
                      _Note(l10n.devApiV2Hint),
                      _Field(
                        key: const Key('dev_api_key'),
                        label: l10n.devApiKey,
                        controller: _apiKey,
                        onChanged: prefs.setApiKey,
                        secret: true,
                      ),
                    ],
                  },
                  Text(
                    l10n.devCredentialsNote,
                    style: font(StoreFonts.inter, 13, lineHeight: 18, color: StartColors.muted),
                  ),
                  GroupHeader(l10n.devSdkLook),
                  _Label(l10n.devSheetTheme),
                  _Chips(
                    options: [
                      (SheetThemeOverride.cover, l10n.devThemeCover),
                      (SheetThemeOverride.light, l10n.devThemeLight),
                      (SheetThemeOverride.dark, l10n.devThemeDark),
                      (SheetThemeOverride.system, l10n.devThemeSystem),
                    ],
                    selected: prefs.sheetTheme,
                    onSelect: prefs.setSheetTheme,
                  ),
                  _Label(l10n.devLanguage),
                  // The SDK reads it from the settings with each payment; the demo's own screens switch at once.
                  _Chips<String?>(
                    options: [(null, l10n.devLanguageSystem), ...demoLanguages],
                    selected: prefs.localeOverride,
                    onSelect: prefs.setLocaleOverride,
                  ),
                  if (iOS) ...[
                    GroupHeader(l10n.devApplePay),
                    _Field(
                      label: l10n.devGpayMerchant,
                      controller: _applePayMerchantId,
                      onChanged: prefs.setApplePayMerchantId,
                    ),
                  ] else ...[
                    GroupHeader(l10n.devWallets),
                    _Field(
                      label: l10n.devGpayGateway,
                      controller: _googlePayGateway,
                      onChanged: (value) => prefs.setGooglePay(gateway: value),
                      placeholder: l10n.devGpayFromGateway,
                    ),
                    _Field(
                      label: l10n.devGpayMerchant,
                      controller: _googlePayMerchantId,
                      onChanged: (value) => prefs.setGooglePay(merchantId: value),
                      placeholder: l10n.devGpayFromGateway,
                    ),
                  ],
                  GroupHeader(l10n.devMore),
                  _LastOrder(mdOrder: prefs.lastMdOrder),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Field extends StatelessWidget {
  const _Field({
    super.key,
    required this.label,
    required this.controller,
    required this.onChanged,
    this.keyboard = TextInputType.text,
    this.secret = false,
    this.placeholder,
  });

  final String label;
  final TextEditingController controller;
  final ValueChanged<String> onChanged;
  final TextInputType keyboard;
  final bool secret;
  final String? placeholder;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 8),
    child: TextField(
      controller: controller,
      onChanged: onChanged,
      keyboardType: keyboard,
      obscureText: secret,
      autocorrect: false,
      enableSuggestions: false,
      decoration: InputDecoration(
        labelText: label,
        hintText: placeholder,
        border: const OutlineInputBorder(),
        filled: true,
        fillColor: StartColors.surface,
      ),
    ),
  );
}

/// Puts the Northwind Invest demo account back as a fresh install has it.
class _InvestReset extends StatefulWidget {
  const _InvestReset();

  @override
  State<_InvestReset> createState() => _InvestResetState();
}

class _InvestResetState extends State<_InvestReset> {
  bool _reset = false;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.start,
    children: [
      TextButton(
        key: const Key('dev_invest_reset'),
        onPressed: () {
          context.prefs.setInvestAccount(null);
          setState(() => _reset = true);
        },
        child: Text(context.l10n.devInvestReset),
      ),
      if (_reset) _Note(context.l10n.devInvestResetDone),
    ],
  );
}

class _Note extends StatelessWidget {
  const _Note(this.text);

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 8),
    child: Text(text, style: font(StoreFonts.inter, 13, lineHeight: 18, color: StartColors.muted)),
  );
}

class _Label extends StatelessWidget {
  const _Label(this.text);

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 4, bottom: 4),
    child: Text(text, style: font(StoreFonts.inter, 14, color: StartColors.text)),
  );
}

class _Chips<T> extends StatelessWidget {
  const _Chips({required this.options, required this.selected, required this.onSelect});

  final List<(T, String)> options;
  final T selected;
  final ValueChanged<T> onSelect;

  @override
  Widget build(BuildContext context) => Wrap(
    spacing: 8,
    children: [
      for (final (value, label) in options)
        FilterChip(label: Text(label), selected: value == selected, onSelected: (_) => onSelect(value)),
    ],
  );
}

class _LastOrder extends StatelessWidget {
  const _LastOrder({required this.mdOrder});

  final String? mdOrder;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return ConstrainedBox(
      constraints: const BoxConstraints(minHeight: 56),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(l10n.devLastOrder, style: const TextStyle(fontSize: 15, color: StartColors.text)),
                SelectableText(mdOrder ?? l10n.devNone, style: mono(12, color: StartColors.muted)),
              ],
            ),
          ),
          if (mdOrder case final mdOrder?)
            TextButton(
              onPressed: () => Clipboard.setData(ClipboardData(text: mdOrder)),
              child: Text(l10n.devCopy, style: const TextStyle(color: StartColors.link)),
            ),
        ],
      ),
    );
  }
}
