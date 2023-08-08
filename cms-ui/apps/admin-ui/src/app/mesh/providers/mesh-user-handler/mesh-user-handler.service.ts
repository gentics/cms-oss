import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import { getUserName } from '@admin-ui/mesh/utils';
import { Injectable } from '@angular/core';
import { UserAPITokenResponse, UserCreateRequest, UserListOptions, UserListResponse, UserResponse, UserUpdateRequest } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseMeshEntitiyHandlerService } from '../base-mesh-entity-handler/base-mesh-entity-handler.service';

@Injectable()
export class MeshUserHandlerService extends BaseMeshEntitiyHandlerService {

    constructor(
        errorHandler: ErrorHandler,
        notification: I18nNotificationService,
        protected mesh: MeshRestClientService,
    ) {
        super(errorHandler, notification);
    }

    public async get(uuid: string): Promise<UserResponse> {
        try {
            const res = await this.mesh.users.get(uuid);
            this.nameMap[res.uuid] = getUserName(res);
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public async create(body: UserCreateRequest): Promise<UserResponse> {
        try {
            const res = await this.mesh.users.create(body);
            const name = getUserName(res);
            this.notification.show({
                type: 'success',
                message: 'mesh.create_role_success',
                translationParams: {
                    entityName: name,
                },
            });
            this.nameMap[res.uuid] = name;
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public async update(uuid: string, body: UserUpdateRequest): Promise<UserResponse> {
        try {
            const res = await this.mesh.users.update(uuid, body);
            const name = getUserName(res);
            this.notification.show({
                type: 'success',
                message: 'mesh.update_role_success',
                translationParams: {
                    entityName: name,
                },
            });
            this.nameMap[res.uuid] = name;
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public async delete(uuid: string): Promise<void> {
        try {
            await this.mesh.users.delete(uuid);
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

    public async list(params?: UserListOptions): Promise<UserListResponse> {
        try {
            const res = await this.mesh.users.list(params);
            for (const user of res.data) {
                this.nameMap[user.uuid] = getUserName(user);
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public async createAPIToken(uuid: string): Promise<UserAPITokenResponse> {
        try {
            const res = await this.mesh.users.createAPIToken(uuid);
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }
}
