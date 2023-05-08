// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

const { join } = require('path');
const getBaseKarmaConfig = require('../../karma.conf');

module.exports = function (config) {
    const baseConfig = getBaseKarmaConfig();
    config.set({
        ...baseConfig,
        files: [
            './testing/global-variables.js',
            // Serve .html in the ./testing folder on the karma web server - they are needed for some tests.
            { pattern: './testing/*.html', included: false, served: true }
        ],
        coverageIstanbulReporter: {
            ...baseConfig.coverageIstanbulReporter,
            dir: join(__dirname, '../../coverage/apps/editor-ui'),
        },
        junitReporter: {
            ...baseConfig.junitReporter,
            outputDir: join(__dirname, '.reports'),
        },
    });
};
