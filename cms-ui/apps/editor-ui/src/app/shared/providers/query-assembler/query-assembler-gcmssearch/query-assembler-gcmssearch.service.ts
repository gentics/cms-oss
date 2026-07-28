import { Injectable } from '@angular/core';
import {
    FolderItemType,
    GtxCmsQueryOptions,
    IndexById,
    Node,
    Normalized,
} from '@gentics/cms-models';
import {
    combineLatest,
    Observable,
} from 'rxjs';
import {
    filter,
    map,
} from 'rxjs/operators';
import {
    GtxChipSearchProperty,
    GtxChipSearchPropertyBoolean,
    GtxChipSearchPropertyDate,
    GtxChipSearchPropertyKeys,
    GtxChipSearchPropertyNumber,
    GtxChipSearchPropertyObjectId,
    GtxChipSearchPropertyString,
    GtxChipSearchSearchFilterMap,
} from '../../../../common/models';
import { ChipPropertyOptionValueGcmssearch } from '../../../../shared/providers/chip-search-bar-config/chip-search-bar-config.models';
import { ApplicationStateService } from '../../../../state';
import {
    GCMSSEARCH_AVAILABLE_FILTERS_FILE,
    GCMSSEARCH_AVAILABLE_FILTERS_FOLDER,
    GCMSSEARCH_AVAILABLE_FILTERS_FORM,
    GCMSSEARCH_AVAILABLE_FILTERS_IMAGE,
    GCMSSEARCH_AVAILABLE_FILTERS_PAGE,
} from './query-assembler-gcmssearch.models';

/** For the 'all' query we define fields where the user's search term is likely to be found. */
const GCMSSEARCH_ALL_ENTITY_PROPERTIES: readonly (keyof GtxChipSearchSearchFilterMap)[] = Object.freeze([ 'search', 'q' ]);

/**
 * This service is dedicated to assemble GCMS search request options from
 * search filters defined in `GtxChipSearchSearchFilterMap`.
 * Main method `getOptions` returns URL query parameter options. All other methods are
 * helper methods of `getQuery`. All `private static {SearchablePropertyName}FilterAdd` methods
 * translate filter definitions into parts of the Elastic Search query object.
 *
 * @Note Be aware that `getOptions` can return `null` instead of queryData if query
 * sanitanization rules forbids queries of a specific `type`.
 */
@Injectable()
export class QueryAssemblerGCMSSearchService {

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

    /** Translate `boolean` filter to GCMS query data. */
    private static booleanFilterAdd(
        property: GtxChipSearchPropertyKeys,
        filters: GtxChipSearchPropertyBoolean[],
        options: GtxCmsQueryOptions,
    ): GtxCmsQueryOptions {
        const mappedOptions: GtxCmsQueryOptions = { ...options };
        let value: boolean;

        filters.forEach(filter => {
            value = filter.value;
        });

        mappedOptions[property] = value;
        return mappedOptions;
    }

    /** Translate `number` filter to GCMS query data. */
    private static numberFilterAdd(
        property: GtxChipSearchPropertyKeys,
        filters: GtxChipSearchPropertyNumber[],
        options: GtxCmsQueryOptions,
    ): GtxCmsQueryOptions {
        const mappedOptions: GtxCmsQueryOptions = { ...options };
        let value: number | 'all';

        filters.forEach(filter => {
            // type sanitanization
            const sanitizedInt = typeof filter.value === 'number' ? filter.value : parseInt(filter.value, 10);
            // `all` is not allowed for GCMS search here
            if (!Number.isInteger(sanitizedInt)) {
                return;
            }
            value = sanitizedInt;
        });

        mappedOptions[property] = value;
        return mappedOptions;
    }

    /** Translate `type` filter to GCMS query data. */
    private static objectidFilterAdd(
        property: GtxChipSearchPropertyKeys,
        filters: GtxChipSearchPropertyObjectId[],
        options: GtxCmsQueryOptions,
    ): GtxCmsQueryOptions {
        const mappedOptions: GtxCmsQueryOptions = { ...options };
        let value: number | string;

        filters.forEach(filter => {
            value = filter.value;
            // type sanitanization
            if (!value) {
                return;
            }
        });

        mappedOptions[property] = value;
        return mappedOptions;
    }

