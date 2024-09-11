import { Component, Input, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, flush, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { EditMode, TagChangedFn, TagEditorContext } from '@gentics/cms-integration-api-models';
import {
    EditableObjectTag,
    Feature,
    File,
    Folder,
    FolderSaveRequestOptions,
    Image,
    ItemWithObjectTags,
    Language,
    Node,
    NodeFeature,
    ObjectTag,
    OverviewTagPartProperty,
    Page,
    Raw,
    StringTagPartProperty,
    Tag,
    TagPartType,
    TagPropertyType,
    Tags,
    Template,
} from '@gentics/cms-models';
import {
    getExampleFileData,
    getExampleFolderData,
    getExampleImageData,
    getExampleLanguageData,
    getExampleNodeData,
    getExamplePageData,
} from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { BehaviorSubject, of as observableOf, of } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { mockPipes } from '../../../../testing/mock-pipe';
import { getExampleEditableTag, mockEditableObjectTag } from '../../../../testing/test-tag-editor-data.mock';
import { ITEM_PROPERTIES_TAB } from '../../../common/models';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { I18nNotification } from '../../../core/providers/i18n-notification/i18n-notification.service';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { ResourceUrlBuilder } from '../../../core/providers/resource-url-builder/resource-url-builder';
import { UserSettingsService } from '../../../core/providers/user-settings/user-settings.service';
import { SharedModule } from '../../../shared/shared.module';
import { ApplicationStateService, EditorActionsService, EditorStateUrlOptions, FolderActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { EditTagInfo, TagEditorHostComponent, TagEditorService } from '../../../tag-editor';
import { IFrameWrapperComponent } from '../../../tag-editor/components/iframe-wrapper/iframe-wrapper.component';
import { ObjectTagNamePipe } from '../../../tag-editor/pipes/object-tag-name/object-tag-name.pipe';
import { CustomScriptHostService } from '../../providers/custom-script-host/custom-script-host.service';
import { DescriptionTooltipComponent } from '../description-tooltip/description-tooltip.component';
import { NodePropertiesFormComponent } from '../node-properties/node-properties-form.component
import { PropertiesEditor } from '../properties-editor/properties-editor.component';
import { generateContentTagList } from '../../utils';
import { CombinedPropertiesEditorComponent } from './combined-properties-editor.component';

const CONTENT_TAG_NAME = 'contenttag0';
const TAG0_NAME = 'object.tag0';
const TAG1_NAME = 'object.tag1';
const TAG2_NAME = 'object.tag2';
const TAG_WITH_OVERVIEW = TAG2_NAME;
const OLD_TAGFILL_URL = 'tagfillUrl';

const TAB_SELECTOR = '.tab-link a';
const ITEM_PROPERTIES_EDITOR_SELECTOR = '.item-properties';
const OBJECT_PROPERTIES_EDITOR_SELECTOR = '.object-property';
const ACTIVE_CHECKBOX_DIV_SELECTOR = '.activate-obj-prop';

describe('CombinedPropertiesEditorComponent', () => {

    let mockNode: Node;
    let mockPage: Page;
    let mockFolder: Folder;
    let mockFile: File;
    let mockImage: Image;
    let mockObjProps: { [name: string]: EditableObjectTag };
    let mockObjPropsSorted: EditableObjectTag[];

    let entityResolver: MockEntityResolver;
    let folderActions: MockFolderActions;
    let state: TestApplicationState;
    let client: GCMSTestRestClientService;
    let validateTagSpy: jasmine.Spy;

    function applyObjectProperties(item: ItemWithObjectTags, objProps: { [name: string]: EditableObjectTag }): void {
        item.tags = cloneDeep(mockObjProps);
        Object.keys(objProps).forEach(tagName => {
            delete (item.tags[tagName] as EditableObjectTag).tagType;
        });
        item.tags[CONTENT_TAG_NAME] = getExampleEditableTag();
    }

    beforeEach(() => {
        validateTagSpy = null;
        mockNode = getExampleNodeData();
        mockPage = getExamplePageData();
        mockFolder = getExampleFolderData();
        mockFile = getExampleFileData();
        mockImage = getExampleImageData();
        mockObjProps = mockObjectProperties();
        mockObjPropsSorted = [
            mockObjProps[TAG1_NAME],
            mockObjProps[TAG2_NAME],
            mockObjProps[TAG0_NAME],
        ];
        applyObjectProperties(mockPage, mockObjProps);
        applyObjectProperties(mockFolder, mockObjProps);

        configureComponentTest({
            providers: [
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: CustomScriptHostService, useClass: MockCustomScriptHostService },
                { provide: EditorActionsService, useClass: MockEditorActions },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: EntityResolver, useClass: MockEntityResolver },
                { provide: I18nService, useClass: MockI18nService },
                { provide: I18nNotification, useClass: MockI18nNotification },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: PermissionService, useClass: MockPermissionService },
                { provide: ResourceUrlBuilder, useClass: MockResourceUrlBuilderService },
                { provide: TagEditorService, useClass: MockTagEditorService },
                { provide: UserSettingsService, useClass: MockUserSettingsService},
            ],
            declarations: [
                CombinedPropertiesEditorComponent,
                DescriptionTooltipComponent,
                MockIFrameWrapper,
                MockPropertiesEditor,
                MockTagEditorHost,
                MockTagEditorOverlayHost,
                NodePropertiesFormComponent,
                ObjectTagNamePipe,
                TestComponent,
                mockPipes('i18n', 'i18nDate', 'filesize'),
            ],
            imports: [
                GenticsUICoreModule.forRoot(),
                FormsModule,
                ReactiveFormsModule,
                SharedModule,
            ],
        });

        entityResolver = TestBed.inject(EntityResolver) as any;
        folderActions = TestBed.inject(FolderActionsService) as any;
        state = TestBed.inject(ApplicationStateService) as any;
        client = TestBed.inject(GCMSRestClientService) as any;

        state.mockState({
            editor: {
                editMode: EditMode.EDIT_PROPERTIES,
                itemId: mockPage.id,
                itemType: 'page',
                nodeId: mockNode.id,
                openPropertiesTab: ITEM_PROPERTIES_TAB,
                openObjectPropertyGroups: [],
                contentModified: false,
                objectPropertiesModified: false,
                modifiedObjectPropertiesValid: false,
            },
            features: {
                nodeFeatures: {
                    [mockNode.id]: [ NodeFeature.ASSET_MANAGEMENT ],
                },
                [Feature.TAGFILL_LIGHT]: true,
            },
            folder: {
                activeNode: mockNode.id,
                activeNodeLanguages: {
                    fetching: false,
                    list: mockNode.languagesId,
                    total: mockNode.languagesId.length,
                },
                activeFolder: mockPage.folderId,
                templates: {
                    fetching: false,
                    list: [ mockPage.templateId, mockPage.templateId + 1 ],
                    total: 2,
                },
            },
            entities: {
                language: getExampleLanguageData(),
            },

        });
    });

    describe('item properties', () => {

        it('loads additional item properties data and displays the item\'s properties',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                fixture.detectChanges();
                testComponent.item = mockPage;
                multiDetectChanges(fixture, 3);

                const expectedLanguages = state.now.folder.activeNodeLanguages.list.map(id => mockLanguage(id));
                const expectedTemplates = state.now.folder.templates.list.map(id => mockTemplate(id));

                expect(folderActions.getFolder).toHaveBeenCalledWith(mockPage.folderId, { construct: true });
                expect(entityResolver.getLanguage).toHaveBeenCalledTimes(mockNode.languagesId.length);
                expect(entityResolver.getTemplate).toHaveBeenCalledTimes(state.now.folder.templates.total);

                expect(fixture.debugElement.query(By.css(ITEM_PROPERTIES_EDITOR_SELECTOR))).toBeTruthy();
                const propEditor = fixture.debugElement.query(By.directive(MockPropertiesEditor));
                expect(propEditor).toBeTruthy();
                expect(fixture.debugElement.query(By.css(OBJECT_PROPERTIES_EDITOR_SELECTOR))).toBeFalsy();
                const propEditorComponent = propEditor.componentInstance as MockPropertiesEditor;
                expect(propEditorComponent.item).toEqual(mockPage);
                expect(propEditorComponent.languages).toEqual(expectedLanguages);
                expect(propEditorComponent.templates).toEqual(expectedTemplates);
            }),
        );

        it('saving of item properties works',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                fixture.detectChanges();
                state.mockState({
                    editor: {
                        ...state.now.editor,
                        contentModified: true,
                    },
                });
                testComponent.item = mockPage;
                multiDetectChanges(fixture, 3);

                const changes = { change: 'some change' };
                testComponent.combinedPropertiesEditor.handlePropChanges(changes as any);

                let promiseResolved = false;
                testComponent.combinedPropertiesEditor.saveChanges()
                    .then(() => {
                        expect(folderActions.updatePageProperties).toHaveBeenCalledWith(mockPage.id, changes, {
                            showNotification: true,
                            fetchForUpdate: true,
                            fetchForConstruct: true,
                        });
                        promiseResolved = true;
                    })
                    .catch(() => fail('The updatePageProperties() promise should not be rejected.'));
                tick();
                expect(promiseResolved).toBe(true);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    contentModified: false,
                    objectPropertiesModified: false,
                }));
            }),
        );

    });

    describe('object properties', () => {

        it('displays one tab for the item properties and one tab for each object property',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                testComponent.item = mockPage;
                multiDetectChanges(fixture, 2);

                // Check if the correct sequence of tabs is displayed (one for the item properties,
                // one for the tag list and one for each object property in the right order).
                const tabs = fixture.debugElement.queryAll(By.css(TAB_SELECTOR));
                expect(tabs.length).toBe(mockObjPropsSorted.length + 2); // +2 because ITEM_PROPERTIES_TAB and ITEM_TAG_LIST_TAB are also displayed
                expect((tabs[0].nativeElement as HTMLElement).innerText.toLowerCase()).toEqual('editor.general_properties_label');
                expect((tabs[1].nativeElement as HTMLElement).innerText.toLowerCase()).toEqual('editor.tag_list_label');
                // 'code' and 'info' in this is the material icon name which is being rendered before.
                const expectedTabLabels = mockObjPropsSorted.map(tag => `info${tag.displayName}`);
                const actualTabLabels = tabs.slice(2).map(tab => tab.nativeElement.textContent.toLowerCase().trim());
                expect(actualTabLabels).toEqual(expectedTabLabels);
            }),
        );

        it('displays the new TagEditor for the selected object property',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                const navService = TestBed.get(NavigationService) as MockNavigationService;
                const permissionService = TestBed.get(PermissionService) as MockPermissionService;
                const expectedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                const expectedTagEditorContext = mockTagEditorContext({
                    readOnly: false,
                    node: entityResolver.getNode(),
                    tag: expectedObjProp,
                    tagType: expectedObjProp.tagType,
                    tagOwner: mockPage,
                    withDelete: false,
                });

                testComponent.item = mockPage;
                multiDetectChanges(fixture, 2);

                // Navigate to the last object property.
                const tabs = fixture.debugElement.queryAll(By.css(TAB_SELECTOR));
                tabs[tabs.length - 1].nativeElement.click();
                multiDetectChanges(fixture, 2);

                expect(navService.detailOrModal).toHaveBeenCalledWith(
                    mockNode.id, 'page', mockPage.id, 'editProperties', { openTab: 'properties', propertiesTab: expectedObjProp.name, readOnly: false },
                );
                expect(permissionService.forItem).toHaveBeenCalledWith(mockPage.id, 'page', state.now.editor.nodeId);

                // Make sure that the appropriate components exist.
                expect(fixture.debugElement.query(By.css(ITEM_PROPERTIES_EDITOR_SELECTOR))).toBeFalsy();
                expect(fixture.debugElement.query(By.css(OBJECT_PROPERTIES_EDITOR_SELECTOR))).toBeTruthy();
                expect(fixture.debugElement.query(By.directive(MockTagEditorHost))).toBeTruthy();
                expect(fixture.debugElement.query(By.directive(MockIFrameWrapper))).toBeFalsy();

                // Make sure that the TagEditor has been initialized correctly.
                expect(validateTagSpy).toHaveBeenCalledTimes(1);
                expect(validateTagSpy.calls.argsFor(0)[0]).toEqual(expectedObjProp.properties);
                const tagEditorHost: MockTagEditorHost = testComponent.combinedPropertiesEditor.tagEditorHostList.first as any;
                expect(tagEditorHost.editTagLive).toHaveBeenCalledTimes(1);
                const editTagLiveArgs = tagEditorHost.editTagLive.calls.argsFor(0);
                expect(editTagLiveArgs[0]).toEqual(expectedObjProp);
                expect(editTagLiveArgs[1]).toEqual(expectedTagEditorContext);
                expect(editTagLiveArgs[2] instanceof Function).toBeTruthy();

                // Make sure that the state has been updated correctly.
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: false,
                    modifiedObjectPropertiesValid: true,
                }));
            }),
        );

        it('displaying the new TagEditor in read-only mode for an item without edit permissions works',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                fixture.detectChanges();
                const permissionService = TestBed.get(PermissionService) as MockPermissionService;
                permissionService.forItem.and.returnValue(observableOf({ edit: false }));
                const expectedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                const expectedTagEditorContext = mockTagEditorContext({
                    readOnly: true,
                    node: entityResolver.getNode(),
                    tag: expectedObjProp,
                    tagType: expectedObjProp.tagType,
                    tagOwner: mockPage,
                    withDelete: false,
                });
                state.mockState({
                    editor: {
                        ...state.now.editor,
                        openTab: 'properties',
                        openPropertiesTab: expectedObjProp.name,
                    },
                });

                testComponent.item = mockPage;
                multiDetectChanges(fixture, 2);

                // Make sure that the TagEditor has been initialized correctly.
                expect(fixture.debugElement.query(By.directive(MockTagEditorHost))).toBeTruthy();
                expect(fixture.debugElement.query(By.directive(MockIFrameWrapper))).toBeFalsy();
                const tagEditorHost: MockTagEditorHost = testComponent.combinedPropertiesEditor.tagEditorHostList.first as any;
                expect(tagEditorHost.editTagLive).toHaveBeenCalledTimes(1);
                const editTagLiveArgs = tagEditorHost.editTagLive.calls.argsFor(0);
                expect(editTagLiveArgs[0]).toEqual(expectedObjProp);
                expect(editTagLiveArgs[1]).toEqual(expectedTagEditorContext);
                expect(editTagLiveArgs[2] instanceof Function).toBeTruthy();

                // Make sure that the state has been updated correctly.
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: false,
                    modifiedObjectPropertiesValid: false,
                }));
            }),
        );

        it('displaying the new TagEditor in read-only mode for a tag without edit permissions works',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                fixture.detectChanges();
                const expectedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                expectedObjProp.readOnly = true;
                (mockPage.tags[expectedObjProp.name] as ObjectTag).readOnly = true;
                const expectedTagEditorContext = mockTagEditorContext({
                    readOnly: true,
                    node: entityResolver.getNode(),
                    tag: expectedObjProp,
                    tagType: expectedObjProp.tagType,
                    tagOwner: mockPage,
                    withDelete: false,
                });
                state.mockState({
                    editor: {
                        ...state.now.editor,
                        openTab: 'properties',
                        openPropertiesTab: expectedObjProp.name,
                    },
                });

                testComponent.item = mockPage;
                multiDetectChanges(fixture, 2);

                // Make sure that the TagEditor has been initialized correctly.
                expect(fixture.debugElement.query(By.directive(MockTagEditorHost))).toBeTruthy();
                expect(fixture.debugElement.query(By.directive(MockIFrameWrapper))).toBeFalsy();
                const tagEditorHost: MockTagEditorHost = testComponent.combinedPropertiesEditor.tagEditorHostList.first as any;
                expect(tagEditorHost.editTagLive).toHaveBeenCalledTimes(1);
                const editTagLiveArgs = tagEditorHost.editTagLive.calls.argsFor(0);
                expect(editTagLiveArgs[0]).toEqual(expectedObjProp);
                expect(editTagLiveArgs[1]).toEqual(expectedTagEditorContext);
                expect(editTagLiveArgs[2] instanceof Function).toBeTruthy();

                // Make sure that the state has been updated correctly.
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: false,
                    modifiedObjectPropertiesValid: false,
                }));
            }),
        );

        it('state modifications according to the object property\'s validity work',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                fixture.detectChanges();
                const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                state.mockState({
                    editor: {
                        ...state.now.editor,
                        openTab: 'properties',
                        openPropertiesTab: editedObjProp.name,
                    },
                });

                testComponent.item = mockPage;
                multiDetectChanges(fixture, 2);

                const tagEditorHost: MockTagEditorHost = testComponent.combinedPropertiesEditor.tagEditorHostList.first as any;
                expect(tagEditorHost.editTagLive.calls.argsFor(0)[0]).toEqual(editedObjProp);
                const onTagChangeFn: TagChangedFn = tagEditorHost.editTagLive.calls.argsFor(0)[2];
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: false,
                    modifiedObjectPropertiesValid: true,
                }));

                // Report a valid modification.
                const validModification0 = cloneDeep(editedObjProp.properties);
                (validModification0[editedObjProp.tagType.parts[0].keyword] as StringTagPartProperty).stringValue = 'modified value';
                onTagChangeFn(validModification0);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: true,
                    modifiedObjectPropertiesValid: true,
                }));

                // Report an invalid modification.
                onTagChangeFn(null);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: true,
                    modifiedObjectPropertiesValid: false,
                }));

                // Report another valid modification.
                const validModification1 = cloneDeep(editedObjProp.properties);
                (validModification1[editedObjProp.tagType.parts[0].keyword] as StringTagPartProperty).stringValue = 'another modified value';
                onTagChangeFn(validModification1);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: true,
                    modifiedObjectPropertiesValid: true,
                }));
            }),
        );

        it('sets the state appropriately if an object property is initially invalid',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                fixture.detectChanges();
                const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                // We need to call this function to initialize the validateTagSpy.
                mockTagEditorContext({
                    readOnly: false,
                    node: entityResolver.getNode(),
                    tag: editedObjProp,
                    tagType: editedObjProp.tagType,
                    tagOwner: mockPage,
                    withDelete: false,
                });
                state.mockState({
                    editor: {
                        ...state.now.editor,
                        openTab: 'properties',
                        openPropertiesTab: editedObjProp.name,
                    },
                });
                validateTagSpy.and.returnValue({ allPropertiesValid: false });

                testComponent.item = mockPage;
                multiDetectChanges(fixture, 2);

                const tagEditorHost: MockTagEditorHost = testComponent.combinedPropertiesEditor.tagEditorHostList.first as any;
                expect(tagEditorHost.editTagLive).toHaveBeenCalledTimes(1);
                expect(tagEditorHost.editTagLive.calls.argsFor(0)[0]).toEqual(editedObjProp);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: false,
                    modifiedObjectPropertiesValid: false,
                }));
            }),
        );

        it('does not mark object properties as modified if only isActive was changed',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                fixture.detectChanges();
                const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                editedObjProp.active = true;
                mockPage.tags[editedObjProp.name].active = false;
                state.mockState({
                    editor: {
                        ...state.now.editor,
                        openTab: 'properties',
                        openPropertiesTab: editedObjProp.name,
                    },
                });

                testComponent.item = mockPage;
                multiDetectChanges(fixture, 3);

                const tagEditorHost: MockTagEditorHost = testComponent.combinedPropertiesEditor.tagEditorHostList.first as any;
                expect(tagEditorHost.editTagLive).toHaveBeenCalledTimes(1);
                expect(tagEditorHost.editTagLive.calls.argsFor(0)[0]).toEqual(editedObjProp);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: false,
                    modifiedObjectPropertiesValid: true,
                }));
            }),
        );

        it('saving an object property works',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                fixture.detectChanges();
                const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                state.mockState({
                    editor: {
                        ...state.now.editor,
                        openTab: 'properties',
                        openPropertiesTab: editedObjProp.name,
                    },
                });

                testComponent.item = mockPage;
                multiDetectChanges(fixture, 2);

                const tagEditorHost: MockTagEditorHost = testComponent.combinedPropertiesEditor.tagEditorHostList.first as any;
                expect(tagEditorHost.editTagLive.calls.argsFor(0)[0]).toEqual(editedObjProp);
                const onTagChangeFn: TagChangedFn = tagEditorHost.editTagLive.calls.argsFor(0)[2];
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: false,
                    modifiedObjectPropertiesValid: true,
                }));

                // Report a valid modification.
                const validModification0 = cloneDeep(editedObjProp.properties);
                (validModification0[editedObjProp.tagType.parts[0].keyword] as StringTagPartProperty).stringValue = 'modified value';
                onTagChangeFn(validModification0);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: true,
                    modifiedObjectPropertiesValid: true,
                }));

                // Report an invalid modification.
                onTagChangeFn(null);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: true,
                    modifiedObjectPropertiesValid: false,
                }));

                // Report another valid modification.
                const validModification1 = cloneDeep(editedObjProp.properties);
                (validModification1[editedObjProp.tagType.parts[0].keyword] as StringTagPartProperty).stringValue = 'another modified value';
                onTagChangeFn(validModification1);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: true,
                    modifiedObjectPropertiesValid: true,
                }));

                const itemAfterSaving = { newItem: true };
                folderActions.updateItemObjectProperties.and.returnValue(Promise.resolve(itemAfterSaving));
                delete editedObjProp.tagType;
                const expectedUpdate: Tags = {
                    [editedObjProp.name]: {
                        ...editedObjProp,
                        properties: validModification1,
                    },
                };

                // Save the object property.
                let promiseResolved = false;
                testComponent.combinedPropertiesEditor.saveChanges()
                    .then(() => {
                        // Make sure that the changes have been saved.
                        expect(folderActions.updateItemObjectProperties).toHaveBeenCalledTimes(1);
                        expect(folderActions.updateItemObjectProperties).toHaveBeenCalledWith(
                            'page', mockPage.id, expectedUpdate, { showNotification: true, fetchForUpdate: true, fetchForConstruct: true }, undefined,
                        );

                        // The item should not be refetched manually, because it is returned by the save request.
                        expect(folderActions.getItem).not.toHaveBeenCalled();

                        // Make sure that the displayed item has been updated.
                        expect(testComponent.combinedPropertiesEditor.item as any).toBe(itemAfterSaving);

                        expect(state.now.editor).toEqual(jasmine.objectContaining({
                            objectPropertiesModified: false,
                            modifiedObjectPropertiesValid: true,
                        }));
                        promiseResolved = true;
                    })
                    .catch(() => fail('saveChanges() should not fail here.'));

                tick();
                expect(promiseResolved).toBe(true);
            }),
        );

        it('saving a folder object property with applyToSubfolders works',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                fixture.detectChanges();
                const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                state.mockState({
                    editor: {
                        ...state.now.editor,
                        openTab: 'properties',
                        openPropertiesTab: editedObjProp.name,
                        itemId: mockFolder.id,
                        itemType: 'folder',
                    },
                });

                testComponent.item = mockFolder;
                multiDetectChanges(fixture, 2);

                const tagEditorHost: MockTagEditorHost = testComponent.combinedPropertiesEditor.tagEditorHostList.first as any;
                expect(tagEditorHost.editTagLive.calls.argsFor(0)[0]).toEqual(editedObjProp);
                const onTagChangeFn: TagChangedFn = tagEditorHost.editTagLive.calls.argsFor(0)[2];
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: false,
                    modifiedObjectPropertiesValid: true,
                }));

                // Report a valid modification.
                const validModification0 = cloneDeep(editedObjProp.properties);
                (validModification0[editedObjProp.tagType.parts[0].keyword] as StringTagPartProperty).stringValue = 'modified value';
                onTagChangeFn(validModification0);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: true,
                    modifiedObjectPropertiesValid: true,
                }));

                const itemAfterSaving = { newItem: true };
                folderActions.updateItemObjectProperties.and.returnValue(Promise.resolve(itemAfterSaving));
                delete editedObjProp.tagType;
                const expectedUpdate: Tags = {
                    [editedObjProp.name]: {
                        ...editedObjProp,
                        properties: validModification0,
                    },
                };
                const expectedOptions: FolderSaveRequestOptions = {
                    nodeId: mockFolder.nodeId,
                    tagsToSubfolders: [ editedObjProp.name ],
                };

                // Save the object property.
                let promiseResolved = false;
                testComponent.combinedPropertiesEditor.saveChanges({ applyToSubfolders: true })
                    .then(() => {
                        // Make sure that the changes have been saved.
                        expect(folderActions.updateItemObjectProperties).toHaveBeenCalledTimes(1);
                        expect(folderActions.updateItemObjectProperties).toHaveBeenCalledWith(
                            'folder', mockFolder.id, expectedUpdate, { showNotification: true, fetchForUpdate: true, fetchForConstruct: true }, expectedOptions,
                        );

                        // The item should not be refetched manually, because it is returned by the save request.
                        expect(folderActions.getItem).not.toHaveBeenCalled();

                        // Make sure that the displayed item has been updated.
                        expect(testComponent.combinedPropertiesEditor.item as any).toBe(itemAfterSaving);

                        expect(state.now.editor).toEqual(jasmine.objectContaining({
                            objectPropertiesModified: false,
                            modifiedObjectPropertiesValid: true,
                        }));
                        promiseResolved = true;
                    })
                    .catch(() => fail('saveChanges() should not fail here.'));

                tick();
                expect(promiseResolved).toBe(true);
            }),
        );

        it('saving with applyToLanguageVariants works',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                fixture.detectChanges();
                const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                state.mockState({
                    editor: {
                        ...state.now.editor,
                        openTab: 'properties',
                        openPropertiesTab: editedObjProp.name,
                    },
                });

                testComponent.item = mockPage;
                multiDetectChanges(fixture, 2);

                const tagEditorHost: MockTagEditorHost = testComponent.combinedPropertiesEditor.tagEditorHostList.first as any;
                expect(tagEditorHost.editTagLive.calls.argsFor(0)[0]).toEqual(editedObjProp);
                const onTagChangeFn: TagChangedFn = tagEditorHost.editTagLive.calls.argsFor(0)[2];
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: false,
                    modifiedObjectPropertiesValid: true,
                }));

                // Report a valid modification.
                const validModification0 = cloneDeep(editedObjProp.properties);
                (validModification0[editedObjProp.tagType.parts[0].keyword] as StringTagPartProperty).stringValue = 'modified value';
                onTagChangeFn(validModification0);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: true,
                    modifiedObjectPropertiesValid: true,
                }));

                // Report an invalid modification.
                onTagChangeFn(null);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: true,
                    modifiedObjectPropertiesValid: false,
                }));

                // Report another valid modification.
                const validModification1 = cloneDeep(editedObjProp.properties);
                (validModification1[editedObjProp.tagType.parts[0].keyword] as StringTagPartProperty).stringValue = 'another modified value';
                onTagChangeFn(validModification1);
                expect(state.now.editor).toEqual(jasmine.objectContaining({
                    objectPropertiesModified: true,
                    modifiedObjectPropertiesValid: true,
                }));

                const itemAfterSaving = { id: mockPage.id, newItem: true };
                folderActions.updateItemsObjectProperties.and.returnValue(Promise.resolve([itemAfterSaving]));
                delete editedObjProp.tagType;

                const expectedUpdateToLanguageVariants: Tags = {
                    [editedObjProp.name]: {
                        ...editedObjProp,
                        properties: validModification1,
                    },
                };

                const languageVariants = Object.values((mockPage ).languageVariants).map((languageVariant: any) => languageVariant.id);

                // Save the object property.
                let promiseResolved = false;
                testComponent.combinedPropertiesEditor.saveChanges({ applyToLanguageVariants: languageVariants })
                    .then(() => {
                        // Make sure that the changes have been saved.
                        expect(folderActions.updateItemsObjectProperties).toHaveBeenCalledTimes(1);
                        expect(folderActions.updateItemsObjectProperties).toHaveBeenCalledWith(
                            'page',
                            languageVariants.map(variant => ({
                                itemId: variant,
                                updatedObjProps: expectedUpdateToLanguageVariants,
                                requestOptions: undefined,
                            })),
                            { showNotification: true, fetchForUpdate: true, fetchForConstruct: true },
                        );

                        // The item should not be refetched manually, because it is returned by the save request.
                        expect(folderActions.getItem).not.toHaveBeenCalled();

                        // Make sure that the displayed item has been updated.
                        expect(testComponent.combinedPropertiesEditor.item as any).toBe(itemAfterSaving);

                        expect(state.now.editor).toEqual(jasmine.objectContaining({
                            objectPropertiesModified: false,
                            modifiedObjectPropertiesValid: true,
                        }));
                        promiseResolved = true;
                    })
                    .catch(() => fail('saveChanges() should not fail here.'));

                tick();
                expect(promiseResolved).toBe(true);
            }),
        );

        // Disabled old-tagfill tests, as it is not available anymore (CMS >= v6.0 dropped PHP and therefore the old UI)
        xdescribe('old tagfill', () => {

            let resourceUrlBuilder: MockResourceUrlBuilderService;

            beforeEach(() => {
                resourceUrlBuilder = TestBed.get(ResourceUrlBuilder);
            });

            it('is loaded if the NodeFeature.newTagEditor is not enabled',
                componentTest(() => TestComponent, (fixture, testComponent) => {
                    const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                    mockPage.tags[editedObjProp.name].active = false;
                    state.mockState({
                        editor: {
                            ...state.now.editor,
                            openTab: 'properties',
                            openPropertiesTab: editedObjProp.name,
                        },
                        features: {
                            ...state.now.features,
                            nodeFeatures: {
                                [mockNode.id]: [ ],
                            },
                        },
                    });

                    testComponent.item = mockPage;
                    multiDetectChanges(fixture, 2);

                    expect(fixture.debugElement.query(By.css(ITEM_PROPERTIES_EDITOR_SELECTOR))).toBeFalsy();
                    expect(fixture.debugElement.query(By.css(OBJECT_PROPERTIES_EDITOR_SELECTOR))).toBeTruthy();
                    expect(fixture.debugElement.query(By.directive(MockTagEditorHost))).toBeFalsy();
                    const iFrameWrapper = fixture.debugElement.query(By.directive(MockIFrameWrapper));
                    expect(iFrameWrapper).toBeTruthy();
                    const iFrameWrapperComp = iFrameWrapper.componentInstance as MockIFrameWrapper;
                    expect(iFrameWrapperComp.srcUrl).toEqual(OLD_TAGFILL_URL);

                    expect(state.now.editor).toEqual(jasmine.objectContaining({
                        objectPropertiesModified: true,
                        modifiedObjectPropertiesValid: true,
                    }));
                }),
            );

            it('is loaded if tag.newEditor is false',
                componentTest(() => TestComponent, (fixture, testComponent) => {
                    const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                    mockPage.tags[editedObjProp.name].active = false;
                    state.mockState({
                        editor: {
                            ...state.now.editor,
                            openTab: 'properties',
                            openPropertiesTab: editedObjProp.name,
                        },
                    });

                    testComponent.item = mockPage;
                    multiDetectChanges(fixture, 2);

                    expect(fixture.debugElement.query(By.css(ITEM_PROPERTIES_EDITOR_SELECTOR))).toBeFalsy();
                    expect(fixture.debugElement.query(By.css(OBJECT_PROPERTIES_EDITOR_SELECTOR))).toBeTruthy();
                    expect(fixture.debugElement.query(By.directive(MockTagEditorHost))).toBeFalsy();
                    const iFrameWrapper = fixture.debugElement.query(By.directive(MockIFrameWrapper));
                    expect(iFrameWrapper).toBeTruthy();
                    const iFrameWrapperComp = iFrameWrapper.componentInstance as MockIFrameWrapper;
                    expect(iFrameWrapperComp.srcUrl).toEqual(OLD_TAGFILL_URL);

                    expect(state.now.editor).toEqual(jasmine.objectContaining({
                        objectPropertiesModified: true,
                        modifiedObjectPropertiesValid: true,
                    }));
                }),
            );

            it('saves an object property without an id once before opening it',
                componentTest(() => TestComponent, (fixture, testComponent) => {
                    const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                    mockFolder.tags[editedObjProp.name].active = false;
                    state.mockState({
                        editor: {
                            ...state.now.editor,
                            openTab: 'properties',
                            openPropertiesTab: editedObjProp.name,
                        },
                    });

                    // Simulate that the tag has not been added to the DB yet.
                    delete mockFolder.tags[editedObjProp.name].id;

                    const expectedSavedTag: Tags = {
                        [editedObjProp.name]: {
                            ...mockFolder.tags[editedObjProp.name],
                        },
                    };
                    const tagId = 4711;
                    const updatedItem = cloneDeep(mockFolder);
                    updatedItem.tags[editedObjProp.name].id = tagId;
                    folderActions.updateItemObjectProperties.and.returnValue(Promise.resolve(updatedItem));

                    testComponent.item = mockFolder;
                    multiDetectChanges(fixture, 2);

                    const iFrameWrapper = fixture.debugElement.query(By.directive(MockIFrameWrapper));
                    expect(iFrameWrapper).toBeTruthy();
                    const iFrameWrapperComp = iFrameWrapper.componentInstance as MockIFrameWrapper;
                    expect(iFrameWrapperComp.srcUrl).toEqual(OLD_TAGFILL_URL);

                    expect(folderActions.updateItemObjectProperties).toHaveBeenCalledTimes(1);
                    expect(folderActions.updateItemObjectProperties).toHaveBeenCalledWith(
                        'folder', mockFolder.id, expectedSavedTag, { showNotification: false, fetchForUpdate: true, fetchForConstruct: true }, undefined,
                    );

                    expect(state.now.editor).toEqual(jasmine.objectContaining({
                        objectPropertiesModified: true,
                        modifiedObjectPropertiesValid: true,
                    }));
                }),
            );

            it('does not save object property without an id if it is readOnly',
                componentTest(() => TestComponent, (fixture, testComponent) => {
                    const i18nNotificationService = TestBed.get(I18nNotification) as MockI18nNotification;
                    const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                    (mockFolder.tags[editedObjProp.name] as ObjectTag).readOnly = true;
                    mockFolder.tags[editedObjProp.name].active = false;
                    state.mockState({
                        editor: {
                            ...state.now.editor,
                            openTab: 'properties',
                            openPropertiesTab: editedObjProp.name,
                        },
                    });

                    // Simulate that the tag has not been added to the DB yet.
                    delete mockFolder.tags[editedObjProp.name].id;

                    testComponent.item = mockFolder;
                    multiDetectChanges(fixture, 2);

                    const iFrameWrapper = fixture.debugElement.query(By.directive(MockIFrameWrapper));
                    expect(iFrameWrapper).toBeTruthy();
                    const iFrameWrapperComp = iFrameWrapper.componentInstance as MockIFrameWrapper;
                    expect(iFrameWrapperComp.srcUrl).toEqual('about:blank');

                    expect(folderActions.updateItemObjectProperties).not.toHaveBeenCalledTimes(1);
                    expect(i18nNotificationService.show).toHaveBeenCalledTimes(1);

                    expect(state.now.editor).toEqual(jasmine.objectContaining({
                        objectPropertiesModified: false,
                        modifiedObjectPropertiesValid: false,
                    }));
                }),
            );

            it('uses the correct URL builder parameters if the tag does not contain an overview',
                componentTest(() => TestComponent, (fixture, testComponent) => {
                    const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                    state.mockState({
                        editor: {
                            ...state.now.editor,
                            openTab: 'properties',
                            openPropertiesTab: editedObjProp.name,
                        },
                    });

                    testComponent.item = mockPage;
                    multiDetectChanges(fixture, 2);
                }),
            );

            it('uses the correct URL builder parameters if the tag contains an overview',
                componentTest(() => TestComponent, (fixture, testComponent) => {
                    const editedObjProp = mockPage.tags[TAG_WITH_OVERVIEW];
                    state.mockState({
                        editor: {
                            ...state.now.editor,
                            openTab: 'properties',
                            openPropertiesTab: editedObjProp.name,
                        },
                    });

                    testComponent.item = mockPage;
                    multiDetectChanges(fixture, 2);
                }),
            );

            it('sets the state correctly if the object property was already active',
                componentTest(() => TestComponent, (fixture, testComponent) => {
                    const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                    state.mockState({
                        editor: {
                            ...state.now.editor,
                            openTab: 'properties',
                            openPropertiesTab: editedObjProp.name,
                        },
                    });

                    testComponent.item = mockPage;
                    multiDetectChanges(fixture, 2);

                    expect(fixture.debugElement.query(By.css(ITEM_PROPERTIES_EDITOR_SELECTOR))).toBeFalsy();
                    expect(fixture.debugElement.query(By.css(OBJECT_PROPERTIES_EDITOR_SELECTOR))).toBeTruthy();
                    expect(fixture.debugElement.query(By.directive(MockTagEditorHost))).toBeFalsy();
                    const iFrameWrapper = fixture.debugElement.query(By.directive(MockIFrameWrapper));
                    expect(iFrameWrapper).toBeTruthy();
                    const iFrameWrapperComp = iFrameWrapper.componentInstance as MockIFrameWrapper;
                    expect(iFrameWrapperComp.srcUrl).toEqual(OLD_TAGFILL_URL);

                    expect(state.now.editor).toEqual(jasmine.objectContaining({
                        objectPropertiesModified: false,
                        modifiedObjectPropertiesValid: true,
                    }));
                }),
            );

            it('saving works',
                componentTest(() => TestComponent, (fixture, testComponent) => {
                    const customScriptHostService = TestBed.get(CustomScriptHostService) as MockCustomScriptHostService;
                    const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                    mockPage.tags[editedObjProp.name].active = false;
                    state.mockState({
                        editor: {
                            ...state.now.editor,
                            openTab: 'properties',
                            openPropertiesTab: editedObjProp.name,
                        },
                    });

                    testComponent.item = mockPage;
                    multiDetectChanges(fixture, 2);

                    expect(fixture.debugElement.query(By.directive(MockIFrameWrapper))).toBeTruthy();
                    expect(state.now.editor).toEqual(jasmine.objectContaining({
                        objectPropertiesModified: true,
                        modifiedObjectPropertiesValid: true,
                    }));

                    const editedItem = cloneDeep(mockPage);
                    (editedItem.tags[editedObjProp.name]
                        .properties[editedObjProp.tagType.parts[0].keyword] as StringTagPartProperty).stringValue = 'modified value';
                    editedItem.tags[editedObjProp.name].active = true;
                    folderActions.getItem.and.returnValue(editedItem);

                    // Save the object property.
                    let promiseResolved = false;
                    testComponent.combinedPropertiesEditor.saveChanges()
                        .then(() => {
                            // Make sure that the changes have been saved.
                            expect(customScriptHostService.saveObjectProperty).toHaveBeenCalledTimes(1);

                            // Make sure that the updated item has been fetched.
                            expect(folderActions.getItem).toHaveBeenCalledTimes(1);
                            expect(folderActions.getItem).toHaveBeenCalledWith(mockPage.id, mockPage.type, {
                                nodeId: mockNode.id,
                                construct: true,
                                update: true,
                            });
                            expect(testComponent.combinedPropertiesEditor.item).toBe(editedItem);

                            expect(state.now.editor).toEqual(jasmine.objectContaining({
                                objectPropertiesModified: false,
                                modifiedObjectPropertiesValid: true,
                            }));
                            promiseResolved = true;
                        })
                        .catch(() => fail('saveChanges() should not fail here.'));

                    tick();
                    expect(promiseResolved).toBe(true);
                }),
            );

            it('saving with applyToSubfolders works',
                componentTest(() => TestComponent, (fixture, testComponent) => {
                    const customScriptHostService = TestBed.get(CustomScriptHostService) as MockCustomScriptHostService;
                    const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                    mockFolder.tags[editedObjProp.name].active = false;
                    state.mockState({
                        editor: {
                            ...state.now.editor,
                            openTab: 'properties',
                            openPropertiesTab: editedObjProp.name,
                        },
                    });

                    testComponent.item = mockFolder;
                    multiDetectChanges(fixture, 2);

                    expect(fixture.debugElement.query(By.directive(MockIFrameWrapper))).toBeTruthy();
                    expect(state.now.editor).toEqual(jasmine.objectContaining({
                        objectPropertiesModified: true,
                        modifiedObjectPropertiesValid: true,
                    }));

                    const editedItem0 = cloneDeep(mockFolder);
                    (editedItem0.tags[editedObjProp.name]
                        .properties[editedObjProp.tagType.parts[0].keyword] as StringTagPartProperty).stringValue = 'modified value';
                    editedItem0.tags[editedObjProp.name].active = true;
                    folderActions.getItem.and.returnValue(editedItem0);
                    const editedItem1 = cloneDeep(editedItem0);
                    folderActions.updateItemObjectProperties.and.returnValue(Promise.resolve(editedItem1));
                    delete editedObjProp.tagType;
                    const expectedUpdateToSubfolders: Tags = {
                        [editedObjProp.name]: {
                            ...editedObjProp,
                            properties: editedItem0.tags[editedObjProp.name].properties,
                        },
                    };
                    const expectedOptions: FolderSaveRequestOptions = {
                        nodeId: mockFolder.nodeId,
                        tagsToSubfolders: [ editedObjProp.name ],
                    };

                    // Save the object property.
                    let promiseResolved = false;
                    testComponent.combinedPropertiesEditor.saveChanges({ applyToSubfolders: true })
                        .then(() => {
                            // Make sure that the changes have been saved.
                            expect(customScriptHostService.saveObjectProperty).toHaveBeenCalledTimes(1);

                            // Make sure that the updated item has been fetched and then saved again with tagsToSubfolders.
                            expect(folderActions.getItem).toHaveBeenCalledTimes(1);
                            expect(folderActions.getItem).toHaveBeenCalledWith(mockFolder.id, mockFolder.type, {
                                nodeId: mockNode.id,
                                construct: true,
                                update: true,
                            });
                            expect(folderActions.updateItemObjectProperties).toHaveBeenCalledTimes(1);
                            expect(folderActions.updateItemObjectProperties).toHaveBeenCalledWith(
                                'folder', mockFolder.id, expectedUpdateToSubfolders,
                                { showNotification: true, fetchForUpdate: true, fetchForConstruct: true },
                                expectedOptions,
                            );
                            expect(testComponent.combinedPropertiesEditor.item).toBe(editedItem1);

                            expect(state.now.editor).toEqual(jasmine.objectContaining({
                                objectPropertiesModified: false,
                                modifiedObjectPropertiesValid: true,
                            }));
                            promiseResolved = true;
                        })
                        .catch(() => fail('saveChanges() should not fail here.'));

                    tick();
                    expect(promiseResolved).toBe(true);
                }),
            );

            it('saving with applyToLanguageVariants works',
                componentTest(() => TestComponent, (fixture, testComponent) => {
                    const customScriptHostService = TestBed.get(CustomScriptHostService) as MockCustomScriptHostService;
                    const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
                    mockPage.tags[editedObjProp.name].active = false;
                    state.mockState({
                        editor: {
                            ...state.now.editor,
                            openTab: 'properties',
                            openPropertiesTab: editedObjProp.name,
                        },
                    });

                    testComponent.item = mockPage;
                    multiDetectChanges(fixture, 2);

                    expect(fixture.debugElement.query(By.directive(MockIFrameWrapper))).toBeTruthy();
                    expect(state.now.editor).toEqual(jasmine.objectContaining({
                        objectPropertiesModified: true,
                        modifiedObjectPropertiesValid: true,
                    }));

                    const editedItem = cloneDeep(mockPage);
                    (editedItem.tags[editedObjProp.name]
                        .properties[editedObjProp.tagType.parts[0].keyword] as StringTagPartProperty).stringValue = 'modified value';
                    editedItem.tags[editedObjProp.name].active = true;
                    folderActions.getItem.and.returnValue(editedItem);

                    const updatedItem = cloneDeep(editedItem);
                    folderActions.updateItemsObjectProperties.and.returnValue(Promise.resolve([updatedItem]));

                    delete editedObjProp.tagType;
                    const expectedUpdateToLanguageVariants: Tags = {
                        [editedObjProp.name]: {
                            ...editedObjProp,
                            properties: updatedItem.tags[editedObjProp.name].properties,
                        },
                    };

                    // Save the object property.
                    const languageVariants = Object.values((editedItem ).languageVariants).map((languageVariant: any) => languageVariant.id);
                    let promiseResolved = false;
                    testComponent.combinedPropertiesEditor.saveChanges({ applyToLanguageVariants: languageVariants })
                        .then(() => {
                            // Make sure that the changes have been saved.
                            expect(customScriptHostService.saveObjectProperty).toHaveBeenCalledTimes(1);

                            // Make sure that the updated item has been fetched.
                            expect(folderActions.getItem).toHaveBeenCalledTimes(1);
                            expect(folderActions.getItem).toHaveBeenCalledWith(mockPage.id, mockPage.type, {
                                nodeId: mockNode.id,
                                construct: true,
                                update: true,
                            });
                            expect(folderActions.updateItemsObjectProperties).toHaveBeenCalledTimes(1);
                            expect(folderActions.updateItemsObjectProperties).toHaveBeenCalledWith(
                                'page',
                                languageVariants.map(variant => ({
                                    itemId: variant,
                                    updatedObjProps: expectedUpdateToLanguageVariants,
                                    requestOptions: undefined,
                                })).filter(variant => variant.itemId !== updatedItem.id),
                                { showNotification: true, fetchForUpdate: true, fetchForConstruct: true },
                            );
                            expect(testComponent.combinedPropertiesEditor.item).toBe(updatedItem);

                            expect(state.now.editor).toEqual(jasmine.objectContaining({
                                objectPropertiesModified: false,
                                modifiedObjectPropertiesValid: true,
                            }));
                            promiseResolved = true;
                        })
                        .catch(() => fail('saveChanges() should not fail here.'));

                    tick();
                    expect(promiseResolved).toBe(true);
                }),
            );

            it('does not display the active/inactive checkbox',
                componentTest(() => TestComponent, (fixture, testComponent) => {
                    testComponent.item = mockPage;
                    multiDetectChanges(fixture, 2);

                    // Navigate to the last object property.
                    const tabs = fixture.debugElement.queryAll(By.css(TAB_SELECTOR));
                    tabs[tabs.length - 1].nativeElement.click();
                    multiDetectChanges(fixture, 2);

                    const checkboxDiv = fixture.debugElement.query(By.css(ACTIVE_CHECKBOX_DIV_SELECTOR));

                    expect(checkboxDiv).toBeFalsy();
                }),
            );

        });

        describe('with tagfill_light disabled', () => {
            beforeEach(() => {
                state.mockState({
                    features: {
                        ...state.now.features,
                        tagfill_light: false,
                    },
                });
            });

            it('displays the active/inactive checkbox',
                componentTest(() => TestComponent, (fixture, testComponent) => {
                    testComponent.item = mockPage;
                    multiDetectChanges(fixture, 2);

                    // Navigate to the last object property.
                    const tabs = fixture.debugElement.queryAll(By.css(TAB_SELECTOR));
                    tabs[tabs.length - 1].nativeElement.click();
                    multiDetectChanges(fixture, 2);

                    const checkboxDiv = fixture.debugElement.query(By.css(ACTIVE_CHECKBOX_DIV_SELECTOR));

                    expect(checkboxDiv).toBeTruthy();
                }),
            );

            // TODO: Fix these unit tests.
            // it('displays the active/inactive checkbox and checks it when tag.active is true',
            //     componentTest(() => TestComponent, (fixture, testComponent) => {
            //         const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
            //         mockPage.tags[editedObjProp.name].active = true;

            //         testComponent.item = mockPage;
            //         multiDetectChanges(fixture, 2);

            //         // Navigate to the last object property.
            //         const tabs = fixture.debugElement.queryAll(By.css(TAB_SELECTOR));
            //         tabs[tabs.length - 1].nativeElement.click();
            //         multiDetectChanges(fixture, 10);

            //         const checkboxDiv = fixture.debugElement.query(By.css(ACTIVE_CHECKBOX_DIV_SELECTOR));

            //         expect(checkboxDiv).toBeTruthy();

            //         const checkbox = checkboxDiv.query(By.directive(Checkbox));

            //         expect(checkbox.componentInstance.checked).toBe(true);
            //     })
            // );

            // it('displays the active/inactive checkbox and unchecks it when tag.active is false',
            //     componentTest(() => TestComponent, (fixture, testComponent) => {
            //         const editedObjProp = mockObjPropsSorted[mockObjPropsSorted.length - 1];
            //         mockPage.tags[editedObjProp.name].active = false;

            //         testComponent.item = mockPage;
            //         multiDetectChanges(fixture, 2);

            //         // Navigate to the last object property.
            //         const tabs = fixture.debugElement.queryAll(By.css(TAB_SELECTOR));
            //         tabs[tabs.length - 1].nativeElement.click();
            //         multiDetectChanges(fixture, 2);

            //         const checkboxDiv = fixture.debugElement.query(By.css(ACTIVE_CHECKBOX_DIV_SELECTOR));

            //         expect(checkboxDiv).toBeTruthy();

            //         const checkbox = checkboxDiv.query(By.directive(Checkbox));

            //         expect(checkbox.componentInstance.checked).toBe(false);
            //     })
            // );
        });
    });

    describe('tag list', () => {
        it('displays one tab for the tag list for page items',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                testComponent.item = mockPage;
                multiDetectChanges(fixture, 2);

                // Check if tab list tag is present.
                const tabs = fixture.debugElement.queryAll(By.css(TAB_SELECTOR));
                expect((tabs[1].nativeElement as HTMLElement).innerText.toLowerCase()).toEqual('editor.tag_list_label');
            }),
        );

        it('displays no tab for the tag list for folder items',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                spyOn(client.folder, 'templates').and.returnValue(of({
                    templates: [],
                    hasMoreItems: false,
                    messages: [],
                    numItems: 0,
                    responseInfo: null,
                }));
                testComponent.item = mockFolder;
                multiDetectChanges(fixture, 2);

                // Check if no tab list tag is present.
                const tabs = fixture.debugElement.queryAll(By.css(TAB_SELECTOR));
                if (tabs[1]) {
                    expect((tabs[1].nativeElement as HTMLElement).innerText.toLowerCase()).not.toEqual('editor.tag_list_label');
                } else {
                    expect(tabs[1]).toBeUndefined();
                }
            }),
        );

        it('displays no tab for the tag list for file items',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                spyOn(client.folder, 'templates').and.returnValue(of({
                    templates: [],
                    hasMoreItems: false,
                    messages: [],
                    numItems: 0,
                    responseInfo: null,
                }));
                testComponent.item = mockFile;
                multiDetectChanges(fixture, 2);

                // Check if no tab list tag is present.
                const tabs = fixture.debugElement.queryAll(By.css(TAB_SELECTOR));
                if (tabs[1]) {
                    expect((tabs[1].nativeElement as HTMLElement).innerText.toLowerCase()).not.toEqual('editor.tag_list_label');
                } else {
                    expect(tabs[1]).toBeUndefined();
                }
            }),
        );

        it('displays no tab for the tag list for image items',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                testComponent.item = mockImage;
                multiDetectChanges(fixture, 2);

                // Check if no tab list tag is present.
                const tabs = fixture.debugElement.queryAll(By.css(TAB_SELECTOR));
                if (tabs[1]) {
                    expect((tabs[1].nativeElement as HTMLElement).innerText.toLowerCase()).not.toEqual('editor.tag_list_label');
                } else {
                    expect(tabs[1]).toBeUndefined();
                }
            }),
        );

        it('with a page as item parses content tags accordingly',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                testComponent.item = mockPage;
                fixture.detectChanges();
                const contentTags: Tag[] = generateContentTagList(mockPage);
                tick(1000);
                const mockContentTags = getExampleEditableTag();
                expect(contentTags).toEqual([mockContentTags]);
                expect(testComponent.combinedPropertiesEditor.contentTagRows.length).toEqual(contentTags.length);
                expect(testComponent.combinedPropertiesEditor.contentTagRows.map(row => row.item)).toEqual(contentTags);
            }),
        );

        it('updated accordingly when switching from a page to an item that does not have content tags',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                spyOn(client.folder, 'templates').and.returnValue(of({
                    templates: [],
                    hasMoreItems: false,
                    messages: [],
                    numItems: 0,
                    responseInfo: null,
                }));
                testComponent.item = mockPage;
                fixture.detectChanges();
                let contentTags: Tag[] = generateContentTagList(mockPage);
                tick(100);
                const mockContentTags = getExampleEditableTag();
                expect(contentTags).toEqual([mockContentTags]);
                expect(testComponent.combinedPropertiesEditor.contentTagRows.length).toEqual(contentTags.length);
                expect(testComponent.combinedPropertiesEditor.contentTagRows.map(row => row.item)).toEqual(contentTags);

                // switch to folder item
                testComponent.item = mockFolder;
                fixture.detectChanges();
                contentTags = generateContentTagList(mockFolder as any);
                tick(100);
                expect(contentTags).toEqual([]);
                expect(testComponent.combinedPropertiesEditor.contentTagRows.length).toEqual(contentTags.length);
                expect(testComponent.combinedPropertiesEditor.contentTagRows.map(row => row.item)).toEqual(contentTags);
            }),
        );

        it('updated accordingly when switching from an item that does not have content tags to a page',
            componentTest(() => TestComponent, (fixture, testComponent) => {
                spyOn(client.folder, 'templates').and.returnValue(of({
                    templates: [],
                    hasMoreItems: false,
                    messages: [],
                    numItems: 0,
                    responseInfo: null,
                }));
                testComponent.item = mockFolder;
                fixture.detectChanges();
                let contentTags: Tag[] = generateContentTagList(mockFolder as any);
                tick(100);
                expect(contentTags).toEqual([]);
                expect(testComponent.combinedPropertiesEditor.contentTagRows).toEqual([]);

                // switch to page item
                testComponent.item = mockPage;
                fixture.detectChanges();
                contentTags = generateContentTagList(mockPage);
                tick(100);
                const mockContentTags = getExampleEditableTag();
                expect(contentTags).toEqual([mockContentTags]);
                expect(testComponent.combinedPropertiesEditor.contentTagRows.length).toEqual(contentTags.length);
                expect(testComponent.combinedPropertiesEditor.contentTagRows.map(row => row.item)).toEqual(contentTags);
            }),
        );
    });


    function mockTagEditorContext(editTagInfo: EditTagInfo): TagEditorContext {
        if (!validateTagSpy) {
            validateTagSpy = jasmine.createSpy('validateAllTagProperties').and.returnValue({ allPropertiesValid: true });
        }
        const ret: Partial<TagEditorContext> = {
            page: editTagInfo.tagOwner as any,
            editedTag: {
                ...editTagInfo.tag,
                tagType: editTagInfo.tagType,
            },
            readOnly: editTagInfo.readOnly,
            node: editTagInfo.node as Node<Raw>,
            validator: {
                validateAllTagProperties: validateTagSpy,
            } as any,
        };
        return ret as TagEditorContext;
    }

    class MockEditorActions {

        changeTab(): void { }
    }

    class MockEntityResolver {
        getLanguage = jasmine.createSpy('getLanguage').and.callFake(mockLanguage);
        getTemplate = jasmine.createSpy('getTemplate').and.callFake(mockTemplate);

        private callsCount = 0;

        getNode(): Node {
            return mockNode;
        }

        getFolder(): any {
            return null;
        }
    }

    class MockFolderActions {
        getFolder = jasmine.createSpy('getFolder').and.returnValue(
            Promise.resolve(getExampleFolderData({ id: mockPage.folderId })),
        );

        getItem = jasmine.createSpy('getItem');
        updatePageProperties = jasmine.createSpy('updatePageProperties').and.returnValue(Promise.resolve());
        updateItemObjectProperties = jasmine.createSpy('updateItemObjectProperties');
        updateItemsObjectProperties = jasmine.createSpy('updateItemsObjectProperties');

        getNode(): any {
            return mockNode;
        }
    }

    class MockNavigationService {
        detailOrModal = jasmine.createSpy('detailOrModal').and.callFake((nodeId, itemType, itemId, editMode, options?: EditorStateUrlOptions) => ({
            navigate: () => {
                state.mockState({
                    editor: {
                        ...state.now.editor,
                        editMode,
                        openTab: options.openTab,
                        openPropertiesTab: options.propertiesTab,
                    },
                });
            },
        }));
    }

    class MockTagEditorService {
        createTagEditorContext = jasmine.createSpy('createTagEditorContext').and.callFake(mockTagEditorContext);
    }

    class MockUserSettingsService {}

});

