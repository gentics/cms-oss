import { EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Role } from '@gentics/mesh-models';
import { Observable, from } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshRoleBO } from '../../common';
import { MeshRoleHandlerService } from '../mesh-role-handler/mesh-role-handler.service';

@Injectable()
export class MeshRoleTableLoaderService extends BaseTableLoaderService<Role, MeshRoleBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected handler: MeshRoleHandlerService,
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

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<MeshRoleBO>> {
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
