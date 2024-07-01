import { ENV_KEYCLOAK_ENABLED } from "@gentics/e2e-utils";

describe('Login', () => {
    describe('Without keycloak feature enabled', { env: { [ENV_KEYCLOAK_ENABLED]: false } }, () => {
        it('should be able to login', () => {
            cy.visit('http://localhost:8080/admin/', { });
            cy.login(true);
            cy.get('gtx-dashboard-item').should('have.length.gte', 19);
        });
    });

    describe('With keycloak feature enabled', { env: { [ENV_KEYCLOAK_ENABLED]: true } }, () => {
        it('should be able to login (skip-sso)', () => {
            cy.visit('http://localhost:8080/admin/?skip-sso', { });
            cy.login(true);
            cy.get('gtx-dashboard-item').should('have.length.gte', 19);
        });

        it('should be able to login (default without skip-sso)', () => {
            cy.visit('http://localhost:8080/admin/', { });
            cy.login(false);
            cy.get('gtx-dashboard-item').should('have.length.gte', 19);
        });
    });
});
