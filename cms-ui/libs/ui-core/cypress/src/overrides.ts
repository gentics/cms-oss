import type { mount } from 'cypress/angular';
import { MOUNT_REFERENCE_ALIAS } from './common';

/**
 * Registers the mount function from `cypress/angular`, but also aliases
 * the result for later uses in other commands.
 * If the function is already registered, you may use {@link overrideMount} instead.
 * @param mountFn The original mount function
 */
export function registerMount(mountFn: typeof mount): void {
    Cypress.Commands.add('mount', (component, config) => {
        const res = mountFn(component, config);

        res.as(MOUNT_REFERENCE_ALIAS);

        return res;
    });
};

/**
 * Overrides the already registered `cypress/angular` `mount` command,
 * to add the result as an alias for other uses in other commands.
 * It is preferred to register the mount function directly via {@link registerMount} if possible.
 */
export function overrideMount(): void {
    // Call the original mount and simply store the result so we can use it in our own functions.
    Cypress.Commands.overwrite('mount' as any, (originalFn, component, config) => {
        const res = originalFn(component, config);

        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        res.as(MOUNT_REFERENCE_ALIAS);

        return res;
    });
}
