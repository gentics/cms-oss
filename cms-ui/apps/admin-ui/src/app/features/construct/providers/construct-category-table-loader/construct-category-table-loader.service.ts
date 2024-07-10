import {
    ConstructCategoryBO,
    discard,
    EntityPageResponse,
    TableLoadOptions,
} from '@admin-ui/common';
import { ConstructCategoryHandlerService } from '@admin-ui/core/providers/construct-category-handler/construct-category-handler.service';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ConstructCategory } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { TableSortOrder } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../../../../core/providers/base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../../../../core/providers/entity-manager';

@Injectable()
export class ConstructCategoryTableLoaderService extends BaseTableLoaderService<ConstructCategory, ConstructCategoryBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected handler: ConstructCategoryHandlerService,
    ) {
        super('constructCategory', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.handler.delete(entityId).pipe(discard()).toPromise();
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<ConstructCategoryBO>> {
        const loadOptions = this.createDefaultOptions(options);
        const sortingByOrder = options.sortBy === 'sortOrder' && options.sortOrder === TableSortOrder.ASCENDING;

        return this.handler.list(null as never, loadOptions).pipe(
            map(response => {
                const entities = response.items.map((category, index) => this.handler.mapToBusinessObject(category, sortingByOrder ? index : null));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }
}
