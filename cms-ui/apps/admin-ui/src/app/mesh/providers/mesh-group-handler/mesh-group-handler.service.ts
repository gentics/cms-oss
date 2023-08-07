import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import { Injectable } from '@angular/core';
import {
    GroupCreateRequest,
    GroupListOptions,
    GroupListResponse,
    GroupReference,
    GroupResponse,
    GroupUpdateRequest,
    RoleReference,
    UserListOptions,
    UserListResponse,
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
            for (const role of res.data) {
                this.nameMap[role.uuid] = role.name
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    async getUsers(uuid: string, params?: UserListOptions): Promise<UserListResponse> {
        return this.mesh.groups.getUsers(uuid);
    }

    async assignRoleToGroup(role: RoleReference, group: GroupReference): Promise<void> {
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

    async unassignRoleFromGroup(role: RoleReference, group: GroupReference): Promise<void> {
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
}
