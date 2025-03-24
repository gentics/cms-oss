import { DevToolPackageBO, EntityList, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Package } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../../providers/base-table-loader/base-table-loader.service';
import { DevToolPackageHandlerService } from '../dev-tool-package-handler/dev-tool-package-handler.service';
import { EntityManagerService } from '../../../core/providers/entity-manager/entity-manager.service';

export interface DevToolPackageTableLoaderOptions {
    nodeId?: number;
}

@Injectable()
export class DevToolPackageTableLoaderService extends BaseTableLoaderService<Package, DevToolPackageBO, DevToolPackageTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected handler: DevToolPackageHandlerService,
    ) {
        super('package', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string): Promise<void> {
        return this.handler.delete(entityId).toPromise();
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: DevToolPackageTableLoaderOptions,
    ): Observable<EntityPageResponse<DevToolPackageBO>> {
        const loadOptions = this.createDefaultOptions(options);
        let loader: Observable<EntityList<DevToolPackageBO>>;

        if (additionalOptions?.nodeId) {
            loader = this.handler.listFromNodeMapped(additionalOptions.nodeId, null as never, loadOptions);
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
}
