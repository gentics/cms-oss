import { nxComponentTestingPreset } from '@nx/angular/plugins/component-testing';
import { defineConfig } from 'cypress';
import { createComponentReporterOptions } from '../../cypress.preset';

export default defineConfig({
    component: {
        ...nxComponentTestingPreset(__filename),
        indexHtmlFile: './cypress/support/component-index.html',
        video: false,
        // Please ensure you use `cy.origin()` when navigating between domains and remove this option.
        // See https://docs.cypress.io/app/references/migration-guide#Changes-to-cyorigin
        injectDocumentDomain: true,
    },
    ...createComponentReporterOptions('libs', 'form-generator', false),
});
