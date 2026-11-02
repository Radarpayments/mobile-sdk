import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:payrdr_sdk/payrdr_sdk.dart';

import '../app_scope.dart';
import '../store/theme/cover_style.dart';
import 'card_fields_group.dart';
import 'card_network.dart';

/// The three kinds of token, remembered under [storageKey].
enum SdkCoreVariant {
  newCard('new'),
  instant('instant'),
  stored('stored');

  const SdkCoreVariant(this.storageKey);

  final String storageKey;

  static SdkCoreVariant fromStorageKey(String key) =>
      values.where((variant) => variant.storageKey == key).firstOrNull ?? newCard;
}

/// Inputs of the SDK Core form; each sdk-core error is shown next to one of them.
enum CoreInput { pan, expiry, cvc, holder, bindingId, mdOrder, pubKey }

/// The form field an sdk-core error belongs to; null for errors no field explains.
CoreInput? coreInputOf(CoreField field) => switch (field) {
  CoreField.pan => CoreInput.pan,
  CoreField.expiry => CoreInput.expiry,
  CoreField.cvc => CoreInput.cvc,
  CoreField.cardholder => CoreInput.holder,
  CoreField.bindingId => CoreInput.bindingId,
  CoreField.mdOrder => CoreInput.mdOrder,
  CoreField.pubKey => CoreInput.pubKey,
  CoreField.storedPaymentId || CoreField.unknown => null,
};

/// sdk-core reports stable codes; a merchant maps them to its own words, as here.
String errorText(BuildContext context, String code) => switch (code) {
  'required' => context.l10n.coreErrorRequired,
  'invalid-format' => context.l10n.coreErrorFormat,
  _ => context.l10n.coreErrorInvalid,
};

/// SDK Core: a card form of the merchant's own, encrypted on the device into the token the merchant's
/// server sends to the gateway. Card data stay in memory; mdOrder, key, bindingId and the variant are
/// remembered between launches.
class SdkCoreScreen extends StatefulWidget {
  const SdkCoreScreen({super.key});

  @override
  State<SdkCoreScreen> createState() => _SdkCoreScreenState();
}

class _SdkCoreScreenState extends State<SdkCoreScreen> {
  late SdkCoreVariant _variant;
  final _card = CardFields();
  late final TextEditingController _mdOrder;
  late final TextEditingController _pubKey;
  late final TextEditingController _bindingId;
  Map<CoreInput, String> _errors = {};
  String? _token;

  @override
  void initState() {
    super.initState();
    final prefs = context.prefs;
    _variant = SdkCoreVariant.fromStorageKey(prefs.sdkCoreVariant);
    _mdOrder = TextEditingController(text: prefs.mdOrder);
    _pubKey = TextEditingController(text: prefs.pubKeyPem);
    _bindingId = TextEditingController(text: prefs.bindingId);
  }

  @override
  void dispose() {
    _card.dispose();
    _mdOrder.dispose();
    _pubKey.dispose();
    _bindingId.dispose();
    super.dispose();
  }

  CoreTokenRequest _request() {
    final pan = _card.pan.text.replaceAll(' ', '');
    final holder = _card.holder.text.trim();
    final cvc = _card.cvc.text;
    return switch (_variant) {
      SdkCoreVariant.newCard => CoreTokenRequest.card(
        mdOrder: _mdOrder.text,
        pan: pan,
        expiry: _card.expiry.text,
        cvc: cvc,
        cardholder: holder.isEmpty ? null : holder,
        pubKey: _pubKey.text,
      ),
      SdkCoreVariant.instant => CoreTokenRequest.cardWithoutOrder(
        pan: pan,
        expiry: _card.expiry.text,
        cvc: cvc,
        cardholder: holder.isEmpty ? null : holder,
        pubKey: _pubKey.text,
      ),
      SdkCoreVariant.stored => CoreTokenRequest.savedCard(
        mdOrder: _mdOrder.text,
        bindingId: _bindingId.text,
        cvc: cvc.isEmpty ? null : cvc,
        pubKey: _pubKey.text,
      ),
    };
  }

  Future<void> _generate() async {
    Map<CoreInput, String> errors;
    String? token;
    try {
      final result = await PayrdrCore.generateToken(_request());
      errors = {for (final MapEntry(:key, :value) in result.errors.entries) ?coreInputOf(key): value};
      // An error no field explains (e.g. an unusable key) still needs a place: the key field.
      if (!result.isSuccessful && errors.isEmpty) errors = {CoreInput.pubKey: 'invalid'};
      token = result.isSuccessful ? result.token : null;
    } on ArgumentError {
      // The bridge refused the request as a whole, which here can only be the key.
      errors = {CoreInput.pubKey: 'invalid'};
      token = null;
    }
    if (mounted) {
      setState(() {
        _errors = errors;
        _token = token;
      });
    }
  }

  void _edited(CoreInput field) {
    if (_errors.containsKey(field)) setState(() => _errors = {..._errors}..remove(field));
  }

  @override
  Widget build(BuildContext context) {
    final colors = CoreColors.of(context);
    final token = _token;
    return PopScope(
      canPop: token == null,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) setState(() => _token = null);
      },
      child: SystemBarIcons(
        lightBackground: !colors.dark,
        child: Scaffold(
          backgroundColor: colors.background,
          body: SafeArea(
            child: ListView(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
              children: [
                _Header(
                  subtitle: token == null ? null : context.l10n.coreDoneSubtitle,
                  onBack: () => Navigator.of(context).maybePop(),
                  colors: colors,
                ),
                if (token != null)
                  _TokenPanel(token: token, onAgain: () => setState(() => _token = null), colors: colors)
                else
                  ..._form(colors),
              ],
            ),
          ),
        ),
      ),
    );
  }

  List<Widget> _form(CoreColors colors) {
    final l10n = context.l10n;
    final prefs = context.prefs;
    Widget field(
      CoreInput field,
      String label,
      TextEditingController controller,
      ValueChanged<String>? remember, {
      String? hint,
      bool singleLine = true,
    }) => _PastedInput(
      key: Key('core_field_${field.name}'),
      label: label,
      controller: controller,
      hint: hint,
      singleLine: singleLine,
      error: _errors[field],
      onChanged: (value) {
        _edited(field);
        remember?.call(value);
      },
      colors: colors,
    );
    return [
      _VariantSwitch(
        selected: _variant,
        onSelect: (variant) {
          setState(() {
            _variant = variant;
            _errors = {};
          });
          prefs.setSdkCoreVariant(variant.storageKey);
        },
        colors: colors,
      ),
      const SizedBox(height: 12),
      Text(switch (_variant) {
        SdkCoreVariant.newCard => l10n.coreHintNew,
        SdkCoreVariant.instant => l10n.coreHintInstant,
        SdkCoreVariant.stored => l10n.coreHintStored,
      }, style: font(StoreFonts.inter, 13, lineHeight: 18, color: colors.muted)),
      const SizedBox(height: 12),
      if (_variant == SdkCoreVariant.stored) ...[
        field(CoreInput.bindingId, l10n.coreBinding, _bindingId, prefs.setBindingId),
        const SizedBox(height: 12),
        _CvcInput(
          controller: _card.cvc,
          error: _errors[CoreInput.cvc],
          onChanged: () => _edited(CoreInput.cvc),
          colors: colors,
        ),
      ] else
        CardFieldsGroup(fields: _card, errors: _errors, onEdit: _edited, colors: colors),
      if (_variant != SdkCoreVariant.instant) ...[
        const SizedBox(height: 12),
        field(CoreInput.mdOrder, l10n.coreMdOrder, _mdOrder, prefs.setMdOrder, hint: l10n.coreMdOrderHint),
      ],
      const SizedBox(height: 12),
      field(CoreInput.pubKey, l10n.corePubKey, _pubKey, prefs.setPubKeyPem, singleLine: false),
      const SizedBox(height: 16),
      FilledButton(
        key: const Key('core_generate'),
        onPressed: _generate,
        style: FilledButton.styleFrom(
          backgroundColor: colors.button,
          foregroundColor: colors.onButton,
          minimumSize: const Size.fromHeight(52),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        ),
        child: Text(l10n.coreGenerate, style: font(StoreFonts.inter, 16, weight: FontWeight.w700)),
      ),
    ];
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.subtitle, required this.onBack, required this.colors});

  final String? subtitle;
  final VoidCallback onBack;
  final CoreColors colors;

  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(top: 4, bottom: 12),
    child: Row(
      children: [
        BackButton(onPressed: onBack, color: colors.text),
        const SizedBox(width: 4),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                context.l10n.coreTitle,
                style: font(StoreFonts.inter, 22, weight: FontWeight.w800, color: colors.text),
              ),
              if (subtitle case final subtitle?) Text(subtitle, style: font(StoreFonts.inter, 13, color: colors.muted)),
            ],
          ),
        ),
      ],
    ),
  );
}

class _VariantSwitch extends StatelessWidget {
  const _VariantSwitch({required this.selected, required this.onSelect, required this.colors});

