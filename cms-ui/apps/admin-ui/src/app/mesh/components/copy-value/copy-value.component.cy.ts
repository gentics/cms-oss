import { NO_ERRORS_SCHEMA } from '@angular/core';
import { mockPipes } from '@gentics/ui-core/testing/mock-pipe';
import { MountResponse } from 'cypress/angular';
import { CopyValueComponent } from './copy-value.component';

describe('CopyValueComponent', () => {

    const TEXT_VALUE = 'hello world 123';
    let instance: Cypress.Chainable<MountResponse<CopyValueComponent>>;

    beforeEach(() => {
        instance = cy.mount(CopyValueComponent, {
            componentProperties: {
                value: TEXT_VALUE,
            },
            declarations: [
                mockPipes('i18n'),
            ],
            schemas: [
                NO_ERRORS_SCHEMA,
            ],
        });
    });

    it('should display the value', () => {
        instance.get('.content').contains(TEXT_VALUE);
    });

    it('should copy the content to the clipboard', () => {
        instance.get('.copy-button').click();
        cy.window().then(win => {
            win.navigator.clipboard.readText().then(text => {
                expect(text).to.equal(TEXT_VALUE);
            });
        });
    });
});
