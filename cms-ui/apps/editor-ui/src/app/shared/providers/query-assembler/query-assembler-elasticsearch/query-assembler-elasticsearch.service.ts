import { Injectable } from '@angular/core';
import {
    GtxChipSearchProperties,
    GtxChipSearchPropertyBoolean,
    GtxChipSearchPropertyDate,
    GtxChipSearchPropertyKeys,
    GtxChipSearchPropertyNumber,
    GtxChipSearchPropertyObjectId,
    GtxChipSearchPropertyString,
    GtxChipSearchSearchFilterMap,
} from '@editor-ui/app/common/models';
import { ChipPropertyOptionValueElasticsearch } from '@editor-ui/app/shared/providers/chip-search-bar-config/chip-search-bar-config.models';
import { ApplicationStateService } from '@editor-ui/app/state';
import {
    ElasticSearchQuery,
    FolderItemType,
    GtxCmsQueryOptions,
    IndexById,
    Node,
    Normalized,
} from '@gentics/cms-models';
import { SearchQuery } from 'elastic-types/queries';
import {
    combineLatest,
    Observable,
} from 'rxjs';
import {
    filter,
    map,
} from 'rxjs/operators';
import {
    ELASTICSEARCH_INDEXED_PROPERTIES_FILE,
    ELASTICSEARCH_INDEXED_PROPERTIES_FOLDER,
    ELASTICSEARCH_INDEXED_PROPERTIES_FORM,
    ELASTICSEARCH_INDEXED_PROPERTIES_IMAGE,
    ELASTICSEARCH_INDEXED_PROPERTIES_PAGE,
} from './query-assembler-elasticsearch.models';

/** Functional method return value */
interface ElasticSearchQueryRequestPayload {
    options: GtxCmsQueryOptions;
    filtersForSearch: SearchQuery[];
    filtersForNotSearch: SearchQuery[];
    mustArray: SearchQuery[];
    mustNotArray: SearchQuery[];
}

/** for the 'all' query we define fields where the user's search term is likely to be found */
const SPECIFIED_QUERYALL_FIELDS: readonly GtxChipSearchPropertyKeys[] = Object.freeze([ 'name', 'path', 'description', 'content' ]);

/**
 * This service is dedicated to assemble Elastic Search request queries from
 * search filters defined in `GtxChipSearchSearchFilterMap`.
 * Main method `getQuery` returns POST request body and options. All other methods are
 * helper methods of `getQuery`. All `private static {SearchablePropertyName}FilterAdd` methods
 * translate filter definitions into parts of the Elastic Search query object.
 *
 * @Note Be aware that `getQuery` can emit `null` instead of queryData if query
 * sanitanization rules forbids queries of a specific `type`.
 */
@Injectable()
export class QueryAssemblerElasticSearchService {

    /** Paging: page index */
    from = 0;

    /** Maximum amount of search results returned by Elastic Search query */
    maxItems = 25;

    /** Include source */
    sourceInclude = false;

    /** State data: current nodeId */
    private activeNode$: Observable<number>;
    /** State data: all nodes */
    private node$: Observable<IndexById<Node<Normalized>>>;

    constructor(
        appState: ApplicationStateService,
    ) {
        this.activeNode$ = appState.select(state => state.folder.activeNode);
        this.node$ = appState.select(state => state.entities.node);
    }

    /** Clear properties from query the requested type doesn't have */
    private static sanitizeFilters(esFilters: GtxChipSearchSearchFilterMap, allowedProperties: readonly string[]): [ GtxChipSearchSearchFilterMap, boolean ] {
        let didSanitanization = false;
        // remove null values
        Object.entries(esFilters).forEach(([key, value]) => {
            if (value === null || value === undefined) {
                delete esFilters[key as any];
            }
        });
        // clone object
        const allowed = [ ...allowedProperties ];
        // add `all` virtual property
        allowed.push('all');
        // remove properties the requested type doesn't have
        Object.keys(esFilters).forEach(key => {
            if (!allowed.includes(key)) {
                delete esFilters[key];
                didSanitanization = true;
            }
        });
        return [ esFilters, didSanitanization ];
    }

