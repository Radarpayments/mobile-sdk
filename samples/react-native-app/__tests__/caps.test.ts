import { toCaps } from '../src/ui/caps';

test('greek capitals lose the stress mark', () => {
  expect(toCaps('Πύλη πληρωμών')).toBe('ΠΥΛΗ ΠΛΗΡΩΜΩΝ');
});

test('other accents stay', () => {
  expect(toCaps('étape île')).toBe('ÉTAPE ÎLE');
});
