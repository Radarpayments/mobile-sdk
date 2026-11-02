import 'package:flutter/foundation.dart';

/// The amount typed in the top-up field, in euros with `.` as the separator whatever the locale shows: at most six
/// whole digits and two decimals, no leading zeros.
@immutable
class TopUpAmount {
  const TopUpAmount([this.text = '100']);

  final String text;

  static const min = 1000;
  static const max = 100000;
  static const presets = [5000, 10000, 25000, 50000];
  static const _decimals = 2;
  static const _wholeDigits = 6;

  /// Euro cents, or null while nothing is typed.
  int? get minor {
    if (text.isEmpty) return null;
    final dot = text.indexOf('.');
    final whole = int.tryParse(dot < 0 ? text : text.substring(0, dot)) ?? 0;
    final cents = dot < 0 ? 0 : int.parse(text.substring(dot + 1).padRight(2, '0'));
    return whole * 100 + cents;
  }

  bool get valid => switch (minor) {
    final m? => m >= min && m <= max,
    null => false,
  };

  /// [c] typed at the end: a digit or `.`; anything that would break the format is ignored.
  TopUpAmount type(String c) {
    final dot = text.indexOf('.');
    if (c == '.') return dot >= 0 ? this : TopUpAmount('${text.isEmpty ? '0' : text}.');
    if (!RegExp(r'^[0-9]$').hasMatch(c)) return this;
    if (dot >= 0) return text.length - dot > _decimals ? this : TopUpAmount(text + c);
    if (text == '0') return TopUpAmount(c);
    return text.length >= _wholeDigits ? this : TopUpAmount(text + c);
  }

  /// What the system keyboard left in the field, cleaned as if typed key by key: the locale's separator (`,` or the
  /// Arabic `٫`) becomes `.`, Arabic-Indic and Persian digits become ASCII ones, anything else that does not fit the
  /// format is dropped.
  static TopUpAmount parse(String input) =>
      input.split('').fold(const TopUpAmount(''), (amount, c) => amount.type(_ascii(c)));

  static String _ascii(String c) {
    if (c == ',' || c == '\u066B') return '.';
    final code = c.codeUnitAt(0);
    for (final zero in const [0x0660, 0x06F0]) {
      if (code >= zero && code <= zero + 9) return '${code - zero}';
    }
    return c;
  }

  /// The amount of a preset chip.
  static TopUpAmount preset(int minor) {
    final cents = minor % 100;
    final whole = '${minor ~/ 100}';
    if (cents == 0) return TopUpAmount(whole);
    return TopUpAmount('$whole.${cents.toString().padLeft(2, '0').replaceAll(RegExp(r'0+$'), '')}');
  }

  @override
  bool operator ==(Object other) => other is TopUpAmount && other.text == text;

  @override
  int get hashCode => text.hashCode;
}

/// What the amount field lets through: digits (ASCII, Arabic-Indic, Persian) and the separators [TopUpAmount.parse] reads.
final amountCharacters = RegExp('[0-9.,\u066B\u0660-\u0669\u06F0-\u06F9]');

/// Where the top-up screen is in the transfer.
sealed class TopUpStatus {
  const TopUpStatus();

  /// Whether "Continue" may start a transfer of [amount] now.
  bool canContinue(TopUpAmount amount) => amount.valid && (this is TopUpEditing || this is TopUpDeclined);
}

final class TopUpEditing extends TopUpStatus {
  const TopUpEditing();

  @override
  bool operator ==(Object other) => other is TopUpEditing;

  @override
  int get hashCode => (TopUpEditing).hashCode;
}

/// The bank or the gateway refused; [message] is the SDK's text, when it has one.
final class TopUpDeclined extends TopUpStatus {
  const TopUpDeclined(this.message);

  final String? message;

  @override
  bool operator ==(Object other) => other is TopUpDeclined && other.message == message;

  @override
  int get hashCode => message.hashCode;
}

/// How the transfer ended is not known: the money is not credited and the app does not offer to pay again.
final class TopUpUnconfirmed extends TopUpStatus {
  const TopUpUnconfirmed();

  @override
  bool operator ==(Object other) => other is TopUpUnconfirmed;

  @override
  int get hashCode => (TopUpUnconfirmed).hashCode;
}

/// Credited: [amountMinor] of order [orderId] took free cash from [freeCashBefore] to [freeCashAfter] at [at].
final class TopUpDone extends TopUpStatus {
  const TopUpDone(this.orderId, this.amountMinor, this.freeCashBefore, this.freeCashAfter, this.at);

  final String orderId;
  final int amountMinor;
  final int freeCashBefore;
  final int freeCashAfter;
  final int at;

  @override
  bool operator ==(Object other) =>
      other is TopUpDone &&
      other.orderId == orderId &&
      other.amountMinor == amountMinor &&
      other.freeCashBefore == freeCashBefore &&
      other.freeCashAfter == freeCashAfter &&
      other.at == at;

  @override
  int get hashCode => Object.hash(orderId, amountMinor, freeCashBefore, freeCashAfter, at);
}