function mockObjectProperties(): { [name: string]: EditableObjectTag } {
    const tag0 = mockEditableObjectTag<StringTagPartProperty>([
        {
            type: TagPropertyType.STRING,
            typeId: TagPartType.Text,
            stringValue: 'test',
        },
    ], {
        tagType: {
            id: 4711,
            keyword: 'tagType4711',
        },
        name: `${TAG0_NAME}`,
        sortOrder: 10,
    });
    const tag1 = mockEditableObjectTag<StringTagPartProperty>([
        {
            type: TagPropertyType.STRING,
            typeId: TagPartType.Text,
            stringValue: 'test',
        },
    ], {
        tagType: {
            id: 2,
            keyword: 'tagType2',
        },
        name: `${TAG1_NAME}`,
        sortOrder: 0,
    });
    const tag2 = mockEditableObjectTag<OverviewTagPartProperty>([
        {
            type: TagPropertyType.OVERVIEW,
            typeId: TagPartType.Overview,
        },
    ], {
        tagType: {
            id: 100,
            keyword: 'tagTypeWithOverview',
        },
        name: `${TAG2_NAME}`,
        sortOrder: 4,
    });

    return {
        [tag0.name]: tag0,
        [tag1.name]: tag1,
        [tag2.name]: tag2,
    };
}

