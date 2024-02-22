import { EntityIdType, FileListOptions, FileListResponse, FileRequestOptions, FileResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

export class FileApi {
    constructor(
        private apiBase: ApiBase,
    ) {}

    getFile(id: EntityIdType, options?: FileRequestOptions): Observable<FileResponse> {
        return this.apiBase.get(`file/load/${id}`, options);
    }

    listFiles(options?: FileListOptions): Observable<FileListResponse> {
        return this.apiBase.get('file', options);
    }
}
