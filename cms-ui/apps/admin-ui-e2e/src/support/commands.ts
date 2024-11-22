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

Cypress.Commands.add('navigateToModule', (moduleId, perms) => {
    const ALIAS_FEATURE_CHECK_REQ = '@featureChecksReq';
    const ALIAS_PERM_CHECK_REQ = '@permChecksReq';

    cy.intercept({
        pathname: '/rest/admin/features/*',
    }, req => {
        req.alias = ALIAS_FEATURE_CHECK_REQ;
    });
    if (perms) {
        cy.intercept({
            pathname: `/rest/perm/${perms}`,
        }, req => {
            req.alias = ALIAS_PERM_CHECK_REQ;
        });
    }

    // Wait for all features and permissions to load
    cy.wait(ALIAS_FEATURE_CHECK_REQ);
    if (perms) {
        cy.wait(ALIAS_PERM_CHECK_REQ);
    }

    cy.get(`gtx-dashboard-item[data-id="${moduleId}"]`).click();

    return cy.get('gtx-split-view-router-outlet .master-route-wrapper > *:not(router-outlet)');
});

Cypress.Commands.add('getDetailView', () => {
    return cy.get('gtx-split-view-router-outlet .detail-route-wrapper > *:not(router-outlet)');
});

Cypress.Commands.add('editMeshEntity', { prevSubject: 'element' }, (subject, type, identifier) => {
    let tabId: string;
    let properties: string;

    switch (type) {
        case 'user':
            tabId = 'users';
            properties = 'gtx-mesh-user-properties';
            break;
        default:
            return cy.root().end();
    }

    cy.wrap(subject, { log: false })
        .find(`gtx-grouped-tabs .grouped-tabs .tab-link[data-id="${tabId}"]`)
        .click();

    // Find the user in the table and edit it
    cy.wrap(subject, { log: false })
        .find('gtx-grouped-tabs .grouped-tab-content gtx-table .grid-row.data-row')
        .contains(identifier)
        .parents('.grid-row.data-row')
        .findTableAction('edit')
        .click();

    // Get the properties component, which should be open in a modal now
    return cy.get(properties);
});
