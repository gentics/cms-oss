import { nxComponentTestingPreset } from '@nx/angular/plugins/component-testing';
import { defineConfig } from 'cypress';
import { createComponentReporterOptions } from '../../cypress.preset';

export default defineConfig({
    component: {
        ...nxComponentTestingPreset(__filename),
        indexHtmlFile: './cypress/support/component-index.html',
        video: false,
        screenshotOnRunFailure: false,
        // Cypress 14+ defaults justInTimeCompile to true (webpack only), which can
        // intermittently run 0 tests in CI. Remove this line to opt back in.
        justInTimeCompile: false,
    },
    ...createComponentReporterOptions('libs', 'cms-components', true),
});