    /** Translate `boolean` filter to ES query data */
    private static booleanFilterAdd(
        property: GtxChipSearchPropertyKeys,
        queryData: ElasticSearchQueryRequestPayload,
        filters: GtxChipSearchPropertyBoolean[],
        allNodes: IndexById<Node>,
    ): ElasticSearchQueryRequestPayload {
        filters.forEach(filter => {
            if (typeof filter.value === 'boolean') {
                const value = filter.value;
                // property `online` is not boolean but integer indicating node where it's published
                if (property === 'online') {
                    const allNodeIds: number[] = Object.keys(allNodes).map(nodeId => parseInt(nodeId, 10));
                    const filterOnline = { terms: { [property]: allNodeIds } } as any;
                    if (filter.value) {
                        queryData.filtersForSearch.push(filterOnline);
                    } else {
                        queryData.filtersForNotSearch.push(filterOnline);
                    }
                } else {
                    const filterBoolean = { term: { [property]: value } };
                    switch (filter.operator) {
                        case 'IS':
                            queryData.mustArray.push(filterBoolean);
                            break;
                        case 'IS_NOT':
                            queryData.mustNotArray.push(filterBoolean);
                            break;
                        default:
                            return;
                    }
                }
            }
        });
        return queryData;
    }

    /** Translate `number` filter to ES query data */
    private static numberFilterAdd(
        property: GtxChipSearchPropertyKeys,
        queryData: ElasticSearchQueryRequestPayload,
        filters: GtxChipSearchPropertyNumber[],
    ): ElasticSearchQueryRequestPayload {
        filters.forEach(filter => {
            if (/^\d+$/.test(filter.value.toString())) {
                if (filter.operator === 'IS') {
                    queryData.filtersForSearch.push({ term: { [property]: filter.value } });
                }
                if (filter.operator === 'IS_NOT') {
                    queryData.filtersForNotSearch.push({ term: { [property]: filter.value } });
                }
            }
        });
        return queryData;
    }

    /** Translate `objecttpye` filter to ES query data */
    private static objecttypeFilterAdd(
        property: GtxChipSearchPropertyKeys,
        queryData: ElasticSearchQueryRequestPayload,
        filters: GtxChipSearchPropertyObjectId[],
    ): ElasticSearchQueryRequestPayload {
        filters.forEach(filter => {
            if (filter.operator === 'IS') {
                queryData.filtersForSearch.push({ term: { [property]: filter.value } });
            }
            if (filter.operator === 'IS_NOT') {
                queryData.filtersForNotSearch.push({ term: { [property]: filter.value } });
            }
        });
        return queryData;
    }

    /** Translate `string` filter to ES query data */
    private static stringFilterAdd(
        property: GtxChipSearchPropertyKeys,
        queryData: ElasticSearchQueryRequestPayload,
        filters: GtxChipSearchPropertyString[],
    ): ElasticSearchQueryRequestPayload {
        filters.forEach(filter => {
            const filterValue = filter.value.toString();
            const rawProperty = property + '.raw';
            switch (filter.operator) {
                case 'CONTAINS':
                    queryData.mustArray.push({multi_match: {
                        fields: [property],
                        query: filterValue,
                    }});
                    break;
                case 'CONTAINS_NOT':
                    queryData.mustNotArray.push({multi_match: {
                        fields: [property],
                        query: filterValue,
                    }});
                    break;
                case 'IS':
                    queryData.mustArray.push({multi_match: {
                        fields: [rawProperty],
                        query: filterValue,
                    }});
                    break;
                case 'IS_NOT':
                    queryData.mustNotArray.push({multi_match: {
                        fields: [rawProperty],
                        query: filterValue,
                    }});
                    break;
                default:
                    return;
            }
        });
        return queryData;
    }

