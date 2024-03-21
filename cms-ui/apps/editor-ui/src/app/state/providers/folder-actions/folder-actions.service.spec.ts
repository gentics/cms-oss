import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import {
    File as FileModel,
    FileUploadResponse,
    FolderItemType,
    FolderItemTypePlural,
    folderItemTypes,
    Language,
    NodeFeature,
} from '@gentics/cms-models';
import {
    getExampleFileData,
    getExampleFolderData,
    getExampleImageData,
    getExampleLanguageData,
    getExampleNodeDataNormalized,
    getExamplePageData,
    getExampleUserDataNormalized,
} from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { UploadResponse } from '@gentics/cms-rest-clients-angular';
import { ModalService } from '@gentics/ui-core';
import { NgxsModule } from '@ngxs/store';
import { Observable, of, throwError } from 'rxjs';
import { emptyItemInfo, GtxChipSearchSearchFilterMap, ItemsInfo, plural } from '../../../common/models';
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

const TEST_UPLOAD_FILE = new File(['bar'], 'foo.txt', {
    type: 'text/plain',
});
const TEST_UPLOAD_IMAGE = new File(['world'], 'hello.png', {
    type: 'image/png',
});

class MockI18nNotification {
    show = jasmine.createSpy();
}
class MockNavigationService {}
class MockPermissionService {
    normalizeAPIResponse = jasmine.createSpy('normalizeAPIResponse');
}
class MockI18nService {}

let uidCounter = 1;
const uidCache: Record<string, any> = {};

function fileToResponse(file: File): FileUploadResponse {
    return {
        success: true,
        file: {
            id: 123,
            fileType: file.type,
            fileSize: file.size,
            name: file.name,
        } as Partial<FileModel>,
    } as any;
}

function fileToUploadResponse(file: File): UploadResponse {
    // eslint-disable-next-line @typescript-eslint/restrict-plus-operands
    const uid = (uidCounter++) + '';
    uidCache[uid] = {
        file,
        name: file.name,
        type: file.type,
        size: file.size,
        dimensions: Promise.resolve({ width: 1, height: 1 }),
    };

    return {
        cancelled: false,
        error: null,
        name: file.name,
        response: fileToResponse(file),
        statusCode: 200,
        uid,
    }
}

class MockModalService {
    fromComponent = () => Promise.resolve({
        open: () => Promise.resolve(),
    });
}

function fakeListResponse(key: FolderItemTypePlural | 'items'): Observable<any> {
    return of({
        [key]: RESPONSE_ITEM_LIST,
        hasMoreItems: false,
        numItems: RESPONSE_ITEM_LIST.length,
    });
}

