// Only for the unit tests of the TypeScript layer; apps compile the sources with Metro.
module.exports = {
  presets: [['@babel/preset-env', { targets: { node: 'current' } }], '@babel/preset-typescript'],
};
