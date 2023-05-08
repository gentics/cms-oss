import { ListResponse, LogActionsRequest, LogsListRequest, LogsListResponse, LogTypeListItem, LogTypesRequest } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

/**
 * API methods related to Logs.
 *
 * Docs for the endpoints used here can be found at:
 * https://localhost:4200/CNPortletapp/guides/restapi/resource_AdminResource.html
 *
 */
export class LogsApi {

    constructor(
        private apiBase: ApiBase,
    ) { }

    /**
     * List all logs
     */
    getLogs(options?: LogsListRequest): Observable<LogsListResponse> {
        return this.apiBase.get('admin/actionlog', options);
    }

    /**
     * Get all actions
     */
    getActions(options?: LogActionsRequest): Observable<ListResponse<LogTypeListItem>> {
        return this.apiBase.get('admin/actionlog/actions', options);
    }

    /**
     * Get all objects
     */
    getTypes(options?: LogTypesRequest): Observable<ListResponse<LogTypeListItem>> {
        return this.apiBase.get('admin/actionlog/types', options);
    }

}
