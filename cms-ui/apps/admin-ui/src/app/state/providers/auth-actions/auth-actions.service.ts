import { Injectable } from '@angular/core';
import { GCMSRestClientRequestError } from '@gentics/cms-rest-client';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { normalize } from 'normalizr';
import { userSchema } from '../../../common/models';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import {
    ValidateError,
    ValidateStart,
    ValidateSuccess,
} from '@gentics/cms-components/auth';
import { AppStateService } from '../app-state/app-state.service';
import { AddEntitiesAction } from '../../entity/entity.actions';

@Injectable()
export class AuthActionsService {

    constructor(
        private appState: AppStateService,
        private errorHandler: ErrorHandler,
        private client: GCMSRestClientService,
    ) {}

    /**
     * Attempt to validate the user's session
     * with the API.
     */
    async validateSession(): Promise<boolean> {
        await this.appState.dispatch(new ValidateStart()).toPromise();

        try {
            const res = await this.client.user.me().toPromise();
            const normalizedUser = normalize(res.user, userSchema);
            await this.appState.dispatch(new AddEntitiesAction(normalizedUser)).toPromise();
            await this.appState.dispatch(new ValidateSuccess(res.user)).toPromise();
            return true;
        } catch (error) {
            await this.appState.dispatch(new ValidateError(error.message)).toPromise();

            if (error instanceof GCMSRestClientRequestError && error.responseCode !== 401) {
                this.errorHandler.catch(error);
            }

            return false;
        }
    }
}
