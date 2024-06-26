/* eslint-disable @typescript-eslint/restrict-template-expressions */
/* eslint-disable @typescript-eslint/unbound-method */
/* eslint-disable import/no-nodejs-modules */
const { resolve } = require('path');

module.exports = {
    createReporterOptions(type, name, isCI) {
        isCI = typeof isCI === 'boolean' ? isCI : false;

        if (isCI) {
            return {
                reporter: resolve(__dirname, 'node_modules/mocha-junit-reporter'),
                reporterOptions: {
                    testsuitesTitle: `Integration Tests: ${name}`,
                    mochaFile: resolve(__dirname, `.reports/${type}/${name}/CYPRESS-report.xml`),
                    jenkinsMode: true,
                },
            };
        }

        return {
            reporter: 'min',
        }
    },
};
