import { Component, OnInit } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { ButtonComponent } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { MockErrorHandler } from '../../../core/providers/error-handler/error-handler.mock';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { LocalStorage } from '../../../core/providers/local-storage/local-storage.service';
import { ApplicationStateService, AuthActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { SingleSignOnComponent } from './single-sign-on.component';

class MockActivatedRoute {}

class MockAuthActions {}

class MockLocalStorage {}

class MockRouter {}

@Component({
    template: '<gtx-single-sign-on />',
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

describe('SingleSignOnComponent', () => {
    let state: TestApplicationState;

    beforeEach(() => {
        configureComponentTest({
            providers: [
                { provide: ActivatedRoute, useClass: MockActivatedRoute },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: AuthActionsService, useClass: MockAuthActions },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: LocalStorage, useClass: MockLocalStorage },
                { provide: Router, useClass: MockRouter },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
            ],
            declarations: [
                ButtonComponent,
                SingleSignOnComponent,
                TestComponent,
            ],
        });

        state = TestBed.get(ApplicationStateService);
        expect(state instanceof TestApplicationState).toBe(true);
    });

    it('call nothing if showSSOButton is true and keycloakEnabled is true',
        componentTest(() => TestComponent, (fixture, instance) => {
            state.mockState({
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
            state.mockState({
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
