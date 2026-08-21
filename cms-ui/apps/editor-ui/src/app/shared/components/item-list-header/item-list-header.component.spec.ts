import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { I18nNotificationService } from '@gentics/cms-components';
import { Folder, Image, Page } from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { BehaviorSubject } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { EditorPermissions, ItemsInfo, getNoPermissions } from '../../../common/models';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { DecisionModalsService } from '../../../core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { UploadConflictService } from '../../../core/providers/upload-conflict/upload-conflict.service';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';
import { AllItemsSelectedPipe } from '../../../shared/pipes/all-items-selected/all-items-selected.pipe';
import { ApplicationStateService, FolderActionsService, UsageActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { AnyItemDeletedPipe } from '../../pipes/any-item-deleted/any-item-deleted.pipe';
import { AnyItemInheritedPipe } from '../../pipes/any-item-inherited/any-item-inherited.pipe';
import { AnyItemPublishedPipe } from '../../pipes/any-item-published/any-item-published.pipe';
import { AnyPageUnpublishedPipe } from '../../pipes/any-page-unpublished/any-page-unpublished.pipe';
import { FilterItemsPipe } from '../../pipes/filter-items/filter-items.pipe';
import { ItemListHeaderComponent } from './item-list-header.component';

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
        ></item-list-header>`,
    standalone: false,
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
                { provide: I18nNotificationService, useClass: MockI18nNotification },
            ],
            declarations: [
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
        state = TestBed.inject(ApplicationStateService) as any;
    });

    it('it works', componentTest(() => TestComponent, (fixture, instance) => {
        fixture.detectChanges();
        tick();
        expect(fixture.debugElement).toBeTruthy();
    }),
    );

    it('displays correct status info for selected not filtering items', componentTest(() => TestComponent, (fixture, instance) => {
        instance.selectedItems = [1];
        instance.filterTerm = '';

        fixture.detectChanges();
        tick();

        const elem = fixture.debugElement.query(By.css('item-list-header .item-list-status.selected:not(.filtering)'));
        expect(elem).toBeDefined();
    }));

    it('displays correct status info for selected and filtering item', componentTest(() => TestComponent, (fixture, instance) => {
        instance.selectedItems = [1, 2, 3];
        instance.filterTerm = 'xxx';

        fixture.detectChanges();
        tick();

        const elem = fixture.debugElement.query(By.css('item-list-header .item-list-status.selected.filtering'));
        expect(elem).toBeDefined();
    }));

    it('displays correct status info for not selected and all visible items', componentTest(() => TestComponent, (fixture, instance) => {
        instance.selectedItems = [];
        instance.itemsInfo.total = 20;
        instance.itemsInfo.itemsPerPage = 25;
        instance.filterTerm = '';

        fixture.detectChanges();
        tick();

        const elem = fixture.debugElement.query(By.css('item-list-header .item-list-status:not(.selected).all-items'));
        expect(elem).toBeDefined();
    }));

    it('displays correct status info for not selected and partial visible items', componentTest(() => TestComponent, (fixture, instance) => {
        instance.selectedItems = [];
        instance.itemsInfo.total = 30;
        instance.itemsInfo.itemsPerPage = 25;
        instance.filterTerm = '';

        fixture.detectChanges();
        tick();

        const elem = fixture.debugElement.query(By.css('item-list-header .item-list-status:not(.selected).partial-items'));
        expect(elem).toBeDefined();
    }));

    it('displays correct status info for not selected and filtering', componentTest(() => TestComponent, (fixture, instance) => {
        instance.selectedItems = [];
        instance.filterTerm = 'xxx';

        fixture.detectChanges();
        tick();

        const elem = fixture.debugElement.query(By.css('item-list-header .item-list-status:not(.selected).filtering'));
        expect(elem).toBeDefined();
    }));

});