    /** Translate `string` filter to ES query data including a wildcard search */
    private static stringAndWildcardFilterAdd(
        property: GtxChipSearchPropertyKeys,
        queryData: ElasticSearchQueryRequestPayload,
        filters: GtxChipSearchPropertyString[],
    ): ElasticSearchQueryRequestPayload {
        filters.forEach(filter => {
            const filterValue = filter.value.toString();
            const rawProperty = property + '.raw';
            const queryPart: SearchQuery = {
                bool: {
                    should: [
                        {
                            multi_match: {
                                fields: [property],
                                query: filterValue,
                            },
                        },
                        {
                            wildcard: {
                                [rawProperty]: {
                                    value: '*' + filterValue.toLowerCase() + '*',
                                },
                            },
                        },
                    ],
                },
            };
            const exactQueryPart: SearchQuery = {
                multi_match: {
                    fields: [rawProperty],
                    query: filterValue,
                },
            };
            switch (filter.operator) {
                case 'CONTAINS':
                    queryData.mustArray.push(queryPart);
                    break;
                case 'CONTAINS_NOT':
                    queryData.mustNotArray.push(queryPart);
                    break;
                case 'IS':
                    queryData.mustArray.push(exactQueryPart);
                    break;
                case 'IS_NOT':
                    queryData.mustNotArray.push(exactQueryPart);
                    break;
                default:
                    return;
            }
        });
        return queryData;
    }

    /** Translate `string` filter to ES query data as wildcard search */
    private static wildcardFilterAdd(
        property: GtxChipSearchPropertyKeys,
        queryData: ElasticSearchQueryRequestPayload,
        filters: GtxChipSearchPropertyString[],
    ): ElasticSearchQueryRequestPayload {
        filters.forEach(filter => {
            const filterValue = filter.value.toString();
            let exactQueryPart: SearchQuery;

            const queryPart = {
                wildcard: {
                    [property]: {
                        value: '*' + filterValue.toLowerCase() + '*',
                    },
                },
            };

            if (property === 'filename') {
                exactQueryPart = {
                    multi_match: {
                        fields: ['name.raw'],
                        query: filterValue,
                    },
                };
            }

            switch (filter.operator) {
                case 'CONTAINS':
                    queryData.mustArray.push(queryPart);
                    break;
                case 'CONTAINS_NOT':
                    queryData.mustNotArray.push(queryPart);
                    break;
                case 'IS':
                    if (exactQueryPart) {
                        queryData.mustArray.push(exactQueryPart);
                    }
                    break;
                case 'IS_NOT':
                    if (exactQueryPart) {
                        queryData.mustNotArray.push(exactQueryPart);
                    }
                    break;
                default:
                    return;
            }
        });
        return queryData;
    }

    /** Translate `date` filter to ES query data */
    private static dateFilterAdd(
        property: GtxChipSearchPropertyKeys,
        queryData: ElasticSearchQueryRequestPayload,
        filters: GtxChipSearchPropertyDate[],
    ): ElasticSearchQueryRequestPayload {
        filters.forEach(filter => {
            const value = new Date(filter.value * 1000).toISOString().slice(0, 10);

            if (value) {
                let gte: string;
                let lte: string;

                switch (filter.operator) {
                    case 'AT':
                        gte = value;
                        lte = value;
                        break;
                    case 'AFTER':
                        gte = value;
                        break;
                    case 'BEFORE':
                        lte = value;
                        break;
                    default:
                        return;
                }

                const filterRange: any = {
                    range: {
                        [property]: {
                            format: 'yyyy-MM-dd',
                            ...( gte && ({ gte })),
                            ...( lte && ({ lte })),
                            // pages without time management have `publishAt: 0`, therefore add `gt: 0`
                            ...( lte && !gte && ({ gte: '1970-01-01' })),
                        },
                    },
                };
                queryData.filtersForSearch.push(filterRange);
            } else {
                throw Error(`Value ${filter.value} is not of type date.`);
            }
        });
        return queryData;
    }