describe('FolderActionsService', () => {

    let folderActions: FolderActionsService;
    let client: GCMSRestClientService;
    let permissions: MockPermissionService;
    let initialState: MockAppState;
    let state: TestApplicationState;

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
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                QueryAssemblerGCMSSearchService,
                QueryAssemblerElasticSearchService,
                { provide: ModalService, useClass: MockModalService },
            ],
        });

        folderActions = TestBed.get(FolderActionsService);
        client = TestBed.inject(GCMSRestClientService);
        permissions = TestBed.get(PermissionService);
        state = TestBed.get(ApplicationStateService);

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
            features: {
                nodeFeatures: {
                    [ACTIVE_NODE_ID]: [],
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

        spyOn(client.folder, 'create').and.callFake((props: { name: string }) => {
            if (props.name === 'existing') {
                return throwError({ message: '' });
            } else {
                return of({ folder: getExampleFolderData() }) as any;
            }
        })
        spyOn(client.folder, 'folders').and.returnValue(fakeListResponse('folders'));
        spyOn(client.folder, 'pages').and.returnValue(fakeListResponse('pages'));
        spyOn(client.folder, 'files').and.returnValue(fakeListResponse('files'))
        spyOn(client.folder, 'images').and.returnValue(fakeListResponse('files'));
        spyOn(client.folder, 'items').and.returnValue(fakeListResponse('items'))
        spyOn(client.page, 'update').and.returnValue(fakeListResponse('items'));
        spyOn(client.page, 'publishQueueApprove').and.returnValue(fakeListResponse('pages'));
        spyOn(client.elasticSearch, 'search').and.returnValue(fakeListResponse('items'));
        // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
        spyOn(client.file, 'upload').and.callFake((file) => of(fileToResponse(file as any)));
        // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
        spyOn(client.file, 'uploadTo').and.callFake((_, file) => of(fileToResponse(file as any)));
        spyOn(client.image, 'get').and.callFake((id) => of({
            // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
            image: getExampleImageData({ id: id as any }),
            responseInfo: null,
            messages: [],
        }));
        spyOn(client.file, 'get').and.callFake((id) => of({
            // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
            file: getExampleFileData({ id: id as any }),
            responseInfo: null,
            messages: [],
        }));
    });

    describe('updateItem()', () => {
        it('calls with right parameters if name was changed', fakeAsync(() => {
            const page = getExamplePageData();
            const payload = {
                name: 'Test Page',
            };
            const expectedPage = {...page};

            expectedPage.name = payload.name;
            folderActions.getItem = jasmine.createSpy('getItem').and.returnValue(Promise.resolve(expectedPage));

            folderActions.updateItem(page.type, page.id, payload, { }).then(value => {
                expect(client.page.update).toHaveBeenCalledWith(page.id, { page: payload, deriveFileName: true });
                expect(folderActions.getItem).toHaveBeenCalledWith(page.id, page.type);
                expect(value).toEqual(expectedPage);
            });
        }));

        describe('updateItemChanges()', () => {
            it('calls with right parameters if niceUrl was removed', fakeAsync(() => {
                const page = getExamplePageData();
                page.niceUrl = 'Test';
                const payload: any = {
                    niceUrl: null,
                };
                const expectedPage = {...page};
                delete expectedPage.niceUrl;

                folderActions.getItem = jasmine.createSpy('getItem').and.returnValue(Promise.resolve(expectedPage));
                // spyOn(state.actions.folder, 'fetchItemSuccess');
                const result = folderActions.updateItem(page.type, page.id, payload);
                tick();
                expect(client.page.update).toHaveBeenCalledWith(page.id, { page: payload, deriveFileName: true });
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

        it('calls the correct api method with right parameters for pages with queuedPublish', fakeAsync(() => {
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
            tick();

            expect(client.page.publishQueueApprove).toHaveBeenCalledWith(examplePage.id);
        }));

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
            expect(client.page.publishQueueApprove).toHaveBeenCalledWith(examplePage.id);
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

        it('calls the correct api method and options according to state', fakeAsync(() => {
            folderActions.refreshList('page');
            tick();
            expect(client.folder.pages).toHaveBeenCalledWith(PARENT_ID, expectedOptions as any);
        }));

        it('calls the correct api method and options according to state for multiple languages', fakeAsync(() => {
            folderActions.refreshList('page', ['en', 'de']);
            tick();

            const expectedOptionsFirst = {
                ...expectedOptions,
                language: 'en',
            };
            expect(client.folder.pages).toHaveBeenCalledWith(PARENT_ID, expectedOptionsFirst as any);

            const expectedOptionsSecond = {
                ...expectedOptions,
                language: 'de',
            };
            expect(client.folder.pages).toHaveBeenCalledWith(PARENT_ID, expectedOptionsSecond as any);
        }));

    });

    describe('getItems()', () => {

        const PARENT_ID = 42;

        it('calls the correct api method for folders', fakeAsync(() => {
            folderActions.getItems(PARENT_ID, 'folder');
            tick();
            expect(client.folder.folders).toHaveBeenCalled();
        }));

        it('calls the correct api method for pages', fakeAsync(() => {
            folderActions.getItems(PARENT_ID, 'page');
            tick();
            expect(client.folder.pages).toHaveBeenCalled();
        }));

        it('calls the correct api method for files', fakeAsync(() => {
            folderActions.getItems(PARENT_ID, 'file');
            tick();
            expect(client.folder.files).toHaveBeenCalled();
        }));

        it('calls the correct api method for images', fakeAsync(() => {
            folderActions.getItems(PARENT_ID, 'image');
            tick();
            expect(client.folder.images).toHaveBeenCalled();
        }));

        it('adds nodeId, sortby and sortorder to the options', fakeAsync(() => {
            folderActions.getItems(PARENT_ID, 'page');
            tick();
            const expectedOptions = {
                nodeId: ACTIVE_NODE_ID,
                sortby: 'name',
                sortorder: 'asc',
                wastebin: 'exclude',
                recursive: false,
                langvars: true,
                folderId: 42,
            };
            expect(client.folder.pages).toHaveBeenCalledWith(PARENT_ID, expectedOptions as any);
        }));

        it('adds nodeId, sortby and sortorder to the options and includes deleted items', fakeAsync(() => {
            state.dispatch(new SetDisplayDeletedAction(true));
            tick();
            folderActions.getItems(PARENT_ID, 'page');
            tick();
            const expectedOptions = {
                nodeId: ACTIVE_NODE_ID,
                sortby: 'name',
                sortorder: 'asc',
                wastebin: 'include',
                recursive: false,
                langvars: true,
                folderId: 42,
            };
            expect(client.folder.pages).toHaveBeenCalledWith(PARENT_ID, expectedOptions as any);
        }));

        it('adds privilegeMap = true to folder requests', fakeAsync(() => {
            folderActions.getItems(PARENT_ID, 'folder');
            tick();
            const expectedOptions = {
                nodeId: ACTIVE_NODE_ID,
                sortby: 'name',
                sortorder: 'asc',
                wastebin: 'exclude',
                recursive: false,
                folderId: 42,
                privilegeMap: true,
            };
            expect(client.folder.folders).toHaveBeenCalledWith(PARENT_ID, expectedOptions as any);
        }));

        it('calls PermissionService.normalizeAPIResponse() on folder result', fakeAsync(() => {
            const privilegeMap1 = {};
            const privilegeMap2 = {};

            client.folder.folders = jasmine.createSpy('folders.getFolders').and
                .returnValue(of({
                    folders: [{ privilegeMap: privilegeMap1 }, { privilegeMap: privilegeMap2 }],
                    hasMoreItems: false,
                    numItems: 3,
                }),
                );

            folderActions.getItems(PARENT_ID, 'folder');
            tick();
            expect(permissions.normalizeAPIResponse).toHaveBeenCalledTimes(2);
            expect(permissions.normalizeAPIResponse.calls.argsFor(0)[0]).toBe(privilegeMap1);
            expect(permissions.normalizeAPIResponse.calls.argsFor(1)[0]).toBe(privilegeMap2);
        }));

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

            it('does not use search endpoint if no search term is passed and node filter is set to current node', fakeAsync(() => {
                setSearchFiltersState({
                    searchFiltersVisible: false,
                    searchFiltersValid: false,
                    searchFiltersChanging: false,
                    searchFilters: {
                        nodeId: [{ value: ACTIVE_NODE_ID, operator: 'IS' }],
                    },
                });
                folderActions.getItems(PARENT_ID, 'page');
                tick();
                expect(client.elasticSearch.search).not.toHaveBeenCalled();
            }));

            it('uses search endpoint if filters exist and are valid', fakeAsync(() => {
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
                tick();

                expect(client.elasticSearch.search).toHaveBeenCalledWith(
                    'page',
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
                    } as any,
                );
            }));

            it('resets search filters when advanced search is closed', fakeAsync(() => {
                setSearchFiltersState({
                    searchFiltersVisible: false,
                    searchFiltersValid: false,
                    searchFiltersChanging: false,
                    searchFilters: {
                        nodeId: [{ value: ACTIVE_NODE_ID, operator: 'IS' }],
                    },
                });
                folderActions.resetSearchFilters();
                tick();

                expect(state.now.folder.searchFilters).toEqual({
                    nodeId: null,
                });
            }));

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

                it('passes default options', fakeAsync(() => {
                    getType(PARENT_ID);
                    tick();

                    expect(folderActions.getItems).toHaveBeenCalledWith(PARENT_ID, type, false, jasmine.objectContaining({
                        maxItems: 10,
                        search: '',
                        recursive: false,
                    }));
                }));

                it('sets options for a searchTerm', fakeAsync(() => {
                    getType(PARENT_ID, false, 'foo');
                    tick();

                    expect(folderActions.getItems).toHaveBeenCalledWith(PARENT_ID, type, false, jasmine.objectContaining({
                        maxItems: 10,
                        search: 'foo',
                        recursive: true,
                    }));
                }));
            });

        });

        function getMethodName(type: FolderItemType): keyof FolderActionsService {
            return `get${type.charAt(0).toUpperCase() + type.slice(1)}s` as any;
        }

        function getItemsDelegate(type: FolderItemType): typeof FolderActionsService.prototype.getPages {
            return folderActions[getMethodName(type)].bind(folderActions);
        }

        it('getPages() passes correct default options', fakeAsync(() => {
            folderActions.getPages(PARENT_ID);
            tick();

            expect(folderActions.getItems).toHaveBeenCalledWith(PARENT_ID, 'page', false, {
                maxItems: 10,
                search: '',
                recursive: false,
                langvars: true,
                template: true,
                sortby: 'filename',
                skipCount: 0,
            } as any);
        }));

        it('getPages() passes correct language code', fakeAsync(() => {
            state.mockState({
                entities: {
                    language: {
                        [ACTIVE_LANGUAGE.id]: ACTIVE_LANGUAGE,
                    },
                },
            });

            folderActions.getPages(PARENT_ID);
            tick();

            expect(folderActions.getItems).toHaveBeenCalledWith(PARENT_ID, 'page', false, jasmine.objectContaining({
                language: ACTIVE_LANGUAGE.code,
            }));
        }));

        it('setFilterTerm() loads additional items if necessary', fakeAsync(() => {
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
            tick();

            expect(folderActions.getPages).toHaveBeenCalledWith(state.now.folder.activeFolder, true, state.now.folder.searchTerm);
            expect(folderActions.getFolders).toHaveBeenCalledWith(state.now.folder.activeFolder, true, state.now.folder.searchTerm);
            expect(folderActions.getFiles).toHaveBeenCalledWith(state.now.folder.activeFolder, true, state.now.folder.searchTerm);
            expect(folderActions.getImages).toHaveBeenCalledWith(state.now.folder.activeFolder, true, state.now.folder.searchTerm);
        }));

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

        it('updatePageLanguage calls updateItem() with correct parameters', fakeAsync(() => {
            folderActions.updatePageLanguage(PAGE_ID, LANGUAGE);
            tick();

            expect(client.page.update).toHaveBeenCalledWith(PAGE_ID, {
                page: {
                    id: PAGE_ID,
                    language: LANGUAGE.code,
                },
                createVersion: true,
                unlock: true,
                deriveFileName: true,
            });
        }));
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

        it('creates correct folder', fakeAsync(async () => {
            const folder = {
                name: 'A new folder',
                directory: '/new-folder',
                description: 'A new folder',
                nodeId: 2222,
                parentFolderId: 1111,
            };
            const createdFolder = await folderActions.createNewFolder(folder);
            tick();

            expect(createdFolder).not.toBeUndefined();
            expect(createdFolder ? createdFolder.name : '').toEqual('A new folder');
            expect(client.folder.create).toHaveBeenCalledWith(mapCreateNewFolderModelToCreateFolderModel(folder));
        }));

        it('creates correct folder with optional failOnDuplicate flag', fakeAsync(async () => {
            const folder = {
                name: 'A new folder',
                directory: '/new-folder',
                description: 'A new folder',
                nodeId: 2222,
                parentFolderId: 1111,
                failOnDuplicate: true,
            };
            const createdFolder = await folderActions.createNewFolder(folder);
            tick();

            expect(createdFolder).not.toBeUndefined();
            expect(createdFolder ? createdFolder.name : '').toEqual('A new folder');
            expect(client.folder.create).toHaveBeenCalledWith(mapCreateNewFolderModelToCreateFolderModel(folder));
        }));

        it('emits void value if an error occurs', fakeAsync(async () => {
            const folder = {
                name: 'existing',
                directory: '/new-folder',
                description: 'A new folder',
                nodeId: 2222,
                parentFolderId: 1111,
                failOnDuplicate: true,
            };
            const createdFolder = await folderActions.createNewFolder(folder);
            tick();

            expect(createdFolder).toBeUndefined();
            expect(client.folder.create).toHaveBeenCalledWith(mapCreateNewFolderModelToCreateFolderModel(folder));
        }));
    });

    describe('openUploadModals()', () => {
        it('should not attempt to open the modal without the feature active', fakeAsync(async () => {
            spyOn(folderActions, 'openUploadModals').and.callThrough();
            spyOn(folderActions, 'openImageModal').and.returnValue(Promise.resolve());

            await folderActions.uploadAndReplace({
                create: {
                    files: [TEST_UPLOAD_FILE],
                    images: [TEST_UPLOAD_IMAGE],
                },
                replace: {
                    files: [],
                    images: [],
                },
            }, 1, 1).toPromise();
            tick(100);

            expect(folderActions.openUploadModals).toHaveBeenCalledTimes(0);
            expect(folderActions.openImageModal).toHaveBeenCalledTimes(0);
        }));

        it('should open the modal because file-properties are enabled', fakeAsync(async () => {
            state.mockState({
                features: {
                    nodeFeatures: {
                        [ACTIVE_NODE_ID]: [NodeFeature.UPLOAD_FILE_PROPERTIES],
                    },
                },
            });

            spyOn(folderActions, 'openUploadModals').and.callThrough();
            spyOn(folderActions, 'openImageModal').and.returnValue(Promise.resolve());

            await folderActions.uploadAndReplace({
                create: {
                    files: [TEST_UPLOAD_FILE],
                    images: [TEST_UPLOAD_IMAGE],
                },
                replace: {
                    files: [],
                    images: [],
                },
            }, 1, ACTIVE_NODE_ID).toPromise();
            tick(100);

            expect(folderActions.openUploadModals).toHaveBeenCalledTimes(1);
            expect(folderActions.openImageModal).toHaveBeenCalledTimes(1);
        }));

        it('should open the modal because image-properties are enabled', fakeAsync(async () => {
            state.mockState({
                features: {
                    nodeFeatures: {
                        [ACTIVE_NODE_ID]: [NodeFeature.UPLOAD_IMAGE_PROPERTIES],
                    },
                },
            });

            spyOn(folderActions, 'openUploadModals').and.callThrough();
            spyOn(folderActions, 'openImageModal').and.returnValue(Promise.resolve());

            await folderActions.uploadAndReplace({
                create: {
                    files: [TEST_UPLOAD_FILE],
                    images: [TEST_UPLOAD_IMAGE],
                },
                replace: {
                    files: [],
                    images: [],
                },
            }, 1, ACTIVE_NODE_ID).toPromise();
            tick(100);

            expect(folderActions.openUploadModals).toHaveBeenCalledTimes(1);
            expect(folderActions.openImageModal).toHaveBeenCalledTimes(1);
        }));

        it('should open a modal for each file', fakeAsync(async () => {
            state.mockState({
                features: {
                    nodeFeatures: {
                        [ACTIVE_NODE_ID]: [NodeFeature.UPLOAD_FILE_PROPERTIES, NodeFeature.UPLOAD_IMAGE_PROPERTIES],
                    },
                },
            });

            spyOn(folderActions, 'openUploadModals').and.callThrough();
            spyOn(folderActions, 'openImageModal').and.returnValue(Promise.resolve());

            await folderActions.uploadAndReplace({
                create: {
                    files: [TEST_UPLOAD_FILE],
                    images: [TEST_UPLOAD_IMAGE],
                },
                replace: {
                    files: [],
                    images: [],
                },
            }, 1, ACTIVE_NODE_ID).toPromise();
            tick(100);

            expect(folderActions.openUploadModals).toHaveBeenCalledTimes(1);
            expect(folderActions.openImageModal).toHaveBeenCalledTimes(2);
        }));
    })
});
