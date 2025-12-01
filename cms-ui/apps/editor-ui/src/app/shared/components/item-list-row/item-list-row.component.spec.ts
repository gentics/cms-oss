import {
    ChangeDetectorRef,
    Component,
    DebugElement,
    NO_ERRORS_SCHEMA,
    Pipe,
    PipeTransform,
    ViewChild,
} from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { I18nDatePipe, I18nService, WindowRef } from '@gentics/cms-components';
import { Favourite, File, Folder, GcmsPermission, Image, Page, PermissionsMapCollection } from '@gentics/cms-models';
import {
    getExampleFolderDataNormalized,
    getExamplePageData,
    getExamplePageDataNormalized,
} from '@gentics/cms-models/testing/test-data.mock';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { provideTranslateService } from '@ngx-translate/core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { EditorPermissions, getNoPermissions, ItemsInfo } from '../../../common/models';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { DecisionModalsService } from '../../../core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { FavouritesService } from '../../../core/providers/favourites/favourites.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import {
    FavouriteToggleComponent,
    FileStatusLabel,
    IconCheckbox,
    InheritedLocalizedIcon,
    ItemBreadcrumbsComponent,
    ItemStatusLabelComponent,
    ListItemDetails,
    PageLanguageIndicatorComponent,
    StartPageIcon,
} from '../../../shared/components';
import {
    FileSizePipe,
    GetInheritancePipe,
    HighlightPipe,
    IsFavouritePipe,
    ItemIsLocalizedPipe,
    ItemPathPipe,
    PageIsLockedPipe,
    RouterCommandsForItemPipe,
    TruncatePathPipe,
    UserFullNamePipe,
} from '../../../shared/pipes';
import {
    ApplicationStateService,
    FolderActionsService,
    SetDisplayAllLanguagesAction,
    SetDisplayStatusIconsAction,
    UsageActionsService,
    WastebinActionsService,
} from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { IsStartPagePipe } from '../../pipes/is-start-page/is-start-page.pipe';
import { ItemContextMenuComponent } from '../item-context-menu/item-context-menu.component';
import { ItemListRowComponent } from './item-list-row.component';

/**
 * Returns the name of the item.
 */
// eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
const getItemName = (listItem: Element): string => (listItem.querySelector('.item-name') as any).innerText;

@Component({
    template: `
        <item-list-row
            [activeNode]="activeNode"
            [item]="item"
            [itemInEditor]="itemInEditor"
            [icon]="'icon'"
            [selected]="true"
            [itemType]="itemType"
            [startPageId]="startPageId"
            [linkPaths]="isSearching"
            [nodeLanguages]="nodeLanguages"
            [itemsInfo]="itemsInfo"
        ></item-list-row>`,
    standalone: false,
})
class TestComponent {
    itemType = 'file';
    item: Partial<Page> | Partial<Folder> | Partial<Image> | Partial<File> = {
        id: 1,
        name: 'item1',
        path: 'root/item1',
        publishPath: '/root/item1',
        type: 'file',
        deleted: { at: 0, by: null },
    };

    activeNode: any = {
        name: '',
    };

    itemsInfo: ItemsInfo = {
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
        showPath: true,
        itemsPerPage: 0,
    };

    startPageId: number = Number.NaN;
    itemInEditor: any = undefined;
    linkPaths = false;
    isSearching = false;
    nodeLanguages = [
        { id: 1, code: 'en', name: 'English' },
        { id: 2, code: 'de', name: 'Deutsch (German)' },
        { id: 3, code: 'fr', name: 'Français (French)' },
    ];

    @ViewChild(ItemListRowComponent, { static: true }) itemListRow: ItemListRowComponent;
}

class MockNavigationService {
    instruction(): any {
        return {
            commands(): void {},
        };
    }
}

class MockFavouritesService {
    add = jasmine.createSpy('FavouritesService.add');
    remove = jasmine.createSpy('FavouritesService.remove');
}

class MockDecisionModalService {
    getTotalUsage(): void {}
}

class MockErrorHandler {
    catch(): void {}
}

@Pipe({
    name: 'permissions',
    standalone: false,
})
class MockPermissionPipe implements PipeTransform {
    transform(item: any): EditorPermissions {
        const val = {
            ...getNoPermissions(),
            // eslint-disable-next-line @typescript-eslint/naming-convention
            __forItem: item,
        };
        val.page.view = true;
        return val;
    }
}

class MockChangeDetector {
    markForCheck(): void { }
    detectChanges(): void { }
}

class MockWindowRef { }

class MockUsageActions {
    getTotalUsage(): void {}
}

class MockContextMenuOperationsService {}

