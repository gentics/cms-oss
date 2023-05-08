import { fakeAsync, TestBed } from '@angular/core/testing';
import {
    BaseListResponse,
    Folder,
    FolderListResponse,
    Language,
    Node,
    Page,
    PageListResponse,
    Raw,
    RepoItem,
} from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { forkJoin, Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { Api } from '../../../core/providers/api/api.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { ApplicationStateService, STATE_MODULES } from '../../../state';
import { MockAppState, TestApplicationState } from '../../../state/test-application-state.mock';
import { RepositoryBrowserDataService } from './repository-browser-data.service';

const NODE_1_ID = 1;
const NODE_1_FOLDER_ID = 11;
const NODE_1_PAGE_1_EN_ID = 12;
const NODE_1_PAGE_2_EN_ID = 13;
const NODE_1_PAGE_3_EN_ID = 14;
const NODE_1_PAGE_1_DE_ID = 15;
const NODE_1_PAGE_2_DE_ID = 16;
const NODE_1_PAGE_3_DE_ID = 17;
const NODE_2_ID = 2;
const NODE_2_FOLDER_ID = 22;
const NODE_1_SUBFOLDER_1_ID = 111;
const LANGUAGE_1_ID = 1;
const LANGUAGE_2_ID = 2;

const languages: { [key: number]: Language } = {
    [LANGUAGE_1_ID]: {
        id: LANGUAGE_1_ID,
        code: 'en',
        name: 'English',
    },
    [LANGUAGE_2_ID]: {
        id: LANGUAGE_2_ID,
        code: 'de',
        name: 'Deutsch',
    },
}

const testStateData: MockAppState = {
    folder: {
        activeFolder: NODE_1_FOLDER_ID,
        activeLanguage: LANGUAGE_1_ID,
        activeNode: NODE_1_ID,
        activeNodeLanguages: { list: [LANGUAGE_1_ID, LANGUAGE_2_ID] },
        breadcrumbs: { list: [NODE_1_FOLDER_ID] },
        nodes: { list: [NODE_1_ID, NODE_2_ID] },
        folders: { list: [NODE_1_SUBFOLDER_1_ID] },
        pages: { list: [] },
        files: { list: [] },
        images: { list: [] },
        templates: { list: [] },
    },
    entities: {
        node: {
            [NODE_1_ID]: {
                id: NODE_1_ID,
                type: 'node',
                name: 'Node One',
                folderId: NODE_1_FOLDER_ID,
                inheritedFromId: NODE_1_ID,
            },
            [NODE_2_ID]: {
                id: NODE_2_ID,
                type: 'node',
                name: 'Node Two',
                folderId: NODE_2_FOLDER_ID,
                inheritedFromId: NODE_2_ID,
            },
        },
        folder: {
            [NODE_1_FOLDER_ID]: {
                id: NODE_1_FOLDER_ID,
                type: 'folder',
                name: 'Node One',
                nodeId: NODE_1_ID,
            },
            [NODE_1_SUBFOLDER_1_ID]: {
                id: NODE_1_SUBFOLDER_1_ID,
                type: 'folder',
                name: 'Subfolder One',
                nodeId: NODE_1_ID,
            },
            [NODE_2_FOLDER_ID]: {
                id: NODE_2_FOLDER_ID,
                type: 'folder',
                name: 'Node Two',
                nodeId: NODE_2_ID,
            },
        },
        page: {
            [NODE_1_PAGE_1_EN_ID]: {
                id: NODE_1_PAGE_1_EN_ID,
                folderId: NODE_1_FOLDER_ID,
                type: 'page',
                name: `NODE_1_PAGE_${NODE_1_PAGE_1_EN_ID}`,
                language: 'en',
                languageVariants: {
                    1: NODE_1_PAGE_1_EN_ID,
                    2: NODE_1_PAGE_1_DE_ID,
                },
            },
            [NODE_1_PAGE_2_EN_ID]: {
                id: NODE_1_PAGE_2_EN_ID,
                folderId: NODE_1_FOLDER_ID,
                type: 'page',
                name: `NODE_1_PAGE_${NODE_1_PAGE_2_EN_ID}`,
                language: 'en',
                languageVariants: {
                    1: NODE_1_PAGE_2_EN_ID,
                    2: NODE_1_PAGE_2_DE_ID,
                },
            },
            [NODE_1_PAGE_3_EN_ID]: {
                id: NODE_1_PAGE_3_EN_ID,
                folderId: NODE_1_FOLDER_ID,
                type: 'page',
                name: `NODE_1_PAGE_${NODE_1_PAGE_3_EN_ID}`,
                language: 'en',
                languageVariants: {
                    1: NODE_1_PAGE_3_EN_ID,
                    2: NODE_1_PAGE_3_DE_ID,
                },
            },

            [NODE_1_PAGE_1_DE_ID]: {
                id: NODE_1_PAGE_1_DE_ID,
                folderId: NODE_1_FOLDER_ID,
                type: 'page',
                name: `NODE_1_PAGE_${NODE_1_PAGE_1_DE_ID}`,
                language: 'de',
            },
            [NODE_1_PAGE_2_DE_ID]: {
                id: NODE_1_PAGE_2_DE_ID,
                folderId: NODE_1_FOLDER_ID,
                type: 'page',
                name: `NODE_1_PAGE_${NODE_1_PAGE_2_DE_ID}`,
                language: 'de',
            },
            [NODE_1_PAGE_3_DE_ID]: {
                id: NODE_1_PAGE_3_DE_ID,
                folderId: NODE_1_FOLDER_ID,
                type: 'page',
                name: `NODE_1_PAGE_${NODE_1_PAGE_3_DE_ID}`,
                language: 'de',
            },
        },
        language: languages,
    },
};

const availableLanguagesPerNode: { [key: number]: Language[] } = {
    [NODE_1_ID]: [languages[LANGUAGE_1_ID], languages[LANGUAGE_2_ID]],
    [NODE_2_ID]: [languages[LANGUAGE_1_ID]],
}

let service: RepositoryBrowserDataService;
let entityResolver: EntityResolver;

describe('RepositoryBrowserDataService', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                RepositoryBrowserDataService,
                { provide: Api, useClass: MockApi },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                EntityResolver,
                { provide: I18nNotification, useClass: MockNotificationService },
            ],
        });

        service = TestBed.get(RepositoryBrowserDataService);
        entityResolver = TestBed.get(EntityResolver);
        prefillAppStateWithDefaultTestData();
    });

    it('can be created', () => {
        expect(service).toBeDefined();
    });

    describe('init', () => {

        it('does not throw for "pick a single folder"', () => {
            setupDataServiceForSingleFolder();
        });

    });

    describe('canSubmit$', () => {

        it('emits true without selecting an item when picking a folder', () => {
            setupDataServiceForSingleFolder();

            let canSubmit: any;
            service.canSubmit$.subscribe(v => canSubmit = v);
            expect(canSubmit).toEqual(true);
        });

        describe('when picking multiple items', () => {

            beforeEach(setupDataServiceForMultipleFolders);
            let canSubmit: any;
            beforeEach(() => service.canSubmit$.subscribe(v => canSubmit = v));

            it('emits false initially', fakeAsync(() => {
                expect(canSubmit).toEqual(false);
            }));

            it('emits true after selecting a folder', () => {
                selectFirstItem().then(() => {
                    expect(canSubmit).toEqual(true);
                });
            });

            it('emits false after deselecting all selected items', () => {
                selectFirstItem().then(() => {
                    deselectAllItems();
                    expect(canSubmit).toEqual(false);
                });
            });

        });

    });

    describe('currentNode$', () => {

        let currentNode: number;
        beforeEach(() => service.currentNodeId$.subscribe(c => currentNode = c));

        it('starts with a "startNode" if the service is initialized with one', () => {
            const startNode = NODE_2_ID;
            setupDataServiceWithStartNode(startNode);
            expect(currentNode).toEqual(startNode);
        });

        it('starts with the current node of the item list when initialized with no "startNode"', () => {
            const activeNodeInItemList = NODE_2_ID;
            prefillAppStateWithActiveNode(activeNodeInItemList);
            setupDataServiceForSingleFolder();
            expect(currentNode).toEqual(activeNodeInItemList);
        });

        it('emits the node ID when navigating to a different node with changeNode()', () => {
            setupDataServiceWithStartNode(NODE_1_ID);
            expect(currentNode).toEqual(NODE_1_ID);

            service.changeNode(NODE_2_ID);
            expect(currentNode).toEqual(NODE_2_ID);
        });

        it('emits -1 when navigating to favourites with switchToFavourites()', () => {
            setupDataServiceWithStartNode(NODE_1_ID);
            service.switchToFavourites();
            expect(currentNode).toEqual(-1);
        });

    });

    describe('currentAvailableLanguages$', () => {

        let currentAvailableLanguages: Language[];
        beforeEach(() => service.currentAvailableLanguages$.subscribe(c => currentAvailableLanguages = c));

        it('starts with the corresponding available languages if the service is initialized with a start node', () => {
            const startNode = NODE_2_ID;
            setupDataServiceWithStartNode(startNode);
            expect(currentAvailableLanguages).toEqual(availableLanguagesPerNode[startNode]);
        });

        it('starts with the current available languages of the item list when initialized with no "startNode"', () => {
            const activeNodeInItemList = NODE_2_ID;
            prefillAppStateWithActiveNode(activeNodeInItemList);
            setupDataServiceForSingleFolder();
            expect(currentAvailableLanguages).toEqual(availableLanguagesPerNode[activeNodeInItemList]);
        });

        it('emits updated available languages when navigating to a different node with changeNode()', () => {
            setupDataServiceWithStartNode(NODE_1_ID);
            expect(currentAvailableLanguages).toEqual(availableLanguagesPerNode[NODE_1_ID]);

            service.changeNode(NODE_2_ID);
            expect(currentAvailableLanguages).toEqual(availableLanguagesPerNode[NODE_2_ID]);
        });

        it('emits an empty array of available languages when navigating to favourites with switchToFavourites()', () => {
            setupDataServiceWithStartNode(NODE_1_ID);
            service.switchToFavourites();
            expect(currentAvailableLanguages.length).toEqual(0);
        });

    });

    describe('currentContentLanguage$', () => {

        let currentContentLanguage: Language;
        beforeEach(() => service.currentContentLanguage$.subscribe(c => currentContentLanguage = c));

        it('starts with a "contentLanguage" if the service is initialized with one', () => {
            const startLanguage = languages[LANGUAGE_2_ID];
            setupDataServiceWithContentLanguage(startLanguage);
            expect(currentContentLanguage).toEqual(startLanguage);
        });

        it('starts with the current content language of the item list when initialized with no "contentLanguage"', () => {
            const startLanguage = languages[LANGUAGE_2_ID];
            prefillAppStateWithActiveLanguage(startLanguage);
            setupDataServiceForSingleFolder();
            expect(currentContentLanguage).toEqual(startLanguage);
        });

        it('emits the node ID when navigating to a different content language with setContentLanguage()', () => {
            setupDataServiceWithContentLanguage(languages[LANGUAGE_1_ID]);
            expect(currentContentLanguage).toEqual(languages[LANGUAGE_1_ID]);

            service.setContentLanguage(languages[LANGUAGE_2_ID]);
            expect(currentContentLanguage).toEqual(languages[LANGUAGE_2_ID]);
        });

        it('emits the first available language when switching to another node that has active languages, if the current content language is not in the available languages', () => {
            setupDataServiceWithContentLanguage(languages[LANGUAGE_2_ID]);
            service.changeNode(NODE_2_ID);
            expect(currentContentLanguage).toEqual(languages[LANGUAGE_1_ID]);
        });

        it('emits the first available language of the start node or active node if an invalid language was chosen during initialization', () => {
            const activeNodeInItemList = NODE_2_ID;
            prefillAppStateWithActiveNode(activeNodeInItemList);
            setupDataServiceWithContentLanguage(languages[LANGUAGE_2_ID]);
            expect(currentContentLanguage).toEqual(languages[LANGUAGE_1_ID]);
        });

    });

    describe('filter$', () => {

        let filter: any;
        beforeEach(() => service.filter$.subscribe(f => filter = f));

        it('emits "" initially', () => {
            expect(filter).toEqual('');
        });

        it('emits when the filter is changed with setFilter()', () => {
            service.setFilter('abc');
            expect(filter).toEqual('abc');
        });

        it('does not re-emit the same value', () => {
            const emitted: any[] = [];
            service.filter$.subscribe(f => emitted.push(f));

            service.setFilter('abc');
            expect(emitted).toEqual(['', 'abc']);

            service.setFilter('abc');
            expect(emitted).not.toEqual(['', 'abc', 'abc']);
        });

    });

    describe('currentParent', () => {

        it('starts with the activeFolder', fakeAsync(() => {
            setupDataServiceWithStartNode(NODE_1_ID);
            expect(service.currentParent.id).toEqual(NODE_1_FOLDER_ID);
        }));

        it('has the correct value after changing nodes', fakeAsync(() => {
            setupDataServiceWithStartNode(NODE_1_ID);
            service.changeNode(NODE_2_ID);
            expect(service.currentParent).not.toBeUndefined();
            expect(service.currentParent.id).toEqual(NODE_2_FOLDER_ID);
        }));

    });

    // TODO: More unit tests
    // - currentNodeId
    // - currentParent
    // - files$
    // - hasPermissions$
    // - images$
    // - itemTypesToDisplay$
    // - isDisplayingFavourites$
    // - isDisplayingFavouritesFolder$
    // - isPickingFolder
    // - itemTypesToDisplay$
    // - loading$
    // - nodes$
    // - pages$
    // - parentItems$
    // - search$
    // - selected$
    // - selectedItems
    // - showFavourites$
    // - sortOrder$
    // - startPageId$
    // - tags$
    // - templates$
    // - changeFolder()
    // - changeNode()
    // - changeParent()
    // - init()
    // - isSelected()
    // - selectItem()
    // - deselectItem()
    // - setFilter()
    // - setSearch()
    // - setSorting()
    // - switchToFavourites()

});

