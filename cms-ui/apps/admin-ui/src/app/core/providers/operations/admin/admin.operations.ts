import {
    AppStateService,
    DecrementMasterLoading,
    FetchMaintenanceStatusSuccess,
    IncrementMasterLoading,
    SetCmpVersion,
    SetCmsUpdates,
    SetUsersnapSettings,
} from '@admin-ui/state';
import { Injectable, Injector } from '@angular/core';
import {
    ContentMaintenanceAction,
    ContentMaintenanceActionRequest,
    ContentMaintenanceType,
    DirtQueueItem,
    DirtQueueListOptions,
    DirtQueueSummary,
    JobListRequestOptions,
    Jobs,
    MaintenanceModeRequestOptions,
    MaintenanceModeResponse,
    PublishInfo,
    PublishQueue,
    Response,
    UsersnapSettings,
    Version,
} from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map, tap } from 'rxjs/operators';
import { I18nNotificationService } from '../../i18n-notification';
import { OperationsBase } from '../operations.base';

@Injectable()
export class AdminOperations extends OperationsBase {

    constructor(
        injector: Injector,
        private api: GcmsApi,
        private appState: AppStateService,
        private notification: I18nNotificationService,
    ) {
        super(injector);
    }

    getUsersnapSettings(): Observable<UsersnapSettings> {
        return this.api.admin.getUsersnapSettings().pipe(
            map(response => {
                this.appState.dispatch(new SetUsersnapSettings(response.settings));
                return response.settings;
            }),
            this.catchAndRethrowError(),
        );
    }

    getCmsVersion(silent: boolean = false): Observable<Version> {
        return this.api.adminInfo.getVersion().pipe(
            map(response => {
                const version: Version = {
                    cmpVersion: response.cmpVersion,
                    version: response.version,
                    variant: response.variant,
                    nodeInfo: response.nodeInfo,
                };
                this.appState.dispatch(new SetCmpVersion(version));
                return version;
            }),
            silent ? catchError(() => of(null)) : this.catchAndRethrowError(),
        );
    }

    getCmsUpdates(silent: boolean = false): Observable<string[]> {
        return this.api.adminInfo.getUpdates().pipe(
            map(response => {
                this.appState.dispatch(new SetCmsUpdates(response.available));
                return response.available;
            }),
            silent ? catchError(() => of(null)) : this.catchAndRethrowError(),
        );
    }

    getPublishInfo(): Observable<PublishInfo> {
        return this.api.adminInfo.getPublishInfo().pipe(
            this.catchAndRethrowError(),
        );
    }

    setMaintenanceMode(options: MaintenanceModeRequestOptions): Observable<MaintenanceModeResponse> {
        this.appState.dispatch(new IncrementMasterLoading('setMaintenanceMode'));
        return this.api.adminInfo.setMaintenanceMode(options).pipe(
            tap(response => {
                this.appState.dispatch(new FetchMaintenanceStatusSuccess(response));
                this.notification.show({
                    type: 'success',
                    message: 'shared.set_maintenance_mode_success',
                });
            }),
            catchError(error => {
                this.notification.show({
                    type: 'alert',
                    message: 'shared.set_maintenance_mode_failed',
                });
                return of(error);
            }),
            finalize(() => this.appState.dispatch(new DecrementMasterLoading())),
        );
    }

    getPublishQueue(): Observable<PublishQueue> {
        return this.api.adminInfo.getPublishQueue().pipe(
            this.catchAndRethrowError(),
        );
    }

    getJobs(options?: JobListRequestOptions): Observable<Jobs> {
        return this.api.adminInfo.getJobs(options).pipe(
            map(response => {
                return response;
            }),
            this.catchAndRethrowError(),
        );
    }

    getDirtQueue(options?: DirtQueueListOptions): Observable<DirtQueueItem[]> {
        return this.api.adminInfo.getDirtQueue(options).pipe(
            map(response => response.items),
            this.catchAndRethrowError(),
        );
    }

    getPublishQueueSummary(): Observable<DirtQueueSummary> {
        return this.api.adminInfo.getPublishQueueSummary().pipe(
            this.catchAndRethrowError(),
        );
    }

    // ACTIONS ////////////////////////////////////////////////////////////////////////

