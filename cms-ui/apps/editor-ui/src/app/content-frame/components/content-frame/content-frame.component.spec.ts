/* eslint-disable no-underscore-dangle */
import {
    ChangeDetectorRef,
    Component,
    DebugElement,
    Directive,
    EventEmitter,
    Input,
    Output,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, Params } from '@angular/router';
import { EditMode } from '@gentics/cms-integration-api-models';
import { FolderListResponse, Form, ItemWithObjectTags, Language, Node, Page } from '@gentics/cms-models';
import {
    getExampleFolderData,
    getExampleFormData,
    getExampleFormDataNormalized,
    getExampleLanguageData,
    getExampleNodeDataNormalized,
    getExamplePageData,
    getExamplePageDataNormalized,
} from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { BehaviorSubject, NEVER, Observable, of as observableOf } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { mockPipes } from '../../../../testing/mock-pipe';
import { Api } from '../../../core/providers/api/api.service';
import { DecisionModalsService } from '../../../core/providers/decision-modals/decision-modals.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { ResourceUrlBuilder } from '../../../core/providers/resource-url-builder/resource-url-builder';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';
import { EditorOverlayService } from '../../../editor-overlay/providers/editor-overlay.service';
import { FolderPropertiesComponent } from '../../../shared/components/folder-properties/folder-properties.component';
import { InheritedLocalizedIcon } from '../../../shared/components/inherited-localized-icon/inherited-localized-icon.component';
import { ItemStatusLabelComponent } from '../../../shared/components/item-status-label/item-status-label.component';
import { DynamicDisableDirective } from '../../../shared/directives/dynamic-disable/dynamic-disable.directive';
import { ItemIsLocalizedPipe } from '../../../shared/pipes/item-is-localized/item-is-localized.pipe';
import { BreadcrumbsService } from '../../../shared/providers/breadcrumbs.service';
import { ApplicationStateService, EditorActionsService, FolderActionsService, MarkContentAsModifiedAction } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { TagEditorService } from '../../../tag-editor';
import { CustomScriptHostService } from '../../providers/custom-script-host/custom-script-host.service';
import { CustomerScriptService } from '../../providers/customer-script/customer-script.service';
import { CombinedPropertiesEditorComponent } from '../combined-properties-editor/combined-properties-editor.component';
import { NodePropertiesComponent } from '../node-properties/node-properties.component';
import { ContentFrameComponent } from './content-frame.component';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';

let appState: TestApplicationState;
let permissionService: MockPermissionService;
let route: MockActivatedRoute;
let folderActions: MockFolderActions;

const ITEM_ID = 1;
const PARENTFOLDER_ID = 2;
const ITEM_NODE = 11;
const MOCK_NODE_NAME = 'MockNode';
const CURRENT_USER_ID = 1;
const OTHER_USER_ID = 2;

class MockApi {
    folders = {
        getBreadcrumbs: (): Observable<Partial<FolderListResponse>> => observableOf<Partial<FolderListResponse>>({
            folders: [],
        }),
        cancelEditing: (): Observable<Partial<Response>> => observableOf<Partial<Response>>({
        }),
    };
}
class MockActivatedRoute implements Partial<ActivatedRoute> {
    public paramSub = new BehaviorSubject<Params>(undefined);

    params: Observable<Params> = this.paramSub.asObservable();

    public clear(): void {
        this.paramSub.next(undefined);
    }
}

class MockCanSaveService {
    getCanSave = jasmine.createSpy('getCanSave').and.returnValue(true);
}
class MockNavigationService implements Partial<NavigationService> {
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

    deserializeOptions<T>(options: string): T {
        try {
            return JSON.parse(options || '');
        } catch (_err) {
            return {} as any;
        }
    }

    serializeOptions(options: any): { options: string; } {
        return { options: JSON.stringify(options) };
    }
}
class MockModalService {}
class MockErrorHandler {}
class MockDecisionModalsService {}
class MockChangeDetector {
    detectChanges = jasmine.createSpy('detectChanges');
    markForCheck = jasmine.createSpy('markForCheck');
}