const mockNodeName = 'MockNode';

class MockApi {
    responseEnvelope: BaseListResponse = {
        hasMoreItems: false,
        messages: [],
        numItems: 3,
        responseInfo: {
            responseCode: 'OK',
        },
    };
    folders = {
        getBreadcrumbs(): Observable<FolderListResponse> {
            return Observable.of({
                ...this.responseEnvelope,
                folders: Object.values(testStateData.entities.folder) as Partial<Folder<Raw>>[],
            });
        },
        getFolders(): Observable<FolderListResponse> {
            return Observable.of({
                ...this.responseEnvelope,
                folders: Object.values(testStateData.entities.folder) as Partial<Folder<Raw>>[],
            });
        },
        getPages(): Observable<PageListResponse> {
            return Observable.of({
                ...this.responseEnvelope,
                pages: Object.values(testStateData.entities.page) as Partial<Page<Raw>>[],
            });
        },
        getNodes(): Observable<FolderListResponse & { nodes: Node<Raw>[] }> {
            return Observable.of({
                ...this.responseEnvelope,
                folders: Object.values(testStateData.entities.folder) as Partial<Folder<Raw>>[],
                nodes: Object.values(testStateData.entities.node) as Partial<Node<Raw>>[],
            });
        },
        getLanguagesOfNode: jasmine.createSpy('getLanguagesOfNode').and.callFake((nodeId: number): Observable<{ languages: Language[] }> => {
            return of({ languages: availableLanguagesPerNode[nodeId] });
        }),
        getNode: (): Observable<Partial<Node>> => of({ name: mockNodeName }),
    };
}

