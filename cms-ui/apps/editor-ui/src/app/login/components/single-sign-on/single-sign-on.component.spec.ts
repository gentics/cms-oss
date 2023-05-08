import { Component, OnInit } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { ButtonComponent } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { MockErrorHandler } from '../../../core/providers/error-handler/error-handler.mock';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { LocalStorage } from '../../../core/providers/local-storage/local-storage.service';
import { ApplicationStateService, AuthActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { KeycloakService } from '../../providers/keycloak/keycloak.service';
import { SingleSignOn } from './single-sign-on.component';

class MockActivatedRoute {}

class MockAuthActions {}

class MockKeycloakService {
    keycloakEnabled = true;
    showSSOButton = false;

    login(): void {}
}

class MockLocalStorage {}

class MockRouter {}

@Component({
    template: `<single-sign-on></single-sign-on>`,
    })
class TestComponent implements OnInit {
    ngOnInitSpy = jasmine.createSpy('ngOnInit');
    attemptSsoWithKeycloakSpy = jasmine.createSpy('attemptSsoWithKeycloak');
    attemptSsoWithIframeSpy = jasmine.createSpy('attemptSsoWithIframe');
    loginSpy = jasmine.createSpy('login');

    ngOnInit(): void {
        this.ngOnInitSpy();
    }
}

describe('SingleSignOn', () => {
    let keycloakService: MockKeycloakService;
    let state: TestApplicationState;

    beforeEach(() => {
        configureComponentTest({
            imports: [],
            providers: [
                { provide: ActivatedRoute, useClass: MockActivatedRoute },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: AuthActionsService, useClass: MockAuthActions },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: KeycloakService, useClass: MockKeycloakService },
                { provide: LocalStorage, useClass: MockLocalStorage },
                { provide: Router, useClass: MockRouter },
            ],
            declarations: [
                ButtonComponent,
                SingleSignOn,
                TestComponent,
            ],
        });

        keycloakService = TestBed.get(KeycloakService);

        state = TestBed.get(ApplicationStateService);
        expect(state instanceof TestApplicationState).toBe(true);
    });

    it('call nothing if showSSOButton is true and keycloakEnabled is true',
        componentTest(() => TestComponent, (fixture, instance) => {
            keycloakService.keycloakEnabled = true;
            keycloakService.showSSOButton = true;

            fixture.detectChanges();
            tick();

            expect(instance.attemptSsoWithKeycloakSpy).not.toHaveBeenCalled();
            expect(instance.attemptSsoWithIframeSpy).not.toHaveBeenCalled();
        }),
    );

    it('call nothing if showSSOButton is true and keycloakEnabled is false',
        componentTest(() => TestComponent, (fixture, instance) => {
            keycloakService.keycloakEnabled = false;
            keycloakService.showSSOButton = true;

            fixture.detectChanges();
            tick();

            expect(instance.attemptSsoWithKeycloakSpy).not.toHaveBeenCalled();
            expect(instance.attemptSsoWithIframeSpy).not.toHaveBeenCalled();
        }),
    );
});
