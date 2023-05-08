import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MessageFromServer } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { Observable, throwError } from 'rxjs';
import { ApplicationStateService } from '..';
import { Api } from '../../../core/providers/api/api.service';
import { STATE_MODULES } from '../../modules';
import { TestApplicationState } from '../../test-application-state.mock';
import { MessageActionsService } from './message-actions.service';

describe('MessageActionsService', () => {

    let messageActions: MessageActionsService;
    let state: TestApplicationState;
    let api: MockAPI;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: Api, useClass: MockAPI },
                MessageActionsService,
            ],
        });
        state = TestBed.get(ApplicationStateService);
        api = TestBed.get(Api);
        messageActions = TestBed.get(MessageActionsService);
    });

    describe('fetchAllMessages', () => {
        it('calls getMessages action before loading from the API', () => {
            let loadingWasStarted = false;
            api.messages.getMessages = jasmine.createSpy('api.messages.getMessages').and.callFake(() => {
                loadingWasStarted = true;
                return Observable.never();
            });

            messageActions.fetchAllMessages();

            expect(loadingWasStarted).toBe(true);
        });

        it('fetches read and unread messages from the API', () => {
            api.messages.getMessages = jasmine.createSpy('api.messages.getMessages')
                .and.callFake(() => Observable.never());

            messageActions.fetchAllMessages();

            expect(api.messages.getMessages).toHaveBeenCalledWith(true);
            expect(api.messages.getMessages).toHaveBeenCalledWith(false);
        });

        it('calls fetchAllMessagesSuccess action when loaded successfully', fakeAsync(() => {
            const message: MessageFromServer = {
                id: 123,
                message: 'test message',
                sender: null,
                timestamp: new Date().getTime(),
                type: 'INFO',
            };
            api.messages.getMessages = () => Observable.of({ messages: [message] });

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

            api.messages.getMessages = (unreadOnly: boolean) =>
                Observable.of({ messages: unreadOnly ? unreadMessages : allMessages });

            let resolved = false;
            messageActions.fetchAllMessages()
                .then(result => {
                    expect(result).toEqual(true);
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

            api.messages.getMessages = (unreadOnly: boolean) =>
                Observable.of({ messages: unreadOnly ? unreadMessages : allMessages });

            let resolved = false;
            messageActions.fetchAllMessages()
                .then(result => {
                    expect(result).toEqual(true);
                    resolved = true;
                });
            tick();

            expect(resolved).toBe(true);
            expect(state.now.messages.all).toEqual([1, 2]);
            expect(state.now.messages.unread).toEqual([2]);
            expect(state.now.entities.message).toEqual(expectedEntityResult);
        }));

        it('calls fetchAllMessagesError action when loading fails', fakeAsync(() => {
            const errorMessage = 'Failed to load';
            api.messages.getMessages = () => throwError(new Error(errorMessage));

            messageActions.fetchAllMessages().catch(() => { });
            tick();

            expect(state.now.messages.fetching).toEqual(false);
            expect(state.now.messages.lastError).toEqual(errorMessage);
        }));

        it('does not reject the returned Promise when loading fails', fakeAsync(() => {
            api.messages.getMessages = () => Observable.throw('Failed to load');
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
            api.messages.getMessages = jasmine.createSpy('api.messages.getMessages').and.callFake(() => {
                wasDispatched = true;
                return Observable.never();
            });

            messageActions.fetchUnreadMessages();

            expect(wasDispatched).toBe(true);
        });

        it('fetches unread messages from the API', () => {
            api.messages.getMessages = jasmine.createSpy('api.messages.getMessages')
                .and.callFake(() => Observable.never());

            messageActions.fetchUnreadMessages();

            expect(api.messages.getMessages).toHaveBeenCalledWith(true);
            expect(api.messages.getMessages).not.toHaveBeenCalledWith(false);
        });

        it('calls fetchUnreadMessagesSuccess action when loaded successfully', fakeAsync(() => {
            const message: MessageFromServer = {
                id: 123,
                message: 'test content',
                sender: null,
                timestamp: new Date().getTime(),
                type: 'INFO',
            };
            api.messages.getMessages = () => Observable.of({ messages: [message] });

            messageActions.fetchUnreadMessages();
            tick();

            expect(state.now.messages.fetching).toEqual(false);
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

            api.messages.getMessages = () => Observable.of({ messages: unreadMessages });

            let resolved = false;
            messageActions.fetchUnreadMessages()
                .then(result => {
                    expect(result).toEqual(true);
                    resolved = true;
                });
            tick();

            expect(resolved).toBe(true);
            expect(state.now.messages.unread).toEqual(unreadMessages.map(msg => msg.id));
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

            api.messages.getMessages = () => Observable.of({ messages: unreadMessages });

            let resolved = false;
            messageActions.fetchUnreadMessages()
                .then(result => {
                    expect(result).toEqual(true);
                    resolved = true;
                });
            tick();

            expect(resolved).toBe(true);
            expect(state.now.messages.unread).toEqual(unreadMessages.map(msg => msg.id));
            expect(state.now.entities.message).toEqual(expectedResult);
        }));

        it('calls fetchUnreadMessagesError action when loading fails', fakeAsync(() => {
            const errorMessage = 'Failed to load';
            api.messages.getMessages = () => throwError(new Error(errorMessage));

            messageActions.fetchUnreadMessages().catch(() => {});
            tick();

            expect(state.now.messages.fetching).toEqual(false);
            expect(state.now.messages.lastError).toEqual(errorMessage);
        }));

        it('does not reject returned Promise when loading fails', fakeAsync(() => {
            api.messages.getMessages = () => Observable.throw('Failed to load');
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

        it('marks the passed messages as read via the API', () => {
            api.messages.markAsRead = jasmine.createSpy('api.messages.markAsRead')
                .and.returnValue(Observable.never());

            messageActions.markMessagesAsRead([1, 2, 3]);

            expect(api.messages.markAsRead).toHaveBeenCalledWith([1, 2, 3]);
        });

        it('marks the messages as read in the app state when the API request succeeds', () => {
            api.messages.markAsRead = jasmine.createSpy('api.messages.markAsRead')
                .and.callFake(() => Observable.of({}));

            messageActions.markMessagesAsRead([1, 2, 3]);

            expect(state.now.messages.read).toEqual([1, 2, 3]);
        });

        it('does not mark the messages as read in the app state  when the API request fails', () => {
            api.messages.markAsRead = jasmine.createSpy('api.messages.markAsRead')
                .and.callFake(() => Observable.throw('Request failed'));

            messageActions.markMessagesAsRead([1, 2, 3]);

            expect(state.now.messages.read).toEqual([]);
        });

        it('works with an array of message IDs as input', () => {
            api.messages.markAsRead = jasmine.createSpy('api.messages.markAsRead')
                .and.callFake(() => Observable.of({}));

            messageActions.markMessagesAsRead([1, 2, 3]);

            expect(state.now.messages.read).toEqual([1, 2, 3]);
        });

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
