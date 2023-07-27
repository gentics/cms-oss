import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, ConstructBO, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { DevToolsConstructListResponse, PagedConstructListRequestOptions, PermissionListResponse, TagType } from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { ConstructHandlerService } from '../construct-handler/construct-handler.service';
import { EntityManagerService } from '../entity-manager';

export interface ConstructTableLoaderOptions {
    packageName?: string;
    dataSourceId?: string | number;
}

@Injectable()
export class ConstructTableLoaderService extends BaseTableLoaderService<TagType, ConstructBO, ConstructTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected handler: ConstructHandlerService,
    ) {
        super('construct', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.handler.delete(entityId).toPromise();
    }

    public override storeEntity(entity: ConstructBO, id?: string): void {
        if (!id) {
            id = entity[BO_ID];
        }

        const old = this.storedEntities[id];

        if (!old) {
            this.storedEntities[id] = entity;
            return;
        }

        // Not all responses from constructs contain the permissions. To not accidently override
        // them with entites which do not have it, copy it from the stored one.
        // Creates an edge-case where the permissions are changed for constructs and aren't fetched
        // again, but that is extremely unlikely.
        if (
            (!entity[BO_PERMISSIONS] || entity[BO_PERMISSIONS].length === 0)
            && (old[BO_PERMISSIONS] && old[BO_PERMISSIONS].length > 0)
        ) {
            entity[BO_PERMISSIONS] = old[BO_PERMISSIONS];
        }

        this.storedEntities[id] = entity;
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: ConstructTableLoaderOptions,
    ): Observable<EntityPageResponse<ConstructBO>> {
        const loadOptions: PagedConstructListRequestOptions = {
            ...this.createDefaultOptions(options) as any,
            perms: true,
            embed: 'category',
        };
        let loader: Observable<PermissionListResponse<TagType> | DevToolsConstructListResponse>;

        if (additionalOptions?.packageName) {
            loader = this.handler.listFromDevtool(additionalOptions.packageName, null as never, loadOptions);
        } else if (additionalOptions?.dataSourceId) {
            loader = this.handler.listFromDataSource(additionalOptions.dataSourceId, null as never, loadOptions as any);
        } else {
            loader = this.handler.list(null as never, loadOptions);
        }

        return loader.pipe(
            map(response => {
                const entities = response.items.map(construct => this.mapToBusinessObject(construct));
                this.applyPermissions(entities, response as PermissionListResponse<TagType>);

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(construct: TagType): ConstructBO {
        return {
            ...construct,
            [BO_ID]: String(construct.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: this.handler.displayName(construct),
        };
    }

}
