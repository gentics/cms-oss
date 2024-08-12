import { EntityImporter, TestSize } from '@gentics/e2e-utils';

describe('Content Repository', () => {

    const IMPORTER = new EntityImporter();
    const CR_NAME = 'Mesh CR';

    beforeEach(async () => {
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);

        cy.navigateToApp();
        cy.login(true);
        cy.get('gtx-dashboard-item[data-id="content-repositories"]').click();
    });

    it('should have content repositories listed', () => {
        cy.get('gtx-table')
            .find('.grid-row')
            .should('have.length.gte', 1);
    });

    it('should open the details on click', () => {
        cy.get('gtx-table')
            .find('.grid-row')
            .contains(CR_NAME)

        cy.get('gtx-table .grid-row.data-row')
            .click({ force: true });

        cy.get('gtx-table .grid-row.data-row')
            .should('have.class', 'active');
        cy.get('gtx-content-repository-editor').should('exist');
    });

    it('should be possible to select the management tab', () => {
        cy.get('gtx-table')
            .find('.grid-row').contains(CR_NAME)
            .click({ force: true });

        cy.get('gtx-content-repository-editor')
            .find('gtx-tabs .tab-link[data-id="management"]')
            .click()

        cy.get('gtx-content-repository-editor gtx-tabs .tab-link[data-id="management"]')
            .should('have.class', 'is-active');

        cy.get('gtx-mesh-management').should('exist');
    });

    describe('Mesh Management', () => {
        beforeEach(() => {
            cy.get('gtx-table')
                .find('.grid-row.data-row').contains(CR_NAME)
                .click();
            cy.get('gtx-content-repository-editor')
                .find('gtx-tabs .tab-link[data-id="management"]')
                .click();
            cy.get('gtx-mesh-management').should('exist');
        });

        it('should be possible to login via manual credentials and to logout again', () => {
            // Management data should not be loaded until actually logged in.
            cy.get('.management-container').should('not.exist');

            cy.fixture('auth.json').then(auth => {
                cy.get('.login-form input[type="text"]').type(auth.mesh.username);
                cy.get('.login-form input[type="password"]').type(auth.mesh.password);
                cy.get('.login-form button[type="submit"]').click();
            });

            // Management should be visible now since we're logged in
            cy.get('.management-container').should('exist');

            // Logout again
            cy.get('.management-container .logout-button').click();
            cy.get('.management-container').should('not.exist');
        });

        it('should be possible to login via CR credentials and to logout again', () => {
            // Management data should not be loaded until actually logged in.
            cy.get('.management-container').should('not.exist');

            cy.get('.cr-login-button').click();

            // Management should be visible now since we're logged in
            cy.get('.management-container').should('exist');

            // Logout again
            cy.get('.management-container .logout-button').click();
            cy.get('.management-container').should('not.exist');
        });

        it('should force a new password, apply a new one, and reset it manually to the original one', () => {
            cy.fixture('auth.json').then(auth => {
                cy.get('.cr-login-button').click();
                cy.get('.management-container').should('exist');

                cy.editEntity('user', auth.mesh.username)
                    .find('[data-control="forcePasswordChange"] label')
                    .click();

                // Save the user
                cy.get('gtx-mesh-user-modal .modal-footer gtx-button')
                    .first()
                    .click();

                // Logout
                cy.get('.management-container .logout-button').click();

                // Login with CR should not be possible, as the user needs to update the password
                cy.get('.management-container').should('not.exist');
                cy.get('.cr-login-button').click();
                cy.get('.management-container').should('not.exist');

                // Attempt to login, which will fail
                cy.get('.login-form input[type="text"]').type(auth.mesh.username);
                cy.get('.login-form input[type="password"]').type(auth.mesh.password);
                cy.get('.login-form button[type="submit"]').click();

                // Login should have worked now
                cy.get('.login-form input[type="password"]:nth(1)').type(auth.mesh.newPassword);
                cy.get('.login-form button[type="submit"]').click();
                cy.get('.management-container').should('exist');

                // Reset the user password to the original one
                cy.editEntity('user', auth.mesh.username).as('props')
                    .find('.password-checkbox label')
                    .click();
                cy.get('@props')
                    .find('[data-control="password"] input')
                    .each(el => {
                        cy.wrap(el).type(auth.mesh.password)
                    });

                // Save the user
                cy.get('gtx-mesh-user-modal .modal-footer gtx-button')
                    .first()
                    .click();

                // Logout
                cy.get('.management-container .logout-button').click();

                cy.get('.management-container').should('not.exist');
                cy.get('.cr-login-button').click();
                cy.get('.management-container').should('exist');
            });
        });

        // TODO: Fix this test, as password change has been changed
        xit('should be possible to update the CR login information', () => {
            cy.fixture('auth.json').then(auth => {
                cy.get('.cr-login-button').click();
                cy.get('.management-container').should('exist');

                function updateUserPassword(passwordToSet: string) {
                    cy.editEntity('user', auth.mesh.username).as('props')
                        .find('.password-checkbox label')
                        .click();
                    cy.get('@props')
                        .find('[data-control="password"] input')
                        .each(el => {
                            cy.wrap(el).type(passwordToSet)
                        });

                    // Save the user
                    cy.get('gtx-mesh-user-modal .modal-footer gtx-button')
                        .first()
                        .click();
                }

                function updateCRPassword(passwordToSet: string) {
                    // Switch to properties tab
                    cy.get('gtx-content-repository-editor')
                        .find('gtx-tabs .tab-link[data-id="properties"]')
                        .click();

                    // Fill in the password
                    cy.get('gtx-content-repository-properties .gtx-password-box')
                        .find('input[type="password"]')
                        .each(el => {
                            cy.wrap(el).type(passwordToSet)
                        });

                    // Save the CR
                    cy.get('.gtx-entity-details-tab-content-header .gtx-save-button button')
                        .click();
                }

                updateUserPassword(auth.mesh.newPassword);

                // Logout
                cy.get('.management-container .logout-button').click();

                // Login should fail
                cy.get('.management-container').should('not.exist');
                cy.get('.cr-login-button').click();
                cy.get('.management-container').should('not.exist');

                updateCRPassword(auth.mesh.newPassword);

                // Switch back to the management tab
                cy.get('gtx-content-repository-editor')
                    .find('gtx-tabs .tab-link[data-id="management"]')
                    .click();

                // CR Login should work now
                // cy.get('.cr-login-button').click();
                cy.get('.management-container').should('exist');

                // Reset to original state
                updateUserPassword(auth.mesh.password);
                updateCRPassword(auth.mesh.password);
            });
        });
    });
});