    /** Translate `string` filter to GCMS query data. */
    private static stringFilterAdd(
        property: GtxChipSearchPropertyKeys,
        filters: GtxChipSearchPropertyString[],
        options: GtxCmsQueryOptions,
    ): GtxCmsQueryOptions {
        const mappedOptions: GtxCmsQueryOptions = { ...options };
        let value: string;

        filters.forEach(filter => {
            value = filter.value;
        });

        mappedOptions[property] = value;
        return mappedOptions;
    }

    /** Translate `string` filter to GCMS query data including a wildcard search. */
    private static stringAndWildcardFilterAdd(
        property: GtxChipSearchPropertyKeys,
        filters: GtxChipSearchPropertyString[],
        options: GtxCmsQueryOptions,
    ): GtxCmsQueryOptions {
        const mappedOptions: GtxCmsQueryOptions = { ...options };
        let value: string;

        filters.forEach(filter => {
            switch (filter.operator) {
                case 'CONTAINS':
                    value = `%${filter.value}%`;
                    break;
                default:
                    throw new Error(`No definiton for operator "${filter.operator}".`);
            }
        });

        mappedOptions[property] = value;
        return mappedOptions;
    }

    /** Translate `string` filter to GCMS query data as RegExp search. */
    private static stringAndRegExFilterAdd(
        property: GtxChipSearchPropertyKeys,
        filters: GtxChipSearchPropertyString[],
        options: GtxCmsQueryOptions,
    ): GtxCmsQueryOptions {
        const mappedOptions: GtxCmsQueryOptions = { ...options };
        let value: string;

        filters.forEach(filter => {
            switch (filter.operator) {
                case 'CONTAINS':
                    value = `.*${filter.value}.*`;
                    break;
                default:
                    throw new Error(`No definiton for operator "${filter.operator}".`);
            }
        });

        mappedOptions[property] = value;
        return mappedOptions;
    }

    /** Translate `date` filter to GCMS query data. */
    private static dateFilterAdd(
        property: GtxChipSearchPropertyKeys,
        filters: GtxChipSearchPropertyDate[],
        options: GtxCmsQueryOptions,
    ): GtxCmsQueryOptions {
        const mappedOptions: GtxCmsQueryOptions = { ...options };
        // presume that naming-convention adheres to `propertyString`+`suffixString`
        const optionsKeyDateBefore = `${property}before`;
        const optionsKeyDateSince = `${property}since`;

        // convert JS Date to unix timestamp
        const convertToTimestamp = (v: Date): number => Math.floor( v.getTime() / 1000 );

        filters.forEach(filter => {
            switch (filter.operator) {
                case 'BEFORE':
                    mappedOptions[optionsKeyDateBefore] = filter.value;
                    break;

                case 'AFTER':
                    mappedOptions[optionsKeyDateSince] = filter.value;
                    break;

                case 'AT': {
                    // get 24h interval of day date
                    const dateValue = new Date(filter.value * 1000);
                    const dateStart = new Date(dateValue);
                    dateStart.setHours( 0, 0, 0, 0 );
                    const dateEnd = new Date(dateValue);
                    dateEnd.setHours( 23, 59, 59, 999 );

                    mappedOptions[optionsKeyDateSince] = convertToTimestamp(dateStart);
                    mappedOptions[optionsKeyDateBefore] = convertToTimestamp(dateEnd);
                    break;
                }

                default:
                    throw new Error(`No definiton for operator "${filter.operator}".`);
            }
        });

        return mappedOptions;
    }

