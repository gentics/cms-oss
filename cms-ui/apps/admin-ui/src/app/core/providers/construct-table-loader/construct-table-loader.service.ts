import { BO_ID, BO_PERMISSIONS, ConstructBO, EntityList, EntityPageResponse, PackageTableEntityLoader, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { PagedConstructListRequestOptions, TagType } from '@gentics/cms-models';
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
export class ConstructTableLoaderService extends BaseTableLoaderService<TagType, ConstructBO, ConstructTableLoaderOptions>
    implements PackageTableEntityLoader<ConstructBO, ConstructTableLoaderOptions> {

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

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: ConstructTableLoaderOptions,
    ): Observable<EntityPageResponse<ConstructBO>> {
        const loadOptions: PagedConstructListRequestOptions = {
            ...this.createDefaultOptions(options) as any,
            perms: true,
            embed: 'category',
        };
        let loader: Observable<EntityList<ConstructBO>>;

        if (additionalOptions?.packageName) {
            loader = this.handler.listFromDevToolMapped(additionalOptions.packageName, null as never, loadOptions);
        } else if (additionalOptions?.dataSourceId) {
            loader = this.handler.listFromDataSourceMapped(additionalOptions.dataSourceId, null as never, loadOptions as any);
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