class MockFolderActions {
    refreshList(): void {}
    getFolder(): void {}
    setDisplayAllPageLanguages(): void {}
    setDisplayStatusIcons(): void {}
}

class MockWastebinActionsService {
    restoreItemsFromWastebin = jasmine.createSpy('restoreItemsFromWastebin');
}

// Helper
const elementStateIsActive = (
    debugElement: DebugElement,
    cssClass: '.stateUntranslated' | '.stateModified' | '.stateInQueue' | '.statePlanned' | '.stateInherited' | '.stateLocalized',
): boolean => !(debugElement.query(By.css(cssClass))?.nativeElement?.hasAttribute?.('hidden') ?? true);

function getExampleFolderWithPermissions(
    { id, userId, publishDir }: { id: number; userId?: number; publishDir?: string } = { id: 115, userId: 3, publishDir: '/' },
) {
    const folder = getExampleFolderDataNormalized({ id, userId, publishDir });
    folder.permissionsMap = getDefaultTestPermissions();
    return folder;
}

const TEST_FOLDER_ID = 21;
function getDefaultTestPermissions(): PermissionsMapCollection {
    return {
        permissions: {
            [GcmsPermission.VIEW]: true,
            [GcmsPermission.READ]: true,
            [GcmsPermission.READ_ITEMS]: true,
            [GcmsPermission.EDIT]: true,
            [GcmsPermission.DELETE]: true,
            [GcmsPermission.DELETE_ITEMS]: true,
            [GcmsPermission.PUBLISH_PAGES]: true,
        },
    };
}