class MockErrorHandler {
}

class MockNotificationService {
}

function prefillAppStateWithDefaultTestData(): void {
    const state: TestApplicationState = TestBed.get(ApplicationStateService);
    state.mockState(testStateData);
}

// function apiReturnsDataFromAppState(): void {
//     const api: MockApi = TestBed.get(Api);
//     api.folders.getNodes = () => {
//         const appState: TestApplicationState = TestBed.get(ApplicationStateService);
//         const entities = appState.now.entities;

//         const nodes = Object.keys(entities.node)
//             .map(Number)
//             .map(id => entities.node[id]);

//         const nodeFolders = nodes
//             .map(node => entities.folder[node.folderId]);

//         return Observable.of({ folders: nodeFolders, nodes });
//     };
// }

// function prefillAppStateWithSingleNode(): void {
//     const state: TestApplicationState = TestBed.get(ApplicationStateService);
//     state.mockState({
//         folder: {
//             nodes: {
//                 list: [NODE_1_ID]
//             }
//         },
//         entities: {
//             node: {
//                 [NODE_1_ID]: {
//                     id: NODE_1_ID,
//                     type: 'node',
//                     name: 'Node One'
//                 }
//             }
//         }
//     });

//     const api: MockApi = TestBed.get(Api);
//     api.folders.getNodes = () => Observable.of({
//         folders: [
//             {
//                 id: NODE_1_FOLDER_ID
//             }
//         ],
//         nodes: [
//             {
//                 id: NODE_1_ID,
//                 type: 'node',
//                 name: 'Node One',
//                 folderId: NODE_1_FOLDER_ID
//             }
//         ]
//     });
// }

