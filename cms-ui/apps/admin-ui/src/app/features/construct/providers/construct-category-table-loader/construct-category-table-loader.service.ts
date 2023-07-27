import {
    BO_DISPLAY_NAME,
    BO_ID,
    BO_NEW_SORT_ORDER,
    BO_ORIGINAL_SORT_ORDER,
    BO_PERMISSIONS,
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

        return this.api.constructCategory.getConstructCategoryCategories(loadOptions).pipe(
            map(response => {
                const entities = response.items.map((category, index) => this.mapToBusinessObject(category, sortingByOrder ? index : null));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(category: ConstructCategory, index?: number): ConstructCategoryBO {
        // This is a workaround for setting a proper sort order initially.
        // Categories from existing setups have it all set to 0, which would screw
        // up the sorting fields initially (until sorting is performed once).
        let order = category.sortOrder;
        if (index != null) {
            if (order === 0 && index !== 0) {
                order = index;
            }
        }

        return {
            ...category,
            [BO_ID]: String(category.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: this.handler.displayName(category),
            [BO_ORIGINAL_SORT_ORDER]: order,
            [BO_NEW_SORT_ORDER]: order,
        };
    }
}
