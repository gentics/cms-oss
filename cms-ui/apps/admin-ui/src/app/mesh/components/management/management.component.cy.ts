import { CoreModule } from '@admin-ui/core/core.module';
import { I18nNotificationService } from '@admin-ui/core/providers/i18n-notification/i18n-notification.service';
import { AppStateService } from '@admin-ui/state/providers/app-state/app-state.service';
import { TestAppState } from '@admin-ui/state/utils/test-app-state/test-app-state.mock';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';
import { ContentRepository, ContentRepositoryType } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { MeshRestClientConfig } from '@gentics/mesh-rest-client';
import { LoginGateComponent } from '../login-gate/login-gate.component';
import { ManagementComponent } from './management.component';

// TODO: Create a test client service like the cms-client in the mesh package
class MockMeshRestClientService implements Partial<MeshRestClientService> {
    init(config: MeshRestClientConfig, apiKey?: string): void {
        // nothing to do
    }

    auth = {
        me: () => ({
            send: () => Promise.resolve({
                username: 'anonymous',
            }),
        }),
    } as any;
}

class MockI18nNotificationService {}

// Yet to find out why the providers aren't overriding anything.
describe('ManagementComponent', () => {

    it('should display the login when not logged in', () => {
        const REPO: ContentRepository = {
            id: 1,
            name: 'text',
            crType: ContentRepositoryType.MESH,
        } as any;

        cy.mount(ManagementComponent, {
            imports: [
                GenticsUICoreModule.forRoot(),
                CoreModule,
                RouterTestingModule,
            ],
            declarations: [
                LoginGateComponent,
            ],
            providers: [
                { provide: MeshRestClientService, useClass: MockMeshRestClientService },
                { provide: AppStateService, useClass: TestAppState },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                { provide: I18nNotificationService, useClass: MockI18nNotificationService },
            ],
            schemas: [CUSTOM_ELEMENTS_SCHEMA],
            componentProperties: {
                repository: REPO,
            },
        })

        cy.get('.login')
            .should('exist')
            .and('be.visible');
    });

});
