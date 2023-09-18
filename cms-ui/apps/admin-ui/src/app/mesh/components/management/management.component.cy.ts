import { EntityManagerService } from '@admin-ui/core';
import { InitializableServiceBase } from '@gentics/cms-components';
import { ManagementComponent } from './management.component';
class MockEntityManager extends InitializableServiceBase {
    protected onServiceInit(): void { }
}

// Yet to find out why the providers aren't overriding anything.
describe('ManagementComponent', () => {

    xit('should display the login when not logged in', () => {
        cy.mount(ManagementComponent, {
            imports: [
                // RouterModule,
                // CoreModule,
                // MeshModule,
            ],
            providers: [
                { provide: EntityManagerService, useClass: MockEntityManager },
            ],
        }).find('.login').should('exist');
    });

});
