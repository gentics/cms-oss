import { Injectable } from '@angular/core';
import { GraphQLOptions } from '@gentics/mesh-models';
import { MeshRestClientService } from '@gentics/mesh-rest-client-angular';
import { chunk } from 'lodash-es';
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

    public async getSchemaNamesWithNodes(
        project: string,
        branch: string,
        node: string,
        languages: string[],
        schemaNames: string[],
    ): Promise<string[]> {
        // Technically, it's possible that mesh has thousands of schemas
        // We can't put them all into one gigantic graphql request however,
        // as grpahql will just simply abort it (too many tokens).
        // Therefore chunk them to max 150 at a time and process them in parallel
        // as good as possible.
        const schemaChunks = chunk(schemaNames, 150);

        const loaded = await Promise.all(schemaChunks.map(async singleChunk => {
            let query = `
                query ($nodeUuid: String, $lang: [String]) {
                    node(uuid: $nodeUuid, lang: $lang) {
                `;

            for (const schema of singleChunk) {
                query += `
                    ${schema}: children(perPage: 0, lang: $lang, filter: {schema:{ name: { equals: "${schema}" }}}) {
                        hasNextPage
                    }`;
            }

            query += `
                    }
                }`;

            const response = await this.meshClient.graphql(project, {
                query,
                variables: {
                    nodeUuid: node,
                    lang: languages,
                },
            }, {
                branch,
            }).send();

            return Object.entries(response?.data?.node || {}).reduce((acc, [schemaName, info]: [string, any]) => {
                if (info?.hasNextPage) {
                    acc.push(schemaName);
                }
                return acc;
            }, [] as string[]);
        }));

        return loaded.flatMap(names => names);
    }
}
