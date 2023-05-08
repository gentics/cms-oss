import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, ContentRepositoryFragmentBO, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ContentRepositoryFragment, ContentRepositoryFragmentListResponse } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../entity-manager';
import { ContentRepositoryFragmentOperations } from '../operations';

export interface CRFragmentTableLoaderOptions {
    packageName?: string;
}

@Injectable()
export class CRFragmentTableLoaderService
    extends BaseTableLoaderService<ContentRepositoryFragment, ContentRepositoryFragmentBO, CRFragmentTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: ContentRepositoryFragmentOperations,
    ) {
        super('contentRepositoryFragment', entityManager, appState);
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.operations.delete(entityId).toPromise();
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: CRFragmentTableLoaderOptions,
    ): Observable<EntityPageResponse<ContentRepositoryFragmentBO>> {
        const loadOptions = this.createDefaultOptions(options);
        let loader: Observable<ContentRepositoryFragmentListResponse>;

        if (additionalOptions?.packageName) {
            loader = this.api.devTools.getContentRepositoryFragments(additionalOptions.packageName, loadOptions);
        } else {
            loader = this.api.contentRepositoryFragments.getContentRepositoryFragments(loadOptions);
        }

        return loader.pipe(
            map(response => {
                const entities = response.items.map(fragment => this.mapToBusinessObject(fragment));

                return  {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public mapToBusinessObject(fragment: ContentRepositoryFragment): ContentRepositoryFragmentBO {
        return {
            ...fragment,
            [BO_ID]: String(fragment.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: fragment.name,
        };
    }
}
