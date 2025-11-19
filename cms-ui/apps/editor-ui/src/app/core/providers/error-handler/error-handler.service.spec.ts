import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { I18nNotificationService, I18nService, TranslateParameters } from '@gentics/cms-components';
import { ResponseCode } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { NgxsModule } from '@ngxs/store';
import { ApplicationStateService, STATE_MODULES } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { ApiError } from '../api';
import { ErrorHandler } from './error-handler.service';

describe('ErrorHandler', () => {

    let appState: TestApplicationState;
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

        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: Router, useClass: MockRouter },
                { provide: ModalService, useClass: MockModalService },
                { provide: I18nNotificationService, useClass: MockNotificationService },
                { provide: I18nService, useClass: MockI18nService },
                ErrorHandler,
            ],
        });
        appState = TestBed.inject(ApplicationStateService) as any;
        errorHandler = TestBed.inject(ErrorHandler);
        modalService = TestBed.inject(ModalService) as any;
        notification = TestBed.inject(I18nNotificationService) as any;
        router = TestBed.inject(Router) as any;
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

            errorHandler.catch(loggedOutError);

            // User is logged out in app state
            // expect(appState.actions.auth.logoutSuccess).toHaveBeenCalled();

            // User is redirected to login
            expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: router.routerState.snapshot.url } });

            // Dialog "you have been logged out" is displayed
            expect(modalService.dialog).toHaveBeenCalled();
        });

    });

});

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
        .and.returnValue(new Promise((neverResolve) => {}));
}

class MockI18nService implements Partial<I18nService> {
    public instant(key: string | string[], params?: TranslateParameters): string {
        return `translated(${key})`;
    }
}
