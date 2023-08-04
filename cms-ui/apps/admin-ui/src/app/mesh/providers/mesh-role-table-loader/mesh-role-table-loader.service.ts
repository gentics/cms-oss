import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Permission, Role, RoleResponse } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { Observable, from } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshRoleBO } from '../../common';

@Injectable()
export class MeshRoleTableLoaderService extends BaseTableLoaderService<Role, MeshRoleBO> {

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

    protected loadEntities(options: TableLoadOptions, additionalOptions?: never): Observable<EntityPageResponse<MeshRoleBO>> {
        return from(this.api.roles.list({
            page: options.page + 1,
            perPage: options.perPage,
            order: options.sortOrder?.toUpperCase?.() as any,
            sortBy: options.sortBy,
        })).pipe(
            map(res => {
                const entities: MeshRoleBO[] = res.data.map(role => ({
                    ...role,
                    [BO_ID]: role.uuid,
                    [BO_DISPLAY_NAME]: role.name,
                    [BO_PERMISSIONS]: this.getPermissions(role),
                }));

                return {
                    entities,
                    // eslint-disable-next-line no-underscore-dangle
                    totalCount: res._metainfo.totalCount,
                };
            }),
        );
    }

    protected getPermissions(role: RoleResponse): Permission[] {
        return Object.entries((role.permissions || {}))
            .filter(([, value]) => value)
            .map(([key]) => key as Permission);
    }
}
