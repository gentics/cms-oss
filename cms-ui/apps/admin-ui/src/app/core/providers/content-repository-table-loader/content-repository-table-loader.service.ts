import { ContentRepositoryBO, discard, EntityList, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ContentRepository } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { ContentRepositoryHandlerService } from '../content-repository-handler/content-repository-handler.service';
import { EntityManagerService } from '../entity-manager';

export interface ContentRepositoryTableLoaderOptions {
    packageName?: string;
}

@Injectable()
export class ContentRepositoryTableLoaderService
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
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.handler.delete(entityId).pipe(discard()).toPromise();
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions: ContentRepositoryTableLoaderOptions,
    ): Observable<EntityPageResponse<ContentRepositoryBO>> {
        const loadOptions = this.createDefaultOptions(options);
        let loader: Observable<EntityList<ContentRepositoryBO>>;

        if (additionalOptions?.packageName) {
            loader = this.handler.listFromDevtoolMapped(additionalOptions.packageName, null as never, loadOptions);
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
        )
    }
}
