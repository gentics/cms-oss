import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Feature, FolderItemTypePlural } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { Subject } from 'rxjs';
import { emptyItemInfo, FolderState, GtxChipSearchPropertyNumberOperators, GtxChipSearchSearchFilterMap, ItemsInfo } from '../../../common/models';
import { ListUrlParams, NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ApplicationStateService, FolderActionsService, SetFeatureAction, SetSearchFiltersChangingAction, STATE_MODULES } from '../../../state';
import { MockAppState, TestApplicationState } from '../../../state/test-application-state.mock';
import { ListService } from './list.service';
import Spy = jasmine.Spy;

const DEBOUNCE_INTERVAL = 50;

class MockFolderActions {
    getAllFolderContents = jasmine.createSpy('getAllFolderContents');
    getAllItemsInFolder = jasmine.createSpy('getAllItemsInFolder');
    setActiveFolder = jasmine.createSpy('setActiveFolder');
    setActiveNode = jasmine.createSpy('setActiveNode');
    setSearchTerm = jasmine.createSpy('setSearchTerm');
    setSearchFilter = jasmine.createSpy('setSearchFilter');
    setSearchFiltersVisible = jasmine.createSpy('setSearchFiltersVisible');
    resetSearchFilters = jasmine.createSpy('resetSearchFilters');
    getFolders = jasmine.createSpy('getFolders');
    getForms = jasmine.createSpy('getForms');
    getPages = jasmine.createSpy('getPages');
    getImages = jasmine.createSpy('getImages');
    getFiles = jasmine.createSpy('getFiles');
}

class MockNavigationService {
    list = jasmine.createSpy('list').and.returnValue({
        commands: jasmine.createSpy('commands').and.returnValue([]),
    });
    deserializeOptions = jasmine.createSpy('deserializeOptions').and.returnValue({});
}

class MockActivatedRoute {
    params = new Subject<ListUrlParams>();
}

