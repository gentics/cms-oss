import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { I18nNotificationService, TranslatedNotificationOptions } from '@gentics/cms-components';
import { IndexByKey, MaintenanceModeResponse, ResponseCode } from '@gentics/cms-models';
import { ApiError, GcmsApi } from '@gentics/cms-rest-clients-angular';
import { ActionType, ofActionDispatched } from '@ngxs/store';
import { NEVER, Subject, Subscription } from 'rxjs';
import { take, takeUntil } from 'rxjs/operators';
import { ObservableStopper } from '../../../common/utils/observable-stopper/observable-stopper';
import { AppStateService } from '../../../state';
import {
    FetchMaintenanceStatusError,
    FetchMaintenanceStatusStart,
    FetchMaintenanceStatusSuccess,
} from '../../../state/maintenance-mode/maintenance-mode.actions';
import { assembleTestAppStateImports, TestAppState } from '../../../state/utils/test-app-state';
import { MockErrorHandler } from '../error-handler/error-handler.mock';
import { ErrorHandler } from '../error-handler/error-handler.service';
import { MaintenanceModeService } from './maintenance-mode.service';

class MockApiBase {
    get: any = jasmine.createSpy('ApiBase.get').and.returnValue(NEVER);
    post: any = jasmine.createSpy('ApiBase.post').and.returnValue(NEVER);
    upload: any = jasmine.createSpy('ApiBase.upload').and.returnValue(NEVER);
}

class MockI18nNotificationService {
    shown = false;
    show = jasmine.createSpy('show').and.callFake(() => {
        this.shown = true;
        return {
            dismiss: () => { this.shown = false; },
        };
    });
}

describe('MaintenanceModeService', () => {

    let service: MaintenanceModeService;
    let appState: TestAppState;
    let apiBase: MockApiBase;
    let notification: MockI18nNotificationService;
    let subscription: Subscription;
    let stopper: ObservableStopper;

    beforeEach(() => {
        apiBase = new MockApiBase();

        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                MaintenanceModeService,
                TestAppState,
                { provide: AppStateService, useExisting: TestAppState },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: GcmsApi, useFactory: () => new GcmsApi(apiBase as any) },
                MockI18nNotificationService,
                { provide: I18nNotificationService, useExisting: MockI18nNotificationService },
            ],
        }).compileComponents();

        appState = TestBed.inject(TestAppState);
        notification = TestBed.inject(I18nNotificationService) as any;
        service = TestBed.inject(MaintenanceModeService);
        stopper = new ObservableStopper();
    });

    afterEach(() => {
        stopper.stop();
        if (subscription) {
            subscription.unsubscribe();
        }
    });

    it('does not perform any requests initially', () => {
        expect(apiBase.get).not.toHaveBeenCalled();
        expect(apiBase.post).not.toHaveBeenCalled();
        expect(apiBase.upload).not.toHaveBeenCalled();
    });

    describe('refresh()', () => {

        let actionsDispatched: IndexByKey<boolean>;

        beforeEach(() => {
            actionsDispatched = {};

            appState.trackActions().pipe(
                ofActionDispatched(
                    FetchMaintenanceStatusStart as ActionType, FetchMaintenanceStatusSuccess as ActionType, FetchMaintenanceStatusError as ActionType,
                ),
                takeUntil(stopper.stopper$),
            ).subscribe((action) => actionsDispatched[action.constructor.name] = true);
        });

        it('requests the maintenance mode status from the API', () => {
            expect(apiBase.get).not.toHaveBeenCalled();
            service.refresh();
            expect(apiBase.get).toHaveBeenCalledWith('info/maintenance');
        });

        it('dispatches the status to the app state', () => {
            const responses = new Subject<MaintenanceModeResponse>();
            apiBase.get = () => responses.pipe(take(1));

            expect(actionsDispatched[FetchMaintenanceStatusStart.name]).toBeFalsy();

            service.refresh();
            expect(actionsDispatched[FetchMaintenanceStatusStart.name]).toBe(true);
            expect(actionsDispatched[FetchMaintenanceStatusSuccess.name]).toBeFalsy();
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

            expect(actionsDispatched[FetchMaintenanceStatusSuccess.name]).toBe(true);
            expect(appState.now.maintenanceMode.active).toEqual(true);
            expect(appState.now.maintenanceMode.message).toEqual('Stop working, the building is on fire!');

            service.refresh();
            expect(actionsDispatched[FetchMaintenanceStatusError.name]).toBeFalsy();
            responses.error(new ApiError('Not Found', 'http', { statusCode: 404 }));
            expect(actionsDispatched[FetchMaintenanceStatusError.name]).toBe(true);
        });

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

    });

    describe('refreshPeriodically()', () => {

        it('refreshes once initially', fakeAsync(() => {
            spyOn(service, 'refresh');
            subscription = service.refreshPeriodically(10000);
            tick(0);
            expect(service.refresh).toHaveBeenCalledTimes(1);

            subscription.unsubscribe();
        }));

        it('refreshes periodically after the passed timeout', fakeAsync(() => {
            spyOn(service, 'refresh');
            subscription = service.refreshPeriodically(10000);

            tick(0);
            expect(service.refresh).toHaveBeenCalledTimes(1);
            tick(10000);
            expect(service.refresh).toHaveBeenCalledTimes(2);
            tick(10000);
            expect(service.refresh).toHaveBeenCalledTimes(3);

            subscription.unsubscribe();
        }));

        it('returns a subscription that stops the periodic refresh interval', fakeAsync(() => {
            spyOn(service, 'refresh');
            subscription = service.refreshPeriodically(10000);
            tick(0);
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
            let clickNotificationButton: () => void;
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
            let clickNotificationButton: () => void;
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
