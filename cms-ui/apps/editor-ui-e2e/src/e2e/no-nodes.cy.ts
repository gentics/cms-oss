import { EntityImporter, IMPORT_ID, rootGroup, userAlpha, userBeta } from '@gentics/e2e-utils';
import { AUTH_ADMIN } from '../support/app.po';

describe('No Nodes', () => {

    const IMPORTER =  new EntityImporter();

    before(async () => {
        await IMPORTER.cleanupTest(true);
        await IMPORTER.importData([
            rootGroup,
            userAlpha,
            userBeta,
        ]);

        cy.wrap({
            username: userAlpha.login,
            password: userAlpha.password,
        }).as(userAlpha[IMPORT_ID]);
    });

    describe('Display the "no-nodes" route when no nodes are present', () => {
        it('should display the correct error message for a regular user', () => {
            cy.navigateToApp();
            cy.login(`@${userAlpha[IMPORT_ID]}`);

            cy.get('gtx-no-nodes .error-container')
                .should('exist');
            cy.get('gtx-no-nodes .error-container .error-title')
                // TODO: Find a way to get translations from the translations-files instead of copy-pasting them
                // Makes this quite prone to error when translations change
                .should('contain.text', 'Fehlende Berechtigungen');
        });

        it('should display the correct error message for a admin user with the admin-ui button', () => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);

            cy.get('gtx-no-nodes .error-container')
                .should('exist');
            cy.get('gtx-no-nodes .error-container .error-title')
                .should('contain.text', 'Keine Nodes eingestellt');
            cy.get('gtx-no-nodes .admin-button-link')
                .should('exist')
                .should('have.attr', 'href');
        });
    });
});
