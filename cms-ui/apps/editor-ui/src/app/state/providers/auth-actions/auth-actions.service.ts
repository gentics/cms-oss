import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { userSchema } from '@editor-ui/app/common/models';
import { AccessControlledType } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { normalize } from 'normalizr';
import { GCMSRestClientRequestError } from '@gentics/cms-rest-client';
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
import { FolderActionsService } from '../folder-actions/folder-actions.service';

@Injectable()
export class AuthActionsService {

    constructor(
        private appState: ApplicationStateService,
        private router: Router,
        private localStorage: LocalStorage,
        private notification: I18nNotification,
        private errorHandler: ErrorHandler,
        private client: GCMSRestClientService,
        private folderActions: FolderActionsService,
    ) {}

    /**
     * Check the local storage for an sid, and if found, attempt to validate the user's session
     * with the API.
     */
    async validateSession(): Promise<boolean> {
        const sid = this.localStorage.getSid();
        if (!sid) {
            return false;
        }

        await this.appState.dispatch(new StartValidationAction()).toPromise();

        try {
            const res = await this.client.user.me({ sid } as any).toPromise();
            const normalizedUser = normalize(res.user, userSchema);
            await this.appState.dispatch(new AddEntitiesAction(normalizedUser)).toPromise();
            await this.appState.dispatch(new ValidationSuccessAction(sid, res.user)).toPromise();
            return true;
        } catch (error) {
            this.localStorage.setSid(null);
            await this.appState.dispatch(new ValidationErrorAction(error.message)).toPromise();

            if (error instanceof GCMSRestClientRequestError && error.responseCode !== 401) {
                this.errorHandler.catch(error);
            }

            return false;
        }
    }

    updateAdminState(): void {
        const admin = AccessControlledType.ADMIN;
        const response = this.client.permission.getType(admin, { map: true });
        response.subscribe(permissionResponse => {
            const permissionsMap = permissionResponse.permissionsMap.permissions;
            this.appState.dispatch(new UpdateIsAdminAction(permissionsMap.read))
        });
    }

    async login(username: string, password: string, returnUrl?: string): Promise<boolean> {
        await this.appState.dispatch(new StartLoginAction()).toPromise();

        try {
            const res = await this.client.auth.login({ login: username, password }).toPromise();
            this.localStorage.setSid(res.sid);
            const normalizedUser = normalize(res.user, userSchema);
            await this.appState.dispatch(new AddEntitiesAction(normalizedUser)).toPromise();
            await this.appState.dispatch(new LoginSuccessAction(res.sid, res.user)).toPromise();

            if (returnUrl) {
                this.router.navigateByUrl(returnUrl);
            } else {
                this.folderActions.navigateToDefaultNode();
            }

            return true;
        } catch (error) {
            this.appState.dispatch(new LoginErrorAction(error.message));
            this.errorHandler.catch(error);
            return false;
        }
    }

    async logout(sid: number): Promise<void> {
        await this.appState.dispatch(new StartLogoutAction()).toPromise();

        try {
            await this.client.auth.logout(sid).toPromise();
            this.localStorage.setSid(null);
            await this.appState.dispatch(new LogoutSuccessAction()).toPromise();
            await this.appState.dispatch(new UpdateSearchFilterAction({
                changing: false,
                valid: false,
                visible: false,
            })).toPromise();
        } catch (error) {
            this.notification.show({
                message: error.message,
                type: 'alert',
            });
            await this.appState.dispatch(new LogoutErrorAction(error.message)).toPromise();
        }
    }

    /**
     * Update the user's password.
     */
    async changePassword(userId: number, newPassword: string): Promise<boolean> {
        await this.appState.dispatch(new ChangePasswordAction(true)).toPromise();

        try {
            await this.client.user.update(userId, {
                password: newPassword,
            }).toPromise();
            this.localStorage.setSid(null);
            this.notification.show({
                message: 'message.updated_password',
                type: 'success',
            });
            await this.appState.dispatch(new ChangePasswordAction(false)).toPromise();
            return true;
        } catch (error) {
            await this.appState.dispatch(new ChangePasswordAction(false, error.message || error)).toPromise();
            this.errorHandler.catch(error);
            return false;
        }
    }
}