describe('ListService', () => {
    let listService: ListService;
    let state: TestApplicationState;
    let folderActions: MockFolderActions;
    let route: MockActivatedRoute;
    let navigationService: MockNavigationService;

    const ACTIVE_FOLDER_ID = 1;
    const ACTIVE_NODE_ID = 2;
    const NODE_HOST = 'host.com';
    const NODE_PUBLISH_DIR = '/stuff';
    const keys: FolderItemTypePlural[]  = ['folders', 'pages', 'files', 'images'];

    let initialState: MockAppState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                NgxsModule.forRoot(STATE_MODULES),
                RouterTestingModule,
            ],
            providers: [
                ListService,
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: NavigationService, useClass: MockNavigationService },
            ],
        });
    });

    /**
     * Creates and initializes the ListService.
     * We cannot do this in beforeEach(), because it needs to be run
     * in the fake async zone of each test.
     */
    function initListService(): void {
        listService = TestBed.get(ListService);
        state = TestBed.get(ApplicationStateService);
        folderActions = TestBed.get(FolderActionsService);
        navigationService = TestBed.get(NavigationService);

        route = new MockActivatedRoute();
        initialState = {
            auth: {
                isLoggedIn: true,
            },
            entities: {
                node: {
                    [ACTIVE_NODE_ID]: {
                        id: ACTIVE_NODE_ID,
                        publishDir: NODE_PUBLISH_DIR,
                        host: NODE_HOST,
                    },
                },
            },
            features: {
                elasticsearch: false,
            },
            folder: {
                activeLanguage: 1,
                activeNode: ACTIVE_NODE_ID,
                activeFolder: ACTIVE_FOLDER_ID,
                searchTerm: '',
                filterTerm: '',
                searchFilters: {
                    nodeId: [ { value: ACTIVE_NODE_ID, operator: 'IS' } ],
                },
                searchFiltersVisible: false,
                folders: emptyItemInfo,
                forms: emptyItemInfo,
                images: emptyItemInfo,
                files: emptyItemInfo,
                pages: emptyItemInfo,
            },
        };
        state.mockState(initialState);

        listService.init(route as any);
        route.params.next({ folderId: ACTIVE_FOLDER_ID, nodeId: ACTIVE_NODE_ID });
    }

    /** Trigger search event by signalling start end end event of filter manipulation */
    function triggerSearchQuery(): void {
        state.dispatch(new SetSearchFiltersChangingAction(false));
        tick(DEBOUNCE_INTERVAL);
    }

    it('only calls getAllFolderContents and no other actions on init', fakeAsync(() => {
        initListService();
        tick(DEBOUNCE_INTERVAL);
        expect(folderActions.getAllFolderContents).toHaveBeenCalledTimes(1);
        expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(0);
        expect(folderActions.getFolders).toHaveBeenCalledTimes(0);
        expect(folderActions.getForms).toHaveBeenCalledTimes(0);
        expect(folderActions.getPages).toHaveBeenCalledTimes(0);
        expect(folderActions.getFiles).toHaveBeenCalledTimes(0);
        expect(folderActions.getImages).toHaveBeenCalledTimes(0);
    }));

    describe('search and filter', () => {

        function setFolderState(partialFolderState: Partial<FolderState>): void {
            state.mockState({ folder: { ...initialState.folder,  ...partialFolderState }});
            tick(DEBOUNCE_INTERVAL);
        }

        describe('search', () => {

            it('gets all items when user enters valid search query', fakeAsync(() => {
                initListService();
                tick(DEBOUNCE_INTERVAL);
                const testSearchFilters = {
                    nodeId: [
                        {
                            value: '10',
                            operator: 'IS',
                        },
                    ],
                    all: [
                        {
                            value: 'test',
                            operator: 'CONTAINS',
                        },
                    ],
                    language: [
                        {
                            value: 'en',
                            operator: 'IS',
                        },
                    ],
                } as GtxChipSearchSearchFilterMap;
                setFolderState({  searchFilters: testSearchFilters, searchFiltersVisible: true, searchFiltersValid: true, searchFiltersChanging: false });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(1);
            }));

            it('gets all items whenever search query changes', fakeAsync(() => {
                initListService();
                tick(DEBOUNCE_INTERVAL);
                const testSearchFilters = {
                    nodeId: [
                        {
                            value: '10',
                            operator: 'IS',
                        },
                    ],
                    all: [
                        {
                            value: 'test',
                            operator: 'CONTAINS',
                        },
                    ],
                    language: [
                        {
                            value: 'en',
                            operator: 'IS',
                        },
                    ],
                } as GtxChipSearchSearchFilterMap;
                setFolderState({  searchFilters: testSearchFilters, searchFiltersVisible: true, searchFiltersValid: true, searchFiltersChanging: false });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(1);

                testSearchFilters.all[0].value = 'testx';
                // Note that `searchFiltersChanging` must be set true in case of filter-value-change only
                setFolderState({  searchFilters: testSearchFilters, searchFiltersVisible: true, searchFiltersValid: true, searchFiltersChanging: true });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(2);

                Object.assign(testSearchFilters, { online: [{ value: true, operator: 'IS' }] });
                setFolderState({  searchFilters: testSearchFilters, searchFiltersVisible: true, searchFiltersValid: true, searchFiltersChanging: false });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(3);
            }));

            it('does not get all items when searchFilters changes and filters are not visible', fakeAsync(() => {
                initListService();
                tick(DEBOUNCE_INTERVAL);
                setFolderState({ searchFilters: { creatorId: [{ value: 1, operator: 'IS' }] } });
                setFolderState({ searchFilters: { creatorId: [{ value: 2, operator: 'IS' }] } });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(0);
            }));

            it('gets all items whenever searchFilters changes and filters are visible', fakeAsync(() => {
                initListService();
                tick(DEBOUNCE_INTERVAL);
                state.dispatch(new SetFeatureAction(Feature.elasticsearch, true));
                triggerSearchQuery();
                setFolderState({ searchFilters: { creatorId: [{ value: 1, operator: 'IS' }] }, searchFiltersVisible: true });
                // setFolderState({ searchFilters: { creatorId: [{ value: 2, operator: 'IS' }] }, searchFiltersVisible: true });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(1);
            }));

            it('does get all items once to initialize', fakeAsync(() => {
                initListService();
                tick(DEBOUNCE_INTERVAL);
                setFolderState({ searchFiltersVisible: true });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(1);
            }));

            it('does fetch items when user has entered qualified search query', fakeAsync(() => {
                initListService();
                tick(DEBOUNCE_INTERVAL);
                setFolderState({ searchTerm: 'foo' });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(0);
                setFolderState({ searchTerm: 'foo', searchFiltersVisible: true, searchFiltersValid: true });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(1);
            }));

            it('gets default items when search query is cleared after setting searchFilters', fakeAsync(() => {
                initListService();
                tick(DEBOUNCE_INTERVAL);
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(0);

                const filters = { creatorId: [{ value: 1, operator: 'IS' as GtxChipSearchPropertyNumberOperators }] };
                setFolderState({ searchFilters: filters, searchFiltersVisible: true, searchFiltersValid: true });
                triggerSearchQuery();
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(1);

                setFolderState({ searchFilters: { creatorId: [] }, searchFiltersVisible: false });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(2);
            }));

        });

        describe('filter', () => {

            it('does not fetch items whenever filterTerm changes', fakeAsync(() => {
                initListService();
                tick(DEBOUNCE_INTERVAL);
                setFolderState({  filterTerm: 'foo' });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(0);
                setFolderState({  filterTerm: 'bar' });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(0);
                setFolderState({  filterTerm: 'baz' });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(0);
            }));

            it('does not get all items when the filter term is a liveUrl', fakeAsync(() => {
                initListService();
                tick(DEBOUNCE_INTERVAL);
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(0);
                setFolderState({  filterTerm: NODE_HOST + NODE_PUBLISH_DIR });
                expect(folderActions.getAllItemsInFolder).toHaveBeenCalledTimes(0);
            }));

        });

    });

    describe('sorting', () => {

        keys.forEach(key => {

            it(`${key} get fetched when sortBy changes`, () => {
                initListService();
                patchItemsInfo(key, { sortBy: 'cdate' });
                expect(getActionSpyForKey(key)).toHaveBeenCalledWith(ACTIVE_FOLDER_ID, false, '');
            });

            it(`${key} get fetched when sortOrder changes`, () => {
                initListService();
                patchItemsInfo(key, { sortOrder: 'desc' });
                expect(getActionSpyForKey(key)).toHaveBeenCalledWith(ACTIVE_FOLDER_ID, false, '');
            });

        });
    });

    it('fetches pages when activeLanguage changes', fakeAsync(() => {
        initListService();
        tick(DEBOUNCE_INTERVAL);
        expect(folderActions.getPages).toHaveBeenCalledTimes(0);
        const newState: MockAppState = { ...initialState, ...{
            folder: {
                ...initialState.folder,
                ...{
                    activeLanguage: 5,
                    searchTerm: 'foo',
                },
            },
        },
        };
        state.mockState(newState);
        tick(DEBOUNCE_INTERVAL);
        expect(folderActions.getPages).toHaveBeenCalledWith(ACTIVE_FOLDER_ID, false, 'foo');
    }));


    /**
     * White box tests to make sure that a bug fix for a race condition is still in place.
     * They check whether the active node in the state is only updated when the node
     * route parameter changes.
     * Earlier the active node was updated if either of the values below changed, causing
     * the currently available languages to be fetched multiple times. In the worse case
     * the response for a request with the wrong node id arrives latest.
     *
     * setActiveNode should only be called when the node route parameter changed.
     * By checking whether setSearchTerm was called, we make sure that the folderIdParam$
     * emits a value, since this method is called in every case. (With the exception that it
     * should not emit a value when activeNode in the state is changed.)
     */
    describe('active node', () => {
        it('is NOT updated when activeFolder is updated', fakeAsync(() => {
            initListService();
            tick(DEBOUNCE_INTERVAL);
            folderActions.setSearchTerm.calls.reset();
            folderActions.setActiveNode.calls.reset();
            expect(folderActions.setSearchTerm).toHaveBeenCalledTimes(0);
            expect(folderActions.setActiveNode).toHaveBeenCalledTimes(0);


            initialState.folder.activeFolder = ACTIVE_FOLDER_ID + 1;
            state.mockState(initialState);
            tick(DEBOUNCE_INTERVAL);
            expect(folderActions.setSearchTerm).toHaveBeenCalledTimes(1);
            expect(folderActions.setActiveNode).toHaveBeenCalledTimes(0);
        }));

        it('is NOT updated when activeNode is updated', fakeAsync(() => {
            initListService();
            tick(DEBOUNCE_INTERVAL);
            folderActions.setSearchTerm.calls.reset();
            folderActions.setActiveNode.calls.reset();
            expect(folderActions.setSearchTerm).toHaveBeenCalledTimes(0);
            expect(folderActions.setActiveNode).toHaveBeenCalledTimes(0);


            initialState.folder.activeNode = ACTIVE_NODE_ID + 1;
            state.mockState(initialState);
            tick(DEBOUNCE_INTERVAL);
            expect(folderActions.setSearchTerm).toHaveBeenCalledTimes(1);
            expect(folderActions.setActiveNode).toHaveBeenCalledTimes(0);
        }));

        it('is NOT updated when search term route parameter is updated', fakeAsync(() => {
            initListService();
            tick(DEBOUNCE_INTERVAL);
            folderActions.setSearchTerm.calls.reset();
            folderActions.setActiveNode.calls.reset();
            expect(folderActions.setSearchTerm).toHaveBeenCalledTimes(0);
            expect(folderActions.setActiveNode).toHaveBeenCalledTimes(0);


            route.params.next({ folderId: ACTIVE_FOLDER_ID, nodeId: ACTIVE_NODE_ID, searchTerm: 'new search term' });
            tick(DEBOUNCE_INTERVAL);
            expect(folderActions.setSearchTerm).toHaveBeenCalledTimes(1);
            expect(folderActions.setActiveNode).toHaveBeenCalledTimes(0);
        }));

        it('is NOT updated when search filters route parameter is updated', fakeAsync(() => {
            initListService();
            tick(DEBOUNCE_INTERVAL);
            folderActions.setSearchTerm.calls.reset();
            folderActions.setActiveNode.calls.reset();
            expect(folderActions.setSearchTerm).toHaveBeenCalledTimes(0);
            expect(folderActions.setActiveNode).toHaveBeenCalledTimes(0);

            /**
             * Create a new spy, s.t. it returns a new deserialization result.
             * This is done to avoid changing the original spy and potentially breaking all previous tests.
             */

            navigationService.deserializeOptions = jasmine.createSpy('deserializeOptions').and.returnValue({ new: 'new search filter' });
            route.params.next({ folderId: ACTIVE_FOLDER_ID, nodeId: ACTIVE_NODE_ID, searchFilters: { new: 'new search filter' } });
            tick(DEBOUNCE_INTERVAL);
            expect(folderActions.setSearchTerm).toHaveBeenCalledTimes(1);
            expect(folderActions.setActiveNode).toHaveBeenCalledTimes(0);
        }));

        it('is updated when respective node route parameter is updated', fakeAsync(() => {
            initListService();
            tick(DEBOUNCE_INTERVAL);
            folderActions.setSearchTerm.calls.reset();
            folderActions.setActiveNode.calls.reset();
            expect(folderActions.setSearchTerm).toHaveBeenCalledTimes(0);
            expect(folderActions.setActiveNode).toHaveBeenCalledTimes(0);


            route.params.next({ folderId: ACTIVE_FOLDER_ID, nodeId: ACTIVE_NODE_ID + 1 });
            tick(DEBOUNCE_INTERVAL);
            expect(folderActions.setSearchTerm).toHaveBeenCalledTimes(1);
            expect(folderActions.setActiveNode).toHaveBeenCalledTimes(1);
        }));
    });

    function patchItemsInfo(key: FolderItemTypePlural, itemsInfo: Partial<ItemsInfo>): void {
        state.mockState({
            ...initialState,
            ...{ folder: {
                ...initialState.folder, ...{
                    [key]: {
                        ...initialState.folder[key], ...itemsInfo,
                    },
                },
            },
            },
        });
    }

    function getActionSpyForKey(key: FolderItemTypePlural): Spy {
        switch (key) {
            case 'folders': return folderActions.getFolders;
            case 'forms': return folderActions.getForms;
            case 'pages': return folderActions.getPages;
            case 'files': return folderActions.getFiles;
            case 'images': return folderActions.getImages;
            default: throw Error('invalid key');
        }
    }
});
