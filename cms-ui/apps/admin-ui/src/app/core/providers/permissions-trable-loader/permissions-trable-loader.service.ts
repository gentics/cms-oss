import { Injectable } from '@angular/core';
import { AccessControlledType, Group, GroupPermissionsListOptions, GroupTypeOrInstancePermissionsResponse, PermissionsSet } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { TrableRow } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS, PermissionsCategorizer, PermissionsSetBO } from '../../../common';
import { BaseTrableLoaderService } from '../base-trable-loader/base-trable-loader.service';

export interface PermissionsTrableLoaderOptions {
    group: Group;
    parentId?: number;
    parentType?: AccessControlledType;
    excludedParentType?: AccessControlledType;
    channelId?: number;
    parentName?: string;
    parentHasChildren?: boolean;
    categorizer?: PermissionsCategorizer;
}

@Injectable()
export class PermissionsTrableLoaderService extends BaseTrableLoaderService<PermissionsSet, PermissionsSetBO, PermissionsTrableLoaderOptions> {

    constructor(
        protected client: GCMSRestClientService,
    ) {
        super();
    }

    protected loadEntityRow(entity: PermissionsSetBO, options?: PermissionsTrableLoaderOptions): Observable<PermissionsSetBO> {
        let loader: Observable<GroupTypeOrInstancePermissionsResponse>;
        if (entity.id) {
            loader = this.client.group.getInstancePermission(options.group.id, entity.type, entity.id);
        } else {
            loader = this.client.group.getPermission(options.group.id, entity.type);
        }

        return loader.pipe(
            map((res) => {
                entity.perms = res.perms;
                entity.categorized = options.categorizer.categorizePermissions(res.perms);
                entity.roles = res.roles;
                return entity;
            }),
        );
    }

    protected loadEntityChildren(parent: PermissionsSetBO | null, options?: PermissionsTrableLoaderOptions): Observable<PermissionsSetBO[]> {
        const loadOptions: GroupPermissionsListOptions = {};
        if (!parent) {
            if (options.parentType) {
                loadOptions.parentType = options.parentType;

                if (options.parentId) {
                    loadOptions.parentId = options.parentId;
                }
            }

            if (options.excludedParentType) {
                loadOptions.excludedParentType = options.excludedParentType;
            }

            if (options.channelId) {
                loadOptions.channelId = options.channelId;
            }
        } else {
            loadOptions.parentType = parent.type;

            if (options.excludedParentType) {
                loadOptions.excludedParentType = options.excludedParentType;
            }

            if (parent.id) {
                loadOptions.parentId = parent.id;
            }
            if (parent.channelId) {
                loadOptions.channelId = parent.channelId;
            }
        }

        return this.client.group.listPermissions(options.group.id, loadOptions).pipe(
            map((res) => res.items.map((perm) => this.mapToBusinessObject(perm, options))),
        );
    }

    public override createRowHash(entity: PermissionsSetBO): string | null {
        return new Date().toISOString();
    }

    protected override mapToTrableRow(
        entity: PermissionsSetBO,
        parent?: TrableRow<PermissionsSetBO>,
        options?: PermissionsTrableLoaderOptions,
    ): TrableRow<PermissionsSetBO> {
        const row = super.mapToTrableRow(entity, parent, options);
        row.hasChildren = entity.children;
        row.loaded = !entity.children;

        return row;
    }

    public mapToBusinessObject(perms: PermissionsSet, context: PermissionsTrableLoaderOptions): PermissionsSetBO {
        return {
            ...perms,
            [BO_ID]: `${perms.type}${perms.id ? '_' + perms.id : ''}`,
            [BO_PERMISSIONS]: [],
            [BO_DISPLAY_NAME]: perms.label,
            group: context.group,
            categorized: context.categorizer.categorizePermissions(perms.perms),
        };
    }
}
