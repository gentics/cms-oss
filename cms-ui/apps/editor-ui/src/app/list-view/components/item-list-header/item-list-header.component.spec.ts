import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { EditorPermissions, ItemsInfo, getNoPermissions } from '@editor-ui/app/common/models';
import { ContextMenuOperationsService } from '@editor-ui/app/core/providers/context-menu-operations/context-menu-operations.service';
import { DecisionModalsService } from '@editor-ui/app/core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { UploadConflictService } from '@editor-ui/app/core/providers/upload-conflict/upload-conflict.service';
import { UserSettingsService } from '@editor-ui/app/core/providers/user-settings/user-settings.service';
import { AllItemsSelectedPipe } from '@editor-ui/app/shared/pipes/all-items-selected/all-items-selected.pipe';
import { I18nDatePipe } from '@editor-ui/app/shared/pipes/i18n-date/i18n-date.pipe';
import { ApplicationStateService, FolderActionsService, UsageActionsService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { componentTest, configureComponentTest } from '@editor-ui/testing';
import { Folder, Image, Page } from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { BehaviorSubject } from 'rxjs';
import { AnyItemDeletedPipe } from '../../pipes/any-item-deleted/any-item-deleted.pipe';
import { AnyItemInheritedPipe } from '../../pipes/any-item-inherited/any-item-inherited.pipe';
import { AnyItemPublishedPipe } from '../../pipes/any-item-published/any-item-published.pipe';
import { AnyPageUnpublishedPipe } from '../../pipes/any-page-unpublished/any-page-unpublished.pipe';
import { FilterItemsPipe } from '../../pipes/filter-items/filter-items.pipe';
import { ItemListHeaderComponent } from './item-list-header.component';

const enum ITEM_LIST_STATES {
    STATE_1 = 'item-list-status-1',
    STATE_2 = 'item-list-status-2',
    STATE_3 = 'item-list-status-3',
    STATE_4 = 'item-list-status-4',
    STATE_5 = 'item-list-status-5'
}

function getItemListStatus(fixture: ComponentFixture<TestComponent>): string {
    // get dom element
    const statusElements: HTMLElement[] = Array.from(fixture.nativeElement.querySelectorAll('item-list-header .item-list-status'));
    const statusElementClass = statusElements.map(e => ({ id: e.id, class: e.classList.value }));
    if (statusElementClass.length === 1 && statusElementClass[0].class) {
        return statusElementClass[0].id;
    } else {
        throw new Error('Unexpected status structure.');
    }
}

const allPermissions = (): EditorPermissions => // Sorry, but it works.
    JSON.parse(JSON.stringify(getNoPermissions()).replace(/false/g, 'true'));

@Component({
    template: `
    <item-list-header class="list-header icon-checkbox-trigger"
        [itemsInfo]="itemsInfo"
        [items]="items"
        [selectedItems]="selectedItems"
        [icon]="icon"
        [filterTerm]="filterTerm"
        [nodeLanguages]="languages$ | async"
        [itemType]="itemType"
        [acceptUploads]="acceptUploads"
        [folderPermissions]="folderPermissions"
        [canCreateItem]="canCreateItem"
        [activeNode]="activeNode"
        [currentFolderId]="currentFolderId"
        [showAllLanguages]="showAllLanguages$ | async"
        [showImagesGridView]="showImagesGridView$ | async"
        [activeLanguage]="activeLanguage"
        [showStatusIcons]="true"
    >
    </item-list-header>`,
})
class TestComponent {
    itemType = 'folder';
    items: Array<Partial<Page> | Partial<Folder> | Partial<Image>> = [
        { id: 1, name: 'item1', path: 'root/item1', type: 'folder' },
        { id: 2, name: 'item2', path: 'root/item2', type: 'folder' },
        { id: 3, name: 'item3', path: 'root/item3', type: 'folder' },
    ];
    selectedItems = [];
    activeNode: any = {
        name: '',
    };
    itemsInfo: ItemsInfo = {
        list: [1, 2, 3],
        selected: [],
        total: 3,
        hasMore: true,
        fetchAll: false,
        creating: false,
        fetching: false,
        saving: false,
        deleting: [],
        currentPage: 1,
        showPath: true,
        itemsPerPage: 10,
    };
    filterTerm = 'xxx';
    startPageId: number = Number.NaN;
    itemInEditor: any = undefined;
    linkPaths = false;
    isSearching = false;
    folderPermissions = allPermissions();
    activeLanguage = 'en';
    searchQueryActive$ = new BehaviorSubject<boolean>(false);
}

class MockUsageActionsService {
    getTotalUsage(): void {}
}

class MockFolderActions {
    getFolders(): void {}
    getPages(): void {}
    getFiles(): void {}
    getImages(): void {}
    getTemplates(): void {}
}

class MockUserSettingsService {}

class MockUploadConflictService {}

class MockContextMenuOperationsService {}

class MockI18nService {}

class MockI18nNotification {}

class MockNavigationService {
    instruction(): any {
        return {
            commands(): void {},
        };
    }
}

class MockDecisionModalService {
    getTotalUsage(): void {}
}

class MockErrorHandler {
    catch(): void {}
}

describe('ItemListHeader', () => {

    let state: TestApplicationState;

    beforeEach(() => {
        const testState = {
            folder: {
                searchTerm: '',
            },
        };
        configureComponentTest({
            imports: [GenticsUICoreModule.forRoot()],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                EntityResolver,
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: DecisionModalsService, useClass: MockDecisionModalService },
                { provide: UsageActionsService, useClass: MockUsageActionsService },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: UserSettingsService, useClass: MockUserSettingsService },
                { provide: UploadConflictService, useClass: MockUploadConflictService },
                { provide: ContextMenuOperationsService, useClass: MockContextMenuOperationsService },
                { provide: I18nService, useClass: MockI18nService },
                { provide: I18nNotification, useClass: MockI18nNotification },
            ],
            declarations: [
                I18nDatePipe,
                ItemListHeaderComponent,
                TestComponent,
                AllItemsSelectedPipe,
                FilterItemsPipe,
                AnyItemPublishedPipe,
                AnyPageUnpublishedPipe,
                AnyItemInheritedPipe,
                AnyItemDeletedPipe,
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });
        state = TestBed.get(ApplicationStateService);
    });

    it('it works', componentTest(() => TestComponent, (fixture, instance) => {
        fixture.detectChanges();
        tick();
        expect(fixture.debugElement).toBeTruthy();
    }),
    );

    it(`displays correct status info for ${ITEM_LIST_STATES.STATE_1}`, componentTest(() => TestComponent, (fixture, instance) => {
        instance.selectedItems = [1];
        instance.filterTerm = '';

        fixture.detectChanges();
        tick();
        expect(getItemListStatus(fixture)).toEqual(ITEM_LIST_STATES.STATE_1);
    }));

    it(`displays correct status info for ${ITEM_LIST_STATES.STATE_2}`, componentTest(() => TestComponent, (fixture, instance) => {
        instance.selectedItems = [1, 2, 3];
        instance.filterTerm = 'xxx';

        fixture.detectChanges();
        tick();
        expect(getItemListStatus(fixture)).toEqual(ITEM_LIST_STATES.STATE_2);
    }));

    it(`displays correct status info for ${ITEM_LIST_STATES.STATE_3}`, componentTest(() => TestComponent, (fixture, instance) => {
        instance.selectedItems = [];
        instance.itemsInfo.total = 20;
        instance.itemsInfo.itemsPerPage = 25;
        instance.filterTerm = '';

        fixture.detectChanges();
        tick();
        expect(getItemListStatus(fixture)).toEqual(ITEM_LIST_STATES.STATE_3);
    }));

    it(`displays correct status info for ${ITEM_LIST_STATES.STATE_4}`, componentTest(() => TestComponent, (fixture, instance) => {
        instance.selectedItems = [];
        instance.itemsInfo.total = 30;
        instance.itemsInfo.itemsPerPage = 25;
        instance.filterTerm = '';

        fixture.detectChanges();
        tick();
        expect(getItemListStatus(fixture)).toEqual(ITEM_LIST_STATES.STATE_4);
    }));

    it(`displays correct status info for ${ITEM_LIST_STATES.STATE_5}`, componentTest(() => TestComponent, (fixture, instance) => {
        instance.selectedItems = [];
        instance.filterTerm = 'xxx';

        fixture.detectChanges();
        tick();
        expect(getItemListStatus(fixture)).toEqual(ITEM_LIST_STATES.STATE_5);
    }));

});
