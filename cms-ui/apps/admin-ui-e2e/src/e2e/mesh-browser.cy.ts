import { EntityImporter, TestSize } from '@gentics/e2e-utils';
import { AUTH_ADMIN } from '../support/app.po';

describe('Mesh Browser', () => {

    const CR_NAME = 'Mesh CR';
    const IMPORTER = new EntityImporter();

    before(async () => {
        await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
    });

    beforeEach(() => {
        // If this client isn't recreated for WHATEVER reason, the CMS gives back a 401 for importer requests.
        IMPORTER.client = null;
        cy.wrap(IMPORTER.syncPackages(TestSize.MINIMAL));

        cy.navigateToApp();
        cy.login(AUTH_ADMIN);

        cy.intercept({
            pathname: '/rest/admin/features/*',
        }).as('featureChecks');
        cy.intercept({
            pathname: '/rest/perm/contentrepositoryadmin',
        }).as('permChecks');

        cy.wait('@featureChecks');
        cy.wait('@permChecks');

        cy.get('gtx-dashboard-item[data-id="mesh-browser"]').click();
    });

    it('should have content repositories listed', () => {
        cy.get('gtx-table')
            .find('.grid-row')
            .should('have.length.gte', 1);
    });

    it('should show login gate on click', () => {
        cy.get('gtx-table')
            .find('.grid-row')
            .contains(CR_NAME)
            .click();

        cy.get('.login-gate-wrapper').should('exist');
    });

    // TODO: Needs proper CR repair and content import to work
    describe.skip('Mesh Browser', () => {
        beforeEach(() => {
            cy.get('gtx-table')
                .find('.grid-row')
                .contains(CR_NAME)
                .click();

            cy.fixture('auth.json').then(auth => {
                cy.get('.login-form input[type="text"]').type(auth.mesh.username);
                cy.get('.login-form input[type="password"]').type(auth.mesh.password);
                cy.get('.login-form button[type="submit"]').click();

                // Content list should be visible now since we're logged in
                cy.get('.schema-list-wrapper').should('exist');
            });

        });

        it('should list content', () => {
            cy.get('.schema-items')
                .should('have.length.gte', 1);
        });

        it('should be able to navigate to node content', () => {
            cy.get('.schema-items')
                .find('.schema-element')
                .find('[data-is-container="true"]')
                .first()
                .should('have.length.gte', 1)
                .click();
        });

        it('should be able to open detail view', () => {
            cy.get('.schema-items')
                .find('.schema-element')
                .find('[data-is-container="false"]')
                .first()
                .should('have.length.gte', 1)
                .click()

            cy.get('gtx-mesh-browser-editor').should('exist')
        });

    });

});
