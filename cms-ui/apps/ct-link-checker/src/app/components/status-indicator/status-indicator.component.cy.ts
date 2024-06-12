import { StatusIndicatorComponent } from './status-indicator.component';

describe('StatusIndicatorComponent', () => {

    it('should render the icon correctly', () => {
        cy.mount(StatusIndicatorComponent, {
            componentProperties: {
                alert: false,
                info: false,
                success: true,
                iconName: 'home',
            },
        });

        cy.get('.status > .material-icons')
            .should('have.class', 'success')
            .and('have.text', 'home');
    });
});
