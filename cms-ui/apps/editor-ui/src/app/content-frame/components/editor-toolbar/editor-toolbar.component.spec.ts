import { Directive, ErrorHandler } from '@angular/core';
import { ComponentFixture, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ActivatedRoute } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { Api, GcmsApi } from '@editor-ui/app/core/providers/api';
import { DecisionModalsService } from '@editor-ui/app/core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { I18nNotification } from '@editor-ui/app/core/providers/i18n-notification/i18n-notification.service';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { PermissionService } from '@editor-ui/app/core/providers/permissions/permission.service';
import { ResourceUrlBuilder } from '@editor-ui/app/core/providers/resource-url-builder/resource-url-builder';
import { UserSettingsService } from '@editor-ui/app/core/providers/user-settings/user-settings.service';
import { EditorOverlayService } from '@editor-ui/app/editor-overlay/providers/editor-overlay.service';
import { InheritedLocalizedIcon, ItemStatusLabelComponent } from '@editor-ui/app/shared/components';
import { ItemIsLocalizedPipe } from '@editor-ui/app/shared/pipes';
import { BreadcrumbsService } from '@editor-ui/app/shared/providers';
import { ApplicationStateService, EditorActionsService, FolderActionsService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { componentTest, configureComponentTest } from '@editor-ui/testing';
import { mockPipes } from '@editor-ui/testing/mock-pipe';
import { EditMode } from '@gentics/cms-integration-api-models';
import { Folder, FolderListResponse, Form, FormPermissions, Node, Page, PagePermissions } from '@gentics/cms-models';
import { getExampleFormDataNormalized, getExamplePageDataNormalized } from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { NEVER, Observable, of } from 'rxjs';
import { EditorToolbarComponent } from './editor-toolbar.component';

const ITEM_ID = 1;
const PARENTFOLDER_ID = 2;
const ITEM_NODE = 11;
const MOCK_NODE_NAME = 'MockNode';

let appState: TestApplicationState;

describe('EditorToolbarComponent', () => {

    beforeEach(() => {
        const testBed = configureComponentTest({
            providers: [
                BreadcrumbsService,
                EntityResolver,
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: Api, useClass: MockClient },
                { provide: GcmsApi, useClass: MockClient },
                { provide: GCMSRestClientService, useClass: MockClient },
                { provide: ActivatedRoute, useClass: MockActivatedRoute },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: ModalService, useClass: MockModalService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: DecisionModalsService, useClass: MockDecisionModalsService },
                { provide: EditorActionsService, useClass: MockEditorActions },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: I18nService, useClass: MockI18nService },
                { provide: I18nNotification, useClass: MockI18nNotification },
                { provide: UserSettingsService, useClass: MockUserSettingsService },
                { provide: EditorOverlayService, useClass: MockEditorOverlayService },
                { provide: PermissionService, useClass: MockPermissionService },
                MockCanSaveService,
                ResourceUrlBuilder,
            ],
            declarations: [
                InheritedLocalizedIcon,
                ItemIsLocalizedPipe,
                MockOverrideSlotDirective,
                ItemStatusLabelComponent,
                EditorToolbarComponent,
                mockPipes('i18n', 'i18nDate', 'filesize', 'replaceEscapedCharacters'),
            ],
            imports: [
                GenticsUICoreModule.forRoot(),
                FormsModule,
                ReactiveFormsModule,
                RouterTestingModule.withRoutes([]),
            ],
        });

        appState = testBed.inject(ApplicationStateService) as any;
    });

    describe('context menu', () => {

        it('is visible if edited item is a page',
            componentTest(() => EditorToolbarComponent, (fixture, instance) => {
                openPropertiesOfAPage(instance);
                instance.itemPermissions = {
                    create: true,
                    delete: true,
                    edit: true,
                    inherit: true,
                    localize: true,
                    publish: true,
                    unlocalize: true,
                    view: true,
                } as PagePermissions;
                instance.currentNode = {
                    id: 1,
                } as any;
                fixture.detectChanges();

                const visible = contextMenuIsVisible(fixture);
                expect(visible).toBe(true);
            }),
        );

        it('is not visible if edited item is a folder',
            componentTest(() => EditorToolbarComponent, (fixture, instance) => {
                openPropertiesOfAFolder(instance);

                const visible = contextMenuIsVisible(fixture);
                expect(visible).toBe(false);
            }),
        );

        it('is not visible if edited item is a folder and a navigation is performed',
            componentTest(() => EditorToolbarComponent, (fixture, instance) => {
                openPropertiesOfAFolder(instance);
                navigateToFolderOfItem(fixture);

                const visible = contextMenuIsVisible(fixture);
                expect(visible).toBe(false);
            }),
        );

        it('is not visible after opening the properties of a node',
            componentTest(() => EditorToolbarComponent, (fixture, instance) => {
                openPropertiesOfANode(instance);

                const visible = contextMenuIsVisible(fixture);
                expect(visible).toBe(false);
            }),
        );
    });

    describe('publish button in edit mode', () => {

        it('is visible for pages if the user has right permissions ', componentTest(() => EditorToolbarComponent, (fixture, instance) => {
            openEditModeOfAPage(instance);
            instance.itemPermissions = {
                create: false,
                delete: false,
                edit: true,
                inherit: false,
                localize: false,
                publish: true,
                unlocalize: false,
                view: false,
            } as PagePermissions;

            const buttons = instance.determineVisibleButtons();

            expect(buttons.publish).toEqual(true);
        }));

        it('is visible for pages if the user does not have right permissions ', componentTest(() => EditorToolbarComponent, (fixture, instance) => {
            openEditModeOfAPage(instance);
            instance.itemPermissions = {
                create: false,
                delete: false,
                edit: true,
                inherit: false,
                localize: false,
                publish: true,
                unlocalize: false,
                view: false,
            } as PagePermissions;

            const buttons = instance.determineVisibleButtons();

            expect(buttons.publish).toEqual(true);
        }));

        it('is visible for forms if the user does have right permissions ', componentTest(() => EditorToolbarComponent, (fixture, instance) => {
            openEditModeOfAPage(instance);
            instance.itemPermissions = {
                create: false,
                delete: false,
                edit: true,
                inherit: false,
                localize: false,
                publish: true,
                unlocalize: false,
                view: false,
            } as PagePermissions;

            const buttons = instance.determineVisibleButtons();

            expect(buttons.publish).toEqual(true);
        }));

        it('is not visible for forms if the user does not have right permissions ', componentTest(() => EditorToolbarComponent, (fixture, instance) => {
            openEditModeOfAForm(instance);
            instance.itemPermissions = {
                create: false,
                delete: false,
                edit: true,
                inherit: false,
                localize: false,
                publish: false,
                unlocalize: false,
                view: false,
            } as FormPermissions;

            const buttons = instance.determineVisibleButtons();

            expect(buttons.publish).toEqual(false);
        }));
    });

    describe('takeOffline / timeManagement buttons in edit mode', () => {

        it('are visible for pages if the user has right permissions ', componentTest(() => EditorToolbarComponent, (fixture, instance) => {
            openEditModeOfAPage(instance);
            instance.itemPermissions = {
                create: false,
                delete: false,
                edit: true,
                inherit: false,
                localize: false,
                publish: true,
                unlocalize: false,
                view: false,
            } as PagePermissions;
            (instance.currentItem as Page).online = true;

            const buttons = instance.determineVisibleButtons();

            expect(buttons.takeOffline).toEqual(true);
            expect(buttons.timeManagement).toEqual(true);
        }));

        it('are visible for pages if the user does not have right permissions ', componentTest(() => EditorToolbarComponent, (fixture, instance) => {
            openEditModeOfAPage(instance);
            instance.itemPermissions = {
                create: false,
                delete: false,
                edit: true,
                inherit: false,
                localize: false,
                publish: false,
                unlocalize: false,
                view: false,
            } as PagePermissions;
            (instance.currentItem as Page).online = true;

            const buttons = instance.determineVisibleButtons();

            expect(buttons.takeOffline).toEqual(true);
            expect(buttons.timeManagement).toEqual(true);
        }));

        it('are visible for forms if the user does have right permissions ', componentTest(() => EditorToolbarComponent, (fixture, instance) => {
            openEditModeOfAForm(instance);
            instance.itemPermissions = {
                create: false,
                delete: false,
                edit: true,
                inherit: false,
                localize: false,
                publish: true,
                unlocalize: false,
                view: false,
            } as FormPermissions;
            (instance.currentItem as Form).online = true;

            const buttons = instance.determineVisibleButtons();

            expect(buttons.takeOffline).toEqual(true);
            expect(buttons.timeManagement).toEqual(true);
        }));

        it('are not visible for forms if the user does not have right permissions ', componentTest(() => EditorToolbarComponent, (fixture, instance) => {
            openEditModeOfAForm(instance);
            instance.itemPermissions = {
                create: false,
                delete: false,
                edit: true,
                inherit: false,
                localize: false,
                publish: false,
                unlocalize: false,
                view: false,
            } as FormPermissions;
            (instance.currentItem as Form).online = true;

            const buttons = instance.determineVisibleButtons();

            expect(buttons.takeOffline).toEqual(false);
            expect(buttons.timeManagement).toEqual(false);
        }));
    });
});

