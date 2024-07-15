import { nxComponentTestingPreset } from '@nx/angular/plugins/component-testing';
import { defineConfig } from 'cypress';
import { createComponentReporterOptions } from '../../cypress.preset';

export default defineConfig({
    component: {
        ...nxComponentTestingPreset(__filename),
        indexHtmlFile: './cypress/support/component-index.html',
    },
    ...createComponentReporterOptions('libs', 'form-generator', false),
});
