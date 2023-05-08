import { ServiceBase } from '@admin-ui/shared/providers/service-base/service.base';
import { AppStateService, selectLogoutEvent } from '@admin-ui/state';
import {
    DeleteMessageError,
    FetchAllMessageError,
    FetchAllMessageStart,
    FetchAllMessageSuccess,
    FetchUnreadMessageError,
    FetchUnreadMessageStart,
    FetchUnreadMessageSuccess,
    MarkMessagesAsRead,
    } from '@admin-ui/state/messages/message.actions';
import { Injectable } from '@angular/core';
import { AccessControlledType, GcmsPermission, MessageListResponse } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import {
    forkJoin,
    NEVER,
    Observable,
    Subject,
    Subscription,
    timer,
    } from 'rxjs';
import { filter, map, switchMap } from 'rxjs/operators';
import { PermissionsService } from '../permissions/permissions.service';

const DEFAULT_DELAY = 5;
const DEFAULT_INTERVAL = 30;

@Injectable()
export class MessageService extends ServiceBase {

    get onOpenInbox$(): Observable<void> {
        return this.openInbox$.asObservable();
    }

    private fetchInterval = DEFAULT_INTERVAL;
    private fetchDelay = DEFAULT_DELAY;
    private subscription: Subscription;
    private openInbox$ = new Subject<void>();

    constructor(
        private appState: AppStateService,
        private api: GcmsApi,
        private permissions: PermissionsService,
    ) {
        super();
    }

    protected onServiceDestroy(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
    }

    /** Polls for new messages when a user is logged in and has inbox permissions. */
    poll(delayInSeconds: number = DEFAULT_DELAY, intervalInSeconds: number = DEFAULT_INTERVAL): Subscription {
        if (!this.subscription || delayInSeconds !== this.fetchDelay || intervalInSeconds !== this.fetchInterval) {
            if (this.subscription) {
                this.subscription.unsubscribe();
            }

            this.fetchDelay = delayInSeconds;
            this.fetchInterval = intervalInSeconds;
            this.fetchWhenUserIsLoggedIn();
        }

        return new Subscription(() => this.subscription.unsubscribe());
    }

    openInbox(): void {
        this.openInbox$.next();
    }

    private hasInboxPermissions(): Observable<boolean> {
        return this.permissions.getTypePermissions(AccessControlledType.INBOX).pipe(
            map(permissions => permissions.hasPermission(GcmsPermission.READ)),
        );
    }

    private fetchWhenUserIsLoggedIn(): void {
        this.subscription = this.appState.select(state => state.auth.isLoggedIn).pipe(
            filter(loggedIn => loggedIn),
            switchMap(() => this.hasInboxPermissions()),
            switchMap(hasPermissions => hasPermissions
                ? timer(this.fetchDelay * 1000, this.fetchInterval * 1000).pipe(
                    map((ignored, index) => index === 0),
                )
                : NEVER,
            ),
        )
        .subscribe(firstFetch => {
            if (firstFetch) {
                this.fetchAllMessages();
            } else {
                this.fetchUnreadMessages();
            }
        });
    }

    fetchAllMessages(): Promise<boolean> {
        this.appState.dispatch(new FetchAllMessageStart());

        return forkJoin<MessageListResponse>(
                this.api.messages.getMessages(false),
                this.api.messages.getMessages(true))
            .toPromise()
            .then(responses => {
                const [all, unread] = responses.map(res => res.messages);
                this.appState.dispatch(new FetchAllMessageSuccess(all, unread));
                return true;
            }, error => {
                const errorMessage = typeof error === 'string' ? error : error.message || error.toString();
                this.appState.dispatch(new FetchAllMessageError(errorMessage));
                return false;
            });
    }

    fetchUnreadMessages(): Promise<boolean> {
        this.appState.dispatch(new FetchUnreadMessageStart());

        return this.api.messages.getMessages(true)
            .toPromise()
            .then(response => {
                this.appState.dispatch(new FetchUnreadMessageSuccess(response.messages));
                return true;
            }, error => {
                const errorMessage = typeof error === 'string' ? error : error.message || error.toString();
                this.appState.dispatch(new FetchUnreadMessageError(errorMessage));
                return false;
            });
    }

    markMessagesAsRead(messageIds: number[]): void {
        this.api.messages.markAsRead(messageIds)
            .subscribe(
                success => this.appState.dispatch(new MarkMessagesAsRead(messageIds)),
                error => {},
            );
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
                        const errorMessage = typeof error === 'string' ? error : error.message || error.toString();
                        this.appState.dispatch(new DeleteMessageError(errorMessage));
                        return false;
                    },
                );
    }


}
