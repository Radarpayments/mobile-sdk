import { sessionIdToMdOrder } from '../src/merchant/sessionId';

// A session the dev gateway created, and the mdOrder of its paymentUrl.
const sessionId = 'ps_2GRSigK2sUQKFSAZWbg2z7UuQxxjz7wqCjZqYyWRiq22aF8xxCDPua';
const sessionMdOrder = '0180a01a-a8cb-75b8-8f9d-1a5f00ab8a11';

test('a session id is ps_ and the Base58 of ps: and the mdOrder', () => {
  expect(sessionIdToMdOrder(sessionId)).toBe(sessionMdOrder);
});

test.each([
  ['', 'empty'],
  ['not-a-session', 'no ps_ prefix'],
  ['ps_', 'nothing after the prefix'],
  ['ps_0OIl', 'outside the Base58 alphabet'],
  // Base58 of "md:1": decodes, but not to "ps:…".
  ['ps_3oBKJY', 'not ps: inside'],
])('%s is not a session id (%s)', id => {
  expect(sessionIdToMdOrder(id)).toBeUndefined();
});
