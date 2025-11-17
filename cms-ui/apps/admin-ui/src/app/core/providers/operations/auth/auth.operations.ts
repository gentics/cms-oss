import {
    AppStateService,
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
} from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import { Router } from '@angular/router';
import { I18nNotificationService } from '@gentics/cms-components';
import { ApiError, GcmsApi } from '@gentics/cms-rest-clients-angular';
import { EditorUiLocalStorageService } from '../../editor-ui-local-storage/editor-ui-local-storage.service';
import { EntityManagerService } from '../../entity-manager';
import { OperationsBase } from '../operations.base';

@Injectable()
export class AuthOperations extends OperationsBase {

    constructor(
        injector: Injector,
        private appState: AppStateService,
        private editorLocalStorage: EditorUiLocalStorageService,
        private entities: EntityManagerService,
        private router: Router,
        private notification: I18nNotificationService,
        private api: GcmsApi,
    ) {
        super(injector);
    }

    /**
     * Validates the specified sid with the API.
     */
    validateSessionId(sid: number): void {
        this.appState.dispatch(new ValidateStart());

        this.api.auth.validate(sid)
            .subscribe((res) => {
                this.appState.dispatch(new ValidateSuccess(sid, res.user));
                if (this.readSidFromEditorUi() !== sid) {
                    this.storeSidForEditorUi(sid);
                }
                this.entities.addEntity('user', res.user);
            },
            (error: ApiError) => {
                this.storeSidForEditorUi(null);
                this.appState.dispatch(new ValidateError(error.message));

                if (error.reason !== 'auth') {
                    this.errorHandler.catch(error);
                }
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

        this.api.auth.login(username, password)
            .subscribe((res) => {
                this.storeSidForEditorUi(res.sid);
                this.appState.dispatch(new LoginSuccess(res.sid, res.user));
                if (returnUrl) {
                    this.router.navigateByUrl(returnUrl);
                }
                this.entities.addEntity('user', res.user);
            },
            (error: ApiError) => {
                this.appState.dispatch(new LoginError(error.message));
                this.errorHandler.catch(error);
            });
    }

    logout(sid: number): Promise<any> {
        this.appState.dispatch(new LogoutStart());

        return this.api.auth.logout(sid).toPromise()
            .then((res) => {
                this.storeSidForEditorUi(null);
                this.appState.dispatch(new LogoutSuccess());
            },
            (error: ApiError) => {
                this.appState.dispatch(new LogoutError(error.message));
                this.errorHandler.catch(error);
            });
    }

    /**
     * Update the user's password.
     */
    changePassword(userId: number, newPassword: string): Promise<any> {
        this.appState.dispatch(new ChangePasswordStart());

        return this.api.auth.changePassword(userId, newPassword).toPromise()
            .then((res) => {
                this.storeSidForEditorUi(null);
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

    private readSidFromEditorUi(): number | null {
        return this.editorLocalStorage.getSid();
    }

    private storeSidForEditorUi(sid: number | null): void {
        this.editorLocalStorage.setSid(sid);
    }
}
