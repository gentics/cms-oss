import { Injectable } from '@angular/core';
import { UserTokenData } from '@gentics/mesh-models';
import { from, map, Observable } from 'rxjs';
import { EntityPageResponse, TableLoadOptions } from '../../../common';
import { BaseTableLoaderService, EntityManagerService } from '../../../core';
import { AppStateService } from '../../../state';
import { MeshUserTokenBO } from '../../common';
import { MeshUserHandlerService } from '../mesh-user-handler/mesh-user-handler.service';

interface MeshUserTokenTableLoaderOptions {
    user: string;
}

@Injectable()
export class MeshUserTokenTableLoaderService extends BaseTableLoaderService<UserTokenData, MeshUserTokenBO, MeshUserTokenTableLoaderOptions> {

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

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number, additionalOptions?: MeshUserTokenTableLoaderOptions): Promise<void> {
        return this.handler.deleteToken(additionalOptions.user, entityId as any);
    }

    protected loadEntities(options: TableLoadOptions, additionalOptions?: MeshUserTokenTableLoaderOptions): Observable<EntityPageResponse<MeshUserTokenBO>> {
        return from(this.handler.listTokens(additionalOptions.user, {
            page: Math.max(options.page, 1),
            perPage: options.perPage,
            // order: options.sortOrder?.toLowerCase?.() as any,
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
