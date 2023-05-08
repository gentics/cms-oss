import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { FolderItemType, FolderItemTypePlural, folderItemTypes, Language } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { Observable, of, throwError } from 'rxjs';
import {
    getExampleFolderData,
    getExampleLanguageData,
    getExampleNodeDataNormalized,
    getExamplePageData,
    getExampleUserDataNormalized,
} from '../../../../testing/test-data.mock';
import { emptyItemInfo, GtxChipSearchSearchFilterMap, ItemsInfo, plural } from '../../../common/models';
import { Api } from '../../../core/providers/api/api.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { MockErrorHandler } from '../../../core/providers/error-handler/error-handler.mock';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { QueryAssemblerElasticSearchService, QueryAssemblerGCMSSearchService } from '../../../shared/providers/query-assembler';
import { SetDisplayDeletedAction, STATE_MODULES } from '../../modules';
import { MockAppState, TestApplicationState } from '../../test-application-state.mock';
import { ApplicationStateService } from '../application-state/application-state.service';
import { FolderActionsService } from './folder-actions.service';

const ACTIVE_NODE_ID = 1;
const RESPONSE_ITEM_LIST = [{}, {}, {}];
const ACTIVE_LANGUAGE = {
    id: 3,
    code: 'ut',
    name: 'Unit Testian',
};

class MockI18nNotification {
    show = jasmine.createSpy();
}
class MockNavigationService {}
class MockPermissionService {
    normalizeAPIResponse = jasmine.createSpy('normalizeAPIResponse');
}
class MockI18nService {}

class MockApi {
    folders = {
        createFolder: jasmine.createSpy('createFolder').and.callFake((props: { name: string }) => {
            if (props.name === 'existing') {
                return throwError({ message: '' });
            } else {
                return of({ folder: getExampleFolderData() });
            }
        }),
        getFolders: createSpyApiMethod('getFolders', 'folders'),
        getPages: createSpyApiMethod('getPages', 'pages'),
        getFiles: createSpyApiMethod('getFiles', 'files'),
        getImages: createSpyApiMethod('getImages', 'files'),
        getItem: createSpyApiMethod('getItem', 'items'),
        getItems: createSpyApiMethod('getItems', 'items'),
        searchItems: createSpyApiMethod('searchItems', 'items'),
        updateItem: createSpyApiMethod('updateItem', 'items'),
    };
    publishQueue = {
        approvePageStatus: createSpyApiMethod('approvePageStatus', 'pages'),
    };
}

const createSpyApiMethod = (name: string, collectionKey: FolderItemTypePlural | 'items'): jasmine.Spy => jasmine.createSpy(name).and
    .returnValue(Observable.of({
        [collectionKey]: RESPONSE_ITEM_LIST,
        hasMoreItems: false,
        numItems: RESPONSE_ITEM_LIST.length,
    }),
    );