const mockLanguage = (id: number): Language => ({
    id,
    name: `Language${id}`,
    code: `lang${id}`,
});

function mockTemplate(id: number): Template {
    const ret: Partial<Template> = {
        id,
        name: `template${id}`,
    };
    return ret as Template;
}

function multiDetectChanges(fixture: ComponentFixture<any>, count: number, delay: number = 100): void {
    for (let i = 0; i < count; ++i) {
        fixture.detectChanges();
        flush();
        tick(delay);
    }
}

class MockErrorHandler {
    catch(): void {}
}

class MockPermissionService {
    forItem = jasmine.createSpy('forItem').and.returnValue(observableOf({ edit: true }));
}

class MockCustomScriptHostService {
    saveObjectProperty = jasmine.createSpy('saveObjectProperty').and.callFake(() => Promise.resolve());
}

class MockResourceUrlBuilderService {
}

class MockI18nNotification {
    show = jasmine.createSpy('show').and.stub();
}

@Component({
    selector: 'test-component',
    template: `
        <combined-properties-editor [item]="item"></combined-properties-editor>
        <gtx-overlay-host></gtx-overlay-host>
    `,
})
class TestComponent {
    @ViewChild(CombinedPropertiesEditorComponent, { static: true })
    combinedPropertiesEditor: CombinedPropertiesEditorComponent;

    item: ItemWithObjectTags | Node;
}

@Component({
    selector: 'properties-editor',
    providers: [ { provide: PropertiesEditor, useClass: MockPropertiesEditor } ],
    template: '',
})
class MockPropertiesEditor {
    @Input() item: any;
    @Input() nodeId: any;
    @Input() templates: any;
    @Input() languages: any;

    changes = new BehaviorSubject<any>(null);
}

@Component({
    selector: 'tag-editor-host',
    providers: [ { provide: TagEditorHostComponent, useClass: MockTagEditorHost } ],
    template: '',
})
class MockTagEditorHost {
    editTagLive = jasmine.createSpy('editTagLive');
}

@Component({
    selector: 'tag-editor-overlay-host',
    template: '',
})
class MockTagEditorOverlayHost {}


class MockI18nService {
    translate(key: string): string {
        return key;
    }
}

@Component({
    selector: 'iframe-wrapper',
    providers: [ { provide: IFrameWrapperComponent, useClass: MockIFrameWrapper } ],
    template: '',
})
class MockIFrameWrapper {
    @Input() height: string;
    @Input() srcUrl: string;
    @Input() disableValidation = false;
}