describe('ItemListRow', () => {

    let state: TestApplicationState;
    let i18n: I18nService;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                NoopAnimationsModule,
                GenticsUICoreModule.forRoot(),
            ],
            providers: [
                provideTranslateService(),
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: ChangeDetectorRef, useClass: MockChangeDetector },
                { provide: ContextMenuOperationsService, useClass: MockContextMenuOperationsService },
                { provide: DecisionModalsService, useClass: MockDecisionModalService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: FavouritesService, useClass: MockFavouritesService },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: UsageActionsService, useClass: MockUsageActions },
                { provide: WastebinActionsService, useClass: MockWastebinActionsService },
                { provide: WindowRef, useClass: MockWindowRef },
                I18nService,
                EntityResolver,
            ],
            declarations: [
                FavouriteToggleComponent,
                FileSizePipe,
                HighlightPipe,
                I18nDatePipe,
                IconCheckbox,
                InheritedLocalizedIcon,
                IsFavouritePipe,
                IsStartPagePipe,
                ItemBreadcrumbsComponent,
                ItemContextMenuComponent,
                ItemIsLocalizedPipe,
                ItemListRowComponent,
                ItemPathPipe,
                ListItemDetails,
                GetInheritancePipe,
                MockPermissionPipe,
                PageIsLockedPipe,
                PageLanguageIndicatorComponent,
                ItemStatusLabelComponent,
                FileStatusLabel,
                RouterCommandsForItemPipe,
                StartPageIcon,
                TestComponent,
                TruncatePathPipe,
                UserFullNamePipe,
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });

        state = TestBed.inject(ApplicationStateService) as any;
        expect(state instanceof ApplicationStateService).toBeTruthy();
        i18n = TestBed.inject(I18nService);
        state.mockState({
            entities: {
                folder: {
                    [TEST_FOLDER_ID]: {
                        permissionsMap: getDefaultTestPermissions(),
                    },
                },
            },
        });
    });

    it('binds the item name',
        componentTest(() => TestComponent, (fixture) => {
            fixture.detectChanges();
            tick();
            expect(getItemName(fixture.nativeElement)).toContain('item1');
        }),
    );

    it('shows online status for images that are online',
        componentTest(() => TestComponent, (fixture, instance) => {
            const testImage: Partial<Image> = { name: 'item1', path: 'root/item1', globalId: 'itemA', type: 'image', online: true };
            instance.item = testImage;
            fixture.detectChanges();
            tick();

            const getImageStatus = (el: Element) => (el.querySelector('.online'));
            expect(getImageStatus(fixture.nativeElement)).toBeTruthy();
        }),
    );

    it('shows offline status for images that are offline',
        componentTest(() => TestComponent, (fixture, instance) => {
            const testImage: Partial<Image> = { name: 'item1', path: 'root/item1', globalId: 'itemA', type: 'image', online: false };
            instance.item = testImage;
            fixture.detectChanges();
            tick();

            const getImageStatus = (el: Element) => (el.querySelector('.online'));
            expect(getImageStatus(fixture.nativeElement)).toBeFalsy();
        }),
    );

    it('binds the item path',
        componentTest(() => TestComponent, (fixture) => {
            fixture.detectChanges();
            tick();
            // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
            const getItemFilename = (el: Element) => (el.querySelector('.file-name') as any).innerText;
            expect(getItemFilename(fixture.nativeElement)).toContain('root/item1');
        }),
    );

    describe('page languages', () => {

        it('does not show a language indicator for pages when less than 2 node languages',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'page';
                instance.item = { ...getExamplePageData({ id: 1 }), languageVariants: [], deleted: { at: 0, by: null } };
                instance.nodeLanguages = [
                    { id: 1, code: 'en', name: 'English' },
                ];
                fixture.detectChanges();

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBe(null);

                tick();
            }),
        );

        it('shows a language indicator for translated pages without additional status icons and without all untranslated languages visible',
            componentTest(() => TestComponent, (fixture, instance) => {
                fixture.detectChanges();

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBe(null);

                instance.itemType = 'page';
                instance.item = {
                    ...getExamplePageData({ id: 1 }),
                    languageVariants: [1, 2],
                    online: true,
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };

                fixture.detectChanges();

                state.dispatch(new SetDisplayStatusIconsAction(false));
                state.dispatch(new SetDisplayAllLanguagesAction(false));

                tick();

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBeTruthy();
                // If present the css-class 'statePublished' indicates published state.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statePublished')).toBe(true);
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(false);
            }),
        );

        it('does show an offline language indicator for English page without additional status icons and without all untranslated languages visible',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'page';
                instance.item = {
                    ...getExamplePageData({ id: 1 }),
                    languageVariants: [1, 2],
                    online: false,
                    modified: true,
                    queued: false,
                    planned: false,
                    inherited: false,
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };

                state.dispatch(new SetDisplayStatusIconsAction(false));
                state.dispatch(new SetDisplayAllLanguagesAction(false));

                fixture.detectChanges();
                tick();

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBeTruthy();
                // If present the css-class 'statePublished' indicates published state.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statePublished')).toBe(false);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(false);
            }),
        );

        it('does show an published language indicator for English page without additional status icons and without all untranslated languages visible',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'page';
                instance.item = {
                    ...getExamplePageData({ id: 1 }),
                    languageVariants: [1, 2],
                    online: true,
                    modified: true,
                    queued: false,
                    planned: false,
                    inherited: false,
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };

                state.dispatch(new SetDisplayStatusIconsAction(false));
                state.dispatch(new SetDisplayAllLanguagesAction(false));

                fixture.detectChanges();
                tick();

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBeTruthy();
                // If present the css-class 'statePublished' indicates published state.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statePublished')).toBe(true);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(false);
            }),
        );

        it('does show a language indicator for English page and with additional status icon "modified" and without all untranslated languages visible',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'page';
                instance.item = {
                    ...getExamplePageData({ id: 1 }),
                    languageVariants: [1, 2],
                    online: true,
                    modified: true,
                    queued: false,
                    planned: false,
                    inherited: false,
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };

                state.dispatch(new SetDisplayStatusIconsAction(true));
                state.dispatch(new SetDisplayAllLanguagesAction(false));

                fixture.detectChanges();
                tick();

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBeTruthy();
                // If present the css-class 'statePublished' indicates published state.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statePublished')).toBe(true);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                // additional status icons should not be visible
                expect(elementStateIsActive(fixture.debugElement, '.stateInQueue')).toBe(false);
                expect(elementStateIsActive(fixture.debugElement, '.stateModified')).toBe(true);
                expect(elementStateIsActive(fixture.debugElement, '.statePlanned')).toBe(false);
                expect(elementStateIsActive(fixture.debugElement, '.stateInherited')).toBe(false);
                expect(elementStateIsActive(fixture.debugElement, '.stateLocalized')).toBe(false);
            }),
        );

        it('does show a language indicator for English page and with additional status icon "queued" and without all untranslated languages visible',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'page';
                instance.item = {
                    ...getExamplePageData({ id: 1 }),
                    languageVariants: [1, 2],
                    online: true,
                    modified: false,
                    queued: true,
                    planned: false,
                    inherited: false,
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };

                state.dispatch(new SetDisplayStatusIconsAction(true));
                state.dispatch(new SetDisplayAllLanguagesAction(false));

                fixture.detectChanges();
                tick();

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBeTruthy();
                // If present the css-class 'statePublished' indicates published state.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statePublished')).toBe(true);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                // additional status icons should not be visible
                expect(elementStateIsActive(fixture.debugElement, '.stateInQueue')).toBe(true);
                expect(elementStateIsActive(fixture.debugElement, '.stateModified')).toBe(false);
                expect(elementStateIsActive(fixture.debugElement, '.statePlanned')).toBe(false);
                expect(elementStateIsActive(fixture.debugElement, '.stateInherited')).toBe(false);
                expect(elementStateIsActive(fixture.debugElement, '.stateLocalized')).toBe(false);
            }),
        );

        it('does show a language indicator for English page and with additional status icon "planned" and without all untranslated languages visible',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'page';
                instance.item = {
                    ...getExamplePageData({ id: 1 }),
                    languageVariants: [1, 2],
                    online: true,
                    modified: false,
                    queued: false,
                    planned: true,
                    inherited: false,
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };

                state.dispatch(new SetDisplayStatusIconsAction(true));
                state.dispatch(new SetDisplayAllLanguagesAction(false));

                fixture.detectChanges();
                tick();

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBeTruthy();
                // If present the css-class 'statePublished' indicates published state.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statePublished')).toBe(true);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                // additional status icons should not be visible
                expect(elementStateIsActive(fixture.debugElement, '.stateInQueue')).toBe(false);
                expect(elementStateIsActive(fixture.debugElement, '.stateModified')).toBe(false);
                expect(elementStateIsActive(fixture.debugElement, '.statePlanned')).toBe(true);
                expect(elementStateIsActive(fixture.debugElement, '.stateInherited')).toBe(false);
                expect(elementStateIsActive(fixture.debugElement, '.stateLocalized')).toBe(false);
            }),
        );

        it('does show a language indicator for English page and with additional status icon "inherited" and without all untranslated languages visible',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.itemType = 'page';
                instance.item = {
                    ...getExamplePageData({ id: 1 }),
                    languageVariants: [1, 2],
                    online: true,
                    modified: false,
                    queued: false,
                    planned: false,
                    inherited: true,
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };

                state.dispatch(new SetDisplayStatusIconsAction(true));
                state.dispatch(new SetDisplayAllLanguagesAction(false));

                fixture.detectChanges();
                tick();

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBeTruthy();
                // If present the css-class 'statePublished' indicates published state.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statePublished')).toBe(true);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                // additional status icons should not be visible
                expect(elementStateIsActive(fixture.debugElement, '.stateInQueue')).toBe(false);
                expect(elementStateIsActive(fixture.debugElement, '.stateModified')).toBe(false);
                expect(elementStateIsActive(fixture.debugElement, '.statePlanned')).toBe(false);
                expect(elementStateIsActive(fixture.debugElement, '.stateInherited')).toBe(true);
                expect(elementStateIsActive(fixture.debugElement, '.stateLocalized')).toBe(false);
            }),
        );

        it('does show language indicators for English page without additional status icons and with all untranslated languages visible',
            componentTest(() => TestComponent, (fixture, instance) => {
                const pageEN = {
                    ...getExamplePageDataNormalized({ id: 1 }),
                    languageVariants: [1, 2],
                    online: true,
                    language: 'en',
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };
                const pageDE = {
                    ...getExamplePageDataNormalized({ id: 2 }),
                    languageVariants: [1, 2],
                    online: false,
                    language: 'de',
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };
                instance.itemType = 'page';
                instance.item = pageEN;

                state.mockState({
                    entities: {
                        language: {
                            1: { id: 1, code: 'en', name: 'English' },
                            2: { id: 2, code: 'de', name: 'Deutsch (German)' },
                            3: { id: 3, code: 'fr', name: 'Français (French)' },
                        },
                        page: {
                            1: pageEN,
                            2: pageDE,
                        },
                        folder: {
                            [pageEN.folder]: getExampleFolderWithPermissions({ id: pageEN.folder }),
                            [pageDE.folder]: getExampleFolderWithPermissions({ id: pageDE.folder }),
                        },
                    },
                    folder: {
                        activeNodeLanguages: {
                            list: [1, 2, 3],
                        },
                        pages: {
                            list: [1, 2],
                        },
                    },
                });

                state.dispatch(new SetDisplayStatusIconsAction(false));
                state.dispatch(new SetDisplayAllLanguagesAction(true));

                fixture.detectChanges();
                tick();

                const pageLanguageIndicatorLanguageIcons = fixture.debugElement.queryAll(By.css('page-language-indicator .language-icon'));

                const pageLanguageIndicatorLanguageIconEN = pageLanguageIndicatorLanguageIcons
                    .find((languageIcon) => languageIcon.query(By.css('.language-code')).nativeElement.innerText === 'EN');
                const pageLanguageIndicatorLanguageIconDE = pageLanguageIndicatorLanguageIcons
                    .find((languageIcon) => languageIcon.query(By.css('.language-code')).nativeElement.innerText === 'DE');
                const pageLanguageIndicatorLanguageIconFR = pageLanguageIndicatorLanguageIcons
                    .find((languageIcon) => languageIcon.query(By.css('.language-code')).nativeElement.innerText === 'FR');

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBeTruthy();
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(false);

                // If present the css-class 'statePublished' indicates published state.
                expect(pageLanguageIndicatorLanguageIconEN.nativeElement.classList.contains('statePublished')).toBe(true);

                // If present the css-class 'statePublished' indicates published state.
                expect(pageLanguageIndicatorLanguageIconDE.nativeElement.classList.contains('statePublished')).toBe(false);
                // expect(pageLanguageIndicatorLanguageIconDE.query(By.css('.stateUntranslated')).nativeElement.hasAttribute('hidden')).toBe(true);

                // If present the css-class 'statePublished' indicates published state.
                expect(pageLanguageIndicatorLanguageIconFR.nativeElement.classList.contains('statePublished')).toBe(false);
                expect(pageLanguageIndicatorLanguageIconFR.query(By.css('.stateUntranslated')).nativeElement.hasAttribute('hidden')).toBe(false);
            }),
        );

        it('does show language indicators for English and German page with additional status icon "modified" and with all untranslated languages visible',
            componentTest(() => TestComponent, (fixture, instance) => {
                const pageEN = {
                    ...getExamplePageDataNormalized({ id: 1 }),
                    languageVariants: [1, 2],
                    online: true,
                    modified: true,
                    planned: false,
                    inherited: false,
                    language: 'en',
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };
                const pageDE = {
                    ...getExamplePageDataNormalized({ id: 2 }),
                    languageVariants: [1, 2],
                    online: true,
                    modified: false,
                    planned: false,
                    inherited: false,
                    language: 'de',
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };
                instance.itemType = 'page';
                instance.item = pageEN;
                instance.itemsInfo.total = 1;

                state.mockState({
                    entities: {
                        language: {
                            1: { id: 1, code: 'en', name: 'English' },
                            2: { id: 2, code: 'de', name: 'Deutsch (German)' },
                            3: { id: 3, code: 'fr', name: 'Français (French)' },
                        },
                        page: {
                            1: pageEN,
                            2: pageDE,
                        },
                    },
                });

                state.dispatch(new SetDisplayStatusIconsAction(true));
                state.dispatch(new SetDisplayAllLanguagesAction(true));

                fixture.detectChanges();
                tick();

                const pageLanguageIndicatorLanguageIcons = fixture.debugElement.queryAll(By.css('page-language-indicator .language-icon'));

                const pageLanguageIndicatorLanguageIconEN = pageLanguageIndicatorLanguageIcons
                    .find((languageIcon) => languageIcon.query(By.css('.language-code')).nativeElement.innerText === 'EN');
                const pageLanguageIndicatorLanguageIconDE = pageLanguageIndicatorLanguageIcons
                    .find((languageIcon) => languageIcon.query(By.css('.language-code')).nativeElement.innerText === 'DE');
                const pageLanguageIndicatorLanguageIconFR = pageLanguageIndicatorLanguageIcons
                    .find((languageIcon) => languageIcon.query(By.css('.language-code')).nativeElement.innerText === 'FR');

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBeTruthy();

                // If present the css-class 'statePublished' indicates published state.
                expect(pageLanguageIndicatorLanguageIconEN.nativeElement.classList.contains('statePublished')).toBe(true);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.stateModified')).toBe(true);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.statePlanned')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.stateInherited')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.stateInQueue')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.stateLocalized')).toBe(false);

                // If present the css-class 'statePublished' indicates published state.
                expect(pageLanguageIndicatorLanguageIconDE.nativeElement.classList.contains('statePublished')).toBe(true);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.stateModified')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.statePlanned')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.stateInherited')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.stateInQueue')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.stateLocalized')).toBe(false);

                // If present the css-class 'statePublished' indicates published state.
                expect(pageLanguageIndicatorLanguageIconFR.nativeElement.classList.contains('statePublished')).toBe(false);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                expect(pageLanguageIndicatorLanguageIconFR.query(By.css('.stateUntranslated')).nativeElement.hasAttribute('hidden')).toBe(false);
            }),
        );

        it('does show language indicators for English and German page with additional status icon "planned" and with all untranslated languages visible',
            componentTest(() => TestComponent, (fixture, instance) => {
                const pageEN = {
                    ...getExamplePageDataNormalized({ id: 1 }),
                    languageVariants: [1, 2],
                    online: true,
                    modified: false,
                    planned: true,
                    inherited: false,
                    language: 'en',
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };
                const pageDE = {
                    ...getExamplePageDataNormalized({ id: 2 }),
                    languageVariants: [1, 2],
                    online: true,
                    modified: false,
                    planned: false,
                    inherited: false,
                    language: 'de',
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };
                instance.itemType = 'page';
                instance.item = pageEN;
                instance.itemsInfo.total = 1;

                state.mockState({
                    entities: {
                        language: {
                            1: { id: 1, code: 'en', name: 'English' },
                            2: { id: 2, code: 'de', name: 'Deutsch (German)' },
                            3: { id: 3, code: 'fr', name: 'Français (French)' },
                        },
                        page: {
                            1: pageEN,
                            2: pageDE,
                        },
                    },
                });

                state.dispatch(new SetDisplayStatusIconsAction(true));
                state.dispatch(new SetDisplayAllLanguagesAction(true));

                fixture.detectChanges();
                tick();

                const pageLanguageIndicatorLanguageIcons = fixture.debugElement.queryAll(By.css('page-language-indicator .language-icon'));

                const pageLanguageIndicatorLanguageIconEN = pageLanguageIndicatorLanguageIcons
                    .find((languageIcon) => languageIcon.query(By.css('.language-code')).nativeElement.innerText === 'EN');
                const pageLanguageIndicatorLanguageIconDE = pageLanguageIndicatorLanguageIcons
                    .find((languageIcon) => languageIcon.query(By.css('.language-code')).nativeElement.innerText === 'DE');
                const pageLanguageIndicatorLanguageIconFR = pageLanguageIndicatorLanguageIcons
                    .find((languageIcon) => languageIcon.query(By.css('.language-code')).nativeElement.innerText === 'FR');

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBeTruthy();

                // If present the css-class 'statePublished' indicates published state.
                expect(pageLanguageIndicatorLanguageIconEN.nativeElement.classList.contains('statePublished')).toBe(true);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.stateModified')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.statePlanned')).toBe(true);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.stateInherited')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.stateInQueue')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.stateLocalized')).toBe(false);

                // If present the css-class 'statePublished' indicates published state.
                expect(pageLanguageIndicatorLanguageIconDE.nativeElement.classList.contains('statePublished')).toBe(true);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.stateModified')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.statePlanned')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.stateInherited')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.stateInQueue')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.stateLocalized')).toBe(false);

                // If present the css-class 'statePublished' indicates published state.
                expect(pageLanguageIndicatorLanguageIconFR.nativeElement.classList.contains('statePublished')).toBe(false);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                expect(pageLanguageIndicatorLanguageIconFR.query(By.css('.stateUntranslated')).nativeElement.hasAttribute('hidden')).toBe(false);
            }),
        );

        it('does show language indicators for English and German page with additional status icon "inherited" and with all untranslated languages visible',
            componentTest(() => TestComponent, (fixture, instance) => {
                const pageEN = {
                    ...getExamplePageDataNormalized({ id: 1 }),
                    languageVariants: [1, 2],
                    online: true,
                    modified: false,
                    planned: false,
                    inherited: true,
                    language: 'en',
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };
                const pageDE = {
                    ...getExamplePageDataNormalized({ id: 2 }),
                    languageVariants: [1, 2],
                    online: true,
                    modified: false,
                    planned: false,
                    inherited: false,
                    language: 'de',
                    deleted: {
                        at: 0,
                        by: null,
                    },
                };
                instance.itemType = 'page';
                instance.item = pageEN;
                instance.itemsInfo.total = 1;

                state.mockState({
                    entities: {
                        language: {
                            1: { id: 1, code: 'en', name: 'English' },
                            2: { id: 2, code: 'de', name: 'Deutsch (German)' },
                            3: { id: 3, code: 'fr', name: 'Français (French)' },
                        },
                        page: {
                            1: pageEN,
                            2: pageDE,
                        },
                    },
                });

                state.dispatch(new SetDisplayStatusIconsAction(true));
                state.dispatch(new SetDisplayAllLanguagesAction(true));

                fixture.detectChanges();
                tick();

                const pageLanguageIndicatorLanguageIcons = fixture.debugElement.queryAll(By.css('page-language-indicator .language-icon'));

                const pageLanguageIndicatorLanguageIconEN = pageLanguageIndicatorLanguageIcons
                    .find((languageIcon) => languageIcon.query(By.css('.language-code')).nativeElement.innerText === 'EN');
                const pageLanguageIndicatorLanguageIconDE = pageLanguageIndicatorLanguageIcons
                    .find((languageIcon) => languageIcon.query(By.css('.language-code')).nativeElement.innerText === 'DE');
                const pageLanguageIndicatorLanguageIconFR = pageLanguageIndicatorLanguageIcons
                    .find((languageIcon) => languageIcon.query(By.css('.language-code')).nativeElement.innerText === 'FR');

                expect(fixture.nativeElement.querySelector('page-language-indicator')).toBeTruthy();

                // If present the css-class 'statePublished' indicates published state.
                expect(pageLanguageIndicatorLanguageIconEN.nativeElement.classList.contains('statePublished')).toBe(true);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.stateModified')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.statePlanned')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.stateInherited')).toBe(true);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.stateInQueue')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconEN, '.stateLocalized')).toBe(false);

                // If present the css-class 'statePublished' indicates published state.
                expect(pageLanguageIndicatorLanguageIconDE.nativeElement.classList.contains('statePublished')).toBe(true);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.stateModified')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.statePlanned')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.stateInherited')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.stateInQueue')).toBe(false);
                expect(elementStateIsActive(pageLanguageIndicatorLanguageIconDE, '.stateLocalized')).toBe(false);

                // If present the css-class 'statePublished' indicates published state.
                expect(pageLanguageIndicatorLanguageIconFR.nativeElement.classList.contains('statePublished')).toBe(false);
                // While DOM elements indicating states might be present, they won't be visible without the css class 'statusInfos'.
                expect(fixture.debugElement.query(By.css('.language-icon')).nativeElement.classList.contains('statusInfos')).toBe(true);
                expect(pageLanguageIndicatorLanguageIconFR.query(By.css('.stateUntranslated')).nativeElement.hasAttribute('hidden')).toBe(false);
            }),
        );

    });

    describe('start page', () => {

        beforeEach(() => {
            i18n.setLanguage('en');
        });

        it('shows no start page icon if startPageId is not set',
            componentTest(() => TestComponent, (fixture, instance) => {
                const item: Partial<Page> = {
                    ...getExamplePageDataNormalized({ id: 1 }),
                    languageVariants: [1],
                    online: true,
                    modified: false,
                    planned: false,
                    inherited: false,
                    language: 'de',
                };
                instance.item = item;
                instance.startPageId = undefined;
                fixture.detectChanges();
                tick();
                const startPageIcon = fixture.nativeElement.querySelector('start-page-icon');
                expect(startPageIcon).toBeNull();
            }),
        );

        it('shows no start page icon if startPageId does not match one of the ids of the page and its language variants',
            componentTest(() => TestComponent, (fixture, instance) => {
                const item: Partial<Page> = {
                    ...getExamplePageDataNormalized({ id: 1 }),
                    languageVariants: [1, 2, 3],
                    online: true,
                    modified: false,
                    planned: false,
                    inherited: false,
                    language: 'de',
                };
                instance.item = item;
                instance.startPageId = 4;
                fixture.detectChanges();
                tick();
                const startPageIcon = fixture.nativeElement.querySelector('start-page-icon');
                expect(startPageIcon).toBeNull();
            }),
        );

        it('shows a start page icon if startPageId matches the id of the page',
            componentTest(() => TestComponent, (fixture, instance) => {
                const item: Partial<Page> = {
                    ...getExamplePageDataNormalized({ id: 1 }),
                    languageVariants: [1],
                    online: true,
                    modified: false,
                    planned: false,
                    inherited: false,
                    language: 'de',
                };
                instance.item = item;
                instance.startPageId = 1;
                fixture.detectChanges();
                tick();
                const startPageIcon = fixture.nativeElement.querySelector('start-page-icon');
                expect(startPageIcon).toBeTruthy();
            }),
        );

        it('shows a start page icon if startPageId matches an id of a languageVariant',
            componentTest(() => TestComponent, (fixture, instance) => {
                const item: Partial<Page> = {
                    ...getExamplePageDataNormalized({ id: 1 }),
                    languageVariants: [1, 2, 3],
                    online: true,
                    modified: false,
                    planned: false,
                    inherited: false,
                    language: 'de',
                };
                instance.item = item;
                instance.startPageId = 3;
                fixture.detectChanges();
                tick();
                const startPageIcon = fixture.nativeElement.querySelector('start-page-icon');
                expect(startPageIcon).toBeTruthy();
            }),
        );
    });

    describe('start page', () => {

        beforeEach(() => {
            i18n.setLanguage('en');
        });

        it('shows no start page icon if startPageId is not set',
            componentTest(() => TestComponent, (fixture, instance) => {
                const item: Partial<Page> = {
                    ...getExamplePageDataNormalized({ id: 1 }),
                    languageVariants: [1],
                    online: true,
                    modified: false,
                    planned: false,
                    inherited: false,
                    language: 'de',
                };
                instance.item = item;
                instance.startPageId = undefined;
                fixture.detectChanges();
                tick();
                const startPageIcon = fixture.nativeElement.querySelector('start-page-icon');
                expect(startPageIcon).toBeNull();
            }),
        );

        it('shows no start page icon if startPageId does not match one of the ids of the page and its language variants',
            componentTest(() => TestComponent, (fixture, instance) => {
                const item: Partial<Page> = {
                    ...getExamplePageDataNormalized({ id: 1 }),
                    languageVariants: [1, 2, 3],
                    online: true,
                    modified: false,
                    planned: false,
                    inherited: false,
                    language: 'de',
                };
                instance.item = item;
                instance.startPageId = 4;
                fixture.detectChanges();
                tick();
                const startPageIcon = fixture.nativeElement.querySelector('start-page-icon');
                expect(startPageIcon).toBeNull();
            }),
        );

        it('shows a start page icon if startPageId matches the id of the page',
            componentTest(() => TestComponent, (fixture, instance) => {
                const item: Partial<Page> = {
                    ...getExamplePageDataNormalized({ id: 1 }),
                    languageVariants: [1],
                    online: true,
                    modified: false,
                    planned: false,
                    inherited: false,
                    language: 'de',
                };
                instance.item = item;
                instance.startPageId = 1;
                fixture.detectChanges();
                tick();
                const startPageIcon = fixture.nativeElement.querySelector('start-page-icon');
                expect(startPageIcon).toBeTruthy();
            }),
        );

        it('shows a start page icon if startPageId matches an id of a languageVariant',
            componentTest(() => TestComponent, (fixture, instance) => {
                const item: Partial<Page> = {
                    ...getExamplePageDataNormalized({ id: 1 }),
                    languageVariants: [1, 2, 3],
                    online: true,
                    modified: false,
                    planned: false,
                    inherited: false,
                    language: 'de',
                };
                instance.item = item;
                instance.startPageId = 3;
                fixture.detectChanges();
                tick();
                const startPageIcon = fixture.nativeElement.querySelector('start-page-icon');
                expect(startPageIcon).toBeTruthy();
            }),
        );
    });

    describe('favourites', () => {

        let state: TestApplicationState;
        let favourites: MockFavouritesService;

        beforeEach(() => {
            state = TestBed.inject(ApplicationStateService) as any;
            expect(state instanceof TestApplicationState).toBe(true);

            favourites = TestBed.inject(FavouritesService) as any;
            expect(favourites instanceof MockFavouritesService).toBe(true);
        });

        function getIconButton(text: string, fixture: ComponentFixture<TestComponent>): HTMLElement {
            const iconButton: any = fixture.nativeElement.querySelector('favourite-toggle gtx-button');
            return iconButton;
        }

        it('adds to favourites when favourite star is clicked',
            componentTest(() => TestComponent, (fixture, instance) => {
                const testFolder: Partial<Folder> = { name: 'item1', path: 'root/item1', globalId: 'itemA', type: 'folder' };
                instance.item = testFolder;
                instance.itemsInfo.list = [1];
                instance.itemsInfo.total = 1;
                state.mockState({ favourites: { list: [] } });
                fixture.detectChanges();
                tick();

                const addFavButton = getIconButton('star_border', fixture);
                expect(addFavButton).toBeDefined('"Add to favourites" button is not visible.');
                addFavButton.click();

                expect(favourites.add).toHaveBeenCalledTimes(1);
                expect(favourites.add).toHaveBeenCalledWith([testFolder]);
            }),
        );

        it('removes from favourites when unfavourite star is clicked',
            componentTest(() => TestComponent, (fixture, instance) => {
                const testFolder: Partial<Folder> = { name: 'item1', path: 'root/item1', globalId: 'itemA', type: 'folder' };
                instance.item = testFolder;
                instance.itemsInfo.list = [1];
                instance.itemsInfo.total = 1;
                state.mockState({
                    folder: { ...state.now, activeNode: 5 },
                    favourites: {
                        list: [
                            { globalId: 'itemA', nodeId: 5, type: 'folder' } as Favourite,
                        ],
                    },
                });
                fixture.detectChanges();
                tick();

                const removeFavButton = getIconButton('star', fixture);
                expect(removeFavButton).toBeDefined('"Remove from favourites" button is not visible.');
                removeFavButton.click();

                expect(favourites.remove).toHaveBeenCalledTimes(1);
                expect(favourites.remove).toHaveBeenCalledWith([testFolder], { nodeId: 5 });
            }),
        );

    });

});
