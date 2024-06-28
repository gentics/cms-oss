import { TestSize, bootstrapSuite } from '@gentics/e2e-utils';
import { setup } from '../fixtures/auth.json';

describe('Content Repository', () => {
    const CR_NAME = 'Mesh CR';

    beforeEach(() => {
        cy.wrap(bootstrapSuite(setup, TestSize.MINIMAL));

        cy.visit('http://localhost:8080/admin/?skip-sso', {});
        cy.login(true);
        cy.get('gtx-dashboard-item[data-id="mesh-browser"]').click();
    });

    it('should have content repositories listed', () => {
        cy.get('gtx-table')
            .find('.grid-row')
            .should('have.length.gte', 1);
    });

    it('should show login gate on click', () => {
        cy.get('gtx-table')
            .find('.grid-row').contains(CR_NAME)
            .click();

        cy.get('.login-gate-wrapper').should('exist');
    });

    describe('Mesh Browser', () => {
        beforeEach(() => {
            cy.get('gtx-table')
                .find('.grid-row').contains(CR_NAME)
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
            cy.intercept('POST', '**graphql**').as('graphqlRequest');

            cy.get('.schema-items')
                .find('.schema-element')
                .find('[data-is-container="true"]')
                .first()
                .should('have.length.gte', 1)
                .click()

            cy.wait('@graphqlRequest').then(({ request, response }) => {
                expect(response?.statusCode).to.eq(200);
            });
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
