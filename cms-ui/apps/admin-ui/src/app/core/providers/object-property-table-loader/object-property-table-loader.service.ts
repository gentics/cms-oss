import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, discard, EntityPageResponse, ObjectPropertyBO, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ObjectPropertiesObjectType, ObjectProperty, ObjectPropertyListOptions, ObjectPropertyListResponse } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../entity-manager';
import { ObjectPropertyOperations } from '../operations';

export interface ObjectPropertyTableLoaderOptions {
    packageName?: string;
    types?: ObjectPropertiesObjectType[];
}

@Injectable()
export class ObjectPropertyTableLoaderService extends BaseTableLoaderService<ObjectProperty, ObjectPropertyBO, ObjectPropertyTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: ObjectPropertyOperations,
    ) {
        super(
            'objectProperty',
            entityManager,
            appState,
        );
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string): Promise<void> {
        return this.operations.delete(entityId).pipe(discard()).toPromise();
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: ObjectPropertyTableLoaderOptions,
    ): Observable<EntityPageResponse<ObjectPropertyBO>> {
        const loadOptions: ObjectPropertyListOptions = {
            ...this.createDefaultOptions(options),
            embed: ['category', 'construct'],
        };
        let loader: Observable<ObjectPropertyListResponse>;

        if (additionalOptions?.types) {
            // loadOptions.type = additionalOptions.types;
        }

        if (additionalOptions?.packageName) {
            loader = this.api.devTools.getObjectproperties(additionalOptions.packageName, loadOptions);
        } else {
            loader = this.api.objectproperties.getObjectProperties(loadOptions);
        }

        return loader.pipe(
            map(response => {
                const entities = response.items.map(property => this.mapToBusinessObject(property));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(property: ObjectProperty): ObjectPropertyBO {
        return {
            ...property,
            [BO_ID]: String(property.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: property.name,
        };
    }

}
