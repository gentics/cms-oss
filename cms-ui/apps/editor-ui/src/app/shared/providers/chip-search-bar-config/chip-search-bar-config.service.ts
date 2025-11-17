import { Injectable } from '@angular/core';
import {
    ItemType,
    Language,
    Node,
    Normalized,
    Raw,
    Template,
    User,
} from '@gentics/cms-models';
import { isEqual } from 'lodash-es';
import {
    BehaviorSubject,
    Observable,
    combineLatest,
    of,
} from 'rxjs';
import {
    distinctUntilChanged,
    filter,
    map,
    switchMap
} from 'rxjs/operators';
import {
    GtxChipInputSelectOption,
    GtxChipSearchChipOperatorOption,
    GtxChipSearchChipPropertyOption,
    GtxChipSearchConfig,
    GtxChipSearchPropertyKeys,
    GtxChipSearchSearchFilterMap,
} from '../../../common/models';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import {
    QueryAssemblerElasticSearchService,
    QueryAssemblerGCMSSearchService,
} from '../../../shared/providers/query-assembler';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import {
    ChipPropertyOptionValueElasticsearch,
    ChipPropertyOptionValueGcmssearch,
} from './chip-search-bar-config.models';

/* Reusable config parts **************************************************************************************************************** */

const RELATIONOPERATOR_IS: GtxChipSearchChipOperatorOption<GtxChipSearchPropertyKeys> = {
    value: 'IS',
    label: 'search.is',
};
const RELATIONOPERATOR_IS_NOT: GtxChipSearchChipOperatorOption<GtxChipSearchPropertyKeys> = {
    value: 'IS_NOT',
    label: 'search.is_not',
};
const RELATIONOPERATOR_CONTAINS: GtxChipSearchChipOperatorOption<GtxChipSearchPropertyKeys> = {
    value: 'CONTAINS',
    label: 'search.contains',
};
const RELATIONOPERATOR_CONTAINS_NOT: GtxChipSearchChipOperatorOption<GtxChipSearchPropertyKeys> = {
    value: 'CONTAINS_NOT',
    label: 'search.contains_not',
};
const RELATIONOPERATOR_AT: GtxChipSearchChipOperatorOption<GtxChipSearchPropertyKeys> = {
    value: 'AT',
    label: 'search.at',
};
const RELATIONOPERATOR_AFTER: GtxChipSearchChipOperatorOption<GtxChipSearchPropertyKeys> = {
    value: 'AFTER',
    label: 'search.after',
};
const RELATIONOPERATOR_BEFORE: GtxChipSearchChipOperatorOption<GtxChipSearchPropertyKeys> = {
    value: 'BEFORE',
    label: 'search.before',
};

const GCMSSEARCH_SEARCHABLE_PROPERTY_NICEURL: GtxChipSearchChipPropertyOption = {
    value: ChipPropertyOptionValueGcmssearch.niceurl,
    label: 'search.niceUrl',
    type: 'string',
    context: {
        relationOperators: [
            RELATIONOPERATOR_CONTAINS,
        ],
    },
};
const ELASTICSEARCH_SEARCHABLE_PROPERTY_NICEURL: GtxChipSearchChipPropertyOption = {
    value: ChipPropertyOptionValueElasticsearch.niceUrl,
    label: 'search.niceUrl',
    type: 'string',
    context: {
        relationOperators: [
            RELATIONOPERATOR_CONTAINS,
            RELATIONOPERATOR_CONTAINS_NOT,
        ],
    },
};

const GCMS_OBJECT_TYPES: readonly ItemType[] = Object.freeze([
    'folder',
    'page',
    'file',
    'image',
    'form',
]);

