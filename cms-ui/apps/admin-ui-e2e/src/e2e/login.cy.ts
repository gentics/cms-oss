import { Feature, Variant } from '@gentics/cms-models';
import { EntityImporter, isVariant, skipableSuite, TestSize } from '@gentics/e2e-utils';

describe('Login', () => {

    const IMPORTER = new EntityImporter();

    describe('Without keycloak feature enabled', () => {
        // Make sure to have keycloak disabled for these tests
        before(async () => {
            await IMPORTER.setupFeatures(TestSize.MINIMAL, {
                [Feature.KEYCLOAK]: false,
            });
        });

        it('should be able to login', () => {
            // TODO: This skip-sso can be removed once the above setupFeatures works
            cy.navigateToApp();
            cy.login(true);
            cy.get('gtx-dashboard-item').should('have.length.gte', 19);
        });
    });

    // Keycloak is an enterprise edition feature, therefore we can only test it in that variant.
    skipableSuite(isVariant(Variant.ENTERPRISE), 'With keycloak feature enabled', () => {
        // Make sure to have keycloak enabled for these tests
        before(async () => {
            await IMPORTER.setupFeatures(TestSize.MINIMAL, {
                [Feature.KEYCLOAK]: true,
            });
        });

        it('should be able to login (skip-sso)', () => {
            cy.navigateToApp();
            cy.login(true);
            cy.get('gtx-dashboard-item').should('have.length.gte', 19);
        });

        it('should be able to login (default without skip-sso)', () => {
            cy.navigateToApp('', true);
            cy.login(false);
            cy.get('gtx-dashboard-item').should('have.length.gte', 19);
        });
    });
});
