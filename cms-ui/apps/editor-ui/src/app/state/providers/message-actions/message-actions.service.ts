import { Injectable } from '@angular/core';
import { MessageFromServer } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { catchError, forkJoin, map, of, tap } from 'rxjs';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import {
    MessagesFetchingSuccessAction,
    MessagesReadAction,
} from '../../modules';
import { ApplicationStateService } from '../../providers';

@Injectable()
export class MessageActionsService {

    constructor(
        private appState: ApplicationStateService,
        private client: GCMSRestClientService,
        private errorHandler: ErrorHandler,
    ) {}

    fetchAllMessages(): Promise<{
        all: MessageFromServer[];
        unread: MessageFromServer[];
    }> {
        return forkJoin([
            this.client.message.list({ unread: false }).pipe(
                map((res) => res.messages || []),
            ),
            this.client.message.list({ unread: true }).pipe(
                map((res) => res.messages || []),
            ),
        ]).pipe(
            map(([all, unread]) => {
                const unreadInboxMessages = unread.filter(
                    (message) => !message.isInstantMessage,
                );

                this.appState.dispatch(new MessagesFetchingSuccessAction(
                    false,
                    unreadInboxMessages,
                    all,
                ));

                return { all, unread };
            }),
            catchError((error) => {
                this.errorHandler.catch(error, { notification: false });

                return of({
                    all: [],
                    unread: [],
                });
            }),
        ).toPromise();
    }

    fetchUnreadMessages(): Promise<MessageFromServer[]> {
        return this.client.message.list({ unread: true }).pipe(
            map((res) => {
                return res.messages;
            }),
            tap((messages) => {
                this.appState.dispatch(new MessagesFetchingSuccessAction(
                    true,
                    messages,
                ));
            }),
            catchError((error) => {
                this.errorHandler.catch(error, { notification: false });
                return of([]);
            }),
        ).toPromise();
    }

    markMessagesAsRead(messageIds: number[]): void {
        this.client.message.markAsRead({ messages: messageIds }).subscribe({
            next: () => {
                this.appState.dispatch(new MessagesReadAction(messageIds));
            },
            error: (error) => {
                this.errorHandler.catch(error, { notification: true });
            },
        });
    }

    deleteMessages(messageIds: number[]): void {
        forkJoin(messageIds.map((id) => this.client.message.delete(id))).subscribe({
            next: () => {
                this.fetchAllMessages();
            },
            error: (error) => {
                this.errorHandler.catch(error, { notification: true });
            },
        });
    }
}
