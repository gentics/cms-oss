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
        login(source: 'cms' | 'keycloak'): Chainable<void>;
    }
}

//
// -- This is a parent command --
Cypress.Commands.add('login', (source) => {
    return cy.fixture('auth.json').then(auth => {
        const data = auth[source];

        cy.get('input[type="text"]').type(data.username);
        cy.get('input[type="password"]').type(data.password);

        if (source === 'cms') {
            cy.get('button[type="submit"]').click();
        } else {
            cy.get('input[type="submit"]').click();
        }
    });
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