  final SdkCoreVariant selected;
  final ValueChanged<SdkCoreVariant> onSelect;
  final CoreColors colors;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return Container(
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(color: colors.track, borderRadius: BorderRadius.circular(12)),
      child: Row(
        children: [
          for (final (variant, label) in [
            (SdkCoreVariant.newCard, l10n.coreVariantNew),
            (SdkCoreVariant.instant, l10n.coreVariantInstant),
            (SdkCoreVariant.stored, l10n.coreVariantStored),
          ])
            Expanded(
              child: Semantics(
                selected: variant == selected,
                button: true,
                child: GestureDetector(
                  onTap: () => onSelect(variant),
                  child: Container(
                    constraints: const BoxConstraints(minHeight: 40),
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      color: variant == selected ? colors.surface : Colors.transparent,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      label,
                      style: font(
                        StoreFonts.inter,
                        13,
                        weight: variant == selected ? FontWeight.w700 : FontWeight.w500,
                        color: variant == selected ? colors.text : colors.muted,
                      ),
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

/// A value copied from elsewhere (mdOrder, bindingId, the key): monospace, with "Paste".
class _PastedInput extends StatelessWidget {
  const _PastedInput({
    super.key,
    required this.label,
    required this.controller,
    required this.onChanged,
    required this.error,
    required this.colors,
    this.hint,
    this.singleLine = true,
  });

  final String label;
  final TextEditingController controller;
  final ValueChanged<String> onChanged;
  final String? error;
  final CoreColors colors;
  final String? hint;
  final bool singleLine;

  Future<void> _paste() async {
    final pasted = (await Clipboard.getData(Clipboard.kTextPlain))?.text?.trim();
    if (pasted == null) return;
    controller.text = pasted;
    onChanged(pasted);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    return _Labelled(
      label: label,
      error: error,
      colors: colors,
      child: Container(
        constraints: const BoxConstraints(minHeight: 48),
        padding: const EdgeInsetsDirectional.only(start: 14, end: 4),
        decoration: BoxDecoration(
          color: colors.surface,
          border: Border.all(color: error != null ? colors.error : colors.border),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          // In a tall field "Paste" stays by the first line.
          crossAxisAlignment: singleLine ? CrossAxisAlignment.center : CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 12),
                child: TextField(
                  controller: controller,
                  onChanged: onChanged,
                  // A whole 2048-bit PEM key: its 64-character lines wrap in two.
                  minLines: singleLine ? 1 : 4,
                  maxLines: singleLine ? 1 : 18,
                  autocorrect: false,
                  enableSuggestions: false,
                  cursorColor: colors.accent,
                  style: mono(singleLine ? 15 : 12, lineHeight: singleLine ? 20 : 16, color: colors.text),
                  decoration: InputDecoration.collapsed(
                    hintText: hint,
                    hintStyle: font(StoreFonts.inter, 15, color: colors.placeholder),
                  ),
                ),
              ),
            ),
            TextButton(
              onPressed: _paste,
              child: Text(
                l10n.corePaste,
                style: font(StoreFonts.inter, 13, weight: FontWeight.w600, color: colors.accent),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// The CVC of a saved card, on its own.
class _CvcInput extends StatelessWidget {
  const _CvcInput({required this.controller, required this.error, required this.onChanged, required this.colors});

  final TextEditingController controller;
  final String? error;
  final VoidCallback onChanged;
  final CoreColors colors;

  @override
  Widget build(BuildContext context) => _Labelled(
    label: context.l10n.coreCvcOptional,
    error: error,
    colors: colors,
    child: Container(
      constraints: const BoxConstraints(minHeight: 48),
      alignment: AlignmentDirectional.centerStart,
      padding: const EdgeInsets.symmetric(horizontal: 14),
      decoration: BoxDecoration(
        color: colors.surface,
        border: Border.all(color: error != null ? colors.error : colors.border),
        borderRadius: BorderRadius.circular(12),
      ),
      child: TextField(
        key: const Key('core_field_cvc'),
        controller: controller,
        onChanged: (_) => onChanged(),
        keyboardType: TextInputType.number,
        inputFormatters: [DigitsFormatter(maxDigits: (_) => 4)],
        cursorColor: colors.accent,
        style: mono(15, color: colors.text),
        decoration: const InputDecoration.collapsed(hintText: null),
      ),
    ),
  );
}

/// [child] with a [label] above it and the [error], if any, below.
class _Labelled extends StatelessWidget {
  const _Labelled({required this.label, required this.error, required this.colors, required this.child});

  final String label;
  final String? error;
  final CoreColors colors;
  final Widget child;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      Padding(
        padding: const EdgeInsets.only(bottom: 6),
        child: Text(
          label,
          style: font(StoreFonts.inter, 13, weight: FontWeight.w600, color: colors.muted),
        ),
      ),
      child,
      if (error case final error?)
        Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Text(errorText(context, error), style: font(StoreFonts.inter, 12, color: colors.error)),
        ),
    ],
  );
}

class _TokenPanel extends StatefulWidget {
  const _TokenPanel({required this.token, required this.onAgain, required this.colors});

  final String token;
  final VoidCallback onAgain;
  final CoreColors colors;

  @override
  State<_TokenPanel> createState() => _TokenPanelState();
}

class _TokenPanelState extends State<_TokenPanel> {
  bool _copied = false;

  @override
  Widget build(BuildContext context) {
    final colors = widget.colors;
    final l10n = context.l10n;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: colors.surface,
            border: Border.all(color: colors.border),
            borderRadius: BorderRadius.circular(16),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(color: colors.successSoft, borderRadius: BorderRadius.circular(999)),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.check, color: colors.success, size: 14),
                    const SizedBox(width: 4),
                    Text(
                      l10n.coreTokenOk,
                      style: font(StoreFonts.inter, 12, weight: FontWeight.w700, color: colors.success),
                    ),
                  ],
                ),
              ),
              Container(
                margin: const EdgeInsets.only(top: 12),
                width: double.infinity,
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(color: colors.track, borderRadius: BorderRadius.circular(10)),
                child: SelectableText(
                  widget.token,
                  key: const Key('core_token'),
                  style: mono(11, lineHeight: 16, color: colors.text),
                ),
              ),
              Align(
                alignment: AlignmentDirectional.centerEnd,
                child: TextButton(
                  onPressed: () async {
                    await Clipboard.setData(ClipboardData(text: widget.token));
                    if (mounted) setState(() => _copied = true);
                  },
                  child: Text(
                    _copied ? l10n.coreCopied : l10n.coreCopy,
                    style: font(StoreFonts.inter, 13, weight: FontWeight.w700, color: colors.accent),
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        for (final (index, step) in [l10n.coreStepSend, l10n.coreStepGateway, l10n.coreStepPay].indexed)
          Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Row(
              children: [
                Container(
                  width: 24,
                  height: 24,
                  alignment: Alignment.center,
                  decoration: BoxDecoration(color: colors.stepSoft, shape: BoxShape.circle),
                  child: Text(
                    '${index + 1}',
                    style: font(StoreFonts.inter, 12, weight: FontWeight.w700, color: colors.step),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(step, style: font(StoreFonts.inter, 14, lineHeight: 19, color: colors.text)),
                ),
              ],
            ),
          ),
        const SizedBox(height: 6),
        FilledButton(
          onPressed: widget.onAgain,
          style: FilledButton.styleFrom(
            backgroundColor: colors.track,
            foregroundColor: colors.text,
            minimumSize: const Size.fromHeight(52),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          ),
          child: Text(l10n.coreAgain, style: font(StoreFonts.inter, 16, weight: FontWeight.w700)),
        ),
      ],
    );
  }
}

/// Colours of the SDK Core screen; it follows the phone's light or dark theme.
class CoreColors {
  const CoreColors._({
    required this.dark,
    required this.background,
    required this.surface,
    required this.track,
    required this.text,
    required this.muted,
    required this.placeholder,
    required this.border,
    required this.accent,
    required this.button,
    required this.onButton,
    required this.error,
    required this.success,
    required this.successSoft,
    required this.step,
    required this.stepSoft,
  });

  final bool dark;
  final Color background;
  final Color surface;
  final Color track;
  final Color text;
  final Color muted;
  final Color placeholder;
  final Color border;
  final Color accent;
  final Color button;
  final Color onButton;
  final Color error;
  final Color success;
  final Color successSoft;
  final Color step;
  final Color stepSoft;

  static CoreColors of(BuildContext context) =>
      MediaQuery.platformBrightnessOf(context) == Brightness.dark ? _dark : _light;

  static const _dark = CoreColors._(
    dark: true,
    background: Color(0xFF0B1120),
    surface: Color(0xFF111827),
    track: Color(0xFF1F2937),
    text: Color(0xFFF1F5F9),
    muted: Color(0xFF94A3B8),
    placeholder: Color(0xFF64748B),
    border: Color(0xFF1F2937),
    accent: Color(0xFF60A5FA),
    button: Color(0xFFF1F5F9),
    onButton: Color(0xFF0F172A),
    error: Color(0xFFF87171),
    success: Color(0xFF4ADE80),
    successSoft: Color(0xFF052E16),
    step: Color(0xFFC7D2FE),
    stepSoft: Color(0xFF312E81),
  );

  static const _light = CoreColors._(
    dark: false,
    background: Color(0xFFF8FAFC),
    surface: Color(0xFFFFFFFF),
    track: Color(0xFFEEF2F6),
    text: Color(0xFF0F172A),
    muted: Color(0xFF475569),
    placeholder: Color(0xFF94A3B8),
    border: Color(0xFFE2E8F0),
    accent: Color(0xFF2563EB),
    button: Color(0xFF0F172A),
    onButton: Color(0xFFFFFFFF),
    error: Color(0xFFDC2626),
    success: Color(0xFF166534),
    successSoft: Color(0xFFDCFCE7),
    step: Color(0xFF3730A3),
    stepSoft: Color(0xFFE0E7FF),
  );
}
