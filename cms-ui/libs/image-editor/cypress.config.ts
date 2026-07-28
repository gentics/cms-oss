import { nxComponentTestingPreset } from '@nx/angular/plugins/component-testing';
import { defineConfig } from 'cypress';
import { createComponentReporterOptions } from '../../cypress.preset';

export default defineConfig({
    component: {
        ...nxComponentTestingPreset(__filename),
        indexHtmlFile: './cypress/support/component-index.html',
        video: false,
        screenshotOnRunFailure: false,
    },
    ...createComponentReporterOptions('libs', 'image-editor', false),
});
