// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html
process.env.CHROME_BIN = require('puppeteer').executablePath();

const { join } = require('path');
const { constants } = require('karma');

module.exports = (type, name, junitPackageName) => {
    if (!junitPackageName) {
        junitPackageName = `ui.${type}.${name}`;
    }

    return {
        basePath: './src',
        frameworks: ['jasmine', '@angular-devkit/build-angular'],
        plugins: [
            require('karma-jasmine'),
            require('karma-chrome-launcher'),
            require('karma-jasmine-html-reporter'),
            require('karma-junit-reporter'),
            require('karma-mocha-reporter'),
            require('karma-coverage'),
            require('@angular-devkit/build-angular/plugins/karma'),
        ],
        jasmine: {
            random: true, // random test execution
        },
        client: {
            clearContext: false, // leave Jasmine Spec Runner output visible in browser,
            jasmine: {
                random: false, // random test execution
            },
        },
        coverageReporter: {
            dir: join(__dirname, `coverage/${type}/${name}`),
            subdir: '.',
            reporters: [{ type: 'text-summary' }, { type: 'cobertura' }],
        },

        junitReporter: {
            outputDir: join(__dirname, `.reports/${type}/${name}/`),
            suite: junitPackageName,
            useBrowserName: false,
            outputFile: 'KARMA-report.xml',
        },

        // Test results reporter to use
        // available reporters: https://npmjs.org/browse/keyword/karma-reporter
        // The JUnit reporter is specified as a command line argument when running build from Maven.
        reporters: ['mocha', 'kjhtml'],

        // web server port
        port: 9876,

        // enable / disable colors in the output (reporters and logs)
        colors: true,

        // level of logging
        // possible values: constants.LOG_DISABLE || constants.LOG_ERROR || constants.LOG_WARN || constants.LOG_INFO || constants.LOG_DEBUG
        logLevel: constants.LOG_INFO,

        // enable / disable watching file and executing tests whenever any file changes
        autoWatch: true,

        // start these browsers
        // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
        browsers: ['ChromeDebugging'],

        // Custom browser launchers for use by CI (when running build from Maven).
        customLaunchers: {
            ChromeDebugging: {
                base: 'Chrome',
                flags: ['--remote-debugging-port=9333'],
            },
            ChromeDocker: {
                base: 'ChromeHeadless',
                // We must disable the Chrome sandbox when running Chrome inside Docker (Chrome's sandbox needs
                // more permissions than Docker allows by default)
                flags: [
                    '--no-sandbox',
                    '--headless',
                    '--disable-gpu',
                    '--remote-debugging-port=9222',
                ],
            },
        },

        // Continuous Integration mode
        // if true, Karma captures browsers, runs the tests and exits
        singleRun: false,

        captureTimeout: 60000,
        browserNoActivityTimeout: 120000,
        browserDisconnectTimeout: 20000,

        // Tells Karma how long to wait (in milliseconds) from the last file change before starting the test process again,
        // resetting the timer each time a file changes
        autoWatchBatchDelay: 500,
    };
};