    /** Translate `all` filter to ES query data */
    private static allFilterAdd(
        type: FolderItemType,
        queryData: ElasticSearchQueryRequestPayload,
        filters: GtxChipSearchPropertyString[],
        searchForAllFields: readonly GtxChipSearchPropertyKeys[],
    ): ElasticSearchQueryRequestPayload {
        // It is specified that property `name` shall be boosted here.
        const mappedSearchFields = searchForAllFields.map(field => {
            if (field === 'name') {
                // modify field identifer according to ES REST API:
                // https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-multi-match-query.html#CO120-1
                return `${field}^2`;
            } else {
                return field;
            }
        });

        filters.forEach(filter => {
            const filterValue = filter.value.toString();
            const queryPart = {
                bool: {
                    should: [],
                },
            };

            // search in "normal" attributes for all types
            queryPart.bool.should.push({
                multi_match: {
                    fields: mappedSearchFields,
                    query: filterValue,
                },
            });

            // add type specific wildcard searches
            const wildcardFilterValue = '*' + filterValue.toLowerCase() + '*';
            switch (type) {
                case 'folder':
                case 'form':
                    queryPart.bool.should.push({
                        wildcard: {
                            'name.raw': {
                                value: wildcardFilterValue,
                                boost: 2.0,
                            },
                        },
                    });
                    break;
                case 'page':
                    queryPart.bool.should.push({
                        wildcard: {
                            niceUrl: {
                                value: wildcardFilterValue,
                            },
                        },
                    }, {
                        wildcard: {
                            filename: {
                                value: wildcardFilterValue,
                                boost: 2.0,
                            },
                        },
                    }, {
                        wildcard: {
                            'name.raw': {
                                value: wildcardFilterValue,
                                boost: 2.0,
                            },
                        },
                    });
                    break;
                case 'file':
                case 'image':
                    queryPart.bool.should.push({
                        wildcard: {
                            filename: {
                                value: wildcardFilterValue,
                                boost: 2.0,
                            },
                        },
                    }, {
                        wildcard: {
                            'name.raw': {
                                value: wildcardFilterValue,
                                boost: 2.0,
                            },
                        },
                    });
                    break;
                default:
                    break;
            }
            switch (filter.operator) {
                case 'CONTAINS':
                    queryData.mustArray.push(queryPart);
                    break;
                case 'CONTAINS_NOT':
                    queryData.mustNotArray.push(queryPart);
                    break;
                default:
                    return;
            }
        });
        return queryData;
    }

    /** Translate `node` filter to ES query data */
    private static nodeFilterAdd(
        queryData: ElasticSearchQueryRequestPayload,
        filters: GtxChipSearchPropertyObjectId[],
        activeNodeId: number,
        allNodes: IndexById<Node>,
        parentId: number,
    ): ElasticSearchQueryRequestPayload {

        // If no `nodeId` filter active, presume that user wants to search in `activeNodeId`.
        const isActiveNodeId: boolean = filters.length === 0 || (filters.length === 1 && filters[0].value === activeNodeId);

        // If one filter active, use it.
        // Note: a query can only performed for one Node or all Nodes.
        const filterNodeIdParsed: number | null = filters.length === 1 && parseInt(filters[0].value as any, 10);
        const otherNode: Node<Normalized> | false = Number.isInteger(filterNodeIdParsed) && allNodes[filterNodeIdParsed];

        // Deleting properties for ES request shall result in performing query for all Nodes.
        const isSearchAll: boolean = filters.length > 1 || filters.some(f => f.value === 'all');

        if (isActiveNodeId) {
            queryData.options.nodeId = activeNodeId;
            queryData.options.folderId = parentId;
        } else if (!!otherNode && otherNode.folderId) {
            queryData.options.nodeId = otherNode.id;
            // todo: For unknown reasons CMS node of id `1` is the only node without `folderId`-property. Thus, check is required.
            queryData.options.folderId = otherNode.folderId;
        } else if (isSearchAll) {
            delete queryData.options.nodeId;
            delete queryData.options.folderId;
        } else {
            // If no information available, remove property entirely
            // since `folderId: undefined` will cause REST API to return `404` since `5.42.4`.
            delete queryData.options.folderId;
        }

        return queryData;
    }

    /** Translate `language` filter to ES query data */
    private static languageFilterAdd(
        type: FolderItemType,
        queryData: ElasticSearchQueryRequestPayload,
        filters: GtxChipSearchPropertyObjectId[],
    ): ElasticSearchQueryRequestPayload {
        if (type !== 'page') {
            return queryData;
        }

        // if all languages shall be queried, add no and remove all language-related queryData
        if (filters.some(f => f.value === 'all')) {
            return queryData;
        }

        filters.forEach(filter => {
            switch (filter.operator) {
                case 'IS':
                    queryData.filtersForSearch.push({
                        term: { languageCode: filter.value },
                    });
                    break;
                case 'IS_NOT':
                    queryData.filtersForNotSearch.push({
                        term: { languageCode: filter.value },
                    });
                    break;
                default:
                    return;
            }
        });
        return queryData;
    }

