import { provideHttpClientTesting } from '@angular/common/http/testing';
import { discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { I18nService, TypePermissions, UniformTypePermissions } from '@gentics/cms-components';
import { MockI18nService } from '@gentics/cms-components/testing';
import { AccessControlledType } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { GenticsUICoreModule, NotificationService } from '@gentics/ui-core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { AppStateService } from '../../../state';
import { assembleTestAppStateImports, TestAppState } from '../../../state/utils/test-app-state';
import { ErrorHandler } from '../error-handler';
import { PermissionsService } from '../permissions/permissions.service';
import { MessageService } from './message.service';

class MockPermissionsService {
    private viewInbox$ = new BehaviorSubject(new UniformTypePermissions(AccessControlledType.INBOX, true));

    getTypePermissions(): Observable<TypePermissions> {
        return this.viewInbox$;
    }

    userHasMessagePermission(perm: boolean): void {
        this.viewInbox$.next(new UniformTypePermissions(AccessControlledType.INBOX, perm));
    }
}

class MockErrorHandler implements Partial<ErrorHandler> {
    catch: (error: Error, options?: { notification: boolean }) => string = () => '';
}

describe('MessageService', () => {

    let appState: TestAppState;
    let poller: MessageService;
    let subscription: Subscription;
    let permissionsService: MockPermissionsService;

    let fetchAllMessages;
    let fetchUnreadMessages;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
                GenticsUICoreModule.forRoot(),
            ],
            providers: [
                MessageService,
                { provide: AppStateService, useClass: TestAppState },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                { provide: PermissionsService, useClass: MockPermissionsService },
                { provide: I18nService, useClass: MockI18nService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: NotificationService },
                provideHttpClientTesting(),
            ],
        });

        poller = TestBed.inject(MessageService);
        appState = TestBed.inject(AppStateService) as any;
        permissionsService = TestBed.inject(PermissionsService) as any;

        fetchAllMessages = spyOn(poller, 'fetchAllMessages');
        fetchUnreadMessages = spyOn(poller, 'fetchUnreadMessages');

        appState.mockState({
            auth: {
                isLoggedIn: true,
            },
        });
    });

    afterEach(() => {
        if (subscription) {
            subscription.unsubscribe();
        }
    });

    describe('poll()', () => {

        it('does not fetch messages if the user is not logged in', fakeAsync(() => {
            appState.mockState({ auth: { isLoggedIn: false } });
            subscription = poller.poll(1, 1);
            tick(20000);

            expect(fetchAllMessages).not.toHaveBeenCalled();
            expect(fetchUnreadMessages).not.toHaveBeenCalled();
        }));

        it('does not fetch messages if the user has no permissions', fakeAsync(() => {
            permissionsService.userHasMessagePermission(false);
            subscription = poller.poll(1, 1);
            tick(20000);

            expect(fetchAllMessages).not.toHaveBeenCalled();
            expect(fetchUnreadMessages).not.toHaveBeenCalled();
        }));

        it('fetches messages once after an initial timeout', fakeAsync(() => {
            subscription = poller.poll(5, 30);
            tick(5000);

            expect(fetchAllMessages).toHaveBeenCalledTimes(1);
            expect(fetchUnreadMessages).toHaveBeenCalledTimes(0);

            discardPeriodicTasks();
        }));

        it('fetches messages periodically after an interval', fakeAsync(() => {
            subscription = poller.poll(5, 30);
            tick(5000);
            expect(fetchUnreadMessages).toHaveBeenCalledTimes(0);

            tick(30000);
            expect(fetchUnreadMessages).toHaveBeenCalledTimes(1);

            tick(30000);
            expect(fetchUnreadMessages).toHaveBeenCalledTimes(2);

            discardPeriodicTasks();
        }));

    });

});
