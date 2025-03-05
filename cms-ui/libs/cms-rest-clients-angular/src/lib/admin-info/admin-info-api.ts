import {
    BaseListOptionsWithPaging,
    CmsFeatureInfo,
    ContentMaintenanceActionRequest,
    DirtQueueListResponse,
    DirtQueueItem,
    DirtQueueSummary,
    JobListRequestOptions,
    Jobs,
    MaintenanceModeRequestOptions,
    MaintenanceModeResponse,
    PublishInfo,
    PublishQueue,
    Response,
    UpdatesInfo,
    VersionResponse,
    DirtQueueListOptions,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';
import { stringifyPagingSortOptions } from '../util/sort-options/sort-options';

/**
 * API methods related to the admin info.
 *
 * Docs for the endpoints used here can be found at:
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/resource_AdminResource.html
 */
export class AdminInfoApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * Get information of the current publish process
     */
    getPublishInfo(): Observable<PublishInfo> {
        return this.apiBase.get('admin/publishInfo');
    }

    /**
     * Get information of the current publish process per node
     */
    getPublishQueue(): Observable<PublishQueue> {
        return this.apiBase.get('admin/content/publishqueue');
    }

    /**
     * Get information of the current publish process
     */
    getDirtQueue(options?: DirtQueueListOptions): Observable<DirtQueueListResponse> {
        if (options?.sort) {
            const copy: any = {...options };
            copy.sort = stringifyPagingSortOptions(copy.sort);
            options = copy;
        }

        return this.apiBase.get('admin/content/dirtqueue', options);
    }

    /**
     * Get information of the current publish process
     */
    getPublishQueueSummary(): Observable<DirtQueueSummary> {
        return this.apiBase.get('admin/content/dirtqueue/summary');
    }

    /**
     * Get jobs
     */
    getJobs(options?: JobListRequestOptions): Observable<Jobs> {
        return this.apiBase.get('scheduler/jobs', options);
    }

    /**
     * Get available updates
     */
    getUpdates(): Observable<UpdatesInfo> {
        return this.apiBase.get('admin/updates');
    }

    /**
     * Get the current version of the REST API on the server
     */
    getVersion(): Observable<VersionResponse> {
        return this.apiBase.get('admin/version');
    }

    /**
     * Get info about a feature activation
     */
    getFeatureInfo(featureName: string): Observable<CmsFeatureInfo> {
        return this.apiBase.get(`admin/features/${featureName}`);
    }

    /**
     * Get maintenance mode settings.
     */
    getMaintenanceMode(): Observable<MaintenanceModeResponse> {
        return this.apiBase.get('info/maintenance');
    }

    /**
     * Configure maintenance mode settings.
     */
    setMaintenanceMode(options: MaintenanceModeRequestOptions): Observable<MaintenanceModeResponse> {
        return this.apiBase.post('admin/maintenance', options);
    }

    /*
     * Perform a maintenance action on the publish queue
     */
    modifyPublishQueue(payload: ContentMaintenanceActionRequest): Observable<Response> {
        return this.apiBase.post('admin/content/publishqueue', payload);
    }

    /**
     * Repeat the failed dirt queue entry with given ID
     */
    repeatFailedDirtQueueOfNode(actionId: number | string): Observable<void> {
        return this.apiBase.put(`admin/content/dirtqueue/${actionId}/redo`, null) as any as Observable<void>;
    }

    /**
     * Delete the failed dirt queue entry with given ID.
     */
    deleteFailedDirtQueueOfNode(actionId: number | string): Observable<void> {
        return this.apiBase.delete(`admin/content/dirtqueue/${actionId}`);
    }

    /**
     * Reload the CMS configuration
     */
    reloadConfiguration(): Observable<Response> {
        return this.apiBase.put('admin/config/reload', null);
    }

    /**
     * Cancel any CMS publishing processes
     */
    stopPublishing(): Observable<PublishInfo> {
        return this.apiBase.delete('publisher', {"block": "true", "wait": 10000});
    }

}
