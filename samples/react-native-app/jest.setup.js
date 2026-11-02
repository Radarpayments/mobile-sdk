/* global jest */
// Native modules have no JS side in Jest: the libraries' own mocks, and the SDK as jest functions that each test
// sets up.

jest.mock('@react-native-async-storage/async-storage', () =>
  require('@react-native-async-storage/async-storage/jest/async-storage-mock'),
);
jest.mock('@react-native-clipboard/clipboard', () => require('@react-native-clipboard/clipboard/jest/clipboard-mock'));
jest.mock('react-native-safe-area-context', () => require('react-native-safe-area-context/jest/mock').default);
jest.mock('@payrdr/react-native-sdk', () => ({
  PayrdrCheckout: jest.fn().mockImplementation(config => ({ config, present: jest.fn(), presentSession: jest.fn() })),
  PayrdrCore: { generateToken: jest.fn() },
}));
// The device's languages (iOS reads them from the user defaults): English, as the tests expect.
jest.mock('react-native/Libraries/Settings/Settings', () => ({
  __esModule: true,
  default: {
    get: key => (key === 'AppleLanguages' ? ['en-US'] : undefined),
    set: () => {},
    watchKeys: () => 0,
    clearWatch: () => {},
  },
}));
