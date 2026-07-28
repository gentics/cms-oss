import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { I18nNotificationService } from '@gentics/cms-components';
import { AccessControlledType } from '@gentics/cms-models';
import { GCMSRestClientRequestError } from '@gentics/cms-rest-client';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { normalize } from 'normalizr';
import { userSchema } from '../../../common/models';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { LocalStorage } from '../../../core/providers/local-storage/local-storage.service';
import { AddEntitiesAction } from '../../modules/entity/entity.actions';
import { UpdateSearchFilterAction } from '../../modules/folder/folder.actions';
import { ApplicationStateService } from '../application-state/application-state.service';
import { FolderActionsService } from '../folder-actions/folder-actions.service';
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
import { UpdateIsAdminAction } from '../../modules';

@Injectable()
export class AuthActionsService {

    constructor(
        private appState: ApplicationStateService,
        private router: Router,
        private localStorage: LocalStorage,
        private notification: I18nNotificationService,
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

        await this.appState.dispatch(new ValidateStart()).toPromise();

        try {
            const res = await this.client.user.me({ sid } as any).toPromise();
            const normalizedUser = normalize(res.user, userSchema);
            await this.appState.dispatch(new AddEntitiesAction(normalizedUser)).toPromise();
            await this.appState.dispatch(new ValidateSuccess(sid, res.user)).toPromise();
            return true;
        } catch (error) {
            this.localStorage.setSid(null);
            await this.appState.dispatch(new ValidateError(error.message)).toPromise();

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
        await this.appState.dispatch(new LoginStart()).toPromise();

        try {
            const res = await this.client.auth.login({ login: username, password }).toPromise();
            this.localStorage.setSid(res.sid);
            const normalizedUser = normalize(res.user, userSchema);
            await this.appState.dispatch(new AddEntitiesAction(normalizedUser)).toPromise();
            await this.appState.dispatch(new LoginSuccess(res.sid, res.user)).toPromise();

            if (returnUrl) {
                this.router.navigateByUrl(returnUrl);
            } else {
                this.folderActions.navigateToDefaultNode();
            }

            return true;
        } catch (error) {
            this.appState.dispatch(new LoginError(error.message));
            this.errorHandler.catch(error);
            return false;
        }
    }

    async logout(sid: number): Promise<void> {
        await this.appState.dispatch(new LogoutStart()).toPromise();

        try {
            await this.client.auth.logout(sid).toPromise();
            this.localStorage.setSid(null);
            await this.appState.dispatch(new LogoutSuccess()).toPromise();
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
            await this.appState.dispatch(new LogoutError(error.message)).toPromise();
        }
    }

    /**
     * Update the user's password.
     */
    async changePassword(userId: number, newPassword: string): Promise<boolean> {
        await this.appState.dispatch(new ChangePasswordStart()).toPromise();

        try {
            await this.client.user.update(userId, {
                password: newPassword,
            }).toPromise();
            this.localStorage.setSid(null);
            this.notification.show({
                message: 'message.updated_password',
                type: 'success',
            });
            await this.appState.dispatch(new ChangePasswordSuccess()).toPromise();
            return true;
        } catch (error) {
            await this.appState.dispatch(new ChangePasswordError(error.message || error)).toPromise();
            this.errorHandler.catch(error);
            return false;
        }
    }
}
