import { fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { IS_NORMALIZED, MessageFromServer, Raw, User } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { EntityStateModule } from '../entity/entity.state';
import { AppStateService } from '../providers/app-state/app-state.service';
import { TEST_APP_STATE, TestAppState } from '../utils/test-app-state';
import {
    ClearMessageState,
    FetchAllMessageError,
    FetchAllMessageStart,
    FetchAllMessageSuccess,
    FetchUnreadMessageError,
    FetchUnreadMessageSuccess,
    MarkMessagesAsRead,
} from './message.actions';
import { INITIAL_MESSAGE_STATE, MessageStateModel, MessageStateModule } from './message.state';

describe('MessageStateModule', () => {

    let appState: TestAppState;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot([EntityStateModule, MessageStateModule])],
            providers: [TEST_APP_STATE],
        }).compileComponents();
        appState = TestBed.inject(AppStateService) as any;
    }));

    /**
     * Verify that all users and messages in the appState have IS_NORMALIZED set.
     * This is necessary, because jasmine's expect().toEqual() ignores symbols.
     */
    function assertAllMessagesAndUsersNormalized(): void {
        Object.keys(appState.now.entity.message).forEach(
            id => expect(appState.now.entity.message[id][IS_NORMALIZED]).toBe(true),
        );
        Object.keys(appState.now.entity.user).forEach(
            id => expect(appState.now.entity.user[id][IS_NORMALIZED]).toBe(true),
        );
    }

    it('sets the correct initial state', () => {
        appState.selectOnce(state => state.messages).subscribe(messages => {
            expect(messages).toEqual(INITIAL_MESSAGE_STATE);
        });
    });

    it('FetchAllMessageStart works', () => {
        appState.dispatch(new FetchAllMessageStart());
        expect(appState.snapshot().messages).toEqual(jasmine.objectContaining<MessageStateModel>({
            fetching: true,
        }));
    });

    it('FetchAllMessageSuccess works', fakeAsync(() => {
        appState.mockState({});

        const messagesFromServer: MessageFromServer[] = [
            {
                id: 2,
                message: 'Hello',
                sender: { id: 2, firstName: 'Second', lastName: 'User' } as User<Raw>,
                timestamp: 1122334455,
                type: 'INFO',
                isInstantMessage: false,
            },
            {
                id: 3,
                message: 'Hello',
                sender: { id: 2, firstName: 'Second', lastName: 'User' } as User<Raw>,
                timestamp: 1122334455,
                type: 'INFO',
                isInstantMessage: false,
            },
        ];

        const unreadMessagesFromServer: MessageFromServer[] = [
            {
                id: 2,
                message: 'Hello',
                sender: { id: 2, firstName: 'Second', lastName: 'User' } as User<Raw>,
                timestamp: 1122334455,
                type: 'INFO',
                isInstantMessage: false,
            },
        ];

        const instantMessagesFromServer: MessageFromServer[] = [];

        appState.dispatch(new FetchAllMessageSuccess(messagesFromServer, unreadMessagesFromServer, instantMessagesFromServer))
            .toPromise()
            .then(() => {
                expect(appState.now.messages.fetching).toBe(false);
                expect(appState.now.messages.all).toEqual([2, 3]);
                expect(appState.now.messages.read).toEqual([3]);
                expect(appState.now.messages.unread).toEqual([2]);
                expect(appState.now.entity.message).toEqual({
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

                expect(appState.now.entity.user[2]).toEqual(jasmine.objectContaining({
                    id: 2,
                    firstName: 'Second',
                    lastName: 'User',
                }));

                assertAllMessagesAndUsersNormalized();
            });

        tick();
    }));

    it('FetchAllMessagesError works', () => {
        appState.dispatch(new FetchAllMessageError('some error happened'));
        expect(appState.now.messages.fetching).toBe(false);
        expect(appState.now.messages.lastError).toBe('some error happened');
    });

    it('FetchUnreadMessagesStart works', () => {
        appState.dispatch(new FetchAllMessageStart());
        expect(appState.snapshot().messages).toEqual(jasmine.objectContaining<MessageStateModel>({
            fetching: true,
        }));
    });

    describe('FetchUnreadMessagesSuccess', () => {

        beforeEach(() => {
            appState.mockState({
                entity: {
                    message: {
                        1: {
                            id: 1,
                            message: 'Test message',
                            sender: 1,
                            timestamp: 1234567890,
                            type: 'INFO',
                            unread: false,
                            [IS_NORMALIZED]: true,
                        },
                        2: {
                            id: 2,
                            message: 'Second message',
                            sender: 2,
                            timestamp: 1122334455,
                            type: 'INFO',
                            unread: true,
                            [IS_NORMALIZED]: true,
                        },
                    },
                    user: {
                        1: { id: 1, firstName: 'First', lastName: 'User', [IS_NORMALIZED]: true },
                        2: { id: 2, firstName: 'Second', lastName: 'User', [IS_NORMALIZED]: true },
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
                isInstantMessage: false,
            };

            appState.dispatch(new FetchUnreadMessageSuccess([unreadMessage]));

            expect(appState.now.messages.fetching).toBe(false);
            expect(appState.now.messages.all).toEqual([1, 2, 3]);
            expect(appState.now.messages.read).toEqual([1]);
            expect(appState.now.messages.unread).toEqual([2, 3]);
            expect(appState.now.entity.message[3]).toEqual(jasmine.objectContaining({
                id: 3,
                message: 'Third message',
                sender: 3,
                timestamp: 1122334455,
                type: 'INFO',
                unread: true,
            }));
            expect(appState.now.entity.user[3]).toEqual(jasmine.objectContaining({
                id: 3,
                firstName: 'Third',
                lastName: 'User',
            }));
            assertAllMessagesAndUsersNormalized();
        });

        it('does not change the state when no new information was received', () => {
            const alreadyUnreadMessage: MessageFromServer = {
                id: 2,
                message: 'Second message',
                sender: { id: 2, firstName: 'Second', lastName: 'User' } as User<Raw>,
                timestamp: 1122334455,
                type: 'INFO',
                isInstantMessage: false,
            };

            const stateBefore = appState.now;
            appState.dispatch(new FetchUnreadMessageSuccess([alreadyUnreadMessage]));
            const stateAfter = appState.now;

            expect(stateAfter.messages.fetching).toBe(false, 'fetching != false');
            expect(stateAfter.messages === stateBefore.messages).toBe(false, 'messages before === after');
            expect(stateAfter.messages.all === stateBefore.messages.all).toBe(true, '.all changed');
            expect(stateAfter.messages.read === stateBefore.messages.read).toBe(true, '.read changed');
            expect(stateAfter.messages.unread === stateBefore.messages.unread).toBe(true, '.unread changed');
        });

    });

    it('FetchUnreadMessagesError works', () => {
        appState.dispatch(new FetchUnreadMessageError('some error'));
        expect(appState.now.messages.lastError).toBe('some error');
    });

    it('MarkMessagesAsRead works', () => {
        appState.mockState({
            entity: {
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
        const message1 = appState.now.entity.message[1];

        appState.dispatch(new MarkMessagesAsRead([2, 3]));

        expect(appState.now.messages.all).toEqual([1, 2, 3]);
        expect(appState.now.messages.read).toEqual([2, 3]);
        expect(appState.now.messages.unread).toEqual([1]);
        expect(appState.now.entity.message[1].unread).toBe(true);
        expect(appState.now.entity.message[2].unread).toBe(false, 'message 2 not marked as read');
        expect(appState.now.entity.message[3].unread).toBe(false, 'message 3 not marked as read');
        expect(appState.now.entity.message[1]).toBe(message1, 'message 1 reference changed');
    });

    it('ClearMessageState works', () => {
        appState.mockState({
            messages: {
                fetching: false,
                all: [1, 2],
                read: [1],
                unread: [2],
            },
        });

        appState.dispatch(new ClearMessageState());
        expect(appState.now.messages).toEqual(INITIAL_MESSAGE_STATE);
    });

});
