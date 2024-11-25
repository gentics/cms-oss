import { EntityImporter, TestSize } from '@gentics/e2e-utils';
import { AccessControlledType } from '@gentics/cms-models';
import { AUTH_ADMIN } from '../support/app.po';

describe('Mesh Browser', () => {

    const CR_NAME = 'Mesh CR';
    const IMPORTER = new EntityImporter();
    const ALIAS_MODULE = '@module';

    before(() => {
        cy.wrap(null, { log: false }).then(() => {
            return cy.wrap(IMPORTER.bootstrapSuite(TestSize.MINIMAL), { log: false, timeout: 60_000 });
        });
    });

    beforeEach(() => {
        cy.muteXHR();

        cy.wrap(null, { log: false }).then(() => {
            return cy.wrap(IMPORTER.cleanupTest(), { log: false, timeout: 60_000 });
        }).then(() => {
            return cy.wrap(IMPORTER.syncPackages(TestSize.MINIMAL), { log: false, timeout: 60_000 });
        }).then(() => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.navigateToModule('mesh-browser', AccessControlledType.CONTENT_REPOSITORY_ADMIN)
                .as(ALIAS_MODULE);
        });
    });

    it('should have content repositories listed', () => {
        cy.get(ALIAS_MODULE)
            .find('gtx-table .grid-row')
            .should('have.length.gte', 1);
    });

    it('should show login gate on click', () => {
        cy.get(ALIAS_MODULE)
            .find('gtx-table .grid-row')
            .contains(CR_NAME)
            .click();

        cy.get('.login-gate-wrapper').should('exist');
    });

    // TODO: Needs proper CR repair and content import to work
    describe.skip('Mesh Browser', () => {
        beforeEach(() => {
            cy.get(ALIAS_MODULE)
                .find('gtx-table .grid-row')
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
