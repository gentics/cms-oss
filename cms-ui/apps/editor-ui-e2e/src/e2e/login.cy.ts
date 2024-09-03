import { Feature, Variant } from '@gentics/cms-models';
import { EntityImporter, isVariant, skipableSuite, TestSize } from '@gentics/e2e-utils';
import { AUTH_ADMIN, AUTH_KEYCLOAK } from '../support/common';

describe('Login', () => {

    const IMPORTER = new EntityImporter();

    const ALIAS_SSO_LOGIN = '@ssoLogin';

    describe('Without keycloak feature enabled', () => {
        // Make sure to have keycloak disabled for these tests
        before(async () => {
            // If this client isn't recreated for WHATEVER reason, the CMS gives back a 401 for importer requests.
            IMPORTER.client = null;
            await IMPORTER.setupFeatures({
                [Feature.KEYCLOAK]: false,
            });
            await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        });

        beforeEach(async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        it('should be able to login', () => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.get('project-editor').should('exist');
        });
    });

    // Keycloak is an enterprise edition feature, therefore we can only test it in that variant.
    skipableSuite(isVariant(Variant.ENTERPRISE), 'With keycloak feature enabled', () => {
        // Make sure to have keycloak enabled for these tests
        before(async () => {
            // If this client isn't recreated for WHATEVER reason, the CMS gives back a 401 for importer requests.
            IMPORTER.client = null;
            await IMPORTER.setupFeatures({
                [Feature.KEYCLOAK]: true,
            });
            await IMPORTER.bootstrapSuite(TestSize.MINIMAL);
        });

        beforeEach(async () => {
            await IMPORTER.cleanupTest();
            await IMPORTER.setupTest(TestSize.MINIMAL);
        });

        it('should be able to login (skip-sso)', () => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.get('project-editor').should('exist');
        });

        // TODO: Temporarly skipped, because we can't change the features at runtime currently
        it.skip('should be able to login (default without skip-sso)', () => {
            cy.navigateToApp('', true);
            cy.login(AUTH_KEYCLOAK, true);

            // Wait for login to be finished
            cy.intercept({
                pathname: '/rest/auth/ssologin',
            }).as(ALIAS_SSO_LOGIN);
            cy.wait(ALIAS_SSO_LOGIN, { timeout: 60_000 });

            cy.get('project-editor').should('exist');
        });
    });
});
