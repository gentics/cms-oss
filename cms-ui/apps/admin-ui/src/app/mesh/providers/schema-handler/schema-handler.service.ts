import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS } from '@admin-ui/common';
import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import {
    BASIC_ENTITY_PERMISSIONS,
    MBO_AVILABLE_PERMISSIONS,
    MBO_PERMISSION_PATH,
    MBO_ROLE_PERMISSIONS,
    MBO_TYPE,
    MeshSchemaBO,
    MeshType,
} from '@admin-ui/mesh/common';
import { toPermissionArray } from '@admin-ui/mesh/utils';
import { Injectable } from '@angular/core';
import {
    ListResponse,
    ProjectReference,
    SchemaCreateRequest,
    SchemaListOptions,
    SchemaListResponse,
    SchemaLoadOptions,
    SchemaReference,
    SchemaResponse,
    SchemaUpdateRequest,
} from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseMeshEntitiyHandlerService } from '../base-mesh-entity-handler/base-mesh-entity-handler.service';

@Injectable()
export class SchemaHandlerService extends BaseMeshEntitiyHandlerService {

    constructor(
        errorHandler: ErrorHandler,
        notification: I18nNotificationService,
        protected mesh: MeshRestClientService,
    ) {
        super(errorHandler, notification);
    }

    public mapToBusinessObject(
        schema: SchemaResponse,
        _index?: number,
    ): MeshSchemaBO {
        return {
            ...schema,
            [BO_ID]: schema.uuid,
            [BO_PERMISSIONS]: toPermissionArray(schema.permissions),
            [BO_DISPLAY_NAME]: schema.name,
            [MBO_TYPE]: MeshType.SCHEMA,
            [MBO_AVILABLE_PERMISSIONS]: BASIC_ENTITY_PERMISSIONS,
            [MBO_ROLE_PERMISSIONS]: toPermissionArray(schema.rolePerms),
            [MBO_PERMISSION_PATH]: `schemas/${schema.uuid}`,
        };
    }

    public async get(uuid: string, params?: SchemaLoadOptions): Promise<SchemaResponse> {
        try {
            const res = await this.mesh.schemas.get(uuid, params);
            this.nameMap[res.uuid] = res.name;
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public getMapped(uuid: string, params?: SchemaLoadOptions): Promise<MeshSchemaBO> {
        return this.get(uuid, params).then(schema => this.mapToBusinessObject(schema));
    }

    public async create(body: SchemaCreateRequest): Promise<SchemaResponse> {
        try {
            const res = await this.mesh.schemas.create(body);
            this.notification.show({
                type: 'success',
                message: 'mesh.create_schema_success',
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

    public createMapped(body: SchemaCreateRequest): Promise<MeshSchemaBO> {
        return this.create(body).then(schema => this.mapToBusinessObject(schema));
    }

    public async update(uuid: string, body: SchemaUpdateRequest): Promise<SchemaResponse> {
        try {
            const res = await this.mesh.schemas.update(uuid, body);
            this.notification.show({
                type: 'success',
                message: 'mesh.update_schema_success',
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

    public updateMapped(uuid: string, body: SchemaUpdateRequest): Promise<MeshSchemaBO> {
        return this.update(uuid, body).then(schema => this.mapToBusinessObject(schema));
    }

    public async delete(uuid: string): Promise<void> {
        try {
            await this.mesh.schemas.delete(uuid);
            const name = this.nameMap[uuid];
            delete this.nameMap[uuid];
            this.notification.show({
                type: 'success',
                message: 'mesh.delete_schema_success',
                translationParams: {
                    entityName: name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }

    public async list(params?: SchemaListOptions): Promise<SchemaListResponse> {
        try {
            const res = await this.mesh.schemas.list(params);
            for (const schema of res.data) {
                this.nameMap[schema.uuid] = schema.name;
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public listMapped(params?: SchemaListOptions): Promise<ListResponse<MeshSchemaBO>> {
        return this.list(params).then(res => {
            return {
                // eslint-disable-next-line no-underscore-dangle, @typescript-eslint/naming-convention
                _metainfo: res._metainfo,
                data: res.data.map((schema, index) => this.mapToBusinessObject(schema, index)),
            };
        });
    }

    public async listFromProject(project: string, params?: SchemaListOptions): Promise<SchemaListResponse> {
        try {
            const res = await this.mesh.projects.listSchemas(project, params);
            for (const schema of res.data) {
                this.nameMap[schema.uuid] = schema.name;
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public async assignToProject(project: ProjectReference, schema: SchemaReference): Promise<SchemaResponse> {
        try {
            const res = await this.mesh.projects.assignSchema(project.name, schema.uuid);
            this.notification.show({
                type: 'success',
                message: 'mesh.assign_schema_to_project_success',
                translationParams: {
                    projectName: project.name,
                    schemaName: schema.name,
                },
            });
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public async unassignFromProject(project: ProjectReference, schema: SchemaReference): Promise<void> {
        try {
            await this.mesh.projects.unassignSchema(project.name, schema.uuid);
            this.notification.show({
                type: 'success',
                message: 'mesh.unassign_schema_from_project_success',
                translationParams: {
                    projectName: project.name,
                    schemaName: schema.name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }
}
