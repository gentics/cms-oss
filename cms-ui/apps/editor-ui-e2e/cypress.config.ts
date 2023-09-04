import { nxE2EPreset } from '@nx/cypress/plugins/cypress-preset';
import { defineConfig } from 'cypress';
import { resolve } from 'path';

export default defineConfig({
    e2e: nxE2EPreset(__dirname),
    video: false,
    reporter: resolve(__dirname, '../../node_modules/mocha-junit-reporter'),
    reporterOptions: {
        mochaFile: '.reports/report.xml',
    },
});