    /** Get request body payload and request options for Elastic Search query */
    getQuery(
        type: FolderItemType,
        parentId: number,
        filters: GtxChipSearchSearchFilterMap = {},
        options: GtxCmsQueryOptions = {} as any,
    ): Observable<[ ElasticSearchQuery, GtxCmsQueryOptions ] | null> {
        return combineLatest([
            this.activeNode$,
            this.node$,
        ]).pipe(
            // check if all data is available
            filter(([
                activeNodeId,
                allNodes,
            ]: [
                number,
                IndexById<Node<Normalized>>,
            ]) => (
                Number.isInteger(activeNodeId)
                && allNodes
                && Object.keys(allNodes).length > 0
            )),
            map(([activeNodeId, allNodes]) => {

                let esFilters = { ...filters };

                const preparedOptions: GtxCmsQueryOptions = { ...options };
                // we need folder data for breadcrumbs info
                preparedOptions.folder = true;
                // delete search term as this won't needed here this way
                delete preparedOptions.search;
                // always search recursively
                preparedOptions.recursive = true;
                // ES requests don't need `language`-options
                delete preparedOptions.language;

                // is TRUE if sanitanization has been performed, to decide whether query should be returned at all
                let didSanitanization = false;

                // sanitize filters
                switch (type) {
                    case 'file':
                        [esFilters, didSanitanization] = QueryAssemblerElasticSearchService.sanitizeFilters(
                            esFilters,
                            ELASTICSEARCH_INDEXED_PROPERTIES_FILE,
                        );
                        break;
                    case 'folder':
                        [esFilters, didSanitanization] = QueryAssemblerElasticSearchService.sanitizeFilters(
                            esFilters,
                            ELASTICSEARCH_INDEXED_PROPERTIES_FOLDER,
                        );
                        break;
                    case 'form':
                        [esFilters, didSanitanization] = QueryAssemblerElasticSearchService.sanitizeFilters(
                            esFilters,
                            ELASTICSEARCH_INDEXED_PROPERTIES_FORM,
                        );
                        break;
                    case 'image':
                        [esFilters, didSanitanization] = QueryAssemblerElasticSearchService.sanitizeFilters(
                            esFilters,
                            ELASTICSEARCH_INDEXED_PROPERTIES_IMAGE,
                        );
                        break;
                    case 'page':
                        [esFilters, didSanitanization] = QueryAssemblerElasticSearchService.sanitizeFilters(
                            esFilters,
                            ELASTICSEARCH_INDEXED_PROPERTIES_PAGE,
                        );
                        break;
                    default:
                        break;
                }

                // Assemble query
                const [queryPayload, assembledOptions] = this.assembleQuery(
                    type,
                    esFilters,
                    preparedOptions,
                    activeNodeId,
                    allNodes,
                    parentId,
                );

                // if query turns out to be empty because of sanitization, then suppress request at all
                if (didSanitanization) {
                    return null;
                } else {
                    return [queryPayload, assembledOptions];
                }
            }),
        );
    }

