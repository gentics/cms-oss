import { Injectable } from '@angular/core';
import { MessageListResponse } from '@gentics/cms-models';
import { forkJoin } from 'rxjs';
import { Api } from '../../../core/providers/api/api.service';
import { ApplicationStateService } from '../../providers';
import { MessagesFetchingErrorAction, MessagesFetchingSuccessAction, MessagesReadAction, StartMessagesFetchingAction } from '../../modules';

@Injectable()
export class MessageActionsService {

    constructor(
        private appState: ApplicationStateService,
        private api: Api,
    ) {}

    fetchAllMessages(): Promise<boolean> {
        this.appState.dispatch(new StartMessagesFetchingAction());

        return forkJoin<MessageListResponse, MessageListResponse>([
            this.api.messages.getMessages(false),
            this.api.messages.getMessages(true),
        ])
            .toPromise()
            .then(responses => {
                let [all, unread] = responses.map(res => res.messages);
                this.appState.dispatch(new MessagesFetchingSuccessAction(false, unread, all));
                return true;
            }, error => {
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                const errorMessage = typeof error === 'string' ? error : (error.message || error.toString());
                this.appState.dispatch(new MessagesFetchingErrorAction(errorMessage));
                return false;
            });
    }

    fetchUnreadMessages(): Promise<boolean> {
        this.appState.dispatch(new StartMessagesFetchingAction());

        return this.api.messages.getMessages(true)
            .toPromise()
            .then(response => {
                this.appState.dispatch(new MessagesFetchingSuccessAction(true, response.messages));
                return true;
            }, error => {
                const errorMessage = typeof error === 'string' ? error : (error.message || error.toString());
                this.appState.dispatch(new MessagesFetchingErrorAction(errorMessage));
                return false;
            });
    }

    markMessagesAsRead(messageIds: number[]): void {
        this.api.messages.markAsRead(messageIds).subscribe(success => {
            this.appState.dispatch(new MessagesReadAction(messageIds));
        }, error => {
            // Handle error?
        });
    }

    deleteMessages(messageIds: number[]): Promise<boolean> {
        const deleteReqs = [];

        messageIds.forEach(messageId =>
            deleteReqs.push(
                this.api.messages.deleteMessage(messageId),
            ),
        );

        return forkJoin(deleteReqs)
            .toPromise()
            .then(
                () => this.fetchAllMessages(),
                error => {
                    const errorMessage = typeof error === 'string' ? error : (error.message || error.toString());
                    this.appState.dispatch(new MessagesFetchingErrorAction(errorMessage));
                    return false;
                },
            );
    }
}
