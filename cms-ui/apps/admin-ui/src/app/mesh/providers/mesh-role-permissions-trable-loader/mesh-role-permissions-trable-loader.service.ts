import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { BaseTrableLoaderService } from '@admin-ui/core/providers/base-trable-loader/base-trable-loader.service';
import {
    BASIC_ENTITY_PERMISSIONS,
    MBO_AVILABLE_PERMISSIONS,
    MBO_PERMISSION_PATH,
    MBO_PROJECT_CONTEXT,
    MBO_ROLE_PERMISSIONS,
    MBO_TYPE,
    MeshBusinessObject,
    MeshProjectBO,
    MeshTagFamilyBO,
    MeshType,
    NODE_PERMISSIONS,
} from '@admin-ui/mesh/common';
import { toPermissionArray } from '@admin-ui/mesh/utils';
import { Injectable } from '@angular/core';
import { NodeResponse, Permission } from '@gentics/mesh-models';
import { TrableRow } from '@gentics/ui-core';
import { Observable, forkJoin, from, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshGroupHandlerService } from '../mesh-group-handler/mesh-group-handler.service';
import { MeshRoleHandlerService } from '../mesh-role-handler/mesh-role-handler.service';
import { MeshRolePermissionHandlerService } from '../mesh-role-permission-handler/mesh-role-permission-handler.service';
import { MeshUserHandlerService } from '../mesh-user-handler/mesh-user-handler.service';
import { MicroschemaHandlerService } from '../microschema-handler/microschema-handler.service';
import { ProjectHandlerService } from '../project-handler/project-handler.service';
import { SchemaHandlerService } from '../schema-handler/schema-handler.service';
import { TagFamilyHandlerService } from '../tag-family-handler/tag-family-handler.service';
import { TagHandlerService } from '../tag-handler/tag-handler.service';

export const PROJECTS_ROOT_ID = '_projects';
export const SCHEMAS_ROOT_ID = '_schemas';
export const MICROSCHEMA_ROOT_ID = '_microschemas';
export const USERS_ROOT_ID = '_users';
export const GROUPS_ROOT_ID = '_groups';
export const ROLES_ROOT_ID = '_roles';

export interface MeshRolePermissionsTrableLoaderOptions {
    role: string;
}

function createRoot(): MeshBusinessObject[] {
    return [
        {
            [BO_ID]: PROJECTS_ROOT_ID,
            [BO_DISPLAY_NAME]: 'mesh.projects',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.PROJECT,
            [MBO_AVILABLE_PERMISSIONS]: BASIC_ENTITY_PERMISSIONS,
            [MBO_PERMISSION_PATH]: 'projects',
        },
        {
            [BO_ID]: SCHEMAS_ROOT_ID,
            [BO_DISPLAY_NAME]: 'mesh.schemas',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.SCHEMA,
            [MBO_AVILABLE_PERMISSIONS]: BASIC_ENTITY_PERMISSIONS,
            [MBO_PERMISSION_PATH]: 'schemas',
        },
        {
            [BO_ID]: MICROSCHEMA_ROOT_ID,
            [BO_DISPLAY_NAME]: 'mesh.microschemas',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.MICROSCHEMA,
            [MBO_AVILABLE_PERMISSIONS]: BASIC_ENTITY_PERMISSIONS,
            [MBO_PERMISSION_PATH]: 'microschemas',
        },
        {
            [BO_ID]: USERS_ROOT_ID,
            [BO_DISPLAY_NAME]: 'mesh.users',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.USER,
            [MBO_AVILABLE_PERMISSIONS]: BASIC_ENTITY_PERMISSIONS,
            [MBO_PERMISSION_PATH]: 'users',
        },
        {
            [BO_ID]: GROUPS_ROOT_ID,
            [BO_DISPLAY_NAME]: 'mesh.groups',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.GROUP,
            [MBO_AVILABLE_PERMISSIONS]: BASIC_ENTITY_PERMISSIONS,
            [MBO_PERMISSION_PATH]: 'groups',
        },
        {
            [BO_ID]: ROLES_ROOT_ID,
            [BO_DISPLAY_NAME]: 'mesh.roles',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.ROLE,
            [MBO_AVILABLE_PERMISSIONS]: BASIC_ENTITY_PERMISSIONS,
            [MBO_PERMISSION_PATH]: 'roles',
        },
    ];
}

