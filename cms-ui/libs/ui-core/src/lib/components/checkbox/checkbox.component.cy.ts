import { NO_ERRORS_SCHEMA } from '@angular/core';
import { createOutputSpy } from 'cypress/angular';
import { CheckboxComponent } from './checkbox.component';

describe('CheckboxComponent', () => {
    it('should toggle between the boolean states correctly', () => {
        cy.mount(CheckboxComponent, {
            componentProperties: {
                valueChange: createOutputSpy('valueChangeSpy'),
            },
            schemas: [NO_ERRORS_SCHEMA],
        });

        cy.get('label')
            .click();

        cy.get('@valueChangeSpy')
            .should('have.been.calledOnceWith', true);

        cy.get('label')
            .click();

        cy.get('@valueChangeSpy')
            .should('have.been.calledOnceWith', false);

        cy.get('label')
            .click();

        cy.get('@valueChangeSpy')
            .should('have.been.calledOnceWith', true);
    });

    it('should have the correct initial state "true" with [value] binding', () => {
        cy.mount(CheckboxComponent, {
            componentProperties: {
                value: true,
            },
            schemas: [NO_ERRORS_SCHEMA],
        });

        cy.get('input')
            .should('have.property', 'checked', true);
    });

    it('should have the correct initial state "true" with [(ngModel)] binding', () => {
        const ref = cy.mount(CheckboxComponent, {
            schemas: [NO_ERRORS_SCHEMA],
        });

        ref.instance.writeValue(true);

        cy.get('input')
            .should('have.property', 'checked', true);
    });
});
