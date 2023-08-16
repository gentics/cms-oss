import { BO_DISPLAY_NAME, BO_ID, BO_PERMISSIONS } from '@admin-ui/common';
import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import {
    BASIC_ENTITY_PERMISSIONS,
    MBO_AVILABLE_PERMISSIONS,
    MBO_PERMISSION_PATH,
    MBO_PROJECT_CONTEXT,
    MBO_ROLE_PERMISSIONS,
    MBO_TYPE,
    MeshTagFamilyBO,
    MeshType,
} from '@admin-ui/mesh/common';
import { toPermissionArray } from '@admin-ui/mesh/utils';
import { Injectable } from '@angular/core';
import {
    ListResponse,
    TagFamilyCreateRequest,
    TagFamilyListOptions,
    TagFamilyListResponse,
    TagFamilyLoadOptions,
    TagFamilyResponse,
    TagFamilyUpdateRequest,
} from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { BaseMeshEntitiyHandlerService } from '../base-mesh-entity-handler/base-mesh-entity-handler.service';
import { TagHandlerService } from '../tag-handler/tag-handler.service';

@Injectable()
export class TagFamilyHandlerService extends BaseMeshEntitiyHandlerService {

    constructor(
        errorHandler: ErrorHandler,
        notification: I18nNotificationService,
        protected mesh: MeshRestClientService,
        protected tagHandler: TagHandlerService,
    ) {
        super(errorHandler, notification);
    }

    public mapToBusinessObject(
        project: string,
        family: TagFamilyResponse,
        _index?: number,
    ): MeshTagFamilyBO {
        return {
            ...family,
            [BO_ID]: family.uuid,
            [BO_PERMISSIONS]: toPermissionArray(family.permissions),
            [BO_DISPLAY_NAME]: family.name,
            [MBO_TYPE]: MeshType.TAG_FAMILY,
            [MBO_AVILABLE_PERMISSIONS]: BASIC_ENTITY_PERMISSIONS,
            [MBO_ROLE_PERMISSIONS]: toPermissionArray(family.rolePerms),
            [MBO_PERMISSION_PATH]: `${project}/tagFamilies/${family.uuid}`,
            [MBO_PROJECT_CONTEXT]: project,
        };
    }

    public async get(project: string, uuid: string, params?: TagFamilyLoadOptions): Promise<TagFamilyResponse> {
        try {
            const res = await this.mesh.tagFamilies.get(project, uuid, params);
            this.nameMap[res.uuid] = res.name;
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public getMapped(project: string, uuid: string, params?: TagFamilyLoadOptions): Promise<MeshTagFamilyBO> {
        return this.get(project, uuid, params).then(family => this.mapToBusinessObject(project, family));
    }

    public async create(project: string, body: TagFamilyCreateRequest): Promise<TagFamilyResponse> {
        try {
            const res = await this.mesh.tagFamilies.create(project, body);
            this.notification.show({
                type: 'success',
                message: 'mesh.create_tag_family_success',
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

    public createMapped(project: string, body: TagFamilyCreateRequest): Promise<MeshTagFamilyBO> {
        return this.create(project, body).then(family => this.mapToBusinessObject(project, family));
    }

    public async update(project: string, uuid: string, body: TagFamilyUpdateRequest): Promise<TagFamilyResponse> {
        try {
            const res = await this.mesh.tagFamilies.update(project, uuid, body);
            this.notification.show({
                type: 'success',
                message: 'mesh.update_tag_family_success',
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

    public updateMapped(project: string, uuid: string, body: TagFamilyUpdateRequest): Promise<MeshTagFamilyBO> {
        return this.update(project, uuid, body).then(family => this.mapToBusinessObject(project, family));
    }

    public async delete(project: string, uuid: string): Promise<void> {
        try {
            await this.mesh.tagFamilies.delete(project, uuid);
            const name = this.nameMap[uuid];
            delete this.nameMap[uuid];
            this.notification.show({
                type: 'success',
                message: 'mesh.delete_tag_family_success',
                translationParams: {
                    entityName: name,
                },
            });
        } catch (err) {
            this.handleError(err);
        }
    }

    public async list(project: string, params?: TagFamilyListOptions): Promise<TagFamilyListResponse> {
        try {
            const res = await this.mesh.tagFamilies.list(project, params);
            for (const tag of res.data) {
                this.nameMap[tag.uuid] = tag.name;
            }
            return res;
        } catch (err) {
            this.handleError(err);
        }
    }

    public listMapped(project: string, params?: TagFamilyListOptions): Promise<ListResponse<MeshTagFamilyBO>> {
        return this.list(project, params).then(res => {
            return {
                // eslint-disable-next-line no-underscore-dangle, @typescript-eslint/naming-convention
                _metainfo: res._metainfo,
                data: res.data.map((family, index) => this.mapToBusinessObject(project, family, index)),
            };
        });
    }

    public async listWithTags(project: string, params?: TagFamilyListOptions): Promise<ListResponse<MeshTagFamilyBO>> {
        try {
            const res = await this.mesh.graphql(project, {
                query: `
query($page: Long, $perPage: Long, $sortBy: String, $order: SortOrder) {
    tagFamilies(page: $page, perPage: $perPage, sortBy: $sortBy, sortOrder: $order) {
        currentPage
        pageCount
        perPage
        totalCount

        elements {
            uuid
            name

            permissions {
                create
                read
                update
                delete
            }

            tags {
                totalCount

                elements {
                    uuid
                    name
                    permissions {
                        create
                        read
                        update
                        delete
                    }
                }
            }
        }
    }
}`,
                variables: params,
            });

            const families = res.data.tagFamilies;
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            const data: MeshTagFamilyBO[] = (families.elements || []).map((family, index) => {
                const { tags, ...rawFamily } = family;
                rawFamily[MBO_PROJECT_CONTEXT] = project;
                const mapped = this.mapToBusinessObject(project, rawFamily, index);
                // eslint-disable-next-line @typescript-eslint/no-unsafe-call
                mapped.tags = (tags.elements || []).map((tag, index) => {
                    tag.tagFamily = rawFamily;
                    this.tagHandler.nameMap[tag.uuid] = tag.name;
                    return this.tagHandler.mapToBusinessObject(project, rawFamily.uuid, tag, index);
                });

                return mapped;
            });

            return {
                // eslint-disable-next-line @typescript-eslint/naming-convention
                _metainfo: {
                    currentPage: families.currentPage,
                    pageCount: families.pageCount,
                    perPage: families.perPage,
                    totalCount: families.totalCount,
                },
                data,
            };
        } catch (err) {
            this.handleError(err);
        }
    }
}
