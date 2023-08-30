import { ObservableStopper } from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { isEqual } from'lodash-es'
import { combineLatest, Subscription, timer } from 'rxjs';
import {
    catchError,
    distinctUntilChanged,
    filter,
    finalize,
    map,
    skip,
    switchMap,
    takeUntil
} from 'rxjs/operators';
import { ServiceBase } from '../../../shared/providers/service-base/service.base';
import { AppStateService } from '../../../state';
import {
    FetchMaintenanceStatusError,
    FetchMaintenanceStatusStart,
    FetchMaintenanceStatusSuccess
} from '../../../state/maintenance-mode/maintenance-mode.actions';
import { ErrorHandler } from '../error-handler/error-handler.service';
import { I18nNotificationService, TranslatedNotificationOptions } from '../i18n-notification/i18n-notification.service';

@Injectable()
export class MaintenanceModeService extends ServiceBase {

    private stopper = new ObservableStopper();

    constructor(
        private api: GcmsApi,
        private appState: AppStateService,
        private errorHandler: ErrorHandler,
        private notification: I18nNotificationService,
    ) {
        super();
    }

    protected onServiceDestroy(): void {
        this.stopper.stop();
    }

    /**
     * A notification is displayed when the maintenance mode is activated
     * or "show maintenance message banner" is enabled server-side.
     *
     * Users can dismiss the message when they are able to log in
     * or when the message is only a warning banner (no maintenance mode).
     *
     * To stop showing notifications, the returned subscription may be unsubscribed.
     * If it is not unsubscribed manually, its source observable will automatically complete
     * when this service is destroyed.
     */
    displayNotificationWhenActive(): Subscription {
        let openNotification = { dismiss: () => {} };
        let dismissedForMessage: string | undefined;

        const subscription = combineLatest([
            this.appState.select(state => state.auth.isLoggedIn),
            this.appState.select(state => state.maintenanceMode),
            this.appState.select(state => state.ui.language),
        ]).pipe(
            map(([loggedIn, maintenanceMode, uiLanguage]) => ({
                showNotification: maintenanceMode.active || (maintenanceMode.showBanner && maintenanceMode.message),
                message: maintenanceMode.message,
                notificationType: maintenanceMode.active ? 'alert' : 'default',
                userDismissible: loggedIn || !maintenanceMode.active,
            })),
            distinctUntilChanged(isEqual),
            takeUntil(this.stopper.stopper$),
            finalize(() => openNotification.dismiss()),
        ).subscribe(({ showNotification, message, notificationType, userDismissible }) => {
            openNotification.dismiss();

            if (this.appState.now.maintenanceMode.reportedByServer === false) {
                subscription.unsubscribe();
            }

            if (showNotification && dismissedForMessage !== message) {
                const notificationOptions: TranslatedNotificationOptions = {
                    delay: -1,
                    dismissOnClick: false,
                    type: notificationType as any,
                    message: message
                        ? 'dashboard.maintenance_mode_active_custom'
                        : 'dashboard.maintenance_mode_active_default',
                    translationParams: { message },
                };

                if (userDismissible) {
                    notificationOptions.action = {
                        label: 'OK',
                        onClick: () => {
                            openNotification.dismiss();
                            dismissedForMessage = message;
                        },
                    };
                }

                openNotification = this.notification.show(notificationOptions);
            }
        });

        return subscription;
    }

    refresh(): Promise<void> {
        this.appState.dispatch(new FetchMaintenanceStatusStart());

        return new Promise<void>(resolve => {
            this.api.info.getMaintenanceModeStatus()
                .subscribe(
                    response => {
                        this.appState.dispatch(new FetchMaintenanceStatusSuccess(response));
                        resolve();
                    },
                    error => {
                        this.appState.dispatch(new FetchMaintenanceStatusError());
                        resolve();
                    },
                );
        });
    }

    /** Check maintenance mode when the user is logged out. */
    refreshOnLogout(): Subscription {
        const subscription = this.appState
            .select(state => state.auth.isLoggedIn).pipe(
                skip(1),
                filter(loggedIn => !loggedIn),
                takeUntil(this.stopper.stopper$),
            ).subscribe(() => this.refresh());

        return subscription;
    }

    /** Check maintenance mode status once initially and perodically after [milliseconds]. */
    refreshPeriodically(milliseconds: number): Subscription {
        const refresh$ = timer(0, milliseconds).pipe(
            takeUntil(this.stopper.stopper$),
        );
        return refresh$.subscribe(() => this.refresh());
    }

    /**
     * When the maintenance mode is activated on the server, revalidate the user session.
     */
    validateSessionWhenActivated(): Subscription {
        return this.appState.select(state => state.maintenanceMode.active).pipe(
            filter(active => active && this.appState.now.auth.isLoggedIn),
            switchMap(() =>
                this.api.auth.validate(this.appState.now.auth.sid).pipe(
                    catchError((err, observable) => {
                        // The error handler is currently responsible for detecting that the user
                        // was logged out by the server and moving them to the login form.
                        // TODO: We should refactor that to a more fitting service.
                        this.errorHandler.catch(err);
                        return observable;
                    }),
                ),
            ),
            takeUntil(this.stopper.stopper$),
        ).subscribe();
    }
}
