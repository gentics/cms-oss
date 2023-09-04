const nxPreset = require('@nx/jest/preset').default;

module.exports = {
    ...nxPreset,
    // Lodash-ES would need to be transpiled, which we skip by just using regular lodash instead.
    moduleNameMapper: {
        '^lodash-es': 'lodash',
    },
};
