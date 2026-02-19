import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import {
    ChangePasswordError,
    ChangePasswordStart,
    ChangePasswordSuccess,
    LoginError,
    LoginStart,
    LoginSuccess,
    LogoutError,
    LogoutStart,
    LogoutSuccess,
    ValidateError,
    ValidateStart,
    ValidateSuccess
} from '@gentics/cms-components/auth';
import { GCMSRestClientRequestError } from '@gentics/cms-rest-client';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { EditorUiLocalStorageService } from '../../editor-ui-local-storage/editor-ui-local-storage.service';
import { ErrorHandler } from '../../error-handler';
import { I18nNotificationService } from '../../i18n-notification/i18n-notification.service';

@Injectable()
export class AuthOperations {

    constructor(
        private errorHandler: ErrorHandler,
        private appState: AppStateService,
        private editorLocalStorage: EditorUiLocalStorageService,
        private router: Router,
        private notification: I18nNotificationService,
        private client: GCMSRestClientService,
    ) {}

    /**
     * Validates the specified sid with the API.
     */
    validateSessionId(sid: number): void {
        this.appState.dispatch(new ValidateStart());

        this.client.user.me({ sid: sid }).subscribe({
            next: res => {
                this.appState.dispatch(new ValidateSuccess(sid, res.user));
                if (this.readSidFromEditorUi() !== sid) {
                    this.storeSidForEditorUi(sid);
                }
            },
            error: (error: GCMSRestClientRequestError) => {
                this.storeSidForEditorUi(null);
                this.appState.dispatch(new ValidateError(error.message));

                if (error.responseCode !== 401) {
                    this.errorHandler.catch(error);
                }
            },
        });
    }

    /**
     * Check the local storage for an sid, and if found, attempt to validate the user's session
     * with the API.
     */
    validateSessionFromLocalStorage(): void {
        const sid = this.readSidFromEditorUi();
        if (!sid) {
            return;
        }
        this.validateSessionId(sid);
    }

    login(username: string, password: string, returnUrl: string): void {
        this.appState.dispatch(new LoginStart());

        this.client.auth.login({
            login: username,
            password: password,
        }).subscribe({
            next: res => {
                this.storeSidForEditorUi(res.sid);
                this.appState.dispatch(new LoginSuccess(res.sid, res.user));
                if (returnUrl) {
                    this.router.navigateByUrl(returnUrl);
                }
            },
            error: (error: GCMSRestClientRequestError) => {
                this.appState.dispatch(new LoginError(error.message));
                this.errorHandler.catch(error);
            },
        });
    }

    logout(sid: number): Promise<any> {
        this.appState.dispatch(new LogoutStart());

        return this.client.auth.logout(sid).toPromise()
            .then(() => {
                this.storeSidForEditorUi(null);
                this.appState.dispatch(new LogoutSuccess());
            })
            .catch((error: GCMSRestClientRequestError) => {
                this.appState.dispatch(new LogoutError(error.message));
                this.errorHandler.catch(error);
            });
    }

    /**
     * Update the user's password.
     */
    changePassword(userId: number, newPassword: string): Promise<any> {
        this.appState.dispatch(new ChangePasswordStart());

        return this.client.user.update(userId, {
            password: newPassword,
        }).toPromise()
            .then(() => {
                this.storeSidForEditorUi(null);
                this.notification.show({
                    message: 'modal.updated_password',
                    type: 'success',
                });
                this.appState.dispatch(new ChangePasswordSuccess());
            })
            .catch(error => {
                this.appState.dispatch(new ChangePasswordError(error.message || error));
                this.errorHandler.catch(error);
            });
    }

    private readSidFromEditorUi(): number | null {
        return this.editorLocalStorage.getSid();
    }

    private storeSidForEditorUi(sid: number | null): void {
        this.editorLocalStorage.setSid(sid);
    }
}
