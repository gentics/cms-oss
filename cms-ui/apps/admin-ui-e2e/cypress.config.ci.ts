import { nxE2EPreset } from '@nx/cypress/plugins/cypress-preset';
import { defineConfig } from 'cypress';
import { createE2EReporterOptions } from '../../cypress.preset';

export default defineConfig({
    e2e: {
        ...nxE2EPreset(__dirname),
        // Please ensure you use `cy.origin()` when navigating between domains and remove this option.
        // See https://docs.cypress.io/app/references/migration-guide#Changes-to-cyorigin
        injectDocumentDomain: true,
    },
    ...createE2EReporterOptions('apps', 'admin-ui', true),
});
