import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { NgxsModule } from '@ngxs/store';
import { ApplicationStateService, STATE_MODULES } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { KeycloakService } from './keycloak.service';

class MockHttpClient {}

describe('KeycloakService', () => {

    let appState: TestApplicationState;
    let keycloakService: KeycloakService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                KeycloakService,
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: HttpClient, useClass: MockHttpClient },
            ],
        });

        appState = TestBed.get(ApplicationStateService);
        keycloakService = TestBed.get(KeycloakService);
    });

    it('can be created', () => {
        expect(keycloakService).toBeTruthy();
    });
});
