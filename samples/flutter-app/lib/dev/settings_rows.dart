import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../app_scope.dart';
import '../caps.dart';
import '../store/start_screen.dart';
import '../store/theme/cover_style.dart';

/// The settings page: grey like the iOS grouped form, which the four sample apps share.
const settingsBackground = Color(0xFFF2F2F7);
const _error = Color(0xFFB91C1C);
const _done = Color(0xFF15803D);
const _placeholder = Color(0xFF9CA3AF);
const _cardRadius = BorderRadius.all(Radius.circular(14));

/// How long "Copied" stays before Copy is back.
const _doneFor = Duration(seconds: 2);

final _rowText = font(StoreFonts.inter, 16, color: StartColors.text);

/// A titled group of [rows] on a white card, with an optional explanation under it.
class SettingsSection extends StatelessWidget {
  const SettingsSection({super.key, required this.title, required this.rows, this.footer = const []});

  final String title;
  final List<Widget> rows;
  final List<String> footer;

  @override
  Widget build(BuildContext context) => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      Padding(
        padding: const EdgeInsetsDirectional.only(start: 4, top: 24, bottom: 8),
        child: Semantics(
          header: true,
          child: Text(
            toCaps(title),
            style: font(StoreFonts.inter, 12, weight: FontWeight.w600, color: StartColors.muted, letterSpacing: 0.6),
          ),
        ),
      ),
      Material(
        color: StartColors.surface,
        borderRadius: _cardRadius,
        clipBehavior: Clip.antiAlias,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            for (final (index, row) in rows.indexed) ...[
              // The hairline between two rows, indented like the iOS form's.
              if (index > 0) const Divider(height: 1, thickness: 1, indent: 16, color: StartColors.border),
              row,
            ],
          ],
        ),
      ),
      for (final note in footer)
        Padding(
          padding: const EdgeInsetsDirectional.only(start: 4, end: 4, top: 8),
          child: Text(note, style: font(StoreFonts.inter, 13, lineHeight: 18, color: StartColors.muted)),
        ),
    ],
  );
}

/// A text field with its name always above the value. A [secret] field masks the value and has a button to show it,
/// since a typo in a merchant password is otherwise impossible to spot.
class FieldRow extends StatefulWidget {
  const FieldRow({
    super.key,
    required this.label,
    required this.controller,
    required this.onChanged,
    this.keyboard = TextInputType.text,
    this.secret = false,
    this.placeholder,
    this.error,
  });

  final String label;
  final TextEditingController controller;
  final ValueChanged<String> onChanged;
  final TextInputType keyboard;
  final bool secret;
  final String? placeholder;
  final String? error;

  @override
  State<FieldRow> createState() => _FieldRowState();
}

class _FieldRowState extends State<FieldRow> {
  bool _shown = false;

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final error = widget.error;
    return ConstrainedBox(
      constraints: const BoxConstraints(minHeight: 56),
      child: Padding(
        padding: EdgeInsetsDirectional.only(start: 16, end: widget.secret ? 4 : 16),
        child: Row(
          children: [
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 10),
                // One node for the screen reader: the field is announced by its label, then the error.
                child: MergeSemantics(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        widget.label,
                        style: font(StoreFonts.inter, 12, weight: FontWeight.w500, color: StartColors.muted),
                      ),
                      Padding(
                        padding: const EdgeInsets.only(top: 2),
                        child: TextField(
                          controller: widget.controller,
                          onChanged: widget.onChanged,
                          keyboardType: widget.keyboard,
                          obscureText: widget.secret && !_shown,
                          autocorrect: false,
                          enableSuggestions: false,
                          style: _rowText,
                          cursorColor: StartColors.text,
                          decoration: InputDecoration.collapsed(
                            hintText: widget.placeholder,
                            hintStyle: font(StoreFonts.inter, 16, color: _placeholder),
                          ),
                        ),
                      ),
                      if (error != null)
                        Padding(
                          padding: const EdgeInsets.only(top: 4),
                          child: Text(error, style: font(StoreFonts.inter, 12, color: _error)),
                        ),
                    ],
                  ),
                ),
              ),
            ),
            if (widget.secret)
              IconButton(
                tooltip: _shown ? l10n.devHideSecret : l10n.devShowSecret,
                onPressed: () => setState(() => _shown = !_shown),
                icon: Icon(
                  _shown ? Icons.visibility_off_outlined : Icons.visibility_outlined,
                  color: StartColors.muted,
                ),
              ),
          ],
        ),
      ),
    );
  }
}

