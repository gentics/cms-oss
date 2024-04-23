import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { GtxChipSearchPropertyDateOperators, GtxChipSearchSearchFilterMap } from '@editor-ui/app/common/models';
import { ApplicationStateService, STATE_MODULES } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { FolderItemType, GtxCmsQueryOptions } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { first } from 'rxjs/operators';
import { QueryAssemblerGCMSSearchService } from './query-assembler-gcmssearch.service';

describe('QueryAssemblerGCMSSearchService', () => {
    let state: TestApplicationState;
    let queryAssemblerService: QueryAssemblerGCMSSearchService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                QueryAssemblerGCMSSearchService,
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        queryAssemblerService = TestBed.inject(QueryAssemblerGCMSSearchService);
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
                searchFiltersVisible: true,
                searchFiltersValid: true,
            },
        };
        state.mockState(initialState);
    });

    it('should be created', fakeAsync(() => {
        tick();
        expect(queryAssemblerService).toBeDefined();
        expect(queryAssemblerService).toBeTruthy();
    }));

    it('correctly assembles single `nodeId` query and `language`', fakeAsync(() => {
        let requestOptionsAssembled: GtxCmsQueryOptions;
        const requestOptionsExpected: GtxCmsQueryOptions = {
            folderId: 2,
            nodeId: 2,
            language: 'en',
            langfallback: false,
        };
        const type: FolderItemType = 'page';
        const parentId = 2;
        const optionsInitial: GtxCmsQueryOptions = {
            folderId: 1,
        };
        const filters: GtxChipSearchSearchFilterMap = {
            nodeId: [{ value: 2, operator: 'IS' }],
            language: [ { value: 'en', operator: 'IS' } ],
        };

        queryAssemblerService.getOptions(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(assembled => {
            requestOptionsAssembled = assembled;
        });
        expect(requestOptionsAssembled).toEqual( requestOptionsExpected );
    }));

    it('correctly assembles `all` query', fakeAsync(() => {
        let requestOptionsAssembled: GtxCmsQueryOptions;
        const searchTerm = 'searchTermForTesting-00001';
        const requestOptionsExpected: GtxCmsQueryOptions = {
            folderId: 1,
            search: searchTerm,
            q: searchTerm,
            searchcontent: true,
            language: 'en',
            langfallback: false,
        };
        const type: FolderItemType = 'page';
        const parentId = 1;
        const optionsInitial: GtxCmsQueryOptions = {
            folderId: 1,
        };
        const filters: GtxChipSearchSearchFilterMap = {
            all: [{ value: searchTerm, operator: 'CONTAINS' }],
            language: [{ value: 'en', operator: 'IS' }],
        };

        queryAssemblerService.getOptions(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(assembled => {
            requestOptionsAssembled = assembled;
        });
        expect(requestOptionsAssembled).toEqual( requestOptionsExpected );
    }));

    it('correctly assembles `planned` query', fakeAsync(() => {
        let requestOptionsAssembled: GtxCmsQueryOptions;
        const requestOptionsExpected: GtxCmsQueryOptions = {
            folderId: 1,
            planned: true,
            language: 'en',
            langfallback: false,
        };
        const type: FolderItemType = 'page';
        const parentId = 1;
        const optionsInitial: GtxCmsQueryOptions = {
            folderId: 1,
        };
        const filters: GtxChipSearchSearchFilterMap = {
            planned: [{ value: true, operator: 'IS' }],
            language: [{ value: 'en', operator: 'IS' }],
        };

        queryAssemblerService.getOptions(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(assembled => {
            requestOptionsAssembled = assembled;
        });
        expect(requestOptionsAssembled).toEqual( requestOptionsExpected );
    }));

    it('correctly assembles string fields query `niceurl`', fakeAsync(() => {
        let requestOptionsAssembled: GtxCmsQueryOptions;
        const searchTerm = 'searchTermForTesting-00001';
        const requestOptionsExpected: GtxCmsQueryOptions = {
            folderId: 1,
            niceurl: `.*${searchTerm}.*`,
            language: 'en',
            langfallback: false,
        };
        const type: FolderItemType = 'page';
        const parentId = 1;
        const optionsInitial: GtxCmsQueryOptions = {
            folderId: 1,
        };
        const filters: GtxChipSearchSearchFilterMap = {
            niceurl: [{ value: searchTerm, operator: 'CONTAINS' }],
            language: [{ value: 'en', operator: 'IS' }],
        };

        queryAssemblerService.getOptions(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(assembled => {
            requestOptionsAssembled = assembled;
        });
        expect(requestOptionsAssembled).toEqual( requestOptionsExpected );
    }));

    it('correctly assembles string fields query `filename`', fakeAsync(() => {
        let requestOptionsAssembled: GtxCmsQueryOptions;
        const searchTerm = 'searchTermForTesting-00001';
        const requestOptionsExpected: GtxCmsQueryOptions = {
            folderId: 1,
            filename: `%${searchTerm}%`,
            language: 'en',
            langfallback: false,
        };
        const type: FolderItemType = 'page';
        const parentId = 1;
        const optionsInitial: GtxCmsQueryOptions = {
            folderId: 1,
        };
        const filters: GtxChipSearchSearchFilterMap = {
            filename: [{ value: searchTerm, operator: 'CONTAINS' }],
            language: [{ value: 'en', operator: 'IS' }],
        };

        queryAssemblerService.getOptions(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(assembled => {
            requestOptionsAssembled = assembled;
        });
        expect(requestOptionsAssembled).toEqual( requestOptionsExpected );
    }));

    function executeDateFieldsTest(
        stringField: keyof GtxChipSearchSearchFilterMap,
        operator: GtxChipSearchPropertyDateOperators,
        filters: GtxChipSearchSearchFilterMap,
        requestOptionsExpected: Partial<GtxCmsQueryOptions>,
    ): void {
        it(`correctly assembles date fields query '${stringField}' with operator '${operator}'`, fakeAsync(() => {
            let requestOptionsAssembled: GtxCmsQueryOptions;
            const mappedExpected: GtxCmsQueryOptions = {
                folderId: 1,
                ...requestOptionsExpected,
            };
            const type: FolderItemType = 'page';
            const parentId = 1;
            const optionsInitial: GtxCmsQueryOptions = {
                folderId: 1,
            };

            queryAssemblerService.getOptions(
                type,
                parentId,
                filters,
                optionsInitial,
            ).pipe(first()).subscribe(assembled => {
                requestOptionsAssembled = assembled;
            });
            tick();
            expect(requestOptionsAssembled).toEqual( mappedExpected );
        }));
    }
    [
        'created',
        'edited',
        'published',
    ].forEach(stringField => {
        const searchTerm = '2022-02-03';
        let operator: GtxChipSearchPropertyDateOperators;

        operator = 'BEFORE';
        executeDateFieldsTest(
            stringField,
            operator,
            { [stringField]: [{ value: searchTerm, operator }] },
            { [`${stringField}before` ]: 1643846400 },
        );
        operator = 'AFTER';
        executeDateFieldsTest(
            stringField,
            operator,
            { [stringField]: [{ value: searchTerm, operator }] },
            { [`${stringField}since` ]: 1643846400 },
        );
        operator = 'AT';
        /*
        This is a workaround, since the system's timezone influences the resulting dates.
        Since there was no good way to mock the timezone, we create the expected timestamps
        in the same way as they are created in the tested code.

        This test should be revised.
        Expected timestamps should be stated explicitly again and the timezone should be
        mocked.
        */
        const date = new Date(searchTerm);
        date.setHours( 0, 0, 0, 0 );
        const timestampSince: number = Math.floor( date.getTime() / 1000 );
        date.setHours( 23, 59, 59, 999 );
        const timestampBefore: number = Math.floor( date.getTime() / 1000 );
        executeDateFieldsTest(
            stringField,
            operator,
            { [stringField]: [{ value: searchTerm, operator }] },
            {
                [`${stringField}before` ]: timestampBefore,
                [`${stringField}since` ]: timestampSince,
            },
        );
    });

    it('correctly assembles mixed query', fakeAsync(() => {
        let requestOptionsAssembled: GtxCmsQueryOptions;
        const searchTerm = 'searchTermForTesting-00001';
        const searchDate = '2022-02-03';
        const requestOptionsExpected: any = {
            search: searchTerm,
            searchcontent: true,
            q: searchTerm,
            folderId: 2,
            nodeId: 2,
            filename: `%${searchTerm}%`,
            editedsince: 1643846400,
            language: 'de',
            langfallback: false,
        };
        const type: FolderItemType = 'page';
        const parentId = 1;
        const optionsInitial: GtxCmsQueryOptions = {
            folderId: 1,
        };
        const filters: GtxChipSearchSearchFilterMap = {
            all: [{ value: searchTerm, operator: 'CONTAINS' }],
            nodeId: [{ value: 2, operator: 'IS' }],
            filename: [{ value: searchTerm, operator: 'CONTAINS' }],
            edited: [{ value: searchDate as any, operator: 'AFTER' }],
            language: [{ value: 'de', operator: 'IS' }],
        };

        queryAssemblerService.getOptions(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(assembled => {
            requestOptionsAssembled = assembled;
        });
        expect(requestOptionsAssembled).toEqual( requestOptionsExpected );
    }));

    it('correctly returns `null` if query property is not allowed for entity', fakeAsync(() => {
        let requestOptionsAssembled: GtxCmsQueryOptions;
        const type: FolderItemType = 'folder';
        const parentId = 1;
        const optionsInitial: GtxCmsQueryOptions = {
            folderId: 1,
        };
        const filters: GtxChipSearchSearchFilterMap = {
            planned: [{ value: true, operator: 'IS' }],
        };

        queryAssemblerService.getOptions(
            type,
            parentId,
            filters,
            optionsInitial,
        ).pipe(first()).subscribe(assembled => {
            requestOptionsAssembled = assembled;
        });
        expect(requestOptionsAssembled).toBeNull();
    }));

});
