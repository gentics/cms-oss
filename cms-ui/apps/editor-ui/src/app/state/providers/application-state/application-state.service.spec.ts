import { TestBed } from '@angular/core/testing';
import {
    AuthenticationModule,
    LoginStart,
    LoginSuccess,
    LogoutStart,
    LogoutSuccess,
} from '@gentics/cms-components/auth';
import { getExampleUserData } from '@gentics/cms-models/testing/test-data.mock';
import { NgxsModule } from '@ngxs/store';
import { STATE_MODULES } from '../../modules';
import { TestApplicationState } from '../../test-application-state.mock';
import { ApplicationStateService } from './application-state.service';

describe('ApplicationStateService', () => {

    let appState: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                NgxsModule.forRoot(STATE_MODULES),
                AuthenticationModule.forRoot(),
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        appState = TestBed.get(ApplicationStateService);
    });

    it('can be created', () => {
        expect(appState).toBeDefined();
    });

    it('lets his actions set the initial value', () => {
        expect(appState.now.auth).toEqual({
            isLoggedIn: false,
            loggingIn: false,
            loggingOut: false,
            changingPassword: false,
            user: null,
            sid: null,
            lastError: null,
            keycloakAvailable: null,
            keycloakError: null,
            showSingleSignOnButton: false,
            ssoSkipped: false,
        });
    });

    it('can be subscribed to via select()', () => {
        const emittedValues: number[] = [];
        const sub = appState
            .select(state => state.auth.user)
            .subscribe(user => emittedValues.push(user?.id ?? null));

        expect(emittedValues.length).toBe(1);
        appState.dispatch(new LoginStart());
        appState.dispatch(new LoginSuccess(7777, getExampleUserData({ id: 1234 })));

        expect(emittedValues.length).toBe(2);
        expect(emittedValues[1]).toBe(1234);

        appState.dispatch(new LogoutStart());
        appState.dispatch(new LogoutSuccess());

        expect(emittedValues.length).toBe(3);
        expect(emittedValues[2]).toEqual(null);

        sub.unsubscribe();
    });

    it('select() result can be unsubscribed from', () => {
        const emittedValues: number[] = [];
        const sub = appState
            .select(state => state.auth.user)
            .subscribe(user => emittedValues.push(user?.id ?? null));

        expect(emittedValues.length).toBe(1);
        sub.unsubscribe();

        appState.dispatch(new LoginStart());
        expect(emittedValues.length).toBe(1);

        appState.dispatch(new LoginSuccess(7777, getExampleUserData({ id: 1234 })));
        expect(emittedValues.length).toBe(1);

        appState.dispatch(new LogoutStart());
        expect(emittedValues.length).toBe(1);

        appState.dispatch(new LogoutSuccess());
        expect(emittedValues.length).toBe(1);
    });

    it('provides the current app state with "state"', () => {
        expect(appState.now.auth.user).toEqual(null);

        appState.dispatch(new LoginStart());
        expect(appState.now.auth.loggingIn).toBe(true);

        appState.dispatch(new LoginSuccess(7777, getExampleUserData({ id: 1234 })));
        expect(appState.now.auth.user?.id).toBe(1234);
        expect(appState.now.auth.sid).toBe(7777);

        appState.dispatch(new LogoutStart());
        expect(appState.now.auth.loggingOut).toBe(true);

        appState.dispatch(new LogoutSuccess());
        expect(appState.now.auth.loggingOut).toBe(false);
        expect(appState.now.auth.isLoggedIn).toBe(false);
        expect(appState.now.auth.user).toEqual(null);
        expect(appState.now.auth.sid).toBe(null);
    });

    it('can mock the application state for testing', () => {
        appState.mockState({
            auth: {
                user: {
                    id: 1234
                } as any,
            },
        });
        expect(appState.now.auth.user.id).toBe(1234, 'appState.state');
    });

});
