import {
    ChangeDetectorRef,
    Component,
    EventEmitter,
    Input,
    NO_ERRORS_SCHEMA,
    OnInit,
    Pipe,
    PipeTransform,
    SimpleChange,
    ViewChild,
} from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ItemsInfo } from '@editor-ui/app/common/models';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { WindowRef } from '@gentics/cms-components';
import {
    EditorPermissions,
    File,
    Folder,
    FolderItemType,
    Image,
    Node as NodeModel,
    Page,
    getNoPermissions,
} from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { NgxPaginationModule } from 'ngx-pagination';
import { Observable } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { getExamplePageData, getExamplePageDataNormalized } from '@gentics/cms-models/testing/test-data.mock';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { DecisionModalsService } from '../../../core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { FavouritesService } from '../../../core/providers/favourites/favourites.service';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { ListSearchService } from '../../../core/providers/list-search/list-search.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ResourceUrlBuilder } from '../../../core/providers/resource-url-builder/resource-url-builder';
import { UploadConflictService } from '../../../core/providers/upload-conflict/upload-conflict.service';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';
import { MasonryGridComponent } from '../../../shared/components';
import { DetailChip } from '../../../shared/components/detail-chip/detail-chip.component';
import { FavouriteToggle } from '../../../shared/components/favourite-toggle/favourite-toggle.component';
import { IconCheckbox } from '../../../shared/components/icon-checkbox/icon-checkbox.component';
import { ImageThumbnailComponent } from '../../../shared/components/image-thumbnail/image-thumbnail.component';
import { ItemBreadcrumbsComponent } from '../../../shared/components/item-breadcrumbs/item-breadcrumbs.component';
import { ItemListRowComponent } from '../../../shared/components/item-list-row/item-list-row.component';
import { ItemStatusLabelComponent } from '../../../shared/components/item-status-label/item-status-label.component';
import { LanguageContextSelectorComponent } from '../../../shared/components/language-context-selector/language-context-selector.component';
import { ListItemDetails } from '../../../shared/components/list-item-details/list-item-details.component';
import { PageLanguageIndicatorComponent } from '../../../shared/components/page-language-indicator/page-language-indicator.component';
import { PagingControls } from '../../../shared/components/paging-controls/paging-controls.component';
import { StartPageIcon } from '../../../shared/components/start-page-icon/start-page-icon.component';
import { MasonryItemDirective } from '../../../shared/directives';
import { AllItemsSelectedPipe } from '../../../shared/pipes/all-items-selected/all-items-selected.pipe';
import { FileSizePipe } from '../../../shared/pipes/file-size/file-size.pipe';
import { GetInheritancePipe } from '../../../shared/pipes/get-inheritance/get-inheritance.pipe';
import { HighlightPipe } from '../../../shared/pipes/highlight/highlight.pipe';
import { I18nDatePipe } from '../../../shared/pipes/i18n-date/i18n-date.pipe';
import { ImageDimensionsPipe } from '../../../shared/pipes/image-dimensions/image-dimensions.pipe';
import { IsFavouritePipe } from '../../../shared/pipes/is-favourite/is-favourite.pipe';
import { IsStartPagePipe } from '../../../shared/pipes/is-start-page/is-start-page.pipe';
import { ItemIsLocalizedPipe } from '../../../shared/pipes/item-is-localized/item-is-localized.pipe';
import { ItemPathPipe } from '../../../shared/pipes/item-path/item-path.pipe';
import { PageIsLockedPipe } from '../../../shared/pipes/page-is-locked/page-is-locked.pipe';
import { RouterCommandsForItemPipe } from '../../../shared/pipes/router-commands-for-item/router-commands-for-item.pipe';
import { TruncatePathPipe } from '../../../shared/pipes/truncate-path/truncate-path.pipe';
import { UserFullNamePipe } from '../../../shared/pipes/user-full-name/user-full-name.pipe';
import { ApplicationStateService, FolderActionsService, UsageActionsService, WastebinActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { AnyItemDeletedPipe } from '../../pipes/any-item-deleted/any-item-deleted.pipe';
import { AnyItemInheritedPipe } from '../../pipes/any-item-inherited/any-item-inherited.pipe';
import { AnyItemPublishedPipe } from '../../pipes/any-item-published/any-item-published.pipe';
import { AnyPageUnpublishedPipe } from '../../pipes/any-page-unpublished/any-page-unpublished.pipe';
import { FilterItemsPipe } from '../../pipes/filter-items/filter-items.pipe';
import { ListService } from '../../providers/list/list.service';
import { GridItemComponent } from '../grid-item/grid-item.component';
import { ItemListHeaderComponent } from '../item-list-header/item-list-header.component';
import { ItemListComponent } from './item-list.component';

/**
 * Returns an array of items in the contents list.
 */
const getListItems = (fixture: ComponentFixture<TestComponent>): HTMLElement[] => fixture.debugElement
    .queryAll(By.css('item-list-row'))
    .map(itemDebugElement => itemDebugElement.nativeElement);

/**
 * Returns the name of the item.
 */
// eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
const getItemName = (listItem: Element): string => (listItem.querySelector('.item-name') as any).innerText;

const allPermissions = (): EditorPermissions => // Sorry, but it works.
    JSON.parse(JSON.stringify(getNoPermissions()).replace(/false/g, 'true'));


@Component({
    template: `
        <item-list #itemList
            [class]="itemType"
            [itemType]="itemType"
            [filterTerm]="filterTerm"
            [items]="items | filterItems:filterTerm"
            [itemsInfo]="itemsInfo$ | async"
            [currentFolderId]="currentFolderId$ | async"
            [activeNode]="activeNode"
            [startPageId]="startPageId"
            [itemInEditor]="itemInEditor"
            [folderPermissions]="permissions"
            [linkPaths]="isSearching"
            ></item-list>`
    })
class TestComponent implements OnInit {
    @ViewChild('itemList', { static: true })
    itemList: ItemListComponent;

    itemType = 'folder';
    items: Array<Partial<Page> | Partial<Folder> | Partial<Image> | Partial<File>> = [
        { id: 1, name: 'item1', path: 'root/item1', type: 'folder' },
        { id: 2, name: 'item2', path: 'root/item2', type: 'folder' },
        { id: 3, name: 'item3', path: 'root/item3', type: 'folder' },
    ];
    activeNode: any = {
        name: '',
        id: 1,
    };
    filterTerm = '';
    itemsInfo$: Observable<ItemsInfo>;
    currentFolderId$: Observable<number>;
    selectedItems: number[] = [];
    startPageId: number = Number.NaN;
    itemInEditor: any = undefined;
    permissions = allPermissions();
    isSearching = true;
    itemsInfoPipe$: Observable<boolean>;

    constructor(public appState: ApplicationStateService) { }

    ngOnInit(): void {
        this.itemsInfo$ = this.appState.select(state => state.folder.folders);
        this.currentFolderId$ = this.appState.select(state => state.folder.activeFolder);
    }
}

class MockRouter {
    createUrlTree(): void { }
    navigateByUrl(): void { }
}
class MockActivatedRoute { }
class MockLocationStrategy { }

class MockNavigationService {
    instruction(): any {
        return {
            commands(): void { },
        };
    }
}

class MockUsageActionsService {
    getTotalUsage(): void { }
}

class MockFolderActions {
    getFolders(): void { }
    getPages(): void { }
    getFiles(): void { }
    getImages(): void { }
    getTemplates(): void { }
    getItemsOfTypeInFolder(): void { }
    setDisplayAllPageLanguages(): void { }
    setDisplayStatusIcons(): void { }
    setCurrentPage = jasmine.createSpy('setCurrentPage');
}

class MockErrorHandler {
    catch(): void { }
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

class MockI18nService { }

class MockI18nNotification { }

@Pipe({ name: 'permissions' })
class MockPermissionPipe implements PipeTransform {
    transform(item: any): EditorPermissions {
        return {
            ...getNoPermissions(),
            __forItem: item,
        } as any;
    }
}

class MockResourceUrlBuilder { }

class MockUploadConflictService { }

class MockUserSettingsService { }

class MockContextMenuOperationsService {
    copyItems(): void { }
}

@Component({
    selector: 'item-context-menu',
    template: ''
    })
class MockItemContextMenu {
    @Input() isFolderStartPage = false;
    @Input() permissions: EditorPermissions = getNoPermissions();
}

class MockWindowRef { }

class MockListSearchService {
    searchEvent$ = new EventEmitter<{ term: string, nodeId?: number }>(null);
}

class MockWastebinActionsService {
    restoreItemsFromWastebin = jasmine.createSpy('restoreItemsFromWastebin');
}

describe('ItemListComponent', () => {

    let state: TestApplicationState;
    let folderActions: MockFolderActions;

    /** Updates the folder.folders portion of the AppState with the specified changes. */
    let updateItemsInfoState = (changes: Partial<ItemsInfo>) => {
        state.mockState({
            folder: {
                folders: {
                    ...changes,
                },
            },
        });
    };


    beforeEach(() => {
        configureComponentTest({
            imports: [NoopAnimationsModule, GenticsUICoreModule.forRoot(), NgxPaginationModule],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: DecisionModalsService, useClass: MockDecisionModalsService },
                EntityResolver,
                { provide: ContextMenuOperationsService, useClass: MockContextMenuOperationsService },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: UsageActionsService, useClass: MockUsageActionsService },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: ListSearchService, useClass: MockListSearchService },
                { provide: ChangeDetectorRef, useClass: MockChangeDetector },
                { provide: I18nService, useClass: MockI18nService },
                { provide: I18nNotification, useClass: MockI18nNotification },
                { provide: FavouritesService, useClass: MockFavouritesService },
                { provide: ResourceUrlBuilder, useClass: MockResourceUrlBuilder },
                { provide: UsageActionsService, useClass: MockUsageActionsService },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: UploadConflictService, useClass: MockUploadConflictService },
                { provide: UserSettingsService, useClass: MockUserSettingsService },
                { provide: WastebinActionsService, useClass: MockWastebinActionsService },
                { provide: WindowRef, useClass: MockWindowRef },
                ListService,
            ],
            declarations: [
                AllItemsSelectedPipe,
                AnyItemDeletedPipe,
                AnyItemInheritedPipe,
                AnyItemPublishedPipe,
                AnyPageUnpublishedPipe,
                DetailChip,
                FavouriteToggle,
                FileSizePipe,
                FilterItemsPipe,
                GetInheritancePipe,
                GridItemComponent,
                HighlightPipe,
                I18nDatePipe,
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
                LanguageContextSelectorComponent,
                ListItemDetails,
                MasonryGridComponent,
                MasonryItemDirective,
                MockItemContextMenu,
                MockPermissionPipe,
                PageIsLockedPipe,
                PageLanguageIndicatorComponent,
                ItemStatusLabelComponent,
                PagingControls,
                RouterCommandsForItemPipe,
                StartPageIcon,
                TestComponent,
                TruncatePathPipe,
                UserFullNamePipe,
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });

        state = TestBed.get(ApplicationStateService);
        folderActions = TestBed.get(FolderActionsService);
        expect(state instanceof ApplicationStateService).toBeTruthy();
        state.mockState({
            auth: {
                currentUserId: 1,
                sid: 1,
            },
            editor: {
                editorIsOpen: false,
                saving: false,
            },
            entities: {
                node: {
                    1: {},
                },
                language: {
                    1: { id: 1, code: 'en', name: 'English' },
                    2: { id: 2, code: 'de', name: 'Deutsch (German)' },
                },
                page: {
                    1: { ...getExamplePageDataNormalized({ id: 1 }), ...{ languageVariants: [1, 2], deleted: { at: 0, by: null } } },
                    2: { ...getExamplePageDataNormalized({ id: 2 }), ...{ language: 'de', languageVariants: [1, 2], deleted: { at: 0, by: null } } },
                },
            },
            favourites: {
                list: [],
            },
            folder: {
                activeFolder: 5,
                activeNode: 1,
                activeNodeLanguages: {
                    list: [1, 2],
                },
                folders: {
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
                    showPath: true,
                    sortBy: 'name',
                    sortOrder: 'asc',
                    displayFields: [],
                    displayFieldsRepositoryBrowser: {
                        selection: [],
                        showPath: true,
                    },
                },
                pages: {
                    list: [4, 5, 6],
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
                    showPath: true,
                    sortBy: 'name',
                    sortOrder: 'asc',
                    displayFields: [],
                    displayFieldsRepositoryBrowser: {
                        selection: [],
                        showPath: true,
                    },
                },
                searchTerm: '',
            },
        });
    });

    it('displays the correct number of items',
        componentTest(() => TestComponent, fixture => {
            fixture.detectChanges();
            tick();
            let listItems: HTMLElement[] = getListItems(fixture);
            expect(listItems.length).toBe(3);
        }),
    );

    it('calls getTotalUsage correct times with the correct parameters',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.itemType = 'page';
            instance.itemsInfo$ = state.select(state => state.folder.pages);
            instance.items = [
                { id: 1, name: 'item1', path: 'root/item1', type: 'page' },
                { id: 2, name: 'item2', path: 'root/item2', type: 'page' },
                { id: 3, name: 'item3', path: 'root/item3', type: 'page' },
            ];
            instance.itemList.itemsInfo = { list: [1, 2] } as ItemsInfo;
            instance.itemList.activeNode = { id: 11 } as NodeModel;
            instance.itemList.itemType = 'page' as FolderItemType;
            const usageActions = TestBed.inject(UsageActionsService);
            spyOn(instance.itemList, 'updateItemHash').and.callFake(function () { });
            spyOn(instance.itemList, 'getTotalUsage').and.callThrough();
            spyOn(usageActions, 'getTotalUsage').and.callThrough();
            const previousValue = [
                { id: 1, name: 'item1', path: 'root/item1', type: 'page' },
                { id: 2, name: 'item2', path: 'root/item2', type: 'page' },
                { id: 3, name: 'item3', path: 'root/item3', type: 'page' },
            ];
            const currentValue = [
                { id: 1, name: 'item1', path: 'root/item1', type: 'page' },
                { id: 2, name: 'item2', path: 'root/item2', type: 'page' },
                { id: 3, name: 'item3', path: 'root/item333', type: 'page' },
            ];

            const itemsInfo = {
                itemsInfo: {
                    previousValue: {
                        fetchAll: false,
                        list: [],
                    },
                    currentValue: {
                        fetchAll: false,
                        list: [
                            51,
                        ],
                    },
                    firstChange: false,
                },
            }
            const change = {
                items: new SimpleChange(previousValue, currentValue, false),
                itemsInfo: new SimpleChange(itemsInfo.itemsInfo.previousValue, itemsInfo.itemsInfo.currentValue, false),
            };
            instance.itemList.ngOnChanges(change);

            expect(instance.itemList.getTotalUsage).toHaveBeenCalledTimes(1);
            expect(usageActions.getTotalUsage).toHaveBeenCalledTimes(1);
            expect(usageActions.getTotalUsage).toHaveBeenCalledWith(instance.itemList.itemType, [1, 2], 11);
        }),
    );


    it('checks if getTotalUsage gets called with the correct parameters when usage display field is active and the saving state is false',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.itemType = 'page';
            instance.activeNode = {
                name: '',
                id: 33,
            }
            const usageActions = TestBed.inject(UsageActionsService);
            state.mockState({
                folder: {
                    folders: {
                        list: [11,22],
                    },
                    pages: {
                        list: [11,22],
                        displayFields: ['usage'],
                    },
                },
            });

            spyOn(instance.itemList, 'getTotalUsage').and.callThrough();
            spyOn(usageActions, 'getTotalUsage').and.callThrough();

            state.mockState({
                editor: {
                    saving: true,
                },
            })
            tick(200);
            fixture.detectChanges();
            expect(instance.itemList.getTotalUsage).not.toHaveBeenCalled();
            expect(usageActions.getTotalUsage).not.toHaveBeenCalled();

            state.mockState({
                editor: {
                    saving: false,
                },
            })
            tick(200);
            fixture.detectChanges();
            expect(instance.itemList.getTotalUsage).toHaveBeenCalled();
            expect(usageActions.getTotalUsage).toHaveBeenCalledTimes(1);
            expect(usageActions.getTotalUsage).toHaveBeenCalledWith(instance.itemList.itemType, [11,22], 33);
        }),
    );

    it('checks if getTotalUsage gets called when usage display field is active',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.itemType = 'page';
            const usageActions = TestBed.inject(UsageActionsService);
            state.mockState({
                ...state.now,
                folder: {
                    ...state.now.folder,
                    pages: {
                        ...state.now.folder.pages,
                        displayFields: ['creator'],
                    },
                },
            });

            spyOn(instance.itemList, 'getTotalUsage').and.callThrough();
            spyOn(usageActions, 'getTotalUsage').and.callThrough();

            tick(200);
            fixture.detectChanges();
            expect(instance.itemList.getTotalUsage).not.toHaveBeenCalled();
            expect(usageActions.getTotalUsage).not.toHaveBeenCalled();

            state.mockState({
                ...state.now,
                folder: {
                    ...state.now.folder,
                    pages: {
                        ...state.now.folder.pages,
                        displayFields: ['usage'],
                    },
                },
            });
            tick(200);
            fixture.detectChanges();
            expect(instance.itemList.getTotalUsage).toHaveBeenCalled();
            expect(usageActions.getTotalUsage).toHaveBeenCalledTimes(1);
        }),
    );

    it('displays the live URL for images',
        componentTest(() => TestComponent, (fixture, instance) => {
            const testImage: Partial<Image> = { name: 'item1', path: 'root/item1', globalId: 'itemA', type: 'image' };
            instance.items = [testImage];
            fixture.detectChanges();
            tick();
            const getImageLiveURL = (el: Element) => (el.querySelector('image-thumbnail .liveurl-icon'));
            expect(getImageLiveURL).toBeTruthy();
        }),
    );

    it('displays the live URL for files',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.itemType = 'file';
            instance.items = [
                { type: 'file', name: 'file1', id: 1, path: 'root/file1' },
                { type: 'file', name: 'file2', id: 2, path: 'root/file2' },
                { type: 'file', name: 'file3', id: 3, path: 'root/file3' },
            ];
            fixture.detectChanges();
            tick();
            const getFileLiveURL = (el: Element) => (el.querySelector('.liveurl-icon'));
            expect(getFileLiveURL).toBeTruthy();
        }),
    );

    it('displays the correct path for pages if showPath is true',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.itemType = 'page';
            instance.items = [
                { type: 'page', name: 'page1', id: 1, path: 'root/page1', publishPath: '/root/page1' },
            ];
            updateItemsInfoState({
                showPath: true,
            });

            fixture.detectChanges();

            // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
            const getItemFilename = (el: Element) => (el.querySelector('.file-name') as any).innerText;
            expect(getItemFilename(fixture.nativeElement)).toContain('/root/page1');
        }),
    );

    it('does not display the path for pages if showPath is false',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.itemType = 'page';
            instance.items = [
                { type: 'page', name: 'page1', id: 1, path: 'root/page1' },
            ];
            updateItemsInfoState({
                showPath: false,
            });

            fixture.detectChanges();

            const getItemFilename = (el: Element) => (el.querySelector('.file-name'));
            expect(getItemFilename(fixture.nativeElement)).toBeNull();
        }),
    );

    describe('filtering:', () => {

        it('filters based on filterTerm',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.filterTerm = '2';
                fixture.detectChanges();
                tick();
                let listItems: HTMLElement[] = getListItems(fixture);
                expect(listItems.length).toBe(1);
                expect(getItemName(listItems[0])).toContain('item2');
            }),
        );

        it('updates items when filterTerm changes',
            componentTest(() => TestComponent, (fixture, instance) => {
                let listItems: HTMLElement[];

                instance.filterTerm = '2';
                fixture.detectChanges();
                tick();
                listItems = getListItems(fixture);
                expect(listItems.length).toBe(1);
                expect(getItemName(listItems[0])).toContain('item2');

                instance.filterTerm = '';
                fixture.detectChanges();
                listItems = getListItems(fixture);
                expect(listItems.length).toBe(3);
                expect(getItemName(listItems[0])).toContain('item1');
                expect(getItemName(listItems[1])).toContain('item2');
                expect(getItemName(listItems[2])).toContain('item3');
            }),
        );

        it('handles unexpected filterTerm values',
            componentTest(() => TestComponent, (fixture, instance) => {
                const detectChanges = () => fixture.detectChanges();
                instance.filterTerm = <any>2;
                expect(detectChanges).not.toThrow();

                instance.filterTerm = <any>null;
                expect(detectChanges).not.toThrow();

                instance.filterTerm = <any>undefined;
                expect(detectChanges).not.toThrow();
                tick();
            }),
        );

        it('copies only selected items that match filterTerm',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'page';
                instance.itemsInfo$ = state.select(state => state.folder.pages);
                instance.items = [
                    { id: 1, name: 'item1', path: 'root/item1', type: 'page' },
                    { id: 2, name: 'item2', path: 'root/item2', type: 'page' },
                    { id: 3, name: 'item3', path: 'root/item3', type: 'page' },
                ];
                const contextMenuService = TestBed.get(ContextMenuOperationsService);
                spyOn(contextMenuService, 'copyItems').and.stub();

                fixture.detectChanges();
                tick();

                const listItems: HTMLElement[] = getListItems(fixture);

                // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
                ((listItems[0].querySelector('input[type="checkbox"]')) as HTMLElement).click();
                // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
                ((listItems[1].querySelector('input[type="checkbox"]')) as HTMLElement).click();
                tick();
                fixture.detectChanges();
                expect(state.now.folder.pages.selected).toEqual([1, 2]);

                instance.filterTerm = '2';
                fixture.detectChanges();
                tick();
                const copyButton: HTMLElement = fixture.debugElement
                    .queryAll(By.css('item-list-header .group-actions gtx-button button'))
                    .map(itemDebugElement => itemDebugElement.nativeElement)[0];
                copyButton.click();
                fixture.detectChanges();
                tick();

                expect(contextMenuService.copyItems).toHaveBeenCalledWith('page', [instance.items[1]], state.now.folder.activeNode);
            }),
        );

    });

    it('checks the toggleAll checkbox when each row checked',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();
            let checkboxes: IconCheckbox[] = fixture.debugElement.queryAll(By.css('icon-checkbox'))
                .map(checkboxDebugElement => checkboxDebugElement.componentInstance);
            let listItems: HTMLElement[] = getListItems(fixture);

            expect(state.now.folder.folders.selected).toEqual([]);
            expect(checkboxes[0].selected).toBe(false);

            const clickCheckbox = (listItem: any) => listItem.querySelector('input[type="checkbox"]').click();

            for (let listItem of listItems) {
                clickCheckbox(listItem);
                tick();
                fixture.detectChanges();
            }

            expect(checkboxes[0].selected).toBe(true);
            expect(state.now.folder.folders.selected).toEqual([1, 2, 3]);
        }),
    );

    it('toggles all rows when toggleAll checkbox clicked',
        componentTest(() => TestComponent, fixture => {
            fixture.detectChanges();
            let checkboxes: IconCheckbox[] = fixture.debugElement.queryAll(By.css('icon-checkbox'))
                .map(checkboxDebugElement => checkboxDebugElement.componentInstance);
            let toggleAll: HTMLElement = fixture.nativeElement
                .querySelector('.list-header input[type="checkbox"]');

            expect(checkboxes[0].selected).toBe(false);
            expect(checkboxes[1].selected).toBe(false);
            expect(checkboxes[2].selected).toBe(false);
            expect(checkboxes[3].selected).toBe(false);

            toggleAll.click();
            tick();
            fixture.detectChanges();

            expect(checkboxes[0].selected).toBe(true);
            expect(checkboxes[1].selected).toBe(true);
            expect(checkboxes[2].selected).toBe(true);
            expect(checkboxes[3].selected).toBe(true);

            toggleAll.click();
            tick();
            fixture.detectChanges();
            tick();

            expect(checkboxes[0].selected).toBe(false);
            expect(checkboxes[1].selected).toBe(false);
            expect(checkboxes[2].selected).toBe(false);
            expect(checkboxes[3].selected).toBe(false);
        }),
    );

    it('unchecks toggleAll checkbox when a single row is unchecked',
        componentTest(() => TestComponent, fixture => {
            fixture.detectChanges();
            let listItems: HTMLElement[] = getListItems(fixture);
            let checkboxes: IconCheckbox[] = fixture.debugElement.queryAll(By.css('icon-checkbox'))
                .map(checkboxDebugElement => checkboxDebugElement.componentInstance);
            let toggleAll: HTMLElement = fixture.nativeElement
                .querySelector('.list-header input[type="checkbox"]');

            toggleAll.click();
            tick();
            fixture.detectChanges();

            expect(checkboxes[0].selected).toBe(true);
            expect(checkboxes[1].selected).toBe(true);
            expect(checkboxes[2].selected).toBe(true);
            expect(checkboxes[3].selected).toBe(true);

            toggleAll.click();
            tick();
            fixture.detectChanges();


            // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
            ((listItems[2].querySelector('input[type="checkbox"]')) as any).click();
            tick();
            fixture.detectChanges();

            expect(checkboxes[0].selected).toBe(false);
        }),
    );

    describe('selectionChange', () => {

        const clickCheckbox = (listItem: any) => listItem.querySelector('input[type="checkbox"]').click();

        it('emits when items are checked/unchecked',
            componentTest(() => TestComponent, (fixture, instance) => {
                fixture.detectChanges();
                const listItems = getListItems(fixture);

                clickCheckbox(listItems[0]);
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([1]);

                clickCheckbox(listItems[2]);
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([1, 3]);

                clickCheckbox(listItems[0]);
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([3]);
            }),
        );

        it('emits with all items when toggleAll is clicked',
            componentTest(() => TestComponent, (fixture, instance) => {
                fixture.detectChanges();
                const toggleAll: HTMLElement = fixture.nativeElement.querySelector('.list-header input[type="checkbox"]');

                toggleAll.click();
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([1, 2, 3]);

                toggleAll.click();
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([]);

                toggleAll.click();
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([1, 2, 3]);
            }),
        );

        it('emits with all items when toggleAll is clicked and a filterTerm is being applied',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.filterTerm = '2';
                fixture.detectChanges();
                const toggleAll: HTMLElement = fixture.nativeElement.querySelector('.list-header input[type="checkbox"]');

                toggleAll.click();
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([2]);

                toggleAll.click();
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([]);
            }),
        );

        it('loads all items of the type when hasMore = true, then emits full selection on state update',
            componentTest(() => TestComponent, (fixture, instance) => {
                const folderActions: FolderActionsService = TestBed.get(FolderActionsService);
                spyOn(folderActions, 'getItemsOfTypeInFolder');
                updateItemsInfoState({
                    hasMore: true,
                    list: [1, 2, 3],
                    total: 3,
                });
                fixture.detectChanges();
                const fullList = [1, 2, 3, 4, 5, 6, 7, 8];
                const toggleAll: HTMLElement = fixture.nativeElement.querySelector('.list-header input[type="checkbox"]');

                toggleAll.click();
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([]);

                expect(folderActions.getItemsOfTypeInFolder).toHaveBeenCalled();

                updateItemsInfoState({
                    hasMore: false,
                });
                tick();

                expect(state.now.folder.folders.selected).toEqual([]);

                updateItemsInfoState({
                    list: fullList,
                    total: fullList.length,
                });
                tick(100);
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual(fullList);
            }),
        );

        it('emits with all items when a single items is clicked and then toggleAll is clicked',
            componentTest(() => TestComponent, (fixture, instance) => {
                fixture.detectChanges();
                const toggleAll: HTMLElement = fixture.nativeElement.querySelector('.list-header input[type="checkbox"]');
                const listItems = getListItems(fixture);

                clickCheckbox(listItems[0]);
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([1]);

                toggleAll.click();
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([1, 2, 3]);

                toggleAll.click();
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([]);

                toggleAll.click();
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([1, 2, 3]);
            }),
        );

        it('emits with all items when a single items is clicked and then toggleAll is clicked (with hasMore = true)',
            componentTest(() => TestComponent, (fixture, instance) => {
                const folderActions: FolderActionsService = TestBed.get(FolderActionsService);
                spyOn(folderActions, 'getItemsOfTypeInFolder');
                updateItemsInfoState({
                    hasMore: true,
                    list: [1, 2, 3],
                    total: 3,
                });
                fixture.detectChanges();
                const fullList = [1, 2, 3, 4, 5, 6, 7, 8];
                const toggleAll: HTMLElement = fixture.nativeElement.querySelector('.list-header input[type="checkbox"]');
                const listItems = getListItems(fixture);

                clickCheckbox(listItems[0]);
                tick();
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual([1]);

                toggleAll.click();
                tick();
                fixture.detectChanges();

                expect(folderActions.getItemsOfTypeInFolder).toHaveBeenCalled();

                updateItemsInfoState({
                    hasMore: false,
                });
                tick();

                expect(state.now.folder.folders.selected).toEqual([1]);

                updateItemsInfoState({
                    list: fullList,
                    total: fullList.length,
                });
                tick(100);
                fixture.detectChanges();
                expect(state.now.folder.folders.selected).toEqual(fullList);
            }),
        );

    });

    ['page', 'image', 'file'].forEach(itemType => {

        it(`focuses the editor when a ${itemType} item is clicked`,
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.items = [
                    { id: 1, name: 'item1', path: 'root/item1', type: <any>itemType },
                    { id: 2, name: 'item2', path: 'root/item2', type: <any>itemType },
                    { id: 3, name: 'item3', path: 'root/item3', type: <any>itemType },
                ];
                // editorIsFocused won't change unless the editor is open
                state.mockState({
                    editor: {
                        editorIsOpen: true,
                    },
                });
                fixture.detectChanges();
                tick();
                let listItems = getListItems(fixture);

                // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
                ((listItems[0].querySelector('.item-name-only')) as any).click();

                const editorState = state.now.editor;
                expect(editorState.editorIsFocused).toEqual(true);
            }),
        );

    });

    it('does not focus the editor when a folder item is clicked',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.items = [
                { id: 1, name: 'item1', path: 'root/item1', type: 'folder' },
                { id: 2, name: 'item2', path: 'root/item2', type: 'folder' },
                { id: 3, name: 'item3', path: 'root/item3', type: 'folder' },
            ];
            fixture.detectChanges();
            tick();
            let listItems = getListItems(fixture);

            // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
            ((listItems[0].querySelector('.item-name-only')) as any).click();

            const editorState = state.now.editor;
            expect(editorState.editorIsFocused).toEqual(false);
        }),
    );

    it('uses the correct permissions when searching',
        componentTest(() => TestComponent, (fixture, instance) => {
            state.mockState({
                entities: {
                    ...state.now.entities,
                    page: {
                        1: { id: 1, type: 'page', name: 'Page 1', path: '', folderId: 11 },
                        2: { id: 2, type: 'page', name: 'Page 2', path: '', folderId: 11 },
                        3: { id: 3, type: 'page', name: 'Page 3', path: '', folderId: 22 },
                    },
                    folder: {
                        11: { id: 11, type: 'folder', name: 'Folder 11' },
                        12: { id: 22, type: 'folder', name: 'Folder 22', motherId: 11 },
                    },
                },
            });

            instance.isSearching = true;
            instance.items = [1, 2, 3].map(id => state.now.entities.page[id]);
            updateItemsInfoState({
                list: [1, 2, 3],
                total: 3,
            });

            fixture.detectChanges();
            tick();

            let contextMenus: MockItemContextMenu[] = fixture.debugElement
                .queryAll(By.directive(MockItemContextMenu))
                .map(debugElement => debugElement.componentInstance);

            // __forItem is added by the MockPermissionPipe
            const permissionItems = contextMenus.map(menu => (menu.permissions as any).__forItem);

            expect(contextMenus.length).toBe(3);
            expect(permissionItems).toEqual(instance.items);
        }),
    );

    describe('start page', () => {

        let currentFixture: ComponentFixture<TestComponent>;
        let testPage: Page;
        afterEach(() => currentFixture = undefined);

        function setupTestCase(fixture: ComponentFixture<TestComponent>): void {
            testPage = { type: 'page', id: 1, name: 'page1', path: 'root/page1', globalId: 'pageA' } as Page;
            const instance = (currentFixture = fixture).componentRef.instance;
            instance.itemType = 'page';
            instance.items = [testPage];
            updateItemsInfoState({
                list: [1],
                total: 1,
            });
        }

        function mockIsStartPage(startPage: boolean): void {
            currentFixture.componentRef.instance.startPageId = startPage ? 1 : 99999;
            currentFixture.detectChanges();
        }

        function hasStartPageIcon(fixture: ComponentFixture<TestComponent>): boolean {
            let icons: any[] = Array.from(fixture.nativeElement.querySelectorAll('start-page-icon'));
            return icons.length > 0;
        }

        it('shows an icon when a page is set as folder start page',
            componentTest(() => TestComponent, (fixture, instance) => {
                setupTestCase(fixture);

                mockIsStartPage(true);
                expect(hasStartPageIcon(fixture)).toBe(true);

                mockIsStartPage(false);
                expect(hasStartPageIcon(fixture)).toBe(false);

                tick();
            }),
        );

        it('passes the start page information on to its context menu',
            componentTest(() => TestComponent, (fixture, instance) => {
                setupTestCase(fixture);
                mockIsStartPage(true);

                const contextMenu: MockItemContextMenu = fixture.debugElement
                    .query(By.directive(MockItemContextMenu))
                    .componentInstance;

                expect(contextMenu).toBeDefined();
                expect(contextMenu.isFolderStartPage).toBe(true);

                mockIsStartPage(false);
                expect(contextMenu.isFolderStartPage).toBe(false);

                tick();
            }),
        );

    });

    describe('page languages', () => {

        it('shows a language indicator for pages with language variants ',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'page';
                instance.items = [
                    { ...getExamplePageData({ id: 1 }), languageVariants: [1, 2], deleted: { at: 0, by: null } },
                ];
                updateItemsInfoState({
                    list: [66],
                    total: 1,
                });
                fixture.detectChanges();

                const icons: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('page-language-indicator .language-icon'));
                const links: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('page-language-indicator .language-code'));
                // amount of icons shown (displayAllLanguages is disabled by default)
                expect(links.length).toBe(1);
                // icon of available language
                expect(icons[0].className).toMatch(new RegExp(/(available)/, 'g'));
                expect(links[0].textContent).toMatch(new RegExp(/(en)/, 'i'));

                tick();
            }),
        );

        it('shows a language indicator for pages with no language variants but an assigned language if displayAllLangauges is enabled',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'page';
                instance.items = [
                    { ...getExamplePageData({ id: 1 }), languageVariants: [1, 2], deleted: { at: 0, by: null } },
                ];
                updateItemsInfoState({
                    list: [66],
                    total: 1,
                });
                // expand language icons
                state.now.folder.displayAllLanguages = true;
                fixture.detectChanges();

                const icons: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('page-language-indicator .language-icon'));
                const links: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('page-language-indicator .language-code'));
                // amount of icons shown
                expect(links.length).toBe(2);
                // icon of available language
                expect(links[0].textContent).toMatch(new RegExp(/(en)/, 'i'));
                expect(icons[0].className).toMatch(new RegExp(/(available)/, 'g'));
                // icon of unavailable language
                expect(links[1].textContent).toMatch(new RegExp(/(de)/, 'i'));
                expect(icons[1].className).not.toMatch(new RegExp(/(available)/, 'g'));

                tick();
            }),
        );

    });

    it('highlights the item currently opened in the editor (folders, pages, files, and images in list view)',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.itemType = 'page';
            instance.items = [
                { type: 'page', name: 'page1', id: 1, path: 'root/page1' },
                { type: 'page', name: 'page2', id: 2, path: 'root/page2' },
                { type: 'page', name: 'page3', id: 3, path: 'root/page3' },
            ];
            updateItemsInfoState({
                list: [1, 2, 3],
                total: 3,
            });
            state.mockState({
                editor: {
                    editorIsOpen: true,
                    editMode: 'edit',
                    itemType: 'page',
                    itemId: 3,
                },
            });
            instance.itemInEditor = instance.items[2];
            fixture.detectChanges();

            const backgroundColors = Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('gtx-contents-list-item'))
                .map(itemElement => window.getComputedStyle(itemElement).backgroundColor);

            expect(backgroundColors[0]).toBe(backgroundColors[1]);
            expect(backgroundColors[1]).not.toBe(backgroundColors[2]);
            expect(backgroundColors[0]).not.toBe('');
            expect(backgroundColors[0]).not.toBeUndefined();

            tick();
        }),
    );

    it('jumps to the right page for the item currently opened in the editor (pages)',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.itemType = 'page';
            instance.items = [
                { type: 'page', name: 'page1', id: 1, path: 'root/page1' },
                { type: 'page', name: 'page2', id: 2, path: 'root/page2' },
                { type: 'page', name: 'page3', id: 3, path: 'root/page3' },
                { type: 'page', name: 'page4', id: 4, path: 'root/page4' },
                { type: 'page', name: 'page5', id: 5, path: 'root/page5' },
                { type: 'page', name: 'page6', id: 6, path: 'root/page6' },
                { type: 'page', name: 'page7', id: 7, path: 'root/page7' },
                { type: 'page', name: 'page8', id: 8, path: 'root/page8' },
                { type: 'page', name: 'page9', id: 9, path: 'root/page9' },
                { type: 'page', name: 'page10', id: 10, path: 'root/page10' },
                { type: 'page', name: 'page11', id: 11, path: 'root/page11' },
                { type: 'page', name: 'page12', id: 12, path: 'root/page12' },
            ];
            updateItemsInfoState({
                list: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
                total: 12,
            });
            state.mockState({
                editor: {
                    editorIsOpen: true,
                    editMode: 'edit',
                    itemType: 'page',
                    itemId: 12,
                },
            });
            instance.itemInEditor = instance.items[11];
            fixture.detectChanges();

            expect(folderActions.setCurrentPage).toHaveBeenCalledWith('page', 2);

            tick();
        }),
    );

    it('highlights the item currently opened in the editor (images)',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.itemType = 'image';
            instance.items = [
                { type: 'image', name: 'image1', id: 1, path: 'root/image1' },
                { type: 'image', name: 'image2', id: 2, path: 'root/image2' },
                { type: 'image', name: 'image3', id: 3, path: 'root/image3' },
            ];
            updateItemsInfoState({
                list: [1, 2, 3],
                total: 3,
            });
            state.mockState({
                ...state.now,
                folder: {
                    ...state.now.folder,
                    displayImagesGridView: true,
                },
                editor: {
                    editorIsOpen: true,
                    editMode: 'edit',
                    itemType: 'image',
                    itemId: 3,
                },
            });
            instance.itemInEditor = instance.items[2];
            fixture.detectChanges();

            const boxShadows = Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('image-thumbnail'))
                .map(itemElement => window.getComputedStyle(itemElement).boxShadow);

            expect(boxShadows[0]).toBe(boxShadows[1]);
            expect(boxShadows[1]).not.toBe(boxShadows[2]);
            expect(boxShadows[0]).not.toBe('');
            expect(boxShadows[0]).not.toBeUndefined();

            tick();
        }),
    );

});
