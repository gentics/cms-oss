import { TestBed } from '@angular/core/testing';
import { GtxChipSearchSearchFilterMap } from '@editor-ui/app/common/models';
import { ApplicationStateService, STATE_MODULES } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { ElasticSearchQuery, FolderItemType, GtxCmsQueryOptions } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { first } from 'rxjs/operators';
import { QueryAssemblerElasticSearchService } from './query-assembler-elasticsearch.service';

describe('QueryAssemblerElasticSearchService', () => {
    let state: TestApplicationState;
    let queryAssemblerElasticSearchService: QueryAssemblerElasticSearchService;

    const maxItems = 25;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                QueryAssemblerElasticSearchService,
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        queryAssemblerElasticSearchService = TestBed.inject(QueryAssemblerElasticSearchService);
        state = TestBed.get(ApplicationStateService);

        const initialState = {
            entities: {
                node: {
                    1: { id: 1, folderId: 1, name: 'test-node-1', languagesId: [ 1, 2, 3 ] },
                    2: { id: 2, folderId: 2, name: 'test-node-2', languagesId: [ 1, 2 ] },
                    3: { id: 3, folderId: 3, name: 'test-node-3', languagesId: [ 1 ] },
                },
                language: {
                    1: { id: 1, code: 'en', name: 'English' },
                    2: { id: 2, code: 'de', name: 'German' },
                    3: { id: 3, code: 'fr', name: 'French' },
                },
            },
            folder: {
                activeNode: 1,
                activeLanguage: 1,
            },
        };
        state.mockState(initialState);
    });

    it('should be created', () => {
        expect(queryAssemblerElasticSearchService).toBeDefined();
        expect(queryAssemblerElasticSearchService).toBeTruthy();
    });

    it('correctly assembles single `nodeId` query (explicit) and `language` (explicit)', () => {
        const queryExpected: ElasticSearchQuery = {
            query: {
                bool: {
                    must: [{
                        term: {
                            languageCode: 'en',
                        },
                    }],
                },
            },
            from: 0,
            size: maxItems,
            _source: false,
        };
        const requestOptionsExpected: GtxCmsQueryOptions = {
            maxItems: -1,
            recursive: true,
            nodeId: 2,
            folder: true,
            folderId: 2,
        };

        const type: FolderItemType = 'page';
        const parentId = 2;
        const optionsInitial: GtxCmsQueryOptions = {
            maxItems: -1,
            folderId: 1,
        };
        const filters: GtxChipSearchSearchFilterMap = {
            nodeId: [{ value: 2, operator: 'IS' }],
            language: [ { value: 'en', operator: 'IS' } ],
        };

        queryAssemblerElasticSearchService.getQuery(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(queryData => {
            expect(queryData).toEqual([ queryExpected, requestOptionsExpected ]);
        });

    });

    it('correctly assembles all nodes query and `language`', () => {
        const queryExpected: ElasticSearchQuery = {
            query: {
                bool: {
                    must: [{
                        term: {
                            languageCode: 'en',
                        },
                    }],
                },
            },
            from: 0,
            size: maxItems,
            _source: false,
        };
        const requestOptionsExpected: any = {
            maxItems: -1,
            recursive: true,
            folder: true,
        };

        const type: FolderItemType = 'page';
        const parentId = 1;
        const optionsInitial: GtxCmsQueryOptions = {
            maxItems: -1,
            folderId: 1,

        };
        const filters: GtxChipSearchSearchFilterMap = {
            nodeId: [{ value: 'all', operator: 'IS' }],
            language: [ { value: 'en', operator: 'IS' } ],
        };

        queryAssemblerElasticSearchService.getQuery(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(queryData => {
            expect(queryData).toEqual([ queryExpected, requestOptionsExpected ]);
        });

    });

    it('correctly assembles all nodes query and all languages', () => {
        const queryExpected: ElasticSearchQuery = {
            query: {
                bool: {},
            },
            from: 0,
            size: maxItems,
            _source: false,
        };
        const requestOptionsExpected: any = {
            maxItems: -1,
            recursive: true,
            folder: true,
        };

        const type: FolderItemType = 'page';
        const parentId = 1;
        const optionsInitial: any = {
            maxItems: -1,
        };
        const filters: GtxChipSearchSearchFilterMap = {
            nodeId: [{ value: 'all', operator: 'IS' }],
            language: [{ value: 'all', operator: 'IS' }],
        };

        queryAssemblerElasticSearchService.getQuery(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(queryData => {
            expect(queryData).toEqual([ queryExpected, requestOptionsExpected ]);
        });

    });

    it('correctly assembles `all` query', () => {
        const searchTerm = 'searchTermForTesting-00001';
        const queryExpected: ElasticSearchQuery = {
            query: {
                bool: {
                    must: [{
                        bool: {
                            should: [{
                                multi_match: {
                                    fields: ['name^2', 'path', 'description', 'content'],
                                    query: searchTerm,
                                },
                            }, {
                                wildcard: {
                                    niceUrl: {
                                        value: '*' + searchTerm.toLowerCase() + '*',
                                    },
                                },
                            }, {
                                wildcard: {
                                    filename: {
                                        value: '*' + searchTerm.toLowerCase() + '*',
                                        boost: 2.0,
                                    },
                                },
                            }, {
                                wildcard: {
                                    'name.raw': {
                                        value: '*' + searchTerm.toLowerCase() + '*',
                                        boost: 2.0,
                                    },
                                },
                            }],
                        },
                    }, {
                        term: {
                            languageCode: 'en',
                        },
                    }],
                },
            },
            from: 0,
            size: maxItems,
            _source: false,
        };
        const requestOptionsExpected: GtxCmsQueryOptions = {
            maxItems: -1,
            recursive: true,
            folder: true,
            folderId: 1,
        };

        const type: FolderItemType = 'page';
        const parentId = 1;
        const optionsInitial: GtxCmsQueryOptions = {
            maxItems: -1,
            folderId: 1,
        };
        const filters: GtxChipSearchSearchFilterMap = {
            all: [{ value: searchTerm, operator: 'CONTAINS' }],
            language: [{ value: 'en', operator: 'IS' }],
        };

        queryAssemblerElasticSearchService.getQuery(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(queryData => {
            expect(queryData).toEqual([ queryExpected, requestOptionsExpected ]);
        });

    });

    it('correctly assembles `id` query', () => {
        const searchTerm = 42;
        const queryExpected: ElasticSearchQuery = {
            query: {
                bool: {
                    must: [{
                        term: {
                            id: searchTerm,
                        },
                    }, {
                        term: {
                            languageCode: 'en',
                        },
                    }],
                },
            },
            from: 0,
            size: maxItems,
            _source: false,
        };
        const requestOptionsExpected: GtxCmsQueryOptions = {
            maxItems: -1,
            recursive: true,
            folder: true,
            folderId: 1,
        };

        const type: FolderItemType = 'page';
        const parentId = 1;
        const optionsInitial: GtxCmsQueryOptions = {
            maxItems: -1,
            folderId: 1,
        };
        const filters: GtxChipSearchSearchFilterMap = {
            id: [{ value: searchTerm, operator: 'IS' }],
            language: [{ value: 'en', operator: 'IS' }],
        };

        queryAssemblerElasticSearchService.getQuery(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(queryData => {
            expect(queryData).toEqual([ queryExpected, requestOptionsExpected ]);
        });

    });

    it('correctly assembles string fields queries (`description`, `path`, `content`)', () => {
        function executeStringFieldsTest(stringField: keyof GtxChipSearchSearchFilterMap): void {
            const searchTerm = 'searchTermForTesting-00001';
            const queryExpected: ElasticSearchQuery = {
                query: {
                    bool: {
                        must: [{
                            multi_match: {
                                fields: [stringField as string],
                                query: searchTerm,
                            },
                        }, {
                            term: {
                                languageCode: 'en',
                            },
                        }],
                    },
                },
                from: 0,
                size: maxItems,
                _source: false,
            };
            const requestOptionsExpected: GtxCmsQueryOptions = {
                maxItems: -1,
                recursive: true,
                folder: true,
                folderId: 1,
            };

            const type: FolderItemType = 'page';
            const parentId = 1;
            const optionsInitial: GtxCmsQueryOptions = {
                maxItems: -1,
                folderId: 1,
            };
            const filters: GtxChipSearchSearchFilterMap = {
                [stringField as string]: [{ value: searchTerm, operator: 'CONTAINS' }],
                language: [{ value: 'en', operator: 'IS' }],
            };

            queryAssemblerElasticSearchService.getQuery(
                type,
                parentId,
                filters,
                optionsInitial,
            ).pipe(first()).subscribe(queryData => {
                expect(queryData).toEqual([ queryExpected, requestOptionsExpected ]);
            });
        }

        ['description', 'path', 'content'].forEach(executeStringFieldsTest);

    });

    it('correctly assembles wildcard fields queries (`filename`, `niceUrl`)', () => {
        function executeWildcardFieldsTest(stringField: keyof GtxChipSearchSearchFilterMap): void {
            const searchTerm = 'searchTermForTesting-00001';
            const queryExpected: ElasticSearchQuery = {
                query: {
                    bool: {
                        must: [{
                            wildcard: {
                                [stringField]: {
                                    value: '*' + searchTerm.toLowerCase() + '*',
                                },
                            },
                        }, {
                            term: {
                                languageCode: 'en',
                            },
                        }],
                    },
                },
                from: 0,
                size: maxItems,
                _source: false,
            };
            const requestOptionsExpected: GtxCmsQueryOptions = {
                maxItems: -1,
                recursive: true,
                folder: true,
                folderId: 1,
            };

            const type: FolderItemType = 'page';
            const parentId = 1;
            const optionsInitial: GtxCmsQueryOptions = {
                maxItems: -1,
                folderId: 1,
            };
            const filters: GtxChipSearchSearchFilterMap = {
                [stringField as string]: [{ value: searchTerm, operator: 'CONTAINS' }],
                language: [{ value: 'en', operator: 'IS' }],
            };

            queryAssemblerElasticSearchService.getQuery(
                type,
                parentId,
                filters,
                optionsInitial,
            ).pipe(first()).subscribe(queryData => {
                expect(queryData).toEqual([ queryExpected, requestOptionsExpected ]);
            });
        }

        ['filename', 'niceUrl'].forEach(executeWildcardFieldsTest);

    });

    it('correctly assembles string and wildcard fields queries (`name`)', () => {
        function executeStringAndWildcardFieldsTest(stringField: keyof GtxChipSearchSearchFilterMap): void {
            const searchTerm = 'searchTermForTesting-00001';
            const wildcardField = stringField + '.raw';
            const queryExpected: ElasticSearchQuery = {
                query: {
                    bool: {
                        must: [{
                            bool: {
                                should: [
                                    {
                                        multi_match: {
                                            fields: [stringField as string],
                                            query: searchTerm,
                                        },
                                    },
                                    {
                                        wildcard: {
                                            [wildcardField]: {
                                                value: '*' + searchTerm.toLowerCase() + '*',
                                            },
                                        },
                                    },
                                ],
                            },
                        }, {
                            term: {
                                languageCode: 'en',
                            },
                        }],
                    },
                },
                from: 0,
                size: maxItems,
                _source: false,
            };
            const requestOptionsExpected: GtxCmsQueryOptions = {
                maxItems: -1,
                recursive: true,
                folder: true,
                folderId: 1,
            };

            const type: FolderItemType = 'page';
            const parentId = 1;
            const optionsInitial: GtxCmsQueryOptions = {
                maxItems: -1,
                folderId: 1,
            };
            const filters: GtxChipSearchSearchFilterMap = {
                [stringField]: [{ value: searchTerm, operator: 'CONTAINS' as any }],
                language: [{ value: 'en', operator: 'IS' }],
            };

            queryAssemblerElasticSearchService.getQuery(
                type,
                parentId,
                filters,
                optionsInitial,
            ).pipe(first()).subscribe(queryData => {
                expect(queryData).toEqual([ queryExpected, requestOptionsExpected ]);
            });
        }

        ['name'].forEach(executeStringAndWildcardFieldsTest);

    });

    it('correctly assembles date fields queries (`created`, `edited`, `published`, `publishAt`, `offlineAt`, `queuedPublishAt`, `queuedOfflineAt`, `systemCreationDate`, `customCreationDate`, `systemEditDate`, `customEditDate`)', () => {
        function executeDateFieldsTest(stringField: keyof GtxChipSearchSearchFilterMap): void {
            const searchTerm = '2020-01-31';
            const queryExpected: ElasticSearchQuery = {
                query: {
                    bool: {
                        must: [{
                            range: {
                                [stringField]: {
                                    format: 'yyyy-MM-dd',
                                    gte: searchTerm,
                                },
                            },
                        }, {
                            term: {
                                languageCode: 'en',
                            },
                        }],
                    },
                },
                from: 0,
                size: maxItems,
                _source: false,
            };
            const requestOptionsExpected: GtxCmsQueryOptions = {
                maxItems: -1,
                recursive: true,
                folder: true,
                folderId: 1,
            };

            const type: FolderItemType = 'page';
            const parentId = 1;
            const optionsInitial: GtxCmsQueryOptions = {
                maxItems: -1,
                folderId: 1,
            };
            const filters: GtxChipSearchSearchFilterMap = {
                [stringField]: [{ value: searchTerm, operator: 'AFTER' as any }],
                language: [{ value: 'en', operator: 'IS' }],
            };

            queryAssemblerElasticSearchService.getQuery(
                type,
                parentId,
                filters,
                optionsInitial,
            ).pipe(first()).subscribe(queryData => {
                expect(queryData).toEqual([ queryExpected, requestOptionsExpected ]);
            });
        }

        [
            'created',
            'edited',
            'published',
            'publishAt',
            'offlineAt',
            'queuedPublishAt',
            'queuedOfflineAt',
            'systemCreationDate',
            'customCreationDate',
            'systemEditDate',
            'customEditDate',
        ].forEach(executeDateFieldsTest);

    });

    it('correctly assembles mixed query', () => {
        const searchTerm = 'searchTermForTesting-00001';
        const searchDate = '2020-01-31';
        const searchTemplateId = 42;
        const queryExpected: ElasticSearchQuery = {
            query: {
                bool: {
                    must: [{
                        bool: {
                            should: [
                                {
                                    multi_match: {
                                        fields: ['name'],
                                        query: searchTerm,
                                    },
                                },
                                {
                                    wildcard: {
                                        'name.raw': {
                                            value: '*' + searchTerm.toLowerCase() + '*',
                                        },
                                    },
                                },
                            ],
                        },
                    }, {
                        range: {
                            edited: {
                                format: 'yyyy-MM-dd',
                                gte: searchDate,
                                lte: searchDate,
                            },
                        },
                    }, {
                        term: {
                            languageCode: 'de',
                        },
                    }],
                    must_not: [{
                        term: {
                            templateId: searchTemplateId,
                        },
                    }],
                },
            },
            from: 0,
            size: maxItems,
            _source: false,
        };
        const requestOptionsExpected: any = {
            maxItems: -1,
            recursive: true,
            folder: true,
        };

        const type: FolderItemType = 'page';
        const parentId = 1;
        const optionsInitial: GtxCmsQueryOptions = {
            maxItems: -1,
            folderId: 1,
        };
        const filters: GtxChipSearchSearchFilterMap = {
            nodeId: [{ value: 'all', operator: 'IS' }],
            name: [{ value: searchTerm, operator: 'CONTAINS' }],
            edited: [{ value: searchDate, operator: 'AT' }],
            language: [{ value: 'de', operator: 'IS' }],
            templateId: [{ value: searchTemplateId, operator: 'IS_NOT' }],
        };

        queryAssemblerElasticSearchService.getQuery(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(queryData => {
            expect(queryData).toEqual([ queryExpected, requestOptionsExpected ]);
        });

    });

});
