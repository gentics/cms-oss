import { EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { EntityManagerService } from '@admin-ui/core';
import { BaseTableLoaderService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Tag, TagFamilyReference } from '@gentics/mesh-models';
import { Observable, from } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshTagBO } from '../../common';
import { TagHandlerService } from '../tag-handler/tag-handler.service';

export interface TagTableLoaderOptions {
    project: string;
    family: TagFamilyReference;
}

@Injectable()
export class TagTableLoaderService extends BaseTableLoaderService<Tag, MeshTagBO, TagTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected handler: TagHandlerService,
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

    public deleteEntity(entityId: string | number, additionalOptions?: TagTableLoaderOptions): Promise<void> {
        return this.handler.delete(additionalOptions.project, additionalOptions.family.uuid, entityId as any);
    }

    protected loadEntities(options: TableLoadOptions, additionalOptions?: TagTableLoaderOptions): Observable<EntityPageResponse<MeshTagBO>> {
        return from(this.handler.listMapped(additionalOptions.project, additionalOptions.family.uuid, {
            page: Math.max(options.page, 1),
            perPage: options.perPage,
            order: options.sortOrder?.toLowerCase?.() as any,
            sortBy: options.sortBy,
        })).pipe(
            map((res) => ({
                entities: res.data,
                // eslint-disable-next-line no-underscore-dangle
                totalCount: res._metainfo.totalCount,
            })),
        );
    }
}
