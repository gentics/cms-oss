import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, discard, EntityPageResponse, ObjectPropertyBO, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ObjectPropertiesObjectType, ObjectProperty, ObjectPropertyListOptions, ObjectPropertyListResponse } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../entity-manager';
import { ObjectPropertyHandlerService } from '../object-property-handler/object-property-handler.service';

export interface ObjectPropertyTableLoaderOptions {
    packageName?: string;
    types?: ObjectPropertiesObjectType[];
}

@Injectable()
export class ObjectPropertyTableLoaderService extends BaseTableLoaderService<ObjectProperty, ObjectPropertyBO, ObjectPropertyTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected handler: ObjectPropertyHandlerService,
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
        return this.handler.delete(entityId).pipe(discard()).toPromise();
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
            loader = this.handler.listFromDevtool(additionalOptions.packageName, null as never, loadOptions);
        } else {
            loader = this.handler.list(null as never, loadOptions);
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
            [BO_DISPLAY_NAME]: this.handler.displayName(property),
        };
    }

}
