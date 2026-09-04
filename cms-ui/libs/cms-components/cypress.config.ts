import { nxComponentTestingPreset } from '@nx/angular/plugins/component-testing';
import { defineConfig } from 'cypress';
import { createComponentTestConfiguration } from '../../cypress.preset';

export default defineConfig({
    component: createComponentTestConfiguration('libs', 'cms-components', false, {
        ...nxComponentTestingPreset(__filename),
    }),
});
