/* eslint-disable @typescript-eslint/no-unsafe-call */
import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS } from '@admin-ui/common';
import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import {
    EDITABLE_ENTITY_PERMISSIONS,
    MBO_AVILABLE_PERMISSIONS,
    MBO_PERMISSION_PATH,
    MBO_PROJECT_CONTEXT,
    MBO_ROLE_PERMISSIONS,
    MBO_TYPE,
    MeshTagBO,
    MeshType,
} from '@admin-ui/mesh/common';
import { toPermissionArray } from '@admin-ui/mesh/utils';
import { Injectable } from '@angular/core';
import {
    ListResponse,
    NodeListResponse,
    TagCreateRequest,
    TagListOptions,
    TagListResponse,
    TagLoadOptions,
    TagNodeListOptions,
    TagResponse,
    TagUpdateRequest,
} from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseMeshEntitiyHandlerService } from '../base-mesh-entity-handler/base-mesh-entity-handler.service';

@Injectable()
export class TagHandlerService extends BaseMeshEntitiyHandlerService {

    constructor(
        errorHandler: ErrorHandler,
        notification: I18nNotificationService,
        protected mesh: MeshRestClientService,
    ) {
        super(errorHandler, notification);
    }

    public mapToBusinessObject(
        project: string,
        familyUuid: string,
        tag: TagResponse,
        _index?: number,
    ): MeshTagBO {
        return {
            ...tag,
            [BO_ID]: tag.uuid,
            [BO_PERMISSIONS]: toPermissionArray(tag.permissions),
            [BO_DISPLAY_NAME]: tag.name,
            [MBO_TYPE]: MeshType.TAG,
            [MBO_AVILABLE_PERMISSIONS]: EDITABLE_ENTITY_PERMISSIONS,
            [MBO_ROLE_PERMISSIONS]: toPermissionArray(tag.rolePerms),
            [MBO_PERMISSION_PATH]: `${project}/tagFamilies/${familyUuid}/tags/${tag.uuid}`,
            [MBO_PROJECT_CONTEXT]: project,
        };
    }

    public async get(project: string, familyUuid: string, uuid: string, params?: TagLoadOptions): Promise<TagResponse> {
        try {
            const res = await this.mesh.tags.get(project, familyUuid, uuid, params).send();
            this.nameMap[res.uuid] = res.name;
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public getMapped(project: string, familyUuid: string, uuid: string, params?: TagLoadOptions): Promise<MeshTagBO> {
        return this.get(project, familyUuid, uuid, params).then(tag => this.mapToBusinessObject(project, familyUuid, tag));
    }

    public async create(project: string, familyUuid: string, body: TagCreateRequest): Promise<TagResponse> {
        try {
            const res = await this.mesh.tags.create(project, familyUuid, body).send();
            this.notification.show({
                type: 'success',
                message: 'mesh.create_tag_success',
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

    public createMapped(project: string, familyUuid: string, body: TagCreateRequest): Promise<MeshTagBO> {
        return this.create(project, familyUuid, body).then(tag => this.mapToBusinessObject(project, familyUuid, tag));
    }

    public async update(project: string, familyUuid: string, uuid: string, body: TagUpdateRequest): Promise<TagResponse> {
        try {
            const res = await this.mesh.tags.update(project, familyUuid, uuid, body).send();
            this.notification.show({
                type: 'success',
                message: 'mesh.update_tag_success',
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

    public updateMapped(project: string, familyUuid: string, uuid: string, body: TagUpdateRequest): Promise<MeshTagBO> {
        return this.update(project, familyUuid, uuid, body).then(tag => this.mapToBusinessObject(project, familyUuid, tag));
    }

    public async delete(project: string, familyUuid: string, uuid: string): Promise<void> {
        try {
            await this.mesh.tags.delete(project, familyUuid, uuid).send();
            const name = this.nameMap[uuid];
            delete this.nameMap[uuid];
            this.notification.show({
                type: 'success',
                message: 'mesh.delete_tag_success',
                translationParams: {
                    entityName: name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }

    public async list(project: string, familyUuid: string, params?: TagListOptions): Promise<TagListResponse> {
        try {
            const res = await this.mesh.tags.list(project, familyUuid, params).send();
            for (const tag of res.data) {
                this.nameMap[tag.uuid] = tag.name;
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public listMapped(project: string, familyUuid: string, params?: TagListOptions): Promise<ListResponse<MeshTagBO>> {
        return this.list(project, familyUuid, params).then(res => {
            return {
                // eslint-disable-next-line no-underscore-dangle, @typescript-eslint/naming-convention
                _metainfo: res._metainfo,
                data: res.data.map((tag, index) => this.mapToBusinessObject(project, familyUuid, tag, index)),
            };
        });
    }

    public nodes(project: string, familyUuid: string, uuid: string, params?: TagNodeListOptions): Promise<NodeListResponse> {
        return this.mesh.tags.nodes(project, familyUuid, uuid, params).send();
    }
}
