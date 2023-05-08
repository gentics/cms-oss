import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, ContentPackageBO, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, ContentPackageOperations, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ContentPackage, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class ContentPackageTableLoaderService extends BaseTableLoaderService<ContentPackage<Raw>, ContentPackageBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: ContentPackageOperations,
    ) {
        super('construct', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string): Promise<void> {
        return this.operations.delete(entityId, true).toPromise();
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<ContentPackageBO>> {
        const loadOptions = this.createDefaultOptions(options);


        return this.api.contentStaging.listContentPackages(loadOptions).pipe(
            map(response => {
                const entities = response.items.map(pkg => this.mapToBusinessObject(pkg));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(pkg: ContentPackage): ContentPackageBO {
        return {
            ...pkg,
            [BO_ID]: pkg.name,
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: pkg.name,
        };
    }
}
