import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, EntityPageResponse, GroupBO, TableLoadOptions, applyPermissions } from '@admin-ui/common';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Group, GroupListOptions, GroupListResponse, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTableLoaderService } from '../../providers/base-table-loader/base-table-loader.service';
import { EntityManagerService } from '../../../core/providers/entity-manager';
import { GroupOperations } from '../../../core/providers/operations';

export interface LoadUserGroupOptions {
    userId: number;
}

export interface LoadGroupChildrenOptions {
    groupId: number;
}

export type GroupTableLoaderOptions = LoadUserGroupOptions | LoadGroupChildrenOptions;

const isLoadGroupChildrenOptions = (options: GroupTableLoaderOptions): options is LoadGroupChildrenOptions =>
    options != null && typeof options === 'object' && Number.isInteger((options as LoadGroupChildrenOptions).groupId);

@Injectable()
export class GroupTableLoaderService extends BaseTableLoaderService<Group<Raw>, GroupBO, GroupTableLoaderOptions> {

    constructor(
        entityManager: EntityManagerService,
        appState: AppStateService,
        protected api: GcmsApi,
        protected operations: GroupOperations,
    ) {
        super('group', entityManager, appState);
    }

    protected loadEntities(
        options: TableLoadOptions,
        additionalOptions?: GroupTableLoaderOptions,
    ): Observable<EntityPageResponse<GroupBO>> {
        if (isLoadGroupChildrenOptions(additionalOptions)) {
            return this.loadSubGroups(options, additionalOptions);
        }

        return this.loadGroups(options, additionalOptions);
    }

    protected loadGroups(
        options: TableLoadOptions,
        additionalOptions?: LoadUserGroupOptions,
    ): Observable<EntityPageResponse<GroupBO>> {
        const loadOptions: GroupListOptions = {
            ...this.createDefaultOptions(options),
            perms: true,
        };
        let loader: Observable<GroupListResponse>;

        if (additionalOptions?.userId) {
            loader = this.api.user.getUserGroups(additionalOptions.userId, loadOptions);
        } else {
            loader = this.api.group.listGroups(loadOptions);
        }

        return loader.pipe(
            map(response => {
                const entities = response.items.map(group => this.mapToBusinessObject(group));
                applyPermissions(entities, response);

                return {
                    entities,
                    totalCount: response.numItems,
                };
            }),
        );
    }

    protected loadSubGroups(
        options: TableLoadOptions,
        additionalOptions: LoadGroupChildrenOptions,
    ): Observable<EntityPageResponse<GroupBO>> {
        const loadOptions = this.createDefaultOptions(options);

        return this.api.group.getSubgroups(additionalOptions.groupId, loadOptions).pipe(
            map(response => {
                const entities = response.items.map(group => this.mapToBusinessObject(group));

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
        return this.operations.delete(Number(entityId)).toPromise();
    }

    protected mapToBusinessObject(group: Group<Raw>): GroupBO {
        return {
            ...group,
            [BO_ID]: String(group.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: group.name,
        };
    }
}
