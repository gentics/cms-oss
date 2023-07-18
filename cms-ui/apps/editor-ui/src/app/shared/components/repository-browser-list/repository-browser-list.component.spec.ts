import { Component, DebugElement, Injectable, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BrowserDynamicTestingModule } from '@angular/platform-browser-dynamic/testing';
import { FolderApi } from '@editor-ui/app/core/providers/api';
import { Api } from '@editor-ui/app/core/providers/api/api.service';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { MockErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.mock';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { PermissionService } from '@editor-ui/app/core/providers/permissions/permission.service';
import { UserSettingsService } from '@editor-ui/app/core/providers/user-settings/user-settings.service';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { componentTest, configureComponentTest } from '@editor-ui/testing';
import { mockPipes } from '@editor-ui/testing/mock-pipe';
import {
    getExampleFolderDataNormalized,
    getExampleImageDataNormalized,
    getExampleNodeDataNormalized,
    getExamplePageDataNormalized,
    getExampleTemplateDataNormalized,
} from '@editor-ui/testing/test-data.mock';
import {
    AllowedSelection,
    AllowedSelectionType,
    BaseListResponse,
    Folder,
    Image as GtxImage,
    ItemInNode,
    Node,
    Normalized,
    Page,
    RepoItem,
    RepositoryBrowserOptions,
    RepositoryBrowserSorting,
    Template,
} from '@gentics/cms-models';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { RepositoryBrowserClient, RepositoryBrowserDataService } from '../../providers';
import { RepositoryBrowser } from '../repository-browser/repository-browser.component';

let testNodes: Node<Normalized>[];
let entityAmount: number;
let testFolders: Folder<Normalized>[];
let testPages: Page<Normalized>[];
let testImages: GtxImage<Normalized>[];
let testTemplates: Template<Normalized>[];

describe('RepositoryBrowserList', () => {

    let state: TestApplicationState;
    let repositoryBrowserClientService: RepositoryBrowserClient;

    let repositoryBrowserOptions: RepositoryBrowserOptions & { allowedSelection: AllowedSelectionType, selectMultiple: false };

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
            providers: [
                { provide: Api, useClass: MockApi },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                EntityResolver,
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: I18nService, useClass: MockI18nService },
                { provide: I18nNotification, useClass: MockI18nNotification },
                { provide: PermissionService, useClass: MockPermissionService },
                ModalService,
                { provide: UserSettingsService, useClass: MockUserSettingsService },
                { provide: RepositoryBrowserDataService, useClass: MockRepositoryBrowserDataService },
                RepositoryBrowserClient,
            ],
            declarations: [
                RepositoryBrowser,
                TestComponent,
                mockPipes('i18n', 'i18nDate'),
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });

        state = TestBed.get(ApplicationStateService);
        repositoryBrowserClientService = TestBed.get(RepositoryBrowserClient);
        expect(state instanceof ApplicationStateService).toBeTruthy();

        // prepare test data
        generateTestData();

        // set state accordingly
        state.mockState({
            entities: {
                node: testNodes,
                folder: testFolders,
                page: testPages,
                image: testImages,
                template: testTemplates,
            },
            folder: {
                folders: {
                    list: Array(entityAmount),
                },
                pages: {
                    list: Array(entityAmount),
                },
                images: {
                    list: Array(entityAmount),
                },
                files: {
                    list: [],
                },
                templates: {
                    list: Array(entityAmount),
                },
            },
        });

        repositoryBrowserOptions = {
            allowedSelection: 'page',
            selectMultiple: false,
        };
    });

    it('is displayed', componentTest(() => TestComponent, (fixture, instance) => {

        // open repository browser
        repositoryBrowserClientService.openRepositoryBrowser(repositoryBrowserOptions);
        tick();

        const repositoryBrowserModalElement: DebugElement = getRepositoryBrowser(fixture).element;
        expect(repositoryBrowserModalElement).toBeTruthy();

    }));

    // Todo
    // xit('displays correct data', componentTest(() => TestComponent, async (fixture, instance) => {

    // }));

});

function generateTestData(): void {
    // prepare test data
    testNodes = new Array(entityAmount).map((id: number) => ({
        ...getExampleNodeDataNormalized({ id }),
        name: `test-node-${id}`,
    }));
    entityAmount = 5;
    testFolders = new Array(entityAmount).map((id: number) => ({
        ...getExampleFolderDataNormalized({ id }),
        name: `test-folder-${id}`,
    }));
    testPages = new Array(entityAmount).map((id: number) => ({
        ...getExamplePageDataNormalized({ id }),
        name: `test-page-${id}`,
    }));
    testImages = new Array(entityAmount).map((id: number) => ({
        ...getExampleImageDataNormalized({ id }),
        name: `test-image-${id}`,
    }));
    testTemplates = new Array(entityAmount).map((id: number) => ({
        ...getExampleTemplateDataNormalized({ id, masterId: 1, userId: 1 }),
        name: `test-template-${id}`,
    }));
}

