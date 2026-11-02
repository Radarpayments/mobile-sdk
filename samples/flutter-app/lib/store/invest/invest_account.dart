import 'dart:convert';

import 'package:flutter/foundation.dart';

/// A fund in the demo portfolio; prices are fixed, [changeBasisPoints] is the month's change (410 = +4.1%).
@immutable
class Holding {
  const Holding(this.ticker, this.valueMinor, this.unitPriceMinor, this.changeBasisPoints);

  final String ticker;
  final int valueMinor;
  final int unitPriceMinor;
  final int changeBasisPoints;

  /// Units of the fund the holding is worth.
  double get quantity => valueMinor / unitPriceMinor;

  Holding withValue(int value) => Holding(ticker, value, unitPriceMinor, changeBasisPoints);

  Map<String, Object> toJson() => {
    'ticker': ticker,
    'valueMinor': valueMinor,
    'unitPriceMinor': unitPriceMinor,
    'changeBasisPoints': changeBasisPoints,
  };

  static Holding fromJson(Map<String, dynamic> json) => Holding(
    json['ticker'] as String,
    json['valueMinor'] as int,
    json['unitPriceMinor'] as int,
    json['changeBasisPoints'] as int,
  );

  @override
  bool operator ==(Object other) =>
      other is Holding &&
      other.ticker == ticker &&
      other.valueMinor == valueMinor &&
      other.unitPriceMinor == unitPriceMinor &&
      other.changeBasisPoints == changeBasisPoints;

  @override
  int get hashCode => Object.hash(ticker, valueMinor, unitPriceMinor, changeBasisPoints);
}

/// Money in from a card, or into a fund.
enum OperationKind { topUp, buy }

/// One line of the account history; [orderId] is the gateway order of a top-up.
@immutable
class InvestOperation {
  const InvestOperation(this.kind, this.amountMinor, this.atEpochMillis, {this.orderId, this.ticker});

  final OperationKind kind;
  final int amountMinor;
  final int atEpochMillis;
  final String? orderId;
  final String? ticker;

  Map<String, Object> toJson() => {
    'kind': kind.name,
    'amountMinor': amountMinor,
    'atEpochMillis': atEpochMillis,
    'orderId': ?orderId,
    'ticker': ?ticker,
  };

  static InvestOperation fromJson(Map<String, dynamic> json) => InvestOperation(
    OperationKind.values.byName(json['kind'] as String),
    json['amountMinor'] as int,
    json['atEpochMillis'] as int,
    orderId: json['orderId'] as String?,
    ticker: json['ticker'] as String?,
  );

  @override
  bool operator ==(Object other) =>
      other is InvestOperation &&
      other.kind == kind &&
      other.amountMinor == amountMinor &&
      other.atEpochMillis == atEpochMillis &&
      other.orderId == orderId &&
      other.ticker == ticker;

  @override
  int get hashCode => Object.hash(kind, amountMinor, atEpochMillis, orderId, ticker);
}

/// The Northwind Invest demo account, kept on the device. Only [credit] brings money in, and only after the SDK
/// reports a completed transfer; [invest] moves it into a fund locally, without a payment.
@immutable
class InvestAccount {
  const InvestAccount({required this.freeCashMinor, required this.holdings, required this.operations});

  final int freeCashMinor;
  final List<Holding> holdings;
  final List<InvestOperation> operations;

  static const globalEtf = 'GLB';
  static const techLeaders = 'TCH';
  static const _minInvestMinor = 100;

  /// The account a fresh install starts with: €4,812.40 in total. Seed operations: 12 Oct 2026.
  static const seed = InvestAccount(
    freeCashMinor: 1240,
    holdings: [Holding(globalEtf, 290000, 23387, 410), Holding(techLeaders, 190000, 37255, 200)],
    operations: [
      InvestOperation(OperationKind.buy, 25000, 1791800400000, ticker: globalEtf),
      InvestOperation(OperationKind.topUp, 25000, 1791800100000, orderId: 'seed'),
    ],
  );

  int get totalMinor => freeCashMinor + holdings.fold(0, (sum, h) => sum + h.valueMinor);

  bool get canInvest => freeCashMinor >= _minInvestMinor;

  InvestAccount copyWith({int? freeCashMinor, List<Holding>? holdings, List<InvestOperation>? operations}) =>
      InvestAccount(
        freeCashMinor: freeCashMinor ?? this.freeCashMinor,
        holdings: holdings ?? this.holdings,
        operations: operations ?? this.operations,
      );

  /// Adds a completed top-up of order [orderId]; a second result of the same order changes nothing.
  InvestAccount credit(String orderId, int amountMinor, int at) {
    if (operations.any((op) => op.kind == OperationKind.topUp && op.orderId == orderId)) return this;
    return copyWith(
      freeCashMinor: freeCashMinor + amountMinor,
      operations: [
        InvestOperation(OperationKind.topUp, amountMinor, at, orderId: orderId),
        ...operations,
      ],
    );
  }

  /// Buys [ticker] for [amountMinor], at most the free cash; nothing when there is less than a euro to invest.
  InvestAccount invest(int amountMinor, int at, {String ticker = globalEtf}) {
    if (!canInvest) return this;
    final amount = amountMinor < freeCashMinor ? amountMinor : freeCashMinor;
    if (amount <= 0) return this;
    return copyWith(
      freeCashMinor: freeCashMinor - amount,
      holdings: [for (final h in holdings) h.ticker == ticker ? h.withValue(h.valueMinor + amount) : h],
      operations: [
        InvestOperation(OperationKind.buy, amount, at, ticker: ticker),
        ...operations,
      ],
    );
  }

  /// The latest [limit] operations, newest first.
  List<InvestOperation> recent([int limit = 5]) => operations.take(limit).toList();

  /// The account as kept in the app settings.
  String toJson() => jsonEncode({
    'freeCashMinor': freeCashMinor,
    'holdings': [for (final h in holdings) h.toJson()],
    'operations': [for (final op in operations) op.toJson()],
  });

  /// The account saved with [toJson]; the seed when there is none or it cannot be read.
  static InvestAccount fromJson(String? json) {
    if (json == null || json.isEmpty) return seed;
    try {
      final o = jsonDecode(json) as Map<String, dynamic>;
      return InvestAccount(
        freeCashMinor: o['freeCashMinor'] as int,
        holdings: [for (final h in o['holdings'] as List) Holding.fromJson(h as Map<String, dynamic>)],
        operations: [for (final op in o['operations'] as List) InvestOperation.fromJson(op as Map<String, dynamic>)],
      );
    } on Object {
      return seed;
    }
  }

  @override
  bool operator ==(Object other) =>
      other is InvestAccount &&
      other.freeCashMinor == freeCashMinor &&
      listEquals(other.holdings, holdings) &&
      listEquals(other.operations, operations);

  @override
  int get hashCode => Object.hash(freeCashMinor, Object.hashAll(holdings), Object.hashAll(operations));
}
