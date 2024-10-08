import { registerCommonCommands, setupAliasOverrides } from '@gentics/e2e-utils';

setupAliasOverrides();
registerCommonCommands();

Cypress.Commands.add('navigateToApp', (path, raw) => {
    /*
     * The baseUrl is always properly configured via NX.
     * When using the CI however, we use the served UI from the CMS directly.
     * Therefore we also have to use the correct path for it.
     */
    const appBasePath = Cypress.env('CI') ? Cypress.env('CMS_ADMIN_PATH') : '/';
    cy.visit(`${appBasePath}${!raw ? '?skip-sso' : ''}#${path || ''}`);
});

Cypress.Commands.add('login', (account, keycloak) => {
    return cy.fixture('auth.json').then(auth => {
        const data = auth[account];
        if (data) {
            return data;
        }
        return cy.get(account);
    }).then(data => {
        cy.get('input[type="text"]').type(data.username);
        cy.get('input[type="password"]').type(data.password);

        cy.get(`${keycloak ? 'input' : 'button'}[type="submit"]`).click();
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

Cypress.Commands.add('findRowContainingText', {prevSubject: 'element'}, (subject, text) => {
   return cy.wrap(subject)
        .find('.grid-row')
        .contains(text)
        .parents('.grid-row');
});

Cypress.Commands.add('selectRow', {prevSubject: 'element'}, (subject) => {
    return cy.wrap(subject)
        .find('gtx-checkbox.selection-checkbox')
        .click();
});

Cypress.Commands.add('expandTrableRow', {prevSubject: 'element'}, (subject) => {
    return cy.wrap(subject)
        .find('.row-expansion-wrapper')
        .click();
});