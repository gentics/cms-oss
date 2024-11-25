import '@gentics/e2e-utils/commands';
import { EntityImporter, IMPORT_ID, rootGroup, userAlpha, userBeta } from '@gentics/e2e-utils';
import { AUTH_ADMIN } from '../support/common';

describe('No Nodes', () => {

    const IMPORTER =  new EntityImporter();

    before(() => {
        cy.muteXHR();

        cy.wrap(null, { log: false }).then(() => {
            return cy.wrap(IMPORTER.cleanupTest(true), { log: false, timeout: 60_000 });
        }).then(() => {
            return cy.wrap(IMPORTER.importData([
                rootGroup,
                userAlpha,
                userBeta,
            ]), { log: false, timeout: 60_000 });
        });

        cy.wrap({
            username: userAlpha.login,
            password: userAlpha.password,
        }).as(userAlpha[IMPORT_ID]);
    });

    describe('Display the "no-nodes" route when no nodes are present', () => {
        const CLASS_ADMIN = 'admin';

        it('should display the correct error message for a regular user', () => {
            cy.navigateToApp();
            cy.login(`@${userAlpha[IMPORT_ID]}`);

            cy.get('gtx-no-nodes .error-container')
                .should('exist')
                .should('not.have.class', CLASS_ADMIN);
        });

        it('should display the correct error message for a admin user with the admin-ui button', () => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);

            cy.get('gtx-no-nodes .error-container')
                .should('exist')
                .should('have.class', CLASS_ADMIN);
            cy.get('gtx-no-nodes .admin-button-link')
                .should('exist')
                .should('have.attr', 'href');
        });
    });
});
