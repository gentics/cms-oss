import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { ApiError } from '@gentics/cms-rest-clients-angular';
import { TranslateService } from '@ngx-translate/core';
import { ModalService } from '@gentics/ui-core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { ServiceBase } from '../../../shared/providers/service-base/service.base';
import { AppState, AppStateService, LogoutSuccess } from '../../../state';
import { I18nNotificationService } from '../i18n-notification/i18n-notification.service';


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
export class ErrorHandler extends ServiceBase {

    /** Emits a list of all caught errors when an error is caught. */
    caughtErrors$: Observable<Error[]>;

    private lastError: Error;
    private lastErrorTime: number;
    private errorList = new BehaviorSubject<Error[]>([]);

    constructor(
        private appState: AppStateService,
        private router: Router,
        private modalService: ModalService,
        private translate: TranslateService,
        private notification: I18nNotificationService,
    ) {
        super();
        this.caughtErrors$ = this.errorList.asObservable();
    }

    /**
     * Handles an error thrown by the API or the application.
     * Can be extended later to log client-side errors to the server.
     */
    catch = (error: Error, options?: { notification: boolean }): string => {
        let returnValue: string;
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
            // Invalid SID always display the login screen, or tries to login with SSO
            // So it can mislead the user, therefore we not display it.
            const isInvalidSid = !!error.response && error.response.toString().toLowerCase() === 'invalid sid';

            switch (error.reason) {
                case 'failed':
                case 'http':
                    // If 'invalid sid' response get as http error, and we are still logged in, the backend logged us out
                    if (isInvalidSid && this.appState.snapshot().auth.isLoggedIn) {
                        returnValue = error.message || error.toString();
                        this.userWasLoggedOut();
                        return returnValue;
                    }
                case 'permissions':
                    returnValue = error.message || error.toString();
                    if (showNotification && !isInvalidSid) {
                        this.notification.show({
                            message: returnValue,
                            type: 'alert',
                            delay: 10000,
                        });
                    }
                    // break;
                    return returnValue;

                case 'auth':
                    // User should be logged in but is not - he was logged out by the backend.
                    if (this.appState.snapshot().auth.isLoggedIn) {
                        this.userWasLoggedOut();
                    } else if (showNotification) {
                        returnValue = error.message;
                        this.notification.show({
                            message: returnValue,
                            type: 'alert',
                            delay: 10000,
                        });
                    }
                    // break;
                    return returnValue;

                default:
                    console.error(`Need to handle: ApiError(reason = "${error.reason}")`);
                    // break;
                    returnValue = error.message;
                    return returnValue;
            }
        } else {
            // TODO: If we need to handle other errors, here's the spot for it.
            // debugger;
            if (showNotification) {
                returnValue = error.message || error.toString();
                this.notification.show({
                    message: returnValue,
                    type: 'alert',
                    delay: 10000,
                });
            }
        }

        this.lastError = error;
        this.lastErrorTime = Date.now();
        this.errorList.next(this.errorList.value.concat(error));

        return returnValue;
    }

    /**
     * Handles the error by showing a notification and then rethrowing it.
     */
    notifyAndRethrow(error: Error): never {
        this.catch(error, { notification: true });
        throw error;
    }

    /**
     * Handles the error by showing a notification and returning error message string.
     */
    notifyAndReturnErrorMessage(error: Error): string {
        const errorMsg = this.catch(error, { notification: true });
        return errorMsg;
    }

    /**
     * Serializes the current application state and all occurred errors
     * to a base64-encoded string which can be used to reproduce the error.
     */
    serialize(): string {
        const state = this.appState.snapshot();

        // Do not serialize entities and messages
        const stateToSerialize: Partial<AppState> = {
            auth: state.auth,
            maintenanceMode: state.maintenanceMode,
            ui: state.ui,
        };

        const json = JSON.stringify({
            state: stateToSerialize,
            errors: this.errorList.value,
            url: this.router.url,
        });

        // https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64/Base64_encoding_and_decoding
        const escapedUnicode = encodeURIComponent(json).replace(/%([0-9A-F]{2})/g,
            (all: string, hex: string) => String.fromCharCode(Number.parseInt(hex, 16)));

        const base64 = btoa(escapedUnicode);

        return base64;
    }

    /**
     * De-serialize a base64 encoded bug report into the application state
     * and the occurred errors.
     * To get the complete application state, the entities need to be requested from the server.
     */
    deserialize(serializedBase64String: string): { state: any, errors: Error[], url: string } {
        const escapedUnicode = atob(serializedBase64String);

        // https://developer.mozilla.org/en-US/docs/Web/API/WindowBase64/Base64_encoding_and_decoding
        const json = decodeURIComponent(Array.prototype.map.call(escapedUnicode,
            (char: string) => '%' + ('00' + char.charCodeAt(0).toString(16)).slice(-2)).join(''));

        return JSON.parse(json);
    }

    private userWasLoggedOut(): void {
        this.appState.dispatch(new LogoutSuccess());
        this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.routerState.snapshot.url } });

        this.modalService.dialog({
            title: this.translate.instant('modal.logged_out_by_backend_title'),
            body: this.translate.instant(this.appState.snapshot().maintenanceMode.active
                ? 'modal.logged_out_by_backend_maintenance'
                : 'modal.logged_out_by_backend_inactivity'),
            buttons: [{
                type: 'default',
                label: this.translate.instant('common.ok_button'),
            }],
        }).then(modal => modal.open());
    }
}
