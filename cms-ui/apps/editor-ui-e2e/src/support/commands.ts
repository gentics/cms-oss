// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************

type ItemType = 'folder' | 'page' | 'image' | 'file' | 'form';

// eslint-disable-next-line @typescript-eslint/no-namespace
declare namespace Cypress {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    interface Chainable<Subject> {
        navigateToApp(path?: string): Chainable<void>;
        login(source: 'cms' | 'keycloak'): Chainable<void>;
        selectNode(nodeId: number | string): Chainable<JQuery<HTMLElement>>;
        findItem(type: ItemType, id: number): Chainable<JQuery<HTMLElement>>;
        itemAction(type: ItemType, id: number, action: string): Chainable<JQuery<HTMLElement>>;
    }
}

//
// -- This is a parent command --
Cypress.Commands.add('navigateToApp', (path) => {
    /*
     * The baseUrl is always properly configured via NX.
     * When using the CI however, we use the served UI from the CMS directly.
     * Therefore we also have to use the correct path for it.
     */
    const appBasePath = Cypress.env('CI') ? Cypress.env('CMS_EDITOR_PATH') : '/';
    cy.visit(`${appBasePath}${path || ''}`);
});

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

Cypress.Commands.add('selectNode', (nodeId) => {
    cy.get('.node-selector [data-action="select-node"]')
        .click();
    return cy.get('.node-selector-list')
        .find(typeof nodeId === 'number' ? `[data-id="${nodeId}"]` : `[data-global-id="${nodeId}"]`)
        .click();
});

Cypress.Commands.add('findItem', (type, id) => {
    return cy.get(`item-list .list-body[data-item-type="${type}"]`)
        .find(`gtx-contents-list-item[data-id="${id}"]`);
});

Cypress.Commands.add('itemAction', (type, id, action) => {
    cy.findItem(type, id)
        .find('.context-menu gtx-button[data-action="context-menu-trigger"]')
        .click({ force: true });
    return cy.get('.item-context-menu-content')
        .find(`[data-action="${action}"]`)
        .click();
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
