import { EntityIdType, PageListOptions, PageListResponse, PageRequestOptions, PageResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

export class PageApi {
    constructor(
        private apiBase: ApiBase,
    ) {}

    getPage(id: EntityIdType, options?: PageRequestOptions): Observable<PageResponse> {
        return this.apiBase.get(`page/load/${id}`, options);
    }

    listPages(options?: PageListOptions): Observable<PageListResponse> {
        return this.apiBase.get('page', options);
    }
}
