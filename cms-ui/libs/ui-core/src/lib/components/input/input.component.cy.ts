import { NO_ERRORS_SCHEMA } from '@angular/core';
import { createOutputSpy } from 'cypress/angular';
import { InputComponent } from './input.component';

describe('InputComponent', () => {
    it('should trigger the change accordingly', () => {
        const WRITE_VALUE = 'Hello World';

        cy.mount(InputComponent, {
            componentProperties: {
                change: createOutputSpy('changeSpy'),
            },
            schemas: [NO_ERRORS_SCHEMA],
        });

        cy.get('input')
            .type(WRITE_VALUE);

        cy.get('@changeSpy')
            .should('have.been.calledWith', WRITE_VALUE);
    });
});
