import { EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { EntityManagerService } from '@admin-ui/core';
import { BaseTableLoaderService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { TagFamily } from '@gentics/mesh-models';
import { Observable, from } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshTagFamilyBO } from '../../common';
import { TagFamilyHandlerService } from '../tag-family-handler/tag-family-handler.service';

export interface TagFamilyTableLoaderOptions {
    project: string;
}

@Injectable()
export class TagFamilyTableLoaderService extends BaseTableLoaderService<TagFamily, MeshTagFamilyBO, TagFamilyTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected handler: TagFamilyHandlerService,
    ) {
        super(
            null,
            entityManager,
            appState,
        );
    }

    public canDelete(): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number, additionalOptions?: TagFamilyTableLoaderOptions): Promise<void> {
        return this.handler.delete(additionalOptions.project, entityId as any);
    }

    protected loadEntities(options: TableLoadOptions, additionalOptions?: TagFamilyTableLoaderOptions): Observable<EntityPageResponse<MeshTagFamilyBO>> {
        return from(this.handler.listWithTags(additionalOptions.project, {
            page: Math.max(options.page, 1),
            perPage: options.perPage,
            // FIXME: Not supported/working currently in Mesh
            // order: options.sortOrder == null ? null : (options.sortOrder === TableSortOrder.ASCENDING ? 'ASCENDING' : 'DESCENDING') as any,
            // sortBy: options.sortBy,
        })).pipe(
            map((res) => ({
                entities: res.data,
                // eslint-disable-next-line no-underscore-dangle
                totalCount: res._metainfo.totalCount,
            })),
        );
    }
}
