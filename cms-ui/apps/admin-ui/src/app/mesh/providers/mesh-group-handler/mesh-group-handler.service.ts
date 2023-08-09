import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS } from '@admin-ui/common';
import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import {
    BASIC_ENTITY_PERMISSIONS,
    MBO_AVILABLE_PERMISSIONS,
    MBO_PERMISSION_PATH,
    MBO_ROLE_PERMISSIONS,
    MBO_TYPE,
    MeshGroupBO,
    MeshType,
} from '@admin-ui/mesh/common';
import { getUserDisplayName, toPermissionArray } from '@admin-ui/mesh/utils';
import { Injectable } from '@angular/core';
import {
    GroupCreateRequest,
    GroupListOptions,
    GroupListResponse,
    GroupLoadOptions,
    GroupReference,
    GroupResponse,
    GroupUpdateRequest,
    ListResponse,
    RoleReference,
    UserListOptions,
    UserListResponse,
    UserReference,
} from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseMeshEntitiyHandlerService } from '../base-mesh-entity-handler/base-mesh-entity-handler.service';

@Injectable()
export class MeshGroupHandlerService extends BaseMeshEntitiyHandlerService {

    constructor(
        errorHandler: ErrorHandler,
        notification: I18nNotificationService,
        protected mesh: MeshRestClientService,
    ) {
        super(errorHandler, notification);
    }

    public mapToBusinessObject(
        group: GroupResponse,
        _index?: number,
    ): MeshGroupBO {
        return {
            ...group,
            [BO_ID]: group.uuid,
            [BO_PERMISSIONS]: toPermissionArray(group.permissions),
            [BO_DISPLAY_NAME]: group.name,
            [MBO_TYPE]: MeshType.GROUP,
            [MBO_AVILABLE_PERMISSIONS]: BASIC_ENTITY_PERMISSIONS,
            [MBO_ROLE_PERMISSIONS]: toPermissionArray(group.rolePerms),
            [MBO_PERMISSION_PATH]: `groups/${group.uuid}`,
        };
    }

    public async get(uuid: string, params?: GroupLoadOptions): Promise<GroupResponse> {
        try {
            const res = await this.mesh.groups.get(uuid, params);
            this.nameMap[res.uuid] = res.name;
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public getMapped(uuid: string, params?: GroupLoadOptions): Promise<MeshGroupBO> {
        return this.get(uuid, params).then(group => this.mapToBusinessObject(group));
    }

    public async create(body: GroupCreateRequest): Promise<GroupResponse> {
        try {
            const res = await this.mesh.groups.create(body);
            this.notification.show({
                type: 'success',
                message: 'mesh.create_group_success',
                translationParams: {
                    entityName: res.name,
                },
            });
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public createMapped(body: GroupCreateRequest): Promise<MeshGroupBO> {
        return this.create(body).then(group => this.mapToBusinessObject(group));
    }

    public async update(uuid: string, body: GroupUpdateRequest): Promise<GroupResponse> {
        try {
            const res = await this.mesh.groups.update(uuid, body);
            this.notification.show({
                type: 'success',
                message: 'mesh.update_group_success',
                translationParams: {
                    entityName: res.name,
                },
            });
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public updateMapped(uuid: string, body: GroupUpdateRequest): Promise<MeshGroupBO> {
        return this.update(uuid, body).then(group => this.mapToBusinessObject(group));
    }

    public async delete(uuid: string): Promise<void> {
        try {
            await this.mesh.groups.delete(uuid);
            const name = this.nameMap[uuid];
            delete this.nameMap[uuid];
            this.notification.show({
                type: 'success',
                message: 'mesh.delete_group_success',
                translationParams: {
                    entityName: name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }

    public async list(params?: GroupListOptions): Promise<GroupListResponse> {
        try {
            const res = await this.mesh.groups.list(params);
            for (const group of res.data) {
                this.nameMap[group.uuid] = group.name
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public listMapped(params?: GroupListOptions): Promise<ListResponse<MeshGroupBO>> {
        return this.list(params).then(res => {
            return {
                // eslint-disable-next-line no-underscore-dangle, @typescript-eslint/naming-convention
                _metainfo: res._metainfo,
                data: res.data.map((group, index) => this.mapToBusinessObject(group, index)),
            };
        });
    }

    async getUsers(uuid: string, _params?: UserListOptions): Promise<UserListResponse> {
        return this.mesh.groups.getUsers(uuid);
    }

    async assignRole(group: GroupReference, role: RoleReference): Promise<void> {
        try {
            await this.mesh.groups.assignRole(group.uuid, role.uuid);
            this.notification.show({
                type: 'success',
                message: 'mesh.assign_role_to_group_success',
                translationParams: {
                    roleName: role.name,
                    groupName: group.name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }

    async unassignRole(group: GroupReference, role: RoleReference): Promise<void> {
        try {
            await this.mesh.groups.unassignRole(group.uuid, role.uuid);
            this.notification.show({
                type: 'success',
                message: 'mesh.unassign_role_from_group_success',
                translationParams: {
                    roleName: role.name,
                    groupName: group.name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }

    async assignUser(group: GroupReference, user: UserReference): Promise<void> {
        try {
            await this.mesh.groups.assignUser(group.uuid, user.uuid);
            this.notification.show({
                type: 'success',
                message: 'mesh.assign_user_to_group_success',
                translationParams: {
                    roleName: getUserDisplayName(user),
                    groupName: group.name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }

    async unassignUser(group: GroupReference, user: UserReference): Promise<void> {
        try {
            await this.mesh.groups.unassignUser(group.uuid, user.uuid);
            this.notification.show({
                type: 'success',
                message: 'mesh.unassign_user_from_group_success',
                translationParams: {
                    userName: getUserDisplayName(user),
                    groupName: group.name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }
}
