import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app_scope.dart';
import '../store/theme/cover_style.dart';
import 'card_network.dart';
import 'sdk_core_screen.dart';

/// What is typed into the card fields. Kept in memory only: card data are never stored.
class CardFields {
  final pan = TextEditingController();
  final expiry = TextEditingController();
  final cvc = TextEditingController();
  final holder = TextEditingController();

  void dispose() {
    for (final controller in [pan, expiry, cvc, holder]) {
      controller.dispose();
    }
  }
}

/// The card fields as one group, the way the SDK's own form shows them: the number across the top with the
/// network, expiry and CVC side by side, the name below, split by hairlines in one frame. The focused cell
/// is outlined in the accent colour, a cell with an error in red; the error itself goes under the group.
class CardFieldsGroup extends StatefulWidget {
  const CardFieldsGroup({
    super.key,
    required this.fields,
    required this.errors,
    required this.onEdit,
    required this.colors,
  });

  final CardFields fields;
  final Map<CoreInput, String> errors;
  final ValueChanged<CoreInput> onEdit;
  final CoreColors colors;

  @override
  State<CardFieldsGroup> createState() => _CardFieldsGroupState();
}

class _CardFieldsGroupState extends State<CardFieldsGroup> {
  static const _radius = Radius.circular(12);
  static const _stroke = 1.5;
  static const _cellHeight = 48.0;

  final _focus = {for (final input in _cardInputs) input: FocusNode()};

  static const _cardInputs = [CoreInput.pan, CoreInput.expiry, CoreInput.cvc, CoreInput.holder];

  @override
  void initState() {
    super.initState();
    for (final node in _focus.values) {
      node.addListener(() => setState(() {}));
    }
  }

  @override
  void dispose() {
    for (final node in _focus.values) {
      node.dispose();
    }
    super.dispose();
  }

  Color? _highlight(CoreInput input) => widget.errors.containsKey(input)
      ? widget.colors.error
      : _focus[input]!.hasFocus
      ? widget.colors.accent
      : null;

  @override
  Widget build(BuildContext context) {
    final colors = widget.colors;
    final fields = widget.fields;
    final l10n = context.l10n;
    final network = CardNetwork.of(fields.pan.text);
    final failed = _cardInputs.where(widget.errors.containsKey).firstOrNull;
    final hairline = BorderSide(color: colors.border, width: _stroke);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 6),
          child: Text(
            l10n.coreCardGroup,
            style: font(StoreFonts.inter, 13, weight: FontWeight.w600, color: colors.muted),
          ),
        ),
        Container(
          decoration: BoxDecoration(
            color: colors.surface,
            border: Border.fromBorderSide(hairline),
            borderRadius: const BorderRadius.all(_radius),
          ),
          child: Column(
            children: [
              _cell(
                CoreInput.pan,
                fields.pan,
                l10n.corePan,
                const BorderRadius.vertical(top: _radius),
                formatter: DigitsFormatter(
                  maxDigits: (digits) => CardNetwork.of(digits).maxPanLength,
                  format: (digits) => CardNetwork.of(digits).group(digits),
                ),
                onChanged: () {
                  // A new network may take a shorter CVC (Amex has 4 digits, the others 3).
                  final max = CardNetwork.of(fields.pan.text).cvcLength;
                  if (fields.cvc.text.length > max) fields.cvc.text = fields.cvc.text.substring(0, max);
                },
                // The network the number belongs to, as the SDK's form shows its logo there.
                trailing: network == CardNetwork.unknown
                    ? null
                    : Text(
                        network.label,
                        style: font(StoreFonts.inter, 12, weight: FontWeight.w700, color: colors.muted),
                      ),
              ),
              Divider(height: _stroke, thickness: _stroke, color: colors.border),
              IntrinsicHeight(
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Expanded(
                      child: _cell(
                        CoreInput.expiry,
                        fields.expiry,
                        l10n.coreExpiryPlaceholder,
                        BorderRadius.zero,
                        formatter: DigitsFormatter(maxDigits: (_) => 4, format: formatExpiry),
                      ),
                    ),
                    VerticalDivider(width: _stroke, thickness: _stroke, color: colors.border),
                    Expanded(
                      child: _cell(
                        CoreInput.cvc,
                        fields.cvc,
                        l10n.coreCvc,
                        BorderRadius.zero,
                        formatter: DigitsFormatter(maxDigits: (_) => CardNetwork.of(fields.pan.text).cvcLength),
                      ),
                    ),
                  ],
                ),
              ),
              Divider(height: _stroke, thickness: _stroke, color: colors.border),
              _cell(
                CoreInput.holder,
                fields.holder,
                l10n.coreHolder,
                const BorderRadius.vertical(bottom: _radius),
                capitalization: TextCapitalization.characters,
              ),
            ],
          ),
        ),
        if (failed != null)
          Padding(
            padding: const EdgeInsets.only(top: 6),
            child: Text(
              errorText(context, widget.errors[failed]!),
              key: Key('core_error_${failed.name}'),
              style: font(StoreFonts.inter, 12, color: colors.error),
            ),
          ),
      ],
    );
  }

  Widget _cell(
    CoreInput input,
    TextEditingController controller,
    String placeholder,
    BorderRadius corners, {
    TextInputFormatter? formatter,
    VoidCallback? onChanged,
    Widget? trailing,
    TextCapitalization capitalization = TextCapitalization.none,
  }) {
    final colors = widget.colors;
    final highlight = _highlight(input);
    // One font for the placeholder and the value, with even digits, so nothing jumps while typing.
    final style = font(
      StoreFonts.inter,
      15,
      color: colors.text,
    ).copyWith(fontFeatures: const [FontFeature.tabularFigures()]);
    return Container(
      height: _cellHeight,
      padding: const EdgeInsetsDirectional.only(start: 14, end: 10),
      foregroundDecoration: highlight == null
          ? null
          : BoxDecoration(
              border: Border.all(color: highlight, width: _stroke),
              borderRadius: corners,
            ),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              key: Key('core_field_${input.name}'),
              controller: controller,
              focusNode: _focus[input],
              keyboardType: formatter == null ? TextInputType.name : TextInputType.number,
              textCapitalization: capitalization,
              inputFormatters: [?formatter],
              autocorrect: false,
              enableSuggestions: false,
              cursorColor: colors.accent,
              style: style,
              onChanged: (_) {
                onChanged?.call();
                widget.onEdit(input);
                setState(() {});
              },
              decoration: InputDecoration.collapsed(
                hintText: placeholder,
                hintStyle: style.copyWith(color: colors.placeholder),
              ),
            ),
          ),
          ?trailing,
        ],
      ),
    );
  }
}
