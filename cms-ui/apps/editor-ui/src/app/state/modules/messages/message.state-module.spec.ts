import { TestBed } from '@angular/core/testing';
import { Message, MessageFromServer, Normalized, Raw, User } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { MessageState } from '../../../common/models';
import { ApplicationStateService } from '../../providers';
import { TestApplicationState } from '../../test-application-state.mock';
import { STATE_MODULES } from '../state-modules';
import { MessagesFetchingErrorAction, MessagesFetchingSuccessAction, MessagesReadAction, StartMessagesFetchingAction } from './message.actions';

describe('MessageStateModule', () => {

    let state: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        state = TestBed.get(ApplicationStateService);
    });

    it('sets the correct initial state', () => {
        expect(state.now.messages).toEqual({
            fetching: false,
            all: [],
            read: [],
            unread: [],
            lastError: undefined,
        } as MessageState);
    });

    it('fetchAllMessagesStart works', () => {
        state.dispatch(new StartMessagesFetchingAction());
        expect(state.now.messages.fetching).toBe(true);
    });

    it('fetchAllMessagesSuccess works', () => {
        const firstMessage: Message<Normalized> = {
            id: 1,
            message: 'Test message',
            sender: 1,
            timestamp: 1234567890,
            type: 'INFO',
            unread: false,
        };

        state.mockState({
            entities: {
                message: {
                    1: firstMessage,
                },
                user: {
                    1: { id: 1, firstName: 'First', lastName: 'User' },
                },
            },
            messages: {
                fetching: true,
                all: [1],
                read: [1],
                unread: [],
            },
        });

        const messagesFromServer: MessageFromServer[] = [
            {
                id: 2,
                message: 'Hello',
                sender: { id: 2, firstName: 'Second', lastName: 'User' } as User<Raw>,
                timestamp: 1122334455,
                type: 'INFO',
            },
            {
                id: 3,
                message: 'Hello',
                sender: { id: 2, firstName: 'Second', lastName: 'User' } as User<Raw>,
                timestamp: 1122334455,
                type: 'INFO',
            },
        ];

        const unreadMessagesFromServer: MessageFromServer[] = [
            {
                id: 2,
                message: 'Hello',
                sender: { id: 2, firstName: 'Second', lastName: 'User' } as User<Raw>,
                timestamp: 1122334455,
                type: 'INFO',
            },
        ];

        state.dispatch(new MessagesFetchingSuccessAction(false, unreadMessagesFromServer, messagesFromServer));

        expect(state.now.messages.fetching).toBe(false);
        expect(state.now.messages.all).toEqual([2, 3]);
        expect(state.now.messages.read).toEqual([3]);
        expect(state.now.messages.unread).toEqual([2]);
        expect(state.now.entities.message).toEqual({
            2: jasmine.objectContaining({
                id: 2,
                message: 'Hello',
                sender: 2,
                timestamp: 1122334455,
                type: 'INFO',
                unread: true,
            }),
            3: jasmine.objectContaining({
                id: 3,
                message: 'Hello',
                sender: 2,
                timestamp: 1122334455,
                type: 'INFO',
                unread: false,
            }),
        });

        expect(state.now.entities.user[2]).toEqual(jasmine.objectContaining({
            id: 2,
            firstName: 'Second',
            lastName: 'User',
        }));
    });

    it('fetchAllMessagesError works', () => {
        const errorMessage = 'some error happened';
        state.dispatch(new MessagesFetchingErrorAction(errorMessage));
        expect(state.now.messages.fetching).toBe(false);
        expect(state.now.messages.lastError).toBe(errorMessage);
    });

    it('fetchUnreadMessagesStart works', () => {
        state.dispatch(new StartMessagesFetchingAction());
        expect(state.now.messages.fetching).toBe(true);
    });

    describe('fetchUnreadMessagesSuccess', () => {

        beforeEach(() => {
            state.mockState({
                entities: {
                    message: {
                        1: {
                            id: 1,
                            message: 'Test message',
                            sender: 1,
                            timestamp: 1234567890,
                            type: 'INFO',
                            unread: false,
                        },
                        2: {
                            id: 2,
                            message: 'Second message',
                            sender: 2,
                            timestamp: 1122334455,
                            type: 'INFO',
                            unread: true,
                        },
                    },
                    user: {
                        1: { id: 1, firstName: 'First', lastName: 'User' },
                        2: { id: 2, firstName: 'Second', lastName: 'User' },
                    },
                },
                messages: {
                    fetching: true,
                    all: [1, 2],
                    read: [1],
                    unread: [2],
                },
            });
        });

        it('works', () => {
            const unreadMessage: MessageFromServer = {
                id: 3,
                message: 'Third message',
                sender: { id: 3, firstName: 'Third', lastName: 'User' } as User<Raw>,
                timestamp: 1122334455,
                type: 'INFO',
            };

            state.dispatch(new MessagesFetchingSuccessAction(true, [unreadMessage]));

            expect(state.now.messages.fetching).toBe(false);
            expect(state.now.messages.all).toEqual([1, 2, 3]);
            expect(state.now.messages.read).toEqual([1]);
            expect(state.now.messages.unread).toEqual([2, 3]);
            expect(state.now.entities.message[3]).toEqual(jasmine.objectContaining({
                id: 3,
                message: 'Third message',
                sender: 3,
                timestamp: 1122334455,
                type: 'INFO',
                unread: true,
            }));
            expect(state.now.entities.user[3]).toEqual(jasmine.objectContaining({
                id: 3,
                firstName: 'Third',
                lastName: 'User',
            }));
        });

        it('does not change the state when no new information was received', () => {
            const alreadyUnreadMessage: MessageFromServer = {
                id: 2,
                message: 'Second message',
                sender: { id: 2, firstName: 'Second', lastName: 'User' } as User<Raw>,
                timestamp: 1122334455,
                type: 'INFO',
            };

            const stateBefore = state.now;
            state.dispatch(new MessagesFetchingSuccessAction(true, [alreadyUnreadMessage]));
            const stateAfter = state.now;

            expect(stateAfter.messages.fetching).toBeFalse();
            expect(stateBefore.messages.all).toEqual(stateAfter.messages.all);
            expect(stateBefore.messages.read).toEqual(stateAfter.messages.read);
            expect(stateBefore.messages.unread).toEqual(stateAfter.messages.unread);
        });

    });

    it('fetchUnreadMessagesError works', () => {
        const errorMessage = 'some error';
        state.dispatch(new MessagesFetchingErrorAction(errorMessage));
        expect(state.now.messages.lastError).toBe(errorMessage);
    });

    it('markMessagesAsRead works', () => {
        state.mockState({
            entities: {
                message: {
                    1: {
                        id: 1,
                        message: 'First new message',
                        sender: 1,
                        timestamp: 1234567890,
                        type: 'INFO',
                        unread: true,
                    },
                    2: {
                        id: 2,
                        message: 'Second new message',
                        sender: 2,
                        timestamp: 1122334455,
                        type: 'INFO',
                        unread: true,
                    },
                    3: {
                        id: 3,
                        message: 'Third new message',
                        sender: 1,
                        timestamp: 4455667788,
                        type: 'INFO',
                        unread: true,
                    },
                },
            },
            messages: {
                fetching: true,
                all: [1, 2, 3],
                read: [],
                unread: [1, 2, 3],
            },
        });
        const message1 = state.now.entities.message[1];

        state.dispatch(new MessagesReadAction([2, 3]));

        expect(state.now.messages.all).toEqual([1, 2, 3]);
        expect(state.now.messages.read).toEqual([2, 3]);
        expect(state.now.messages.unread).toEqual([1]);
        expect(state.now.entities.message[1].unread).toBe(true);
        expect(state.now.entities.message[2].unread).toBe(false, 'message 2 not marked as read');
        expect(state.now.entities.message[3].unread).toBe(false, 'message 3 not marked as read');
        expect(state.now.entities.message[1]).toBe(message1, 'message 1 reference changed');
    });

});
