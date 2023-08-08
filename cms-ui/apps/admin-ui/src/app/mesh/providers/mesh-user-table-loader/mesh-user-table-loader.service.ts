import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { getUserName, toPermissionArray } from '@admin-ui/mesh/utils';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Permission, User, UserResponse } from '@gentics/mesh-models';
import { Observable, from } from 'rxjs';
import { map } from 'rxjs/operators';
import { MBO_PERMISSION_PATH, MBO_TYPE, MeshType, MeshUserBO } from '../../common';
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

    public canDelete(_entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.handler.delete(entityId as any);
    }

    protected loadEntities(options: TableLoadOptions, _additionalOptions?: never): Observable<EntityPageResponse<MeshUserBO>> {
        return from(this.handler.list({
            page: options.page + 1,
            perPage: options.perPage,
            order: options.sortOrder?.toLowerCase?.() as any,
            sortBy: options.sortBy,
        })).pipe(
            map(res => {
                const entities: MeshUserBO[] = res.data.map(user => ({
                    ...user,
                    [BO_ID]: user.uuid,
                    [BO_DISPLAY_NAME]: getUserName(user),
                    [BO_PERMISSIONS]: toPermissionArray(user.permissions),
                    [MBO_PERMISSION_PATH]: `users/${user.uuid}`,
                    [MBO_TYPE]: MeshType.USER,
                }));

                return {
                    entities,
                    // eslint-disable-next-line no-underscore-dangle
                    totalCount: res._metainfo.totalCount,
                };
            }),
        );
    }
}
