import { InterfaceOf, ObservableStopper } from '@admin-ui/common';
import { AppStateService, SetUsersnapSettings } from '@admin-ui/state';
import { assembleTestAppStateImports, TestAppState, TEST_APP_STATE, TrackedActions } from '@admin-ui/state/utils/test-app-state';
import { createDelayedError, createDelayedObservable, tickAndGetEmission } from '@admin-ui/testing';
import { fakeAsync, TestBed } from '@angular/core/testing';
import { RecursivePartial, UsersnapSettings } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { ofActionDispatched } from '@ngxs/store';
import { ErrorHandler } from '../../error-handler';
import { MockErrorHandler } from '../../error-handler/error-handler.mock';
import { I18nNotificationService } from '../../i18n-notification';
import { AdminOperations } from './admin.operations';

class MockApi implements RecursivePartial<InterfaceOf<GcmsApi>> {
    admin = {
        getUsersnapSettings: jasmine.createSpy('getUsernapSettings'),
    };
}

class MockNotificationService {
    show = jasmine.createSpy('notification.show');
}

describe('AdminOperations', () => {

    let api: MockApi;
    let appState: TestAppState;
    let errorHandler: MockErrorHandler;
    let adminOps: AdminOperations;
    let stopper: ObservableStopper;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                AdminOperations,
                TEST_APP_STATE,
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: GcmsApi, useClass: MockApi },
                { provide: I18nNotificationService, useValue: MockNotificationService },
            ],
        });

        api = TestBed.get(GcmsApi);
        appState = TestBed.get(AppStateService);
        adminOps = TestBed.get(AdminOperations);
        errorHandler = TestBed.get(ErrorHandler);
        stopper = new ObservableStopper();
    });

    afterEach(() => {
        stopper.stop();
    });

    describe('getUsersnapSettings()', () => {

        let dispatchedActions: TrackedActions<SetUsersnapSettings>;

        beforeEach(() => {
            const filterSpy = jasmine.createSpy('ofActionDispatched').and.callFake((...allowedTypes: any[]) => ofActionDispatched(...allowedTypes));
            dispatchedActions = appState.trackActionsAuto(filterSpy, SetUsersnapSettings);
        });

        it('gets the Usersnap settings and adds the result to the AppState', fakeAsync(() => {
            const settings: UsersnapSettings = { key: 'test' };
            api.admin.getUsersnapSettings.and.returnValue(createDelayedObservable({ settings }));

            const result = tickAndGetEmission(adminOps.getUsersnapSettings());
            expect(result).toBe(settings);

            expect(dispatchedActions.count).toBe(1);
            expect(dispatchedActions.get(0).settings).toEqual(settings);
        }));

        it('shows an error notification and rethrows the error', fakeAsync(() => {
            const error = new Error('Test');
            api.admin.getUsersnapSettings.and.returnValue(createDelayedError(error));
            errorHandler.assertNotifyAndRethrowIsCalled(adminOps.getUsersnapSettings(), error);
        }));

    });

});
