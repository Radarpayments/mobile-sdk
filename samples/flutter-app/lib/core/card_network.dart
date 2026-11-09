import 'package:flutter/services.dart';

/// Card networks the form tells apart by the leading digits, as the SDK's own form does
/// (`CardNetworkResolver` in sdk-payment).
enum CardNetwork {
  visa('Visa'),
  mastercard('Mastercard'),
  amex('Amex'),
  unionPay('UnionPay'),
  diners('Diners Club'),
  jcb('JCB'),
  discover('Discover'),
  maestro('Maestro'),
  unknown('');

  const CardNetwork(this.label);

  final String label;

  /// The network of a partial or complete card number.
  static CardNetwork of(String pan) {
    final digits = pan.replaceAll(DigitsFormatter._nonDigit, '');
    int? prefix(int length) => digits.length >= length ? int.parse(digits.substring(0, length)) : null;
    final (two, three, four) = (prefix(2), prefix(3), prefix(4));
    bool within(int? value, int from, int to) => value != null && value >= from && value <= to;
    return switch (digits) {
      '' => unknown,
      _ when digits.startsWith('4') => visa,
      _ when two == 34 || two == 37 => amex,
      _ when within(two, 51, 55) || within(four, 2221, 2720) => mastercard,
      _ when two == 62 => unionPay,
      _ when two == 36 || two == 38 || two == 39 || within(three, 300, 305) || four == 3095 => diners,
      _ when within(four, 3528, 3589) => jcb,
      _ when four == 6011 || within(three, 644, 649) || two == 65 => discover,
      _ when two == 50 || within(two, 56, 58) || four == 6304 || four == 6759 || within(four, 6761, 6763) => maestro,
      _ => unknown,
    };
  }

  /// Longest number the network issues; typing beyond it is dropped.
  int get maxPanLength => switch (this) {
    amex => 15,
    mastercard => 16,
    _ => 19,
  };

  /// CVC printed on its cards: 4 digits on the front for Amex, 3 on the back otherwise.
  int get cvcLength => this == amex ? 4 : 3;

  /// [digits] in the network's groups: "4111 1111 1111 1111", Amex 4-6-5.
  String group(String digits) {
    final sizes = this == amex ? const [4, 6, 5] : const [4, 4, 4, 4, 3];
    final groups = <String>[];
    var at = 0;
    for (final size in sizes) {
      if (at >= digits.length) break;
      final end = (at + size).clamp(0, digits.length);
      groups.add(digits.substring(at, end));
      at = end;
    }
    return groups.join(' ');
  }
}

/// Keeps the digits of what is typed, at most [maxDigits] of them, and shows them through [format];
/// the cursor stays after the same digit, so it never lands on the wrong side of a space or slash.
class DigitsFormatter extends TextInputFormatter {
  DigitsFormatter({required this.maxDigits, this.format = _plain});

  final int Function(String digits) maxDigits;
  final String Function(String digits) format;

  static String _plain(String digits) => digits;

  static final _nonDigit = RegExp(r'\D');

  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    final typed = newValue.text.replaceAll(_nonDigit, '');
    final digits = typed.substring(0, typed.length.clamp(0, maxDigits(typed)));
    final cursor = newValue.selection.isValid ? newValue.selection.baseOffset : newValue.text.length;
    final digitsBeforeCursor = newValue.text
        .substring(0, cursor)
        .replaceAll(_nonDigit, '')
        .length
        .clamp(0, digits.length);
    final text = format(digits);
    var offset = 0;
    for (var seen = 0; seen < digitsBeforeCursor; offset++) {
      if (!_nonDigit.hasMatch(text[offset])) seen++;
    }
    return TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: offset),
    );
  }
}

/// Typed expiry as `MM/YY`: digits only, the slash added after the month.
String formatExpiry(String digits) => digits.length > 2 ? '${digits.substring(0, 2)}/${digits.substring(2)}' : digits;