    /** Translate `nodeId` filter to GCMS query data. */
    private static nodeFilterAdd(
        filters: GtxChipSearchPropertyObjectId[],
        options: GtxCmsQueryOptions,
        activeNodeId: number,
        allNodes: IndexById<Node>,
        parentId: number,
    ): GtxCmsQueryOptions {
        const mappedOptions: GtxCmsQueryOptions = { ...options };

        // If no `nodeId` filter active, presume that user wants to search in `activeNodeId`.
        const isActiveNodeId: boolean = filters.length === 0 || (filters.length === 1 && filters[0].value === activeNodeId);

        // If one filter active, use it.
        const filterNodeIdParsed: number | null = filters.length === 1 && parseInt(filters[0].value as any, 10);
        const otherNode: Node<Normalized> | false = Number.isInteger(filterNodeIdParsed) && allNodes[filterNodeIdParsed];

        if (isActiveNodeId) {
            mappedOptions.nodeId = activeNodeId;
            mappedOptions.folderId = parentId;
        } else if (otherNode) {
            mappedOptions.nodeId = otherNode.id;
            mappedOptions.folderId = otherNode.folderId;
        } else {
            // If no information available, remove property entirely
            // since `folderId: undefined` will cause REST API to return `404` since `5.42.4`.
            delete mappedOptions.folderId;
        }

        return mappedOptions;
    }

    /** Translate `all` filter to GCMS query data, which are multiple params of type `string`. */
    private allFilterAdd(
        filters: GtxChipSearchPropertyString[],
        options: GtxCmsQueryOptions,
    ): GtxCmsQueryOptions | null {
        const mappedOptions: GtxCmsQueryOptions = { ...options };

        filters.forEach(filter => {
            // for this param there is no `IS_NOT`
            if (filter.operator === 'CONTAINS') {
                GCMSSEARCH_ALL_ENTITY_PROPERTIES.forEach((field: string) => {
                    mappedOptions[field] = filter.value;
                });
                mappedOptions.searchcontent = true;
            }
        });

        return mappedOptions;
    }

