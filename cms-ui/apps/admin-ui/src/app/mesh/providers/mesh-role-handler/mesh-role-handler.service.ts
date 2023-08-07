import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import { Injectable } from '@angular/core';
import { RoleCreateRequest, RoleListOptions, RoleListResponse, RoleResponse, RoleUpdateRequest } from '@gentics/mesh-models';
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
}
