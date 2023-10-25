import { Injectable } from '@angular/core';
import { BranchReference, NodeLoadOptions, NodeResponse, UserResponse } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { MeshSchemaListParams, MeshSchemaListResponse, SchemaElement } from '../models/mesh-browser-models';



@Injectable()
export class MeshBrowserLoaderService {

    constructor(protected meshClient: MeshRestClientService) {}


    public authMe(): Promise<UserResponse> {
        return this.meshClient.auth.me();
    }

    public getMeshUrl(): string {
        return this.meshClient.getConfig().connection.basePath;
    }

    public async getProjects(): Promise<Array<string>> {
        const projectList = await this.meshClient.projects.list();
        return projectList.data.map(project => project.name );
    }

    public async getBranches(project: string): Promise<Array<BranchReference>> {
        const branchList = await this.meshClient.branches.list(project);
        return branchList.data;
    }

    public async listSchemasWithRootNode(project: string): Promise<MeshSchemaListResponse> {
        const response = await this.meshClient.graphql(project, {
            query: `
                {
                    project{ rootNode {uuid}}
                    schemas {
                        elements {
                            name
                        }
                    }
                }
            `,
        });

        return {
            rootNodeUuid: response.data.project.rootNode.uuid,
            schemas: response.data.schemas.elements,
        }
    }

    public async listNodeChildrenForSchema(project: string, params: MeshSchemaListParams, branchUuid?: string): Promise<Array<SchemaElement>>  {
        const response = await this.meshClient.graphql(project, {
            query: `
                query ($page: Long, $perPage: Long, $schemaName: String, $nodeUuid: String, $lang: [String]) {
                    node(uuid: $nodeUuid) {
                        children(
                            filter: { schema: { name: { equals: $schemaName } } }
                            perPage: $perPage
                            page: $page
                            lang: $lang
                        ) {
                            elements {
                                uuid
                                displayName
                                isContainer
                                language
                                languages {
                                    language
                                }
                            }
                        }
                    }
                }
            `,
            variables: params,
        }, {
            branch: branchUuid,
            version: 'draft',
        });

        return response.data.node?.children?.elements
    }

    public async getNodeByUuid(project: string, uuid: string, params?: NodeLoadOptions): Promise<NodeResponse> {
        const response = await this.meshClient.nodes.get(project, uuid, params);
        return response;
    }

}
