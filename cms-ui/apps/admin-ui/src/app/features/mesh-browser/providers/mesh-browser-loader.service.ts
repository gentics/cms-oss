import { Injectable } from '@angular/core';
import { BranchReference, GraphQLOptions, Language, NodeLoadOptions, NodeResponse, ProjectResponse, UserResponse } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { MeshSchemaListParams, MeshSchemaListResponse, NumberOfSchemaElements, SchemaContainer, SchemaElement } from '../models/mesh-browser-models';


@Injectable()
export class MeshBrowserLoaderService {

    constructor(protected meshClient: MeshRestClientService) {}


    public authMe(): Promise<UserResponse> {
        return this.meshClient.auth.me();
    }

    public async getProjects(): Promise<ProjectResponse[]> {
        const projectList = await this.meshClient.projects.list();
        return projectList.data;
    }

    public async getBranches(project: string): Promise<BranchReference[]> {
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

    public async getProjectRootNodeUuid(project: string): Promise<string> {
        const response = await this.meshClient.graphql(project, {
            query: `
                {
                    project{ rootNode {uuid}}
                }
            `,
        });

        return response.data.project.rootNode.uuid;
    }

    public async listProjectSchemas(project: string): Promise<SchemaContainer[]> {
        const response = await this.meshClient.projects.listSchemas(project);

        return response.data.map(schemaItem => {
            return {
                name: schemaItem.name,
                fields: schemaItem.fields,
            } as SchemaContainer
        });
    }


    public async listNonEmptyProjectSchemas(project: string, nodeUuid: string): Promise<SchemaContainer[]> {
        const projectSchemas = await this.listProjectSchemas(project)
        const projectSchemaNames = projectSchemas.map(schemaItem => schemaItem.name);

        // filter out all schemas with no elements
        const schemasWithNumElements = await this.getSchemasWithNumberOfElements(project, projectSchemaNames, nodeUuid);

        const schemasContainingElements = [];
        for(const schemaName in schemasWithNumElements) {
            if(schemasWithNumElements[schemaName]?.totalCount > 0) {
                schemasContainingElements.push(schemaName);
            }
        }

        const filteredProjectSchemas = projectSchemas.filter(schemaItem => schemasContainingElements
            .includes(schemaItem.name));

        return filteredProjectSchemas;
    }


    private async getSchemasWithNumberOfElements(project: string, schemas: string[], nodeUuid: string): Promise<NumberOfSchemaElements> {
        const response = await this.meshClient.graphql(project, {
            query: `
            query ($nodeUuid: String) {
    	        node(uuid: $nodeUuid) { 
                    ${this.constructSchemaFilterQuery(schemas)}
                }
            }    
            `,
            variables: {
                nodeUuid: nodeUuid,
            },
        });

        return response.data?.node;
    }

    private constructSchemaFilterQuery(schemas: Array<string>): string {
        const template = `[schemaName]: children(filter: {schema: {is: [schemaName]}}) { 
			totalCount 
		}` as string;

        const query = schemas.reduce((query: string, schemaName: string) => {
            // eslint-disable-next-line @typescript-eslint/restrict-plus-operands, @typescript-eslint/no-unsafe-call
            query += template.replaceAll('[schemaName]', schemaName) +'\n';
            return query;
        }, '')

        return query;
    }


    public async getRootNodeUuid(project: string): Promise<string> {
        const response = await this.meshClient.projects.list();

        return response.data.find(schemaItem => schemaItem.name === project).rootNode.uuid;
    }

    public async listNodeChildrenForSchema(project: string, params: MeshSchemaListParams, branchUuid?: string): Promise<SchemaElement[]>  {
        const queryPrams: GraphQLOptions = {
            version: 'draft',
        }
        if (branchUuid) {
            queryPrams.branch = branchUuid;
        }

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
                                isPublished
                                isDraft
                                version
                                versions {
                                    version, 
                                    published
                                    draft 
                                    created
                                }
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
        },
        queryPrams,
        );

        return response.data.node?.children?.elements
    }


    public async getSchemaNameForNode(project: string, nodeUuid: string): Promise<string>  {
        const schemaFieldsFilter: NodeLoadOptions = {
            fields: ['schema'],
        }

        const response = await this.meshClient.nodes.get(project, nodeUuid, schemaFieldsFilter);
        return response.schema.name;
    }


    public async getNodeByUuid(project: string, uuid: string, params?: NodeLoadOptions): Promise<NodeResponse> {
        // request for all project languages
        if (params?.lang) {
            const projectLanguages = await this.getProjectLanguages(project);
            params.lang += ',' + projectLanguages
                .filter(lang => lang.languageTag !== params.lang)
                .map(lang => lang.languageTag)
                .reduce((result, lang) => result += ','+lang);
        }

        const response = await this.meshClient.nodes.get(project, uuid, params);
        return response;
    }

    public async getProjectLanguages(project: string): Promise<Language[]> {
        const response = await this.meshClient.language.list(project);
        return response.data;
    }

}