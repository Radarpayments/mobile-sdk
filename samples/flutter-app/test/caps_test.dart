import 'package:flutter_test/flutter_test.dart';
import 'package:payrdr_flutter_sample/caps.dart';

void main() {
  test('greek capitals lose the stress mark', () {
    expect(toCaps('Πύλη πληρωμών'), 'ΠΥΛΗ ΠΛΗΡΩΜΩΝ');
  });

  test('other accents stay', () {
    expect(toCaps('étape île'), 'ÉTAPE ÎLE');
  });
}
