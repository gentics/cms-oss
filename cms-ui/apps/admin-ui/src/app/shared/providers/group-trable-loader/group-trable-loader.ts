import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, GroupBO, PermissionsCategorizer } from '@admin-ui/common';
import { BaseTrableLoaderService } from '../../providers/base-trable-loader/base-trable-loader.service';
import { Injectable } from '@angular/core';
import { AccessControlledType, Group, PermissionInfo, Raw } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { TrableRow } from '@gentics/ui-core';
import { combineLatest, forkJoin, Observable, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { GroupOperations } from '../../../core/providers/operations';

export interface GroupTrableLoaderOptions {
    permissions?: boolean;
    parentId?: number;
    parentType?: AccessControlledType.NODE | AccessControlledType.FOLDER;
    parentName?: string;
    parentHasChildren?: boolean;
    categorizer?: PermissionsCategorizer;
}

@Injectable()
export class GroupTrableLoaderService extends BaseTrableLoaderService<Group, GroupBO, GroupTrableLoaderOptions> {

    constructor(
        protected api: GcmsApi,
        protected operations: GroupOperations,
    ) {
        super();
    }

    protected loadEntityRow(entity: GroupBO, options?: GroupTrableLoaderOptions): Observable<GroupBO> {
        return combineLatest([
            this.api.group.getGroup(entity.id),
            this.operations.getGroupInstancePermissions(entity.id, options.parentType, options.parentId),
        ]).pipe(
            map(([loadRes, permRes]) => this.mapToBusinessObject(loadRes.group, options, permRes)),
        );
    }

    protected loadEntityChildren(parent: GroupBO | null, options?: GroupTrableLoaderOptions): Observable<GroupBO[]> {
        let groupLoader: Observable<Group[]>;

        if (parent?.id) {
            groupLoader = this.api.group.getSubgroups(parent.id).pipe(
                map(res => res.items),
            );
        } else {
            groupLoader = this.api.group.getGroupsTree().pipe(
                map(res => res.groups),
            );
        }

        return groupLoader.pipe(
            switchMap(groups => {
                if (!options?.permissions || !options.parentId) {
                    return of(groups.map(group => this.mapToBusinessObject(group)));
                }

                if (groups.length === 0) {
                    return of([]);
                }

                return forkJoin(groups.map(group => this.operations.getGroupInstancePermissions(group.id, options.parentType, options.parentId).pipe(
                    map(perms => this.mapToBusinessObject(group, options, perms)),
                )));
            }),
        );
    }

    protected override mapToTrableRow(
        entity: GroupBO,
        parent?: TrableRow<GroupBO>,
        options?: GroupTrableLoaderOptions,
    ): TrableRow<GroupBO> {
        const mapped = super.mapToTrableRow(entity, parent, options);

        mapped.children = (entity.children || []).map(child => this.mapToTrableRow(child as any, mapped));

        return mapped;
    }

    public mapToBusinessObject(
        group: Group<Raw>,
        context?: GroupTrableLoaderOptions,
        permissions?: PermissionInfo[],
    ): GroupBO {
        const mapped: GroupBO = {
            ...group,
            [BO_ID]: String(group.id),
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: group.name,
        };

        if (context && permissions) {
            mapped.permissionSet = {
                type: context.parentType,
                id: context.parentId,
                channelId: null,
                perms: permissions,
                label: context.parentName,
                children: context.parentHasChildren,
                editable: true,
            };
            mapped.categorizedPerms = context.categorizer.categorizePermissions(permissions);
        }

        return mapped;
    }
}
