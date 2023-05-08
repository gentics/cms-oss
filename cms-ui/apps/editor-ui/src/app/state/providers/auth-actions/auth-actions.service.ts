import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { userSchema } from '@editor-ui/app/common/models';
import { AccessControlledType } from '@gentics/cms-models';
import { normalize } from 'normalizr';
import { Api, ApiError } from '../../../core/providers/api';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { LocalStorage } from '../../../core/providers/local-storage/local-storage.service';
import {
    AddEntitiesAction,
    ChangePasswordAction,
    LoginErrorAction,
    LoginSuccessAction,
    LogoutErrorAction,
    LogoutSuccessAction,
    StartLoginAction,
    StartLogoutAction,
    StartValidationAction,
    UpdateIsAdminAction,
    UpdateSearchFilterAction,
    ValidationErrorAction,
    ValidationSuccessAction,
} from '../../modules';
import { ApplicationStateService } from '../application-state/application-state.service';

@Injectable()
export class AuthActionsService {

    constructor(
        private appState: ApplicationStateService,
        private router: Router,
        private localStorage: LocalStorage,
        private notification: I18nNotification,
        private errorHandler: ErrorHandler,
        private api: Api,
    ) {}

    /**
     * Check the local storage for an sid, and if found, attempt to validate the user's session
     * with the API.
     */
    validateSession(): void {
        let sid = this.localStorage.getSid();
        if (!sid) {
            return;
        }

        this.appState.dispatch(new StartValidationAction());

        this.api.auth.validate(sid)
            .subscribe(res => {
                this.appState.dispatch(new ValidationSuccessAction(sid, res.user));
                const normalizedUser = normalize(res.user, userSchema);
                this.appState.dispatch(new AddEntitiesAction(normalizedUser));
            },
            (error: ApiError) => {
                this.localStorage.setSid(null);
                this.appState.dispatch(new ValidationErrorAction(error.message));

                if (error.reason !== 'auth') {
                    this.errorHandler.catch(error);
                }
            });
    }

    updateAdminState(): void {
        let admin = AccessControlledType.ADMIN;
        let response = this.api.permissions.getPermissionsForType(admin);
        response.subscribe(permissionResponse => {
            const permissionsMap = permissionResponse.permissionsMap.permissions;
            this.appState.dispatch(new UpdateIsAdminAction(permissionsMap.read))
        });
    }

    login(username: string, password: string, returnUrl: string): void {
        this.appState.dispatch(new StartLoginAction());

        this.api.auth.login(username, password)
            .subscribe(res => {
                this.localStorage.setSid(res.sid);
                this.appState.dispatch(new LoginSuccessAction(res.sid, res.user));
                const normalizedUser = normalize(res.user, userSchema);
                this.appState.dispatch(new AddEntitiesAction(normalizedUser));

                if (returnUrl) {
                    this.router.navigateByUrl(returnUrl);
                }
            },
            (error: ApiError) => {
                this.appState.dispatch(new LoginErrorAction(error.message));
                this.errorHandler.catch(error);
            });
    }

    logout(sid: number): Promise<any> {
        this.appState.dispatch(new StartLogoutAction());

        return this.api.auth.logout(sid).toPromise()
            .then(res => {
                this.localStorage.setSid(null);
                this.appState.dispatch(new LogoutSuccessAction());
                this.appState.dispatch(new UpdateSearchFilterAction({
                    changing: false,
                    valid: false,
                    visible: false,
                }));
            },
            (error: ApiError) => {
                this.notification.show({
                    message: error.message,
                    type: 'alert',
                });
                this.appState.dispatch(new LogoutErrorAction(error.message));
            });
    }

    /**
     * Update the user's password.
     */
    changePassword(userId: number, newPassword: string): Promise<any> {
        this.appState.dispatch(new ChangePasswordAction(true));

        return this.api.auth.changePassword(userId, newPassword).toPromise()
            .then(res => {
                this.localStorage.setSid(null);
                this.notification.show({
                    message: 'message.updated_password',
                    type: 'success',
                });
                this.appState.dispatch(new ChangePasswordAction(false));
            })
            .catch(error => {
                this.appState.dispatch(new ChangePasswordAction(false, error.message || error));
                this.errorHandler.catch(error);
            });
    }
}
