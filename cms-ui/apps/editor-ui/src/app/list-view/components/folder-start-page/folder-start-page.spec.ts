import { Component, ViewChild } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { EntityState } from '@editor-ui/app/common/models';
import { ContextMenuOperationsService } from '@editor-ui/app/core/providers/context-menu-operations/context-menu-operations.service';
import { Folder, FolderRequestOptions, Normalized, Page, PageRequestOptions, StringTagPartProperty, TagPropertyMap } from '@gentics/cms-models';
import { GenticsUICoreModule, OverlayHostService, SizeTrackerService } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { StartPageIcon } from '../../../shared/components/start-page-icon/start-page-icon.component';
import { PageIsLockedPipe } from '../../../shared/pipes/page-is-locked/page-is-locked.pipe';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { FolderStartPageComponent } from './folder-start-page.component';

const FOLDER_WITH_INTERNAL_START_PAGE: Folder = {
    id: 1,
    globalId: 'item1',
    type: 'folder',
    nodeId: 1,
    startPageId: 1,
    tags: {
        'object.startpage': {
            id: 2,
            constructId: 2,
            name: 'object.startpage',
            active: true,
            type: 'OBJECTTAG',
            properties: {
                url: {
                    id: 3,
                    partId: 2,
                    type: 'PAGE',
                    pageId: 1,
                },
            } as Partial<TagPropertyMap>,
        },
    },
} as any;

const FOLDER_WITH_EXTERNAL_START_PAGE: Folder = {
    id: 1,
    globalId: 'item1',
    type: 'folder',
    nodeId: 1,
    tags: {
        'object.startpage': {
            id: 2,
            constructId: 2,
            name: 'object.startpage',
            active: true,
            type: 'OBJECTTAG',
            properties: {
                url: {
                    id: 3,
                    partId: 2,
                    type: 'PAGE',
                    stringValue: 'https://gentics.com',
                },
            } as Partial<TagPropertyMap>,
        },
    },
} as any;

const FOLDER_WITHOUT_START_PAGE: Folder = {
    id: 1,
    globalId: 'item1',
    type: 'folder',
    nodeId: 1,
    tags: {
        'object.startpage': {
            id: 2,
            constructId: 2,
            name: 'object.startpage',
            active: true,
            type: 'OBJECTTAG',
            properties: {
                url: {
                    id: 3,
                    partId: 2,
                    type: 'PAGE',
                    stringValue: '',
                },
            } as Partial<TagPropertyMap>,
        },
    },
} as any;

const PAGE: Page<Normalized> = {
    id: 1,
    globalId: 'item1',
    type: 'page',
    name: 'Page 1',
} as any;

class MockContextMenuOperationsService {}

