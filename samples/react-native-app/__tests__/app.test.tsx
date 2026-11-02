import AsyncStorage from '@react-native-async-storage/async-storage';
import React from 'react';
import { BackHandler, Text, TextInput } from 'react-native';
import ReactTestRenderer, {
  act,
  type ReactTestInstance,
  type ReactTestRenderer as Renderer,
} from 'react-test-renderer';

import { gatewayRegistrar, type AppScopeValue } from '../src/AppScope';
import type { PresentSheet } from '../src/checkout/checkout';
import { AppPrefs } from '../src/prefs/appPrefs';
import { SampleApp } from '../src/SampleApp';
import { StoreModel } from '../src/store/storeModel';

let mounted: Renderer | undefined;

async function renderApp(scope: Partial<AppScopeValue> = {}) {
  const value: AppScopeValue = {
    prefs: await AppPrefs.load(),
    store: new StoreModel(),
    registrarFactory: () => ({ register: async () => ({ kind: 'registered', mdOrder: 'md-1' }) }),
    ...scope,
  };
  let app!: Renderer;
  await act(async () => {
    app = ReactTestRenderer.create(<SampleApp scope={value} />);
  });
  mounted = app;
  return { app, scope: value };
}

function texts(app: Renderer): string[] {
  return app.root.findAllByType(Text).map(node => [node.props.children].flat().join(''));
}

/** The pressable with `testID` (the first match: the component, not the host views it renders). */
function byTestId(app: Renderer, testID: string): ReactTestInstance {
  const [node] = app.root.findAll(candidate => candidate.props.testID === testID && candidate.props.onPress);
  if (!node) throw new Error(`No ${testID}`);
  return node;
}

/** The settings chip that reads `label`. */
function chip(app: Renderer, label: string): ReactTestInstance {
  return app.root.find(
    node =>
      node.props.accessibilityRole === 'radio' &&
      node.props.onPress &&
      node.findAllByType(Text).some(text => text.props.children === label),
  );
}

async function press(app: Renderer, testID: string) {
  await act(async () => byTestId(app, testID).props.onPress());
}

beforeEach(() => AsyncStorage.clear());

// The seat hold ticks every second until its screen goes.
afterEach(() => act(() => mounted?.unmount()));

test('the start screen shows the three stores and SDK Core', async () => {
  const { app } = await renderApp();
  expect(texts(app)).toEqual(
    expect.arrayContaining(['Payment demo', 'SDK Payment', 'Hearth & Co', 'Roast Lane', 'Stage Pass', 'SDK Core']),
  );
});

test('the language setting switches the demo at once', async () => {
  const { app, scope } = await renderApp();
  await act(() => scope.prefs.setLocaleOverride('ru'));
  expect(texts(app)).toContain('Демо оплаты');
});

test('a furniture order: product → cart → Pay → the store result screen', async () => {
  const present = jest.fn<ReturnType<PresentSheet>, Parameters<PresentSheet>>(async () => ({
    status: 'completed',
    orderId: 'md-1',
  }));
  const { app, scope } = await renderApp({ presentSheet: present });

  await press(app, 'cover_furniture');
  expect(scope.prefs.lastCover).toBe('furniture');
  await press(app, 'furniture_velvet_sofa');
  await press(app, 'furniture_add');
  await press(app, 'furniture_cart_bar');
  expect(texts(app)).toContain('Pay €1,249.00');

  await press(app, 'store_pay');
  expect(present).toHaveBeenCalledWith(
    expect.objectContaining({ theme: expect.objectContaining({ mode: 'custom' }) }),
    { kind: 'registered', mdOrder: 'md-1' },
  );
  expect(texts(app)).toEqual(expect.arrayContaining(['Thank you for your order', 'md-1']));
  expect(scope.store.cart('furniture').isEmpty).toBe(true);
  expect(scope.prefs.lastMdOrder).toBe('md-1');
});

test('without credentials Pay explains why and leads to the settings', async () => {
  const { app } = await renderApp({
    registrarFactory: () => ({ register: async () => ({ kind: 'missingCredentials' }) }),
  });
  await press(app, 'cover_tickets');
  await press(app, 'tickets_next');
  await press(app, 'store_pay');
  expect(texts(app)).toContain('Merchant login and password are not set.');
});

test('without an API key Pay says so', async () => {
  const { app } = await renderApp({
    registrarFactory: () => ({ register: async () => ({ kind: 'missingApiKey' }) }),
  });
  await press(app, 'cover_tickets');
  await press(app, 'tickets_next');
  await press(app, 'store_pay');
  expect(texts(app)).toContain('The API key is not set.');
});

