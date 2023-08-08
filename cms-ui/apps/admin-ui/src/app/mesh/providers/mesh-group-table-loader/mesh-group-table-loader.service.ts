import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Group, GroupResponse, Permission } from '@gentics/mesh-models';
import { Observable, forkJoin, from, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { toPermissionArray } from '@admin-ui/mesh/utils';
import { MBO_PERMISSION_PATH, MBO_TYPE, MeshGroupBO, MeshType } from '../../common';
import { MeshGroupHandlerService } from '../mesh-group-handler/mesh-group-handler.service';

export interface MeshGroupTableLoaderOptions {
    users?: boolean;
}

@Injectable()
export class MeshGroupTableLoaderService extends BaseTableLoaderService<Group, MeshGroupBO, MeshGroupTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected handler: MeshGroupHandlerService,
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

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.handler.delete(entityId as any);
    }

    protected loadEntities(options: TableLoadOptions, additionalOptions?: MeshGroupTableLoaderOptions): Observable<EntityPageResponse<MeshGroupBO>> {
        return from(this.handler.list({
            page: options.page + 1,
            perPage: options.perPage,
            order: options.sortOrder?.toLowerCase?.() as any,
            sortBy: options.sortBy,
        })).pipe(
            map((res) => {
                const entities: MeshGroupBO[] = res.data.map(group => ({
                    ...group,
                    [BO_ID]: group.uuid,
                    [BO_DISPLAY_NAME]: group.name,
                    [BO_PERMISSIONS]: toPermissionArray(group.permissions),
                    [MBO_PERMISSION_PATH]: `groups/${group.uuid}`,
                    [MBO_TYPE]: MeshType.GROUP,
                }));

                return {
                    entities,
                    // eslint-disable-next-line no-underscore-dangle
                    totalCount: res._metainfo.totalCount,
                };
            }),
            switchMap(page => {
                if (!(additionalOptions?.users ?? false)) {
                    return of(page);
                }

                return forkJoin(page.entities.map(group => {
                    return from(this.handler.getUsers(group.uuid).then(userRes => {
                        group.users = userRes.data;
                        return group;
                    }));
                })).pipe(
                    map(mapped => {
                        return {
                            ...page,
                            entities: mapped,
                        };
                    }),
                );
            }),
        );
    }
}