function prefillAppStateWithActiveNode(nodeId: number): void {
    const state: TestApplicationState = TestBed.get(ApplicationStateService);
    state.mockState({
        folder: {
            ...state.now.folder,
            activeNode: nodeId,
        },
    });
}

function prefillAppStateWithActiveLanguage(contentLanguage: Language): void {
    const state: TestApplicationState = TestBed.get(ApplicationStateService);
    state.mockState({
        folder: {
            ...state.now.folder,
            activeLanguage: contentLanguage.id,
        },
    });
}

function setupDataServiceForSingleFolder(): void {
    service.init({
        allowedSelection: { folder: true },
        selectMultiple: false,
    });
}

function setupDataServiceForMultipleFolders(): void {
    service.init({
        allowedSelection: { folder: true },
        selectMultiple: true,
    });
}

function setupDataServiceForSingleFileOrImage(): void {
    service.init({
        allowedSelection: { file: true, image: true },
        selectMultiple: false,
    });
}

function setupDataServiceForMultipleFilesOrImages(): void {
    service.init({
        allowedSelection: { file: true, image: true },
        selectMultiple: true,
    });
}

function setupDataServiceWithStartNode(startNode: number = NODE_2_ID): void {
    service.init({
        allowedSelection: { folder: true },
        selectMultiple: false,
        startNode,
    });
}

function setupDataServiceWithContentLanguage(contentLanguage: Language = languages[LANGUAGE_2_ID]): void {
    service.init({
        allowedSelection: { folder: true },
        selectMultiple: false,
        contentLanguage: contentLanguage.code,
    });
}

const getAllVisibleItems = (): Promise<RepoItem[]> => forkJoin([
    service.folders$,
    service.pages$,
    service.files$,
    service.images$,
    service.templates$,
]).pipe(
    map((data: RepoItem[][]) => [].concat(...data)),
).toPromise();

const selectFirstItem = (): Promise<void> => getAllVisibleItems().then(items => service.selectItem(items[0]));

function deselectAllItems(): void {
    const selected = [...service.selectedItems];
    for (const item of selected) {
        service.deselectItem(item);
    }
}
