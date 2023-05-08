import { EntityIdType, FileListOptions, FileListResponse, FileResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { ApiBase } from '../base/api-base.service';

export class FileApi {
    constructor(
        private apiBase: ApiBase,
    ) {}

    getFile(id: EntityIdType): Observable<FileResponse> {
        return this.apiBase.get(`file/load/${id}`);
    }

    listFiles(options?: FileListOptions): Observable<FileListResponse> {
        return this.apiBase.get('file', options);
    }
}
