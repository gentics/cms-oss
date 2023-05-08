import { Injectable, Injector } from '@angular/core';
import { EntityIdType, File, FileListOptions, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class FileOperations extends ExtendedEntityOperationsBase<'file'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
    ) {
        super(injector, 'file');
    }

    getAll(options: FileListOptions, parentId: any): Observable<File<Raw>[]> {
        return this.api.folders.getFiles(parentId, options).pipe(map(res => res.files));
    }

    get(entityId: EntityIdType, options?: any, parentId?: any): Observable<File<Raw>> {
        return this.api.files.getFile(entityId).pipe(
            map(res => res.file),
        );
    }
}
