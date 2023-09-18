describe('App', () => {
    it('should be able to login (skip sso)', () => {
        cy.visit('/?skip-sso', { });
        cy.login(true);
        cy.get('gtx-dashboard-item').should('have.length.gte', 19);
    });

    xit('should be able to login (with sso)', () => {
        cy.visit('/', { });
        cy.login(false);
        cy.get('gtx-dashboard-item').should('have.length.gte', 19);
    });
});
