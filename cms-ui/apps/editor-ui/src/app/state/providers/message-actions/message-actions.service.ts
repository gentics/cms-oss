import { Injectable } from '@angular/core';
import { MessageFromServer, MessageListResponse } from '@gentics/cms-models';
import { forkJoin } from 'rxjs';
import { Api } from '../../../core/providers/api/api.service';
import { ApplicationStateService } from '../../providers';
import {
    MessagesFetchingErrorAction,
    MessagesFetchingSuccessAction,
    MessagesReadAction,
    StartMessagesFetchingAction,
} from '../../modules';

@Injectable()
export class MessageActionsService {
    constructor(private appState: ApplicationStateService, private api: Api) {}

    fetchAllMessages(): Promise<MessageFromServer[][]> {
        this.appState.dispatch(new StartMessagesFetchingAction());

        return forkJoin<MessageListResponse, MessageListResponse>([
            this.api.messages.getMessages(false),
            this.api.messages.getMessages(true),
        ])
            .toPromise()
            .then(
                (responses) => {
                    const [all, unread] = responses.map((res) => res.messages);

                    const unreadInboxMessages = unread.filter(
                        (message) => !message.isInstantMessage,
                    );

                    this.appState.dispatch(
                        new MessagesFetchingSuccessAction(
                            false,
                            unreadInboxMessages,
                            all,
                        ),
                    );
                    return [all, unread];
                },
                (error) => {
                    const errorMessage =
                        typeof error === 'string'
                            ? error
                            : // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                            error.message || error.toString();
                    this.appState.dispatch(
                        new MessagesFetchingErrorAction(errorMessage),
                    );
                    return [] as MessageFromServer[][];
                },
            );
    }

    fetchUnreadMessages(): Promise<MessageFromServer[]> {
        this.appState.dispatch(new StartMessagesFetchingAction());

        return this.api.messages
            .getMessages(true)
            .toPromise()
            .then(
                (response) => {
                    this.appState.dispatch(
                        new MessagesFetchingSuccessAction(
                            true,
                            response.messages,
                        ),
                    );
                    return response.messages;
                },
                (error) => {
                    const errorMessage =
                        typeof error === 'string'
                            ? error
                            : // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                            error.message || error.toString();
                    this.appState.dispatch(
                        new MessagesFetchingErrorAction(errorMessage),
                    );
                    return [] as MessageFromServer[];
                },
            );
    }

    markMessagesAsRead(messageIds: number[]): void {
        this.api.messages.markAsRead(messageIds).subscribe(
            (success) => {
                this.appState.dispatch(new MessagesReadAction(messageIds));
            },
            (error) => {
                // Handle error?
            },
        );
    }

    deleteMessages(messageIds: number[]): void {
        const deleteReqs = [];

        messageIds.forEach((messageId) =>
            deleteReqs.push(this.api.messages.deleteMessage(messageId)),
        );

        forkJoin(deleteReqs)
            .toPromise()
            .then(
                () => this.fetchAllMessages(),
                (error) => {
                    const errorMessage =
                        typeof error === 'string'
                            ? error
                            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                            : error.message || error.toString();
                    this.appState.dispatch(
                        new MessagesFetchingErrorAction(errorMessage),
                    );
                    return false;
                },
            );
    }
}
