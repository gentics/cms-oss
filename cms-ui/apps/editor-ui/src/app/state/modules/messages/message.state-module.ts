import { Injectable } from '@angular/core';
import { AnyModelType, IS_NORMALIZED, Message, MessageFromServer, Normalized, Raw, User } from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import { patch } from '@ngxs/store/operators';
import { NormalizedSchema } from 'normalizr';
import { MessageState } from '../../../common/models';
import { ActionDefinition, AppStateBranch, concatUnique } from '../../state-utils';
import { AddEntitiesAction, SetMessageEntitiesAction, UpdateEntitiesAction } from '../entity/entity.actions';
import {
    MESSAGES_STATE_KEY,
    MessagesFetchingErrorAction,
    MessagesFetchingSuccessAction,
    MessagesReadAction,
    StartMessagesFetchingAction,
} from './message.actions';
import { ApplicationStateService } from '../../providers';

const INITIAL_MESSAGES_STATE: MessageState = {
    fetching: false,
    all: [],
    read: [],
    unread: [],
    lastError: undefined,
};

@AppStateBranch<MessageState>({
    name: MESSAGES_STATE_KEY,
    defaults: INITIAL_MESSAGES_STATE,
    })
@Injectable()
export class MessageStateModule {

    constructor(
        private appState: ApplicationStateService,
    ) {}

    @ActionDefinition(StartMessagesFetchingAction)
    handleStartMessagesFetchingAction(ctx: StateContext<MessageState>, action: StartMessagesFetchingAction): void {
        ctx.patchState({
            fetching: true,
        });
    }

    @ActionDefinition(MessagesFetchingSuccessAction)
    handleMessagesFetchSuccessAction(ctx: StateContext<MessageState>, action: MessagesFetchingSuccessAction): void {
        const state = ctx.getState();
        let newMessages: MessageFromServer[];

        if (!action.onlyUnread) {
            newMessages = action.all;
        } else {
            newMessages = action.unread;

            // No new messages from the server, nothing to do
            if (newMessages.length === 0) {
                ctx.patchState({
                    fetching: false,
                });
                return;
            }
        }

        let allMessageIds = newMessages.map(msg => msg.id);
        let unreadIds = action.unread.map(msg => msg.id);

        const { messages, normalized } = normalizeMessages(newMessages);
        const messagesMap = {} as { [id: number]: Message<Normalized> };

        for (let message of messages) {
            message.unread = action.onlyUnread || unreadIds.includes(message.id);
            messagesMap[message.id] = message;
        }

        ctx.dispatch(new AddEntitiesAction(normalized));

        if (!action.onlyUnread) {
            ctx.dispatch(new SetMessageEntitiesAction(messagesMap));
        } else {
            // When we update the unread only, then we need to append all ids instead of replacing them
            allMessageIds = concatUnique(state.all || [], allMessageIds);
            unreadIds = concatUnique(state.unread || [], unreadIds);
        }

        ctx.setState(patch<MessageState>({
            fetching: false,
            all: allMessageIds,
            read: allMessageIds.filter(id => !unreadIds.includes(id)),
            unread: unreadIds,
        }));
    }

    @ActionDefinition(MessagesFetchingErrorAction)
    handleMessagesFetchingErrorAction(ctx: StateContext<MessageState>, action: MessagesFetchingErrorAction): void {
        ctx.patchState({
            fetching: false,
            lastError: action.errorMessage,
        });
    }

    @ActionDefinition(MessagesReadAction)
    handleMessagesReadAction(ctx: StateContext<MessageState>, action: MessagesReadAction): void {
        const state = ctx.getState();
        const messageEntities = this.appState.now.entities.message;
        const hasChanges = action.messageIds?.length && action.messageIds.some(id => messageEntities[id]?.unread);

        if (!hasChanges) {
            // Nothing to do
            return;
        }

        const diff: { [id: number]: Partial<Message> } = {};
        for (const msgId of action.messageIds) {
            diff[msgId] = {
                unread: false,
            };
        }

        ctx.dispatch(new UpdateEntitiesAction({
            message: diff,
        }));

        const allReadIds = concatUnique(state.read || [], action.messageIds);

        ctx.patchState({
            read: allReadIds,
            unread: state.unread.filter(id => !allReadIds.includes(id)),
        });
    }
}

/**
 * Normalizes messages and the users that sent them.
 * Since specific system messages do not have a sender, normalizr can not be used for messages.
 */
function normalizeMessages(messages: MessageFromServer[]): {
    messages: Message<Normalized>[],
    normalized: NormalizedSchema<any, any>,
} {
    const messageHash: { [id: number]: Message<Normalized> } = {};
    const userHash: { [id: number]: User<Raw> } = {};
    const messagesWithUserId: Message<Normalized>[] = [];
    const messageIds: number[] = [];

    for (let message of messages as Array<Message<AnyModelType>>) {
        const normalizedMessage: Message<Normalized> = { ...message, sender: 1, [IS_NORMALIZED]: true };
        if (typeof message.sender === 'object' && message.sender) {
            userHash[message.sender.id] = message.sender;
            normalizedMessage.sender = message.sender.id;
        } else if (typeof message.sender === 'number') {
            normalizedMessage.sender = message.sender;
        }
        messagesWithUserId.push(messageHash[message.id] = normalizedMessage);
        messageIds.push(message.id);
    }

    return {
        messages: messagesWithUserId,
        normalized: {
            entities: {
                user: userHash,
                message: messageHash,
            },
            result: messageIds,
        },
    };
}

