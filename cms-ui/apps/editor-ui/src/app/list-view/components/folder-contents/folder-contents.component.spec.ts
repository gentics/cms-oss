import { ChangeDetectorRef, Component, EventEmitter, Input, NO_ERRORS_SCHEMA, Pipe, PipeTransform, ViewChild } from '@angular/core';
import { TestBed, discardPeriodicTasks, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { EditorPermissions, ItemsInfo, getNoPermissions } from '@editor-ui/app/common/models';
import { ContextMenuOperationsService } from '@editor-ui/app/core/providers/context-menu-operations/context-menu-operations.service';
import { DecisionModalsService } from '@editor-ui/app/core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { FavouritesService } from '@editor-ui/app/core/providers/favourites/favourites.service';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { ListSearchService } from '@editor-ui/app/core/providers/list-search/list-search.service';
import { NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { PermissionService } from '@editor-ui/app/core/providers/permissions/permission.service';
import { ResourceUrlBuilder } from '@editor-ui/app/core/providers/resource-url-builder/resource-url-builder';
import { UploadConflictService } from '@editor-ui/app/core/providers/upload-conflict/upload-conflict.service';
import { UserSettingsService } from '@editor-ui/app/core/providers/user-settings/user-settings.service';
import {
    DetailChip,
    FavouriteToggleComponent,
    IconCheckbox,
    ImageThumbnailComponent,
    ItemBreadcrumbsComponent,
    ItemListRowComponent,
    ItemStatusLabelComponent,
    LanguageContextSelectorComponent,
    ListItemDetails,
    MasonryGridComponent,
    PageLanguageIndicatorComponent,
    PagingControls,
    StartPageIcon,
} from '@editor-ui/app/shared/components';
import { MasonryItemDirective } from '@editor-ui/app/shared/directives/masonry-item/masonry-item.directive';
import {
    AllItemsSelectedPipe,
    FileSizePipe,
    GetInheritancePipe,
    HighlightPipe,
    ImageDimensionsPipe,
    IsFavouritePipe,
    IsStartPagePipe,
    ItemIsLocalizedPipe,
    ItemPathPipe,
    PageIsLockedPipe,
    RouterCommandsForItemPipe,
    TruncatePathPipe,
    UserFullNamePipe,
} from '@editor-ui/app/shared/pipes';
import {
    BreadcrumbsService,
    QueryAssemblerElasticSearchService,
    QueryAssemblerGCMSSearchService,
} from '@editor-ui/app/shared/providers';
import {
    ApplicationStateService,
    ContentStagingActionsService,
    FolderActionsService,
    PublishQueueActionsService,
    STATE_MODULES,
    UsageActionsService,
    WastebinActionsService,
} from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { componentTest, configureComponentTest } from '@editor-ui/testing';
import { mockPipes } from '@editor-ui/testing/mock-pipe';
import {  TypePermissions, UniformTypePermissions, WindowRef } from '@gentics/cms-components';
import { AccessControlledType, ResponseCode } from '@gentics/cms-models';
import {
    getExampleFolderData,
    getExampleFolderDataNormalized,
    getExampleNodeDataNormalized,
    getExamplePageDataNormalized,
} from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GenticsUICoreModule, ModalService, SplitViewContainerComponent } from '@gentics/ui-core';
import { NgxsModule } from '@ngxs/store';
import { NgxPaginationModule } from 'ngx-pagination';
import { Observable, Subject, of, throwError } from 'rxjs';
import { AnyItemDeletedPipe } from '../../pipes/any-item-deleted/any-item-deleted.pipe';
import { AnyItemInheritedPipe } from '../../pipes/any-item-inherited/any-item-inherited.pipe';
import { AnyItemPublishedPipe } from '../../pipes/any-item-published/any-item-published.pipe';
import { AnyPageUnpublishedPipe } from '../../pipes/any-page-unpublished/any-page-unpublished.pipe';
import { FilterItemsPipe } from '../../pipes/filter-items/filter-items.pipe';
import { ListService } from '../../providers/list/list.service';
import { FolderContentsComponent } from '../folder-contents/folder-contents.component';
import { GridItemComponent } from '../grid-item/grid-item.component';
import { ItemListHeaderComponent } from '../item-list-header/item-list-header.component';
import { ItemListComponent } from '../item-list/item-list.component';

const PERMISSIONS = {
    assignPermissions: true,
    file: {
        create: true,
        delete: true,
        edit: true,
        import: true,
        inherit: true,
        localize: true,
        unlocalize: true,
        upload: true,
        view: true,
    },
    folder: {
        create: true,
        delete: true,
        edit: true,
        inherit: true,
        localize: true,
        unlocalize: true,
        view: true,
    },
    form: {
        create: true,
        delete: true,
        edit: true,
        publish: true,
        view: true,
        inherit: true,
        localize: true,
        unlocalize: true,
    },
    image: {
        create: true,
        delete: true,
        edit: true,
        import: true,
        inherit: true,
        localize: true,
        unlocalize: true,
        upload: true,
        view: true,
    },
    page: {
        create: true,
        delete: true,
        edit: true,
        import: true,
        inherit: true,
        linkTemplate: true,
        localize: true,
        publish: true,
        translate: true,
        unlocalize: true,
        view: true,
    },
    synchronizeChannel: true,
    tagType: {
        create: true,
        delete: true,
        edit: true,
        view: true,
    },
    template: {
        create: true,
        delete: true,
        edit: true,
        inherit: true,
        link: true,
        localize: true,
        unlocalize: true,
        view: true,
    },
    wastebin: true,
};

@Component({
    template: `
    <folder-contents #componentToBeTested>
    </folder-contents>
    `,
})
class TestComponent {
    @ViewChild('componentToBeTested', { static: true })
    componentToBeTested: FolderContentsComponent;
}

class MockNavigationService {
    instruction(): any {
        return {
            commands(): void {},
        };
    }
}

class MockUsageActionsService {
    getTotalUsage(): void {}
}

class MockErrorHandler {
    catch(): void {}
}

class MockChangeDetector {
    markForCheck(): void { }
    detectChanges(): void { }
}

class MockDecisionModalsService { }

class MockFavouritesService {
    add = jasmine.createSpy('FavouritesService.add');
    remove = jasmine.createSpy('FavouritesService.remove');
}

class MockI18nService {}

class MockI18nNotification {}

@Pipe({ name: 'permissions' })
class MockPermissionPipe implements PipeTransform {
    transform(item: any): EditorPermissions {
        return {
            ...getNoPermissions(),
            // eslint-disable-next-line @typescript-eslint/naming-convention
            __forItem: item,
        } as any;
    }
}

class MockResourceUrlBuilder { }

class MockUploadConflictService { }

class MockUserSettingsService {
    setLastNodeId() {
        return;
    }
}

class MockContextMenuOperationsService {
    copyItems(): void { }
}

class MockPermissionService {
    forFolder(): Observable<EditorPermissions> {
        return of(PERMISSIONS);
    }
    all$: Observable<EditorPermissions> = of(PERMISSIONS);
    getTypePermissions(type: AccessControlledType): Observable<TypePermissions> {
        return of(new UniformTypePermissions(type, false));
    }
}

@Component({
    selector: 'item-context-menu',
    template: '',
})
class MockItemContextMenu {
    @Input() isFolderStartPage = false;
    @Input() permissions: EditorPermissions = getNoPermissions();
}

class MockListSearchService {
    searchEvent$ = new EventEmitter<{ term: string, nodeId?: number }>(null);
}

class MockWastebinActionsService {
    restoreItemsFromWastebin = jasmine.createSpy('restoreItemsFromWastebin');
}

class MockContentStatingActions {

}

class MockSplitViewContainer {
    rightPanelOpened = new Subject<void>();
    rightPanelClosed = new Subject<void>();
    splitDragEnd = new Subject<number>();
    rightPanelVisible = true;
    split = 50;
    scrollLeftPanelTo(scrollTop: number): void {
        return;
    }
}

class MockClient {
    page = {
        get: () => throwError('not mocked'),
    };

    file = {
        get: () => throwError('not mocked'),
    };

    image = {
        get: () => throwError('not mocked'),
    };

    form = {
        get: () => throwError('not mocked'),
    };

    folder = {
        get: () => throwError('not mocked'),
        folders: jasmine.createSpy('folder.folders').and.returnValue(of({
            hasMoreItems: true,
            messages: [],
            numItems: 26,
            responseInfo: {
                responseCode: ResponseCode.OK,
                responseMessage: 'Successfully loaded subfolders',
            },
            folders: [
                { ...getExampleFolderData({ id: 1 }) },
                { ...getExampleFolderData({ id: 2 }) },
                { ...getExampleFolderData({ id: 3 }) },
                { ...getExampleFolderData({ id: 4 }) },
                { ...getExampleFolderData({ id: 5 }) },
                { ...getExampleFolderData({ id: 6 }) },
                { ...getExampleFolderData({ id: 7 }) },
                { ...getExampleFolderData({ id: 8 }) },
                { ...getExampleFolderData({ id: 9 }) },
                { ...getExampleFolderData({ id: 10 }) },
                { ...getExampleFolderData({ id: 11 }) },
                { ...getExampleFolderData({ id: 12 }) },
                { ...getExampleFolderData({ id: 13 }) },
                { ...getExampleFolderData({ id: 14 }) },
                { ...getExampleFolderData({ id: 15 }) },
                { ...getExampleFolderData({ id: 16 }) },
                { ...getExampleFolderData({ id: 17 }) },
                { ...getExampleFolderData({ id: 18 }) },
                { ...getExampleFolderData({ id: 19 }) },
                { ...getExampleFolderData({ id: 20 }) },
                { ...getExampleFolderData({ id: 21 }) },
                { ...getExampleFolderData({ id: 22 }) },
                { ...getExampleFolderData({ id: 23 }) },
                { ...getExampleFolderData({ id: 24 }) },
                { ...getExampleFolderData({ id: 25 }) },
                { ...getExampleFolderData({ id: 26 }) },
            ],
        })),
        templates: () => of({
            templates: [],
            hasMoreItems: false,
            messages: [],
            numItems: 0,
            responseInfo: {
                responseCode: ResponseCode.OK,
                responseMessage: 'Successfully loaded templates',
            },
        }),
        breadcrumbs: () => of({
            folders: [],
            hasMoreItems: false,
            messages: [],
            numItems: 0,
            responseInfo: {
                responseCode: ResponseCode.OK,
                responseMessage: 'Successfully loaded breadcrumb',
            },
        }),
    };
}

describe('FolderContentsComponent', () => {

    let state: TestApplicationState;
    let folderActions: FolderActionsService;

    /** Updates the folder.folders portion of the AppState with the specified changes. */
    const updateItemsInfoState = (changes: Partial<ItemsInfo>) => {
        state.mockState({
            ...state.now,
            folder: {
                ...state.now.folder,
                folders: {
                    ...state.now.folder.folders,
                    ...changes,
                },
            },
        });
    };

    beforeEach(() => {
        /** *******************************************************************************************************  CONFIG MODULE */
        configureComponentTest({
            imports: [
                NgxsModule.forRoot(STATE_MODULES),
                GenticsUICoreModule.forRoot(),
                NgxPaginationModule,
                NoopAnimationsModule,
                RouterTestingModule,
            ],
            providers: [
                {
                    provide: ActivatedRoute,
                    useValue: {
                        params: of({
                            nodeId: 1,
                            folderId: 1,
                            searchTerm: '',
                            searchFilters: {},
                        }),
                        snapshot: {},
                    },
                },
                { provide: GCMSRestClientService, useClass: MockClient },
                {
                    provide: ApplicationStateService,
                    useClass: TestApplicationState,
                },
                { provide: ChangeDetectorRef, useClass: MockChangeDetector },
                {
                    provide: ContextMenuOperationsService,
                    useClass: MockContextMenuOperationsService,
                },
                {
                    provide: DecisionModalsService,
                    useClass: MockDecisionModalsService,
                },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: FavouritesService, useClass: MockFavouritesService },
                { provide: I18nNotification, useClass: MockI18nNotification },
                { provide: I18nService, useClass: MockI18nService },
                { provide: ListSearchService, useClass: MockListSearchService },
                { provide: ModalService, useClass: MockNavigationService },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: PermissionService, useClass: MockPermissionService },
                {
                    provide: ResourceUrlBuilder,
                    useClass: MockResourceUrlBuilder,
                },
                {
                    provide: SplitViewContainerComponent,
                    useClass: MockSplitViewContainer,
                },
                {
                    provide: UploadConflictService,
                    useClass: MockUploadConflictService,
                },
                { provide: UsageActionsService, useClass: MockUsageActionsService },
                { provide: UsageActionsService, useClass: MockUsageActionsService },
                {
                    provide: UserSettingsService,
                    useClass: MockUserSettingsService,
                },
                { provide: WastebinActionsService, useClass: MockWastebinActionsService },
                BreadcrumbsService,
                EntityResolver,
                FolderActionsService,
                FolderActionsService,
                ListService,
                PublishQueueActionsService,
                QueryAssemblerGCMSSearchService,
                QueryAssemblerElasticSearchService,
                WindowRef,
                { provide: ContentStagingActionsService, useClass: MockContentStatingActions },
            ],
            declarations: [
                AllItemsSelectedPipe,
                AnyItemDeletedPipe,
                AnyItemInheritedPipe,
                AnyItemPublishedPipe,
                AnyPageUnpublishedPipe,
                DetailChip,
                FavouriteToggleComponent,
                FileSizePipe,
                FilterItemsPipe,
                FolderContentsComponent,
                GetInheritancePipe,
                GridItemComponent,
                HighlightPipe,
                IconCheckbox,
                ImageDimensionsPipe,
                ImageThumbnailComponent,
                IsFavouritePipe,
                IsStartPagePipe,
                ItemBreadcrumbsComponent,
                ItemIsLocalizedPipe,
                ItemListComponent,
                ItemListHeaderComponent,
                ItemListRowComponent,
                ItemPathPipe,
                ItemStatusLabelComponent,
                LanguageContextSelectorComponent,
                ListItemDetails,
                MasonryGridComponent,
                MasonryItemDirective,
                MockItemContextMenu,
                MockPermissionPipe,
                PageIsLockedPipe,
                PageLanguageIndicatorComponent,
                PagingControls,
                RouterCommandsForItemPipe,
                StartPageIcon,
                TestComponent,
                TruncatePathPipe,
                UserFullNamePipe,
                mockPipes('i18n', 'i18nDate'),
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });

        state = TestBed.inject(ApplicationStateService) as TestApplicationState;
        folderActions = TestBed.inject(FolderActionsService);

        expect(state instanceof ApplicationStateService).toBeTruthy();
        state.mockState({
            auth: {
                currentUserId: 1,
                sid: 1,
                loggingIn: false,
                isLoggedIn: true,
            },
            editor: {
                editorIsOpen: false,
            },
            entities: {
                node: {
                    1: { ...getExampleNodeDataNormalized({ id: 1 }) },
                },
                language: {
                    1: { id: 1, code: 'en', name: 'English' },
                    2: { id: 2, code: 'de', name: 'Deutsch (German)' },
                },
                folder: {
                    1: { ...getExampleFolderDataNormalized({ id: 1 }) },
                    2: { ...getExampleFolderDataNormalized({ id: 2 }) },
                    3: { ...getExampleFolderDataNormalized({ id: 3 }) },
                    4: { ...getExampleFolderDataNormalized({ id: 4 }) },
                    5: { ...getExampleFolderDataNormalized({ id: 5 }) },
                    6: { ...getExampleFolderDataNormalized({ id: 6 }) },
                    7: { ...getExampleFolderDataNormalized({ id: 7 }) },
                    8: { ...getExampleFolderDataNormalized({ id: 8 }) },
                    9: { ...getExampleFolderDataNormalized({ id: 9 }) },
                    10: { ...getExampleFolderDataNormalized({ id: 10 }) },
                },
                page: {
                    1: { ...getExamplePageDataNormalized({ id: 1 }), ...{ languageVariants: [ 1, 2 ], deleted: { at: 0, by: null } } },
                    2: { ...getExamplePageDataNormalized({ id: 2 }), ...{ language: 'de', languageVariants: [ 1, 2 ], deleted: { at: 0, by: null } } },
                    3: { ...getExamplePageDataNormalized({ id: 3 }) },
                    4: { ...getExamplePageDataNormalized({ id: 4 }) },
                },
                file: {},
                image: {},
                form: {},
            },
            favourites: {
                list: [],
            },
            folder: {
                activeFolder: 1,
                activeNode: 1,
                activeNodeLanguages: {
                    list: [1, 2],
                },
                breadcrumbs: {
                    creating: false,
                    deleting: [],
                    fetchAll: false,
                    fetching: false,
                    hasMore: false,
                    list: [1],
                    selected: [],
                    saving: false,
                    showPath: true,
                    sortBy: 'name',
                    sortOrder: 'asc',
                    total: 1,
                    currentPage: 1,
                    itemsPerPage: 10,
                },
                nodes: {
                    creating: false,
                    currentPage: 1,
                    deleting: [],
                    fetchAll: false,
                    fetching: false,
                    hasMore: false,
                    itemsPerPage: 10,
                    list: [1],
                    saving: false,
                    selected: [],
                    total: 1,
                },
                folders: {
                    list: Array.from({ length: 10 }, (_, i) => i + 1),
                    selected: [],
                    total: 26,
                    hasMore: false,
                    fetchAll: false,
                    creating: false,
                    fetching: false,
                    saving: false,
                    deleting: [],
                    currentPage: 1,
                    itemsPerPage: 10,
                },
                pages: {
                    list: [1, 2, 3],
                    selected: [],
                    total: 3,
                    hasMore: false,
                    fetchAll: false,
                    creating: false,
                    fetching: false,
                    saving: false,
                    deleting: [],
                    currentPage: 1,
                    itemsPerPage: 10,
                },
                files: {
                    list: [],
                    selected: [],
                    total: 0,
                    hasMore: false,
                    fetchAll: false,
                    creating: false,
                    fetching: false,
                    saving: false,
                    deleting: [],
                    currentPage: 1,
                    itemsPerPage: 10,
                },
                images: {
                    list: [],
                    selected: [],
                    total: 0,
                    hasMore: false,
                    fetchAll: false,
                    creating: false,
                    fetching: false,
                    saving: false,
                    deleting: [],
                    currentPage: 1,
                    itemsPerPage: 10,
                },
                forms: {
                    list: [],
                    selected: [],
                    total: 0,
                    hasMore: false,
                    fetchAll: false,
                    creating: false,
                    fetching: false,
                    saving: false,
                    deleting: [],
                    currentPage: 1,
                    itemsPerPage: 10,
                },
                searchTerm: '',
                searchFilters: {
                    nodeId: [
                        {
                            value: 1,
                            operator: 'IS',
                        },
                    ],
                    all: null,
                    languageCode: [
                        {
                            value: 2,
                            operator: 'IS',
                        },
                    ],
                    id: null,
                    name: null,
                    filename: null,
                    description: null,
                    content: null,
                    created: null,
                    edited: null,
                    published: null,
                    creatorId: null,
                    editorId: null,
                    publisherId: null,
                    templateId: null,
                    online: null,
                    modified: null,
                    queued: null,
                    planned: null,
                    publishAt: null,
                    offlineAt: null,
                    queuedPublishAt: null,
                    queuedOfflineAt: null,
                    systemCreationDate: null,
                    systemEditDate: null,
                    niceUrl: null,
                },
                searchFiltersValid: false,
            },
        });
    });

    /** *******************************************************************************************************  TESTS */

    it('has initialized correctly',
        componentTest(() => TestComponent, (fixture, instance) => {
            const activatedroute: ActivatedRoute = TestBed.inject(ActivatedRoute);

            const listService: ListService = TestBed.inject(ListService);
            spyOn(listService, 'init');

            const folderContentsComponent: FolderContentsComponent = fixture.debugElement.query(By.directive(FolderContentsComponent)).componentInstance;
            spyOn(folderContentsComponent, 'initFolderContents');

            fixture.detectChanges();
            tick();

            expect(folderContentsComponent.initFolderContents).toHaveBeenCalledTimes(1);
            expect(listService.init).toHaveBeenCalledWith(activatedroute);
            expect(listService.init).toHaveBeenCalledTimes(1);
        }),
    );

    it('loads paginated items of the type when paging link is clicked',
        componentTest(() => TestComponent, (fixture, instance) => {
            spyOn(folderActions, 'getItems');
            // spyOn(apiService.folders, 'getFolders');

            updateItemsInfoState({
                hasMore: true,
            });
            fixture.detectChanges();
            tick();

            expect(state.now.folder.folders.currentPage).toBe(1);
            expect(state.now.folder.folders.itemsPerPage).toBe(10);
            expect(state.now.folder.folders.list.length).toBe(10);
            expect(state.now.folder.folders.total).toBe(26);

            const folderContentsComponent: FolderContentsComponent = fixture.debugElement.query(By.directive(FolderContentsComponent)).componentInstance;
            folderContentsComponent.pageChange( 'folder', 2 );

            fixture.detectChanges();
            tick();

            expect(state.now.folder.folders.currentPage).toBe(2);
            expect(state.now.folder.folders.itemsPerPage).toBe(10);
            expect(state.now.folder.folders.list.length).toBe(10);
            expect(state.now.folder.folders.total).toBe(26);

            expect(folderActions.getItems).toHaveBeenCalledWith(1, 'folder', false, { maxItems: 10, search: '', recursive: false, skipCount: 10 } );
            expect(folderActions.getItems).toHaveBeenCalledTimes(1);

            discardPeriodicTasks();
        }),
    );

    it('loads paginated items of the type when paging size is changed',
        componentTest(() => TestComponent, (fixture, instance) => {
            spyOn(folderActions, 'getItems');

            fixture.detectChanges();
            tick();

            // expect currentPage to be reset to 1 after itemsPerPage changed
            expect(state.now.folder.folders.itemsPerPage).toBe(10);
            expect(state.now.folder.folders.list.length).toBe(10);
            expect(state.now.folder.folders.total).toBe(26);

            const folderContentsComponent: FolderContentsComponent = fixture.debugElement.query(By.directive(FolderContentsComponent)).componentInstance;
            folderContentsComponent.itemsPerPageChange( 'folder', 25 );

            fixture.detectChanges();
            tick();

            expect(state.now.folder.folders.itemsPerPage).toBe(25);

            expect(folderActions.getItems).toHaveBeenCalledWith(1, 'folder', false, { maxItems: 25, search: '', recursive: false, skipCount: 0 } );
            expect(folderActions.getItems).toHaveBeenCalledTimes(1);

            discardPeriodicTasks();
        }),
    );

});
