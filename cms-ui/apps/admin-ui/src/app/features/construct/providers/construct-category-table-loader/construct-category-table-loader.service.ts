import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, ConstructCategoryBO, discard, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ConstructCategory } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../../../../core/providers/base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../../../../core/providers/entity-manager';
import { ConstructCategoryOperations } from '../../../../core/providers/operations';

@Injectable()
export class ConstructCategoryTableLoaderService extends BaseTableLoaderService<ConstructCategory, ConstructCategoryBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: ConstructCategoryOperations,
    ) {
        super('constructCategory', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.operations.delete(entityId).pipe(discard()).toPromise();
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<ConstructCategoryBO>> {
        const loadOptions = this.createDefaultOptions(options);

        return this.api.constructCategory.getConstructCategoryCategories(loadOptions).pipe(
            map(response => {
                const entities = response.items.map(category => this.mapToBusinessObject(category));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(category: ConstructCategory): ConstructCategoryBO {
        return {
            ...category,
            [BO_ID]: String(category.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: category.name,
        };
    }
}
