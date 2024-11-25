import { AccessControlledType } from '@gentics/cms-models';
import { EntityImporter, TestSize } from '@gentics/e2e-utils';
import { AUTH_ADMIN } from '../support/app.po';
import '@gentics/e2e-utils/commands';

describe('Content Repositories Module', () => {

    const IMPORTER = new EntityImporter({
        logRequests: false,
        logImports: false
    });

    const NEW_PROJECT_NAME = 'New Project';

    const TRABLE_PROJECTS = 'Projekte';

    const TRABLE_NODES = 'Nodes';

    const EXAMPLE_PROJECT = 'example';

    const MINIMAL = "Minimal";
    const FOLDER_A = "Folder A";
    const FOLDER_B = "Folder B";
    const CR_ID = '1';

    const ALIAS_MODULE = '@module';
    const ALIAS_CR_TABLE = '@crTable';

    before(() => {
        cy.muteXHR();
        cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL));
    });

    beforeEach(() => {
        cy.muteXHR();
        // If this client isn't recreated for WHATEVER reason, the CMS gives back a 401 for importer requests.
        IMPORTER.client = null;
        cy.wrap(IMPORTER.syncPackages(TestSize.MINIMAL));
        cy.wrap(IMPORTER.cleanupTest());
        cy.wrap(IMPORTER.setupTest(TestSize.MINIMAL));

        cy.navigateToApp();
        cy.login(AUTH_ADMIN);

        // Table loading
        const ALIAS_TABLE_LOAD_REQ = '@tableLoadReq';

        cy.intercept({
            method: 'GET',
            pathname: '/rest/contentrepositories',
        }, req => {
            req.alias = ALIAS_TABLE_LOAD_REQ;
        });

        cy.navigateToModule('content-repositories', AccessControlledType.CONTENT_ADMIN)
            .as(ALIAS_MODULE)
            .find('gtx-table')
            .as(ALIAS_CR_TABLE);

        cy.wait(ALIAS_TABLE_LOAD_REQ);
    });

    it('should have content repositories listed', () => {
        cy.get(ALIAS_CR_TABLE)
            .find('.grid-row')
            .should('have.length.gte', 1);
    });

    it('should open the details on click', () => {
        // eslint-disable-next-line cypress/unsafe-to-chain-command
        cy.get(ALIAS_CR_TABLE)
            .findTableRow(CR_ID)
            .click({ force: true })
            .should('have.class', 'active');

        cy.getDetailView()
            .find('gtx-content-repository-editor')
            .should('exist');
    });

    it('should be possible to select the management tab', () => {
        cy.get(ALIAS_CR_TABLE)
            .findTableRow(CR_ID)
            .click({ force: true });

        // eslint-disable-next-line cypress/unsafe-to-chain-command
        cy.get('gtx-content-repository-editor .gtx-entity-detail > gtx-tabs')
            .selectTab('management')
            .find('gtx-mesh-management')
            .should('exist');
    });

    describe('Mesh Management', () => {
        const ALIAS_MANAGEMENT_CONTENT = '@managementContent';

        beforeEach(() => {
            cy.get(ALIAS_CR_TABLE)
                .findTableRow(CR_ID)
                .click({ force: true });

            // eslint-disable-next-line cypress/unsafe-to-chain-command
            cy.get('gtx-content-repository-editor .gtx-entity-detail > gtx-tabs')
                .selectTab('management')
                .as(ALIAS_MANAGEMENT_CONTENT);

            cy.get(ALIAS_MANAGEMENT_CONTENT)
                .find('gtx-mesh-management')
                .should('exist');
        });

        it('should be possible to login via manual credentials and to logout again', () => {
            // Management data should not be loaded until actually logged in.
            cy.get(ALIAS_MANAGEMENT_CONTENT)
                .find('.management-container')
                .should('not.exist');

            cy.fixture('auth.json').then(auth => {
                cy.get('.login-form input[type="text"]').type(auth.mesh.username);
                cy.get('.login-form input[type="password"]').type(auth.mesh.password);
                cy.get('.login-form button[type="submit"]').click();
            });

            // Management should be visible now since we're logged in
            cy.get(ALIAS_MANAGEMENT_CONTENT)
                .find('.management-container')
                .should('exist');

            // Logout again
            cy.get(ALIAS_MANAGEMENT_CONTENT)
                .find('.management-container .logout-button')
                .click();

            cy.get(ALIAS_MANAGEMENT_CONTENT)
                .find('.management-container')
                .should('not.exist');
        });

        it('should be possible to login via CR credentials and to logout again', () => {
            // Management data should not be loaded until actually logged in.
            cy.get(ALIAS_MANAGEMENT_CONTENT)
                .find('.management-container')
                .should('not.exist');

            cy.get(ALIAS_MANAGEMENT_CONTENT)
                .find('.cr-login-button')
                .click();

            // Management should be visible now since we're logged in
            cy.get(ALIAS_MANAGEMENT_CONTENT)
                .find('.management-container')
                .should('exist');

            // Logout again
            cy.get(ALIAS_MANAGEMENT_CONTENT)
                .find('.management-container .logout-button')
                .click();

            cy.get(ALIAS_MANAGEMENT_CONTENT)
                .find('.management-container')
                .should('not.exist');
        });

        it('should force a new password, apply a new one, and reset it manually to the original one', () => {
            cy.fixture('auth.json').then(auth => {
                const ALIAS_USER_PROPERTIES = '@userProps';

                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.cr-login-button')
                    .click();

                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.management-container')
                    .should('exist');

                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .editMeshEntity('user', auth.mesh.username)
                    .find('[data-control="forcePasswordChange"] label')
                    .click();

                // Save the user
                cy.get('gtx-mesh-user-modal .modal-footer gtx-button[data-action="confirm"]')
                    .click();

                // Logout
                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.management-container .logout-button')
                    .click();

                // Login with CR should not be possible, as the user needs to update the password
                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.management-container')
                    .should('not.exist');

                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.cr-login-button')
                    .click();

                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.management-container')
                    .should('not.exist');

                // Attempt to login, which will fail
                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.login-form input[type="text"]')
                    .type(auth.mesh.username);

                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.login-form input[type="password"]')
                    .type(auth.mesh.password);

                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.login-form button[type="submit"]')
                    .click();

                // Login should have worked now
                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.login-form input[type="password"]:nth(1)')
                    .type(auth.mesh.newPassword);

                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.login-form button[type="submit"]')
                    .click();

                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.management-container')
                    .should('exist');

                // Reset the user password to the original one
                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .editMeshEntity('user', auth.mesh.username)
                    .as(ALIAS_USER_PROPERTIES)
                    .find('.password-checkbox label')
                    .click();

                cy.get(ALIAS_USER_PROPERTIES)
                    .find('[data-control="password"] input')
                    .each(el => {
                        cy.wrap(el, { log: false }).type(auth.mesh.password)
                    });

                // Save the user
                cy.get('gtx-mesh-user-modal .modal-footer gtx-button[data-action="confirm"]')
                    .click();

                // Logout
                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.management-container .logout-button')
                    .click();

                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.management-container')
                    .should('not.exist');

                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.cr-login-button')
                    .click();

                cy.get(ALIAS_MANAGEMENT_CONTENT)
                    .find('.management-container')
                    .should('exist');
            });
        });

        // TODO: Fix this test, as password change has been changed
        it.skip('should be possible to update the CR login information', () => {
            cy.fixture('auth.json').then(auth => {
                cy.get('.cr-login-button').click();
                cy.get('.management-container').should('exist');

                function updateUserPassword(passwordToSet: string) {
                    cy.editMeshEntity('user', auth.mesh.username).as('props')
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
                        .findTableRowContainingText('folder')
                        .selectTableRow();
                    cy.get('gtx-mesh-select-schema-modal')
                        .find('.modal-footer [data-action="confirm"]')
                        .click();

                    // create project
                    cy.get('@modal').find('.modal-footer [data-action="confirm"]').click();

                    // assert that the new project is listed
                    cy.get('gtx-mesh-project-table')
                        .findTableRowContainingText(NEW_PROJECT_NAME)
                        .should('exist');

                    // new delete the project
                    cy.get('gtx-mesh-project-table')
                        .findTableRowContainingText(NEW_PROJECT_NAME)
                        .find('[data-id="delete"]')
                        .click();
                    cy.get('gtx-confirm-delete-modal')
                        .find('.modal-footer [data-action="confirm"]')
                        .click();
                });
            });

            describe('Role Permissions', () => {
                beforeEach(() => {
                    cy.wrap(IMPORTER.deleteMeshProjects());
                    cy.wrap(IMPORTER.runPublish());
                });

                afterEach(() => {
                    cy.wrap(IMPORTER.deleteMeshProjects());
                });

                it('should be possible to read and modify role permissions on projects', () => {
                    // select roles
                    cy.get('.grouped-tabs .tab-link[data-id="roles"]').click();

                    // open modal to manage permissions for anonymous
                    cy.get('gtx-table')
                        .findTableRowContainingText('anonymous')
                        .find('[data-id="managePermissions"]')
                        .click();

                    cy.get('gtx-mesh-role-permissions-modal').as('modal');
                    cy.get('gtx-mesh-role-permissions-trable').as('trable');

                    // open "Projekte"
                    cy.get('@trable')
                        .findTableRowContainingText(TRABLE_PROJECTS)
                        .expandTrableRow();

                    cy.get('@trable')
                        .findTableRowContainingText(EXAMPLE_PROJECT)
                        .should('exist')
                        .expandTrableRow();

                    cy.get('@trable')
                        .findTableRowContainingText(TRABLE_NODES)
                        .should('exist')
                        .expandTrableRow();

                    // the node "Minimal" should not have the readPublished permission
                    cy.get('@trable')
                        .findTableRowContainingText(MINIMAL)
                        .should('exist')
                        .find('.permission-icon[data-id="readPublished"]')
                        .should('exist')
                        .should('not.have.class', 'granted');

                    // edit permissions
                    cy.get('@trable')
                        .findTableRowContainingText(MINIMAL)
                        .find('[data-id="edit"]')
                        .click();

                    // set "readPublished"
                    cy.get('gtx-mesh-role-permissions-edit-modal')
                        .as('perm_modal')
                        .find('gtx-checkbox[formcontrolname="readPublished"] label')
                        .click();
                    cy.intercept({pathname: '/rest/contentrepositories/**', method: 'GET'}).as('load_request');
                    cy.get('@perm_modal')
                        .find('.modal-footer [data-action="confirm"]')
                        .click();
                    cy.wait('@load_request');

                    // find the trable again (was reloaded)
                    cy.get('gtx-mesh-role-permissions-trable').as('trable');

                    // the node "Minimal" should have readPublished permission
                    cy.get('@trable')
                        .findTableRowContainingText(MINIMAL)
                        .should('exist')
                        .find('.permission-icon[data-id="readPublished"]')
                        .should('exist')
                        .should('have.class', 'granted');

                    // apply the permissions recursively
                    cy.get('@trable')
                        .findTableRowContainingText(MINIMAL)
                        .find('[data-id="applyRecursive"]')
                        .click();
                    cy.get('gtx-modal-dialog')
                        .find('.modal-footer [data-action="confirm"]')
                        .click();

                    cy.get('@trable')
                        .findTableRowContainingText(MINIMAL)
                        .expandTrableRow();

                    // assert that Folder A and Folder B now also have read published set
                    cy.get('@trable')
                        .findTableRowContainingText(FOLDER_A)
                        .should('exist')
                        .find('.permission-icon[data-id="readPublished"]')
                        .should('exist')
                        .should('have.class', 'granted');

                    cy.get('@trable')
                        .findTableRowContainingText(FOLDER_B)
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
