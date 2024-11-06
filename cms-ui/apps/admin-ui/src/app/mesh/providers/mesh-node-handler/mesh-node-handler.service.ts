/* eslint-disable @typescript-eslint/no-unsafe-call */
import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS } from '@admin-ui/common';
import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import {
    MBO_AVILABLE_PERMISSIONS,
    MBO_PERMISSION_PATH,
    MBO_PROJECT_CONTEXT,
    MBO_ROLE_PERMISSIONS,
    MBO_TYPE,
    MeshNodeBO,
    MeshType,
    NODE_PERMISSIONS,
} from '@admin-ui/mesh/common';
import { toPermissionArray } from '@admin-ui/mesh/utils';
import { Injectable } from '@angular/core';
import {
    GenericMessageResponse,
    ListResponse,
    NodeCreateRequest,
    NodeDeleteOptions,
    NodeListOptions,
    NodeListResponse,
    NodeLoadOptions,
    NodeResponse,
    NodeUpdateRequest,
} from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseMeshEntitiyHandlerService } from '../base-mesh-entity-handler/base-mesh-entity-handler.service';

@Injectable()
export class MeshNodeHandlerService extends BaseMeshEntitiyHandlerService {

    constructor(
        errorHandler: ErrorHandler,
        notifications: I18nNotificationService,
        protected mesh: MeshRestClientService,
    ) {
        super(errorHandler, notifications);
    }

    public mapToBusinessObject(
        project: string,
        node: NodeResponse,
        _index?: number,
    ): MeshNodeBO {
        return {
            ...node,
            [BO_ID]: node.uuid,
            [BO_PERMISSIONS]: toPermissionArray(node.permissions),
            [BO_DISPLAY_NAME]: this.getDisplayName(node),
            [MBO_TYPE]: MeshType.NODE,
            [MBO_AVILABLE_PERMISSIONS]: NODE_PERMISSIONS,
            [MBO_ROLE_PERMISSIONS]: toPermissionArray(node.rolePerms),
            [MBO_PERMISSION_PATH]: `projects/${project}/nodes/${node.uuid}`,
            [MBO_PROJECT_CONTEXT]: project,
        };
    }

    public getDisplayName(node: NodeResponse): string {
        return node.displayName || node.uuid;
    }

    public async get(project: string, uuid: string, params?: NodeLoadOptions): Promise<NodeResponse> {
        try {
            const res = await this.mesh.nodes.get(project, uuid, params).send();
            this.nameMap[res.uuid] = this.getDisplayName(res);
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public getMapped(project: string, uuid: string, params?: NodeLoadOptions): Promise<MeshNodeBO> {
        return this.get(project, uuid, params).then(node => this.mapToBusinessObject(project, node));
    }

    public async create(project: string, body: NodeCreateRequest): Promise<NodeResponse> {
        try {
            const res = await this.mesh.nodes.create(project, body).send();
            const name = this.getDisplayName(res);
            this.nameMap[res.uuid] = name;
            this.notification.show({
                type: 'success',
                message: 'mesh.create_node_success',
                translationParams: {
                    entityName: name,
                },
            });
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public createMapped(project: string, body: NodeCreateRequest): Promise<MeshNodeBO> {
        return this.create(project, body).then(node => this.mapToBusinessObject(project, node));
    }

    public async update(project: string, uuid: string, body: NodeUpdateRequest): Promise<NodeResponse> {
        try {
            const res = await this.mesh.nodes.update(project, uuid, body).send();
            const name = this.getDisplayName(res);
            this.nameMap[res.uuid] = name;
            this.notification.show({
                type: 'success',
                message: 'mesh.update_node_success',
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

    public updateMapped(project: string, uuid: string, body: NodeUpdateRequest): Promise<MeshNodeBO> {
        return this.update(project, uuid, body).then(node => this.mapToBusinessObject(project, node));
    }

    public async delete(project: string, uuid: string, params?: NodeDeleteOptions): Promise<GenericMessageResponse> {
        try {
            const res = await this.mesh.nodes.delete(project, uuid, params).send();
            const name = this.nameMap[uuid];
            delete this.nameMap[uuid];
            this.notification.show({
                type: 'success',
                message: 'mesh.delete_node_success',
                translationParams: {
                    entityName: name,
                },
            });
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public async list(project: string, parent?: string, params?: NodeListOptions): Promise<NodeListResponse> {
        try {
            let res: NodeListResponse;
            if (parent) {
                res = await this.mesh.nodes.children(project, parent, params).send();
            } else {
                res = await this.mesh.nodes.list(project, params).send();
            }
            for (const node of res.data) {
                this.nameMap[node.uuid] = this.getDisplayName(node);
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public listMapped(project: string, parent?: string, params?: NodeListOptions): Promise<ListResponse<MeshNodeBO>> {
        return this.list(project, parent, params).then(res => {
            return {
                // eslint-disable-next-line no-underscore-dangle, @typescript-eslint/naming-convention
                _metainfo: res._metainfo,
                data: res.data.map((node, index) => this.mapToBusinessObject(project, node, index)),
            };
        });
    }
}
