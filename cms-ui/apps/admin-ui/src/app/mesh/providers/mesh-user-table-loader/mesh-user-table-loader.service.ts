import { EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { EntityManagerService } from '@admin-ui/core';
import { BaseTableLoaderService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { User } from '@gentics/mesh-models';
import { Observable, from } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshUserBO } from '../../common';
import { MeshUserHandlerService } from '../mesh-user-handler/mesh-user-handler.service';

@Injectable()
export class MeshUserTableLoaderService extends BaseTableLoaderService<User, MeshUserBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected handler: MeshUserHandlerService,
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

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<MeshUserBO>> {
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
