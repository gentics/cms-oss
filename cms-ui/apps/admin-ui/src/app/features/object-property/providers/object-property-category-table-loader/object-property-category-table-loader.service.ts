import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, discard, EntityPageResponse, ObjectPropertyCategoryBO, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService, ObjectPropertyCategoryOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ObjectPropertyCategory } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class ObjectPropertyCategoryTableLoaderService extends BaseTableLoaderService<ObjectPropertyCategory, ObjectPropertyCategoryBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: ObjectPropertyCategoryOperations,
    ) {
        super('objectPropertyCategory', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string): Promise<void> {
        return this.operations.delete(entityId).pipe(discard()).toPromise();
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<ObjectPropertyCategoryBO>> {
        const loadOptions = this.createDefaultOptions(options);

        return this.api.objectPropertycategories.getObjectPropertyCategories(loadOptions).pipe(
            map(response => {
                const entities = response.items.map(category => this.mapToBusinessObject(category));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(category: ObjectPropertyCategory): ObjectPropertyCategoryBO {
        return {
            ...category,
            [BO_ID]: String(category.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: category.name,
        };
    }
}
