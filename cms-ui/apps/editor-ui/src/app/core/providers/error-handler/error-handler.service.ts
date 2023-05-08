import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { LogoutSuccessAction, UpdateSearchFilterAction } from '@editor-ui/app/state';
import { ModalService } from '@gentics/ui-core';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { ApplicationStateService } from '../../../state';
import { ApiError } from '../api';
import { I18nNotification } from '../i18n-notification/i18n-notification.service';

/**
 * A central error handler that shows a notification for occuring errors,
 * logs their details to the console and supports serializing.
 *
 * Usage:
 * ```
 * constructor(api: Api, errorHandler: ErrorHandler) { }
 *
 * this.api.methodThatMightReject()
 *     .then(res => handleSuccess())
 *     .catch(this.errorHandler.catch);
 *
 * // or
 *
 *     .catch(error => {
 *         showCustomNotificationFor(error);
 *         this.errorHandler.catch(error, { notification: false });
 *     })
 * ```
 *
 * Reacting to errors:
 * ```
 * this.errorHandler.caughtErrors$
 *     .subscribe(errorList => {
 *         // do something with errors
 *     });
 * ```
 */
@Injectable()
export class ErrorHandler {

    /** Emits a list of all caught errors when an error is caught. */
    caughtErrors$: Observable<Error[]>;

    private lastError: Error;
    private lastErrorTime: number;
    private errorList = new BehaviorSubject<Error[]>([]);

    constructor(
        private appState: ApplicationStateService,
        private router: Router,
        private modalService: ModalService,
        private translate: TranslateService,
        private notification: I18nNotification,
    ) {

        this.caughtErrors$ = this.errorList.asObservable();
    }

    /**
     * Handles an error thrown by the API or the application.
     * Can be extended later to log client-side errors to the server.
     */
    catch = (error: Error, options?: { notification: boolean }): void => {
        if (!error || (error as any).reason !== 'auth') {
            console.error('Error details: ', error);
        }

        if (error.message) {
            error.message = error.message.replace(/<br\s*\/?>/g, '\n');
        }

        // Only show one notification with the same message within 3 seconds.
        let showNotification = (!options || options.notification !== false) &&
            (!this.lastError || this.lastError.message !== error.message || (Date.now() - this.lastErrorTime) > 3000);

        // Some error messages should always be displayed to the user.
        if (error instanceof ApiError) {
            switch (error.request.url) {
                case 'auth/login':
                    showNotification = true;
            }
        }

        if (error instanceof ApiError) {
            switch (error.reason) {
                case 'failed':
                case 'invalid_data':
                case 'http':
                case 'permissions':
                    // Invalid SID always display the login screen, or tries to login with SSO
                    // So it can mislead the user, therefore we not display it.
                    const msg = (error.response?.responseInfo?.responseMessage || error.response?.toString?.() || '').toLowerCase();
                    const isInvalidSid = msg === 'invalid sid' || msg === 'missing sid';

                    if (showNotification && !isInvalidSid) {
                        this.notification.show({
                            message: error.message || error.toString(),
                            type: 'alert',
                            delay: 10_000,
                        });
                    }

                    // If the user has been signed out (usually due to activating the maintanance-mode),
                    // but the state still isn't updated yet, then we do this now.
                    // Additionally, send the User back to the Login with the proper return URL.
                    if (isInvalidSid && this.appState.now.auth.isLoggedIn) {
                        this.userWasLoggedOut();
                    }

                    break;

                case 'auth':
                    // User should be logged in but is not - he was logged out by the backend.
                    if (this.appState.now.auth.isLoggedIn) {
                        this.userWasLoggedOut();
                    } else if (showNotification) {
                        this.notification.show({
                            message: error.message,
                            type: 'alert',
                            delay: 10_000,
                        });
                    }
                    break;

                default:
                    console.error(`Need to handle: ApiError(reason = "${error.reason}")`);
                    break;
            }
        } else {
            // TODO: If we need to handle other errors, here's the spot for it.
            // debugger;
            if (showNotification) {
                this.notification.show({
                    message: error.message || error.toString(),
                    type: 'alert',
                    delay: 10_000,
                });
            }
        }

        this.lastError = error;
        this.lastErrorTime = Date.now();
        this.errorList.next(this.errorList.value.concat(error));
    }

    /**
     * Serializes the current application state and all occurred errors
     * to a base64-encoded string which can be used to reproduce the error.
     */
    serialize(): string {
        let state = this.appState.now;

        // Do not serialize entities and messages
        let stateToSerialize = {
            auth: state.auth,
            editor: state.editor,
            favourites: state.favourites,
            folder: state.folder,
            publishQueue: state.publishQueue,
            ui: state.ui,
            usage: state.usage,
            user: state.user,
        };

        let json = JSON.stringify({
            state: stateToSerialize,
            errors: this.errorList.value,
            url: this.router.url,
        });

        // https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64/Base64_encoding_and_decoding
        let escapedUnicode = encodeURIComponent(json).replace(/%([0-9A-F]{2})/g,
            (all: string, hex: string) => String.fromCharCode(Number.parseInt(hex, 16)));

        let base64 = btoa(escapedUnicode);

        return base64;
    }

    /**
     * De-serialize a base64 encoded bug report into the application state
     * and the occurred errors.
     * To get the complete application state, the entities need to be requested from the server.
     */
    deserialize(serializedBase64String: string): { state: any, errors: Error[], url: string } {
        let escapedUnicode = atob(serializedBase64String);

        // https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64/Base64_encoding_and_decoding
        let json = decodeURIComponent(Array.prototype.map.call(escapedUnicode,
            (char: string) => '%' + ('00' + char.charCodeAt(0).toString(16)).slice(-2)).join(''));

        return JSON.parse(json);
    }

    private userWasLoggedOut(): void {
        this.appState.dispatch(new LogoutSuccessAction());
        this.appState.dispatch(new UpdateSearchFilterAction({
            changing: false,
            valid: false,
            visible: false,
        }));
        this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.routerState.snapshot.url } });

        this.modalService.dialog({
            title: this.translate.instant('modal.logged_out_by_backend_title'),
            body: this.translate.instant(this.appState.now.maintenanceMode.active
                ? 'modal.logged_out_by_backend_maintenance'
                : 'modal.logged_out_by_backend_inactivity',
            ),
            buttons: [{
                type: 'default',
                label: this.translate.instant('common.okay_button'),
            }],
        }).then(modal => modal.open());
    }
}
