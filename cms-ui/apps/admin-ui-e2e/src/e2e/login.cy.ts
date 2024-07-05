import { ENV_KEYCLOAK_ENABLED, envAll, envNone, skipableSuite } from "@gentics/e2e-utils";

describe('Login', () => {

    skipableSuite(envNone(ENV_KEYCLOAK_ENABLED), 'Without keycloak feature enabled', () => {
        it('should be able to login', () => {
            cy.navigateToApp();
            cy.login(true);
            cy.get('gtx-dashboard-item').should('have.length.gte', 19);
        });
    });

    skipableSuite(envAll(ENV_KEYCLOAK_ENABLED), 'With keycloak feature enabled', () => {
        it('should be able to login (skip-sso)', () => {
            cy.navigateToApp('/?skip-sso');
            cy.login(true);
            cy.get('gtx-dashboard-item').should('have.length.gte', 19);
        });

        it('should be able to login (default without skip-sso)', () => {
            cy.navigateToApp();
            cy.login(false);
            cy.get('gtx-dashboard-item').should('have.length.gte', 19);
        });
    });
});
