import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { I18nNotificationService } from '@gentics/cms-components';
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
    ValidateSuccess,
} from '@gentics/cms-components/auth';
import { GCMSRestClientRequestError } from '@gentics/cms-rest-client';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { AppStateService } from '../../../../state/providers/app-state/app-state.service';
import { EditorUiLocalStorageService } from '../../editor-ui-local-storage/editor-ui-local-storage.service';
import { ErrorHandler } from '../../error-handler';
import { catchError, map, Observable, of } from 'rxjs';

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
     * Validates the session with the API.
     */
    validateSession(): Observable<boolean> {
        this.appState.dispatch(new ValidateStart());

        return this.client.user.me().pipe(
            map((res) => {
                this.appState.dispatch(new ValidateSuccess(res.user));
                return true;
            }),
            catchError((error: GCMSRestClientRequestError) => {
                this.appState.dispatch(new ValidateError(error.message));

                if (error.responseCode !== 401) {
                    this.errorHandler.catch(error);
                }

                return of(false);
            }),
        );
    }

    login(username: string, password: string, returnUrl: string): void {
        this.appState.dispatch(new LoginStart());

        this.client.auth.login({
            login: username,
            password: password,
        }).subscribe({
            next: (res) => {
                this.appState.dispatch(new LoginSuccess(res.user));
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

    logout(): Promise<any> {
        this.appState.dispatch(new LogoutStart());

        return this.client.auth.logout().toPromise()
            .then(() => {
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
                this.notification.show({
                    message: 'modal.updated_password',
                    type: 'success',
                });
                this.appState.dispatch(new ChangePasswordSuccess());
            })
            .catch((error) => {
                this.appState.dispatch(new ChangePasswordError(error.message || error));
                this.errorHandler.catch(error);
            });
    }
}
