import type { ComponentFixture } from '@angular/core/testing';
import type { MountResponse } from 'cypress/angular';

declare global {
    // eslint-disable-next-line @typescript-eslint/no-namespace
    namespace Cypress {
        interface Chainable<Subject> {
            /**
             * Detects the changes of the currently mounted component (if it can be found),
             * of from the provided MountResponse/ComponentFixture as parameter.
             * Also waits for the component to be rendered before continuing.
             */
            detectChanges: (Subject extends (MountResponse<any> | ComponentFixture<any>)
                // cy.mount(...).detectChanges()
                ? (() => Cypress.Chainable<Subject>)
                // cy.detectChanges()
                : (() => Cypress.Chainable<MountResponse<any> | ComponentFixture<any>>)
            )
            // cy.detectChanges(ref)
            // cy.wrap(somethingElse).detectChanges(ref)
            & (<C = any>(ref: MountResponse<C>) => Cypress.Chainable<MountResponse<C>>)
            & (<C = any>(ref: ComponentFixture<C>) => Cypress.Chainable<ComponentFixture<C>>)
            ;
            /**
             * Allows one to update the component instance, and chains into {@link Chainable.detectChanges},
             * so assertions can be done right after it.
             * The component instance can either be taken from the subject/chain, by parameter,
             * or will otherwise be attempted to be loaded from the mounted store.
             */
            updateInstance: (
                (Subject extends MountResponse<any>
                    ? (fn: (instance: Subject['component']) => any | PromiseLike<any>) => Cypress.Chainable<Subject>
                    : (Subject extends ComponentFixture<any>
                        ? (fn: (instance: Subject['componentInstance']) => any | PromiseLike<any>) => Cypress.Chainable<Subject>
                        : (fn: (instance: any) => any | PromiseLike<any>) => Cypress.Chainable<MountResponse<any>>
                    )
                )
                // cy.updateInstance(ref, (instance) => ...)
                & (<C = any>(
                    ref: MountResponse<C>,
                    fn: (instance: C) => any | PromiseLike<any>,
                ) => Cypress.Chainable<MountResponse<C>>)
                & (<C = any>(
                    ref: ComponentFixture<C>,
                    fn: (instance: C) => any | PromiseLike<any>,
                ) => Cypress.Chainable<ComponentFixture<C>>)
            )
            ;
            /**
             * Basically like the {@link Chainable.then} function, except your return value
             * does not chain further down.
             * This is useful for adding `expects` or other small scripts inbetween a chain,
             * without disrupting the flow or having to re-return the same value.
             * @param fn The function which executes on the current value.
             * @returns The same subject from the chain previously called.
             */
            tap: (fn: (value: Subject) => any | PromiseLike<any>) => Cypress.Chainable<Subject>;
        }
    }
}
