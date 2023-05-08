import { PageListResponse, PublishQueueOptions, QueueApproveResponse, Response } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to the publish queue.
 */
export class PublishQueueApi {

    constructor(private apiBase: ApiBase) {}

    /**
     * Get a list of pages for the given nodeFolder which should be displayed in the publish queue.
     */
    getPublishQueue(options?: PublishQueueOptions): Observable<PageListResponse> {
        return this.apiBase.get('page/pubqueue', options);
    }

    /**
     * Approved queued changes to online status of pages.
     */
    approvePageStatus(ids: number[]): Observable<QueueApproveResponse> {
        return this.apiBase.post('page/pubqueue/approve', { ids });
    }

    /**
     * Assign the given pages to the given users, and sends those users a message.
     */
    assignToUsers(pageIds: number[], userIds: number[], message: string): Observable<Response> {
        return this.apiBase.post('page/assign', {
            message,
            pageIds,
            userIds,
        });
    }
}
