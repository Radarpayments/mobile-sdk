import 'dart:ui' show Brightness, Color;

/// What the payment sheet looks like and which gateway it pays through: the
/// Flutter face of the native `SdkPaymentConfig`.
class PayrdrConfig {
  const PayrdrConfig({
    required this.baseUrl,
    this.locale,
    this.theme = const PayrdrTheme.system(),
    this.googlePay,
    this.applePay,
    this.cardScanner = false,
    this.showResultScreen = false,
    this.keyProviderUrl,
    this.sslPins = const [],
    this.debugLogging = false,
  });

  /// Payment gateway base URL, e.g. `https://pay.example.com/payment`.
  final String baseUrl;

  /// Language of the sheet, a tag such as `de` or `ar`; null follows the device.
  final String? locale;

  /// Colours and corners of the sheet.
  final PayrdrTheme theme;

  /// Shows Google Pay (Android) when the gateway offers it for the order;
  /// null hides it.
  final GooglePayOptions? googlePay;

  /// Shows Apple Pay (iOS) when the gateway offers it for the order; null
  /// hides it.
  final ApplePayOptions? applePay;

  /// Puts the camera scan icon on the card form.
  final bool cardScanner;

  /// The sheet shows its own success / declined screen before it returns.
  final bool showResultScreen;

  /// Where the gateway's public keys come from; null = `{baseUrl}/se/keys.do`.
  final String? keyProviderUrl;

  /// Certificate pins for the gateway's hosts; empty = the system trust store.
  final List<SslPin> sslPins;

  /// Writes the SDK's log to logcat / the Xcode console.
  final bool debugLogging;

  Map<String, Object?> toJson() => {
        'baseUrl': baseUrl,
        if (locale != null) 'locale': locale,
        'theme': theme.toJson(),
        if (googlePay != null) 'googlePay': googlePay!.toJson(),
        if (applePay != null) 'applePay': applePay!.toJson(),
        'cardScanner': cardScanner,
        'showResultScreen': showResultScreen,
        if (keyProviderUrl != null) 'keyProviderUrl': keyProviderUrl,
        if (sslPins.isNotEmpty) 'sslPins': [for (final pin in sslPins) pin.toJson()],
        'debugLogging': debugLogging,
      };
}

/// The sheet's theme: a stock one, or [PayrdrTheme.custom] in the store's
/// brand.
class PayrdrTheme {
  /// Follows the device's light / dark setting.
  const PayrdrTheme.system() : this._('system');

  const PayrdrTheme.light() : this._('light');

  const PayrdrTheme.dark() : this._('dark');

  /// Starts from the [base] palette and changes only the [colors] given and
  /// the corners set, in logical pixels. A large [buttonCornerRadius] (e.g.
  /// 999) makes pill buttons.
  const PayrdrTheme.custom({
    Brightness base = Brightness.light,
    PayrdrColors colors = const PayrdrColors(),
    double? cornerRadius,
    double? buttonCornerRadius,
  }) : this._('custom',
            base: base,
            colors: colors,
            cornerRadius: cornerRadius,
            buttonCornerRadius: buttonCornerRadius);

  const PayrdrTheme._(
    this.mode, {
    this.base = Brightness.light,
    this.colors = const PayrdrColors(),
    this.cornerRadius,
    this.buttonCornerRadius,
  });

  final String mode;
  final Brightness base;
  final PayrdrColors colors;
  final double? cornerRadius;
  final double? buttonCornerRadius;

  Map<String, Object?> toJson() => {
        'mode': mode,
        if (mode == 'custom') ...{
          'base': base == Brightness.dark ? 'dark' : 'light',
          'colors': colors.toJson(),
          if (cornerRadius != null) 'cornerRadius': cornerRadius,
          if (buttonCornerRadius != null) 'buttonCornerRadius': buttonCornerRadius,
        },
      };
}

/// Colours of a custom theme; null keeps the base palette's colour.
class PayrdrColors {
  const PayrdrColors({
    this.primary,
    this.onPrimary,
    this.background,
    this.onBackground,
    this.surface,
    this.onSurface,
    this.error,
    this.onError,
    this.outline,
    this.success,
    this.onSurfaceVariant,
    this.placeholder,
    this.outlineStrong,
    this.scrim,
  });

  /// The pay button and the selected items.
  final Color? primary;
  final Color? onPrimary;

  /// The sheet.
  final Color? background;
  final Color? onBackground;

