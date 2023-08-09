import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { BaseTrableLoaderService } from '@admin-ui/core/providers/base-trable-loader/base-trable-loader.service';
import {
    MBO_AVILABLE_PERMISSIONS,
    MBO_PERMISSION_PATH,
    MBO_ROLE_PERMISSIONS,
    MBO_TYPE,
    MeshBusinessObject,
    MeshType,
} from '@admin-ui/mesh/common';
import { toPermissionArray } from '@admin-ui/mesh/utils';
import { Injectable } from '@angular/core';
import { NodeResponse, Permission } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { TrableRow } from '@gentics/ui-core';
import { Observable, forkJoin, from, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshGroupHandlerService } from '../mesh-group-handler/mesh-group-handler.service';
import { MeshRoleHandlerService } from '../mesh-role-handler/mesh-role-handler.service';
import { MeshUserHandlerService } from '../mesh-user-handler/mesh-user-handler.service';

export interface MeshRolePermissionsTrableLoaderOptions {
    role: string;
}

function createRoot(): MeshBusinessObject[] {
    return [
        {
            [BO_ID]: 'projects',
            [BO_DISPLAY_NAME]: 'mesh.projects',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.PROJECT,
            [MBO_AVILABLE_PERMISSIONS]: [Permission.CREATE],
            [MBO_PERMISSION_PATH]: 'projects',
        },
        {
            [BO_ID]: 'schemas',
            [BO_DISPLAY_NAME]: 'mesh.schemas',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.SCHEMA,
            [MBO_AVILABLE_PERMISSIONS]: [Permission.CREATE],
            [MBO_PERMISSION_PATH]: 'schemas',
        },
        {
            [BO_ID]: 'microschemas',
            [BO_DISPLAY_NAME]: 'mesh.microschemas',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.MICROSCHEMA,
            [MBO_AVILABLE_PERMISSIONS]: [Permission.CREATE],
            [MBO_PERMISSION_PATH]: 'microschemas',
        },
        {
            [BO_ID]: 'users',
            [BO_DISPLAY_NAME]: 'mesh.users',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.USER,
            [MBO_AVILABLE_PERMISSIONS]: [Permission.CREATE],
            [MBO_PERMISSION_PATH]: 'users',
        },
        {
            [BO_ID]: 'groups',
            [BO_DISPLAY_NAME]: 'mesh.groups',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.GROUP,
            [MBO_AVILABLE_PERMISSIONS]: [Permission.CREATE],
            [MBO_PERMISSION_PATH]: 'groups',
        },
        {
            [BO_ID]: 'roles',
            [BO_DISPLAY_NAME]: 'mesh.roles',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.ROLE,
            [MBO_AVILABLE_PERMISSIONS]: [Permission.CREATE],
            [MBO_PERMISSION_PATH]: 'roles',
        },
    ];
}

@Injectable()
export class MeshRolePermissionsTrableLoaderService
    extends BaseTrableLoaderService<MeshBusinessObject, MeshBusinessObject, MeshRolePermissionsTrableLoaderOptions> {

    constructor(
        protected mesh: MeshRestClientService,
        protected i18n: I18nService,
        protected groupHandler: MeshGroupHandlerService,
        protected roleHandler: MeshRoleHandlerService,
        protected userHandler: MeshUserHandlerService,
    ) {
        super();
    }

    protected loadEntityChildren(parent: MeshBusinessObject | null, options?: MeshRolePermissionsTrableLoaderOptions): Observable<MeshBusinessObject[]> {
        if (options == null || options.role == null) {
            return of([]);
        }

        if (parent == null) {
            return forkJoin(createRoot().map(entry => {
                entry[BO_DISPLAY_NAME] = this.i18n.instant(entry[BO_DISPLAY_NAME]);
                return this.loadPermissions(entry, options)
            }));
        }

        switch (parent[MBO_TYPE]) {
            case MeshType.USER:
                return from(this.userHandler.listMapped({ role: options.role })).pipe(
                    map(page => page.data),
                );

            case MeshType.GROUP:
                return from(this.groupHandler.listMapped({ role: options.role })).pipe(
                    map(page => page.data),
                );

            case MeshType.ROLE:
                return from(this.roleHandler.listMapped({ role: options.role })).pipe(
                    map(page => page.data),
                );

            default:
                return of([]);
        }
    }

    protected loadEntityRow(entity: MeshBusinessObject, _options?: MeshRolePermissionsTrableLoaderOptions): Observable<MeshBusinessObject> {
        return of(null);
    }

    protected override hasChildren(entity: MeshBusinessObject, _options?: MeshRolePermissionsTrableLoaderOptions): boolean {
        // Root elements always have children
        if (entity[BO_ID] == null || entity[BO_ID].length !== 32) {
            return true;
        }

        // Nodes may have children
        if (entity[MBO_TYPE] === MeshType.NODE) {
            return Object.values((entity as any as NodeResponse).childrenInfo).some(info => info.count > 0);
        }

        return false;
    }

    protected override mapToTrableRow(
        entity: MeshBusinessObject,
        parent?: TrableRow<MeshBusinessObject>,
        options?: MeshRolePermissionsTrableLoaderOptions,
    ): TrableRow<MeshBusinessObject> {
        const row = super.mapToTrableRow(entity, parent, options);
        if (entity[MBO_TYPE] !== MeshType.NODE) {
            row.loaded = !row.hasChildren;
        }
        return row;
    }

    protected loadPermissions(mbo: MeshBusinessObject, options: MeshRolePermissionsTrableLoaderOptions): Observable<MeshBusinessObject> {
        return from(this.mesh.permissions.get(options.role, mbo[MBO_PERMISSION_PATH]).then(perms => {
            mbo[MBO_ROLE_PERMISSIONS] = toPermissionArray(perms);
            return mbo;
        }));
    }
}
