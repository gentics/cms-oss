/* eslint-disable @typescript-eslint/no-unsafe-call */
import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS } from '@admin-ui/common';
import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import {
    EDITABLE_ENTITY_PERMISSIONS,
    MBO_AVILABLE_PERMISSIONS,
    MBO_PERMISSION_PATH,
    MBO_ROLE_PERMISSIONS,
    MBO_TYPE,
    MeshType,
    MeshUserBO,
} from '@admin-ui/mesh/common';
import { getUserDisplayName, toPermissionArray } from '@admin-ui/mesh/utils';
import { Injectable } from '@angular/core';
import {
    ListResponse,
    UserAPITokenResponse,
    UserCreateRequest,
    UserListOptions,
    UserListResponse,
    UserLoadOptions,
    UserResponse,
    UserUpdateRequest,
} from '@gentics/mesh-models';
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

    public mapToBusinessObject(
        user: UserResponse,
        _index?: number,
    ): MeshUserBO {
        return {
            ...user,
            [BO_ID]: user.uuid,
            [BO_PERMISSIONS]: toPermissionArray(user.permissions),
            [BO_DISPLAY_NAME]: getUserDisplayName(user),
            [MBO_TYPE]: MeshType.USER,
            [MBO_AVILABLE_PERMISSIONS]: EDITABLE_ENTITY_PERMISSIONS,
            [MBO_ROLE_PERMISSIONS]: toPermissionArray(user.rolePerms),
            [MBO_PERMISSION_PATH]: `users/${user.uuid}`,
        };
    }

    public async get(uuid: string, params?: UserLoadOptions): Promise<UserResponse> {
        try {
            const res = await this.mesh.users.get(uuid, params).send();
            this.nameMap[res.uuid] = getUserDisplayName(res);
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public getMapped(uuid: string, params?: UserLoadOptions): Promise<MeshUserBO> {
        return this.get(uuid, params).then(user => this.mapToBusinessObject(user));
    }

    public async create(body: UserCreateRequest): Promise<UserResponse> {
        try {
            const res = await this.mesh.users.create(body).send();
            const name = getUserDisplayName(res);
            this.notification.show({
                type: 'success',
                message: 'mesh.create_user_success',
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

    public createMapped(body: UserCreateRequest): Promise<MeshUserBO> {
        return this.create(body).then(user => this.mapToBusinessObject(user));
    }

    public async update(uuid: string, body: UserUpdateRequest): Promise<UserResponse> {
        try {
            const res = await this.mesh.users.update(uuid, body).send();
            const name = getUserDisplayName(res);
            this.notification.show({
                type: 'success',
                message: 'mesh.update_user_success',
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

    public updateMapped(uuid: string, body: UserUpdateRequest): Promise<MeshUserBO> {
        return this.update(uuid, body).then(user => this.mapToBusinessObject(user));
    }

    public async delete(uuid: string): Promise<void> {
        try {
            await this.mesh.users.delete(uuid).send();
            const name = this.nameMap[uuid];
            delete this.nameMap[uuid];
            this.notification.show({
                type: 'success',
                message: 'mesh.delete_user_success',
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
            const res = await this.mesh.users.list(params).send();
            for (const user of res.data) {
                this.nameMap[user.uuid] = getUserDisplayName(user);
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public listMapped(params?: UserListOptions): Promise<ListResponse<MeshUserBO>> {
        return this.list(params).then(res => {
            return {
                // eslint-disable-next-line no-underscore-dangle, @typescript-eslint/naming-convention
                _metainfo: res._metainfo,
                data: res.data.map((user, index) => this.mapToBusinessObject(user, index)),
            };
        });
    }

    public async createAPIToken(uuid: string): Promise<UserAPITokenResponse> {
        try {
            const res = await this.mesh.users.createAPIToken(uuid).send();
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }
}