    /** Iterates over filter array and translates settings into ES query property */
    private assembleQuery(
        type: FolderItemType,
        esFilters: GtxChipSearchSearchFilterMap,
        options: GtxCmsQueryOptions,
        activeNodeId: number,
        allNodes: IndexById<Node>,
        parentId: number,
    ): [ ElasticSearchQuery, GtxCmsQueryOptions ] {

        const queryParts: ElasticSearchQueryRequestPayload = {
            options: { ...options },
            filtersForSearch: [],
            filtersForNotSearch: [],
            mustArray: [],
            mustNotArray: [],
        };
        // always add the parentId as folderId
        queryParts.options.folderId = parentId;
        let queryPartsEquipped: ElasticSearchQueryRequestPayload;

        Object.entries(esFilters).forEach(([paramfilterKey, paramFilters]: [
            ChipPropertyOptionValueElasticsearch, GtxChipSearchProperties[GtxChipSearchPropertyKeys]
        ]) => {
            // null check
            if (!Array.isArray(paramFilters)) {
                return;
            }

            // equip query filters according to searchable entity property
            switch (paramfilterKey) {
                case ChipPropertyOptionValueElasticsearch.all:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.allFilterAdd(
                        type,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyString[],
                        SPECIFIED_QUERYALL_FIELDS,
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.nodeId:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.nodeFilterAdd(
                        queryParts,
                        paramFilters as GtxChipSearchPropertyObjectId[],
                        activeNodeId,
                        allNodes,
                        parentId,
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.language:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.languageFilterAdd(
                        type,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyObjectId[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.id:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.objecttypeFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyObjectId[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.name:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.stringAndWildcardFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyString[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.filename:
                    if (type !== 'folder') {
                        queryPartsEquipped =
                            QueryAssemblerElasticSearchService.wildcardFilterAdd(
                                paramfilterKey,
                                queryParts,
                                paramFilters as GtxChipSearchPropertyString[]);
                    } else {
                        queryPartsEquipped = queryParts;
                    }
                    break;

                case ChipPropertyOptionValueElasticsearch.description:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.stringFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyString[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.content:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.stringFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyString[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.created:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.dateFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyDate[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.edited:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.dateFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyDate[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.published:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.dateFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyDate[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.creatorId:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.objecttypeFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyObjectId[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.editorId:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.objecttypeFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyObjectId[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.publisherId:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.objecttypeFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyObjectId[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.niceUrl:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.wildcardFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyString[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.templateId:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.objecttypeFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyObjectId[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.online:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.booleanFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyBoolean[],
                        allNodes,
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.modified:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.booleanFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyBoolean[],
                        allNodes,
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.queued:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.booleanFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyBoolean[],
                        allNodes,
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.planned:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.booleanFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyBoolean[],
                        allNodes,
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.publishAt:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.dateFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyDate[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.offlineAt:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.dateFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyDate[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.queuedPublishAt:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.dateFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyDate[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.queuedOfflineAt:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.dateFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyDate[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.systemCreationDate:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.dateFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyDate[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.customCreationDate:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.dateFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyDate[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.systemEditDate:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.dateFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyDate[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.customEditDate:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.dateFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyDate[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.path:
                    queryPartsEquipped = QueryAssemblerElasticSearchService.stringFilterAdd(
                        paramfilterKey,
                        queryParts,
                        paramFilters as GtxChipSearchPropertyString[],
                    );
                    break;

                case ChipPropertyOptionValueElasticsearch.objecttype:
                    // this is a pseudo-parameter which is not processed via HTTP-communication but in frontend application
                    break;

                default:
                    // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                    throw new Error(`Search-property "${paramfilterKey}" of entity "${type}" not defined.`);
            }
        });

        // add translated filters to query data
        Object.assign(queryParts.options, queryPartsEquipped?.options || {});
        queryParts.filtersForSearch.concat(queryPartsEquipped?.filtersForSearch || []);
        queryParts.filtersForNotSearch.concat(queryPartsEquipped?.filtersForNotSearch || []);
        queryParts.mustArray.concat(queryPartsEquipped?.mustArray || []);
        queryParts.mustNotArray.concat(queryPartsEquipped?.mustNotArray || []);

        // initialize Elastic Search request body object
        const query: ElasticSearchQuery = {
            query: {
                bool: {},
            },
            from: options.skipCount || this.from,
            // limit Elastic search results, related to https://jira.gentics.com/browse/SUP-8989
            size: options.maxItems === -1 ? this.maxItems : options.maxItems,
            _source: this.sourceInclude,
        };

        // assemble query from query parts
        if (queryParts.mustArray.length > 0) {
            query.query.bool.must = [
                ...queryParts.mustArray,
                ...queryParts.filtersForSearch,
            ];
        }
        if (queryParts.mustNotArray.length > 0) {
            query.query.bool.must_not = [
                ...queryParts.mustNotArray,
                ...queryParts.filtersForNotSearch,
            ];
        }
        if (queryParts.mustArray.length === 0) {
            if (queryParts.filtersForSearch.length > 0) {
                query.query.bool.must = queryParts.filtersForSearch;
            }
        }
        if (queryParts.mustNotArray.length === 0) {
            if (queryParts.filtersForNotSearch.length > 0) {
                query.query.bool.must_not = queryParts.filtersForNotSearch;
            }
        }

        return [query, queryParts.options];
    }

}