describe('FolderStartPage', () => {

    let state: TestApplicationState;
    let navigationService: MockNavigationService;
    let folderActions: MockFolderActions;
    let entityResolver: MockEntityResolver;

    beforeEach(() => {
        navigationService = new MockNavigationService() as any as NavigationService;
        folderActions = new MockFolderActions() as any as FolderActionsService;
        entityResolver = new MockEntityResolver() as any as EntityResolver;

        configureComponentTest({
            imports: [GenticsUICoreModule],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: EntityResolver, useValue: entityResolver },
                { provide: FolderActionsService, useValue: folderActions },
                { provide: NavigationService, useValue: navigationService },
                { provide: ContextMenuOperationsService, useClass: MockContextMenuOperationsService },
                OverlayHostService,
                SizeTrackerService,
            ],
            declarations: [
                TestComponent,
                FolderStartPageComponent,
                StartPageIcon,
                PageIsLockedPipe,
            ],
        });

        state = TestBed.get(ApplicationStateService);
    });

    it('shows folder with internal start page',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.folder = FOLDER_WITH_INTERNAL_START_PAGE;

            folderActions.getItem = jasmine.createSpy('folderActions.getItem')
                .and.callFake((itemId, type) => {
                    if (type === 'folder') {
                        return Promise.resolve(FOLDER_WITH_INTERNAL_START_PAGE);
                    } else {
                        return Promise.resolve(PAGE);
                    }
                });

            entityResolver.getEntity = jasmine.createSpy('entityResolver.getEntity')
                .and.returnValue(PAGE);

            state.mockState({
                entities: {
                    page: [
                        PAGE,
                    ],
                },
                folder: { activeNode: 1, pages: { list: [] } },
            });

            fixture.detectChanges();
            tick();

            expect(instance.folderStartPage.folder).toBe(FOLDER_WITH_INTERNAL_START_PAGE);
            expect(instance.folderStartPage.getStartPageType).toBe('internal');
            expect(instance.folderStartPage.startPage$.value).toBe(PAGE);

            fixture.detectChanges();
            const folderStartPage: HTMLElement = fixture.nativeElement.querySelector('gtx-dropdown-trigger');
            expect(folderStartPage.innerText).toBe(`home${PAGE.name}`);
        }),
    );

    it('shows folder with external start page',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.folder = FOLDER_WITH_EXTERNAL_START_PAGE;

            folderActions.getItem = jasmine.createSpy('folderActions.getItem')
                .and.callFake((itemId, type) => {
                    if (type === 'folder') {
                        return Promise.resolve(FOLDER_WITH_EXTERNAL_START_PAGE);
                    } else {
                        return Promise.resolve(PAGE);
                    }
                });

            entityResolver.getEntity = jasmine.createSpy('entityResolver.getEntity')
                .and.returnValue(PAGE);

            state.mockState({
                entities: {
                    page: [
                        PAGE,
                    ],
                },
                folder: { activeNode: 1, pages: { list: [] } },
            });

            fixture.detectChanges();
            tick();

            const expectedText = (FOLDER_WITH_EXTERNAL_START_PAGE.tags['object.startpage'].properties.url as StringTagPartProperty).stringValue;

            expect(instance.folderStartPage.folder).toBe(FOLDER_WITH_EXTERNAL_START_PAGE);
            expect(instance.folderStartPage.getStartPageType).toBe('external');
            expect(instance.folderStartPage.startPage$.value).toBe(expectedText);

            fixture.detectChanges();
            const folderStartPage: HTMLElement = fixture.nativeElement.querySelector('gtx-dropdown-trigger');
            expect(folderStartPage.innerText).toBe(`home${expectedText}`);
        }),
    );

    it('do not show start page when its not set',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.folder = FOLDER_WITHOUT_START_PAGE;

            folderActions.getItem = jasmine.createSpy('folderActions.getItem')
                .and.callFake((itemId, type) => {
                    if (type === 'folder') {
                        return Promise.resolve(FOLDER_WITHOUT_START_PAGE);
                    } else {
                        return Promise.resolve(PAGE);
                    }
                });

            entityResolver.getEntity = jasmine.createSpy('entityResolver.getEntity')
                .and.returnValue(PAGE);

            state.mockState({
                entities: {
                    page: [
                        PAGE,
                    ],
                },
                folder: { activeNode: 1, pages: { list: [] } },
            });

            fixture.detectChanges();
            tick();

            expect(instance.folderStartPage.folder).toBe(FOLDER_WITHOUT_START_PAGE);
            expect(instance.folderStartPage.getStartPageType).toBe('external');
            expect(instance.folderStartPage.startPage$.value).toBe(null);

            fixture.detectChanges();
            const folderStartPage: HTMLElement = fixture.nativeElement.querySelector('gtx-dropdown-trigger');
            expect(folderStartPage).toBe(null);
        }),
    );
});

@Component({
    template: `<folder-start-page [folder]="folder"></folder-start-page>
    <gtx-overlay-host></gtx-overlay-host>`,
})
class TestComponent {
    @ViewChild(FolderStartPageComponent, { static: true })
    folderStartPage: FolderStartPageComponent;

    folder: Partial<Folder> = {};
}

class MockEntityResolver {
    // eslint-disable-next-line @typescript-eslint/naming-convention
    getEntity(entityType: keyof EntityState, id: number): any {
        throw new Error('getEntity called but not mocked');
    }
}
class MockNavigationService {}
class MockFolderActions {
    getItem(itemId: number, type: 'folder', options?: FolderRequestOptions): Promise<Folder>;
    getItem(itemId: number, type: 'page', options?: PageRequestOptions): Promise<Page>;
    getItem(): Promise<any> {
        throw new Error('getItem called but not mocked');
    }
}