function getRepositoryBrowser(fixture: ComponentFixture<TestComponent>): { element: DebugElement, folders: any } {
    const element: DebugElement = fixture.debugElement.query(By.css('repository-browser'));
    // this is always empty, but it should not
    const folders: any = fixture.nativeElement.getElementsByClassName('item-list-row');
    return {
        element,
        folders,
    };
}

@Component({
    template: '<gtx-overlay-host></gtx-overlay-host>',
})
class TestComponent { }

@Injectable()
class MockApi {
    defaultResponse: BaseListResponse = {
        responseInfo: {
            responseCode: 'OK',
            responseMessage: '',
        },
        messages: [],
        hasMoreItems: false,
        numItems: 0,
    };

    folders: Partial<FolderApi>;

    constructor(
        private entityResolver: EntityResolver,
    ) {
        // prepare test data
        generateTestData();

        const nodeResponse = { ...this.defaultResponse, folders: [], nodes: testNodes.map(e => this.entityResolver.denormalizeEntity('node', e)) };
        const folderResponse = { ...this.defaultResponse, folders: testFolders.map(e => this.entityResolver.denormalizeEntity('folder', e)) };
        const pageResponse = { ...this.defaultResponse, pages: testPages.map(e => this.entityResolver.denormalizeEntity('page', e)) };
        const fileResponse = { ...this.defaultResponse, files: [] };
        const imageResponse = { ...this.defaultResponse, files: [] };

        this.folders = {
            getNodes: jasmine.createSpy('getNodes').and.returnValue(nodeResponse),
            getFolders: jasmine.createSpy('getFolders').and.returnValue(folderResponse),
            getPages: jasmine.createSpy('getPages').and.returnValue(pageResponse),
            getFiles: jasmine.createSpy('getFiles').and.returnValue(fileResponse),
            getImages: jasmine.createSpy('getImages').and.returnValue(imageResponse),
        };
    }

}

class MockUserSettingsService {
    loadInitialSettings = jasmine.createSpy('loadInitialSettings');
    loadUserSettingsWhenLoggedIn = jasmine.createSpy('loadUserSettingsWhenLoggedIn');
    saveRecentItemsOnUpdate = jasmine.createSpy('saveRecentItemsOnUpdate');
    watchForSettingChangesInOtherTabs = jasmine.createSpy('watchForSettingChangesInOtherTabs');
}

class MockI18nService {
    transform(): Observable<any> {
        return Observable.never();
    }
}

class MockI18nNotification {
    show = jasmine.createSpy();
}

class MockPermissionService {
    forItemInLanguage(): Observable<any> {
        return Observable.never();
    }
    forItem(): Observable<any> {
        return Observable.never();
    }
}

class MockRepositoryBrowserDataService {
    options: RepositoryBrowserOptions;

    allowed: AllowedSelection = {};
    isPickingFolder: boolean;
    itemTypes = ['folder', 'page', 'file', 'image', 'template'];
    submitLabelKey = 'modal.repository_browser_submit';
    titleKey = '';
    titleParams: { [key: string]: string | number } = {};

    canSubmit$: Observable<boolean>;
    currentNode$: Observable<number>;
    filter$: Observable<string>;
    hasPermissions$: Observable<boolean>;
    hasPermissions = false;
    itemTypesToDisplay$: Observable<AllowedSelectionType[]>;
    isDisplayingFavourites$: Observable<boolean>;
    isDisplayingFavouritesFolder$: Observable<boolean>;
    loading$: Observable<boolean>;
    noItemsOfAnyType$: Observable<boolean>;
    nodes$: Observable<Node[]>;
    parentItems$: Observable<Array<Folder | Page | Template | Node>>;
    search$: Observable<string>;
    selected$: Observable<ItemInNode[]>;
    showFavourites$: Observable<boolean>;
    startPageId$: Observable<number | undefined>;
    pageShowPath$: Observable<boolean>;

    /** Observable for each type that emits the display fields. */
    displayFieldsForType: { [key: string]: Observable<string[]> };

    /** Observables that emit the item list for each item type. */
    observableForType: { [key: string]: Observable<RepoItem[]> };

    /** Observables that emit the sort order for each item type. */
    sortOrder$: Observable<RepositoryBrowserSorting>;
}
