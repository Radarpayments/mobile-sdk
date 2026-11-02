const path = require('path');
const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');

// The SDK module is linked from plugins/react-native (package.json: file:): Metro watches it and resolves its imports
// (react-native) from this app's node_modules.
const sdkModule = path.resolve(__dirname, '../../plugins/react-native');

/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 *
 * @type {import('@react-native/metro-config').MetroConfig}
 */
const config = {
  watchFolders: [sdkModule],
  resolver: {
    nodeModulesPaths: [path.resolve(__dirname, 'node_modules')],
    blockList: [new RegExp(`${sdkModule}/node_modules/.*`)],
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
