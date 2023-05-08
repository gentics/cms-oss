import { Injectable } from '@angular/core';
import {
    ApplicationStateService,
    MaintenanceModeFetchErrorAction,
    MaintenanceModeFetchSuccessAction,
    StartMaintenanceModeFetchingAction,
} from '@editor-ui/app/state';
import { Subscription, combineLatest, of } from 'rxjs';
import { catchError, distinctUntilChanged, filter, map, skip, switchMap } from 'rxjs/operators';
import { deepEqual } from '../../../common/utils/deep-equal';
import { Api } from '../api/api.service';
import { ErrorHandler } from '../error-handler/error-handler.service';
import { I18nNotification, TranslatedNotificationOptions } from '../i18n-notification/i18n-notification.service';

@Injectable()
export class MaintenanceModeService {

    constructor(
        private api: Api,
        private appState: ApplicationStateService,
        private errorHandler: ErrorHandler,
        private notification: I18nNotification,
    ) { }

    /**
     * A notification is displayed when the maintenance mode is activated
     * or "show maintenance message banner" is enabled server-side.
     *
     * Users can dismiss the message when they are able to log in
     * or when the message is only a warning banner (no maintenance mode).
     */
    displayNotificationWhenActive(): Subscription {
        let openNotification = { dismiss: () => { } };
        let dismissedForMessage: string | undefined;

        const subscription = combineLatest([
            this.appState.select(state => state.auth.isLoggedIn),
            this.appState.select(state => state.maintenanceMode),
            this.appState.select(state => state.ui.language),
        ]).pipe(
            map(([loggedIn, maintenanceMode]) => ({
                showNotification: maintenanceMode.active || (maintenanceMode.showBanner && maintenanceMode.message),
                message: maintenanceMode.message,
                notificationType: maintenanceMode.active ? 'alert' : 'warning',
                userDismissible: loggedIn || !maintenanceMode.active,
            })),
            distinctUntilChanged(deepEqual),
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
                        ? 'message.maintenance_mode_active_custom'
                        : 'message.maintenance_mode_active_default',
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

        subscription.add(() => openNotification.dismiss());
        return subscription;
    }

    refresh(): Promise<void> {
        /** Maintenance mode endpoint only exists in ContentNode >= 5.27.7 */
        if (this.appState.now.maintenanceMode.reportedByServer === false) {
            return Promise.resolve();
        }

        this.appState.dispatch(new StartMaintenanceModeFetchingAction());

        return this.api.info.getMaintenanceModeStatus().pipe(
            map(response => {
                this.appState.dispatch(new MaintenanceModeFetchSuccessAction(response));
                return;
            }),
            catchError(() => {
                this.appState.dispatch(new MaintenanceModeFetchErrorAction());
                return of<void>();
            }),
        ).toPromise();
    }

    /** Check maintenance mode when the user is logged out. */
    refreshOnLogout(): Subscription {
        const subscription = this.appState
            .select(state => state.auth.isLoggedIn).pipe(
                skip(1),
                filter(loggedIn => !loggedIn),
            ).subscribe(() => {
                this.refresh();
            });

        // Stop polling when the server does not provide the API endpoint
        subscription.add(
            this.appState
                .select(state => state.maintenanceMode.reportedByServer).pipe(
                    filter(reportedByServer => reportedByServer === false),
                ).subscribe(() => subscription.unsubscribe()),
        );

        return subscription;
    }

    /** Check maintenance mode status once initially and perodically after [milliseconds]. */
    refreshPeriodically(milliseconds: number): Subscription {
        const intervalHandle = setInterval(() => {
            this.refresh();
        }, milliseconds);

        // Stop polling when the server does not provide the API endpoint
        const subscription = this.appState
            .select(state => state.maintenanceMode.reportedByServer).pipe(
                filter(reportedByServer => reportedByServer === false),
            ).subscribe(() => clearInterval(intervalHandle));

        subscription.add(() => clearInterval(intervalHandle));

        this.refresh();
        return subscription;
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
        ).subscribe();
    }
}
