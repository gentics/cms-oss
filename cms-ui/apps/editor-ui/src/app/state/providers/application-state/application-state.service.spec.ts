import { TestBed } from '@angular/core/testing';
import { NgxsModule } from '@ngxs/store';
import { getExampleUserData } from '../../../../testing/test-data.mock';
import { STATE_MODULES } from '../../modules';
import { LoginSuccessAction, LogoutSuccessAction, StartLoginAction, StartLogoutAction } from '../../modules/auth/auth.actions';
import { TestApplicationState } from '../../test-application-state.mock';
import { ApplicationStateService } from './application-state.service';

describe('ApplicationStateService', () => {

    let appState: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
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
            isAdmin: false,
            isLoggedIn: false,
            loggingIn: false,
            loggingOut: false,
            changingPassword: false,
            currentUserId: null,
            sid: null,
            lastError: '',
        });
    });

    it('can be subscribed to via select()', () => {
        const emittedValues: number[] = [];
        const sub = appState
            .select(state => state.auth.currentUserId)
            .subscribe(id => emittedValues.push(id));

        expect(emittedValues.length).toBe(1);
        appState.dispatch(new StartLoginAction());
        appState.dispatch(new LoginSuccessAction(7777, getExampleUserData({ id: 1234 })));

        expect(emittedValues.length).toBe(2);
        expect(emittedValues[1]).toBe(1234);

        appState.dispatch(new StartLogoutAction());
        appState.dispatch(new LogoutSuccessAction());

        expect(emittedValues.length).toBe(3);
        expect(emittedValues[2]).toBe(null);

        sub.unsubscribe();
    });

    it('select() result can be unsubscribed from', () => {
        const emittedValues: number[] = [];
        const sub = appState
            .select(state => state.auth.currentUserId)
            .subscribe(id => emittedValues.push(id));

        expect(emittedValues.length).toBe(1);
        sub.unsubscribe();

        appState.dispatch(new StartLoginAction());
        expect(emittedValues.length).toBe(1);

        appState.dispatch(new LoginSuccessAction(7777, getExampleUserData({ id: 1234 })));
        expect(emittedValues.length).toBe(1);

        appState.dispatch(new StartLogoutAction());
        expect(emittedValues.length).toBe(1);

        appState.dispatch(new LogoutSuccessAction());
        expect(emittedValues.length).toBe(1);
    });

    it('provides the current app state with "state"', () => {
        expect(appState.now.auth.currentUserId).toBeNull();

        appState.dispatch(new StartLoginAction());
        expect(appState.now.auth.loggingIn).toBe(true);

        appState.dispatch(new LoginSuccessAction(7777, getExampleUserData({ id: 1234 })));
        expect(appState.now.auth.currentUserId).toBe(1234);
        expect(appState.now.auth.sid).toBe(7777);

        appState.dispatch(new StartLogoutAction());
        expect(appState.now.auth.loggingOut).toBe(true);

        appState.dispatch(new LogoutSuccessAction());
        expect(appState.now.auth.loggingOut).toBe(false);
        expect(appState.now.auth.isLoggedIn).toBe(false);
        expect(appState.now.auth.currentUserId).toBe(null);
        expect(appState.now.auth.sid).toBe(null);
    });

    it('can mock the application state for testing', () => {
        appState.mockState({
            auth: { currentUserId: 1234 },
        });
        expect(appState.now.auth.currentUserId).toBe(1234, 'appState.state');
    });

});
