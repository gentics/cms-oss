import { AppStateService } from '@admin-ui/state';
import { assembleTestAppStateImports, TEST_APP_STATE, TestAppState } from '@admin-ui/state/utils/test-app-state';
import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlSegment } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthGuard } from './auth.guard';

class MockRouter {
    navigate = jasmine.createSpy('navigate');
    navigateByUrl = jasmine.createSpy('navigateByUrl');
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

const ROOT_ROUTE = '/';
const LOGIN_ROUTE = '/login';
const USER_MODULE_ROUTE = '/users';

describe('AuthGuard', () => {

    let authGuard: AuthGuard;
    let appState: TestAppState;
    let router: MockRouter;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                AuthGuard,
                TEST_APP_STATE,
                { provide: Router, useClass: MockRouter },
            ],
        });

        authGuard = TestBed.inject(AuthGuard);
        appState = TestBed.inject(AppStateService) as any;
        router = TestBed.inject(Router) as any;
    });

    function assertNoRedirectAction(): void {
        expect(router.navigate).not.toHaveBeenCalled();
        expect(router.navigateByUrl).not.toHaveBeenCalled();
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

        function assertRedirectToLogin(url: string): void {
            const mockRoute = mockRouterState(url);
            const result = authGuard.canActivate(mockRoute.route, mockRoute.state);

            expect(result).toBe(false);
            expect(router.navigate).toHaveBeenCalledTimes(1);
            expect(router.navigate).toHaveBeenCalledWith([LOGIN_ROUTE], { queryParams: { returnUrl: url } });
        }

        it('canActivate() allows accessing the /login route', () => {
            assertNavigatingToRouteIsAllowed(LOGIN_ROUTE);
        });

        it('canActivate() redirects the user from the root route to /login', () => {
            assertRedirectToLogin(ROOT_ROUTE);
        });

        it('canActivate() redirects the user from a privileged route to /login, appending the return URL as a parameter', () => {
            assertRedirectToLogin(USER_MODULE_ROUTE);
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
            const mockRoute = mockRouterState(USER_MODULE_ROUTE);
            const result = authGuard.canActivate(mockRoute.route, mockRoute.state);

            expect(typeof result === 'object' && typeof (result as Observable<boolean>).subscribe === 'function').toBe(true);
            let accessGranted: boolean;
            (result as Observable<boolean>).subscribe(granted => accessGranted = granted);
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
            const mockRoute = mockRouterState(USER_MODULE_ROUTE);
            const result = authGuard.canActivate(mockRoute.route, mockRoute.state);

            expect(typeof result === 'object' && typeof (result as Observable<boolean>).subscribe === 'function').toBe(true);
            let accessGranted: boolean;
            (result as Observable<boolean>).subscribe(granted => accessGranted = granted);

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
            expect(router.navigate).toHaveBeenCalledWith([LOGIN_ROUTE], { queryParams: { returnUrl: USER_MODULE_ROUTE } });
        }));

    });

    describe('user is logged in', () => {

        beforeEach(() => {
            appState.mockState({
                auth: {
                    isLoggedIn: true,
                    loggingIn: false,
                },
            });
        });

        it('canActivate() allows accessing a privileged route', () => {
            assertNavigatingToRouteIsAllowed(USER_MODULE_ROUTE);
        });

        it('canActivate() redirects from /login to the root route', () => {
            const mockRoute = mockRouterState(LOGIN_ROUTE);
            const result = authGuard.canActivate(mockRoute.route, mockRoute.state);

            expect(result).toBe(false);
            expect(router.navigateByUrl).toHaveBeenCalledTimes(1);
            expect(router.navigateByUrl).toHaveBeenCalledWith(ROOT_ROUTE);
        });

    });

});
