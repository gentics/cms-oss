import { AppStateService } from '@admin-ui/state';
import { discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { TypePermissions, UniformTypePermissions } from '@gentics/cms-components';
import { AccessControlledType } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { assembleTestAppStateImports, TestAppState } from '../../../state/utils/test-app-state';
import { I18nService } from '../i18n';
import { MockI18nServiceWithSpies } from '../i18n/i18n.service.mock';
import { PermissionsService } from '../permissions/permissions.service';
import { MessageService } from './message.service';

class MockGcmsApi {}

class MockPermissionsService {
    private viewInbox$ = new BehaviorSubject(new UniformTypePermissions(AccessControlledType.INBOX, true));

    getTypePermissions(): Observable<TypePermissions> {
        return this.viewInbox$;
    }

    userHasMessagePermission(perm: boolean): void {
        this.viewInbox$.next(new UniformTypePermissions(AccessControlledType.INBOX, perm));
    }
}

describe('MessageService', () => {

    let appState: TestAppState;
    let poller: MessageService;
    let subscription: Subscription;
    let gcmsApi: GcmsApi;
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
                { provide: GcmsApi, useClass: MockGcmsApi },
                { provide: PermissionsService, useClass: MockPermissionsService },
                { provide: I18nService, useClass: MockI18nServiceWithSpies },
            ],
        });

        poller = TestBed.get(MessageService);
        appState = TestBed.get(AppStateService);
        gcmsApi = TestBed.get(GcmsApi);
        permissionsService = TestBed.get(PermissionsService);

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

    it('can be created', () => {
        expect(poller).toBeTruthy();
        expect(gcmsApi).toBeTruthy('no GcmsApi');
        expect(permissionsService).toBeTruthy('no PermissionService');
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
