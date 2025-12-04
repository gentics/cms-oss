import { Injectable, OnDestroy } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { MessageFromServer } from '@gentics/cms-models';
import { Subject, Subscription, combineLatest, interval, of } from 'rxjs';
import { delay, filter, map, mergeMap, startWith } from 'rxjs/operators';
import {
    ApplicationStateService,
    InstantMessagesDeliveredAction,
    MessageActionsService,
} from '../../../state';
import { PermissionService } from '../permissions/permission.service';

const DEFAULT_DELAY = 2;
const DEFAULT_INTERVAL = 30;

@Injectable()
export class MessageService implements OnDestroy {
    private fetchInterval = DEFAULT_INTERVAL;
    private fetchDelay = DEFAULT_DELAY;
    private subscription: Subscription;
    private openInbox$ = new Subject<void>();

    constructor(
        private appState: ApplicationStateService,
        private messageActions: MessageActionsService,
        private permissions: PermissionService,
        private notificationService: I18nNotificationService,
    ) {}

    ngOnDestroy(): void {
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
            !this.subscription ||
            delayInSeconds !== this.fetchDelay ||
            intervalInSeconds !== this.fetchInterval
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

    whenInboxOpens(callback: () => void): Subscription {
        return this.openInbox$.subscribe(callback);
    }

    private fetchWhenUserIsLoggedIn(): void {
        const doFetch$ = this.appState
            .select((state) => state.auth.isLoggedIn)
            .pipe(
                mergeMap((loggedIn) => {
                    if (loggedIn) {
                        return this.permissions.viewInbox$;
                    }
                    return of(false);
                }),
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
                    this.messageActions
                        .fetchAllMessages()
                        .then((allMessages) => {
                            const [_all, unread] = allMessages;
                            this.deliverInstantMessagesOnce(unread);
                        });
                } else {
                    this.messageActions.fetchUnreadMessages().then((unread) => {
                        this.deliverInstantMessagesOnce(unread);
                    });
                }
            });
    }

    private deliverInstantMessagesOnce(messages: MessageFromServer[]): void {
        const deliveredInstantMessages =
            this.appState.now.messages.deliveredInstantMessages;

        messages
            .filter(
                (message) =>
                    message.isInstantMessage &&
                    !deliveredInstantMessages.includes(message.id),
            )
            .forEach((message) => this.sendInstantMessage(message));

        const deliveredMessageIds = messages.map((message) => message.id);
        this.appState.dispatch(
            new InstantMessagesDeliveredAction(deliveredMessageIds),
        );
    }

    private sendInstantMessage(message: MessageFromServer): void {
        const msg = this.notificationService.show({
            message: message.message,
            dismissOnClick: false,
            delay: 0,
            action: {
                label: 'message.message_read',
                onClick: () => {
                    msg.dismiss();
                    this.messageActions.markMessagesAsRead([message.id]);
                },
            },
        });
    }
}