function createProjectRoot(project: MeshProjectBO): MeshBusinessObject[] {
    return [
        {
            [BO_ID]: `_project-${project.name}_tag-families`,
            [BO_DISPLAY_NAME]: 'mesh.tag_families',
            [BO_PERMISSIONS]: [],
            [MBO_TYPE]: MeshType.TAG_FAMILY,
            [MBO_AVILABLE_PERMISSIONS]: BASIC_ENTITY_PERMISSIONS,
            [MBO_PERMISSION_PATH]: `${project.name}/tagFamilies`,
            [MBO_PROJECT_CONTEXT]: project.name,
        }, {
            [BO_ID]: `_project-${project.name}_nodes`,
            [BO_DISPLAY_NAME]: 'mesh.nodes',
            [BO_PERMISSIONS]: NODE_PERMISSIONS,
            [MBO_TYPE]: MeshType.NODE,
            [MBO_AVILABLE_PERMISSIONS]: [Permission.CREATE, Permission.READ, Permission.UPDATE, Permission.PUBLISH, Permission.READ_PUBLISHED],
            [MBO_PERMISSION_PATH]: `${project.name}/nodes`,
            [MBO_PROJECT_CONTEXT]: project.name,
        },
    ];
}

@Injectable()
export class MeshRolePermissionsTrableLoaderService
    extends BaseTrableLoaderService<MeshBusinessObject, MeshBusinessObject, MeshRolePermissionsTrableLoaderOptions> {

    constructor(
        protected i18n: I18nService,
        protected groupHandler: MeshGroupHandlerService,
        protected roleHandler: MeshRoleHandlerService,
        protected userHandler: MeshUserHandlerService,
        protected projectHandler: ProjectHandlerService,
        protected schemaHandler: SchemaHandlerService,
        protected microHandler: MicroschemaHandlerService,
        protected tagFamilyHandler: TagFamilyHandlerService,
        protected tagHandler: TagHandlerService,
        protected permissions: MeshRolePermissionHandlerService,
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
                return this.loadEntityRow(entry, options)
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

            case MeshType.PROJECT:
                if (parent[BO_ID][0] === '_') {
                    return from(this.projectHandler.listMapped({ role: options.role })).pipe(
                        map(page => page.data),
                    );
                }
                return this.loadProjectRootElements(parent as MeshProjectBO, options);
                break;

            case MeshType.SCHEMA:
                return from(this.schemaHandler.listMapped({ role: options.role })).pipe(
                    map(page => page.data),
                );

            case MeshType.MICROSCHEMA:
                return from(this.microHandler.listMapped({ role: options.role })).pipe(
                    map(page => page.data),
                );

            case MeshType.TAG_FAMILY:
                if (parent[BO_ID][0] === '_') {
                    return from(this.tagFamilyHandler.listWithTags(parent[MBO_PROJECT_CONTEXT], { role: options.role })).pipe(
                        map(page => page.data),
                    );
                }

                return from(this.tagHandler.listMapped(
                    parent[MBO_PROJECT_CONTEXT],
                    (parent as MeshTagFamilyBO).uuid,
                    { role: options.role },
                )).pipe(
                    map(page => page.data),
                );

            default:
                return of([]);
        }
    }

    protected loadEntityRow(entity: MeshBusinessObject, options?: MeshRolePermissionsTrableLoaderOptions): Observable<MeshBusinessObject> {
        const clone = { ...entity };
        return from(this.permissions.get(options.role, entity[MBO_PERMISSION_PATH])).pipe(
            map(res => {
                clone[MBO_ROLE_PERMISSIONS] = toPermissionArray(res);
                return clone;
            }),
        );
    }

    protected override hasChildren(entity: MeshBusinessObject, _options?: MeshRolePermissionsTrableLoaderOptions): boolean {
        // Root elements are marked with a leading underscore, and always have children
        if (entity[BO_ID] == null || entity[BO_ID][0] === '_') {
            return true;
        }

        switch (entity[MBO_TYPE]) {
            case MeshType.PROJECT:
                return true;

            case MeshType.NODE:
                return Object.values((entity as any as NodeResponse).childrenInfo).some(info => info.count > 0);

            case MeshType.TAG_FAMILY:
                return (entity as MeshTagFamilyBO).tags?.length > 0;

            default:
                return false;
        }
    }

    protected override mapToTrableRow(
        entity: MeshBusinessObject,
        parent?: TrableRow<MeshBusinessObject>,
        options?: MeshRolePermissionsTrableLoaderOptions,
    ): TrableRow<MeshBusinessObject> {
        const row = super.mapToTrableRow(entity, parent, options);

        switch (entity[MBO_TYPE]) {
            case MeshType.NODE:
                break;

            default:
                row.loaded = !row.hasChildren;
                break;
        }

        row.hash = (entity[MBO_AVILABLE_PERMISSIONS] || []).join(',') + ':' + (entity[MBO_ROLE_PERMISSIONS] || []).join(',');

        return row;
    }

    protected loadProjectRootElements(project: MeshProjectBO, options: MeshRolePermissionsTrableLoaderOptions): Observable<MeshBusinessObject[]> {
        return forkJoin(createProjectRoot(project)
            .map(entry => {
                entry[BO_DISPLAY_NAME] = this.i18n.instant(entry[BO_DISPLAY_NAME]);
                return this.loadEntityRow(entry, options);
            }),
        );
    }
}