    republishObjects(opts: {
        types: ContentMaintenanceType[];
        nodes: number[];
        contentRepositories: number[];
        clearPublishCache: boolean;
        attributes: string[],
        start?: number;
        end?: number;
    }): Observable<Response> {
        const payload: ContentMaintenanceActionRequest = {
            action: ContentMaintenanceAction.dirt,
            types: opts.types,
            nodes: opts.nodes,
            contentRepositories: opts.contentRepositories,
            clearPublishCache: opts.clearPublishCache,
            ...(opts.attributes?.length > 0 && { attributes: opts.attributes }),
            ...(Number.isInteger(opts.start) && { start: opts.start }),
            ...(Number.isInteger(opts.end) && { end: opts.end }),
        };
        this.appState.dispatch(new IncrementMasterLoading('republishObjects'));
        return this.api.adminInfo.modifyPublishQueue(payload).pipe(
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.republish_objects_success',
            })),
            catchError(error => {
                this.notification.show({
                    type: 'alert',
                    message: 'shared.republish_objects_failed',
                });
                return of(error);
            }),
            finalize(() => this.appState.dispatch(new DecrementMasterLoading())),
        );
    }

    delayObjects(opts: {
        types: ContentMaintenanceType[];
        nodes: number[];
        contentRepositories: number[];
        start?: number;
        end?: number;
    }): Observable<Response> {
        const payload: ContentMaintenanceActionRequest = {
            action: ContentMaintenanceAction.delay,
            types: opts.types,
            nodes: opts.nodes,
            contentRepositories: opts.contentRepositories,
            ...(Number.isInteger(opts.start) && { start: opts.start }),
            ...(Number.isInteger(opts.end) && { end: opts.end }),
        };
        this.appState.dispatch(new IncrementMasterLoading('delayObjects'));
        return this.api.adminInfo.modifyPublishQueue(payload).pipe(
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.delay_objects_success',
            })),
            catchError(error => {
                this.notification.show({
                    type: 'alert',
                    message: 'shared.delay_objects_failed',
                });
                return of(error);
            }),
            finalize(() => this.appState.dispatch(new DecrementMasterLoading())),
        );
    }

    republishDelayedObjects(opts: {
        types: ContentMaintenanceType[];
        nodes: number[];
        contentRepositories: number[];
        start?: number;
        end?: number;
    }): Observable<Response> {
        const payload: ContentMaintenanceActionRequest = {
            action: ContentMaintenanceAction.publishDelayed,
            types: opts.types,
            nodes: opts.nodes,
            contentRepositories: opts.contentRepositories,
            ...(Number.isInteger(opts.start) && { start: opts.start }),
            ...(Number.isInteger(opts.end) && { end: opts.end }),
        };
        this.appState.dispatch(new IncrementMasterLoading('republishDelayedObjects'));
        return this.api.adminInfo.modifyPublishQueue(payload).pipe(
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.republish_delayed_objects_success',
            })),
            catchError(error => {
                this.notification.show({
                    type: 'alert',
                    message: 'shared.republish_delayed_objects_failed',
                });
                return of(error);
            }),
            finalize(() => this.appState.dispatch(new DecrementMasterLoading())),
        );
    }

    markObjectsAsPublished(opts: {
        types: ContentMaintenanceType[];
        nodes: number[];
        contentRepositories: number[];
        start?: number;
        end?: number;
    }): Observable<Response> {
        const payload: ContentMaintenanceActionRequest = {
            action: ContentMaintenanceAction.markPublished,
            types: opts.types,
            nodes: opts.nodes,
            contentRepositories: opts.contentRepositories,
            ...(Number.isInteger(opts.start) && { start: opts.start }),
            ...(Number.isInteger(opts.end) && { end: opts.end }),
        };
        this.appState.dispatch(new IncrementMasterLoading('markObjectsAsPublished'));
        return this.api.adminInfo.modifyPublishQueue(payload).pipe(
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.mark_objects_as_published_success',
            })),
            catchError(error => {
                this.notification.show({
                    type: 'alert',
                    message: 'shared.mark_objects_as_published_failed',
                });
                return of(error);
            }),
            finalize(() => this.appState.dispatch(new DecrementMasterLoading())),
        );
    }

    stopPublishing(): Observable<PublishInfo> {
        this.appState.dispatch(new IncrementMasterLoading('stopPublishing'));
        return this.api.adminInfo.stopPublishing().pipe(
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.stop_publishing_success',
            })),
            catchError(error => {
                this.notification.show({
                    type: 'alert',
                    message: 'shared.stop_publishing_failed',
                });
                return of(error);
            }),
            finalize(() => this.appState.dispatch(new DecrementMasterLoading())),
        );
    }

    reloadConfiguration(): Observable<Response> {
        this.appState.dispatch(new IncrementMasterLoading('reloadConfiguration'));
        return this.api.adminInfo.reloadConfiguration().pipe(
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.reload_configuration_success',
            })),
            catchError(error => {
                this.notification.show({
                    type: 'alert',
                    message: 'shared.reload_configuration_failed',
                });
                return of(error);
            }),
            finalize(() => this.appState.dispatch(new DecrementMasterLoading())),
        );
    }

    repeatFailedDirtQueueOfNode(actionId: number | string): Observable<Response> {
        this.appState.dispatch(new IncrementMasterLoading('repeatFailedDirtQueueOfNode'));
        return this.api.adminInfo.repeatFailedDirtQueueOfNode(actionId).pipe(
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.repeat_failed_dirt_queue_of_node_success',
            })),
            catchError(error => {
                this.notification.show({
                    type: 'alert',
                    message: 'shared.repeat_failed_dirt_queue_of_node_failed',
                });
                return of(error);
            }),
            finalize(() => this.appState.dispatch(new DecrementMasterLoading())),
        );
    }

    deleteFailedDirtQueueOfNode(actionId: string | number): Observable<Response> {
        this.appState.dispatch(new IncrementMasterLoading('deleteFailedDirtQueueOfNode'));
        return this.api.adminInfo.deleteFailedDirtQueueOfNode(actionId).pipe(
            tap(() => this.notification.show({
                type: 'success',
                message: 'shared.delete_failed_dirt_queue_of_node_success',
            })),
            catchError(error => {
                this.notification.show({
                    type: 'alert',
                    message: 'shared.delete_failed_dirt_queue_of_node_failed',
                });
                return of(error);
            }),
            finalize(() => this.appState.dispatch(new DecrementMasterLoading())),
        );
    }
}
