import 'package:flutter/foundation.dart';

import '../../checkout/checkout.dart';
import 'invest_account.dart';
import 'top_up_amount.dart';

/// Northwind Invest: the account shown (the saved one), the amount in the top-up field and how the last transfer
/// ended. Money is credited only when the SDK reports the transfer completed.
class InvestModel extends ChangeNotifier {
  InvestModel({required InvestAccount Function() load, required this.save, int Function()? clock})
    : _load = load,
      _clock = clock ?? (() => DateTime.now().millisecondsSinceEpoch),
      _account = load();

  final InvestAccount Function() _load;
  final void Function(InvestAccount) save;
  final int Function() _clock;

  InvestAccount _account;
  TopUpAmount _amount = const TopUpAmount();
  TopUpStatus _status = const TopUpEditing();

  InvestAccount get account => _account;
  TopUpAmount get amount => _amount;
  TopUpStatus get status => _status;

  /// The account as saved on the device (it changes after a reset in the developer settings).
  void reload() {
    _account = _load();
    notifyListeners();
  }

  /// The amount field changed.
  void edit(String input) {
    _amount = TopUpAmount.parse(input);
    notifyListeners();
  }

  /// A preset chip.
  void preset(int minor) {
    _amount = TopUpAmount.preset(minor);
    notifyListeners();
  }

  /// The sheet closed after a transfer of [amountMinor].
  void onOutcome(CheckoutOutcome outcome, int amountMinor) {
    switch (outcome) {
      case Success(:final paymentId):
        final before = _account;
        final after = before.credit(paymentId, amountMinor, _clock());
        if (after == before) return;
        _update(after);
        _status = TopUpDone(paymentId, amountMinor, before.freeCashMinor, after.freeCashMinor, _clock());
      case Declined(:final message):
        _status = TopUpDeclined(message);
      case Unconfirmed():
        _status = const TopUpUnconfirmed();
      case Cancelled():
        _status = const TopUpEditing();
    }
    notifyListeners();
  }

  /// The sheet of "Add card" closed: the account does not change, whatever the outcome.
  void onCardOutcome(CheckoutOutcome outcome) {
    _status = switch (outcome) {
      Success() => const TopUpCardAdded(),
      Declined(:final message) => TopUpCardDeclined(message),
      Unconfirmed() => const TopUpCardUnconfirmed(),
      Cancelled() => const TopUpEditing(),
    };
    notifyListeners();
  }

  /// Buys the Global ETF for [amountMinor] (at most the free cash), without a payment.
  void invest(int amountMinor) {
    _update(_account.invest(amountMinor, _clock()));
    notifyListeners();
  }

  /// The buyer left the top-up screen: its banner goes, the amount stays.
  void leaveTopUp() {
    _status = const TopUpEditing();
    notifyListeners();
  }

  void _update(InvestAccount next) {
    if (next == _account) return;
    _account = next;
    save(next);
  }
}