@Directive({ selector: '[overrideSlot],[overrideParams]' })
class MockOverrideSlotDirective {}

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
class MockI18nService {
    translate(key: string): string {
        return key;
    }
}

class MockI18nNotification {}
class MockUserSettingsService {}

class MockCustomScriptHostService {
    initialize(): void {}
}

class MockCustomerScriptService {
    createGCMSUIObject(): void {}
}

class MockEditorOverlayService {}
class MockRepositoryBrowserClient {}

@Component({
    selector: 'test-component',
    template: `<content-frame></content-frame>
        <gtx-overlay-host></gtx-overlay-host>
    `,
})
class TestComponent {
    @ViewChild(ContentFrameComponent, { static: true }) contentFrame: ContentFrameComponent;

    // If we want to access the services provided directly by ContentFrame, we need the injector of the ViewContainerRef.
    @ViewChild(ContentFrameComponent, { static: true, read: ViewContainerRef })
    contentFrameViewContainerRef: ViewContainerRef;
}

@Component({
    selector: 'file-preview',
    template: '',
})
class MockFilePreview {
    @Input() file: any;
    @Output() imageLoading = new EventEmitter<boolean>();
}

@Component({
    selector: 'combined-properties-editor',
    template: '',
    providers: [
        {
            provide: CombinedPropertiesEditorComponent,
            useClass: MockCombinedPropertiesEditor,
        },
    ],
})
class MockCombinedPropertiesEditor {
    @Input() item: ItemWithObjectTags | Node;
    @Input() isDisabled: boolean;

    constructor(private canSaveService: MockCanSaveService) { }

    saveChanges = jasmine.createSpy('saveChanges').and.returnValue(Promise.resolve());
    get canSave(): boolean {
        return this.canSaveService.getCanSave();
    }
}

@Component({
    selector: 'gtx-form-editor',
    template: '',
})
class MockFormEditor {
    @Input() item: Form;
    @Input() isDisabled = true;
}

@Component({
    selector: 'item-state-contextmenu',
    template: '',
})
class MockPageStateContextMenu {
    @Input() nodeLanguages: Language[];
    @Input() page: Page;
    @Input() activeNodeId: number;
}

@Component({
    selector: 'tag-editor-overlay-host',
    template: '',
})
class MockTagEditorOverlayHost {}

class MockTagEditorService {
    forceCloseTagEditor(): void {}
}

const findCombinedPropEditor = (fixture: ComponentFixture<TestComponent>): DebugElement =>
    fixture.debugElement.query(By.directive(MockCombinedPropertiesEditor));

