import { applyPermissions, BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, discard, EntityPageResponse, TableLoadOptions, UserBO } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Raw, User, UserListOptions, UserListResponse } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../entity-manager';
import { UserOperations } from '../operations';

export interface UserTableLoaderOptions {
    groupId?: number;
}

@Injectable()
export class UserTableLoaderService extends BaseTableLoaderService<User<Raw>, UserBO, UserTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: UserOperations,
    ) {
        super('user', entityManager, appState);
    }

    protected loadEntities(options: TableLoadOptions, additionalOptions?: UserTableLoaderOptions): Observable<EntityPageResponse<UserBO>> {
        const loadOptions: UserListOptions = {
            ...this.createDefaultOptions(options),
            perms: true,
            embed: 'group',
        };
        let loader: Observable<UserListResponse>;

        if (additionalOptions?.groupId) {
            loader = this.api.group.getGroupUsers(additionalOptions.groupId, loadOptions);
        } else {
            loader = this.api.user.getUsers(loadOptions);
        }

        return loader.pipe(
            map(response => {
                const entities = response.items.map(user => this.mapToBusinessObject(user));
                applyPermissions(entities, response);

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
        return this.operations.delete(Number(entityId)).pipe(discard()).toPromise();
    }

    protected mapToBusinessObject(user: User<Raw>): UserBO {
        return {
            [BO_ID]: String(user.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: user.login,
            ...user,
        };
    }
}
