import { Injectable } from '@angular/core';
import { GraphQLOptions } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import {
    MeshSchemaListParams,
    ResolvedParentNode,
    SchemaPage,
} from '../models/mesh-browser-models';

@Injectable()
export class MeshBrowserLoaderService {

    constructor(
        protected meshClient: MeshRestClientService,
    ) {}

    public async listNodeChildrenForSchema(
        project: string,
        params: MeshSchemaListParams,
        branchUuid?: string,
    ): Promise<SchemaPage> {
        const queryPrams: GraphQLOptions = {
            version: 'draft',
        };
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
                            hasNextPage
                            pageCount
                            currentPage
                            totalCount
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
                                availableLanguages
                            }
                        }
                    }
                }
            `,
            variables: params,
        }, queryPrams).send();

        return response.data?.node?.children || [];
    }

    public async getParentNode(
        project: string,
        params: MeshSchemaListParams,
        branchUuid: string,
    ): Promise<ResolvedParentNode> {
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        const response = await this.meshClient.graphql(
            project,
            {
                query: `
                query($nodeUuid: String, $lang: [String]) {
                    node(uuid: $nodeUuid, lang: $lang) {
                        uuid
                        availableLanguages
                        language
                        displayName

                        breadcrumb(lang: $lang) {
                            uuid
                            displayName
                            isContainer
                        }
                    }
                }
            `,
                variables: params,
            },
            {
                branch: branchUuid,
                version: 'draft',
            },
        ).send();

        return response.data?.node;
    }
}
