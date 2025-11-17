import { TestBed } from '@angular/core/testing';
import { RouterModule } from '@angular/router';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { KeycloakService } from './keycloak.service';

describe('KeycloakService', () => {
    let keycloakService: KeycloakService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                KeycloakService,
                {
                    provide: GCMSRestClientService,
                    useClass: GCMSTestRestClientService,
                },
            ],
            imports: [RouterModule.forRoot([])],
        });

        keycloakService = TestBed.inject(KeycloakService);
    });

    it('can be created', () => {
        expect(keycloakService).toBeTruthy();
    });
});
