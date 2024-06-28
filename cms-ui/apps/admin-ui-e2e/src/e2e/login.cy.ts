describe('App', () => {
    it('should be able to login (skip sso)', () => {
        cy.visit('http://localhost:8080/admin/?skip-sso', { });
        cy.login(true);
        cy.get('gtx-dashboard-item').should('have.length.gte', 19);
    });

    // Not available in OSS
    xit('should be able to login (with sso)', () => {
        cy.visit('http://localhost:8080/admin/', { });
        cy.login(false);
        cy.get('gtx-dashboard-item').should('have.length.gte', 19);
    });
});
