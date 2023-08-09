import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS } from '@admin-ui/common';
import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import {
    BASIC_ENTITY_PERMISSIONS,
    MBO_AVILABLE_PERMISSIONS,
    MBO_PERMISSION_PATH,
    MBO_ROLE_PERMISSIONS,
    MBO_TYPE,
    MeshProjectBO,
    MeshType,
} from '@admin-ui/mesh/common';
import { toPermissionArray } from '@admin-ui/mesh/utils';
import { Injectable } from '@angular/core';
import {
    GenericMessageResponse,
    ListResponse,
    MicroschemaListOptions,
    MicroschemaListResponse,
    MicroschemaResponse,
    ProjectCreateRequest,
    ProjectListOptions,
    ProjectListResponse,
    ProjectLoadOptions,
    ProjectResponse,
    ProjectUpdateRequest,
    SchemaListOptions,
    SchemaListResponse,
    SchemaResponse,
} from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseMeshEntitiyHandlerService } from '../base-mesh-entity-handler/base-mesh-entity-handler.service';

@Injectable()
export class ProjectHandlerService extends BaseMeshEntitiyHandlerService {

    constructor(
        errorHandler: ErrorHandler,
        notification: I18nNotificationService,
        protected mesh: MeshRestClientService,
    ) {
        super(errorHandler, notification);
    }

    public mapToBusinessObject(
        project: ProjectResponse,
        _index?: number,
    ): MeshProjectBO {
        return {
            ...project,
            [BO_ID]: project.uuid,
            [BO_PERMISSIONS]: toPermissionArray(project.permissions),
            [BO_DISPLAY_NAME]: project.name,
            [MBO_TYPE]: MeshType.PROJECT,
            [MBO_AVILABLE_PERMISSIONS]: BASIC_ENTITY_PERMISSIONS,
            [MBO_ROLE_PERMISSIONS]: toPermissionArray(project.rolePerms),
            [MBO_PERMISSION_PATH]: `projects/${project.uuid}`,
        };
    }

    public async get(uuid: string, params?: ProjectLoadOptions): Promise<ProjectResponse> {
        try {
            const res = await this.mesh.projects.get(uuid, params);
            this.nameMap[res.uuid] = res.name;
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public getMapped(uuid: string, params?: ProjectLoadOptions): Promise<MeshProjectBO> {
        return this.get(uuid, params).then(schema => this.mapToBusinessObject(schema));
    }

    public async create(body: ProjectCreateRequest): Promise<ProjectResponse> {
        try {
            const res = await this.mesh.projects.create(body);
            this.notification.show({
                type: 'success',
                message: 'mesh.create_project_success',
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

    public createMapped(body: ProjectCreateRequest): Promise<MeshProjectBO> {
        return this.create(body).then(schema => this.mapToBusinessObject(schema));
    }

    public async update(uuid: string, body: ProjectUpdateRequest): Promise<ProjectResponse> {
        try {
            const res = await this.mesh.projects.update(uuid, body);
            this.notification.show({
                type: 'success',
                message: 'mesh.update_project_success',
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

    public updateMapped(uuid: string, body: ProjectUpdateRequest): Promise<MeshProjectBO> {
        return this.update(uuid, body).then(schema => this.mapToBusinessObject(schema));
    }

    public async delete(uuid: string): Promise<void> {
        try {
            await this.mesh.projects.delete(uuid);
            const name = this.nameMap[uuid];
            delete this.nameMap[uuid];
            this.notification.show({
                type: 'success',
                message: 'mesh.delete_project_success',
                translationParams: {
                    entityName: name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }

    public async list(params?: ProjectListOptions): Promise<ProjectListResponse> {
        try {
            const res = await this.mesh.projects.list(params);
            for (const project of res.data) {
                this.nameMap[project.uuid] = project.name;
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public listMapped(params?: ProjectListOptions): Promise<ListResponse<MeshProjectBO>> {
        return this.list(params).then(res => {
            return {
                // eslint-disable-next-line no-underscore-dangle, @typescript-eslint/naming-convention
                _metainfo: res._metainfo,
                data: res.data.map((schema, index) => this.mapToBusinessObject(schema, index)),
            };
        });
    }
}