const GCMSSEARCH_SEARCHABLE_PROPERTY_OBJECTTYPE: GtxChipSearchChipPropertyOption = {
    value: 'objecttype',
    label: 'search.objecttype',
    type: 'objectid',
    context: {
        relationOperators: [
            RELATIONOPERATOR_IS,
            RELATIONOPERATOR_IS_NOT,
        ],
        inputselectOptionsAsync: of(GCMS_OBJECT_TYPES.map(typeIdentifier => ({
            value: typeIdentifier,
            label: `search.${typeIdentifier}`,
        }))),
    },
};

/**
 * # ChipSearchBarConfigService
 * ## Purpose
 * The purpose of this service is to provide an object configuring the
 * `ChipSearchBarComponent.chipSearchBarConfig`-property, defining user-facing
 * filtering options and translation values for corrresponding `GtxChipSearchSearchFilterMap`.
 *
 * @see {@link GtxChipSearchConfig GtxChipSearchConfig interface}
 * @sample
 * ```
 * <chip-search-bar
 *    [chipSearchBarConfig]="chipSearchBarConfigService.chipSearchBarConfig$ | async"
 * >
 * </chip-search-bar>
 * ```
 * ## Details
 * Config data defined here is rendered in {@link ChipSearchBarComponent} which
 * translates user-input into {@link GtxChipSearchSearchFilterMap} in
 * application state at `AppState.folder.searchFilters`.
 * Changing `AppState.folder.searchFilters` will cause {@link FolderActionsService.getItems} to request
 * entities via a QueryAssemblerService computing request options from said {@link GtxChipSearchSearchFilterMap}.
 * There are two QueryAssemblerServices implemented:
 * * {@link QueryAssemblerGCMSSearchService} for GCMS REST API
 * * {@link QueryAssemblerElasticSearchService} for ElasticSearch queries wrapped by GCMS REST API
 *
 * Emitted values of `chipSearchBarConfig$` vary
 * depending on application features defined in app state:
 *
 * ### Feature [nice_urls](https://gentics.com/Content.Node/cmp8/guides/feature_overview.html#nice-urls)
 * `chipSearchBarConfig$` shall contain configuration data for
 * `GtxChipSearchConfig.searchableProperties[].value === 'niceurl' | 'niceUrl'` only if
 * feature `nice_urls` is active; otherwise `GtxChipSearchSearchFilterMap` may contain
 * filters causing CMS REST API to return errors of class `400`.
 *
 * ### Feature [elastic_search](https://gentics.com/Content.Node/cmp8/guides/feature_elasticsearch.html)
 * Since the various QueryAssemblerServices provide different sets of query parameters,
 * `chipSearchBarConfig$` must provide different configuration for the QueryAssemblerService
 * to proceed determined in {@link FolderActionsService.getItems}.
 */
@Injectable()
export class ChipSearchBarConfigService {

    /** filter properties to choose from when creating a new filter chip */
    readonly chipSearchBarConfig$: Observable<GtxChipSearchConfig>;

    /** active NodeId as string */
    get activeNodeId(): string {
        return this.activeNodeIdInternal$.getValue();
    }
    private readonly activeNodeId$: Observable<string | null>;
    private readonly activeNodeIdInternal$ = new BehaviorSubject<string | null>(null);

    /** Input select option data to choose from CMS Nodes. */
    readonly cmsNodes$: Observable<GtxChipInputSelectOption[]>;

    /** Input select option data to choose from CMS Nodes including `all` option. */
    readonly cmsNodesAll$: Observable<GtxChipInputSelectOption[]>;

    /** active LanguageId as string */
    get activeLanguageId(): string {
        return this.activeLanguageIdInternal$.getValue();
    }
    private readonly activeLanguageId$: Observable<string | null>;
    private readonly activeLanguageIdInternal$ = new BehaviorSubject<string | null>(null);

    /** Input select option data to choose from CMS Languages. */
    readonly languages$: Observable<GtxChipInputSelectOption[]>;

    /** Input select option data to choose from CMS Languages including `all` option. */
    readonly languagesAll$: Observable<GtxChipInputSelectOption[]>;

    /** active UserId as string */
    readonly activeUserId$: Observable<string>;