function openPropertiesOfAFolder(fixture: ComponentFixture<any>, editorIsOpen: boolean = true): void {
    appState.mockState({
        entities: {
            folder: {
                [ITEM_ID]: {
                    type: 'folder',
                    id: ITEM_ID,
                    motherId: PARENTFOLDER_ID,
                    nodeId: ITEM_NODE,
                    path: '/',
                },
            },
            node: {
                [ITEM_NODE]: getExampleNodeDataNormalized({id: ITEM_NODE}),
            },
            language: getExampleLanguageData(),
        },
        folder: {
            activeNode: ITEM_NODE,
            activeNodeLanguages: {
                list: [ 1, 2 ],
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
            templates: {
                saving: false,
            },
        },
        ui: {
            contentFrameBreadcrumbsExpanded: true,
        },
    });

    folderActions.getItem.and.returnValue(Promise.resolve(getExampleFolderData({ id: ITEM_ID })));

    route.paramSub.next({
        nodeId: ITEM_NODE,
        type: 'folder',
        itemId: ITEM_ID,
        editMode: EditMode.EDIT_PROPERTIES,
        options: JSON.stringify({
            openTab: 'properties',
        }),
    });

    fixture.detectChanges();
    tick();
}

function openPropertiesOfAPage(fixture: ComponentFixture<any>, pageId: number = ITEM_ID, openPropertiesTab: string = undefined): void {
    appState.mockState({
        entities: {
            node: {
                [ITEM_NODE]: getExampleNodeDataNormalized({id: ITEM_NODE}),
            },
            page: {
                [pageId]: getExamplePageDataNormalized({ id: pageId }),
            },
            language: getExampleLanguageData(),
        },
        folder: {
            activeNode: ITEM_NODE,
            activeNodeLanguages: {
                list: [ 1 ],
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
            templates: {
                saving: false,
            },
        },
        ui: {
            contentFrameBreadcrumbsExpanded: true,
        },
    });

    folderActions.getItem.and.returnValue(Promise.resolve(getExamplePageData({ id: pageId })));

    route.paramSub.next({
        nodeId: ITEM_NODE,
        type: 'page',
        itemId: pageId,
        editMode: EditMode.EDIT_PROPERTIES,
        options: JSON.stringify({
            openTab: 'properties',
            propertiesTab: openPropertiesTab,
        }),
    });

    fixture.detectChanges();
    tick();
}

function openEditModeOfAPage(fixture: ComponentFixture<any>, pageId: number = ITEM_ID): void {
    appState.mockState({
        entities: {
            node: {
                [ITEM_NODE]: getExampleNodeDataNormalized({id: ITEM_NODE}),
            },
            page: {
                [pageId]: getExamplePageDataNormalized({ id: pageId, userId: 3 }),
            },
            language: getExampleLanguageData(),
        },
        folder: {
            activeNode: ITEM_NODE,
            activeNodeLanguages: {
                list: [ 1 ],
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
            templates: {
                saving: false,
            },
        },
        ui: {
            contentFrameBreadcrumbsExpanded: true,
        },
    });

    folderActions.getItem.and.returnValue(Promise.resolve(getExamplePageData({ id: pageId })));

    route.paramSub.next({
        nodeId: ITEM_NODE,
        type: 'page',
        itemId: pageId,
        editMode: EditMode.EDIT,
    });

    fixture.detectChanges();
    tick();
}

function openEditModeOfAForm(fixture: ComponentFixture<any>, formId: number = ITEM_ID): void {
    appState.mockState({
        entities: {
            node: {
                [ITEM_NODE]: getExampleNodeDataNormalized({id: ITEM_NODE}),
            },
            form: {
                [formId]: getExampleFormDataNormalized({ id: formId }),
            },
            language: getExampleLanguageData(),
        },
        folder: {
            activeNode: ITEM_NODE,
            activeNodeLanguages: {
                list: [ 1 ],
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
            templates: {
                saving: false,
            },
        },
        ui: {
            contentFrameBreadcrumbsExpanded: true,
        },
    });

    folderActions.getItem.and.returnValue(Promise.resolve(getExampleFormData({ id: formId })));

    route.paramSub.next({
        nodeId: ITEM_NODE,
        type: 'form',
        itemId: formId,
        editMode: EditMode.EDIT,
    });

    fixture.detectChanges();
    tick();
}

describe('ContentFrameComponent', () => {
    let api: MockApi;

    let origDebounce: any;

    let canSaveService: MockCanSaveService;

    beforeEach(() => {
        permissionService = new MockPermissionService();

        configureComponentTest({
            providers: [
                BreadcrumbsService,
                EntityResolver,
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: Api, useClass: MockApi },
                { provide: ActivatedRoute, useClass: MockActivatedRoute },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: ModalService, useClass: MockModalService },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: DecisionModalsService, useClass: MockDecisionModalsService },
                { provide: ChangeDetectorRef, useClass: MockChangeDetector },
                EditorActionsService,
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: PermissionService, useValue: permissionService },
                { provide: I18nService, useClass: MockI18nService },
                { provide: I18nNotification, useClass: MockI18nNotification },
                { provide: UserSettingsService, useClass: MockUserSettingsService },
                { provide: CustomScriptHostService, useClass: MockCustomScriptHostService },
                { provide: CustomerScriptService, useClass: MockCustomerScriptService },
                { provide: EditorOverlayService, useClass: MockEditorOverlayService },
                { provide: TagEditorService, useClass: MockTagEditorService },
                { provide: RepositoryBrowserClient, useClass: MockRepositoryBrowserClient },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                MockCanSaveService,
                ResourceUrlBuilder,
            ],
            declarations: [
                ContentFrameComponent,
                DynamicDisableDirective,
                FolderPropertiesComponent,
                InheritedLocalizedIcon,
                ItemIsLocalizedPipe,
                MockCombinedPropertiesEditor,
                MockFormEditor,
                MockFilePreview,
                MockOverrideSlotDirective,
                MockPageStateContextMenu,
                MockTagEditorOverlayHost,
                NodePropertiesComponent,
                ItemStatusLabelComponent,
                TestComponent,
                mockPipes('i18n', 'i18nDate', 'filesize', 'replaceEscapedCharacters'),
            ],
            imports: [
                GenticsUICoreModule.forRoot(),
                FormsModule,
                ReactiveFormsModule,
            ],
        });

        appState = TestBed.get(ApplicationStateService);
        api = TestBed.get(Api);
        folderActions = TestBed.get(FolderActionsService);
        canSaveService = TestBed.get(MockCanSaveService);
        route = TestBed.inject(ActivatedRoute) as any;

        // Make sure we're marking the test as logged in, otherwise nothing will be displayed/rendered
        appState.mockState({
            auth: {
                isLoggedIn: true,
                // Not really used anywhere
                currentUserId: CURRENT_USER_ID,
            },
        });

        route.clear();

        // We need to mock the lodash debounce function, otherwise we will get
        // an error about a pending timer in the queue (in newer Angular versions zone.js supports using tick for lodash).
        origDebounce = ContentFrameComponent._debounce;
        ContentFrameComponent._debounce = ((fn: any) => fn) as any;
    });

    afterEach(() => {
        ContentFrameComponent._debounce = origDebounce;
    });

    it('CombinedPropertiesEditor is created and item is set after the state was updated with route params',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            const folderId = 4;
            const expected: Partial<FolderListResponse> = {
                folders: [{
                    name: 'folderA',
                    id: folderId,
                }, {
                    name: 'folderB',
                    id: folderId + 1,
                }] as any,
            };
            spyOn(api.folders, 'getBreadcrumbs').and.returnValue(observableOf(expected));

            openPropertiesOfAFolder(fixture, false);
            let propEditor = findCombinedPropEditor(fixture);
            expect(propEditor).toBeFalsy();

            // Set editorIsOpen to true to indicate that the state has been updated with the route params.
            openPropertiesOfAFolder(fixture, true);
            propEditor = findCombinedPropEditor(fixture);
            expect(propEditor).toBeTruthy();
            expect(propEditor.componentInstance.item).toBeTruthy();
        }),
    );

    describe('page editing', () => {

        describe('editorActions.cancelEditing()', () => {

            beforeEach(() => {
                folderActions.getItem.and.returnValue(Promise.resolve(getExamplePageData({ id: ITEM_ID })));
            });

            it('is called when closing the editor', componentTest(() => TestComponent, (fixture, instance) => {
                openPropertiesOfAPage(fixture);
                tick();
                fixture.detectChanges();

                fixture.destroy();
                tick();

                const state = appState.now;
                const editorState = state.editor;
                const page = state.entities.page[ITEM_ID];

                expect(editorState.contentModified).toBeFalse();
                expect(editorState.objectPropertiesModified).toBeFalse();

                expect(page.locked).toBeFalse();
                expect(page.lockedBy).toBeNull();
                expect(page.lockedSince).toEqual(-1);
            }));

            it('is called when opening another page', componentTest(() => TestComponent, (fixture, instance) => {
                openPropertiesOfAPage(fixture);
                tick();
                fixture.detectChanges();

                openPropertiesOfAPage(fixture, ITEM_ID + 1);
                tick();

                const state = appState.now;
                const editorState = state.editor;
                const page = state.entities.page[ITEM_ID];

                expect(editorState.contentModified).toBeFalse();
                expect(editorState.objectPropertiesModified).toBeFalse();

                expect(page.locked).toBeFalse();
                expect(page.lockedBy).toBeNull();
                expect(page.lockedSince).toEqual(-1);
            }));

        });
    });

    describe('save button', () => {
        it('is enabled when item properties are edited but have not been modified yet', componentTest(() => TestComponent, (fixture, instance) => {
            openPropertiesOfAPage(fixture, ITEM_ID, 'item-properties');
            const currentState = appState.now;
            currentState.editor.contentModified = false;
            currentState.editor.modifiedObjectPropertiesValid = true;
            appState.mockState(currentState);
            tick();
            fixture.detectChanges();

            expect(instance.contentFrame.determineSaveButtonIsDisabled()).toEqual(false);
        }));

        it('is enabled when item properties are edited and are valid and have been modified', componentTest(() => TestComponent, (fixture, instance) => {
            openPropertiesOfAPage(fixture, ITEM_ID, 'item-properties');
            const currentState = appState.now;
            currentState.editor.contentModified = true;
            currentState.editor.modifiedObjectPropertiesValid = true;
            appState.mockState(currentState);
            tick();
            fixture.detectChanges();

            expect(instance.contentFrame.determineSaveButtonIsDisabled()).toEqual(false);
        }));

        it('is disabled when object properties are edited but are invalid', componentTest(() => TestComponent, (fixture, instance) => {
            openPropertiesOfAPage(fixture, ITEM_ID, 'object.fullpagemode');
            const currentState = appState.now;
            currentState.editor.contentModified = true;
            currentState.editor.modifiedObjectPropertiesValid = false;
            appState.mockState(currentState);
            tick();
            fixture.detectChanges();

            expect(instance.contentFrame.determineSaveButtonIsDisabled()).toEqual(true);
        }));

        it(
            'is enabled when properties are edited and valid despite the user has no permissions to save',
            componentTest(() => TestComponent, (fixture, instance) => {
                openPropertiesOfAPage(fixture, ITEM_ID, 'item-properties');
                canSaveService.getCanSave.and.returnValue(false);

                // set contentModified to true, so it cannot be the reason for disabling the save button
                const currentState = appState.now;
                currentState.editor.contentModified = true;
                currentState.editor.modifiedObjectPropertiesValid = true;
                appState.mockState(currentState);

                tick();
                fixture.detectChanges();

                expect(instance.contentFrame.determineSaveButtonIsDisabled()).toEqual(false);
            }),
        );

        it('is enabled when properties are edited and valid and the user is allowed to save', componentTest(() => TestComponent, (fixture, instance) => {
            openPropertiesOfAPage(fixture, ITEM_ID, 'item-properties');
            canSaveService.getCanSave.and.returnValue(true);

            // set contentModified to true, so it cannot be the reason for disabling the save button
            const currentState = appState.now;
            currentState.editor.contentModified = true;
            currentState.editor.modifiedObjectPropertiesValid = true;
            appState.mockState(currentState);

            tick();
            fixture.detectChanges();

            expect(instance.contentFrame.determineSaveButtonIsDisabled()).toEqual(false);
        }));

        it('is enabled despite a form item has not been modified yet', componentTest(() => TestComponent, (fixture, instance) => {
            openEditModeOfAForm(fixture, ITEM_ID);
            const currentState = appState.now;
            currentState.editor.contentModified = false;
            currentState.editor.modifiedObjectPropertiesValid = true;
            instance.contentFrame.setItemValidity(true);
            appState.mockState(currentState);
            tick();
            fixture.detectChanges();

            expect(instance.contentFrame.determineSaveButtonIsDisabled()).toEqual(false);
        }));

        it('is disabled when a form item is edited but not valid', componentTest(() => TestComponent, (fixture, instance) => {
            openEditModeOfAForm(fixture, ITEM_ID);
            const currentState = appState.now;
            currentState.editor.contentModified = true;
            instance.contentFrame.setItemValidity(false);
            appState.mockState(currentState);
            tick();
            fixture.detectChanges();

            expect(instance.contentFrame.determineSaveButtonIsDisabled()).toEqual(true);
        }));

        it('is enabled when a form item is edited, has not been modified and is valid', componentTest(() => TestComponent, (fixture, instance) => {
            openEditModeOfAForm(fixture, ITEM_ID);
            const currentState = appState.now;
            currentState.editor.contentModified = false;
            currentState.editor.modifiedObjectPropertiesValid = true;
            instance.contentFrame.setItemValidity(true);
            appState.mockState(currentState);
            tick();
            fixture.detectChanges();

            expect(instance.contentFrame.determineSaveButtonIsDisabled()).toEqual(false);
        }));

        it(
            'is disabled when a non-form item is edited but neither aloha editor is ready nor master frame is loaded',
            componentTest(() => TestComponent, (fixture, instance) => {
                openEditModeOfAPage(fixture, ITEM_ID);
                instance.contentFrame.alohaReady = false;
                instance.contentFrame.setMasterFrameLoaded(false);
                const currentState = appState.now;
                currentState.editor.contentModified = true;
                currentState.entities.page[ITEM_ID].locked = false;
                appState.mockState(currentState);
                tick();
                fixture.detectChanges();

                expect(instance.contentFrame.determineSaveButtonIsDisabled()).toEqual(true);
            }),
        );

        it('is enabled when a non-form item is edited despite it has not been modified yet', componentTest(() => TestComponent, (fixture, instance) => {
            openEditModeOfAPage(fixture, ITEM_ID);
            instance.contentFrame.alohaReady = true;
            instance.contentFrame.setMasterFrameLoaded(true);
            const currentState = appState.now;
            currentState.editor.contentModified = false;
            currentState.entities.page[ITEM_ID].locked = false;
            appState.mockState(currentState);
            tick();
            fixture.detectChanges();

            expect(instance.contentFrame.determineSaveButtonIsDisabled()).toEqual(false);

        }));

        it('is disabled when a non-form item is edited but it is locked by another user', componentTest(() => TestComponent, (fixture, instance) => {
            openEditModeOfAPage(fixture, ITEM_ID);
            instance.contentFrame.alohaReady = true;
            instance.contentFrame.setMasterFrameLoaded(true);
            const currentState = appState.now;
            currentState.editor.contentModified = true;
            currentState.entities.page[ITEM_ID].locked = true;
            // CurrentUserId is 1, so use another one
            currentState.entities.page[ITEM_ID].lockedBy = 2;
            appState.mockState(currentState);
            tick();
            fixture.detectChanges();

            expect(instance.contentFrame.determineSaveButtonIsDisabled()).toEqual(true);

        }));


        it('is enabled when a non-form item is edited and aloha editor is ready as well as master frame is loaded'
             + ', content has been modified and it is not locked by another user', componentTest(() => TestComponent, (fixture, instance) => {
            openEditModeOfAPage(fixture, ITEM_ID);
            instance.contentFrame.alohaReady = true;
            instance.contentFrame.setMasterFrameLoaded(true);
            const currentState = appState.now;
            currentState.editor.contentModified = true;
            currentState.entities.page[ITEM_ID].locked = false;
            appState.mockState(currentState);
            tick();
            fixture.detectChanges();

            expect(instance.contentFrame.determineSaveButtonIsDisabled()).toEqual(false);
        }))
    });
});
