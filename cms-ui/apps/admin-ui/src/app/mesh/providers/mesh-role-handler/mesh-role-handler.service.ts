import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS } from '@admin-ui/common';
import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import {
    EDITABLE_ENTITY_PERMISSIONS,
    MBO_AVILABLE_PERMISSIONS,
    MBO_PERMISSION_PATH,
    MBO_ROLE_PERMISSIONS,
    MBO_TYPE,
    MeshRoleBO,
    MeshType,
} from '@admin-ui/mesh/common';
import { toPermissionArray } from '@admin-ui/mesh/utils';
import { Injectable } from '@angular/core';
import { ListResponse, RoleCreateRequest, RoleListOptions, RoleListResponse, RoleLoadOptions, RoleResponse, RoleUpdateRequest } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseMeshEntitiyHandlerService } from '../base-mesh-entity-handler/base-mesh-entity-handler.service';

@Injectable()
export class MeshRoleHandlerService extends BaseMeshEntitiyHandlerService {

    constructor(
        errorHandler: ErrorHandler,
        notification: I18nNotificationService,
        protected mesh: MeshRestClientService,
    ) {
        super(errorHandler, notification);
    }

    public mapToBusinessObject(
        role: RoleResponse,
        _index?: number,
    ): MeshRoleBO {
        return {
            ...role,
            [BO_ID]: role.uuid,
            [BO_PERMISSIONS]: toPermissionArray(role.permissions),
            [BO_DISPLAY_NAME]: role.name,
            [MBO_TYPE]: MeshType.ROLE,
            [MBO_AVILABLE_PERMISSIONS]: EDITABLE_ENTITY_PERMISSIONS,
            [MBO_ROLE_PERMISSIONS]: toPermissionArray(role.rolePerms),
            [MBO_PERMISSION_PATH]: `roles/${role.uuid}`,
        };
    }

    public async get(uuid: string, params?: RoleLoadOptions): Promise<RoleResponse> {
        try {
            const res = await this.mesh.roles.get(uuid, params);
            this.nameMap[res.uuid] = res.name;
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public getMapped(uuid: string, params?: RoleLoadOptions): Promise<MeshRoleBO> {
        return this.get(uuid, params).then(role => this.mapToBusinessObject(role));
    }

    public async create(body: RoleCreateRequest): Promise<RoleResponse> {
        try {
            const res = await this.mesh.roles.create(body);
            this.notification.show({
                type: 'success',
                message: 'mesh.create_role_success',
                translationParams: {
                    entityName: res.name,
                },
            });
            this.nameMap[res.uuid] = res.name;
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public createMapped(body: RoleCreateRequest): Promise<MeshRoleBO> {
        return this.create(body).then(role => this.mapToBusinessObject(role));
    }

    public async update(uuid: string, body: RoleUpdateRequest): Promise<RoleResponse> {
        try {
            const res = await this.mesh.roles.update(uuid, body);
            this.notification.show({
                type: 'success',
                message: 'mesh.update_role_success',
                translationParams: {
                    entityName: res.name,
                },
            });
            this.nameMap[res.uuid] = res.name;
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public updateMapped(uuid: string, body: RoleUpdateRequest): Promise<MeshRoleBO> {
        return this.update(uuid, body).then(role => this.mapToBusinessObject(role));
    }

    public async delete(uuid: string): Promise<void> {
        try {
            await this.mesh.roles.delete(uuid);
            const name = this.nameMap[uuid];
            delete this.nameMap[uuid];
            this.notification.show({
                type: 'success',
                message: 'mesh.delete_role_success',
                translationParams: {
                    entityName: name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }

    public async list(params?: RoleListOptions): Promise<RoleListResponse> {
        try {
            const res = await this.mesh.roles.list(params);
            for (const role of res.data) {
                this.nameMap[role.uuid] = role.name
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public listMapped(params?: RoleListOptions): Promise<ListResponse<MeshRoleBO>> {
        return this.list(params).then(res => {
            return {
                // eslint-disable-next-line no-underscore-dangle, @typescript-eslint/naming-convention
                _metainfo: res._metainfo,
                data: res.data.map((role, index) => this.mapToBusinessObject(role, index)),
            };
        });
    }
}
