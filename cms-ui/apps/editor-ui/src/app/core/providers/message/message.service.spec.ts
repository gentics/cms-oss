import { discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { STATE_MODULES } from '@editor-ui/app/state';
import { NgxsModule } from '@ngxs/store';
import { BehaviorSubject, Subscription } from 'rxjs';
import { ApplicationStateService } from '../../../state';
import { MessageActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { PermissionService } from '../permissions/permission.service';
import { MessageService } from './message.service';

describe('MessageService', () => {

    let appState: TestApplicationState;
    let service: MessageService;
    let subscription: Subscription;
    let messageActions: MockMessageActions;
    let permissionsService: MockPermissionsService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                MessageService,
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: MessageActionsService, useClass: MockMessageActions },
                { provide: PermissionService, useClass: MockPermissionsService },
            ],
        });

        service = TestBed.get(MessageService);
        appState = TestBed.get(ApplicationStateService);
        messageActions = TestBed.get(MessageActionsService);
        permissionsService = TestBed.get(PermissionService);

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
        expect(service).toBeTruthy();
        expect(messageActions).toBeTruthy('no MessageActions');
        expect(permissionsService).toBeTruthy('no PermissionService');
    });

    describe('poll()', () => {

        it('does not fetch messages if the user is not logged in', fakeAsync(() => {
            appState.mockState({ auth: { isLoggedIn: false } });
            subscription = service.poll(1, 1);
            tick(20000);

            expect(messageActions.fetchAllMessages).not.toHaveBeenCalled();
            expect(messageActions.fetchUnreadMessages).not.toHaveBeenCalled();

            discardPeriodicTasks();
        }));

        it('does not fetch messages if the user has no permissions', fakeAsync(() => {
            permissionsService.viewInbox$.next(false);
            subscription = service.poll(1, 1);
            tick(20000);

            expect(messageActions.fetchAllMessages).not.toHaveBeenCalled();
            expect(messageActions.fetchUnreadMessages).not.toHaveBeenCalled();

            discardPeriodicTasks();
        }));

        it('fetches messages once after an initial timeout', fakeAsync(() => {
            subscription = service.poll(5, 30);
            tick(10000);

            expect(messageActions.fetchAllMessages).toHaveBeenCalledTimes(1);
            expect(messageActions.fetchUnreadMessages).toHaveBeenCalledTimes(0);

            discardPeriodicTasks();
        }));

        it('fetches messages periodically after an interval', fakeAsync(() => {
            subscription = service.poll(5, 30);
            tick(5000);
            expect(messageActions.fetchUnreadMessages).toHaveBeenCalledTimes(0);

            tick(30000);
            expect(messageActions.fetchUnreadMessages).toHaveBeenCalledTimes(1);

            tick(30000);
            expect(messageActions.fetchUnreadMessages).toHaveBeenCalledTimes(2);

            discardPeriodicTasks();
        }));

    });

});

class MockMessageActions {
    fetchAllMessages = jasmine.createSpy('fetchAllMessages');
    fetchUnreadMessages = jasmine.createSpy('fetchUnreadMessages');
}

class MockPermissionsService {
    viewInbox$ = new BehaviorSubject(true);
}
