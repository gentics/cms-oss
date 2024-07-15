/* eslint-disable @typescript-eslint/restrict-template-expressions */
/* eslint-disable @typescript-eslint/unbound-method */
/* eslint-disable import/no-nodejs-modules */
const { resolve } = require('path');

const CYPRESS_TYPE_E2E = 'e2e';
const CYPRESS_TYPE_COMPONENT = 'component';

function createReporterOptions(cypressType, type, name, isCI) {
    isCI = typeof isCI === 'boolean' ? isCI : false;

    if (isCI) {


        return {
            reporter: resolve(__dirname, 'node_modules/cypress-multi-reporters'),
            reporterOptions: {
                reporterEnabled: 'min, mocha-junit-reporter',
                mochaJunitReporterReporterOptions: {
                    mochaFile: resolve(__dirname, `.reports/${type}/${name}/CYPRESS-${cypressType}-report.xml`),
                    testCaseSwitchClassnameAndName: true,
                    jenkinsMode: true,
                    rootSuiteTitle: name,
                    testsuitesTitle: `UI Cypress ${cypressType} Tests: ${name}`,
                    jenkinsClassnamePrefix: `ui.${cypressType}.${name}`,
                },
            },
        };
    }

    return {
        reporter: 'min',
    }
}

module.exports = {
    createE2EReporterOptions(type, name, isCI) {
        return createReporterOptions(CYPRESS_TYPE_E2E, type, name, isCI);
    },
    createComponentReporterOptions(type, name, isCI) {
        return createReporterOptions(CYPRESS_TYPE_COMPONENT, type, name, isCI);
    },
};
