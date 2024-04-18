import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, DevToolPackageBO, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Package, PackageListResponse } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../entity-manager/entity-manager.service';
import { PackageOperations } from '../operations/package/package.operations';

export interface DevToolPackageTableLoaderOptions {
    nodeId?: number;
}

@Injectable()
export class DevToolPackageTableLoaderService extends BaseTableLoaderService<Package, DevToolPackageBO, DevToolPackageTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: PackageOperations,
    ) {
        super('package', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string): Promise<void> {
        return this.operations.removePackage(entityId).toPromise();
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: DevToolPackageTableLoaderOptions,
    ): Observable<EntityPageResponse<DevToolPackageBO>> {
        const loadOptions = this.createDefaultOptions(options);

        if (additionalOptions?.nodeId) {
            return this.operations.getPackagesOfNode(additionalOptions.nodeId, loadOptions).pipe(
                map(response => {
                    const entities = response.map(pkg => this.mapToBusinessObject(pkg));

                    return {
                        entities,
                        totalCount: response.length,
                    }
                }),
            )
        } else {
            return this.api.devTools.getPackages(loadOptions).pipe(
                map(response => {
                    const entities = response.items.map(pkg => this.mapToBusinessObject(pkg));

                    return {
                        entities,
                        totalCount: response.numItems,
                    };
                }),
            );
        }
    }

    public mapToBusinessObject(pkg: Package): DevToolPackageBO {
        return {
            ...pkg,
            [BO_ID]: pkg.name,
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: pkg.name,
        };
    }
}
