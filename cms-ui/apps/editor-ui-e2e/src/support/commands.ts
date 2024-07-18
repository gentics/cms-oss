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
        login(account: string): Chainable<void>;
        selectNode(nodeId: number | string): Chainable<JQuery<HTMLElement>>;
        findList(type: ItemType): Chainable<JQuery<HTMLElement>>;
        findItem(type: ItemType, id: number): Chainable<JQuery<HTMLElement>>;
        itemAction(type: ItemType, id: number, action: string): Chainable<JQuery<HTMLElement>>;
        listAction(type: ItemType, action: string): Chainable<JQuery<HTMLElement>>;
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
    const appBasePath = Cypress.env('CI') ? Cypress.env('CMS_EDITOR_PATH') : '/?skip-sso';
    cy.visit(`${appBasePath}${path || ''}`);
});

Cypress.Commands.add('login', (account) => {
    return cy.fixture('auth.json').then(auth => {
        const data = auth[account];
        if (data) {
            return data;
        }
        return cy.get(account);
    }).then(data => {
        cy.get('input[type="text"]').type(data.username);
        cy.get('input[type="password"]').type(data.password);

        cy.get('button[type="submit"]').click();
    });
});

Cypress.Commands.add('selectNode', (nodeId) => {
    cy.get('.node-selector [data-action="select-node"]')
        .click();
    return cy.get('.node-selector-list')
        .find(`[data-id="${nodeId}"], [data-global-id="${nodeId}"]`)
        .click();
});

Cypress.Commands.add('findList', (type) => {
    return cy.get(`item-list .content-list[data-item-type="${type}"]`);
});

Cypress.Commands.add('findItem', (type, id) => {
    return cy.findList(type)
        .find(`gtx-contents-list-item[data-id="${id}"]`);
});

Cypress.Commands.add('itemAction', (type, id, action) => {
    cy.findItem(type, id)
        .find('.context-menu gtx-button[data-action="context-menu-trigger"]')
        .click({ force: true });
    return cy.get('.item-context-menu-content')
        .find(`[data-action="${action}"]`)
        .click({ force: true });
});

Cypress.Commands.add('listAction', (type, action) => {
    cy.findList(type)
        .find()
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
