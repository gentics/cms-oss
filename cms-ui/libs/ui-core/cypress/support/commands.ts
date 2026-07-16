/* eslint-disable @typescript-eslint/no-unsafe-call */
/// <reference types="cypress" />

import type { ComponentFixture } from '@angular/core/testing';
import type { MountResponse } from 'cypress/angular';

const MOUNT_REFERENCE_ALIAS = 'mountedReference';

function detectChanges(): Cypress.Chainable<MountResponse<any>>;
function detectChanges<T extends (MountResponse<C> | ComponentFixture<C>), C = any>(): Cypress.Chainable<T>;
function detectChanges<T>(ref?: MountResponse<T>): Cypress.Chainable<MountResponse<T>>;
function detectChanges<T>(ref?: ComponentFixture<T>): Cypress.Chainable<ComponentFixture<T>>;
function detectChanges<T>(ref?: MountResponse<T> | ComponentFixture<T>): Cypress.Chainable<MountResponse<T> | ComponentFixture<T>> {
    let wrapped: Cypress.Chainable<MountResponse<T> | ComponentFixture<T>>;
    if (ref == null) {
        wrapped = cy.get(`@${MOUNT_REFERENCE_ALIAS}`, { log: true });
    } else {
        wrapped = cy.wrap(ref, { log: false });
    }

    return wrapped.then((el) => {
        if (el == null) {
            Cypress.log({
                name: 'detectChanges',
                message: 'Could not find mount or fixture to detect changes on',
            });
            return null;
        }

        const fixture: ComponentFixture<any> = ((el as MountResponse<any>).fixture) != null
            ? (el as MountResponse<any>).fixture
            : el as any;

        fixture.detectChanges();
        return fixture.whenRenderingDone().then(() => el);
    });
}

function updateInstance<C = any>(
    ref: MountResponse<C>,
    fn: (instance: C) => any | PromiseLike<any>,
): Cypress.Chainable<MountResponse<C>>;
function updateInstance<C = any>(
    ref: ComponentFixture<C>,
    fn: (instance: C) => any | PromiseLike<any>,
): Cypress.Chainable<ComponentFixture<C>>;
function updateInstance<C = any>(
    ref: MountResponse<C> | ComponentFixture<C>,
    fn: (instance: C) => any | PromiseLike<any>,
): Cypress.Chainable<MountResponse<C> | ComponentFixture<C>> {
    return cy.wrap(ref, { log: false })
        .then((el) => {
            const fixture: ComponentFixture<C> = ((el as MountResponse<C>).fixture) != null
                ? (el as MountResponse<C>).fixture
                : el as any;
            return fn(fixture.componentInstance);
        })
        .then(() => ref)
        .detectChanges();
}

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

Cypress.Commands.add('detectChanges', {
    prevSubject: 'optional',
} as any, ((subject, ref) => {
    if (subject != null) {
        return detectChanges(subject);
    }
    return detectChanges(ref);
}) as any);

Cypress.Commands.add('updateInstance', { prevSubject: 'optional' }, ((subject, refOrFn, fn) => {
    if (subject != null) {
        return updateInstance(subject, refOrFn);
    }
    if (typeof refOrFn === 'function') {
        fn = refOrFn;
        return cy.get(`@${MOUNT_REFERENCE_ALIAS}`).then((ref) => updateInstance(ref, fn));
    }
    return updateInstance(refOrFn, fn);
}) as any);

Cypress.Commands.add('tap', { prevSubject: true }, (subject, fn) => {
    return cy.wrap(subject, { log: false }).then((val) => {
        const tmp = fn(val);
        if (tmp != null && tmp.then === 'function') {
            return tmp.then(() => val);
        }
        return cy.wrap(val, { log: false });
    });
});

// Call the original mount and simply store the result so we can use it in our own functions.
Cypress.Commands.overwrite('mount', (originalFn, component, config) => {
    const res = originalFn(component, config);

    res.as(MOUNT_REFERENCE_ALIAS);

    return res;
});
