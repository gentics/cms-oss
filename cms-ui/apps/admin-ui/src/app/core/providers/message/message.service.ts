import { Injectable } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import {
    AccessControlledType,
    GcmsPermission,
    MessageFromServer,
} from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { NotificationService } from '@gentics/ui-core';
import {
    combineLatest,
    forkJoin,
    interval,
    Observable,
    of,
    Subject,
    Subscription,
} from 'rxjs';
import { catchError, delay, filter, map, mergeMap, startWith, switchMap } from 'rxjs/operators';
import { ServiceBase } from '../../../shared/providers/service-base/service.base';
import {
    FetchAllMessageSuccess,
    FetchUnreadMessageSuccess,
    MarkMessagesAsRead,
} from '../../../state/messages/message.actions';
import { AppStateService } from '../../../state/providers/app-state/app-state.service';
import { ErrorHandler } from '../error-handler';
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
        private client: GCMSRestClientService,
        private permissions: PermissionsService,
        private notificationService: NotificationService,
        private i18n: I18nService,
        private errorHandler: ErrorHandler,
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
        const doFetch$ = this.appState.select((state) => state.auth.isLoggedIn).pipe(
            switchMap((loggedIn) => {
                if (loggedIn) {
                    return this.hasInboxPermissions().pipe(map((hasPerms) => [loggedIn, hasPerms]));
                }
                return of([false, false]);
            }),
            map(([loggedIn, hasPerms]) => loggedIn && hasPerms),
        );

        this.subscription = combineLatest([
            doFetch$,
            combineLatest([
                interval(this.fetchInterval * 1000).pipe(
                    // Needs to be emitted on default, otherwise `combineLatest` won't publish anything
                    // and this entire observable only starts after the interval, which isn't what's intended.
                    startWith(0),
                ),
                of(null).pipe(delay(this.fetchDelay * 1000)),
            ]),
        ])
            .pipe(
                filter(([allow]) => allow),
                map((_, idx) => idx === 0),
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
        return forkJoin([
            this.client.message.list({ unread: false }).pipe(
                map((res) => res.messages),
            ),
            this.client.message.list({ unread: true }).pipe(
                map((res) => res.messages),
            ),
        ]).pipe(
            map(([all, unread]) => {
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
            }),
            catchError((error) => {
                this.errorHandler.catch(error, { notification: false });
                return of(false);
            }),
        ).toPromise();
    }

    fetchUnreadMessages(): Promise<boolean> {
        return this.client.message.list({ unread: true }).pipe(
            map((res) => {
                this.deliverInstantMessagesOnce(res?.messages);

                this.appState.dispatch(new FetchUnreadMessageSuccess(res?.messages));

                return true;
            }),
            catchError((error) => {
                this.errorHandler.catch(error, { notification: false });
                return of(false);
            }),
        ).toPromise();
    }

    deliverInstantMessagesOnce(messages: MessageFromServer[]): void {
        const deliveredInstantMessages = this.appState.now.messages
            .deliveredInstantMessages;

        messages
            .filter((message) => message.isInstantMessage
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
                    this.client.message.markAsRead({ messages: [message.id] }).subscribe();
                },
            },
        });
    }

    markMessagesAsRead(messageIds: number[]): void {
        this.client.message.markAsRead({ messages: messageIds }).subscribe({
            next: () => this.appState.dispatch(new MarkMessagesAsRead(messageIds)),
            error: (error) => this.errorHandler.catch(error, { notification: true }),
        });
    }

    deleteMessages(messageIds: number[]): Promise<boolean> {
        return forkJoin(messageIds.map((id) => this.client.message.delete(id))).pipe(
            switchMap(() => this.fetchAllMessages()),
            catchError((error) => {
                this.errorHandler.catch(error, { notification: true });
                return of(false);
            }),
        ).toPromise();
    }
}
