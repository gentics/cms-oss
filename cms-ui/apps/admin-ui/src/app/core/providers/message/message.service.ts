import { ServiceBase } from '@admin-ui/shared/providers/service-base/service.base';
import { AppStateService } from '@admin-ui/state';
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
import {
    AccessControlledType,
    GcmsPermission,
    MessageFromServer,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { NotificationService } from '@gentics/ui-core';
import { I18nService } from '@gentics/cms-components';
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

const DEFAULT_DELAY = 2;
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
        private notificationService: NotificationService,
        private i18n: I18nService,
    ) {
        super();
    }

    protected onServiceDestroy(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
    }

    /** Polls for new messages when a user is logged in and has inbox permissions. */
    poll(
        delayInSeconds: number = DEFAULT_DELAY,
        intervalInSeconds: number = DEFAULT_INTERVAL,
    ): Subscription {
        if (
            !this.subscription
            || delayInSeconds !== this.fetchDelay
            || intervalInSeconds !== this.fetchInterval
        ) {
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
        return this.permissions
            .getTypePermissions(AccessControlledType.INBOX)
            .pipe(
                map((permissions) =>
                    permissions.hasPermission(GcmsPermission.READ),
                ),
            );
    }

    private fetchWhenUserIsLoggedIn(): void {
        this.subscription = this.appState
            .select((state) => state.auth.isLoggedIn)
            .pipe(
                filter((loggedIn) => loggedIn),
                switchMap(() => this.hasInboxPermissions()),
                switchMap((hasPermissions) =>
                    hasPermissions
                        ? timer(
                            this.fetchDelay * 1000,
                            this.fetchInterval * 1000,
                        ).pipe(map((ignored, index) => index === 0))
                        : NEVER,
                ),
            )
            .subscribe((firstFetch) => {
                if (firstFetch) {
                    this.fetchAllMessages();
                } else {
                    this.fetchUnreadMessages();
                }
            });
    }

    fetchAllMessages(): Promise<boolean> {
        this.appState.dispatch(new FetchAllMessageStart());

        return forkJoin([
            this.api.messages.getMessages(false),
            this.api.messages.getMessages(true),
        ])
            .toPromise()
            .then(
                (responses) => {
                    const [all, unread] = responses.map((res) => res.messages);
                    const instantMessages = unread.filter(
                        (message) => message.isInstantMessage,
                    );
                    const unreadInboxMessages = unread.filter(
                        (message) => !message.isInstantMessage,
                    );

                    this.deliverInstantMessagesOnce(instantMessages);

                    this.appState.dispatch(
                        new FetchAllMessageSuccess(
                            all,
                            unreadInboxMessages,
                            instantMessages,
                        ),
                    );

                    return true;
                },
                (error) => {
                    const errorMessage
                        = typeof error === 'string'
                            ? error
                            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                            : error.message || error.toString();
                    this.appState.dispatch(
                        new FetchAllMessageError(errorMessage),
                    );
                    return false;
                },
            );
    }

    fetchUnreadMessages(): Promise<boolean> {
        this.appState.dispatch(new FetchUnreadMessageStart());

        return this.api.messages
            .getMessages(true)
            .toPromise()
            .then(
                (response) => {
                    this.deliverInstantMessagesOnce(response?.messages);

                    this.appState.dispatch(
                        new FetchUnreadMessageSuccess(response?.messages),
                    );

                    return true;
                },
                (error) => {
                    const errorMessage
                        = typeof error === 'string'
                            ? error
                            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                            : error.message || error.toString();
                    this.appState.dispatch(
                        new FetchUnreadMessageError(errorMessage),
                    );
                    return false;
                },
            );
    }

    deliverInstantMessagesOnce(messages: MessageFromServer[]): void {
        const deliveredInstantMessages = this.appState.now.messages
            .deliveredInstantMessages;

        messages
            .filter(
                (message) =>
                    message.isInstantMessage
                    && !deliveredInstantMessages.includes(message.id),
            )
            .forEach((undeliveredInstantMessage) => {
                this.sendInstantMessage(undeliveredInstantMessage);
            });
    }

    private sendInstantMessage(message: MessageFromServer): void {
        const msg = this.notificationService.show({
            message: message.message,
            dismissOnClick: false,
            delay: 0,
            action: {
                label: this.i18n.instant('common.message_read_label'),
                onClick: () => {
                    msg.dismiss();
                    this.api.messages.markAsRead([message.id]).subscribe();
                },
            },
        });
    }

    markMessagesAsRead(messageIds: number[]): void {
        this.api.messages.markAsRead(messageIds).subscribe(
            () => this.appState.dispatch(new MarkMessagesAsRead(messageIds)),
        );
    }

    deleteMessages(messageIds: number[]): Promise<boolean> {
        const deleteReqs = [];

        messageIds.forEach((messageId) =>
            deleteReqs.push(this.api.messages.deleteMessage(messageId)),
        );

        return forkJoin(deleteReqs)
            .toPromise()
            .then(
                () => this.fetchAllMessages(),
                (error) => {
                    const errorMessage
                        = typeof error === 'string'
                            ? error
                            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                            : error.message || error.toString();
                    this.appState.dispatch(
                        new DeleteMessageError(errorMessage),
                    );
                    return false;
                },
            );
    }
}