describe('FolderActionsService', () => {

    let folderActions: FolderActionsService;
    let api: MockApi;
    let permissions: MockPermissionService;
    let initialState: MockAppState;
    let state: TestApplicationState;
    let queryAssembler: QueryAssemblerElasticSearchService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                FolderActionsService,
                EntityResolver,
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: I18nNotification, useClass: MockI18nNotification },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: PermissionService, useClass: MockPermissionService },
                { provide: I18nService, useClass: MockI18nService },
                { provide: NavigationService, useClass: MockNavigationService },
                QueryAssemblerGCMSSearchService,
                QueryAssemblerElasticSearchService,
                { provide: Api, useClass: MockApi },
            ],
        });

        folderActions = TestBed.get(FolderActionsService);
        api = TestBed.get(Api);
        permissions = TestBed.get(PermissionService);
        state = TestBed.get(ApplicationStateService);
        queryAssembler = TestBed.get(QueryAssemblerElasticSearchService);

        initialState = {
            folder: {
                activeLanguage: ACTIVE_LANGUAGE.id,
                activeNode: ACTIVE_NODE_ID,
                pages: emptyItemInfo,
                folders: emptyItemInfo,
                files: emptyItemInfo,
                images: emptyItemInfo,
                searchFilters: {
                    nodeId: [
                        {
                            value: ACTIVE_NODE_ID,
                            operator: 'IS',
                        },
                    ],
                },
            },
            entities: {
                language: getExampleLanguageData(),
                node: {
                    [ACTIVE_NODE_ID]: getExampleNodeDataNormalized({ id: ACTIVE_NODE_ID }),
                },
                file: {},
                folder: {},
                form: {},
                image: {},
                page: {},
                user: {
                    1: getExampleUserDataNormalized({ id: 1 }),
                },
            },
        };

        state.mockState(initialState);
    });

    describe('updateItem()', () => {
        it('calls with right parameters if name was changed', fakeAsync(() => {
            let page = getExamplePageData();
            let payload = {
                name: 'Test Page',
            };
            let expectedPage = {...page};

            expectedPage.name = payload.name;
            folderActions.getItem = jasmine.createSpy('getItem').and.returnValue(Promise.resolve(expectedPage));

            folderActions.updateItem(page.type, page.id, payload).then(value => {
                expect(api.folders.updateItem).toHaveBeenCalledWith(page.type, page.id, payload, undefined);
                expect(folderActions.getItem).toHaveBeenCalledWith(page.id, page.type, { });
                expect(value).toEqual(expectedPage);
            });
        }));

        describe('updateItemChanges()', () => {
            it('calls with right parameters if niceUrl was removed', fakeAsync(() => {
                let page = getExamplePageData();
                page.niceUrl = 'Test';
                let payload: any = {
                    niceUrl: null,
                };
                let expectedPage = {...page};
                delete expectedPage.niceUrl;

                folderActions.getItem = jasmine.createSpy('getItem').and.returnValue(Promise.resolve(expectedPage));
                // spyOn(state.actions.folder, 'fetchItemSuccess');
                let result = folderActions.updateItem(page.type, page.id, payload);
                tick();
                expect(api.folders.updateItem).toHaveBeenCalledWith(page.type, page.id, payload, undefined);
                // expect(state.actions.folder.fetchItemSuccess).toHaveBeenCalledWith({ type: expectedPage.type, item: expectedPage });

                let promiseResolved = false;
                result.then(value => {
                    promiseResolved = true;
                    expect(value).toEqual(expectedPage);
                });
                tick();
                expect(promiseResolved).toBe(true);
            }));
        });
    });

    describe('pageQueuedApprove()', () => {
        const examplePage = getExamplePageData();

        it('calls the correct api method with right parameters for pages with queuedPublish', () => {
            examplePage.timeManagement.queuedPublish = {
                version: null,
                at: 0,
                user: {
                    email: '',
                    firstName: 'Test',
                    id: 35,
                    lastName: 'User',
                },
            };

            folderActions.pageQueuedApprove([examplePage]);
            expect(api.publishQueue.approvePageStatus).toHaveBeenCalledWith([examplePage.id]);
        });

        it('calls the correct api method with right parameters for pages with queuedOffline', () => {
            examplePage.timeManagement.queuedOffline = {
                at: 0,
                user: {
                    email: '',
                    firstName: 'Test',
                    id: 35,
                    lastName: 'User',
                },
            };

            folderActions.pageQueuedApprove([examplePage]);
            expect(api.publishQueue.approvePageStatus).toHaveBeenCalledWith([examplePage.id]);
        });
    });

    describe('refreshList()', () => {

        const PARENT_ID = 42;
        const expectedOptions = {
            maxItems: 10,
            search: '',
            recursive: false,
            langvars: true,
            template: true,
            sortby: 'name',
            sortorder: 'asc',
            skipCount: 10,
            nodeId: 1,
            wastebin: 'exclude',
            language: 'en',
            folderId: 42,
        };

        beforeEach(() => {
            state.mockState({
                folder: {
                    activeFolder: PARENT_ID,
                    searchTerm: '',
                    activeLanguage: 1,
                    pages: {
                        currentPage: 2,
                        itemsPerPage: 10,
                        total: 20,
                    },
                },
            });
        });

        it('calls the correct api method and options according to state', () => {
            folderActions.refreshList('page');
            expect(api.folders.getPages).toHaveBeenCalledWith(PARENT_ID, expectedOptions);
        });

        it('calls the correct api method and options according to state for multiple languages', () => {
            folderActions.refreshList('page', ['en', 'de']);

            const expectedOptionsFirst = {
                ...expectedOptions,
                language: 'en',
            };
            expect(api.folders.getPages).toHaveBeenCalledWith(PARENT_ID, expectedOptionsFirst);

            const expectedOptionsSecond = {
                ...expectedOptions,
                language: 'de',
            };
            expect(api.folders.getPages).toHaveBeenCalledWith(PARENT_ID, expectedOptionsSecond);
        });

    });

    describe('getItems()', () => {

        const PARENT_ID = 42;

        it('calls the correct api method for folders', () => {
            folderActions.getItems(PARENT_ID, 'folder');
            expect(api.folders.getFolders).toHaveBeenCalled();
        });

        it('calls the correct api method for pages', () => {
            folderActions.getItems(PARENT_ID, 'page');
            expect(api.folders.getPages).toHaveBeenCalled();
        });

        it('calls the correct api method for files', () => {
            folderActions.getItems(PARENT_ID, 'file');
            expect(api.folders.getFiles).toHaveBeenCalled();
        });

        it('calls the correct api method for images', () => {
            folderActions.getItems(PARENT_ID, 'image');
            expect(api.folders.getImages).toHaveBeenCalled();
        });

        it('adds nodeId, sortby and sortorder to the options', () => {
            folderActions.getItems(PARENT_ID, 'page');
            const expectedOptions = {
                nodeId: ACTIVE_NODE_ID,
                sortby: 'name',
                sortorder: 'asc',
                wastebin: 'exclude',
                recursive: false,
                langvars: true,
                folderId: 42,
            };
            expect(api.folders.getPages).toHaveBeenCalledWith(PARENT_ID, expectedOptions);
        });

        it('adds nodeId, sortby and sortorder to the options and includes deleted items', () => {
            state.dispatch(new SetDisplayDeletedAction(true));
            folderActions.getItems(PARENT_ID, 'page');
            const expectedOptions = {
                nodeId: ACTIVE_NODE_ID,
                sortby: 'name',
                sortorder: 'asc',
                wastebin: 'include',
                recursive: false,
                langvars: true,
                folderId: 42,
            };
            expect(api.folders.getPages).toHaveBeenCalledWith(PARENT_ID, expectedOptions);
        });

        it('adds privilegeMap = true to folder requests', () => {
            folderActions.getItems(PARENT_ID, 'folder');
            const expectedOptions = {
                nodeId: ACTIVE_NODE_ID,
                sortby: 'name',
                sortorder: 'asc',
                wastebin: 'exclude',
                recursive: false,
                folderId: 42,
                privilegeMap: true,
            };
            expect(api.folders.getFolders).toHaveBeenCalledWith(PARENT_ID, expectedOptions);
        });

        it('calls PermissionService.normalizeAPIResponse() on folder result', () => {
            const privilegeMap1 = {};
            const privilegeMap2 = {};

            api.folders.getFolders = jasmine.createSpy('folders.getFolders').and
                .returnValue(Observable.of({
                    folders: [{ privilegeMap: privilegeMap1 }, { privilegeMap: privilegeMap2 }],
                    hasMoreItems: false,
                    numItems: 3,
                }),
                );

            folderActions.getItems(PARENT_ID, 'folder');
            expect(permissions.normalizeAPIResponse).toHaveBeenCalledTimes(2);
            expect(permissions.normalizeAPIResponse.calls.argsFor(0)[0]).toBe(privilegeMap1);
            expect(permissions.normalizeAPIResponse.calls.argsFor(1)[0]).toBe(privilegeMap2);
        });

        describe('action calls', () => {
            function testActionCallFor(type: FolderItemType): void {
                folderActions.getItems(PARENT_ID, type, false);
                tick(500);

                const currentState = state.now.folder[plural[type]];
                expect(state.now.folder.lastError).toEqual('');
                expect(currentState).toEqual(jasmine.objectContaining({
                    creating: false,
                    fetching: false,
                    saving: false,
                    fetchAll: false,
                    hasMore: false,
                }));
            }

            it('calls fetchItemListSuccess for folders', fakeAsync(() => {
                testActionCallFor('folder');
            }));

            it('calls fetchItemListSuccess for pages', fakeAsync(() => {
                testActionCallFor('page');
            }));

            it('calls fetchItemListSuccess for files', fakeAsync(() => {
                testActionCallFor('file');
            }));

            it('calls fetchItemListSuccess for images', fakeAsync(() => {
                testActionCallFor('image');
            }));

        });

        describe('advanced search', () => {

            function setSearchFiltersState(opts: {
                searchFiltersVisible: boolean,
                searchFiltersValid: boolean,
                searchFiltersChanging: boolean,
                searchFilters: GtxChipSearchSearchFilterMap,
            }): void {
                state.mockState({
                    folder: {
                        searchFiltersVisible: opts.searchFiltersVisible,
                        searchFiltersValid: opts.searchFiltersValid,
                        searchFiltersChanging: opts.searchFiltersChanging,
                        searchFilters: opts.searchFilters,
                    },
                    features: {
                        always_localize: false,
                        elasticsearch: true,
                        nice_urls: false,
                        recent_items: false,
                    },
                });
            }

            it('does not use search endpoint if no search term is passed and node filter is set to current node', () => {
                setSearchFiltersState({
                    searchFiltersVisible: false,
                    searchFiltersValid: false,
                    searchFiltersChanging: false,
                    searchFilters: {
                        nodeId: [{ value: ACTIVE_NODE_ID, operator: 'IS' }],
                    },
                });
                folderActions.getItems(PARENT_ID, 'page');
                expect(api.folders.searchItems).not.toHaveBeenCalled();
            });

            it('uses search endpoint if filters exist and are valid', () => {
                setSearchFiltersState({
                    searchFiltersVisible: true,
                    searchFiltersValid: true,
                    searchFiltersChanging: false,
                    searchFilters: {
                        nodeId: [{ value: ACTIVE_NODE_ID, operator: 'IS' }],
                        templateId: [{ value: 1, operator: 'IS' }],
                    },
                });
                folderActions.getItems(PARENT_ID, 'page');
                expect(api.folders.searchItems).toHaveBeenCalledWith(
                    'page',
                    PARENT_ID,
                    {
                        query: {
                            bool: {
                                must: [{
                                    term: {
                                        templateId: 1,
                                    },
                                }],
                            },
                        },
                        from: 0,
                        size: undefined,
                        _source: false,
                    },
                    {
                        nodeId: 1,
                        sortby: 'name',
                        sortorder: 'asc',
                        wastebin: 'exclude',
                        recursive: true,
                        langvars: true,
                        folder: true,
                        folderId: 42,
                    },
                );
            });

            it('resets search filters when advanced search is closed', () => {
                setSearchFiltersState({
                    searchFiltersVisible: false,
                    searchFiltersValid: false,
                    searchFiltersChanging: false,
                    searchFilters: {
                        nodeId: [{ value: ACTIVE_NODE_ID, operator: 'IS' }],
                    },
                });
                folderActions.resetSearchFilters();
                expect(state.now.folder.searchFilters).toEqual({
                    nodeId: null,
                });
            });

        });
    });

    describe('actions which delegate to getItems()', () => {

        const PARENT_ID = 42;

        beforeEach(() => {
            folderActions.getItems = jasmine.createSpy('getItems');
        });

        folderItemTypes.forEach(type => {

            describe(`${getMethodName(type)}()`, () => {

                let getType: typeof FolderActionsService.prototype.getPages;

                beforeEach(() => {
                    getType = getItemsDelegate(type);
                });

                it('passes default options', () => {
                    getType(PARENT_ID);
                    expect(folderActions.getItems).toHaveBeenCalledWith(PARENT_ID, type, false, jasmine.objectContaining({
                        maxItems: 10,
                        search: '',
                        recursive: false,
                    }));
                });

                it('sets options for a searchTerm', () => {
                    getType(PARENT_ID, false, 'foo');
                    expect(folderActions.getItems).toHaveBeenCalledWith(PARENT_ID, type, false, jasmine.objectContaining({
                        maxItems: 10,
                        search: 'foo',
                        recursive: true,
                    }));
                });

            });

        });

        // eslint-disable-next-line prefer-arrow/prefer-arrow-functions
        function getMethodName(type: FolderItemType): keyof FolderActionsService {
            return `get${type.charAt(0).toUpperCase() + type.slice(1)}s` as any;
        }

        // eslint-disable-next-line prefer-arrow/prefer-arrow-functions
        function getItemsDelegate(type: FolderItemType): typeof FolderActionsService.prototype.getPages {
            return folderActions[getMethodName(type)].bind(folderActions);
        }

        it('getPages() passes correct default options', () => {
            folderActions.getPages(PARENT_ID);
            expect(folderActions.getItems).toHaveBeenCalledWith(PARENT_ID, 'page', false, {
                maxItems: 10,
                search: '',
                recursive: false,
                langvars: true,
                template: true,
                sortby: 'filename',
                skipCount: 0,
            });
        });

        it('getPages() passes correct language code', () => {
            state.mockState({
                entities: {
                    language: {
                        [ACTIVE_LANGUAGE.id]: ACTIVE_LANGUAGE,
                    },
                },
            });

            folderActions.getPages(PARENT_ID);
            expect(folderActions.getItems).toHaveBeenCalledWith(PARENT_ID, 'page', false, jasmine.objectContaining({
                language: ACTIVE_LANGUAGE.code,
            }));
        });

        it('setFilterTerm() loads additional items if necessary', () => {
            const hasMoreItemInfo: ItemsInfo = {
                ...emptyItemInfo,
                hasMore: true,
            };
            state.mockState({
                folder: {
                    pages: hasMoreItemInfo,
                    folders: hasMoreItemInfo,
                    files: hasMoreItemInfo,
                    images: hasMoreItemInfo,
                },
            });

            folderActions.getPages = jasmine.createSpy('getPages').and.stub();
            folderActions.getFolders = jasmine.createSpy('getFolders').and.stub();
            folderActions.getFiles = jasmine.createSpy('getFiles').and.stub();
            folderActions.getImages = jasmine.createSpy('getImages').and.stub();

            folderActions.setFilterTerm('test');
            expect(folderActions.getPages).toHaveBeenCalledWith(state.now.folder.activeFolder, true, state.now.folder.searchTerm);
            expect(folderActions.getFolders).toHaveBeenCalledWith(state.now.folder.activeFolder, true, state.now.folder.searchTerm);
            expect(folderActions.getFiles).toHaveBeenCalledWith(state.now.folder.activeFolder, true, state.now.folder.searchTerm);
            expect(folderActions.getImages).toHaveBeenCalledWith(state.now.folder.activeFolder, true, state.now.folder.searchTerm);
        });

        folderItemTypes.forEach(type => {

            it(`getItemsOfTypeInFolder() calls the right method for ${type}s`, () => {
                const typeSpecificMethod = getMethodName(type);
                spyOn(folderActions, typeSpecificMethod);
                folderActions.getAllItemsInFolder(PARENT_ID, '', true);
                expect(folderActions[typeSpecificMethod]).toHaveBeenCalledWith(PARENT_ID, true, '');
            });


            it(`getItemsOfTypeInFolder() calls the right method for ${type}s and searchTerm`, () => {
                const typeSpecificMethod = getMethodName(type);
                spyOn(folderActions, typeSpecificMethod);
                folderActions.getAllItemsInFolder(PARENT_ID, 'search', true);
                expect(folderActions[typeSpecificMethod]).toHaveBeenCalledWith(PARENT_ID, true, 'search');
            });

        });

    });

    describe('update actions', () => {
        const PAGE_ID = 12;
        const LANGUAGE: Language = { id: 2, name: 'English', code: 'en' };

        it('updatePageLanguage calls updateItem() with correct parameters', () => {
            folderActions.updatePageLanguage(PAGE_ID, LANGUAGE);
            expect(api.folders.updateItem).toHaveBeenCalledWith('page', PAGE_ID,
                {
                    id: PAGE_ID,
                    language: LANGUAGE.code,
                },
                {
                    createVersion: true,
                    unlock: true,
                    deriveFileName: true,
                });
        });
    });

    /**
     * Create a new folder
     */
    describe('createNewFolder()', () => {

        const mapCreateNewFolderModelToCreateFolderModel = (folder: {
            name: string,
            directory: string,
            description: string,
            parentFolderId: number,
            nodeId: number,
            failOnDuplicate?: boolean
        }) => {
            return {
                name: folder.name,
                publishDir: folder.directory,
                description: folder.description,
                nodeId: folder.nodeId,
                motherId: folder.parentFolderId,
                failOnDuplicate: folder.failOnDuplicate,
            };
        }

        it('creates correct folder', (done) => {
            const folder = {
                name: 'A new folder',
                directory: '/new-folder',
                description: 'A new folder',
                nodeId: 2222,
                parentFolderId: 1111,
            };
            folderActions.createNewFolder(folder).then((createdFolder) => {
                expect(createdFolder).not.toBeUndefined();
                expect(createdFolder ? createdFolder.name : '').toEqual('A new folder');
                done();
            })
            expect(api.folders.createFolder).toHaveBeenCalledWith(mapCreateNewFolderModelToCreateFolderModel(folder));
        });

        it('creates correct folder with optional failOnDuplicate flag', (done) => {
            const folder = {
                name: 'A new folder',
                directory: '/new-folder',
                description: 'A new folder',
                nodeId: 2222,
                parentFolderId: 1111,
                failOnDuplicate: true,
            };
            folderActions.createNewFolder(folder).then((createdFolder) => {
                expect(createdFolder).not.toBeUndefined();
                expect(createdFolder ? createdFolder.name : '').toEqual('A new folder');
                done();
            })
            expect(api.folders.createFolder).toHaveBeenCalledWith(mapCreateNewFolderModelToCreateFolderModel(folder));
        });

        it('emits void value if an error occurs', (done) => {
            const folder = {
                name: 'existing',
                directory: '/new-folder',
                description: 'A new folder',
                nodeId: 2222,
                parentFolderId: 1111,
                failOnDuplicate: true,
            };
            folderActions.createNewFolder(folder).then((createdFolder) => {
                expect(createdFolder).toBeUndefined();
                done();
            })
            expect(api.folders.createFolder).toHaveBeenCalledWith(mapCreateNewFolderModelToCreateFolderModel(folder));
        });
    });
});
