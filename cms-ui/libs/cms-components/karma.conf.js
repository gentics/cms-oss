// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

const { join } = require('path');
const getBaseKarmaConfig = require('../../karma.conf');

module.exports = function (config) {
    const baseConfig = getBaseKarmaConfig();
    config.set({
        ...baseConfig,
        files: [
            './lib/testing/global-variables.js',
        ],
        coverageIstanbulReporter: {
            ...baseConfig.coverageIstanbulReporter,
            dir: join(__dirname, '../../coverage/libs/cms-components'),
        },
        junitReporter: {
            ...baseConfig.junitReporter,
            outputDir: join(__dirname, '.reports'),
        },
    });
};
