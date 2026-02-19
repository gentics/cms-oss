import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { LogoutSuccess } from '@gentics/cms-components/auth';
import { ResponseCode } from '@gentics/cms-models';
import { ApiError } from '@gentics/cms-rest-clients-angular';
import { ModalService } from '@gentics/ui-core';
import { TranslateService } from '@ngx-translate/core';
import { Actions, ActionType, ofActionDispatched } from '@ngxs/store';
import { Observable } from 'rxjs';
import { AppStateService } from '../../../state';
import { assembleTestAppStateImports, TEST_APP_STATE, TestAppState } from '../../../state/utils/test-app-state';
import { I18nNotificationService } from '../i18n-notification/i18n-notification.service';
import { ErrorHandler } from './error-handler.service';

class MockNotificationService {
    show = jasmine.createSpy('notification.show');
}

class MockConsole {
    constructor(private originalConsole: Console) {
        this.error = jasmine.createSpy('console.error', this.error).and.callThrough();
    }

    error(message?: any, ...optionalParams: any[]): void {
        // Error details are logged to the console unless supressed
        if (message !== 'Error details: ') {
            this.originalConsole.error.apply(this.originalConsole, arguments);
        }
    }

    log(message?: any, ...optionalParams: any[]): void {
        this.originalConsole.log.apply(this.originalConsole, arguments);
    }

    warn(message?: any, ...optionalParams: any[]): void {
        this.originalConsole.warn.apply(this.originalConsole, arguments);
    }
}

class MockRouter {
    navigate = jasmine.createSpy('navigate');
    routerState = {
        snapshot: {
            url: 'test_url',
        },
    };
}

class MockModalService {
    dialog = jasmine.createSpy('dialog')
        .and.returnValue(new Promise(neverResolve => {}));
}

class MockTranslateService {
    instant = (str: string) => `translated(${str})`;
}

describe('ErrorHandler', () => {

    let appState: TestAppState;
    let dispatchedActions$: Observable<any>;
    let console: MockConsole;
    let errorHandler: ErrorHandler;
    let modalService: MockModalService;
    let notification: MockNotificationService;
    let originalConsole: Console;
    let router: MockRouter;

    beforeAll(() => originalConsole = window.console);

    afterAll(() => (window as any).console = originalConsole);

    beforeEach(() => {
        console = new MockConsole(originalConsole);
        (window as any).console = console as any;
        modalService = new MockModalService();
        notification = new MockNotificationService();
        router = new MockRouter();

        TestBed.configureTestingModule({
            imports: [
                ...assembleTestAppStateImports(),
            ],
            providers: [
                ErrorHandler,
                TEST_APP_STATE,
                { provide: Router, useValue: router },
                { provide: ModalService, useValue: modalService },
                { provide: TranslateService, useClass: MockTranslateService },
                { provide: I18nNotificationService, useValue: notification },
            ],
        }).compileComponents();

        appState = TestBed.get(AppStateService);
        const snapshot = appState.snapshot();
        dispatchedActions$ = TestBed.inject(Actions);
        errorHandler = TestBed.inject(ErrorHandler);
    });

    describe('catch()', () => {

        it('does not display multiple notifications for the same error', () => {
            const error = new Error('Something went wrong');

            errorHandler.catch(error);
            expect(notification.show).toHaveBeenCalledTimes(1);
            expect(console.error).toHaveBeenCalledTimes(1);

            errorHandler.catch(error);
            expect(notification.show).toHaveBeenCalledTimes(1);
            expect(console.error).toHaveBeenCalledTimes(2);

            errorHandler.catch(error);
            expect(notification.show).toHaveBeenCalledTimes(1);

            errorHandler.catch(error);
            expect(notification.show).toHaveBeenCalledTimes(1);
        });

        it('always displays a notification for failed login attempts', () => {
            const loginError = new ApiError('Did not find a user with given credentials', 'failed', {
                request: {
                    body: {
                        login: 'username',
                        password: '12345678',
                    },
                    method: 'POST',
                    url: 'auth/login',
                    params: undefined,
                },
                response: {
                    responseInfo: {
                        responseCode: ResponseCode.NOT_FOUND,
                        responseMessage: 'Did not find a user with given credentials',
                    },
                },
            });
            errorHandler.catch(loginError);
            expect(notification.show).toHaveBeenCalledTimes(1);
            expect(console.error).toHaveBeenCalledTimes(1);

            errorHandler.catch(loginError);
            expect(notification.show).toHaveBeenCalledTimes(2);
            expect(console.error).toHaveBeenCalledTimes(2);
        });

        it('correctly handles when the user was logged out by the server', () => {
            const loggedOutError = new ApiError('Invalid SID', 'auth', {
                request: {
                    method: 'GET',
                    url: 'auth/me?sid=1234',
                    params: undefined,
                },
                response: {
                    responseInfo: {
                        responseCode: ResponseCode.AUTH_REQUIRED,
                        responseMessage: 'Invalid SID',
                    },
                },
            });

            appState.mockState({ auth: { isLoggedIn: true } });

            let logoutSuccessDispatched = false;
            const actions$ = appState.trackActions().pipe(
                ofActionDispatched(LogoutSuccess as ActionType),
            );
            const sub = actions$.subscribe(() => logoutSuccessDispatched = true);

            errorHandler.catch(loggedOutError);

            // User is logged out in app state
            expect(logoutSuccessDispatched).toBe(true);

            // User is redirected to login
            expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: router.routerState.snapshot.url } });

            // Dialog "you have been logged out" is displayed
            expect(modalService.dialog).toHaveBeenCalled();

            sub.unsubscribe();
        });

    });

    describe('notifyAndRethrow()', () => {

        it('passes the error to catch() for showing a notification and then rethrows it', () => {
            const catchSpy = spyOn(errorHandler, 'catch').and.stub();
            const error = new Error('Something went wrong');

            expect(() => errorHandler.notifyAndRethrow(error)).toThrow(error);
            expect(catchSpy).toHaveBeenCalledWith(error, { notification: true });
        });

    });

});