function openEditModeOfAPage(instance: EditorToolbarComponent, pageId: number = ITEM_ID): void {
    instance.currentItem = getExamplePageDataNormalized({ id: pageId, userId: 3 });
    instance.editorState = {
        ...appState.now.editor,
        editorIsOpen: true,
        editMode: EditMode.EDIT,
        itemId: pageId,
        itemType: 'page',
        nodeId: ITEM_NODE,
        saving: false,
    };
}

function openEditModeOfAForm(instance: EditorToolbarComponent, formId: number = ITEM_ID): void {
    instance.currentItem = getExampleFormDataNormalized({ id: formId });
    instance.editorState = {
        ...appState.now.editor,
        editorIsOpen: true,
        editMode: EditMode.EDIT,
        itemId: formId,
        itemType: 'form',
        nodeId: ITEM_NODE,
        saving: false,
    };
}

function openPropertiesOfAFolder(instance: EditorToolbarComponent, editorIsOpen: boolean = true, folderId: number = ITEM_ID): void {
    instance.currentItem = {
        type: 'folder',
        id: folderId,
        motherId: PARENTFOLDER_ID,
        nodeId: ITEM_NODE,
        path: '/',
    } as Folder;
    instance.editorState = {
        ...appState.now.editor,
        editorIsOpen: editorIsOpen,
        editMode: EditMode.EDIT_PROPERTIES,
        itemId: folderId,
        itemType: 'folder',
        nodeId: ITEM_NODE,
        openTab: 'properties',
        saving: false,
    };
}

