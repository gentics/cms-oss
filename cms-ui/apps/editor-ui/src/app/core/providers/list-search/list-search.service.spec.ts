import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { I18nNotificationService, I18nService } from '@gentics/cms-components';
import { NgxsModule } from '@ngxs/store';
import { of } from 'rxjs';
import { first } from 'rxjs/operators';
import { emptyItemInfo } from '../../../common/models';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ApplicationStateService, FolderActionsService, STATE_MODULES } from '../../../state';
import { MockAppState, TestApplicationState } from '../../../state/test-application-state.mock';
import { ErrorHandler } from '../error-handler/error-handler.service';
import { QuickJumpService } from '../quick-jump/quick-jump.service';
import { ListSearchService } from './list-search.service';

class MockErrorHandler {
    catch = jasmine.createSpy('catch');
}

class MockFolderActions {
    searchLiveUrl = jasmine.createSpy('searchLiveUrl').and.returnValue(of({}));
    setFilterTerm = jasmine.createSpy('setFilterTerm');
    setSearchTerm = jasmine.createSpy('setSearchTerm');
}

class MockNavigationService {
    list = jasmine.createSpy('list').and.returnValue({
        commands: jasmine.createSpy('commands').and.returnValue([]),
    });

    instruction = jasmine.createSpy('instruction').and.returnValue({
        router: jasmine.createSpy('router'),
    });

    deserializeOptions = jasmine.createSpy('deserializeOptions').and.returnValue({});
}

class MockQuickJumpService {
    searchPageById = jasmine.createSpy('searchPageById').and.returnValue(Promise.resolve());
}

class MockI18nService implements Partial<I18nService> {
    instant = jasmine.createSpy('instant').and.returnValue('error_text');
}

class MockI18Notification {}

describe('ListSearchService', () => {
    let folderActions: MockFolderActions;
    let quickJumpService: MockQuickJumpService;
    let state: TestApplicationState;
    let listSearchService: ListSearchService;

    const ACTIVE_FOLDER_ID = 1;
    const ACTIVE_NODE_ID = 2;
    const NODE_HOST = 'host.com';
    const PAGE_URL = '/Content.Node/news/111.en.html';
    const PAGE_ID = 42;
    const NODE_PUBLISH_DIR = '/stuff';

    let initialState: MockAppState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                ListSearchService,
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: QuickJumpService, useClass: MockQuickJumpService },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: I18nNotificationService, useClass: MockI18Notification },
                { provide: I18nService, useClass: MockI18nService },
            ],
        });

        state = TestBed.inject(ApplicationStateService) as any;
        folderActions = TestBed.inject(FolderActionsService) as any;
        quickJumpService = TestBed.inject(QuickJumpService) as any;
        listSearchService = TestBed.inject(ListSearchService);

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
                    nodeId: [{ value: ACTIVE_NODE_ID, operator: 'IS' }],
                },
                searchFiltersVisible: false,
                folders: emptyItemInfo,
                images: emptyItemInfo,
                files: emptyItemInfo,
                pages: emptyItemInfo,
            },
        };
        state.mockState(initialState);
    });

    it('recognizes liveUrl search with protocol https', fakeAsync(() => {
        const testFilterTerm = `https://${NODE_HOST}/${PAGE_URL}`;
        let newSearchTerm;
        spyOn(listSearchService, 'searchLiveUrl').and.callThrough();
        listSearchService.searchEvent$.pipe(first()).subscribe((searchTerm) => newSearchTerm = searchTerm);
        listSearchService.search(testFilterTerm, ACTIVE_NODE_ID);
        tick();
        expect(newSearchTerm.term).toBe(testFilterTerm);
        expect(listSearchService.searchLiveUrl).toHaveBeenCalledWith(testFilterTerm);
        expect(folderActions.setFilterTerm).toHaveBeenCalledWith('');
        expect(folderActions.searchLiveUrl).toHaveBeenCalledWith(testFilterTerm);
    }));

    it('recognizes liveUrl search with protocol http', fakeAsync(() => {
        const testFilterTerm = `http://${NODE_HOST}/${PAGE_URL}`;
        let newSearchTerm;
        spyOn(listSearchService, 'searchLiveUrl').and.callThrough();
        listSearchService.searchEvent$.pipe(first()).subscribe((searchTerm) => newSearchTerm = searchTerm);
        listSearchService.search(testFilterTerm, ACTIVE_NODE_ID);
        tick();
        expect(newSearchTerm.term).toBe(testFilterTerm);
        expect(listSearchService.searchLiveUrl).toHaveBeenCalledWith(testFilterTerm);
        expect(folderActions.setFilterTerm).toHaveBeenCalledWith('');
        expect(folderActions.searchLiveUrl).toHaveBeenCalledWith(testFilterTerm);
    }));

    it('recognizes liveUrl search without protocol', fakeAsync(() => {
        const testFilterTerm = `${NODE_HOST}/${PAGE_URL}`;
        let newSearchTerm;
        spyOn(listSearchService, 'searchLiveUrl').and.callThrough();
        listSearchService.searchEvent$.pipe(first()).subscribe((searchTerm) => newSearchTerm = searchTerm);
        listSearchService.search(testFilterTerm, ACTIVE_NODE_ID);
        tick();
        expect(newSearchTerm.term).toBe(testFilterTerm);
        expect(listSearchService.searchLiveUrl).toHaveBeenCalledWith(testFilterTerm);
        expect(folderActions.setFilterTerm).toHaveBeenCalledWith('');
        expect(folderActions.searchLiveUrl).toHaveBeenCalledWith(testFilterTerm);
    }));

    it('recognizes jump-to-id syntax', fakeAsync(() => {
        const testFilterTerm = `jump:${PAGE_ID}`;
        let newSearchTerm;
        listSearchService.searchEvent$.pipe(first()).subscribe((searchTerm) => newSearchTerm = searchTerm);
        listSearchService.search(testFilterTerm, ACTIVE_NODE_ID);
        tick();
        expect(newSearchTerm.term).toBe(testFilterTerm);
        expect(quickJumpService.searchPageById).toHaveBeenCalledWith(PAGE_ID, ACTIVE_NODE_ID);
    }));

});
