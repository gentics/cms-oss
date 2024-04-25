/* eslint-disable @typescript-eslint/no-unsafe-call */
import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS } from '@admin-ui/common';
import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import {
    EDITABLE_ENTITY_PERMISSIONS,
    MBO_AVILABLE_PERMISSIONS,
    MBO_PERMISSION_PATH,
    MBO_ROLE_PERMISSIONS,
    MBO_TYPE,
    MeshMicroschemaBO,
    MeshType,
} from '@admin-ui/mesh/common';
import { toPermissionArray } from '@admin-ui/mesh/utils';
import { Injectable } from '@angular/core';
import {
    ListResponse,
    MicroschemaCreateRequest,
    MicroschemaListOptions,
    MicroschemaListResponse,
    MicroschemaLoadOptions,
    MicroschemaReference,
    MicroschemaResponse,
    MicroschemaUpdateRequest,
    ProjectReference,
    SchemaCreateRequest,
    SchemaLoadOptions,
} from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseMeshEntitiyHandlerService } from '../base-mesh-entity-handler/base-mesh-entity-handler.service';

@Injectable()
export class MicroschemaHandlerService extends BaseMeshEntitiyHandlerService {

    constructor(
        errorHandler: ErrorHandler,
        notification: I18nNotificationService,
        protected mesh: MeshRestClientService,
    ) {
        super(errorHandler, notification);
    }

    public mapToBusinessObject(
        microschema: MicroschemaResponse,
        _index?: number,
    ): MeshMicroschemaBO {
        return {
            ...microschema,
            [BO_ID]: microschema.uuid,
            [BO_PERMISSIONS]: toPermissionArray(microschema.permissions),
            [BO_DISPLAY_NAME]: microschema.name,
            [MBO_TYPE]: MeshType.MICROSCHEMA,
            [MBO_AVILABLE_PERMISSIONS]: EDITABLE_ENTITY_PERMISSIONS,
            [MBO_ROLE_PERMISSIONS]: toPermissionArray(microschema.rolePerms),
            [MBO_PERMISSION_PATH]: `microschemas/${microschema.uuid}`,
        };
    }

    public async get(uuid: string, params?: MicroschemaLoadOptions): Promise<MicroschemaResponse> {
        try {
            const res = await this.mesh.microschemas.get(uuid, params).send();
            this.nameMap[res.uuid] = res.name;
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public getMapped(uuid: string, params?: SchemaLoadOptions): Promise<MeshMicroschemaBO> {
        return this.get(uuid, params).then(schema => this.mapToBusinessObject(schema));
    }

    public async create(body: MicroschemaCreateRequest): Promise<MicroschemaResponse> {
        try {
            const res = await this.mesh.microschemas.create(body).send();
            this.notification.show({
                type: 'success',
                message: 'mesh.create_microschema_success',
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

    public createMapped(body: SchemaCreateRequest): Promise<MeshMicroschemaBO> {
        return this.create(body).then(schema => this.mapToBusinessObject(schema));
    }

    public async update(uuid: string, body: MicroschemaUpdateRequest): Promise<MicroschemaResponse> {
        try {
            const res = await this.mesh.microschemas.update(uuid, body).send();
            this.notification.show({
                type: 'success',
                message: 'mesh.update_microschema_success',
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

    public updateMapped(uuid: string, body: MicroschemaUpdateRequest): Promise<MeshMicroschemaBO> {
        return this.update(uuid, body).then(schema => this.mapToBusinessObject(schema));
    }

    public async delete(uuid: string): Promise<void> {
        try {
            await this.mesh.microschemas.delete(uuid).send();
            const name = this.nameMap[uuid];
            delete this.nameMap[uuid];
            this.notification.show({
                type: 'success',
                message: 'mesh.delete_microschema_success',
                translationParams: {
                    entityName: name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }

    public async list(params?: MicroschemaListOptions): Promise<MicroschemaListResponse> {
        try {
            const res = await this.mesh.microschemas.list(params).send();
            for (const microschema of res.data) {
                this.nameMap[microschema.uuid] = microschema.name;
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public listMapped(params?: MicroschemaListOptions): Promise<ListResponse<MeshMicroschemaBO>> {
        return this.list(params).then(res => {
            return {
                // eslint-disable-next-line no-underscore-dangle, @typescript-eslint/naming-convention
                _metainfo: res._metainfo,
                data: res.data.map((microschema, index) => this.mapToBusinessObject(microschema, index)),
            };
        });
    }

    public async listFromProject(project: string, params?: MicroschemaListOptions): Promise<MicroschemaListResponse> {
        try {
            const res = await this.mesh.projects.listMicroschemas(project, params).send();
            for (const microschema of res.data) {
                this.nameMap[microschema.uuid] = microschema.name;
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public async assignToProject(project: ProjectReference, microschema: MicroschemaReference): Promise<MicroschemaResponse> {
        try {
            const res = await this.mesh.projects.assignMicroschema(project.name, microschema.uuid).send();
            this.notification.show({
                type: 'success',
                message: 'mesh.assign_microschema_to_project_success',
                translationParams: {
                    projectName: project.name,
                    microschemaName: microschema.name,
                },
            });
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public async unassignFromProject(project: ProjectReference, microschema: MicroschemaReference): Promise<void> {
        try {
            await this.mesh.projects.unassignMicroschema(project.name, microschema.uuid).send();
            this.notification.show({
                type: 'success',
                message: 'mesh.unassign_microschema_from_project_success',
                translationParams: {
                    projectName: project.name,
                    microschemaName: microschema.name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }

    public async getAllNames(project?: string, checkProject: boolean = true): Promise<MicroschemaReference[]> {
        try {
            if (checkProject) {
                project = (await this.mesh.projects.list({ perPage: 1 }).send())?.data?.[0]?.name;
            }

            let schemas: MicroschemaReference[] = [];

            if (project) {
                const res = await this.mesh.graphql(project, {
                    query: `
{
    microschemas {
        elements {
            uuid
            name
        }
    }
}
                    `,
                }).send();
                schemas = res.data?.schemas?.elements || [];
            } else {
                schemas = (await this.mesh.microschemas.list().send())?.data || [];
            }

            for (const ref of schemas) {
                this.nameMap[ref.uuid] = ref.name;
            }
            return schemas;
        } catch (err) {
            this.handleError(err);
        }
    }
}
