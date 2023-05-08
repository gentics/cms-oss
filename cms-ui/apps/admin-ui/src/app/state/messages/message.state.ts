import { Injectable } from '@angular/core';
import { GcmsNormalizer, Message, Raw } from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import * as _ from 'lodash';

import { concatUnique, removeEntries } from '../../common/utils/list-utils/list-utils';
import { AddEntities } from '../entity/entity.actions';
import { AppStateService } from '../providers/app-state/app-state.service';
import { ActionDefinition, AppStateBranch, defineInitialState } from '../utils/state-utils';
import {
    ClearMessageState,
    DeleteMessageError,
    FetchAllMessageError,
    FetchAllMessageStart,
    FetchAllMessageSuccess,
    FetchUnreadMessageError,
    FetchUnreadMessageStart,
    FetchUnreadMessageSuccess,
    MarkMessagesAsRead,
} from './message.actions';

export interface MessageStateModel {
    all: number[];
    read: number[];
    unread: number[];
    fetching: boolean;
    lastError?: string;
}

export const INITIAL_MESSAGE_STATE = defineInitialState<MessageStateModel>({
    fetching: false,
    all: [],
    read: [],
    unread: [],
    lastError: undefined,
});

@AppStateBranch({
    name: 'messages',
    defaults: INITIAL_MESSAGE_STATE,
})
@Injectable()
export class MessageStateModule {

    private normalizer = new GcmsNormalizer();

    constructor(private appState: AppStateService) {}

    @ActionDefinition(FetchAllMessageStart)
    fetchAllMessageStart(ctx: StateContext<MessageStateModel>): void {
        ctx.patchState({
            fetching: true,
        });
    }

    @ActionDefinition(FetchAllMessageSuccess)
    fetchAllMessageSuccess(ctx: StateContext<MessageStateModel>, action: FetchAllMessageSuccess): Promise<void> {
        const messages = action.allMessagesFromServer as Message<Raw>[];
        const unreadIds = action.unreadMessagesFromServer.map(msg => msg.id);

        const readMessages: Message<Raw>[] = [];
        const unreadMessages: Message<Raw>[] = [];

        const newMessageEntities: Message<Raw>[] = [];

        for (const message of messages) {
            message.unread = unreadIds.indexOf(message.id) >= 0;
            (message.unread ? unreadMessages : readMessages).push(message);

            newMessageEntities.push(message);
        }

        const normalized = this.normalizer.normalize('message', newMessageEntities);

        return ctx.dispatch(new AddEntities(normalized.entities))
            .toPromise()
            .then(() => {
                ctx.patchState({
                    fetching: false,
                    all: messages.map(msg => msg.id),
                    read: readMessages.map(msg => msg.id),
                    unread: unreadMessages.map(msg => msg.id),
                });
            });
    }

    @ActionDefinition(FetchAllMessageError)
    fetchAllMessageError(ctx: StateContext<MessageStateModel>, action: FetchAllMessageError): void {
        ctx.patchState({
            fetching: false,
            lastError: action.errorMessage,
        });
    }

    @ActionDefinition(FetchUnreadMessageStart)
    fetchUnreadMessageStart(ctx: StateContext<MessageStateModel>): void {
        ctx.patchState({
            fetching: true,
        });
    }

    @ActionDefinition(FetchUnreadMessageSuccess)
    fetchUnreadMessagesSuccess(ctx: StateContext<MessageStateModel>, action: FetchUnreadMessageSuccess): void {
        const newMessages = (action.unreadMessagesFromServer as Message<Raw>[])
            .filter(msg => !this.appState.now.entity.message[msg.id]);

        if (!newMessages.length && ctx.getState().unread.length === action.unreadMessagesFromServer.length) {
            // Nothing to do
            if (ctx.getState().fetching) {
                ctx.patchState({
                    fetching: false,
                });
            }
            return;
        }

        const normalized = this.normalizer.normalize('message', newMessages);
        const allIds = [ ...ctx.getState().all ];
        const unreadIds = [ ...ctx.getState().unread ];
        newMessages.forEach(newMsg => {
            normalized.entities.message[newMsg.id].unread = true;
            allIds.push(newMsg.id);
            unreadIds.push(newMsg.id);
        });

        ctx.dispatch(new AddEntities(normalized.entities));

        ctx.patchState({
            fetching: false,
            all: allIds,
            read: ctx.getState().all.filter(id => unreadIds.indexOf(id) < 0),
            unread: unreadIds,
        });
    }

    @ActionDefinition(FetchUnreadMessageError)
    fetchUnreadMessageError(ctx: StateContext<MessageStateModel>, action: FetchUnreadMessageError): void {
        ctx.patchState({
            fetching: false,
            lastError: action.errorMessage,
        });
    }

    @ActionDefinition(MarkMessagesAsRead)
    markMessagesAsRead(ctx: StateContext<MessageStateModel>, action: MarkMessagesAsRead): void {
        const readMessageIDs = action.messageIds;
        const changes = !!readMessageIDs.length && readMessageIDs.some(id => this.appState.now.entity.message[id].unread);
        if (!changes) {
            // Nothing to do
            return;
        }

        const newMessageEntities = Object.assign({}, this.appState.now.entity.message);
        for (const id of readMessageIDs) {
            if (newMessageEntities[id] && newMessageEntities[id].unread) {
                newMessageEntities[id] = {
                    ...newMessageEntities[id],
                    unread: false,
                };
            }
        }

        ctx.dispatch(new AddEntities({
            message: newMessageEntities,
        }));

        ctx.patchState({
            fetching: false,
            read: concatUnique(ctx.getState().read, readMessageIDs),
            unread: removeEntries(ctx.getState().unread, readMessageIDs),
        });
    }

    @ActionDefinition(ClearMessageState)
    clearMessageState(ctx: StateContext<MessageStateModel>): void {
        ctx.setState(INITIAL_MESSAGE_STATE);
    }

    @ActionDefinition(DeleteMessageError)
    deleteMessagesError(ctx: StateContext<MessageStateModel>, action: FetchUnreadMessageError): void {
        ctx.patchState({
            fetching: false,
            lastError: action.errorMessage,
        });
    }
}