test('the developer settings switch the merchant API: login and password for V1, the API key for V2', async () => {
  const { app, scope } = await renderApp();
  const fields = () => app.root.findAll(node => node.type === TextInput).map(node => node.props.testID);
  await press(app, 'start_settings');
  expect(texts(app)).toEqual(expect.arrayContaining(['Merchant API', 'API V1', 'API V2']));
  expect(fields()).toEqual(expect.arrayContaining(['dev_login', 'dev_password']));
  expect(fields()).not.toContain('dev_api_key');

  await act(async () => chip(app, 'API V2').props.onPress());
  expect(scope.prefs.apiVersion).toBe('V2');
  expect(fields()).toContain('dev_api_key');
  expect(fields()).not.toContain('dev_login');
  expect(texts(app)).toContain('Sessions are created with POST /api2/sessions using the API key.');
  const keyField = app.root.find(node => node.type === TextInput && node.props.testID === 'dev_api_key');
  expect(keyField.props.secureTextEntry).toBe(true);
  await act(async () => keyField.props.onChangeText('key-1'));
  expect(scope.prefs.apiKey).toBe('key-1');
  expect((await AppPrefs.load()).apiVersion).toBe('V2');
});

test('API V1 orders are payments or AFT transfers; the transfer note shows with AFT only', async () => {
  const { app, scope } = await renderApp();
  const hint =
    'Transfers are registered with registerP2P.do (one-leg AFT): the sheet asks for the sender and recipient details.';
  await press(app, 'start_settings');
  expect(texts(app)).toEqual(expect.arrayContaining(['Order', 'Payment', 'AFT transfer']));
  expect(texts(app)).not.toContain(hint);

  await act(async () => chip(app, 'AFT transfer').props.onPress());
  expect(scope.prefs.orderType).toBe('AFT');
  expect(texts(app)).toContain(hint);
  expect((await AppPrefs.load()).orderType).toBe('AFT');

  await act(async () => chip(app, 'API V2').props.onPress());
  expect(texts(app)).not.toEqual(expect.arrayContaining(['AFT transfer']));
  expect(texts(app)).not.toContain(hint);
});

test('the registrar of the settings registers an AFT order with registerP2P.do', async () => {
  const prefs = await AppPrefs.load();
  await Promise.all([
    prefs.setBaseUrl('https://gateway.example/payment'),
    prefs.setCredentials({ login: 'merchant-api', password: 'secret' }),
    prefs.setOrderType('AFT'),
  ]);
  const reply = Uint8Array.from('{"orderId":"md-aft"}', char => char.charCodeAt(0));
  const fetch = jest
    .spyOn(globalThis, 'fetch')
    .mockResolvedValue({ arrayBuffer: async () => reply.buffer } as Response);
  try {
    await expect(gatewayRegistrar(prefs).register(100, undefined)).resolves.toEqual({
      kind: 'registered',
      mdOrder: 'md-aft',
    });
    expect(fetch.mock.calls[0][0]).toBe('https://gateway.example/payment/rest/api/p2p/registerP2P.do');
  } finally {
    fetch.mockRestore();
  }
});

test('Back from the order goes to the menu, then out of the store', async () => {
  type BackListener = Parameters<typeof BackHandler.addEventListener>[1];
  const backListeners: BackListener[] = [];
  jest.spyOn(BackHandler, 'addEventListener').mockImplementation((_, listener) => {
    backListeners.push(listener);
    return { remove: () => backListeners.splice(backListeners.indexOf(listener), 1) };
  });
  const back = () =>
    act(async () => {
      backListeners.forEach(listener => listener({} as Parameters<BackListener>[0]));
    });
  const { app, scope } = await renderApp();

  await press(app, 'cover_coffee');
  await press(app, 'coffee_add_flat_white');
  await press(app, 'coffee_order_pill');
  expect(texts(app)).toContain('Your order');

  await back();
  expect(texts(app)).not.toContain('Your order');
  expect(texts(app)).toContain('King St.');
  expect(scope.prefs.lastCover).toBe('coffee');

  await back();
  await act(() => new Promise(done => setTimeout(done, 500)));
  expect(texts(app)).not.toContain('King St.');
  expect(scope.prefs.lastCover).toBeUndefined();
  expect(scope.store.cart('coffee').count).toBe(1);
});
