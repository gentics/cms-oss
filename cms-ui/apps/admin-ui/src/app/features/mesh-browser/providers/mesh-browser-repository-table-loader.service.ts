import { ContentRepositoryBO, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import {
    BaseTableLoaderService,
    ContentRepositoryHandlerService,
    EntityManagerService,
} from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ContentRepository, ContentRepositoryType } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

export interface MeshContentRepositoryTableLoaderOptions {
    crType: ContentRepositoryType;
}

@Injectable()
export class MeshBrowserContentRepositoryTableLoaderService
    extends BaseTableLoaderService<ContentRepository, ContentRepositoryBO, MeshContentRepositoryTableLoaderOptions> {

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
        additionalOptions: MeshContentRepositoryTableLoaderOptions,
    ): Observable<EntityPageResponse<ContentRepositoryBO>> {
        const loadOptions = this.createDefaultOptions(options);
        // const filter = (loadOptions.q || '').toLocaleLowerCase();
        loadOptions.q = ContentRepositoryType.MESH;

        return this.handler.listMapped(null as never, loadOptions).pipe(
            map(response => {
                return {
                    entities: response.items,
                    totalCount: response.totalItems,
                };
            }),
        );
    }
}