  /// Input fields and cards inside the sheet.
  final Color? surface;
  final Color? onSurface;
  final Color? error;
  final Color? onError;
  final Color? outline;
  final Color? success;

  /// Secondary text.
  final Color? onSurfaceVariant;
  final Color? placeholder;
  final Color? outlineStrong;

  /// The dimming behind the sheet.
  final Color? scrim;

  Map<String, String> toJson() => {
        for (final (name, color) in [
          ('primary', primary),
          ('onPrimary', onPrimary),
          ('background', background),
          ('onBackground', onBackground),
          ('surface', surface),
          ('onSurface', onSurface),
          ('error', error),
          ('onError', onError),
          ('outline', outline),
          ('success', success),
          ('onSurfaceVariant', onSurfaceVariant),
          ('placeholder', placeholder),
          ('outlineStrong', outlineStrong),
          ('scrim', scrim),
        ])
          if (color != null) name: _hex(color),
      };

  static String _hex(Color color) => '#${color.toARGB32().toRadixString(16).padLeft(8, '0').toUpperCase()}';
}

enum GooglePayEnvironment { test, production }

enum GooglePayCardNetwork { amex, discover, interac, jcb, mastercard, visa }

enum GooglePayAuthMethod { panOnly, cryptogram3ds }

/// Google Pay on Android. Every field may stay null: the SDK then takes it
/// from the gateway's settings and the order.
class GooglePayOptions {
  const GooglePayOptions({
    this.merchantId,
    this.merchantName,
    this.gateway,
    this.gatewayMerchantId,
    this.environment,
    this.countryCode,
    this.cardNetworks,
    this.authMethods,
    this.billingAddressRequired = false,
  });

  final String? merchantId;

  /// Shown on the Google Pay sheet; null = the merchant name of the order.
  final String? merchantName;
  final String? gateway;
  final String? gatewayMerchantId;
  final GooglePayEnvironment? environment;

  /// ISO 3166-1 alpha-2, e.g. `DE`; null = the merchant's country.
  final String? countryCode;

  /// null = Visa and Mastercard.
  final Set<GooglePayCardNetwork>? cardNetworks;

  /// null = PAN_ONLY and CRYPTOGRAM_3DS.
  final Set<GooglePayAuthMethod>? authMethods;
  final bool billingAddressRequired;

  Map<String, Object?> toJson() => {
        if (merchantId != null) 'merchantId': merchantId,
        if (merchantName != null) 'merchantName': merchantName,
        if (gateway != null) 'gateway': gateway,
        if (gatewayMerchantId != null) 'gatewayMerchantId': gatewayMerchantId,
        if (environment != null) 'environment': environment!.name.toUpperCase(),
        if (countryCode != null) 'countryCode': countryCode,
        if (cardNetworks != null) 'cardNetworks': [for (final n in cardNetworks!) n.name.toUpperCase()],
        if (authMethods != null)
          'authMethods': [
            for (final m in authMethods!) m == GooglePayAuthMethod.panOnly ? 'PAN_ONLY' : 'CRYPTOGRAM_3DS',
          ],
        'billingAddressRequired': billingAddressRequired,
      };
}

enum ApplePayNetwork { amex, discover, jcb, mastercard, visa }

enum ApplePayCapability { threeDSecure, emv, credit, debit }

/// Apple Pay on iOS. Only the Apple merchant id is needed; the country comes
/// from the order unless set.
class ApplePayOptions {
  const ApplePayOptions({required this.merchantId, this.countryCode, this.networks, this.capabilities});

  /// `merchant.…`, the id in the app's Apple Pay entitlement.
  final String merchantId;
  final String? countryCode;

  /// null = Visa and Mastercard.
  final Set<ApplePayNetwork>? networks;

  /// null = 3-D Secure, credit and debit.
  final Set<ApplePayCapability>? capabilities;

  Map<String, Object?> toJson() => {
        'merchantId': merchantId,
        if (countryCode != null) 'countryCode': countryCode,
        if (networks != null) 'networks': [for (final n in networks!) n.name.toUpperCase()],
        if (capabilities != null)
          'capabilities': [
            for (final c in capabilities!) c == ApplePayCapability.threeDSecure ? 'THREE_D_SECURE' : c.name.toUpperCase(),
          ],
      };
}

/// A certificate pin: the base64 SHA-256 of the host's public key (44
/// characters, no `sha256/` prefix).
class SslPin {
  const SslPin({required this.host, required this.sha256});

  final String host;
  final String sha256;

  Map<String, String> toJson() => {'host': host, 'sha256': sha256};
}
