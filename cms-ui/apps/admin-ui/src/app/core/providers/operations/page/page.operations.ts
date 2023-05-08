import { Injectable, Injector } from '@angular/core';
import { EntityIdType, Page, PageListOptions, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ExtendedEntityOperationsBase } from '../extended-entity-operations';

@Injectable()
export class PageOperations extends ExtendedEntityOperationsBase<'page'> {

    constructor(
        injector: Injector,
        private api: GcmsApi,
    ) {
        super(injector, 'page');
    }

    getAll(options: PageListOptions, parentId: any): Observable<Page<Raw>[]> {
        return this.api.folders.getPages(parentId, options).pipe(
            map(res => res.pages),
        );
    }

    get(entityId: EntityIdType, options?: any, parentId?: any): Observable<Page<Raw>> {
        return this.api.pages.getPage(entityId).pipe(
            map(res => res.page),
        );
    }
}
