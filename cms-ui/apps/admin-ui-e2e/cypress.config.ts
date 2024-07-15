import { nxE2EPreset } from '@nx/cypress/plugins/cypress-preset';
import { defineConfig } from 'cypress';
import { createE2EReporterOptions } from '../../cypress.preset';

export default defineConfig({
    e2e: nxE2EPreset(__dirname),
    ...createE2EReporterOptions('apps', 'admin-ui', false),
});
