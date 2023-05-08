import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { folderSchema, pageSchema } from '@editor-ui/app/common/models';
import {
    FolderItemOrTemplateType,
    Language,
    Node,
    Normalized,
    Page,
    Raw,
    User,
} from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { normalize } from 'normalizr';
import { Subscription } from 'rxjs';
import {
    getExampleFolderData,
    getExampleFolderDataNormalized,
    getExampleImageData,
    getExamplePageData,
    getExamplePageDataNormalized,
    getExampleUserData,
} from '../../../../testing/test-data.mock';
import { FolderState, ItemsInfo, emptyItemInfo, plural } from '../../../common/models';
import { ApplicationStateService } from '../../providers';
import { getNormalizrSchema } from '../../state-utils';
import { TestApplicationState } from '../../test-application-state.mock';
import { AddEntitiesAction, UpdateEntitiesAction } from '../entity/entity.actions';
import { STATE_MODULES } from '../state-modules';
import {
    ChannelSyncReportFetchingErrorAction,
    ChannelSyncReportFetchingSuccessAction,
    CreateItemSuccessAction,
    EditImageSuccessAction,
    InheritanceFetchingSuccessAction,
    ItemFetchingSuccessAction,
    LanguageFetchingSuccessAction,
    ListCreatingErrorAction,
    ListCreatingSuccessAction,
    ListDeletingErrorAction,
    ListDeletingSuccessAction,
    ListFetchingErrorAction,
    ListFetchingSuccessAction,
    ListSavingErrorAction,
    ListSavingSuccessAction,
    NodeFetchingSuccessAction,
    RecentItemsFetchingSuccessAction,
    SetActiveFolderAction,
    SetActiveNodeAction,
    SetDisplayAllLanguagesAction,
    SetFilterTermAction,
    SetFolderLanguageAction,
    SetListDisplayFieldsAction,
    SetListSortingAction,
    SetRepositoryBrowserDisplayFieldsAction,
    SetSearchFiltersVisibleAction,
    SetSearchTermAction,
    StartChannelSyncReportFetchingAction,
    StartListCreatingAction,
    StartListDeletingAction,
    StartListFetchingAction,
    StartListSavingAction,
} from './folder.actions';

