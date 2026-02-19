import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ApplicationStateService, STATE_MODULES } from '@editor-ui/app/state';
import { MaintenanceModeResponse, ResponseCode } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { Subject, Subscription, throwError } from 'rxjs';
import { take } from 'rxjs/operators';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { ApiBase, ApiError, GcmsApi } from '../api';
import { MockApiBase } from '../api/api-base.mock';
import { Api } from '../api/api.service';
import { ErrorHandler } from '../error-handler/error-handler.service';
import { I18nNotification, TranslatedNotificationOptions } from '../i18n-notification/i18n-notification.service';
import { MaintenanceModeService } from './maintenance-mode.service';
import { AuthenticationModule } from '@gentics/cms-components/auth';

describe('MaintenanceModeService', () => {

    let service: MaintenanceModeService;
    let apiBase: MockApiBase;
    let appState: TestApplicationState;
    let notification: MockNotificationService;
    let subscription: Subscription;

    beforeEach(() => {
        apiBase = new MockApiBase();
        const api = new Api(new GcmsApi(apiBase as ApiBase));

        TestBed.configureTestingModule({
            imports: [
                NgxsModule.forRoot(STATE_MODULES),
                AuthenticationModule.forRoot(),
            ],
            providers: [
                { provide: Api, useValue: api },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: I18nNotification, useClass: MockNotificationService },
                { provide: ErrorHandler, useClass: SpyErrorHandler },
                MaintenanceModeService,
            ],
        });

        appState = TestBed.inject(ApplicationStateService) as any;
        notification = TestBed.inject(I18nNotification) as any;
        service = TestBed.inject(MaintenanceModeService);
    });

    afterEach(() => {
        if (subscription) { subscription.unsubscribe(); }
    });

    it('does not perform any requests initially', () => {
        expect(apiBase.get).not.toHaveBeenCalled();
        expect(apiBase.post).not.toHaveBeenCalled();
        expect(apiBase.upload).not.toHaveBeenCalled();
    });

    describe('refresh()', () => {

        it('requests the maintenance mode status from the API', fakeAsync(() => {
            expect(apiBase.get).not.toHaveBeenCalled();
            service.refresh();
            tick();
            expect(apiBase.get).toHaveBeenCalledWith('info/maintenance');
        }));

        it('does not re-request the status if the server does not provide the endpoint', async () => {
            expect(apiBase.get).not.toHaveBeenCalled();
            apiBase.get = jasmine.createSpy('get').and.returnValue(
                throwError(new ApiError('Not Found', 'http', { statusCode: 404 })),
            );

            await service.refresh();
            expect(apiBase.get).toHaveBeenCalled();

            await service.refresh();
            expect(apiBase.get).toHaveBeenCalledTimes(1);
        });

        it('dispatches the status to the app state', fakeAsync(() => {
            const responses = new Subject<MaintenanceModeResponse>();
            apiBase.get = () => responses.pipe(take(1));

            service.refresh();
            tick();
            expect(appState.now.maintenanceMode.active).toEqual(false);

            responses.next({
                banner: false,
                maintenance: true,
                message: 'Stop working, the building is on fire!',
                messages: [],
                responseInfo: {
                    responseCode: ResponseCode.OK,
                },
            });

            tick();
            expect(appState.now.maintenanceMode.active).toEqual(true);
            expect(appState.now.maintenanceMode.message).toEqual('Stop working, the building is on fire!');

            service.refresh();
            responses.error(new ApiError('Not Found', 'http', { statusCode: 404 }));
            tick();
            expect(appState.now.maintenanceMode.active).toEqual(false);
        }));

        it('marks the endpoint as unsupported when it returns an error', fakeAsync(() => {
            const responses = new Subject<MaintenanceModeResponse>();
            apiBase.get = jasmine.createSpy('get').and.callFake(() => responses.pipe(take(1)));

            service.refresh();
            responses.error(new ApiError('Not Found', 'http', { statusCode: 404 }));
            tick();

            expect(appState.now.maintenanceMode.reportedByServer).toEqual(false);

            service.refresh();
            tick();
            expect(apiBase.get).toHaveBeenCalledTimes(1);
        }));

    });

    describe('refreshOnLogout()', () => {

        it('refreshes when the user is logged out', () => {
            spyOn(service, 'refresh');
            service.refreshOnLogout();
            expect(service.refresh).not.toHaveBeenCalled();

            appState.mockState({ auth: { isLoggedIn: true } });
            expect(service.refresh).not.toHaveBeenCalled();

            appState.mockState({ auth: { isLoggedIn: false } });
            expect(service.refresh).toHaveBeenCalled();
        });

        it('stops refreshing when the server does not support the required endpoint', () => {
            spyOn(service, 'refresh');
            service.refreshOnLogout();
            expect(service.refresh).not.toHaveBeenCalled();

            appState.mockState({
                auth: { isLoggedIn: true },
                maintenanceMode: { reportedByServer: false },
            });
            expect(service.refresh).not.toHaveBeenCalled();

            appState.mockState({ auth: { isLoggedIn: false } });
            expect(service.refresh).not.toHaveBeenCalled();
        });

    });

    describe('refreshPeriodically()', () => {

        it('refreshes once initially', () => {
            spyOn(service, 'refresh');
            subscription = service.refreshPeriodically(10000);
            expect(service.refresh).toHaveBeenCalled();
        });

        it('refreshes periodically after the passed timeout', fakeAsync(() => {
            spyOn(service, 'refresh');
            subscription = service.refreshPeriodically(10000);

            expect(service.refresh).toHaveBeenCalledTimes(1);
            tick(10000);
            expect(service.refresh).toHaveBeenCalledTimes(2);
            tick(10000);
            expect(service.refresh).toHaveBeenCalledTimes(3);

            subscription.unsubscribe();
        }));

        it('stops refreshing when the server does not provide maintenance mode information', fakeAsync(() => {
            spyOn(service, 'refresh');
            subscription = service.refreshPeriodically(10000);

            expect(service.refresh).toHaveBeenCalledTimes(1);
            appState.mockState({
                maintenanceMode: {
                    active: false,
                    reportedByServer: false,
                },
            });

            tick(10000);
            expect(service.refresh).not.toHaveBeenCalledTimes(2);

            subscription.unsubscribe();
        }));

        it('returns a subscription that stops the periodic refresh interval', fakeAsync(() => {
            spyOn(service, 'refresh');
            subscription = service.refreshPeriodically(10000);
            expect(service.refresh).toHaveBeenCalled();

            subscription.unsubscribe();
            tick(50000);
            expect(service.refresh).toHaveBeenCalledTimes(1);
        }));

    });

    describe('displayNotificationWhenActivated', () => {

        it('does not show a notification by default', () => {
            subscription = service.displayNotificationWhenActive();
            expect(notification.show).not.toHaveBeenCalled();
            subscription.unsubscribe();
        });

        it('shows a notification when the maintenance mode is activated', () => {
            subscription = service.displayNotificationWhenActive();

            appState.mockState({
                maintenanceMode: {
                    active: true,
                    fetching: false,
                    message: 'Stop working, the building is on fire!',
                    reportedByServer: true,
                    showBanner: true,
                },
            });
            expect(notification.show).toHaveBeenCalled();

            subscription.unsubscribe();
        });

        it('closes the notification when the maintenance mode is deactivated', () => {
            subscription = service.displayNotificationWhenActive();

            appState.mockState({
                maintenanceMode: {
                    active: true,
                    fetching: false,
                    message: 'Stop working, the building is on fire!',
                    reportedByServer: true,
                    showBanner: true,
                },
            });
            expect(notification.show).toHaveBeenCalled();
            expect(notification.shown).toBe(true);

            appState.mockState({
                maintenanceMode: {
                    active: false,
                    fetching: false,
                    message: 'Stop working, the building is on fire!',
                    reportedByServer: true,
                    showBanner: false,
                },
            });

            expect(notification.shown).toBe(false);

            subscription.unsubscribe();
        });

        it('allows users to dismiss the message when they can login', () => {
            subscription = service.displayNotificationWhenActive();

            appState.mockState({
                auth: {
                    isLoggedIn: true,
                },
                maintenanceMode: {
                    active: true,
                    fetching: false,
                    message: 'Stop working, the building is on fire!',
                    reportedByServer: true,
                    showBanner: true,
                },
            });

            expect(notification.show).toHaveBeenCalledWith(jasmine.objectContaining({
                action: jasmine.objectContaining({
                    label: 'OK',
                }),
            }));

            subscription.unsubscribe();
        });

        it('does not show the same message when the user has dismissed it', () => {
            subscription = service.displayNotificationWhenActive();

            let currentNotification: { dismiss(): void };
            let clickNotificationButton: Function;
            notification.show = jasmine.createSpy('show')
                .and.callFake((options: TranslatedNotificationOptions) => {
                    clickNotificationButton = options.action.onClick;
                    return currentNotification = jasmine.createSpyObj('toast', ['dismiss']);
                });

            appState.mockState({
                auth: {
                    isLoggedIn: true,
                },
                maintenanceMode: {
                    active: true,
                    fetching: false,
                    message: 'Stop working, the building is on fire!',
                    reportedByServer: true,
                    showBanner: true,
                },
            });

            expect(currentNotification).toBeDefined();
            clickNotificationButton();

            appState.mockState({
                maintenanceMode: {
                    ...appState.now.maintenanceMode,
                    active: false,
                },
            });

            expect(notification.show).toHaveBeenCalledTimes(1);

            subscription.unsubscribe();
        });

        it('show the maintenance message when it changed after the user dismissed a different one', () => {
            subscription = service.displayNotificationWhenActive();

            let currentNotification: { dismiss(): void };
            let clickNotificationButton: Function;
            notification.show = jasmine.createSpy('show')
                .and.callFake((options: TranslatedNotificationOptions) => {
                    clickNotificationButton = options.action.onClick;
                    return currentNotification = jasmine.createSpyObj('toast', ['dismiss']);
                });

            appState.mockState({
                auth: {
                    isLoggedIn: true,
                },
                maintenanceMode: {
                    active: true,
                    fetching: false,
                    message: 'Stop working, the building is on fire!',
                    reportedByServer: true,
                    showBanner: true,
                },
            });

            expect(currentNotification).toBeDefined();
            clickNotificationButton();

            appState.mockState({
                maintenanceMode: {
                    ...appState.now.maintenanceMode,
                    message: 'Please do not slip on banana peels!',
                },
            });

            expect(notification.show).toHaveBeenCalledTimes(2);

            subscription.unsubscribe();
        });

    });

    describe('validateSessionWhenActivated', () => {

        it('validates the session when the maintenance mode was activated', () => {
            subscription = service.validateSessionWhenActivated();
            appState.mockState({
                auth: {
                    isLoggedIn: true,
                    sid: 1234,
                },
                maintenanceMode: {
                    active: true,
                    fetching: false,
                    message: 'Please save your changes, a wormhole opened in our basement.',
                    reportedByServer: true,
                    showBanner: true,
                },
            });

            expect(apiBase.get).toHaveBeenCalledWith('user/me?sid=1234');

            subscription.unsubscribe();
        });

    });

});

class MockNotificationService {
    shown = false;
    show = jasmine.createSpy('show').and.callFake(() => {
        this.shown = true;
        return {
            dismiss: () => { this.shown = false; },
        };
    });
}

class SpyErrorHandler {
    catch = jasmine.createSpy('error');
}
