import { nxComponentTestingPreset } from '@nx/angular/plugins/component-testing';
import { defineConfig } from 'cypress';
import { createComponentTestConfiguration } from '../../cypress.preset';

export default defineConfig({
    component: createComponentTestConfiguration('apps', 'ct-link-checker', true, {
        ...nxComponentTestingPreset(__filename),
    }),
});
