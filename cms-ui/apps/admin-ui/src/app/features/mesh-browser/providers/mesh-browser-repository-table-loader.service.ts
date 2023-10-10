import { ContentRepositoryBO, EntityList, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, ContentRepositoryHandlerService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ContentRepository, ContentRepositoryType } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { filter, map, mergeMap, toArray } from 'rxjs/operators';


export interface ContentRepositoryTableLoaderOptions {
    packageName?: string;
}

@Injectable()
export class MeshBrowserContentRepositoryTableLoaderService
    extends BaseTableLoaderService<ContentRepository, ContentRepositoryBO, ContentRepositoryTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected handler: ContentRepositoryHandlerService,
    ) {
        super('contentRepository', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(false);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return Promise.reject();
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions: ContentRepositoryTableLoaderOptions,
    ): Observable<EntityPageResponse<ContentRepositoryBO>> {
        const loadOptions = this.createDefaultOptions(options);
        const loader: Observable<EntityList<ContentRepositoryBO>> = this.handler.listMapped(null as never, loadOptions);

        return loader.pipe(
            mergeMap(response => response.items),
            filter(contentRepository => contentRepository.crType === ContentRepositoryType.MESH),
            toArray(),
            map(contentRepository => {
                return {
                    entities: contentRepository ,
                    totalCount: contentRepository.length,
                };
            }),
        )
    }

}
