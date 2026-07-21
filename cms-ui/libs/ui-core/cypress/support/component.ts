import { mount } from 'cypress/angular';
import { registerMount } from '../src';
import './commands';

// Only setup mount in here, everything else in commands
declare global {
    // eslint-disable-next-line @typescript-eslint/no-namespace
    namespace Cypress {
        interface Chainable {
            mount: typeof mount;
        }
    }
}

registerMount(mount);
