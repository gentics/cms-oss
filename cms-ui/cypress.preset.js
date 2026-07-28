/* eslint-disable @typescript-eslint/restrict-template-expressions */
/* eslint-disable @typescript-eslint/unbound-method */
/* eslint-disable import/no-nodejs-modules */
const { resolve } = require('path');

const CYPRESS_TYPE_COMPONENT = 'component';

/**
 * Mapping for each package to have it's own dedicated port.
 * This is used for component-tests, so that they can run in paralell,
 * as cypress always attempts to start the tests on 8081, which is in most
 * cases already used, or at the very least, once from in the first test.
 * All subsequent tests would result in an error where the port is already
 * in use, and removes the usefullness of paralellism.
 */
const PORT_MAPPING = {
    // Applications
    'admin-ui': 8381,
    'editor-ui': 8382,
    'ct-link-checker': 8383,
    // Libraries
    'cms-components': 8481,
    'form-generator': 8482,
    'image-editor': 8483,
    'ui-core': 8484,
};

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
    createComponentReporterOptions(type, name, isCI) {
        const config = createReporterOptions(CYPRESS_TYPE_COMPONENT, type, name, isCI);
        config.port = PORT_MAPPING[name] || 8580;
        return config;
    },
};
