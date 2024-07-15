/* eslint-disable @typescript-eslint/restrict-template-expressions */
/* eslint-disable @typescript-eslint/unbound-method */
/* eslint-disable import/no-nodejs-modules */
const { resolve } = require('path');

module.exports = {
    createReporterOptions(type, name, isCI) {
        isCI = typeof isCI === 'boolean' ? isCI : false;

        if (isCI) {
            return {
                reporter: resolve(__dirname, 'node_modules/cypress-multi-reporters'),
                reporterOptions: {
                    reporterEnabled: 'min, mocha-junit-reporter',
                    mochaJunitReporterReporterOptions: {
                        mochaFile: resolve(__dirname, `.reports/${type}/${name}/CYPRESS-report.xml`),
                        testCaseSwitchClassnameAndName: true,
                        jenkinsMode: true,
                        rootSuiteTitle: name,
                        testsuitesTitle: `UI Integration Tests: ${name}`,
                        jenkinsClassnamePrefix: `ui-integration-tests.${name}`,
                    },
                },
            };
        }

        return {
            reporter: 'min',
        }
    },
};
