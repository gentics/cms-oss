import { Injectable, OnDestroy } from '@angular/core';
import { Subject, Subscription, combineLatest, interval, of } from 'rxjs';
import { delay, filter, map, mergeMap } from 'rxjs/operators';
import { ApplicationStateService, MessageActionsService } from '../../../state';
import { PermissionService } from '../permissions/permission.service';

const DEFAULT_DELAY = 5;
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
    ) { }

    ngOnDestroy(): void {
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

    whenInboxOpens(callback: () => void): Subscription {
        return this.openInbox$.subscribe(callback);
    }

    private fetchWhenUserIsLoggedIn(): void {
        const doFetch$ = this.appState.select(state => state.auth.isLoggedIn).pipe(
            mergeMap(loggedIn => {
                if (loggedIn) {
                    return this.permissions.viewInbox$;
                }
                return of(false);
            }),
        );

        this.subscription = combineLatest([
            doFetch$,
            combineLatest([
                interval(this.fetchInterval * 1000),
                of(null).pipe(delay(this.fetchDelay * 1000)),
            ]).map((_, idx) => idx === 0),
        ]).pipe(
            filter(([allow]) => allow),
            map(([_, first]) => first),
        ).subscribe(firstFetch => {
            if (firstFetch) {
                this.messageActions.fetchAllMessages();
            } else {
                this.messageActions.fetchUnreadMessages();
            }
        });
    }

}
