import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlSegment } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { NgxsModule } from '@ngxs/store';
import { ApplicationStateService, FolderActionsService, STATE_MODULES } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { AuthGuard } from './auth-guard';

class MockRouter {
    navigate = jasmine.createSpy('navigate');
}

class MockFolderActions {
    navigateToDefaultNode = jasmine.createSpy('navigateToDefaultNode');
}

interface MockRouterState {
    route: ActivatedRouteSnapshot;
    state: RouterStateSnapshot;
}

function mockRouterState(url: string): MockRouterState {
    const urlSegments: UrlSegment[] = url
        .split('/')
        .slice(1)
        .map(segment => new UrlSegment(segment, {}));
    return {
        route: { url: urlSegments } as any,
        state: { url } as any,
    };
}

const LOGIN_ROUTE = '/login';
const ITEM_LIST_ROUTE = '/editor/(list:node/3/folder/63)';

describe('AuthGuard', () => {

    let authGuard: AuthGuard;
    let appState: TestApplicationState;
    let folderActions: MockFolderActions;
    let router: MockRouter;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                AuthGuard,
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: Router, useClass: MockRouter },
            ],
        });

        authGuard = TestBed.get(AuthGuard);
        appState = TestBed.get(ApplicationStateService);
        folderActions = TestBed.get(FolderActionsService);
        router = TestBed.get(Router);
    });

    function assertNoRedirectAction(): void {
        expect(router.navigate).not.toHaveBeenCalled();
        expect(folderActions.navigateToDefaultNode).not.toHaveBeenCalled();
    }

    function assertNavigatingToRouteIsAllowed(url: string): void {
        const mockRoute = mockRouterState(url);
        const result = authGuard.canActivate(mockRoute.route, mockRoute.state);

        expect(result).toBe(true);
        assertNoRedirectAction();
    }

    describe('user is not logged in', () => {

        beforeEach(() => {
            appState.mockState({
                auth: {
                    isLoggedIn: false,
                    loggingIn: false,
                },
            });
        });

        it('canActivate() allows accessing the /login route', () => {
            assertNavigatingToRouteIsAllowed(LOGIN_ROUTE);
        });

        it('canActivate() redirects the user from a privileged route to /login, appending the return URL as a parameter', () => {
            const mockRoute = mockRouterState(ITEM_LIST_ROUTE);
            const result = authGuard.canActivate(mockRoute.route, mockRoute.state);

            expect(result).toBe(false);
            expect(router.navigate).toHaveBeenCalledTimes(1);
            expect(router.navigate).toHaveBeenCalledWith([LOGIN_ROUTE], { queryParams: { returnUrl: ITEM_LIST_ROUTE } });
            expect(folderActions.navigateToDefaultNode).not.toHaveBeenCalled();
        });

    });

    describe('user is logging in', () => {

        beforeEach(() => {
            appState.mockState({
                auth: {
                    isLoggedIn: false,
                    loggingIn: true,
                },
            });
        });

        it('canActivate() allows accessing the /login route', () => {
            assertNavigatingToRouteIsAllowed(LOGIN_ROUTE);
        });

        it('canActivate() allows accessing a priviliged URL if the login succeeds', fakeAsync(() => {
            const mockRoute = mockRouterState(ITEM_LIST_ROUTE);
            const result = authGuard.canActivate(mockRoute.route, mockRoute.state);

            expect(result instanceof Promise).toBe(true);
            let accessGranted: boolean;
            (result as Promise<boolean>).then(granted => accessGranted = granted);
            tick();
            expect(accessGranted).toBeUndefined('The promise should not resolve before `loggingIn` changes.');

            // Signal a successful login.
            appState.mockState({
                auth: {
                    isLoggedIn: true,
                    loggingIn: false,
                },
            });

            tick();
            expect(accessGranted).toBe(true);
            assertNoRedirectAction();
        }));

        it('canActivate() redirects from a privileged route to the /login route if the login fails', fakeAsync(() => {
            const mockRoute = mockRouterState(ITEM_LIST_ROUTE);
            const result = authGuard.canActivate(mockRoute.route, mockRoute.state);

            expect(result instanceof Promise).toBe(true);
            let accessGranted: boolean;
            (result as Promise<boolean>).then(granted => accessGranted = granted);

            // Signal a failed login.
            appState.mockState({
                auth: {
                    isLoggedIn: false,
                    loggingIn: false,
                },
            });

            tick();
            expect(accessGranted).toBe(false);
            expect(router.navigate).toHaveBeenCalledTimes(1);
            expect(router.navigate).toHaveBeenCalledWith([LOGIN_ROUTE], { queryParams: { returnUrl: ITEM_LIST_ROUTE } });
            expect(folderActions.navigateToDefaultNode).not.toHaveBeenCalled();
        }));

    });

    describe('user is logged in', () => {

        let subscription: Subscription;

        beforeEach(() => {
            appState.mockState({
                auth: {
                    isLoggedIn: true,
                    loggingIn: false,
                },
            });
            subscription = null;
        });

        afterEach(() => {
            if (subscription) {
                subscription.unsubscribe();
            }
        });

        it('canActivate() allows accessing a privileged route', () => {
            assertNavigatingToRouteIsAllowed(ITEM_LIST_ROUTE);
        });

        it('canActivate() redirects from /login to the default node if there is no activeNode in the AppState', () => {
            const mockRoute = mockRouterState(LOGIN_ROUTE);
            const result = authGuard.canActivate(mockRoute.route, mockRoute.state);

            expect(result instanceof Observable).toBe(true, 'canActive() should return an Observable in this case.');
            let accessGranted: boolean;
            subscription = (result as Observable<boolean>).subscribe(granted => accessGranted = granted);
            expect(accessGranted).toBeUndefined('The observable should not emit until we have a list of nodes in the AppState.');

            appState.mockState({
                folder: {
                    nodesLoaded: true,
                    nodes: {
                        list: [1, 2, 3, 4],
                    },
                },
            });

            expect(accessGranted).toBe(false);
            expect(folderActions.navigateToDefaultNode).toHaveBeenCalledTimes(1);
            expect(router.navigate).not.toHaveBeenCalled();
        });

        it('canActivate() does not allow accessing /login, ' +
            'but does not redirect to default node if there is an activeNode in the AppState', () => {
            const mockRoute = mockRouterState(LOGIN_ROUTE);
            const result = authGuard.canActivate(mockRoute.route, mockRoute.state);

            expect(result instanceof Observable).toBe(true, 'canActivate() should return an Observable in this case.');
            let accessGranted: boolean;
            subscription = (result as Observable<boolean>).subscribe(granted => accessGranted = granted);
            expect(accessGranted).toBeUndefined('The observable should not emit until we have a list of nodes in the AppState.');

            appState.mockState({
                folder: {
                    activeNode: 2,
                    nodesLoaded: true,
                    nodes: {
                        list: [1, 2, 3, 4],
                    },
                },
            });

            expect(accessGranted).toBe(false);
            assertNoRedirectAction();
        });

    });

});
