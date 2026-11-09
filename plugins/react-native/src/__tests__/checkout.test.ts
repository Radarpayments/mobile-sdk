import { calls, native } from './react-native.mock';
import { PayrdrCheckout, PayrdrCore } from '../index';

function nativeReplies(reply: unknown) {
  calls.length = 0;
  native.reply = reply;
}

const sent = () => calls[0]!.request;

describe('checkout request', () => {
  it('sends the config as given with a system theme by default and the order', async () => {
    nativeReplies({ status: 'canceled', orderId: 'md-1' });

    await new PayrdrCheckout({ baseUrl: 'https://pay.example.com', locale: 'de' }).present('md-1');

    expect(calls[0]!.method).toBe('present');
    expect(sent()).toEqual({
      config: { baseUrl: 'https://pay.example.com', locale: 'de', theme: { mode: 'system' } },
      mdOrder: 'md-1',
    });
  });

  it('sends a session as sessionId', async () => {
    nativeReplies({ status: 'canceled', orderId: '' });

    await new PayrdrCheckout({ baseUrl: 'u' }).presentSession('ps_1');

    expect(sent().sessionId).toBe('ps_1');
    expect(sent().mdOrder).toBeUndefined();
  });

  it('turns CSS colours into the ARGB hex of the native side', async () => {
    nativeReplies({ status: 'canceled', orderId: 'md-1' });

    await new PayrdrCheckout({
      baseUrl: 'u',
      theme: { mode: 'custom', base: 'dark', colors: { primary: '#f3d9b1', onPrimary: '#fff', scrim: '#00000080' }, cornerRadius: 18 },
    }).present('md-1');

    expect((sent().config as Record<string, unknown>).theme).toEqual({
      mode: 'custom',
      base: 'dark',
      colors: { primary: '#FFF3D9B1', onPrimary: '#FFFFFFFF', scrim: '#80000000' },
      cornerRadius: 18,
    });
  });

  it('rejects a colour that is not hex before anything opens', async () => {
    nativeReplies({ status: 'canceled', orderId: 'md-1' });

    await expect(
      new PayrdrCheckout({ baseUrl: 'u', theme: { mode: 'custom', colors: { primary: 'orange' } } }).present('md-1'),
    ).rejects.toThrow('theme.colors.primary');
    expect(calls).toHaveLength(0);
  });
});

describe('checkout result', () => {
  const resultOf = (reply: unknown) => {
    nativeReplies(reply);
    return new PayrdrCheckout({ baseUrl: 'u' }).present('md-1');
  };

  it('keeps each status and the order', async () => {
    expect(await resultOf({ status: 'completed', orderId: 'md-1' })).toEqual({ status: 'completed', orderId: 'md-1' });
    expect(await resultOf({ status: 'canceled', orderId: 'md-1' })).toEqual({ status: 'canceled', orderId: 'md-1' });
    expect(await resultOf({ status: 'unknown', orderId: 'md-1' })).toEqual({ status: 'unknown', orderId: 'md-1' });
  });

  it('carries a decline with its text and bank code', async () => {
    expect(
      await resultOf({
        status: 'failed',
        orderId: 'md-1',
        error: { code: 'declined', message: 'Insufficient funds', declineCode: '116' },
      }),
    ).toEqual({
      status: 'failed',
      orderId: 'md-1',
      error: { code: 'declined', message: 'Insufficient funds', declineCode: '116' },
    });
  });

  it('keeps the code of an expired session', async () => {
    const result = await resultOf({ status: 'failed', orderId: 'md-1', error: { code: 'session_expired' } });

    expect(result.status === 'failed' && result.error.code).toBe('session_expired');
  });

  it('turns a code this version does not know into unknown', async () => {
    const result = await resultOf({ status: 'failed', orderId: 'md-1', error: { code: 'something_new' } });

    expect(result.status === 'failed' && result.error.code).toBe('unknown');
  });
});

describe('SDK Core', () => {
  it('sends a new card for an order with its native type', async () => {
    nativeReplies({ token: 'tok' });

    const result = await PayrdrCore.generateToken({
      type: 'card',
      mdOrder: 'md-1',
      pan: '4111',
      expiry: '12/28',
      cvc: '123',
      pubKey: 'PEM',
    });

    expect(calls[0]!.method).toBe('coreToken');
    expect(sent()).toEqual({ type: 'card', mdOrder: 'md-1', pan: '4111', expiry: '12/28', cvc: '123', pubKey: 'PEM' });
    expect(result).toEqual({ token: 'tok', errors: {} });
  });

  it('maps the other kinds of token to their native types', async () => {
    const typeSentFor = async (request: Parameters<typeof PayrdrCore.generateToken>[0]) => {
      nativeReplies({ token: 'tok' });
      await PayrdrCore.generateToken(request);
      return sent().type;
    };

    expect(await typeSentFor({ type: 'cardWithoutOrder', pan: '4111', expiry: '12/28', cvc: '123', pubKey: 'PEM' })).toBe('cardInstant');
    expect(await typeSentFor({ type: 'savedCard', mdOrder: 'md-1', bindingId: 'b-1', pubKey: 'PEM' })).toBe('binding');
    expect(await typeSentFor({ type: 'savedCardWithoutOrder', bindingId: 'b-1', pubKey: 'PEM' })).toBe('bindingInstant');
  });

  it('returns field errors by field', async () => {
    nativeReplies({ errors: { pan: 'required', mdOrder: 'invalid' } });

    const result = await PayrdrCore.generateToken({ type: 'card', mdOrder: '', pan: '', expiry: '12/28', cvc: '1', pubKey: 'PEM' });

    expect(result).toEqual({ errors: { pan: 'required', mdOrder: 'invalid' } });
  });

  it('throws on a request the native side cannot read', async () => {
    nativeReplies({ error: { code: 'invalid_request', message: 'bad' } });

    await expect(PayrdrCore.generateToken({ type: 'savedCardWithoutOrder', bindingId: 'b', pubKey: 'PEM' })).rejects.toThrow('bad');
  });
});
