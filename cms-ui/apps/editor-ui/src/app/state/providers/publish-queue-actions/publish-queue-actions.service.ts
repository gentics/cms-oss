import { Injectable } from '@angular/core';
import { FolderItemType } from '@gentics/cms-models';
import { Api } from '../../../core/providers/api/api.service';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { ApplicationStateService } from '..';
import {
    AssigningUsersToPagesErrorAction,
    AssigningUsersToPagesSuccessAction,
    PublishQueueFetchingErrorAction,
    PublishQueuePagesFetchingSuccessAction,
    PublishQueueUsersFetchingSuccessAction,
    SetPublishQueueListDisplayFieldsAction,
    StartAssigningUsersToPagesAction,
    StartPublishQueueFetchingAction,
} from '../../modules';

@Injectable()
export class PublishQueueActionsService {

    constructor(
        private appState: ApplicationStateService,
        private api: Api,
        private errorHandler: ErrorHandler,
        private notification: I18nNotification,
    ) {}

    /**
     * Fetches the publish queue for all nodes.
     */
    getQueue(): void {
        this.appState.dispatch(new StartPublishQueueFetchingAction());

        this.api.publishQueue.getPublishQueue().subscribe(res => {
            this.appState.dispatch(new PublishQueuePagesFetchingSuccessAction(res.pages));
        }, err => {
            this.notification.show({
                message: err,
                type: 'alert',
                delay: 5000,
            });

            this.appState.dispatch(new PublishQueueFetchingErrorAction());
        });
    }

    /**
     * Fetches a list of users to whom a revision may be assigned.
     */
    getUsersForRevision(): void {
        this.appState.dispatch(new StartPublishQueueFetchingAction());

        this.api.user.getUsers().subscribe(response => {
            this.appState.dispatch(new PublishQueueUsersFetchingSuccessAction(response.items));
        }, error => {
            this.appState.dispatch(new PublishQueueFetchingErrorAction());

            this.notification.show({
                message: 'message.get_users_error',
                type: 'alert',
                delay: 2000,
            });

            this.errorHandler.catch(error, { notification: false });
        });
    }

    /**
     * Assign the pages to the given users for revision.
     */
    assignToUsers(pageIds: number[], userIds: number[], message: string): Promise<any> {
        this.appState.dispatch(new StartAssigningUsersToPagesAction());

        return this.api.publishQueue.assignToUsers(pageIds, userIds, message).toPromise()
            .then(() => {
                this.appState.dispatch(new AssigningUsersToPagesSuccessAction(pageIds));
                this.notification.show({
                    type: 'success',
                    message: 'message.assigned_pages_for_revision',
                });
            })
            .catch(err => {
                this.notification.show({
                    message: 'message.assign_pages_error',
                    type: 'alert',
                    delay: 2000,
                });
                this.appState.dispatch(new AssigningUsersToPagesErrorAction());
                this.errorHandler.catch(err, { notification: false });
            });
    }

    /**
     * Set the displayFields for a given type of item.
     */
    setDisplayFields(type: FolderItemType, displayFields: string[]): void {
        this.appState.dispatch(new SetPublishQueueListDisplayFieldsAction(type, displayFields));
    }
}
