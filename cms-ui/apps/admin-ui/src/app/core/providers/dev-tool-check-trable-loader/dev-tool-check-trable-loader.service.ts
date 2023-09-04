import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, PermissionsCategorizer, PermissionsSetBO } from '@admin-ui/common';
import { Injectable } from '@angular/core';
import { AccessControlledType, Group, GroupPermissionsListOptions, PermissionsSet } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { TrableRow } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BaseTrableLoaderService } from '../base-trable-loader/base-trable-loader.service';

export interface PackageCheckLoaderOptions {
    group: Group;
    parentId?: number;
    parentType?: AccessControlledType;
    channelId?: number;
    parentName?: string;
    parentHasChildren?: boolean;
    categorizer?: PermissionsCategorizer;
}

@Injectable()
export class PackageCheckTrableLoaderService extends BaseTrableLoaderService<PermissionsSet, PermissionsSetBO, PackageCheckLoaderOptions> {

    constructor(
        protected api: GcmsApi,
    ) {
        super();
    }

    protected loadEntityRow(entity: PermissionsSetBO, options?: PackageCheckLoaderOptions): Observable<PermissionsSetBO> {
        return this.api.group.getGroupInstancePermissions(options.group.id, entity.type, entity.id).pipe(
            map(res => {
                entity.perms = res.perms;
                entity.categorized = options.categorizer.categorizePermissions(res.perms);
                entity.roles = res.roles;
                return entity;
            }),
        )
    }

    protected loadEntityChildren(parent: PermissionsSetBO | null, options?: PackageCheckLoaderOptions): Observable<PermissionsSetBO[]> {
        const loadOptions: GroupPermissionsListOptions = {};
        if (!parent) {
            if (options.parentType) {
                loadOptions.parentType = options.parentType;

                if (options.parentId) {
                    loadOptions.parentId = options.parentId;
                }
            }
            if (options.channelId) {
                loadOptions.channelId = options.channelId;
            }
        } else {
            loadOptions.parentType = parent.type;
            if (parent.id) {
                loadOptions.parentId = parent.id;
            }
            if (parent.channelId) {
                loadOptions.channelId = parent.channelId;
            }
        }

        return this.api.group.getGroupPermissions(options.group.id, loadOptions).pipe(
            map(res => res.items.map(perm => this.mapToBusinessObject(perm, options))),
        );
    }

    public override createRowHash(entity: PermissionsSetBO): string | null {
        return new Date().toISOString();
    }

    protected override mapToTrableRow(
        entity: PermissionsSetBO,
        parent?: TrableRow<PermissionsSetBO>,
        options?: PackageCheckLoaderOptions,
    ): TrableRow<PermissionsSetBO> {
        const row = super.mapToTrableRow(entity, parent, options);
        row.hasChildren = entity.children;
        row.loaded = !entity.children;

        return row;
    }

    public mapToBusinessObject(perms: PermissionsSet, context: PackageCheckLoaderOptions): PermissionsSetBO {
        return {
            ...perms,
            [BO_ID]: `${perms.type}_${perms.id}`,
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: perms.label,
            group: context.group,
            categorized: context.categorizer.categorizePermissions(perms.perms),
        };
    }
}