/// A choice between two values, as a segmented control next to its name.
class SegmentRow<T> extends StatelessWidget {
  const SegmentRow({
    super.key,
    required this.title,
    required this.options,
    required this.selected,
    required this.onSelect,
  });

  final String title;
  final List<(T, String)> options;
  final T selected;
  final ValueChanged<T> onSelect;

  @override
  Widget build(BuildContext context) => ConstrainedBox(
    constraints: const BoxConstraints(minHeight: 56),
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: LayoutBuilder(
        builder: (context, constraints) => Row(
          children: [
            Expanded(child: Text(title, style: _rowText)),
            const SizedBox(width: 12),
            // A long translation on a narrow phone scales the control down instead of pushing it off the row.
            // Not Flexible segments: they would get half the width each and cut the longer label short.
            ConstrainedBox(
              constraints: BoxConstraints(maxWidth: constraints.maxWidth * 0.65),
              child: FittedBox(
                fit: BoxFit.scaleDown,
                child: DecoratedBox(
                  decoration: const BoxDecoration(
                    color: StartColors.selected,
                    borderRadius: BorderRadius.all(Radius.circular(10)),
                  ),
                  child: Padding(
                    padding: const EdgeInsets.all(2),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        for (final (value, label) in options)
                          _Segment(label: label, selected: value == selected, onTap: () => onSelect(value)),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

class _Segment extends StatelessWidget {
  const _Segment({required this.label, required this.selected, required this.onTap});

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Semantics(
    container: true,
    inMutuallyExclusiveGroup: true,
    checked: selected,
    child: GestureDetector(
      behavior: HitTestBehavior.opaque,
      onTap: onTap,
      child: Container(
        height: 36,
        padding: const EdgeInsets.symmetric(horizontal: 12),
        decoration: selected
            ? const BoxDecoration(
                color: StartColors.surface,
                borderRadius: BorderRadius.all(Radius.circular(8)),
                boxShadow: [BoxShadow(color: Color(0x26000000), blurRadius: 2, offset: Offset(0, 1))],
              )
            : null,
        // Not Container.alignment: that stretches the segment to all the width the row offers.
        child: Center(
          widthFactor: 1,
          child: Text(
            label,
            maxLines: 1,
            style: font(
              StoreFonts.inter,
              14,
              weight: selected ? FontWeight.w600 : FontWeight.w400,
              color: StartColors.text,
            ),
          ),
        ),
      ),
    ),
  );
}

/// A choice among several values: the row shows the current one and opens the list in a bottom sheet.
class PickerRow<T> extends StatelessWidget {
  const PickerRow({
    super.key,
    required this.title,
    required this.options,
    required this.selected,
    required this.onSelect,
  });

  final String title;
  final List<(T, String)> options;
  final T selected;
  final ValueChanged<T> onSelect;

  @override
  Widget build(BuildContext context) {
    final current = options.where((option) => option.$1 == selected).firstOrNull?.$2 ?? '';
    return MergeSemantics(
      child: Semantics(
        button: true,
        child: InkWell(
          onTap: () => _open(context),
          child: ConstrainedBox(
            constraints: const BoxConstraints(minHeight: 56),
            child: Padding(
              padding: const EdgeInsetsDirectional.only(start: 16, end: 12),
              child: Row(
                children: [
                  Expanded(child: Text(title, style: _rowText)),
                  Text(current, style: font(StoreFonts.inter, 16, color: StartColors.muted)),
                  const Padding(
                    padding: EdgeInsetsDirectional.only(start: 4),
                    child: Icon(Icons.chevron_right, color: _placeholder, size: 20),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _open(BuildContext context) {
    // A closing route gives focus back to the field that had it, and the keyboard would pop up over the choice.
    FocusManager.instance.primaryFocus?.unfocus();
    return _sheet(context);
  }

  Future<void> _sheet(BuildContext context) => showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    showDragHandle: true,
    backgroundColor: StartColors.surface,
    builder: (sheetContext) => Theme(
      data: StartColors.theme,
      child: SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
              child: Semantics(
                header: true,
                child: Text(
                  title,
                  style: font(StoreFonts.inter, 17, weight: FontWeight.w600, color: StartColors.text),
                ),
              ),
            ),
            Flexible(
              child: ListView(
                shrinkWrap: true,
                padding: const EdgeInsets.only(bottom: 24),
                children: [
                  for (final (value, label) in options)
                    _OptionRow(
                      label: label,
                      selected: value == selected,
                      onTap: () {
                        onSelect(value);
                        Navigator.pop(sheetContext);
                      },
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

class _OptionRow extends StatelessWidget {
  const _OptionRow({required this.label, required this.selected, required this.onTap});

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => MergeSemantics(
    child: Semantics(
      inMutuallyExclusiveGroup: true,
      checked: selected,
      child: InkWell(
        onTap: onTap,
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: 52),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(
              children: [
                Padding(
                  padding: const EdgeInsets.all(12),
                  child: Icon(
                    selected ? Icons.radio_button_checked : Icons.radio_button_unchecked,
                    color: selected ? StartColors.text : StartColors.muted,
                  ),
                ),
                Expanded(child: Text(label, style: _rowText)),
              ],
            ),
          ),
        ),
      ),
    ),
  );
}

/// The last registered order, monospaced, with Copy that turns into "Copied" for a moment.
class LastOrderRow extends StatefulWidget {
  const LastOrderRow({super.key, required this.mdOrder});

  final String? mdOrder;

  @override
  State<LastOrderRow> createState() => _LastOrderRowState();
}

class _LastOrderRowState extends State<LastOrderRow> {
  final _copied = _DoneFlag();

  @override
  void dispose() {
    _copied.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = context.l10n;
    final mdOrder = widget.mdOrder;
    return ConstrainedBox(
      constraints: const BoxConstraints(minHeight: 56),
      child: Padding(
        padding: const EdgeInsetsDirectional.only(start: 16, end: 8),
        child: Row(
          children: [
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(l10n.devLastOrder, style: _rowText),
                    SelectableText(mdOrder ?? l10n.devNone, style: mono(12, color: StartColors.muted)),
                  ],
                ),
              ),
            ),
            if (mdOrder != null)
              ListenableBuilder(
                listenable: _copied,
                builder: (context, _) => _copied.value
                    ? Padding(padding: const EdgeInsets.symmetric(horizontal: 8), child: _DoneMark(l10n.devCopied))
                    : TextButton(
                        onPressed: () {
                          Clipboard.setData(ClipboardData(text: mdOrder));
                          _copied.show();
                        },
                        child: Text(
                          l10n.devCopy,
                          style: font(StoreFonts.inter, 15, weight: FontWeight.w500, color: StartColors.link),
                        ),
                      ),
              ),
          ],
        ),
      ),
    );
  }
}

/// True for [_doneFor] after [show], then false again.
class _DoneFlag extends ValueNotifier<bool> {
  _DoneFlag() : super(false);

  Timer? _timer;

  void show() {
    _timer?.cancel();
    value = true;
    _timer = Timer(_doneFor, () => value = false);
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }
}

class _DoneMark extends StatelessWidget {
  const _DoneMark(this.text);

  final String text;

  @override
  Widget build(BuildContext context) => Row(
    mainAxisSize: MainAxisSize.min,
    children: [
      Text(
        text,
        style: font(StoreFonts.inter, 15, weight: FontWeight.w500, color: _done),
      ),
      const SizedBox(width: 4),
      const Icon(Icons.check, color: _done, size: 18),
    ],
  );
}
