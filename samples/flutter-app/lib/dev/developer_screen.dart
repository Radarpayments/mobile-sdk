import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

import '../app.dart';
import '../app_scope.dart';
import '../merchant/order_registrar.dart';
import '../store/invest/invest_mode.dart';
import '../store/model/scenario.dart';
import '../store/start_screen.dart';
import '../store/theme/cover_style.dart';
import 'settings_rows.dart';

/// Settings for whoever runs the demo: the gateway and merchant credentials used to register orders,
/// the SDK sheet look and language, the wallet of the platform, and the last order. The four sample apps lay it out
/// the same way.
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
          backgroundColor: settingsBackground,
          body: SafeArea(
            child: ListenableBuilder(
              listenable: prefs,
              builder: (context, _) {
                final v1 = prefs.apiVersion == ApiVersion.v1;
                final aft = isAftDemo(prefs.apiVersion, prefs.orderType);
                return ListView(
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
                    SettingsSection(
                      title: l10n.devGateway,
                      footer: [v1 ? l10n.devApiV1Hint : l10n.devApiV2Hint],
                      rows: [
                        FieldRow(
                          label: l10n.devBaseUrl,
                          controller: _baseUrl,
                          onChanged: prefs.setBaseUrl,
                          keyboard: TextInputType.url,
                          error: isGatewayUrl(_baseUrl.text) ? null : l10n.devBaseUrlError,
                        ),
                        SegmentRow(
                          title: l10n.devApiVersion,
                          options: [(ApiVersion.v1, l10n.devApiV1), (ApiVersion.v2, l10n.devApiV2)],
                          selected: prefs.apiVersion,
                          onSelect: prefs.setApiVersion,
                        ),
                      ],
                    ),
                    SettingsSection(
                      title: l10n.devCredentials,
                      footer: [if (aft) l10n.devOrderAftHint, l10n.devCredentialsNote],
                      rows: v1
                          ? [
                              // Transfers are registered through API V1 (registerP2P.do).
                              SegmentRow(
                                title: l10n.devOrderType,
                                options: [
                                  (OrderType.purchase, l10n.devOrderPurchase),
                                  (OrderType.aft, l10n.devOrderAft),
                                ],
                                selected: prefs.orderType,
                                onSelect: prefs.setOrderType,
                              ),
                              FieldRow(
                                key: const Key('dev_login'),
                                label: l10n.devLogin,
                                controller: _login,
                                onChanged: (value) => prefs.setCredentials(login: value),
                              ),
                              FieldRow(
                                key: const Key('dev_password'),
                                label: l10n.devPassword,
                                controller: _password,
                                onChanged: (value) => prefs.setCredentials(password: value),
                                secret: true,
                              ),
                            ]
                          : [
                              FieldRow(
                                key: const Key('dev_api_key'),
                                label: l10n.devApiKey,
                                controller: _apiKey,
                                onChanged: prefs.setApiKey,
                                secret: true,
                              ),
                            ],
                    ),
                    SettingsSection(
                      title: l10n.devSdkLook,
                      rows: [
                        PickerRow(
                          title: l10n.devSheetTheme,
                          options: [
                            (SheetThemeOverride.cover, l10n.devThemeCover),
                            (SheetThemeOverride.light, l10n.devThemeLight),
                            (SheetThemeOverride.dark, l10n.devThemeDark),
                            (SheetThemeOverride.system, l10n.devThemeSystem),
                          ],
                          selected: prefs.sheetTheme,
                          onSelect: prefs.setSheetTheme,
                        ),
                        // The SDK reads it from the settings with each payment; the demo's own screens switch at once.
                        PickerRow<String?>(
                          title: l10n.devLanguage,
                          options: [(null, l10n.devLanguageSystem), ...demoLanguages],
                          selected: prefs.localeOverride,
                          onSelect: prefs.setLocaleOverride,
                        ),
                      ],
                    ),
                    if (iOS)
                      SettingsSection(
                        title: l10n.devApplePay,
                        rows: [
                          FieldRow(
                            label: l10n.devGpayMerchant,
                            controller: _applePayMerchantId,
                            onChanged: prefs.setApplePayMerchantId,
                          ),
                        ],
                      )
                    else
                      SettingsSection(
                        title: l10n.devWallets,
                        rows: [
                          FieldRow(
                            label: l10n.devGpayGateway,
                            controller: _googlePayGateway,
                            onChanged: (value) => prefs.setGooglePay(gateway: value),
                            placeholder: l10n.devGpayFromGateway,
                          ),
                          FieldRow(
                            label: l10n.devGpayMerchant,
                            controller: _googlePayMerchantId,
                            onChanged: (value) => prefs.setGooglePay(merchantId: value),
                            placeholder: l10n.devGpayFromGateway,
                          ),
                        ],
                      ),
                    SettingsSection(
                      title: l10n.devMore,
                      rows: [LastOrderRow(mdOrder: prefs.lastMdOrder)],
                    ),
                  ],
                );
              },
            ),
          ),
        ),
      ),
    );
  }
}

/// The gateway is reached over HTTPS only; anything else would fail on the first order with a less telling error.
bool isGatewayUrl(String url) {
  final text = url.trim();
  // Uri lowercases the scheme; the other sample apps reject `HTTPS://`, so this one does too.
  final uri = Uri.tryParse(text);
  return text.startsWith('https://') && uri != null && uri.host.isNotEmpty;
}
