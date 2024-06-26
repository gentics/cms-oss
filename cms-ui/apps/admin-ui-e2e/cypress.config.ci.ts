import { nxE2EPreset } from '@nx/cypress/plugins/cypress-preset';
import { defineConfig } from 'cypress';
import { createReporterOptions } from '../../cypress-e2e.preset';

export default defineConfig({
    e2e: nxE2EPreset(__dirname),
    ...createReporterOptions('apps', 'admin-ui', true),
});
