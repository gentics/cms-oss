import { ManagementComponent } from './management.component';

describe('ManagementComponent', () => {

    it('should display the login when not logged in', () => {
        cy.mount(ManagementComponent);
        cy.get('gtx-mesh-login-gate').contains('.login');
    });

});
