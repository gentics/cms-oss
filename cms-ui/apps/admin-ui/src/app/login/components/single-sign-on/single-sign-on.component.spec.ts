import { Component, OnInit } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { Store } from '@ngxs/store';
import { NGXLogger } from 'ngx-logger';
import { componentTest, configureComponentTest } from '../../../../testing';
import { ErrorHandler } from '../../../core/providers/error-handler';
import { MockErrorHandler } from '../../../core/providers/error-handler/error-handler.mock';
import { AuthOperations } from '../../../core/providers/operations/auth/auth.operations';
import { AppStateService } from '../../../state';
import { TestAppState } from '../../../state/utils/test-app-state';
import { MockStore } from '../../../state/utils/test-app-state/test-store.mock';
import { KeycloakService } from '../../providers/keycloak/keycloak.service';
import { SingleSignOnComponent } from './single-sign-on.component';

class MockActivatedRoute {}

class MockAuthOperations {}

class MockKeycloakService {
    keycloakEnabled = true;
    showSSOButton = false;

    login(): void {}
}

class MockNGXLogger {}

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
    let appState: TestAppState;
    let keycloakService: MockKeycloakService;

    beforeEach(() => {
        configureComponentTest({
            imports: [],
            providers: [
                { provide: ActivatedRoute, useClass: MockActivatedRoute },
                { provide: AppStateService, useClass: TestAppState },
                { provide: AuthOperations, useClass: MockAuthOperations },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: KeycloakService, useClass: MockKeycloakService },
                { provide: NGXLogger, useClass: MockNGXLogger },
                { provide: Router, useClass: MockRouter },
                { provide: Store, useClass: MockStore },
            ],
            declarations: [
                SingleSignOnComponent,
                TestComponent,
            ],
        });

        appState = TestBed.get(AppStateService);
        keycloakService = TestBed.get(KeycloakService);
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
