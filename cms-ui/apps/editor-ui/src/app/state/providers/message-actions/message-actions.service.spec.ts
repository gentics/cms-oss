import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MessageFromServer, MessageListOptions } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { NgxsModule } from '@ngxs/store';
import { NEVER, of, throwError } from 'rxjs';
import { ApplicationStateService } from '..';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { STATE_MODULES } from '../../modules';
import { TestApplicationState } from '../../test-application-state.mock';
import { MessageActionsService } from './message-actions.service';

class MockErrorHandler implements Partial<ErrorHandler> {
    catch: (error: Error, options?: { notification: boolean }) => void = () => {};
}

describe('MessageActionsService', () => {

    let messageActions: MessageActionsService;
    let state: TestApplicationState;
    let client: GCMSTestRestClientService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                MessageActionsService,
            ],
        });
        state = TestBed.inject(ApplicationStateService) as any;
        client = TestBed.inject(GCMSRestClientService) as any;
        messageActions = TestBed.inject(MessageActionsService);
    });

    describe('fetchAllMessages', () => {
        it('calls getMessages action before loading from the API', fakeAsync(() => {
            let loadingWasStarted = false;
            client.message.list = jasmine.createSpy('client.message.list').and.callFake(() => {
                loadingWasStarted = true;
                return NEVER;
            });

            messageActions.fetchAllMessages();
            tick();

            expect(loadingWasStarted).toBe(true);
        }));

        it('fetches read and unread messages from the API', fakeAsync(() => {
            client.message.list = jasmine.createSpy('client.message.list')
                .and.callFake(() => NEVER);

            messageActions.fetchAllMessages();
            tick();

            expect(client.message.list).toHaveBeenCalledWith({ unread: true });
            expect(client.message.list).toHaveBeenCalledWith({ unread: false });
        }));

        it('calls fetchAllMessagesSuccess action when loaded successfully', fakeAsync(() => {
            const message: MessageFromServer = {
                id: 123,
                message: 'test message',
                sender: null,
                timestamp: new Date().getTime(),
                type: 'INFO',
                isInstantMessage: false,
            };
            client.message.list = () => of({ messages: [message] } as any);

            messageActions.fetchAllMessages();
            tick();

            expect(state.now.messages.all).toEqual([123]);
        }));

        it('merges read and unread messages together', fakeAsync(() => {
            const allMessages = [
                { id: 1, message: 'Message One', sender: { id: 1, login: 'first' } },
                { id: 2, message: 'Message Two', sender: { id: 2, login: 'second' } },
            ];
            const unreadMessages = [
                { id: 2, message: 'Message Two', sender: { id: 2, login: 'second' } },
            ];
            const expectedEntityResult = {
                1: jasmine.objectContaining({ id: 1, message: 'Message One', sender: 1, unread: false }),
                2: jasmine.objectContaining({ id: 2, message: 'Message Two', sender: 2, unread: true }),
            };

            client.message.list = (options?: MessageListOptions) =>
                of({ messages: options?.unread ? unreadMessages : allMessages } as any);

            let resolved = false;
            messageActions.fetchAllMessages()
                .then((result) => {
                    // expect(result).toEqual(true);
                    resolved = true;
                });
            tick();

            expect(resolved).toBe(true);
            expect(state.now.messages.all).toEqual([1, 2]);
            expect(state.now.messages.unread).toEqual([2]);
            expect(state.now.entities.message).toEqual(expectedEntityResult);
        }));

        it('works when messages do not have a sender', fakeAsync(() => {
            const allMessages = [
                { id: 1, message: 'Message One' },
                { id: 2, message: 'Message Two' },
            ];
            const unreadMessages = [
                { id: 2, message: 'Message Two' },
            ];
            const expectedEntityResult = {
                1: jasmine.objectContaining({ id: 1, message: 'Message One', sender: 1, unread: false }),
                2: jasmine.objectContaining({ id: 2, message: 'Message Two', sender: 1, unread: true }),
            };

            client.message.list = (options?: MessageListOptions) =>
                of({ messages: options?.unread ? unreadMessages : allMessages } as any);

            let resolved = false;
            messageActions.fetchAllMessages()
                .then((result) => {
                    // expect(result).toEqual(true);
                    resolved = true;
                });
            tick();

            expect(resolved).toBe(true);
            expect(state.now.messages.all).toEqual([1, 2]);
            expect(state.now.messages.unread).toEqual([2]);
            expect(state.now.entities.message).toEqual(expectedEntityResult);
        }));

        it('does not reject the returned Promise when loading fails', fakeAsync(() => {
            client.message.list = () => throwError('Failed to load');
            let rejected = false;

            messageActions.fetchAllMessages()
                .catch(() => { rejected = true; });
            tick();
            expect(rejected).toBe(false);
        }));

    });

    describe('fetchUnreadMessages', () => {
        it('calls fetchUnreadMessagesStart action before loading from the API', () => {
            let wasDispatched = false;
            client.message.list = jasmine.createSpy('client.message.list').and.callFake(() => {
                wasDispatched = true;
                return NEVER;
            });

            messageActions.fetchUnreadMessages();

            expect(wasDispatched).toBe(true);
        });

        it('fetches unread messages from the API', () => {
            client.message.list = jasmine.createSpy('client.message.list')
                .and.callFake(() => NEVER);

            messageActions.fetchUnreadMessages();

            expect(client.message.list).toHaveBeenCalledWith({ unread: true });
            expect(client.message.list).not.toHaveBeenCalledWith({ unread: false });
        });

        it('calls fetchUnreadMessagesSuccess action when loaded successfully', fakeAsync(() => {
            const message: MessageFromServer = {
                id: 123,
                message: 'test content',
                sender: null,
                timestamp: new Date().getTime(),
                type: 'INFO',
                isInstantMessage: false,
            };
            client.message.list = () => of({ messages: [message] } as any);

            messageActions.fetchUnreadMessages();
            tick();

            expect(state.now.messages.all).toEqual([123]);
            expect(state.now.messages.unread).toEqual([123]);
        }));

        it('normalizes the returned unread messages', fakeAsync(() => {
            const unreadMessages = [
                { id: 2, message: 'Message Two', sender: { id: 2, login: 'second' } },
                { id: 3, message: 'Message Three', sender: { id: 1, login: 'first' } },
            ];
            const expectedEntityResult = {
                2: jasmine.objectContaining({ id: 2, message: 'Message Two', sender: 2, unread: true }),
                3: jasmine.objectContaining({ id: 3, message: 'Message Three', sender: 1, unread: true }),
            };

            client.message.list = () => of({ messages: unreadMessages } as any);

            let resolved = false;
            messageActions.fetchUnreadMessages()
                .then((result) => {
                    // expect(result).toEqual(true);
                    resolved = true;
                });
            tick();

            expect(resolved).toBe(true);
            expect(state.now.messages.unread).toEqual(unreadMessages.map((msg) => msg.id));
            expect(state.now.entities.message).toEqual(expectedEntityResult);
        }));

        it('works when messages have no sender', fakeAsync(() => {
            const unreadMessages = [
                { id: 2, message: 'Message Two' },
                { id: 3, message: 'Message Three' },
            ];
            const expectedResult = {
                2: jasmine.objectContaining({ id: 2, message: 'Message Two', sender: 1, unread: true }),
                3: jasmine.objectContaining({ id: 3, message: 'Message Three', sender: 1, unread: true }),
            };

            client.message.list = () => of({ messages: unreadMessages } as any);

            let resolved = false;
            messageActions.fetchUnreadMessages()
                .then((result) => {
                    // expect(result).toEqual(true);
                    resolved = true;
                });
            tick();

            expect(resolved).toBe(true);
            expect(state.now.messages.unread).toEqual(unreadMessages.map((msg) => msg.id));
            expect(state.now.entities.message).toEqual(expectedResult);
        }));

        it('does not reject returned Promise when loading fails', fakeAsync(() => {
            client.message.list = () => throwError('Failed to load');
            let rejected = false;

            messageActions.fetchUnreadMessages()
                .catch(() => { rejected = true; });
            tick();
            expect(rejected).toBe(false);
        }));

    });

    describe('markMessagesAsRead', () => {
        beforeEach(() => {
            state.mockState({
                entities: {
                    message: {
                        1: { id: 1, message: 'test msg 1', unread: true },
                        2: { id: 2, message: 'test msg 2', unread: true },
                        3: { id: 3, message: 'test msg 3', unread: true },
                    },
                },
            });
        });

        it('marks the passed messages as read via the API', fakeAsync(() => {
            client.message.markAsRead = jasmine.createSpy('client.message.markAsRead')
                .and.returnValue(NEVER);

            messageActions.markMessagesAsRead([1, 2, 3]);
            tick();

            expect(client.message.markAsRead).toHaveBeenCalledWith({ messages: [1, 2, 3] });
        }));

        it('marks the messages as read in the app state when the API request succeeds', fakeAsync(() => {
            client.message.markAsRead = jasmine.createSpy('client.message.markAsRead')
                .and.callFake(() => of({}));

            messageActions.markMessagesAsRead([1, 2, 3]);
            tick();

            expect(state.now.messages.read).toEqual([1, 2, 3]);
        }));

        it('does not mark the messages as read in the app state  when the API request fails', fakeAsync(() => {
            client.message.markAsRead = jasmine.createSpy('client.message.markAsRead')
                .and.callFake(() => throwError('Request failed'));

            messageActions.markMessagesAsRead([1, 2, 3]);
            tick();

            expect(state.now.messages.read).toEqual([]);
        }));

        it('works with an array of message IDs as input', fakeAsync(() => {
            client.message.markAsRead = jasmine.createSpy('client.message.markAsRead')
                .and.callFake(() => of({}));

            messageActions.markMessagesAsRead([1, 2, 3]);
            tick();

            expect(state.now.messages.read).toEqual([1, 2, 3]);
        }));

    });

});

class MockAPI {
    messages = {
        getMessages(unreadOnly: boolean): any {
            throw new Error('API.messages.getMessages called but not mocked');
        },
        markAsRead(ids: any[]): void {
            throw new Error('API.messages.markAsRead called but not mocked');
        },
    };
}
