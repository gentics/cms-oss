import { EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Project } from '@gentics/mesh-models';
import { Observable, from } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshProjectBO } from '../../common';
import { ProjectHandlerService } from '../project-handler/project-handler.service';

@Injectable()
export class ProjectTableLoaderService extends BaseTableLoaderService<Project, MeshProjectBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected handler: ProjectHandlerService,
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

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.handler.delete(entityId as any);
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<MeshProjectBO>> {
        return from(this.handler.listMapped({
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
