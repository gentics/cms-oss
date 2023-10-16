import { Injectable } from '@angular/core';
import { PagingOptions } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';



export interface MeshSchemaListParams extends PagingOptions  {
    schemaName?: string,
    nodeUuid: string,
}

export interface MeshSchemaListResponse {
    rootNodeUuid: string,
    schemas: Array<Schema>
}


export interface Schema {
    name: string;
    elements: Array<SchemaElement>;
}

export interface SchemaElement {
    displayName: string;
    uuid: string;
    isContainer: boolean;
    languages: Array<ElementLanguage>;
}

export interface ElementLanguage {
    language: string;
}



@Injectable()
export class MeshBrowserLoaderService {

    constructor(protected meshClient: MeshRestClientService) {}


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


    public async listNodeChildrenForSchema(project: string, params: MeshSchemaListParams): Promise<Array<SchemaElement>>  {
        const response = await this.meshClient.graphql(project, {
            query: `
                query ($page: Long, $perPage: Long, $schemaName: String, $nodeUuid: String) {
                    node(uuid: $nodeUuid) {
                        children(
                            filter: { schema: { name: { equals: $schemaName } } }
                            perPage: $perPage
                            page: $page
                        ) {
                            elements {
                                uuid
                                displayName
                                isContainer
                                languages {
                                    language
                                }
                            }
                        }
                    }
                }
            `,
            variables: params,
        });

        return response.data.node.children.elements
    }

}
