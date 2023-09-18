/* eslint-disable @typescript-eslint/restrict-template-expressions */
/* eslint-disable @typescript-eslint/unbound-method */
/* eslint-disable import/no-nodejs-modules */
const { resolve } = require('path');

module.exports = {
    createReporterOptions(type, name) {
        return {
            reporter: resolve(__dirname, 'node_modules/mocha-junit-reporter'),
            reporterOptions: {
                mochaFile: resolve(__dirname, `.reports/${type}/${name}/CYPRESS-[hash]-report.xml`),
            },
        };
    },
};