function openPropertiesOfANode(instance: EditorToolbarComponent): void {
    instance.currentItem = {
        type: 'node',
        id: ITEM_NODE,
        folderId: PARENTFOLDER_ID,
    } as Node;
    instance.editorState = {
        ...appState.now.editor,
        editorIsOpen: true,
        editMode: EditMode.EDIT_PROPERTIES,
        itemId: ITEM_NODE,
        itemType: 'node',
        nodeId: ITEM_NODE,
        openTab: 'properties',
        saving: false,
    };
}

function openPropertiesOfAPage(instance: EditorToolbarComponent, pageId: number = ITEM_ID, openPropertiesTab: string = undefined): void {
    instance.currentItem = getExamplePageDataNormalized({ id: pageId });
    instance.editorState = {
        ...appState.now.editor,
        editorIsOpen: true,
        editMode: EditMode.EDIT_PROPERTIES,
        itemId: pageId,
        itemType: 'page',
        nodeId: ITEM_NODE,
        openTab: 'properties',
        openPropertiesTab: openPropertiesTab,
        saving: false,
    };
}

function navigateToFolderOfItem(fixture: ComponentFixture<any>): void {
    appState.mockState({
        folder: {
            activeFolder: PARENTFOLDER_ID,
            activeNode: ITEM_NODE,
            breadcrumbs: {
                list: [],
            },
            templates: {
                list: [],
            },
            activeNodeLanguages: {
                list: [],
            },
            folders: {
                saving: false,
            },
            pages: {
                saving: false,
            },
            files: {
                saving: false,
            },
            images: {
                saving: false,
            },
            forms: {
                saving: false,
            },
        },
    });
    fixture.detectChanges();
}

function contextMenuIsVisible(fixture: ComponentFixture<any>): boolean {
    tick();
    return !!fixture.debugElement.query(By.css('.content-frame-context-menu'));
}

class MockClient {
    folder = {
        breadcrumbs: (): Observable<Partial<FolderListResponse>> => of<Partial<FolderListResponse>>({
            folders: [],
        }),
    };
}
class MockActivatedRoute {}

class MockCanSaveService {
    getCanSave = jasmine.createSpy('getCanSave').and.returnValue(true);
}
class MockNavigationService {
    instruction(): any {
        return {
            navigate: () => {},
        };
    }

    detailOrModal(): any {
        return {
            commands: () => {},
        };
    }
}
class MockModalService {}
class MockErrorHandler {}
class MockDecisionModalsService {}

@Directive({ selector: '[overrideSlot],[overrideParams]' })
class MockOverrideSlotDirective {}

class MockPermissionService {
    forItemInLanguage(): Observable<any> {
        return NEVER;
    }
    forItem(): Observable<any> {
        return NEVER;
    }
    forFolder(): Observable<any> {
        return NEVER;
    }
}

class MockEditorActions {
    cancelEditing = jasmine.createSpy('cancelEditing');
    closeEditor = jasmine.createSpy('closeEditor');
}
class MockFolderActions {
    getItem = jasmine.createSpy('getItem').and.callFake(() => Promise.resolve(null));
    getNode(): Promise<Partial<Node>> {
        return Promise.resolve({ name: MOCK_NODE_NAME });
    }
}

class MockI18nService {
    translate(key: string): string {
        return key;
    }
}

class MockI18nNotification {}
class MockUserSettingsService {}

class MockEditorOverlayService {}
