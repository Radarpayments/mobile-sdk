import AsyncStorage from '@react-native-async-storage/async-storage';

import { AppPrefs } from '../src/prefs/appPrefs';

beforeEach(() => AsyncStorage.clear());

test('defaults: the dev gateway, no credentials, a fresh demo buyer', async () => {
  const prefs = await AppPrefs.load();
  expect(prefs.baseUrl).toBe('https://dev.bpcbt.com/payment');
  expect(prefs.merchantLogin).toBe('');
  expect(prefs.merchantPassword).toBe('');
  expect(prefs.apiVersion).toBe('V1');
  expect(prefs.orderType).toBe('PURCHASE');
  expect(prefs.apiKey).toBe('');
  expect(prefs.clientId).toMatch(/^demo-[0-9a-f]{8}-[0-9a-f]{4}$/);
  expect(prefs.scenario).toEqual({ buyerWithCards: false, wallet: true, scanner: true, sdkResultScreen: false });
  expect(prefs.sheetTheme).toBe('cover');
  expect(prefs.localeOverride).toBeUndefined();
  expect(prefs.lastCover).toBeUndefined();
  expect(prefs.applePayMerchantId).toBe(AppPrefs.defaultApplePayMerchantId);
});

test('every setting survives a restart', async () => {
  const prefs = await AppPrefs.load();
  await Promise.all([
    prefs.setBaseUrl('https://gateway.test/payment'),
    prefs.setCredentials({ login: 'merchant-api', password: 'secret' }),
    prefs.setApiVersion('V2'),
    prefs.setOrderType('AFT'),
    prefs.setApiKey('key-1'),
    prefs.setGooglePay({ gateway: 'example', merchantId: 'BCR2' }),
    prefs.setApplePayMerchantId('merchant.test'),
    prefs.setScenario({ buyerWithCards: true, wallet: false, scanner: false, sdkResultScreen: true }),
    prefs.setLastCover('tickets'),
    prefs.setLastMdOrder('md-1'),
    prefs.setSheetTheme('dark'),
    prefs.setLocaleOverride('ar'),
    prefs.setMdOrder('md-2'),
    prefs.setPubKeyPem('PEM'),
    prefs.setBindingId('binding-1'),
    prefs.setSdkCoreVariant('stored'),
  ]);

  const again = await AppPrefs.load();
  expect(again.baseUrl).toBe('https://gateway.test/payment');
  expect([again.merchantLogin, again.merchantPassword]).toEqual(['merchant-api', 'secret']);
  expect([again.apiVersion, again.apiKey]).toEqual(['V2', 'key-1']);
  expect(again.orderType).toBe('AFT');
  expect([again.googlePayGateway, again.googlePayMerchantId, again.applePayMerchantId]).toEqual([
    'example',
    'BCR2',
    'merchant.test',
  ]);
  expect(again.scenario).toEqual({ buyerWithCards: true, wallet: false, scanner: false, sdkResultScreen: true });
  expect(again.clientId).toBe(prefs.clientId);
  expect(again.lastCover).toBe('tickets');
  expect(again.lastMdOrder).toBe('md-1');
  expect(again.sheetTheme).toBe('dark');
  expect(again.localeOverride).toBe('ar');
  expect([again.mdOrder, again.pubKeyPem, again.bindingId, again.sdkCoreVariant]).toEqual([
    'md-2',
    'PEM',
    'binding-1',
    'stored',
  ]);
});

test('a change is seen at once and heard by listeners; undefined removes it', async () => {
  const prefs = await AppPrefs.load();
  const listener = jest.fn();
  prefs.subscribe(listener);
  const saving = prefs.setLocaleOverride('de');
  expect(prefs.localeOverride).toBe('de');
  await saving;
  await prefs.setLocaleOverride(undefined);
  expect(listener).toHaveBeenCalledTimes(2);
  expect((await AppPrefs.load()).localeOverride).toBeUndefined();
});

test('an unknown stored API version is API V1', async () => {
  await AsyncStorage.setItem('store_api_version', 'V3');
  expect((await AppPrefs.load()).apiVersion).toBe('V1');
});

test('the order type is stored as PURCHASE or AFT; anything else is a payment', async () => {
  const prefs = await AppPrefs.load();
  await prefs.setOrderType('AFT');
  expect(await AsyncStorage.getItem('store_order_type')).toBe('AFT');
  await AsyncStorage.setItem('store_order_type', 'P2P');
  expect((await AppPrefs.load()).orderType).toBe('PURCHASE');
});

test('Reset gives a new buyer', async () => {
  const prefs = await AppPrefs.load();
  const before = prefs.clientId;
  await prefs.resetClientId();
  expect(prefs.clientId).not.toBe(before);
});