describe('FolderStateModule', () => {

    let state: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        state = TestBed.get(ApplicationStateService);
    });

    function mockListState(key: keyof FolderState, listState: Partial<ItemsInfo>): void {
        state.mockState({
            folder: {
                [key]: listState,
            },
        });
    }

    it('sets the correct initial state', () => {
        expect(state.now.folder).toEqual({
            folders: emptyItemInfo,
            forms: emptyItemInfo,
            pages: emptyItemInfo,
            images: emptyItemInfo,
            files: emptyItemInfo,
            templates: emptyItemInfo,
            nodes: emptyItemInfo,
            activeNodeLanguages: emptyItemInfo,
            activeFolder: null,
            activeNode: null,
            activeLanguage: null,
            activeFormLanguage: null,
            displayAllLanguages: false,
            displayStatusIcons: false,
            displayDeleted: false,
            displayImagesGridView: true,
            lastError: '',
            filterTerm: '',
            recentItems: [],
            searchTerm: '',
            searchFilters: {},
            searchFiltersChanging: false,
            searchFiltersValid: false,
            searchFiltersVisible: false,
            breadcrumbs: emptyItemInfo,
            channelSyncReport: {
                folders: [],
                forms: [],
                pages: [],
                images: [],
                files: [],
                templates: [],
                fetching: false,
            },
        } as FolderState);
    });

    it('copyItemStart works', () => {
        expect(state.now.folder.pages.saving).toBe(false);
        state.dispatch(new StartListSavingAction('page'));
        expect(state.now.folder.pages.saving).toBe(true);
    });

    it('copyItemSuccess works', () => {
        mockListState('pages', { saving: true });
        expect(state.now.folder.pages.saving).toBe(true);
        state.dispatch(new ListSavingSuccessAction('page'));
        expect(state.now.folder.pages.saving).toBe(false);
    });

    it('copyItemError works', () => {
        mockListState('pages', { saving: true });
        const errorMessage = 'something went wrong';
        expect(state.now.folder.pages.saving).toBe(true);
        state.dispatch(new ListSavingErrorAction('page', errorMessage));
        expect(state.now.folder.pages.saving).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('createItemStart works', () => {
        expect(state.now.folder.folders.creating).toBe(false);
        state.dispatch(new StartListCreatingAction('folder'));
        expect(state.now.folder.folders.creating).toBe(true);
    });

    it('createItemSuccess works', () => {
        mockListState('folders', { creating: true, list: [1, 2] });
        const folder = getExampleFolderData({ id: 123, userId: 999 });

        // With addToList: false
        expect(state.now.folder.folders.creating).toBe(true);
        state.dispatch(new CreateItemSuccessAction('folder', [folder], false));
        expect(state.now.folder.folders.creating).toBe(false);
        expect(state.now.folder.folders.list).toEqual([1, 2]);
        expect(state.now.entities.folder[123]).toBeDefined('no folder 123');
        expect(state.now.entities.user[999]).toBeDefined('no user 999');

        // With addToList: true
        const secondFolder = getExampleFolderData({ id: 456, userId: 999 });
        state.dispatch(new CreateItemSuccessAction('folder', [secondFolder], true));
        expect(state.now.folder.folders.list).toEqual([1, 2, 456], 'folder not added to list');
        expect(state.now.entities.folder[456]).toBeDefined('no folder 456');
    });

    it('createItemError works', () => {
        mockListState('folders', { creating: true });
        expect(state.now.folder.folders.creating).toBe(true);
        const errorMessage = 'something went wrong';
        state.dispatch(new ListCreatingErrorAction('folder', errorMessage));
        expect(state.now.folder.folders.creating).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('deleteItemsStart works', () => {
        mockListState('pages', { deleting: [], list: [1, 2, 3] });
        state.dispatch(new StartListDeletingAction('page', [1, 2]));
        expect(state.now.folder.pages.deleting).toEqual([1, 2]);
        expect(state.now.folder.pages.list).toEqual([3]);
    });

    it('deleteItemsSuccess works', () => {
        mockListState('pages', {
            deleting: [1, 2, 5, 6],
            list: [3],
        });
        state.dispatch(new ListDeletingSuccessAction('page', [1, 2]));
        expect(state.now.folder.pages.deleting).toEqual([5, 6]);
        expect(state.now.folder.pages.list).toEqual([3]);
    });

    it('deleteItemsError works', () => {
        mockListState('pages', {
            deleting: [1, 2, 5, 6],
            list: [3],
        });
        const sorted = (list: number[]) => [...list].sort((a, b) => a - b);
        const errorMessage = 'could not delete';

        state.dispatch(new ListDeletingErrorAction('page', [1, 2], errorMessage));
        expect(state.now.folder.pages.deleting).toEqual([5, 6]);
        expect(sorted(state.now.folder.pages.list)).toEqual([1, 2, 3]);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('editImageStart works', () => {
        state.dispatch(new StartListSavingAction('image'));
        expect(state.now.folder.images.saving).toBe(true);
    });

    it('editImageSuccess works', () => {
        mockListState('images', { saving: true, list: [1, 2], total: 2 });
        const image = getExampleImageData({ id: 1234, userId: 3 });
        state.dispatch(new EditImageSuccessAction(image));

        expect(state.now.folder.images.saving).toBe(false);
        expect(state.now.folder.images.list).toEqual([1, 2, 1234]);
        expect(state.now.folder.images.total).toBe(3);
        expect(state.now.entities.image[1234]).toBeDefined('new image not in entities');
    });

    it('editImageError works', () => {
        mockListState('images', { saving: true });
        const errorMessage = 'could not save image';
        state.dispatch(new ListSavingErrorAction('image', errorMessage));
        expect(state.now.folder.images.saving).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('fetchBreadcrumbsStart works', () => {
        expect(state.now.folder.breadcrumbs.fetching).toBe(false);
        state.dispatch(new StartListFetchingAction('breadcrumbs', false));
        expect(state.now.folder.breadcrumbs.fetching).toBe(true);
    });

    it('fetchBreadcrumbsSuccess works', () => {
        state.mockState({
            folder: {
                activeFolder: 111,
                activeNode: 222,
                breadcrumbs: {
                    fetching: true,
                    list: [],
                },
            },
        });

        const items = [
            getExampleFolderData({ id: 1 }),
            getExampleFolderData({ id: 2 }),
            getExampleFolderData({ id: 3 }),
        ];

        state.dispatch(new ListFetchingSuccessAction('breadcrumbs', {
            hasMore: false,
            folderId: 111,
            nodeId: 222,
            items,
            schema: folderSchema,
            total: items.length,
        }));

        expect(state.now.folder.breadcrumbs.fetching).toBe(false);
        expect(state.now.folder.breadcrumbs.list).toEqual([1, 2, 3]);
        expect(state.now.entities.folder[1]).toBeDefined('folder 1 not in entity state');
        expect(state.now.entities.folder[2]).toBeDefined('folder 2 not in entity state');
        expect(state.now.entities.folder[3]).toBeDefined('folder 3 not in entity state');
    });

    it('fetchBreadcrumbsError works', () => {
        mockListState('breadcrumbs', {
            fetching: true,
            list: [1, 5],
        });
        const errorMessage = 'failed fetching breadcrumbs';
        state.dispatch(new ListFetchingErrorAction('breadcrumbs', errorMessage, true));
        expect(state.now.folder.breadcrumbs.fetching).toBe(false);
        expect(state.now.folder.breadcrumbs.list.length).toEqual(2);
        expect(state.now.folder.lastError).toBe('failed fetching breadcrumbs');
    });

    it('fetchInheritanceStart works', () => {
        expect(state.now.folder.pages.fetching).toBe(false);
        state.dispatch(new StartListFetchingAction('page'));
        expect(state.now.folder.pages.fetching).toBe(true);
    });

    it('fetchInheritanceSuccess works', () => {
        state.mockState({
            folder: {
                pages: {
                    fetching: true,
                } as ItemsInfo,
            },
            entities: {
                page: {
                    1: getExamplePageDataNormalized({ id: 1 }),
                },
            },
        });

        expect(state.now.folder.pages.fetching).toBe(true);
        expect(state.now.entities.page[1].disinherit).toBeUndefined('disinherit is defined');
        expect(state.now.entities.page[1].disinherited).toBe(false, 'disinherited != false');
        expect(state.now.entities.page[1].excluded).toBe(false, 'excluded != false');
        expect(state.now.entities.page[1].inheritable).toBeUndefined('inheritable is defined');

        state.dispatch(new InheritanceFetchingSuccessAction('page', 1, {
            disinherit: [5, 8],
            disinheritDefault: true,
            excluded: true,
            inheritable: [77, 99],
        }));
        expect(state.now.entities.page[1].disinherit).toEqual([5, 8]);
        expect(state.now.entities.page[1].disinheritDefault).toBe(true, 'disinheritDefault != true');
        expect(state.now.entities.page[1].excluded).toBe(true, 'excluded != true');
        expect(state.now.entities.page[1].inheritable).toEqual([77, 99]);
    });

    it('fetchInheritanceError works', () => {
        mockListState('pages', { fetching: true });
        const errorMessage = 'fetching inheritance failed';
        state.dispatch(new ListFetchingErrorAction('page', errorMessage, true));
        expect(state.now.folder.pages.fetching).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('fetchItemStart works', () => {
        expect(state.now.folder.pages.fetching).toBe(false);
        state.dispatch(new StartListFetchingAction('page', undefined, true));
        expect(state.now.folder.pages.fetching).toBe(true);
    });

    it('fetchItemSuccess works', () => {
        mockListState('pages', { fetching: true });
        const page = getExamplePageData({ id: 1234, userId: 777 });
        state.dispatch(new ItemFetchingSuccessAction('page', page));

        expect(state.now.folder.pages.fetching).toBe(false);
        expect(state.now.entities.page[1234]).toBeDefined('page not in entity state');
        expect(state.now.entities.user[777]).toBeDefined('user not in entity state');
    });

    it('fetchItemError works', () => {
        mockListState('pages', { fetching: true });
        const errorMessage = 'fetching item failed';
        state.dispatch(new ListFetchingErrorAction('page', errorMessage, true));
        expect(state.now.folder.pages.fetching).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('fetchItemListStart works with fetchAll = false', () => {
        expect(state.now.folder.folders.fetching).toBe(false);
        state.dispatch(new StartListFetchingAction('folders', false));
        expect(state.now.folder.folders.fetching).toBe(true, 'fetching != true');
        expect(state.now.folder.folders.fetchAll).toBe(false, 'fetchAll != false');

        state.mockState({ folder: { folders: { fetchAll: true } } });
        state.dispatch(new StartListFetchingAction('folders', false));
        expect(state.now.folder.folders.fetchAll).toBe(false, 'fetchAll is not overwritten');
    });

    it('fetchItemListStart works with fetchAll = true', () => {
        expect(state.now.folder.pages.fetching).toBe(false);
        state.dispatch(new StartListFetchingAction('pages', true));
        expect(state.now.folder.pages.fetching).toBe(true, 'fetching != true');
        expect(state.now.folder.pages.fetchAll).toBe(true, 'fetchAll != true');

        state.mockState({ folder: { pages: { fetchAll: false } } });
        state.dispatch(new StartListFetchingAction('pages', true));
        expect(state.now.folder.pages.fetchAll).toBe(true, 'fetchAll is not overwritten');
    });

    it('fetchItemListSuccess works for small lists (3 items)', () => {
        state.mockState({
            folder: {
                activeFolder: 111,
                activeNode: 222,
                pages: {
                    fetchAll: true,
                    fetching: true,
                },
            },
        });

        const items = [
            getExamplePageData({ id: 1, userId: 77 }),
            getExamplePageData({ id: 2, userId: 77 }),
            getExamplePageData({ id: 3, userId: 77 }),
        ];

        state.dispatch(new ListFetchingSuccessAction('page', {
            folderId: 111,
            nodeId: 222,
            fetchAll: true,
            hasMore: false,
            items,
            total: 3,
            schema: pageSchema,
        }));

        expect(state.now.folder.pages.fetching).toBe(false);
        expect(state.now.folder.pages.fetchAll).toBe(true, 'fetchAll was reset to false');
        expect(state.now.folder.pages.list).toEqual([1, 2, 3]);
        expect(state.now.folder.pages.hasMore).toBe(false, 'hasMore != false');
        expect(state.now.folder.pages.total).toBe(3);
        expect(state.now.entities.page[1]).toBeDefined('page 1 not in entity state');
        expect(state.now.entities.page[2]).toBeDefined('page 2 not in entity state');
        expect(state.now.entities.page[3]).toBeDefined('page 3 not in entity state');
    });

    describe('fetchItemListSuccess for large lists', () => {

        /** List of 47 Pages with ID 1...47 (and index 0...46) */
        let testPages: Page<Raw>[];
        let emittedIDs: number[][];
        let subscription: Subscription;
        let lastTestPage: Page<Raw>;

        beforeEach(() => {
            testPages = new Array(47) as Page<Raw>[];
            for (let index = 0; index < testPages.length; index++) {
                testPages[index] = getExamplePageData({ id: index + 1, userId: 77 });
            }
            lastTestPage = testPages[testPages.length - 1];

            // Initial state
            state.mockState({
                folder: {
                    activeFolder: 111,
                    activeNode: 222,
                    pages: {
                        fetchAll: true,
                        fetching: true,
                        list: [],
                    },
                    folders: {},
                    files: {},
                    images: {},
                    recentItems: [],
                },
            });

            emittedIDs = [] as number[][];
            subscription = state
                .select(state => state.folder.pages.list)
                .subscribe(list => emittedIDs.push(list));
        });

        afterEach(() => {
            if (subscription) {
                subscription.unsubscribe();
            }
        });

        it('applies the updates in batches', fakeAsync(() => {
            state.dispatch(new ListFetchingSuccessAction('page', {
                folderId: 111,
                nodeId: 222,
                items: testPages,
                fetchAll: true,
                hasMore: false,
                total: testPages.length,
                schema: pageSchema,
            }));
            tick();

            expect(emittedIDs.length).toBe(4);
            expect(emittedIDs[1].slice(0, 20))
                .toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]);
            expect(state.now.entities.page[1]).toBeDefined('page 1 not in entity state');
            expect(state.now.entities.page[20]).toBeDefined('page 20 not in entity state');
            expect(emittedIDs[1].slice(20, 30))
                .toEqual([1, 1, 1, 1, 1, 1, 1, 1, 1, 1]);

            expect(emittedIDs[2].slice(0, 20))
                .toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
                    'first 20 items changed when storing second batch');
            expect(state.now.entities.page[20]).toBeDefined('page 20 not in entity state after second batch');
            expect(emittedIDs[2].slice(20, 40))
                .toEqual([21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40]);
            expect(state.now.entities.page[21]).toBeDefined('page 21 not in entity state after second batch');
            expect(state.now.entities.page[40]).toBeDefined('page 40 not in entity state after second batch');
            expect(emittedIDs[2].slice(40, 47)).toEqual([1, 1, 1, 1, 1, 1, 1], 'items 40-46 not as expected after second batch');

            expect(emittedIDs[3].length).toBe(testPages.length, 'id list has wrong size');
            expect(emittedIDs[3].slice(0, 20))
                .toEqual([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20],
                    'first 20 items changed when storing third batch');
            expect(state.now.entities.page[20]).toBeDefined('page 20 not in entity state after third batch');
            expect(emittedIDs[3].slice(20, 40))
                .toEqual([21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40]);
            expect(state.now.entities.page[40]).toBeDefined('page 40 not in entity state after third batch');
            expect(emittedIDs[3].slice(40, 47))
                .toEqual([41, 42, 43, 44, 45, 46, 47], 'items 40-46 not as expected after third batch');
            expect(state.now.entities.page[40]).toBeDefined('page 40 not in entity state after third batch');
            expect(state.now.entities.page[46]).toBeDefined('page 46 not in entity state after third batch');

            const invalidIds = state.now.folder.pages.list
                .filter((id, index) => id !== index + 1);
            expect(invalidIds).toEqual([], 'some IDs are not as expected');

            tick(15);
            expect(emittedIDs.length).toBe(4, 'ID list was emitted too often');
        }));

        it('works for the initial loaded 10 items', fakeAsync(() => {
            state.dispatch(new ListFetchingSuccessAction('page', {
                folderId: 111,
                nodeId: 222,
                fetchAll: false,
                hasMore: true,
                items: testPages.slice(0, 10),
                total: testPages.length,
                schema: pageSchema,
            }));
            expect(emittedIDs.length).toBe(2);

            tick(15);
            expect(emittedIDs.length).toBe(2, 'list updated after a timeout');

            tick(15);
            expect(emittedIDs.length).toBe(2, 'list updated after second timeout');

            tick(15);
            expect(emittedIDs.length).toBe(2, 'list updated after third timeout');

            expect(state.now.entities.page[testPages[9].id])
                .toBeDefined('page not saved to entity state');
        }));
    });

    it('fetchItemListError works', () => {
        mockListState('pages', { fetching: true });
        const errorMessage = 'some error';
        state.dispatch(new ListFetchingErrorAction('page', errorMessage));
        expect(state.now.folder.pages.fetching).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('fetchNodeLanguagesStart works', () => {
        expect(state.now.folder.activeNodeLanguages.fetching).toBe(false);
        state.dispatch(new StartListFetchingAction('activeNodeLanguages', undefined, true));
        expect(state.now.folder.activeNodeLanguages.fetching).toBe(true);
    });

    it('fetchNodeLanguagesSuccess works', () => {
        mockListState('activeNodeLanguages', { fetching: true });

        const languages: Language[] = [
            { code: 'de', name: 'Deutsch (German)', id: 1 },
            { code: 'en', name: 'English', id: 2 },
        ];

        state.dispatch(new LanguageFetchingSuccessAction(languages));
        expect(state.now.folder.activeNodeLanguages.fetching).toBe(false, 'fetching != false');
        expect(state.now.folder.activeNodeLanguages.list).toEqual([1, 2]);
        expect(state.now.folder.activeNodeLanguages.total).toBe(2);
        expect(state.now.folder.activeNodeLanguages.hasMore).toBe(false, 'hasMore != false');
        expect(state.now.entities.language[1]).toBeDefined('language 1 not in entity state');
        expect(state.now.entities.language[2]).toBeDefined('language 2 not in entity state');
    });

    it('fetchNodeLanguagesError works', () => {
        mockListState('activeNodeLanguages', { fetching: true });
        const errorMessage = 'fetching failed';
        state.dispatch(new ListFetchingErrorAction('activeNodeLanguages', errorMessage));
        expect(state.now.folder.activeNodeLanguages.fetching).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('fetchNodesStart works', () => {
        expect(state.now.folder.nodes.fetching).toBe(false);
        state.dispatch(new StartListFetchingAction('nodes', undefined, true));
        expect(state.now.folder.nodes.fetching).toBe(true);
    });

    it('fetchNodesSuccess works', () => {
        mockListState('nodes', { fetching: true });
        const user = getExampleUserData();
        const folders = [
            getExampleFolderData({ id: 111 }),
            getExampleFolderData({ id: 222 }),
        ];
        const nodes = [
            {
                id: 1,
                name: 'GCN5 Demo',
                type: 'node',
                creator: user,
                editor: user,
                folderId: 111,
            },
            {
                id: 2,
                name: 'Channel of GCN5 Demo',
                type: 'channel',
                creator: user,
                editor: user,
                folderId: 222,
            },
        ] as Node<Raw>[];

        state.dispatch(new NodeFetchingSuccessAction(folders, nodes));
        expect(state.now.folder.nodes.fetching).toBe(false);
        expect(state.now.folder.nodes.hasMore).toBe(false, 'hasMore != false');
        expect(state.now.folder.nodes.list).toEqual([1, 2]);
        expect(state.now.folder.nodes.total).toBe(2);
        expect(state.now.entities.node[1]).toBeDefined('node 1 not in entity state');
        expect(state.now.entities.node[2]).toBeDefined('node 2 not in entity state');
        expect(state.now.entities.user[user.id]).toBeDefined('user not in entity state');
        expect(state.now.entities.folder[111]).toBeDefined('folder 111 not in entity state');
        expect(state.now.entities.folder[222]).toBeDefined('folder 222 not in entity state');
    });

    it('fetchNodesError works', () => {
        mockListState('nodes', { fetching: true });
        const errorMessage = 'fetching nodes failed';
        state.dispatch(new ListFetchingErrorAction('nodes', errorMessage));
        expect(state.now.folder.nodes.fetching).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('fetchNodeStart works', () => {
        expect(state.now.folder.nodes.fetching).toBe(false);
        state.dispatch(new StartListFetchingAction('nodes', undefined, true));
        expect(state.now.folder.nodes.fetching).toBe(true);
    });

    it('fetchNodeSuccess works', () => {
        mockListState('nodes', { fetching: true });
        const user = getExampleUserData();
        const folders = [
            getExampleFolderData({ id: 111 }),
            getExampleFolderData({ id: 222 }),
        ];
        const node = {
            id: 1,
            name: 'GCN5 Demo',
            type: 'node',
            creator: user,
            editor: user,
            folderId: 111,
        } as Node<Raw>;

        state.dispatch(new ItemFetchingSuccessAction('nodes', node));

        expect(state.now.folder.nodes.fetching).toBe(false);
        expect(state.now.folder.nodes.hasMore).toBe(false, 'hasMore != false');
        expect(state.now.entities.node[1]).toBeDefined('node 1 not in entity state');
        expect(state.now.entities.user[user.id]).toBeDefined('user not in entity state');
    });

    it('fetchNodeError works', () => {
        mockListState('nodes', { fetching: true });
        const errorMessage = 'fetching node failed';
        state.dispatch(new ListFetchingErrorAction('nodes', errorMessage));
        expect(state.now.folder.nodes.fetching).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('fetchSyncReportStart works', () => {
        expect(state.now.folder.channelSyncReport.fetching).toBe(false);
        state.dispatch(new StartChannelSyncReportFetchingAction());
        expect(state.now.folder.channelSyncReport.fetching).toBe(true);
    });

    it('fetchSyncReportSuccess works', () => {
        mockListState('channelSyncReport', { fetching: true });
        state.dispatch(new ChannelSyncReportFetchingSuccessAction({
            files: [],
            folders: [11, 22],
            images: [],
            pages: [33],
            templates: [],
            forms: [],
        }));

        expect(state.now.folder.channelSyncReport).toEqual({
            fetching: false,
            files: [],
            folders: [11, 22],
            images: [],
            pages: [33],
            templates: [],
            forms: [],
        });
    });

    it('fetchSyncReportError works', () => {
        mockListState('channelSyncReport', { fetching: true });
        const errorMessage = 'fetching sync report failed';
        state.dispatch(new ChannelSyncReportFetchingErrorAction(errorMessage));
        expect(state.now.folder.channelSyncReport.fetching).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('localizeItemStart works', () => {
        expect(state.now.folder.files.saving).toBe(false);
        state.dispatch(new StartListSavingAction('file'));
        expect(state.now.folder.files.saving).toBe(true);
    });

    it('localizeItemSuccess works', () => {
        mockListState('files', { saving: true });
        expect(state.now.folder.files.saving).toBe(true);
        state.dispatch(new ListSavingSuccessAction('file'));
        expect(state.now.folder.files.saving).toBe(false);
    });

    it('localizeItemError works', () => {
        mockListState('files', { saving: true });
        expect(state.now.folder.files.saving).toBe(true);
        const errorMessage = 'some error';
        state.dispatch(new ListSavingErrorAction('file', errorMessage));
        expect(state.now.folder.files.saving).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('moveItemStart works', () => {
        expect(state.now.folder.files.saving).toBe(false);
        state.dispatch(new StartListSavingAction('file'));
        expect(state.now.folder.files.saving).toBe(true);
    });

    it('moveItemSuccess works', () => {
        mockListState('files', { saving: true });
        expect(state.now.folder.files.saving).toBe(true);
        state.dispatch(new ListSavingSuccessAction('file'));
        expect(state.now.folder.files.saving).toBe(false);
    });

    it('moveItemError works', () => {
        mockListState('files', { saving: true });
        expect(state.now.folder.files.saving).toBe(true);
        const errorMessage = 'some error';
        state.dispatch(new ListSavingErrorAction('file', errorMessage));
        expect(state.now.folder.files.saving).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('publishPageStart works', () => {
        expect(state.now.folder.pages.saving).toBe(false);
        state.dispatch(new StartListSavingAction('page'));
        expect(state.now.folder.pages.saving).toBe(true);
    });

    // describe('publishPageSuccess', () => {
    //     let page: Page<Normalized>;
    //     let folder: Folder<Normalized>;
    //     let language: Language;

    //     function timestampInDays(days: number): number {
    //         return Math.round(Date.now() / 1000) + days * 24 * 60 * 60;
    //     }

    //     beforeEach(() => {
    //         folder = getExampleFolderDataNormalized({ id: 1111 });
    //         language = {
    //             id: 99,
    //             code: 'de',
    //             name: 'Deutsch (German)',
    //         };
    //         page = getExamplePageDataNormalized({ id: 1234 });
    //         page.folderId = folder.id;
    //         page.modified = true;
    //         page.online = false;
    //         page.language = language.code;
    //         page.languageName = language.name;

    //         state.mockState({
    //             folder: {
    //                 pages: {
    //                     saving: true,
    //                 } as ItemsInfo,
    //             },
    //             entities: {
    //                 folder: {
    //                     [folder.id]: folder,
    //                 },
    //                 language: {
    //                     [language.id]: language,
    //                 },
    //                 page: {
    //                     [page.id]: page,
    //                 },
    //             },
    //         });
    //     });

    // it('works when the user has no publish permission', () => {
    //     folder.privilegeMap.privileges.publishpage = false;
    //     folder.privilegeMap.languages = {};
    //     folder.privileges = (folder.privileges || []).filter(p => p !== 'publishpage');

    //     state.actions.folder.publishPageSuccess();
    //     expect(state.now.folder.pages.saving).toBe(false);

    //     const pageEntity = state.now.entities.page[page.id];
    //     expect(pageEntity).toBeDefined();
    //     expect(pageEntity.queued).toBe(false);
    //     expect(pageEntity.online).toBe(false);
    // });

    // it('works when the user has the permission to publish the page', () => {
    //     folder.privilegeMap.privileges.publishpage = true;
    //     folder.privilegeMap.languages = {};
    //     folder.privileges = (folder.privileges || []).filter(p => p !== 'publishpage').concat('publishpage');

    //     state.actions.folder.publishPageSuccess();
    //     expect(state.now.folder.pages.saving).toBe(false);

    //     const pageEntity = state.now.entities.page[page.id];
    //     expect(pageEntity).toBeDefined();
    //     expect(pageEntity.online).toBe(true, 'online != true');
    // });

    // it('works when the user has the permission to publish the page in the specific language', () => {
    //     folder.privilegeMap.privileges.publishpage = false;
    //     folder.privilegeMap.languages = {
    //         [language.id]: {
    //             publishpage: true
    //         } as RolePrivileges
    //     };
    //     folder.privileges = (folder.privileges || []).filter(p => p !== 'publishpage');

    //     state.actions.folder.publishPageSuccess();
    //     expect(state.now.folder.pages.saving).toBe(false);

    //     const pageEntity = state.now.entities.page[page.id];
    //     expect(pageEntity).toBeDefined();
    //     expect(pageEntity.online).toBe(true, 'online != true');
    // });

    // it('correctly updates the status when the page is planned for publishing later', () => {
    //     // SUP-4346
    //     folder.privilegeMap.privileges.publishpage = true;
    //     folder.privileges = (folder.privileges || []).filter(p => p !== 'publishpage').concat('publishpage');
    //     page.timeManagement = {
    //         at: timestampInDays(3),
    //         offlineAt: 0
    //     };
    //     page.online = false;

    //     state.actions.folder.publishPageSuccess();
    //     expect(state.now.folder.pages.saving).toBe(false);

    //     const pageEntity = state.now.entities.page[page.id];
    //     expect(pageEntity).toBeDefined();
    //     expect(pageEntity.online).toBe(false, 'online was set to true');
    // });

    // it('correctly updates the status when the page is out of its time frame', () => {
    //     // SUP-4346
    //     folder.privilegeMap.privileges.publishpage = true;
    //     folder.privileges = (folder.privileges || []).filter(p => p !== 'publishpage').concat('publishpage');
    //     page.timeManagement = {
    //         at: timestampInDays(5),
    //         offlineAt: timestampInDays(15),
    //     };
    //     page.online = true;

    //     state.actions.folder.publishPageSuccess();
    //     expect(state.now.folder.pages.saving).toBe(false);

    //     const pageEntity = state.now.entities.page[page.id];
    //     expect(pageEntity).toBeDefined();
    //     expect(pageEntity.online).toBe(false, 'online was set to true');
    // });

    // });

    it('restorePageVersionSuccess works', () => {
        const rawPage = getExamplePageData({ id: 33 });
        const normalizedPage = getExamplePageDataNormalized({ id: 33 });

        state.mockState({
            entities: {
                page: {
                    [normalizedPage.id]: normalizedPage,
                },
                user: {},
            },
        });
        const restoredPage: Page<Raw> = {
            ...rawPage,
            currentVersion: {
                editor: {},
                number: '0.73',
                timestamp: 1513007164,
            } as any,
            locked: true,
            lockedSince: new Date().getTime(),
            lockedBy: rawPage.creator,
        };
        const normalizedRestore = normalize(restoredPage, pageSchema);

        state.dispatch(new UpdateEntitiesAction(normalizedRestore.entities));

        expect(state.now.entities.page[33].currentVersion).toBeDefined();
        expect(state.now.entities.page[33].currentVersion).toEqual(restoredPage.currentVersion);
        expect(state.now.entities.page[33].locked).toBe(restoredPage.locked);
        expect(state.now.entities.page[33].lockedSince).toBe(restoredPage.lockedSince);
        expect(state.now.entities.page[33].lockedBy).toBeDefined();
        expect(state.now.entities.page[33].lockedBy).toEqual(restoredPage.lockedBy.id);
    });

    it('restorePageVersionSuccess works without page.lockedBy', () => {
        const rawPage = getExamplePageData({ id: 33 });
        const normalizedPage = getExamplePageDataNormalized({ id: 33 });
        state.mockState({
            entities: {
                page: {
                    [normalizedPage.id]: normalizedPage,
                },
                user: {},
            },
        });
        const restoredPage: Page<Raw> = {
            ...rawPage,
            currentVersion: {
                editor: {},
                number: '0.73',
                timestamp: 1513007164,
            } as any,
            locked: false,
            lockedSince: -1,
        };
        // Sometimes the GCMS REST API returns a restored page as not being locked.
        if (typeof restoredPage.lockedBy !== 'undefined') {
            delete restoredPage.lockedBy;
        }

        state.dispatch(new UpdateEntitiesAction({
            page: {
                [restoredPage.id]: {
                    currentVersion: restoredPage.currentVersion,
                    locked: restoredPage.locked,
                    lockedSince: restoredPage.lockedSince,
                    lockedBy: undefined,
                },
            },
        }));

        expect(state.now.entities.page[33].currentVersion).toBeDefined();
        expect(state.now.entities.page[33].currentVersion).toEqual(restoredPage.currentVersion);
        expect(state.now.entities.page[33].locked).toBe(restoredPage.locked);
        expect(state.now.entities.page[33].lockedSince).toBe(restoredPage.lockedSince);
        expect(state.now.entities.page[33].lockedBy).toBeUndefined();
    });

    it('publishPageError works', () => {
        mockListState('pages', { saving: true });
        expect(state.now.folder.pages.saving).toBe(true);
        const errorMessage = 'failed to publish';
        state.dispatch(new ListSavingErrorAction('page', errorMessage));
        expect(state.now.folder.pages.saving).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('pushToMasterStart works', () => {
        expect(state.now.folder.pages.saving).toBe(false);
        state.dispatch(new StartListSavingAction('page'));
        expect(state.now.folder.pages.saving).toBe(true);
    });

    it('pushToMasterSuccess works', () => {
        mockListState('pages', { saving: true });
        expect(state.now.folder.pages.saving).toBe(true);
        state.dispatch(new ListSavingSuccessAction('page'));
        expect(state.now.folder.pages.saving).toBe(false);
    });

    it('pushToMasterError works', () => {
        mockListState('pages', { saving: true });
        expect(state.now.folder.pages.saving).toBe(true);
        const errorMessage = 'some error';
        state.dispatch(new ListSavingErrorAction('page', errorMessage));
        expect(state.now.folder.pages.saving).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('recentItemsLoaded filters out invalid items', () => {
        state.mockState({
            folder: {
                recentItems: [],
            },
        });
        expect(state.now.folder.recentItems).toEqual([]);
        state.dispatch(new RecentItemsFetchingSuccessAction([
            { id: 1337, type: 'file', name: 'l33t.mp4', nodeId: 1 },
            { id: NaN, type: 'page', name: 'invalid', nodeId: 1 },
            { id: '123', type: 'folder', name: 'invalid', nodeId: 1 },
            { type: 'form', name: 'invalid', nodeId: 1 },
            { id: { foo: 'bar' }, type: 'file', name: 'invalid', nodeId: 1 },
            { id: [123], type: 'page', name: 'invalid', nodeId: 1 },
            { id: true, type: 'file', name: 'still-invalid', nodeId: 1 },
            { id: 456, type: 'unknown', name: 'invalid', nodeId: 1 },
            { id: 789, type: 999, name: 'invalid', nodeId: 1 },
            { id: 100, name: 'invalid', nodeId: 1 },
            { id: 101, type: Infinity, name: 'invalid', nodeId: 1 },
            { id: 102, type: true, name: 'invalid', nodeId: 1 },
            { id: 103, type: { nested: { test: 123 } }, name: 'invalid', nodeId: 1 },
            { id: 104, type: ['page'], name: 'invalid', nodeId: 1 },
        ] as any));
        expect(state.now.folder.recentItems).toEqual([
            { id: 1337, type: 'file', name: 'l33t.mp4', nodeId: 1 },
        ] as any);
    });

    it('searchPagesStart works', () => {
        expect(state.now.folder.pages.fetching).toBe(false);
        state.dispatch(new StartListFetchingAction('page', undefined, true));
        expect(state.now.folder.pages.fetching).toBe(true);
    });

    it('searchPagesSuccess works', () => {
        mockListState('pages', { fetching: true });
        const foundPage = getExamplePageData({ id: 1234 });
        state.dispatch(new ItemFetchingSuccessAction('page', foundPage));
        expect(state.now.folder.pages.fetching).toBe(false);
        expect(state.now.entities.page[1234]).toBeDefined('page not added to entity state');
    });

    it('searchPagesError works', () => {
        mockListState('pages', { fetching: true });
        expect(state.now.folder.pages.fetching).toBe(true);
        const errorMessage = 'some error';
        state.dispatch(new ListFetchingErrorAction('page', errorMessage));
        expect(state.now.folder.pages.fetching).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    describe('setActiveFolder', () => {

        it('updates activeFolder id', () => {
            state.dispatch(new SetActiveFolderAction(1234));
            expect(state.now.folder.activeFolder).toBe(1234);
        });

        it('resets currentPage values for folders, pages, images, files', () => {
            state.mockState({
                folder: {
                    folders: { currentPage: 2 },
                    pages: { currentPage: 2 },
                    images: { currentPage: 2 },
                    files: { currentPage: 2 },
                },
            });
            state.dispatch(new SetActiveFolderAction(1234));
            const folderState = state.now.folder;
            expect(folderState.folders.currentPage).toBe(1);
            expect(folderState.pages.currentPage).toBe(1);
            expect(folderState.images.currentPage).toBe(1);
            expect(folderState.files.currentPage).toBe(1);
        });

    });

    it('setActiveLanguage works', () => {
        state.dispatch(new SetFolderLanguageAction(9876));
        expect(state.now.folder.activeLanguage).toBe(9876);
    });

    it('setActiveNode works', () => {
        state.dispatch(new SetActiveNodeAction(7777));
        expect(state.now.folder.activeNode).toBe(7777);
    });

    it('setDisplayAllLanguages works', () => {
        state.dispatch(new SetDisplayAllLanguagesAction(true));
        expect(state.now.folder.displayAllLanguages).toBe(true);

        state.dispatch(new SetDisplayAllLanguagesAction(false));
        expect(state.now.folder.displayAllLanguages).toBe(false);
    });

    it('setDisplayFields works', () => {
        state.dispatch(new SetListDisplayFieldsAction('folder', ['id', 'name', 'cdate']));
        expect(state.now.folder.folders.displayFields).toEqual(['id', 'name', 'cdate']);
    });

    it('setDisplayFields filters out invalid values', () => {
        const types: (FolderItemOrTemplateType)[] = ['file', 'folder', 'form', 'image', 'page'];
        const testEntries = [
            {
                input: ['id', { foo: 'bar' }, 123, 'cdate', false, 'filename', null],
                expected: ['id', 'cdate', 'filename'],
            },
            {
                input: ['cdate', ['foo'], [['bar'], 123], 'cdate', false, 'globalId', undefined],
                expected: ['cdate', 'globalId'],
            },
            {
                input: NaN,
                expected: [],
            },
            {
                input: 'filename',
                expected: ['filename'],
            },
            {
                input: ['creator', Infinity, true, 'id', 'unknown'],
                expected: ['creator', 'id', 'unknown'],
            },
        ];

        types.forEach(type => {
            // Test all entries
            testEntries.forEach(entry => {
                // Reset the type value to empty
                mockListState(plural[type], { displayFields: [] });
                // Set tht test data and check it
                state.dispatch(new SetListDisplayFieldsAction(type, entry.input as any));
                expect(state.now.folder[plural[type]].displayFields).toEqual(entry.expected);
            });
        });
    });

    it('setDisplayFieldsRepositoryBrowser works', () => {
        const selection = ['id', 'name', 'cdate'];
        state.dispatch(new SetRepositoryBrowserDisplayFieldsAction('folder', {
            selection: [...selection],
            showPath: true,
        }));
        expect(state.now.folder.folders.displayFieldsRepositoryBrowser).toEqual({ selection, showPath: true });
    });

    describe('setFilterTerm', () => {

        it('updates the value of filterTerm', () => {
            const term = 'page with';
            state.dispatch(new SetFilterTermAction(term));
            expect(state.now.folder.filterTerm).toBe(term);
        });

        it('resets currentPage values for folders, pages, images, files', () => {
            state.mockState({
                folder: {
                    folders: { currentPage: 2 },
                    pages: { currentPage: 2 },
                    images: { currentPage: 2 },
                    files: { currentPage: 2 },
                },
            });
            state.dispatch(new SetFilterTermAction('page with'));
            const folderState = state.now.folder;
            expect(folderState.folders.currentPage).toBe(1);
            expect(folderState.pages.currentPage).toBe(1);
            expect(folderState.images.currentPage).toBe(1);
            expect(folderState.files.currentPage).toBe(1);
        });

    });

    describe('setSearchTerm', () => {

        it('updates the value of searchTerm', () => {
            const term = 'home';
            state.dispatch(new SetSearchTermAction(term));
            expect(state.now.folder.searchTerm).toBe(term);
        });

        it('resets currentPage values for folders, pages, images, files', () => {
            state.mockState({
                folder: {
                    folders: { currentPage: 2 },
                    pages: { currentPage: 2 },
                    images: { currentPage: 2 },
                    files: { currentPage: 2 },
                },
            });
            state.dispatch(new SetSearchTermAction('home'));
            const folderState = state.now.folder;
            expect(folderState.folders.currentPage).toBe(1);
            expect(folderState.pages.currentPage).toBe(1);
            expect(folderState.images.currentPage).toBe(1);
            expect(folderState.files.currentPage).toBe(1);
        });

    });

    it('setSearchFiltersVisible works', () => {
        expect(state.now.folder.searchFiltersVisible).toBe(false);

        state.dispatch(new SetSearchFiltersVisibleAction(true));
        expect(state.now.folder.searchFiltersVisible).toBe(true);

        state.dispatch(new SetSearchFiltersVisibleAction(false));
        expect(state.now.folder.searchFiltersVisible).toBe(false);
    });

    it('setSorting works', () => {
        state.dispatch(new SetListSortingAction('file', 'filesize', 'desc'));
        expect(state.now.folder.files.sortBy).toBe('filesize');
        expect(state.now.folder.files.sortOrder).toBe('desc');
    });

    it('setSorting correctly validates and correct malformed options', () => {
        const types: (FolderItemOrTemplateType)[] = ['file', 'folder', 'form', 'image', 'page'];
        const testEntries = [
            {
                input: { sortBy: 'name' },
                expected: { sortBy: 'name', sortOrder: 'asc' },
            },
            {
                input: { sortBy: 'cdate', sortOrder: 'desc' },
                expected: { sortBy: 'cdate', sortOrder: 'desc' },
            },
            {
                input: { sortBy: 'edate' },
                expected: { sortBy: 'edate', sortOrder: 'asc' },
            },
            {
                input: { sortBy: 'name', sortOrder: 'invalid' },
                expected: { sortBy: 'name', sortOrder: 'asc' },
            },
            {
                input: {},
                expected: { sortBy: 'name', sortOrder: 'asc' },
            },
            {
                input: { sortBy: Infinity, sortOrder: true },
                expected: { sortBy: 'name', sortOrder: 'asc' },
            },
        ];

        types.forEach(type => {
            // Test all entries
            testEntries.forEach(entry => {
                // Reset the type value to empty
                mockListState(plural[type], { sortBy: null, sortOrder: null });
                // Set tht test data and check it
                state.dispatch(new SetListSortingAction(type, entry.input.sortBy as any, entry.input.sortOrder as any));
                expect(state.now.folder[plural[type]].sortBy).toEqual(entry.expected.sortBy);
                expect(state.now.folder[plural[type]].sortOrder).toEqual(entry.expected.sortOrder);
            });
        });
    });

    it('takePagesOfflineStart works', () => {
        expect(state.now.folder.pages.saving).toBe(false);
        state.dispatch(new StartListSavingAction('page'));
        expect(state.now.folder.pages.saving).toBe(true);
    });

    it('takePagesOfflineSuccess works', () => {
        const page = getExamplePageDataNormalized({ id: 1234 });
        page.online = true;
        state.mockState({
            folder: {
                pages: {
                    saving: true,
                } as ItemsInfo,
            },
            entities: {
                page: {
                    [page.id]: page,
                },
            },
        });

        expect(state.now.folder.pages.saving).toBe(true);
        state.dispatch(new ListSavingSuccessAction('page'));
        const pageUpdates: { [id: number]: Partial<Page<Normalized>> } = {
            [page.id]: {
                online: false,
            },
        };
        state.dispatch(new UpdateEntitiesAction({ page: pageUpdates }));
        expect(state.now.folder.pages.saving).toBe(false);

        const pageEntity = state.now.entities.page[page.id];
        expect(pageEntity).toBeDefined('page not in entity state');
        expect(pageEntity.online).toBe(false, 'online != false');
    });

    it('takePagesOfflineError works', () => {
        mockListState('pages', { saving: true });
        expect(state.now.folder.pages.saving).toBe(true);
        const errorMessage = 'some error';
        state.dispatch(new ListSavingErrorAction('page', errorMessage));
        expect(state.now.folder.pages.saving).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('translatePageStart works', () => {
        expect(state.now.folder.pages.creating).toBe(false);
        state.dispatch(new StartListCreatingAction('page'));
        expect(state.now.folder.pages.creating).toBe(true);
    });

    it('translatePageSuccess works', () => {
        const german: Language = { id: 1, code: 'de', name: 'Deutsch (German)' };
        const english: Language = { id: 2, code: 'en', name: 'English' };

        const originalPage = getExamplePageDataNormalized({ id: 1234 });
        originalPage.languageVariants = { [german.id]: 1234 };

        state.mockState({
            folder: {
                pages: {
                    creating: true,
                } as ItemsInfo,
            },
            entities: {
                page: {
                    [originalPage.id]: originalPage,
                },
                language: {
                    [german.id]: german,
                    [english.id]: english,
                },
                user: {},
            },
        });

        // Translate page from german to english
        const translatedPage = getExamplePageData({ id: 5555 });
        translatedPage.language = english.code;
        translatedPage.languageName = english.name;
        // The GCMS API does not return this data
        translatedPage.languageVariants = undefined;

        state.dispatch(new ListCreatingSuccessAction('page'));
        expect(state.now.folder.pages.creating).toBe(false);

        const newPage = translatedPage;
        const oldPageId = originalPage.id;
        const entityState = state.now.entities;
        const normalized = normalize(newPage, pageSchema);
        // Add the ID of the new page to the languageVariants of the original page and add
        // the language variants to the new page (the API does not return the language variants).
        const languageVariants = {
            ...entityState.page[oldPageId].languageVariants,
            [newPage.contentGroupId]: newPage.id,
        };
        state.dispatch(new AddEntitiesAction(normalized));
        state.dispatch(new UpdateEntitiesAction({
            page: {
                [oldPageId]: { languageVariants },
                [newPage.id]: { languageVariants },
            },
        }));

        const originalEntity = state.now.entities.page[originalPage.id];
        expect(originalEntity.languageVariants).toEqual({
            [german.id]: originalPage.id,
            [english.id]: translatedPage.id,
        }, 'language variants of original page are wrong');

        const translatedEntity = state.now.entities.page[translatedPage.id];
        expect(translatedEntity).toBeDefined('translated page not in entity state');
        expect(translatedEntity.languageVariants).toEqual({
            [german.id]: originalPage.id,
            [english.id]: translatedPage.id,
        }, 'language variants of translated page are wrong');
    });

    it('translatePageError works', () => {
        mockListState('pages', { creating: true });
        expect(state.now.folder.pages.creating).toBe(true);
        const errorMessage = 'some error';
        state.dispatch(new ListCreatingErrorAction('page', errorMessage));
        expect(state.now.folder.pages.creating).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('unlocalizeItemStart works', () => {
        expect(state.now.folder.folders.saving).toBe(false);
        state.dispatch(new StartListSavingAction('folder'));
        expect(state.now.folder.folders.saving).toBe(true);
    });

    it('unlocalizeItemSuccess works', () => {
        mockListState('folders', { saving: true });
        expect(state.now.folder.folders.saving).toBe(true);
        state.dispatch(new ListSavingSuccessAction('folder'));
        expect(state.now.folder.folders.saving).toBe(false);
    });

    it('unlocalizeItemError works', () => {
        mockListState('folders', { saving: true });
        expect(state.now.folder.folders.saving).toBe(true);
        const errorMessage = 'some error';
        state.dispatch(new ListSavingErrorAction('folder', errorMessage));
        expect(state.now.folder.folders.saving).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('updateInheritanceStart works', () => {
        expect(state.now.folder.folders.saving).toBe(false);
        state.dispatch(new StartListSavingAction('folder'));
        expect(state.now.folder.folders.saving).toBe(true);
    });

    it('updateInheritanceSuccess works', () => {
        const folder = getExampleFolderDataNormalized({ id: 1234 });
        folder.disinherit = [];
        folder.disinheritDefault = false;
        folder.excluded = false;

        state.mockState({
            folder: {
                folders: {
                    saving: true,
                } as ItemsInfo,
            },
            entities: {
                folder: {
                    [folder.id]: folder,
                },
            },
        });

        expect(state.now.folder.folders.saving).toBe(true);
        state.dispatch(new ListSavingSuccessAction('folder'));
        state.dispatch(new UpdateEntitiesAction({
            folder: {
                [folder.id]: {
                    disinherit: [5, 6, 7],
                    disinheritDefault: true,
                    excluded: true,
                },
            },
        }));
        expect(state.now.folder.folders.saving).toBe(false);

        const folderEntity = state.now.entities.folder[folder.id];
        expect(folderEntity).toBeDefined('updated folder not in state');
        expect(folderEntity.disinherit).toEqual([5, 6, 7], 'disinherit has the wrong value');
        expect(folderEntity.disinheritDefault).toBe(true, 'disinheritDefault != true');
        expect(folderEntity.excluded).toBe(true, 'excluded != true');
    });

    it('updateInheritanceError works', () => {
        mockListState('folders', { saving: true });
        expect(state.now.folder.folders.saving).toBe(true);
        const errorMessage = 'some error';
        state.dispatch(new ListSavingErrorAction('folder', errorMessage));
        expect(state.now.folder.folders.saving).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

    it('updateItemStart works', () => {
        expect(state.now.folder.pages.saving).toBe(false);
        state.dispatch(new StartListSavingAction('page'));
        expect(state.now.folder.pages.saving).toBe(true);
    });

    describe('updateItemSuccess', () => {

        let page: Page<Normalized>;

        beforeEach(() => {
            page = getExamplePageDataNormalized({ id: 1234, userId: 111 });
            page.description = 'No description';
            page.fileName = 'page.html';
            page.creator = 111 as any;
            page.editor = 111 as any;

            state.mockState({
                folder: {
                    pages: {
                        saving: true,
                    } as ItemsInfo,
                },
                entities: {
                    page: {
                        [page.id]: page,
                    },
                    user: {
                        111: {
                            id: 111,
                            firstName: 'First',
                            lastName: 'User',
                        },
                    },
                },
            });
        });

        it('works without passing changed properties', () => {
            expect(state.now.folder.pages.saving).toBe(true);
            state.dispatch(new ListSavingSuccessAction('page'));
            expect(state.now.folder.pages.saving).toBe(false);
        });

        it('works when passed a hash of changed properties', () => {
            expect(state.now.folder.pages.saving).toBe(true);
            expect(state.now.entities.page[page.id].description).toBe('No description');
            expect(state.now.entities.page[page.id].fileName).toBe('page.html');

            const props: Partial<Page<Raw>> = { description: 'A different description', fileName: 'other.html' };
            state.dispatch(new ListSavingSuccessAction('page'));
            const normalized = normalize({ id: page.id, ...props }, getNormalizrSchema('page'));
            state.dispatch(new AddEntitiesAction(normalized));

            expect(state.now.folder.pages.saving).toBe(false);
            expect(state.now.entities.page[page.id].description).toBe('A different description');
            expect(state.now.entities.page[page.id].fileName).toBe('other.html');
        });

        it('correctly normalizes passed properties', () => {
            expect(state.now.folder.pages.saving).toBe(true);
            expect(state.now.entities.page[page.id].editor).toBe(111);
            expect(state.now.entities.user[222]).toBeUndefined();

            const props: Partial<Page<Raw>> = {
                description: 'A different description',
                editor: {
                    id: 222,
                    firstName: 'Second',
                    lastName: 'User',
                } as User<Raw>,
            };
            state.dispatch(new ListSavingSuccessAction('page'));
            const normalized = normalize({ id: page.id, ...props }, getNormalizrSchema('page'));
            state.dispatch(new AddEntitiesAction(normalized));

            expect(state.now.folder.pages.saving).toBe(false);
            expect(state.now.entities.page[page.id].description).toBe('A different description');
            expect(state.now.entities.page[page.id].editor).toBe(222);
            expect(state.now.entities.user[222]).toBeDefined();
        });

    });

    it('updateItemError works', () => {
        mockListState('pages', { saving: true });
        expect(state.now.folder.pages.saving).toBe(true);
        const errorMessage = 'some error';
        state.dispatch(new ListSavingErrorAction('page', errorMessage));
        expect(state.now.folder.pages.saving).toBe(false);
        expect(state.now.folder.lastError).toBe(errorMessage);
    });

});
