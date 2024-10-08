import { EntityImporter, TestSize } from '@gentics/e2e-utils';
import { AUTH_ADMIN } from '../support/app.po';

describe('Content Repository', () => {

    const IMPORTER = new EntityImporter({
        logRequests: false,
        logImports: false
    });
    const CR_NAME = 'Mesh CR';

    const NEW_PROJECT_NAME = 'New Project';

    const TRABLE_PROJECTS = 'Projekte';

    const TRABLE_NODES = 'Nodes';

    const EXAMPLE_PROJECT = 'example';

    const MINIMAL = "Minimal";
    const FOLDER_A = "Folder A";
    const FOLDER_B = "Folder B";

    before(() => {
        cy.wrap(IMPORTER.cleanupTest());
        cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL));
    });

    beforeEach(() => {
        cy.wrap(IMPORTER.syncPackages(TestSize.MINIMAL));
        cy.wrap(IMPORTER.cleanupTest());
        cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL));
        cy.wrap(IMPORTER.runPublish());

        cy.navigateToApp();
        cy.login(AUTH_ADMIN);

        cy.intercept({
            pathname: '/rest/admin/features/*',
        }).as('featureChecks');
        cy.intercept({
            pathname: '/rest/perm/contentadmin',
        }).as('permChecks');
        cy.intercept({
            method: 'GET',
            pathname: '/rest/contentrepositories',
        }).as('listLoad');

        // Wait for all features and permissions to load
        cy.wait('@featureChecks');
        cy.wait('@permChecks');

        cy.get('gtx-dashboard-item[data-id="content-repositories"]').click();

        // Wait for the table to finish loading
        cy.wait('@listLoad');
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
        it.skip('should be possible to update the CR login information', () => {
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

        describe('Mesh Management Login', () => {
            beforeEach(() => {
                cy.get('.cr-login-button').click();
            });
            afterEach(() => {
                cy.get('.management-container .logout-button').click();
            });

            describe('Projects', () => {
                it('should be possible to create a new project', () => {
                    // select projects
                    cy.get('.grouped-tabs .tab-link[data-id="projects"]').click();
    
                    // click button to create new project
                    cy.get('gtx-mesh-project-table')
                        .find('[data-action="create"]')
                        .click();
                    cy.get('gtx-mesh-project-modal').as('modal');

                    // fill in project name
                    cy.get('@modal').find('gtx-input[formcontrolname="name"] input[type="text"]').type(NEW_PROJECT_NAME);

                    // select "folder" as schema for root node
                    cy.get('@modal').find('gtx-mesh-schema-picker .select-button').click();
                    cy.get('gtx-mesh-schema-table gtx-table')
                        .findRowContainingText('folder')
                        .selectRow();
                    cy.get('gtx-mesh-select-schema-modal')
                        .find('.modal-footer [data-action="confirm"]')
                        .click();

                    // create project
                    cy.get('@modal').find('.modal-footer [data-action="confirm"]').click();

                    // assert that the new project is listed
                    cy.get('gtx-mesh-project-table')
                        .findRowContainingText(NEW_PROJECT_NAME)
                        .should('exist');

                    // new delete the project
                    cy.get('gtx-mesh-project-table')
                        .findRowContainingText(NEW_PROJECT_NAME)
                        .find('[data-id="delete"]')
                        .click();
                    cy.get('gtx-confirm-delete-modal')
                        .find('.modal-footer [data-action="confirm"]')
                        .click();
                });
            });

            describe('Role Permissions', () => {
                it.only('should be possible to read and modify role permissions on projects', () => {
                    // select roles
                    cy.get('.grouped-tabs .tab-link[data-id="roles"]').click();

                    // open modal to manage permissions for anonymous
                    cy.get('gtx-table')
                        .findRowContainingText('anonymous')
                        .find('[data-id="managePermissions"]')
                        .click();

                    cy.get('gtx-mesh-role-permissions-modal').as('modal');
                    cy.get('gtx-mesh-role-permissions-trable').as('trable');

                    // open "Projekte"
                    cy.get('@trable')
                        .findRowContainingText(TRABLE_PROJECTS)
                        .expandTrableRow();

                    cy.get('@trable')
                        .findRowContainingText(EXAMPLE_PROJECT)
                        .should('exist')
                        .expandTrableRow();

                    cy.get('@trable')
                        .findRowContainingText(TRABLE_NODES)
                        .should('exist')
                        .expandTrableRow();

                    // the node "Minimal" should not have the readPublished permission
                    cy.get('@trable')
                        .findRowContainingText(MINIMAL)
                        .should('exist')
                        .find('.permission-icon[data-id="readPublished"]')
                        .should('exist')
                        .should('not.have.class', 'granted');

                    // edit permissions
                    cy.get('@trable')
                        .findRowContainingText(MINIMAL)
                        .find('[data-id="edit"]')
                        .click();

                    // set "readPublished"
                    cy.get('gtx-mesh-role-permissions-edit-modal')
                        .as('perm_modal')
                        .find('gtx-checkbox[formcontrolname="readPublished"]')
                        .click();
                    cy.get('@perm_modal')
                        .find('.modal-footer [data-action="confirm"]')
                        .click();

                    // the node "Minimal" should have readPublished permission
                    cy.get('@trable')
                        .findRowContainingText(MINIMAL)
                        .should('exist')
                        .find('.permission-icon[data-id="readPublished"]')
                        .should('exist')
                        .should('have.class', 'granted');

                    // apply the permissions recursively
                    cy.get('@trable')
                        .findRowContainingText(MINIMAL)
                        .find('[data-id="applyRecursive"]')
                        .click();
                    cy.get('gtx-modal-dialog')
                        .find('.modal-footer [data-id="default"]')
                        .click();

                    cy.get('@trable')
                        .findRowContainingText(MINIMAL)
                        .expandTrableRow();

                    // assert that Folder A and Folder B now also have read published set
                    cy.get('@trable')
                        .findRowContainingText(FOLDER_A)
                        .should('exist')
                        .find('.permission-icon[data-id="readPublished"]')
                        .should('exist')
                        .should('have.class', 'granted');

                    cy.get('@trable')
                        .findRowContainingText(FOLDER_B)
                        .should('exist')
                        .find('.permission-icon[data-id="readPublished"]')
                        .should('exist')
                        .should('have.class', 'granted');

                    // close modal
                    cy.get('@modal')
                        .find('.modal-footer [data-action="cancel"]')
                        .click();
                });
            });
        });
    });
});
