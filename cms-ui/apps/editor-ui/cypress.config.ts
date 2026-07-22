import { nxComponentTestingPreset } from '@nx/angular/plugins/component-testing';
import { defineConfig } from 'cypress';
import { createComponentTestConfiguration } from '../../cypress.preset';

export default defineConfig({
    component: createComponentTestConfiguration('apps', 'editor-ui', false, {
        ...nxComponentTestingPreset(__filename),
    }),
});
