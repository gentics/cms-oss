import { Feature, Variant } from '@gentics/cms-models';
import { EntityImporter, isVariant, skipableSuite } from '@gentics/e2e-utils';
import { AUTH_ADMIN, AUTH_KEYCLOAK } from '../support/app.po';

describe('Login', () => {

    const IMPORTER = new EntityImporter();

    beforeEach(() => {
        cy.muteXHR();
    });

    describe('Without keycloak feature enabled', () => {
        // Make sure to have keycloak disabled for these tests
        before(() => {
            cy.wrap(null, { log: false }).then(() => {
                return cy.wrap(IMPORTER.setupFeatures({
                    [Feature.KEYCLOAK]: false,
                }), { log: false, timeout: 60_000 });
            });
        });

        it('should be able to login', () => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.get('gtx-dashboard').should('exist');
        });
    });

    // Keycloak is an enterprise edition feature, therefore we can only test it in that variant.
    skipableSuite(isVariant(Variant.ENTERPRISE), 'With keycloak feature enabled', () => {
        // Make sure to have keycloak enabled for these tests
        before(() => {
            cy.wrap(null, { log: false }).then(() => {
                return cy.wrap(IMPORTER.setupFeatures({
                    [Feature.KEYCLOAK]: true,
                }), { log: false, timeout: 60_000 });
            });
        });

        it('should be able to login (skip-sso)', () => {
            cy.navigateToApp();
            cy.login(AUTH_ADMIN);
            cy.get('gtx-dashboard').should('exist');
        });

        // TODO: Temporarly skipped, because we can't change the features at runtime currently
        it.skip('should be able to login (default without skip-sso)', () => {
            cy.navigateToApp('', true);
            cy.login(AUTH_KEYCLOAK, true);
            cy.get('gtx-dashboard').should('exist');
        });
    });
});
