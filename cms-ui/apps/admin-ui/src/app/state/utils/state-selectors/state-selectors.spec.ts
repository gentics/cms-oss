import { ObservableStopper } from '@admin-ui/common';
import { TestBed } from '@angular/core/testing';
import { takeUntil } from 'rxjs/operators';
import { AppStateService } from '../..';
import { assembleTestAppStateImports, TEST_APP_STATE, TestAppState } from '../test-app-state';
import { selectLoginEventOrIsLoggedIn, selectLogoutEvent } from './state-selectors';

describe('Common state selectors', () => {

    let appState: TestAppState;
    let stopper: ObservableStopper;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                TEST_APP_STATE,
            ],
        });

        appState = TestBed.inject(AppStateService) as any;
        stopper = new ObservableStopper();
    });

    afterEach(() => {
        stopper.stop();
    });

    function simulateLogin(): void {
        appState.mockState({
            auth: {
                isLoggedIn: true,
            },
        });
    }

    function simulateLogout(): void {
        appState.mockState({
            auth: {
                isLoggedIn: false,
            },
        });
    }

    describe('selectLoginEventOrIsLoggedIn', () => {

        let loginEmissions: number;

        beforeEach(() => {
            loginEmissions = 0;
        });

        function subscribeToLogin(): void {
            selectLoginEventOrIsLoggedIn(appState).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(() => ++loginEmissions);
        }

        it('emits upon login if it is subscribed to before the user logs in', () => {
            subscribeToLogin();
            expect(loginEmissions).toBe(0);

            simulateLogin();
            expect(loginEmissions).toBe(1);
        });

        it('emits if it is subscribed to if the user is already logged in', () => {
            simulateLogin();
            subscribeToLogin();
            expect(loginEmissions).toBe(1);
        });

        it('emits upon login if the user logs out and in again', () => {
            subscribeToLogin();

            simulateLogin();
            expect(loginEmissions).toBe(1);

            simulateLogout();
            expect(loginEmissions).toBe(1);

            simulateLogin();
            expect(loginEmissions).toBe(2);
        });

    });

    describe('selectLogoutEvent', () => {

        let logoutEmissions: number;

        beforeEach(() => {
            logoutEmissions = 0;
        });

        function subscribeToLogoutEvent(): void {
            selectLogoutEvent(appState).pipe(
                takeUntil(stopper.stopper$),
            ).subscribe(() => ++logoutEmissions);
        }

        it('emits upon logout if it is subscribed to after the user logs in', () => {
            simulateLogin();
            subscribeToLogoutEvent();
            expect(logoutEmissions).toBe(0);

            simulateLogout();
            expect(logoutEmissions).toBe(1);
        });

        it('emits upon logout if it is subscribed to before the user logs in', () => {
            subscribeToLogoutEvent();

            simulateLogin();
            expect(logoutEmissions).toBe(0);

            simulateLogout();
            expect(logoutEmissions).toBe(1);
        });

        it('emits upon logout if the user logs in and out again', () => {
            subscribeToLogoutEvent();

            simulateLogin();
            expect(logoutEmissions).toBe(0);

            simulateLogout();
            expect(logoutEmissions).toBe(1);

            simulateLogin();
            expect(logoutEmissions).toBe(1);

            simulateLogout();
            expect(logoutEmissions).toBe(2);
        });

    });

});
