const { resolve } = require('path');

module.exports = {
    createReporterOptions(type, name) {
        return {
            reporter: resolve(__dirname, 'node_modules/mocha-junit-reporter'),
            reporterOptions: {
                mochaFile: resolve(__dirname, `.reports/${type}/${name}/CYPRESS-[hash]-report.xml`),
            },
        };
    }
};