    /** Input select option data to choose from CMS Users. */
    readonly users$: Observable<GtxChipInputSelectOption[]>;

    /** Input select option data to choose from CMS Templates. */
    readonly templates$: Observable<GtxChipInputSelectOption[]>;

    /** Config data for ChipSearchBarComponent for user to define queries to GCMS REST API. */
    private readonly gtxchipConfigGcmssearch: GtxChipSearchConfig = {
        searchableProperties: [],
    };

    /** Config data for ChipSearchBarComponent for user to define queries to ElasticSearch REST API. */
    private readonly gtxchipConfigElasticsearch: GtxChipSearchConfig = {
        searchableProperties: [],
    };

    /* CONSTRUCTOR ********************************************************************************************************************** */

    constructor(
        private state: ApplicationStateService,
        private entityResolver: EntityResolver,
        private folderActions: FolderActionsService,
    ) {

        /* PREPARE INPUT SELECT OPTION DATA ********************************************************************************************* */

        this.activeNodeId$ = this.state.select(state => state.folder.activeNode).pipe(
            distinctUntilChanged(isEqual),
            map(activeNode => activeNode?.toString()),
        );
        this.activeNodeId$.subscribe(activeNodeId => this.activeNodeIdInternal$.next(activeNodeId));

        this.cmsNodes$ = this.state.select(state => state.entities.node).pipe(
            distinctUntilChanged(isEqual),
            map(nodes => Object.values(nodes)
                .map((node: Node<Normalized>) => ({
                    value: node.id.toString(),
                    label: node.name,
                }))
                .sort((a, b) => a.label.localeCompare(b.label)),
            ),
        );
        this.cmsNodesAll$ = this.cmsNodes$.pipe(
            map((items: GtxChipInputSelectOption[]) => {
                items.unshift(<GtxChipInputSelectOption> { value: 'all', label: 'search.all_nodes' });
                return items;
            }),
        );

        this.activeLanguageId$ = this.state.select(state => state.folder.activeLanguage).pipe(
            distinctUntilChanged(isEqual),
            map(activeLanguageId => this.entityResolver.getLanguage(activeLanguageId)?.code),
        );
        this.activeLanguageId$.subscribe(activeLanguageId => this.activeLanguageIdInternal$.next(activeLanguageId));

        this.languages$ = this.state.select(state => state.entities.language).pipe(
            distinctUntilChanged(isEqual),
            map(languages => Object.values(languages)
                .map((language: Language) => ({
                    value: language.code,
                    label: language.name,
                }))
                .sort((a, b) => a.label.localeCompare(b.label)),
            ),
        );
        this.languagesAll$ = this.languages$.pipe(
            map((items: GtxChipInputSelectOption[]) => {
                items.unshift(<GtxChipInputSelectOption> { value: 'all', label: 'search.all_languages' });
                return items;
            }),
        );

        this.activeUserId$ = this.state.select(state => state.auth.currentUserId.toString());
        this.users$ = this.state.select(state => state.entities.user).pipe(
            distinctUntilChanged(isEqual),
            map(items => Object.values(items).map((user: User<Normalized>) => ({
                value: user.id.toString(),
                label: `${user.lastName} ${user.firstName}`,
            })).sort((a, b) => a.label.localeCompare(b.label))),
        );

        this.templates$ = this.state.select(state => state.folder.activeNode).pipe(
            distinctUntilChanged(isEqual),
            filter(activeNode => Number.isInteger(activeNode)),
            switchMap(activeNode => this.folderActions.getAllTemplatesOfNode(activeNode)),
            filter(templates => Array.isArray(templates)),
            map((templates: Template<Raw>[]) => templates.map(t => ({
                value: t.id.toString(),
                label: t.name,
            })).sort((a, b) => a.label.localeCompare(b.label))),
        );

        /* PROVIDE CONIG RETURN VALUE DEPENDING ON STATE PROPERTIES ******************************************************************** */

        this.chipSearchBarConfig$ = combineLatest([
            this.state.select(state => state.features.elasticsearch),
            this.state.select(state => state.features.nice_urls),
        ]).pipe(
            distinctUntilChanged(isEqual),
            map(([featureIsActiveElasticSearch, featureIsActiveNiceurls]) => {
                let retVal: GtxChipSearchConfig;

                // check feature elasticsearch
                if (featureIsActiveElasticSearch) {
                    retVal = { ...this.gtxchipConfigElasticsearch };

                    // check feature niceurls
                    if (featureIsActiveNiceurls) {
                        retVal.searchableProperties.push(ELASTICSEARCH_SEARCHABLE_PROPERTY_NICEURL);
                    }

                } else {
                    retVal = { ...this.gtxchipConfigGcmssearch };

                    // check feature niceurls
                    if (featureIsActiveNiceurls) {
                        retVal.searchableProperties.push(GCMSSEARCH_SEARCHABLE_PROPERTY_NICEURL);
                    }
                }

                return retVal;
            }),
        );

        /* ASSIGN INPUT SELECT OPTION DATA ********************************************************************************************** */

        /* GCMS Search */

        this.gtxchipConfigGcmssearch = {
            defaultFilters: [
                // {
                //     chipProperty: 'nodeId',
                //     chipOperator: 'IS',
                //     chipValue: this.activeNodeId,
                // },
            ],
            searchableProperties: [
                /**
                 * Refers to GCMS entity properties:
                 * * `search`
                 * * `q`
                 */
                {
                    value: ChipPropertyOptionValueGcmssearch.all,
                    label: 'search.all',
                    type: 'string',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_CONTAINS,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueGcmssearch.nodeId,
                    label: 'search.nodeId',
                    type: 'objectid',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                        ],
                        inputselectOptionsAsync: this.cmsNodes$,
                        inputselectOptionsDefaultValueAsync: this.activeNodeId$,
                    },
                },
                {
                    value: ChipPropertyOptionValueGcmssearch.language,
                    label: 'search.languageCode',
                    type: 'objectid',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                        ],
                        inputselectOptionsAsync: this.languages$,
                        inputselectOptionsDefaultValueAsync: this.activeLanguageId$,
                    },
                },
                {
                    value: ChipPropertyOptionValueGcmssearch.filename,
                    label: 'search.filename',
                    type: 'string',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_CONTAINS,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueGcmssearch.modified,
                    label: 'search.modified',
                    type: 'boolean',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueGcmssearch.online,
                    label: 'search.online',
                    type: 'boolean',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueGcmssearch.planned,
                    label: 'search.planned',
                    type: 'boolean',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueGcmssearch.queued,
                    label: 'search.queued',
                    type: 'boolean',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueGcmssearch.template_id,
                    label: 'search.template_id',
                    type: 'objectid',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                            RELATIONOPERATOR_IS_NOT,
                        ],
                        inputselectOptionsAsync: this.templates$,
                    },
                },
                {
                    value: ChipPropertyOptionValueGcmssearch.broken,
                    label: 'search.broken',
                    type: 'boolean',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueGcmssearch.created,
                    label: 'search.created',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueGcmssearch.edited,
                    label: 'search.edited',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueGcmssearch.published,
                    label: 'search.published',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                GCMSSEARCH_SEARCHABLE_PROPERTY_OBJECTTYPE,
            ],
        };

        /* Elastic Search */

        this.gtxchipConfigElasticsearch = {
            defaultFilters: [
                // {
                //     chipProperty: 'nodeId',
                //     chipOperator: 'IS',
                //     chipValue: this.activeNodeId,
                // },
            ],
            searchableProperties: [
                {
                    value: ChipPropertyOptionValueElasticsearch.all,
                    label: 'search.all',
                    type: 'string',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_CONTAINS,
                            RELATIONOPERATOR_CONTAINS_NOT,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.nodeId,
                    label: 'search.nodeId',
                    type: 'objectid',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                            RELATIONOPERATOR_IS_NOT,
                        ],
                        inputselectOptionsAsync: this.cmsNodesAll$,
                        inputselectOptionsDefaultValueAsync: this.activeNodeId$,
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.language,
                    label: 'search.languageCode',
                    type: 'objectid',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                            RELATIONOPERATOR_IS_NOT,
                        ],
                        inputselectOptionsAsync: this.languagesAll$,
                        inputselectOptionsDefaultValueAsync: this.activeLanguageId$,
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.id,
                    label: 'search.id',
                    type: 'number',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                            RELATIONOPERATOR_IS_NOT,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.name,
                    label: 'search.name',
                    type: 'string',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_CONTAINS,
                            RELATIONOPERATOR_CONTAINS_NOT,
                            RELATIONOPERATOR_IS,
                            RELATIONOPERATOR_IS_NOT,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.filename,
                    label: 'search.filename',
                    type: 'string',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_CONTAINS,
                            RELATIONOPERATOR_CONTAINS_NOT,
                            RELATIONOPERATOR_IS,
                            RELATIONOPERATOR_IS_NOT,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.description,
                    label: 'search.description',
                    type: 'string',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_CONTAINS,
                            RELATIONOPERATOR_CONTAINS_NOT,
                            RELATIONOPERATOR_IS,
                            RELATIONOPERATOR_IS_NOT,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.content,
                    label: 'search.content',
                    type: 'string',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_CONTAINS,
                            RELATIONOPERATOR_CONTAINS_NOT,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.created,
                    label: 'search.created',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.edited,
                    label: 'search.edited',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.published,
                    label: 'search.published',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.creatorId,
                    label: 'search.creatorId',
                    type: 'number',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                            RELATIONOPERATOR_IS_NOT,
                        ],
                        inputselectOptionsAsync: this.users$,
                        inputselectOptionsDefaultValueAsync: this.activeUserId$,
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.editorId,
                    label: 'search.editorId',
                    type: 'number',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                            RELATIONOPERATOR_IS_NOT,
                        ],
                        inputselectOptionsAsync: this.users$,
                        inputselectOptionsDefaultValueAsync: this.activeUserId$,
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.publisherId,
                    label: 'search.publisherId',
                    type: 'number',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                            RELATIONOPERATOR_IS_NOT,
                        ],
                        inputselectOptionsAsync: this.users$,
                        inputselectOptionsDefaultValueAsync: this.activeUserId$,
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.templateId,
                    label: 'search.template_id',
                    type: 'objectid',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                            RELATIONOPERATOR_IS_NOT,
                        ],
                        inputselectOptionsAsync: this.templates$,
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.online,
                    label: 'search.online',
                    type: 'boolean',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.modified,
                    label: 'search.modified',
                    type: 'boolean',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.queued,
                    label: 'search.queued',
                    type: 'boolean',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.planned,
                    label: 'search.planned',
                    type: 'boolean',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_IS,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.publishAt,
                    label: 'search.publishAt',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.offlineAt,
                    label: 'search.offlineAt',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.queuedPublishAt,
                    label: 'search.queuedPublishAt',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.queuedOfflineAt,
                    label: 'search.queuedOfflineAt',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.customCreationDate,
                    label: 'search.customCreationDate',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.customEditDate,
                    label: 'search.customEditDate',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.systemCreationDate,
                    label: 'search.systemCreationDate',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                {
                    value: ChipPropertyOptionValueElasticsearch.systemEditDate,
                    label: 'search.systemEditDate',
                    type: 'date',
                    context: {
                        relationOperators: [
                            RELATIONOPERATOR_AT,
                            RELATIONOPERATOR_AFTER,
                            RELATIONOPERATOR_BEFORE,
                        ],
                    },
                },
                GCMSSEARCH_SEARCHABLE_PROPERTY_OBJECTTYPE,
            ],
        };

    }

}
