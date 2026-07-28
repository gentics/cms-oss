import { Component, OnInit } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { componentTest, configureComponentTest, MockErrorHandler } from '../../../../testing';
import { ErrorHandler } from '../../../core/providers/error-handler';
import { AuthOperations } from '../../../core/providers/operations/auth/auth.operations';
import { AppStateService } from '../../../state';
import { assembleTestAppStateImports, TestAppState } from '../../../state/utils/test-app-state';
import { SingleSignOnComponent } from './single-sign-on.component';

class MockActivatedRoute {}

class MockAuthOperations {}

class MockRouter {}

@Component({
    template: '<gtx-single-sign-on/>',
    standalone: false,
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

    beforeEach(() => {
        configureComponentTest({
            imports: [
                ...assembleTestAppStateImports(),
            ],
            providers: [
                { provide: ActivatedRoute, useClass: MockActivatedRoute },
                { provide: AppStateService, useClass: TestAppState },
                { provide: AuthOperations, useClass: MockAuthOperations },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: Router, useClass: MockRouter },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
            ],
            declarations: [
                SingleSignOnComponent,
                TestComponent,
            ],
        });

        appState = TestBed.inject(AppStateService) as any;
    });

    it('call nothing if showSSOButton is true and keycloakEnabled is true',
        componentTest(() => TestComponent, (fixture, instance) => {
            appState.mockState({
                auth: {
                    keycloakAvailable: true,
                    showSingleSignOnButton: true,
                },
            });

            fixture.detectChanges();
            tick();

            expect(instance.attemptSsoWithKeycloakSpy).not.toHaveBeenCalled();
            expect(instance.attemptSsoWithIframeSpy).not.toHaveBeenCalled();
        }),
    );

    it('call nothing if showSSOButton is true and keycloakEnabled is false',
        componentTest(() => TestComponent, (fixture, instance) => {
            appState.mockState({
                auth: {
                    keycloakAvailable: false,
                    showSingleSignOnButton: true,
                },
            });

            fixture.detectChanges();
            tick();

            expect(instance.attemptSsoWithKeycloakSpy).not.toHaveBeenCalled();
            expect(instance.attemptSsoWithIframeSpy).not.toHaveBeenCalled();
        }),
    );
});
