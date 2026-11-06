import 'package:flutter_test/flutter_test.dart';
import 'package:payrdr_flutter_sample/checkout/checkout.dart';
import 'package:payrdr_flutter_sample/merchant/order_registrar.dart';
import 'package:payrdr_flutter_sample/store/invest/invest_account.dart';
import 'package:payrdr_flutter_sample/store/invest/invest_mode.dart';
import 'package:payrdr_flutter_sample/store/invest/invest_model.dart';
import 'package:payrdr_flutter_sample/store/invest/top_up_amount.dart';
import 'package:payrdr_flutter_sample/store/model/catalog.dart';

void main() {
  const seed = InvestAccount.seed;
  const now = 1793280000000;

  group('InvestAccount', () {
    test('the seed totals 4812.40', () {
      expect(seed.totalMinor, 481240);
      expect(seed.freeCashMinor, 1240);
    });

    test('credit adds to free cash and prepends a top-up', () {
      final account = seed.credit('md-1', 10000, now);
      expect(account.freeCashMinor, 11240);
      expect(account.totalMinor, 491240);
      expect(account.operations.first, const InvestOperation(OperationKind.topUp, 10000, now, orderId: 'md-1'));
    });

    test('credit twice with the same order credits once', () {
      final account = seed.credit('md-1', 10000, now).credit('md-1', 10000, now + 1);
      expect(account.freeCashMinor, 11240);
      expect(account.operations.length, seed.operations.length + 1);
    });

    test('invest moves cash into GLB', () {
      final account = seed.credit('md-1', 10000, now).invest(10000, now + 1);
      expect(account.freeCashMinor, 1240);
      expect(account.holdings.firstWhere((h) => h.ticker == 'GLB').valueMinor, 300000);
      expect(account.operations.first, const InvestOperation(OperationKind.buy, 10000, now + 1, ticker: 'GLB'));
    });

    test('invest more than free cash invests all free cash; under a euro does nothing', () {
      expect(seed.invest(1000000, now).freeCashMinor, 0);
      final poor = seed.copyWith(freeCashMinor: 99);
      expect(poor.canInvest, isFalse);
      expect(poor.invest(99, now), poor);
    });

    test('recent operations are newest first and capped', () {
      var account = seed;
      for (var i = 0; i < 6; i++) {
        account = account.credit('md-$i', 1000, now + i);
      }
      expect(account.recent().length, 5);
      expect(account.recent().first.orderId, 'md-5');
    });

    test('json round trip; corrupt json gives the seed', () {
      final account = seed.credit('md-1', 10000, now).invest(5000, now + 1);
      expect(InvestAccount.fromJson(account.toJson()), account);
      for (final json in [null, '', '{', '{"freeCashMinor":"x"}', '[]', '{"freeCashMinor":1}']) {
        expect(InvestAccount.fromJson(json), seed, reason: 'json: $json');
      }
    });
  });

  group('TopUpAmount', () {
    test('default is 100 euros and valid', () {
      expect(const TopUpAmount().minor, 10000);
      expect(const TopUpAmount().valid, isTrue);
    });

    test('text from the system keyboard is cleaned', () {
      expect(TopUpAmount.parse('12,345').text, '12.34');
      expect(TopUpAmount.parse('12.3.4').text, '12.34');
      expect(TopUpAmount.parse('0005').text, '5');
      expect(TopUpAmount.parse(',5').text, '0.5');
      expect(TopUpAmount.parse('.').text, '0.');
      expect(TopUpAmount.parse('-').text, '');
      expect(TopUpAmount.parse('1234567').text, '123456');
      expect(TopUpAmount.parse('10.').minor, 1000);
    });

    test('limits are 10 to 1000 euros; empty has no amount', () {
      expect(TopUpAmount.parse('9.99').valid, isFalse);
      expect(TopUpAmount.parse('10').valid, isTrue);
      expect(TopUpAmount.parse('1000').valid, isTrue);
      expect(TopUpAmount.parse('1000.01').valid, isFalse);
      expect(TopUpAmount.parse('').minor, isNull);
      expect(TopUpAmount.parse('0').valid, isFalse);
    });

    test('preset sets whole euros', () {
      expect(TopUpAmount.preset(25000).text, '250');
      expect(TopUpAmount.preset(1250).text, '12.5');
    });

    test('unconfirmed blocks continue', () {
      expect(const TopUpUnconfirmed().canContinue(const TopUpAmount()), isFalse);
      expect(const TopUpCardUnconfirmed().canContinue(const TopUpAmount()), isTrue);
      expect(const TopUpDeclined(null).canContinue(const TopUpAmount()), isTrue);
      expect(const TopUpEditing().canContinue(const TopUpAmount()), isTrue);
      expect(const TopUpEditing().canContinue(TopUpAmount.parse('5')), isFalse);
    });
  });

  group('mode', () {
    test('the AFT demo is API V1 with AFT orders', () {
      expect(isAftDemo(ApiVersion.v1, OrderType.aft), isTrue);
      expect(isAftDemo(ApiVersion.v2, OrderType.aft), isFalse);
      expect(isAftDemo(ApiVersion.v1, OrderType.purchase), isFalse);
    });

    test('the AFT demo shows only Northwind, otherwise the stores', () {
      expect(visibleCovers(aftDemo: true), [CoverId.invest]);
      expect(visibleCovers(aftDemo: false), [CoverId.furniture, CoverId.coffee, CoverId.tickets]);
    });

    test('the last business reopens only when the mode shows it', () {
      expect(reopenCover(CoverId.furniture, aftDemo: true), isNull);
      expect(reopenCover(CoverId.invest, aftDemo: false), isNull);
      expect(reopenCover(CoverId.invest, aftDemo: true), CoverId.invest);
      expect(reopenCover(null, aftDemo: true), isNull);
    });
  });

  group('InvestModel', () {
    test('a success credits once, saves and shows done', () {
      final saved = <InvestAccount>[];
      final model = InvestModel(load: () => seed, save: saved.add, clock: () => 1000);
      model.onOutcome(const Success('md-1'), 10000);
      model.onOutcome(const Success('md-1'), 10000);
      expect(model.account.freeCashMinor, 11240);
      expect(saved.last.freeCashMinor, 11240);
      expect(model.status, const TopUpDone('md-1', 10000, 1240, 11240, 1000));
    });

    test('declined and unconfirmed keep the balance; cancel goes back to editing', () {
      final model = InvestModel(load: () => seed, save: (_) => fail('nothing to save'), clock: () => 1000);
      model.onOutcome(const Declined('Issuer declined'), 10000);
      expect(model.status, const TopUpDeclined('Issuer declined'));
      model.onOutcome(const Unconfirmed('md-2'), 10000);
      expect(model.status, const TopUpUnconfirmed());
      expect(model.account.freeCashMinor, 1240);
      model.onOutcome(const Cancelled(), 10000);
      expect(model.status, const TopUpEditing());
    });

    test('a saved card credits nothing and the top-up may go on', () {
      final model = InvestModel(load: () => seed, save: (_) => fail('nothing to save'), clock: () => 1000);
      model.onCardOutcome(const Success('md-verify'));
      expect(model.account, seed);
      expect(model.status, const TopUpCardAdded());
      model.edit('100');
      expect(model.status.canContinue(model.amount), isTrue);
    });

    test('a card the bank refused or did not confirm leaves the top-up open', () {
      final model = InvestModel(load: () => seed, save: (_) => fail('nothing to save'), clock: () => 1000);
      model.edit('100');
      model.onCardOutcome(const Declined('Issuer declined'));
      expect(model.status, const TopUpCardDeclined('Issuer declined'));
      expect(model.status.canContinue(model.amount), isTrue);
      model.onCardOutcome(const Unconfirmed('md-verify'));
      expect(model.status, const TopUpCardUnconfirmed());
      expect(model.status.canContinue(model.amount), isTrue);
      expect(model.account, seed);
      model.onCardOutcome(const Cancelled());
      expect(model.status, const TopUpEditing());
    });

    test('the field and the presets edit the amount', () {
      final model = InvestModel(load: () => seed, save: (_) {}, clock: () => 1000);
      model.edit('2505,5');
      expect(model.amount.text, '2505.5');
      model.preset(5000);
      expect(model.amount.minor, 5000);
    });
  });

  group('Arabic input', () {
    test('parse reads the Arabic decimal separator', () {
      expect(TopUpAmount.parse('12\u066B5').text, '12.5');
    });

    test('parse reads Arabic-Indic and Persian digits as ASCII', () {
      expect(TopUpAmount.parse('\u0661\u0662\u066B\u0665').text, '12.5');
      expect(TopUpAmount.parse('\u06F3\u06F0').text, '30');
    });

    test('the field lets the Arabic separator and digits through', () {
      for (final c in ['\u066B', '\u0665', '\u06F5', '5', '.', ',']) {
        expect(amountCharacters.hasMatch(c), isTrue, reason: c);
      }
      expect(amountCharacters.hasMatch('-'), isFalse);
    });
  });

  group('mode switched under an open business', () {
    test('an open business stays only while the mode still shows it', () {
      expect(coverFits(CoverId.invest, aftDemo: true), isTrue);
      expect(coverFits(CoverId.invest, aftDemo: false), isFalse);
      expect(coverFits(CoverId.tickets, aftDemo: true), isFalse);
      expect(coverFits(CoverId.tickets, aftDemo: false), isTrue);
    });
  });
}
