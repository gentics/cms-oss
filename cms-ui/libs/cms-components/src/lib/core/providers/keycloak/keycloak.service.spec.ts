import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { Store } from '@ngxs/store';
import { AppStateService } from '../../../../../../../apps/admin-ui/src/app/state';
import { TestAppState } from '../../../../../../../apps/admin-ui/src/app/state/utils/test-app-state';
import { MockStore } from '../../../../../../../apps/admin-ui/src/app/state/utils/test-app-state/test-store.mock';
import { KeycloakService } from './keycloak.service';

class MockHttpClient {}

describe('KeycloakService', () => {

    let appState: TestAppState;
    let keycloakService: KeycloakService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                KeycloakService,
                { provide: AppStateService, useClass: TestAppState },
                { provide: HttpClient, useClass: MockHttpClient },
                { provide: Store, useClass: MockStore },
            ],
        });

        appState = TestBed.get(AppStateService);
        keycloakService = TestBed.get(KeycloakService);
    });

    it('can be created', () => {
        expect(keycloakService).toBeTruthy();
    });
});
