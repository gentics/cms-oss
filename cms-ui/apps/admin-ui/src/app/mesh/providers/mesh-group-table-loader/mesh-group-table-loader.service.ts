import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Group, GroupResponse, Permission } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { Observable, from } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshGroupBO } from '../../common';

@Injectable()
export class MeshGroupTableLoaderService extends BaseTableLoaderService<Group, MeshGroupBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: MeshRestClientService,
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
        return this.api.roles.delete(String(entityId)).then(() => {});
    }

    protected loadEntities(options: TableLoadOptions, additionalOptions?: never): Observable<EntityPageResponse<MeshGroupBO>> {
        return from(this.api.groups.list({
            page: options.page + 1,
            perPage: options.perPage,
            order: options.sortOrder?.toUpperCase?.() as any,
            sortBy: options.sortBy,
        })).pipe(
            map(res => {
                const entities: MeshGroupBO[] = res.data.map(group => ({
                    ...group,
                    [BO_ID]: group.uuid,
                    [BO_DISPLAY_NAME]: group.name,
                    [BO_PERMISSIONS]: this.getPermissions(group),
                }));

                return {
                    entities,
                    // eslint-disable-next-line no-underscore-dangle
                    totalCount: res._metainfo.totalCount,
                };
            }),
        );
    }

    protected getPermissions(group: GroupResponse): Permission[] {
        return Object.entries((group.permissions || {}))
            .filter(([, value]) => value)
            .map(([key]) => key as Permission);
    }
}
