import { assembleTestAppStateImports, TestAppState, TEST_APP_STATE } from '@admin-ui/state/utils/test-app-state';
import { TestBed } from '@angular/core/testing';
import { ActionType, ofActionDispatched } from '@ngxs/store';
import { takeUntil } from 'rxjs/operators';
import { ConstructorOf, ObservableStopper } from '../../../common';
import { AppStateService, ClearAllEntities, ClearAllPermissions, ClearMessageState } from '../../../state';
import { LogoutCleanupService } from './logout-cleanup.service';

const CLEAR_ACTIONS = [ ClearAllEntities, ClearAllPermissions, ClearMessageState ];

describe('LogoutCleanupService', () => {

    let appState: TestAppState;
    let logoutCleanup: LogoutCleanupService;
    let stopper: ObservableStopper;

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

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                LogoutCleanupService,
                TEST_APP_STATE,
            ],
        });

        appState = TestBed.inject(AppStateService) as any;
        logoutCleanup = TestBed.inject(LogoutCleanupService);
        stopper = new ObservableStopper();
    });

    afterEach(() => {
        stopper.stop();
    });

    function assertActionDispatched(actionCtor: ConstructorOf<any>, dispatchedActions: any[]): void {
        const dispatched = dispatchedActions.find(action => action instanceof actionCtor);
        expect(dispatched).toBeTruthy(`Action ${actionCtor} was not dispatched.`);
    }

    it('cleans up all state branches that need cleanup', () => {
        logoutCleanup.init();
        simulateLogin();

        const dispatchedActions: any[] = [];
        appState.trackActions().pipe(
            ofActionDispatched(...CLEAR_ACTIONS as any),
            takeUntil(stopper.stopper$),
        ).subscribe(action => dispatchedActions.push(action));

        simulateLogout();
        expect(dispatchedActions.length).toBe(CLEAR_ACTIONS.length);
        CLEAR_ACTIONS.forEach(clearAction => assertActionDispatched(clearAction, dispatchedActions));
    });

});
