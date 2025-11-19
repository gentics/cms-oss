import { NO_ERRORS_SCHEMA } from '@angular/core';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { MockI18nPipe } from '../../../../testing/mocks';
import { PasswordConfirmInputComponent } from './password-confirm-input.component';

const MAIN_INPUT = '[data-cy="main-input"]';
const CONFIRM_INPUT = '[data-cy="confirm-input"]';
const ERROR_CONTAINER = '.missmatch-error';

describe('PasswordConfirmInputComponent', () => {

    it('should disable the confirmation input correctly', () => {
        const TEST_PASSWORD = 'Hello World';

        cy.mount(PasswordConfirmInputComponent, {
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
            providers: [],
            declarations: [MockI18nPipe],
            schemas: [NO_ERRORS_SCHEMA],
        });

        // Verify initial state

        cy.get(`${MAIN_INPUT} input`)
            .should('exist')
            .and('be.enabled');
        cy.get(`${CONFIRM_INPUT} input`)
            .should('exist')
            .and('be.disabled');
        cy.get(ERROR_CONTAINER)
            .should('be.hidden');

        // Enter text

        cy.get(`${MAIN_INPUT} input`)
            .type(TEST_PASSWORD);
        cy.get(`${CONFIRM_INPUT} input`)
            .should('be.enabled');
        cy.get(ERROR_CONTAINER)
            .should('be.visible');

        // Confirm password

        cy.get(`${CONFIRM_INPUT} input`)
            .type(TEST_PASSWORD);
        cy.get(ERROR_CONTAINER)
            .should('be.hidden');
    });

});
