import { nxComponentTestingPreset } from '@nx/angular/plugins/component-testing';
import { defineConfig } from 'cypress';
import { createComponentReporterOptions } from '../../cypress.preset';

export default defineConfig({
    component: {
        ...nxComponentTestingPreset(__filename),
        ...createComponentReporterOptions('libs', 'ui-core', false),
    },
});