    /**
     * Get request options with query parameters mapped from search-filter definitions.
     *
     * @param type itemType of CMS entity
     * @param parentFolderId folderId of containing folder
     * @param filters filter-information to be mapped to options object
     * @param options object to be modified
     * @returns options with query parameters to be used in request
     */
    getOptions(
        type: FolderItemType,
        parentFolderId: number,
        filters: GtxChipSearchSearchFilterMap,
        options: GtxCmsQueryOptions,
    ): Observable<GtxCmsQueryOptions> {
        return combineLatest([
            this.activeNode$,
            this.node$,
        ]).pipe(
            // check if all data is available
            filter(([activeNodeId, allNodes]) => (
                Number.isInteger(activeNodeId)
                && allNodes
                && Object.keys(allNodes).length > 0
            )),
            map(([activeNodeId, allNodes]) => {
                let mappedOptions: GtxCmsQueryOptions = { ...options };
                let isValidFilterForProperty = true;

                Object.entries(filters).forEach(([paramfilterKey, paramFilters]: [ChipPropertyOptionValueGcmssearch, GtxChipSearchProperty[]]) => {
                    // null check
                    if (!Array.isArray(paramFilters) || paramFilters.length === 0) {
                        return;
                    }

                    // if query-param of current entity-type is not available, then skip
                    if (!isValidFilterForProperty) {
                        return;
                    }

                    // check if query-param of current entity-type is available
                    switch (type) {
                        case 'page':
                            isValidFilterForProperty = GCMSSEARCH_AVAILABLE_FILTERS_PAGE.some(p => p === paramfilterKey);
                            mappedOptions.folder = true;
                            break;
                        case 'form':
                            isValidFilterForProperty = GCMSSEARCH_AVAILABLE_FILTERS_FORM.some(p => p === paramfilterKey);
                            mappedOptions.folder = true;
                            break;
                        case 'folder':
                            isValidFilterForProperty = GCMSSEARCH_AVAILABLE_FILTERS_FOLDER.some(p => p === paramfilterKey);
                            break;
                        case 'image':
                            isValidFilterForProperty = GCMSSEARCH_AVAILABLE_FILTERS_IMAGE.some(p => p === paramfilterKey);
                            mappedOptions.folder = true;
                            break;
                        case 'file':
                            isValidFilterForProperty = GCMSSEARCH_AVAILABLE_FILTERS_FILE.some(p => p === paramfilterKey);
                            mappedOptions.folder = true;
                            break;
                        default:
                            // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                            throw new Error(`GCMS type of "${type}" is not defined.`);
                    }

                    // if query-param of current entity-type is not available, then skip the rest
                    if (!isValidFilterForProperty) {
                        return;
                    }

                    // map filters to options
                    switch (paramfilterKey) {
                        case ChipPropertyOptionValueGcmssearch.all:
                            // special --> multiple query params
                            mappedOptions = this.allFilterAdd(
                                paramFilters as GtxChipSearchPropertyString[],
                                mappedOptions,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.nodeId:
                            // 'number'
                            mappedOptions = QueryAssemblerGCMSSearchService.nodeFilterAdd(
                                paramFilters as GtxChipSearchPropertyObjectId[],
                                mappedOptions,
                                activeNodeId,
                                allNodes,
                                parentFolderId,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.language:
                            // 'string'
                            mappedOptions = QueryAssemblerGCMSSearchService.objectidFilterAdd(
                                paramfilterKey,
                                paramFilters as GtxChipSearchPropertyObjectId[],
                                mappedOptions,
                            );
                            mappedOptions.langfallback = false;
                            break;
                        case ChipPropertyOptionValueGcmssearch.created:
                            // 'date'
                            mappedOptions = QueryAssemblerGCMSSearchService.dateFilterAdd(
                                paramfilterKey,
                                paramFilters as GtxChipSearchPropertyDate[],
                                mappedOptions,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.edited:
                            // 'date'
                            mappedOptions = QueryAssemblerGCMSSearchService.dateFilterAdd(
                                paramfilterKey,
                                paramFilters as GtxChipSearchPropertyDate[],
                                mappedOptions,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.published:
                            // 'date'
                            mappedOptions = QueryAssemblerGCMSSearchService.dateFilterAdd(
                                paramfilterKey,
                                paramFilters as GtxChipSearchPropertyDate[],
                                mappedOptions,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.filename:
                            // 'string' with SQL-syntax operators
                            mappedOptions = QueryAssemblerGCMSSearchService.stringAndWildcardFilterAdd(
                                paramfilterKey,
                                paramFilters as GtxChipSearchPropertyString[],
                                mappedOptions,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.niceurl:
                            // 'string' with RegExp-syntax operators
                            mappedOptions = QueryAssemblerGCMSSearchService.stringAndRegExFilterAdd(
                                paramfilterKey,
                                paramFilters as GtxChipSearchPropertyString[],
                                mappedOptions,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.modified:
                            // 'boolean'
                            mappedOptions = QueryAssemblerGCMSSearchService.booleanFilterAdd(
                                paramfilterKey,
                                paramFilters as GtxChipSearchPropertyBoolean[],
                                mappedOptions,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.online:
                            // 'boolean'
                            mappedOptions = QueryAssemblerGCMSSearchService.booleanFilterAdd(
                                paramfilterKey,
                                paramFilters as GtxChipSearchPropertyBoolean[],
                                mappedOptions,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.planned:
                            // 'boolean'
                            mappedOptions = QueryAssemblerGCMSSearchService.booleanFilterAdd(
                                paramfilterKey,
                                paramFilters as GtxChipSearchPropertyBoolean[],
                                mappedOptions,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.queued:
                            // 'boolean'
                            mappedOptions = QueryAssemblerGCMSSearchService.booleanFilterAdd(
                                paramfilterKey,
                                paramFilters as GtxChipSearchPropertyBoolean[],
                                mappedOptions,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.template_id:
                            // 'type' --> REST API can proceed array, but implementation here only provides integer
                            mappedOptions = QueryAssemblerGCMSSearchService.objectidFilterAdd(
                                paramfilterKey,
                                paramFilters as GtxChipSearchPropertyObjectId[],
                                mappedOptions,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.broken:
                            // 'boolean'
                            mappedOptions = QueryAssemblerGCMSSearchService.booleanFilterAdd(
                                paramfilterKey,
                                paramFilters as GtxChipSearchPropertyBoolean[],
                                mappedOptions,
                            );
                            break;
                        case ChipPropertyOptionValueGcmssearch.objecttype:
                            // this is a pseudo-parameter which is not processed via HTTP-communication but in frontend application
                            break;

                        default:
                            // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                            throw Error(`GCMS query parameter ${paramfilterKey} is not defined.`);
                    }
                });

                // if filter config is not valid query parameters for entity, return null
                if (isValidFilterForProperty) {
                    return mappedOptions;
                } else {
                    return null;
                }
            }),
        );
    }

}
