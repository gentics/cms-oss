import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, discard, EntityPageResponse, RoleBO, TableLoadOptions } from '@admin-ui/common';
import { BaseTableLoaderService, EntityManagerService, RoleOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Raw, Role } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable()
export class RoleTableLoaderService extends BaseTableLoaderService<Role<Raw>, RoleBO> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: RoleOperations,
    ) {
        super('role', entityManager, appState);
    }

    protected loadEntities(options: TableLoadOptions): Observable<EntityPageResponse<RoleBO>> {
        const loadOptions = this.createDefaultOptions(options);

        return this.api.role.getRoles(loadOptions).pipe(
            map(response => {
                const entities = response.items.map(role => this.mapToBusinessObject(role));

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    public canDelete(entityId: string | number): Promise<boolean> {
        return Promise.resolve(true);
    }

    public deleteEntity(entityId: string | number): Promise<void> {
        return this.operations.delete(entityId).pipe(discard()).toPromise();
    }

    protected mapToBusinessObject(role: Role<Raw>): RoleBO {
        return {
            ...role,
            [BO_ID]: String(role.id),
            [BO_PERMISSIONS]: [],
            // TODO: Fix me
            [BO_DISPLAY_NAME]: role.nameI18n['de'],
        };
    }
}
