/* eslint-disable @typescript-eslint/no-unsafe-call */
/// <reference types="cypress" />

import type { ComponentFixture } from '@angular/core/testing';
import type { MountResponse } from 'cypress/angular';
import { MOUNT_REFERENCE_ALIAS } from './common';

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

export function registerCommands(): void {
    registerDetectChangesCommand();
    registerUpdateInstanceCommand();
    registerTapCommand();
}

export function registerDetectChangesCommand(): void {
    Cypress.Commands.add('detectChanges', {
        prevSubject: 'optional',
    } as any, ((subject, ref) => {
        if (subject != null) {
            return detectChanges(subject);
        }
        return detectChanges(ref);
    }) as any);
}

export function registerUpdateInstanceCommand(): void {
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
}

export function registerTapCommand(): void {
    Cypress.Commands.add('tap', { prevSubject: true }, (subject, fn) => {
        return cy.wrap(subject, { log: false }).then((val) => {
            const tmp = fn(val);
            if (tmp != null && tmp.then === 'function') {
                return tmp.then(() => val);
            }
            return cy.wrap(val, { log: false });
        });
    });
}
