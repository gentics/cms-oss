import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { GenticsUICoreModule } from '@gentics/ui-core';
import '@gentics/ui-core/cypress';
import { PasswordConfirmInputComponent } from './password-confirm-input.component';
import { TestBed } from '@angular/core/testing';

const MAIN_INPUT = '[data-control="main"]';
const CONFIRM_INPUT = '[data-control="confirm"]';
const ERROR_CONTAINER = '.missmatch-error';

@Pipe({
    name: 'gtxI18n',
    standalone: false,
})
class MockI18nPipe implements PipeTransform {
    transform(query: string, ...args: any[]): string {
        return query;
    }
}

describe('PasswordConfirmInputComponent', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
            declarations: [MockI18nPipe],
            schemas: [NO_ERRORS_SCHEMA],
        });
    });

    it('should disable the confirmation input correctly', () => {
        const TEST_PASSWORD = 'Hello World';

        cy.mount(PasswordConfirmInputComponent);

        // Verify initial state

        cy.get(`${MAIN_INPUT} input`)
            .should('exist')
            .and('be.enabled');
        cy.get(`${CONFIRM_INPUT} input`)
            .should('exist')
            .and('be.disabled');
        cy.get(ERROR_CONTAINER)
            .should('not.exist');

        // Enter text

        cy.get(`${MAIN_INPUT} input`)
            .type(TEST_PASSWORD);
        cy.get(`${CONFIRM_INPUT} input`)
            .should('be.enabled');
        cy.get(ERROR_CONTAINER)
            .should('exist')
            .and('be.visible');

        // Confirm password

        cy.get(`${CONFIRM_INPUT} input`)
            .type(TEST_PASSWORD);
        cy.get(ERROR_CONTAINER)
            .should('not.exist');
    });

});
