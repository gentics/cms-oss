import { Injectable, Injector } from '@angular/core';
import { EntityIdType, FolderListOptions, Image, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class ImageOperations extends ExtendedEntityOperationsBase<'image'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
    ) {
        super(injector, 'image');
    }

    getAll(options: FolderListOptions, parentId: any): Observable<Image<Raw>[]> {
        return this.api.folders.getImages(parentId, options).pipe(
            map(res => res.files as any as Image<Raw>[]),
        );
    }

    get(entityId: EntityIdType, options?: any, parentId?: any): Observable<Image<Raw>> {
        return this.api.images.getImage(entityId).pipe(
            map(res => res.image),
        );
    }
}
