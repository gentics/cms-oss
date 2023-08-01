import { discard, EntityList, EntityPageResponse, ObjectPropertyBO, PackageTableEntityLoader, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ObjectPropertiesObjectType, ObjectProperty, ObjectPropertyListOptions } from '@gentics/cms-models';
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
export class ObjectPropertyTableLoaderService extends BaseTableLoaderService<ObjectProperty, ObjectPropertyBO, ObjectPropertyTableLoaderOptions>
    implements PackageTableEntityLoader<ObjectPropertyBO, ObjectPropertyTableLoaderOptions> {

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
        let loader: Observable<EntityList<ObjectPropertyBO>>;

        if (additionalOptions?.types) {
            // loadOptions.type = additionalOptions.types;
        }

        if (additionalOptions?.packageName) {
            loader = this.handler.listFromDevToolMapped(additionalOptions.packageName, null as never, loadOptions);
        } else {
            loader = this.handler.listMapped(null as never, loadOptions);
        }

        return loader.pipe(
            map(response => {
                return {
                    entities: response.items,
                    totalCount: response.totalItems,
                };
            }),
        );
    }

    addToDevToolPackage(devToolPackage: string, entityId: string | number): Observable<void> {
        return this.handler.addToDevTool(devToolPackage, entityId);
    }

    removeFromDevToolPackage(devToolPackage: string, entityId: string | number): Observable<void> {
        return this.handler.removeFromDevTool(devToolPackage, entityId);
    }
}
