// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

// eslint-disable-next-line @typescript-eslint/no-namespace
declare namespace Cypress {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    interface Chainable<Subject> {
        login(cmsLogin: boolean): void;
        editEntity(type: string, identifier: string): Chainable<JQuery<HTMLElement>> | Chainable<null>;
    }
}
//
// -- This is a parent command --
Cypress.Commands.add('login', (cmsLogin) => {
    cy.fixture('auth.json').then(auth => {
        const cred = cmsLogin ? auth.cms : auth.keycloak;

        cy.get('input[type="text"]').type(cred.username);
        cy.get('input[type="password"]').type(cred.password);
        if (cmsLogin) {
            cy.get('button[type="submit"]').click();
        } else {
            cy.get('input[type="submit"]').click();
        }
    });
});

Cypress.Commands.add('editEntity', (type, identifier) => {
    let tabId;
    let properties;

    switch (type) {
        case 'user':
            tabId = 1;
            properties = 'gtx-mesh-user-properties';
            break;
        default:
            return cy.root().end();
    }

    cy.get(`.grouped-tabs .tab-link:nth(${tabId})`).click();
    // Find the user in the table and edit it
    cy.get('gtx-table')
        .find('.grid-row').contains(identifier)
        .parents('.grid-row')
        .find('gtx-button[data-id="edit"]')
        .click();

    return cy.get(properties);
});

//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })
