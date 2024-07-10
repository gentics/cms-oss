import { Component, DebugElement, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowseBoxComponent } from '@gentics/cms-components';
import { TagEditorContext } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    Feature,
    FileResponse,
    FileTagPartProperty,
    FolderResponse,
    ImageResponse,
    ImageTagPartProperty,
    ResponseCode,
    TagPart,
    TagPartType,
    TagPropertyType,
} from '@gentics/cms-models';
import { getExampleFileData, getExampleFolderData, getExampleImageData } from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { Observable, of, throwError } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../../testing';
import { getMockedTagEditorContext, mockEditableTag } from '../../../../../testing/test-tag-editor-data.mock';
import { FeaturesState } from '../../../../common/models';
import { ApiBase } from '../../../../core/providers/api';
import { MockApiBase } from '../../../../core/providers/api/api-base.mock';
import { I18nService } from '../../../../core/providers/i18n/i18n.service';
import { UploadConflictService } from '../../../../core/providers/upload-conflict/upload-conflict.service';
import { EditorOverlayService } from '../../../../editor-overlay/providers/editor-overlay.service';
import { FilePropertiesForm } from '../../../../shared/components/file-properties-form/file-properties-form.component';
import { DynamicDisableDirective } from '../../../../shared/directives/dynamic-disable/dynamic-disable.directive';
import { FileSizePipe } from '../../../../shared/pipes/file-size/file-size.pipe';
import { RepositoryBrowserClient } from '../../../../shared/providers/repository-browser-client/repository-browser-client.service';
import { ApplicationStateService, FolderActionsService } from '../../../../state';
import { TestApplicationState } from '../../../../state/test-application-state.mock';
import { TagPropertyLabelPipe } from '../../../pipes/tag-property-label/tag-property-label.pipe';
import { TagPropertyEditorResolverService } from '../../../providers/tag-property-editor-resolver/tag-property-editor-resolver.service';
import { ExpansionButtonComponent } from '../../shared/expansion-button/expansion-button.component';
import { ImagePreviewComponent } from '../../shared/image-preview/image-preview.component';
import { UploadWithPropertiesComponent } from '../../shared/upload-with-properties/upload-with-properties.component';
import { ValidationErrorInfoComponent } from '../../shared/validation-error-info/validation-error-info.component';
import { TagPropertyEditorHostComponent } from '../../tag-property-editor-host/tag-property-editor-host.component';
import { FileOrImageUrlTagPropertyEditor } from './file-or-image-url-tag-property-editor.component';

const FILE_A = getExampleFileData({ id: 4712 });
const FILE_B_REMOVED = getExampleFileData({ id: -10 });

const IMAGE_A = getExampleImageData({ id: 4711 });
const IMAGE_B_REMOVED = getExampleImageData({ id: 1 });

const SELECTED_ITEM_PATH_SELECTOR = '.path';
const UPLOAD_BUTTON_SELECTOR = '.browse-box__button--upload';
const EDIT_IMAGE_BUTTON_SELECTOR = '.edit_image';

/** Returns the DebugElement for the selectedItemPath element. */
const findSelectedItemPath = (fixture: ComponentFixture<TestComponent>): DebugElement => fixture.debugElement.query(By.css(SELECTED_ITEM_PATH_SELECTOR));

/** Checks if the selectedItemPath is displayed and if its value equals the expectedPath. */
function checkSelectedItemPath(fixture: ComponentFixture<TestComponent>, expectedPath: string): void {
    const pathElement = findSelectedItemPath(fixture);
    expect(pathElement).toBeTruthy();
    expect((pathElement.nativeElement as HTMLElement).innerText.endsWith(': ' + expectedPath)).toBeTruthy('selectedItemPath not displayed correctly');
}

/** Checks that the selectedItemPath is not displayed. */
function checkNoSelectedItemPath(fixture: ComponentFixture<TestComponent>): void {
    expect(findSelectedItemPath(fixture)).toBeFalsy();
}

/**
 * We don't add the FileOrImageUrlTagPropertyEditor directly to the template, but instead have it
 * created dynamically just like in the real use cases.
 *
 * This also tests if the mappings in the TagPropertyEditorResolverService are correct.
 */
@Component({
    template: `
        <tag-property-editor-host #tagPropEditorHost [tagPart]="tagPart"></tag-property-editor-host>
    `,
})
class TestComponent {
    @ViewChild('tagPropEditorHost', { static: true })
    tagPropEditorHost: TagPropertyEditorHostComponent;

    tagPart: TagPart;
}

class MockRepositoryBrowserClientService {
    openRepositoryBrowser(): void { }
}

class MockEditorOverlayService { }

class MockI18nService {
    translate(key: string | string, params?: any): string {
        return key;
    }
}

class MockFolderActions { }

class MockUploadConflictService { }

/**
 * TODO: Implement tests after feature release in Dec 2018.
 * Currently only the initilization tests for "URL (Image)" are done.
 */
describe('FileOrImageUrlTagPropertyEditor', () => {

    let appState: TestApplicationState;
    let client: GCMSTestRestClientService;
    let fileGet: jasmine.Spy<jasmine.Func>;
    let imageGet: jasmine.Spy<jasmine.Func>;
    let folderGet: jasmine.Spy<jasmine.Func>;

    let fileReturnValue: Observable<FileResponse>;
    let imageReturnValue: Observable<ImageResponse>;
    let folderReturnValue: Observable<FolderResponse>;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                FormsModule,
                GenticsUICoreModule.forRoot(),
                ReactiveFormsModule,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: ApiBase, useClass: MockApiBase },
                { provide: EditorOverlayService, useClass: MockEditorOverlayService },
                { provide: RepositoryBrowserClient, useClass: MockRepositoryBrowserClientService },
                { provide: I18nService, useClass: MockI18nService },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: UploadConflictService, useClass: MockUploadConflictService },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                TagPropertyEditorResolverService,
            ],
            declarations: [
                BrowseBoxComponent,
                DynamicDisableDirective,
                ExpansionButtonComponent,
                FileOrImageUrlTagPropertyEditor,
                FilePropertiesForm,
                FileSizePipe,
                ImagePreviewComponent,
                TagPropertyEditorHostComponent,
                TagPropertyLabelPipe,
                TestComponent,
                UploadWithPropertiesComponent,
                ValidationErrorInfoComponent,
            ],
        });

        client = TestBed.inject(GCMSRestClientService) as any;
        fileGet = client.file.get = jasmine.createSpy('file.get', client.file.get).and.callFake(() => fileReturnValue);
        imageGet = client.image.get = jasmine.createSpy('image.get', client.image.get).and.callFake(() => imageReturnValue);
        folderGet = client.folder.get = jasmine.createSpy('folder.get', client.folder.get).and.callFake(() => folderReturnValue);
    });

    beforeEach(() => {
        appState = TestBed.inject(ApplicationStateService) as any;

        setFeatures({
            [Feature.IMAGE_MANIPULATION2]: true,
            [Feature.ENABLE_UPLOAD_IN_TAGFILL]: true,
        });
    });

    function setFeatures(features: Partial<FeaturesState>): void {
        appState.mockState({
            features: features,
        });
    }

    describe('initialization', () => {

        beforeEach(() => {
            client.reset();
            fileGet.calls.reset();
            imageGet.calls.reset();
            folderGet.calls.reset();

            // Default value
            folderReturnValue = of({
                folder: { id: -99 },
            } as any);
        });

        function validateInit(
            fixture: ComponentFixture<TestComponent>,
            instance: TestComponent,
            tag: EditableTag,
            contextInfo?: Partial<TagEditorContext>,
        ): void {
            const context = getMockedTagEditorContext(tag, contextInfo);
            const tagPart = tag.tagType.parts[0];
            const tagProperty = tag.properties[tagPart.keyword] as FileTagPartProperty | ImageTagPartProperty;
            const origTagProperty = cloneDeep(tagProperty);

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(FileOrImageUrlTagPropertyEditor));
            expect(editorElement).toBeTruthy();

            // Set up the return values for folderApi.getItem().
            if (origTagProperty.type === TagPropertyType.FILE) {
                if (origTagProperty.fileId) {
                    // If an initial file is set, we need to add a response for loading the file.
                    if (origTagProperty.fileId === FILE_A.id) {
                        // Simulated existing file
                        fileReturnValue = of({
                            file: getExampleFileData({ id: origTagProperty.fileId }),
                        } as any);
                    } else {
                        // Simulated removed file
                        fileReturnValue = throwError({
                            messages: [{
                                message: 'File with ID 4712 does not exist.',
                                type: 'CRITICAL',
                            } ],
                            responseInfo: {
                                responseCode: ResponseCode.NOT_FOUND,
                                responseMessage: 'File with ID 4712 does not exist.',
                            },
                        });
                    }
                }
            } else if (origTagProperty.type === TagPropertyType.IMAGE) {
                if (origTagProperty.imageId) {
                    // If an initial image is set, we need to add a response for loading the image.
                    if (origTagProperty.imageId === IMAGE_A.id) {
                        // Simulated existing image
                        imageReturnValue = of({
                            image: getExampleImageData({ id: origTagProperty.imageId }),
                        } as any);
                    } else {
                        // Simulated removed image
                        imageReturnValue = throwError({
                            messages: [{
                                message: 'The specified image was not found.',
                                type: 'CRITICAL',
                            } ],
                            responseInfo: {
                                responseCode: ResponseCode.NOT_FOUND,
                                responseMessage: 'The specified image was not found.',
                            },
                        });
                    }
                }
            }

            folderReturnValue = of({
                folder: getExampleFolderData({ id: context.page.folderId }),
            } as any);

            const editor: FileOrImageUrlTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(() => null);
            fixture.detectChanges(); // detect changes after calling initTagPropertyEditor() (!= ngOnInit) s.t. the displayValue$ observable is subscribed to
            tick();

            // Make sure that the BrowseBox displays the correct initial values.
            const browseBoxElement = editorElement.query(By.directive(BrowseBoxComponent));
            expect(browseBoxElement).toBeTruthy();
            const browseBox = browseBoxElement.componentInstance as BrowseBoxComponent;
            expect(browseBox.label).toEqual(tagPart.name); // Here it is expected that the element is not mandatory.
            expect(browseBox.disabled).toBe(context.readOnly);

            if (origTagProperty.type === TagPropertyType.FILE) {
                if (origTagProperty.fileId) {
                    if (origTagProperty.fileId === FILE_A.id) {
                        expect(browseBox.displayValue).toEqual(FILE_A.name);
                        checkSelectedItemPath(fixture, 'GCN5 Demo > [Media] > [Files]');
                    } else {
                        // Simulated removed file
                        expect(browseBox.displayValue).toEqual('editor.file_not_found');
                        checkNoSelectedItemPath(fixture);
                    }
                } else {
                    expect(browseBox.displayValue).toEqual('editor.file_no_selection');
                    checkNoSelectedItemPath(fixture);
                }

                // If an image was pre-selected, make sure that is has been loaded.
                if (origTagProperty.fileId) {
                    expect(fileGet).toHaveBeenCalledWith(origTagProperty.fileId, { nodeId: context.node.id });
                } else {
                    expect(fileGet).not.toHaveBeenCalled();
                }

                // Make sure that the folder of the tagOwner has been loaded.
                expect(folderGet).toHaveBeenCalledWith(context.page.folderId, { nodeId: context.node.id });

            } else if (origTagProperty.type === TagPropertyType.IMAGE) {
                if (origTagProperty.imageId) {
                    if (origTagProperty.imageId === IMAGE_A.id) {
                        expect(browseBox.displayValue).toEqual(IMAGE_A.name);
                        checkSelectedItemPath(fixture, 'GCN5 Demo > [Media] > [Images]');
                    } else {
                        // Simulated removed image
                        expect(browseBox.displayValue).toEqual('editor.image_not_found');
                        checkNoSelectedItemPath(fixture);
                    }
                } else {
                    expect(browseBox.displayValue).toEqual('editor.image_no_selection');
                    checkNoSelectedItemPath(fixture);
                }

                // If an image was pre-selected, make sure that is has been loaded.

                if (origTagProperty.imageId) {
                    expect(imageGet).toHaveBeenCalledWith(origTagProperty.imageId, { nodeId: context.node.id });
                } else {
                    expect(imageGet).not.toHaveBeenCalled();
                }

                // Make sure that the folder of the tagOwner has been loaded.
                expect(folderGet).toHaveBeenCalledWith(context.page.folderId, { nodeId: context.node.id });
            }
        }

        it('initializes properly for unset TagPropertyType.FILE',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<FileTagPartProperty>([
                    {
                        type: TagPropertyType.FILE,
                        typeId: TagPartType.UrlFile,
                    },
                ]);

                validateInit(fixture, instance, tag);

                // The upload image button should be shown, the edit image button not.
                expect(fixture.debugElement.query(By.css(UPLOAD_BUTTON_SELECTOR))).toBeTruthy();
                expect(fixture.debugElement.query(By.css(EDIT_IMAGE_BUTTON_SELECTOR))).toBeFalsy();
            }),
        );

        it('initializes properly for set TagPropertyType.FILE',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<FileTagPartProperty>([
                    {
                        type: TagPropertyType.FILE,
                        typeId: TagPartType.UrlFile,
                        fileId: FILE_A.id,
                    },
                ]);

                validateInit(fixture, instance, tag);

                // The upload image button and the edit image button should be shown.
                expect(fixture.debugElement.query(By.css(UPLOAD_BUTTON_SELECTOR))).toBeTruthy();
                expect(fixture.debugElement.query(By.css(EDIT_IMAGE_BUTTON_SELECTOR))).toBeFalsy();
            }),
        );

        it('initializes properly for set TagPropertyType.FILE that is no longer available',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<FileTagPartProperty>([
                    {
                        type: TagPropertyType.FILE,
                        typeId: TagPartType.UrlFile,
                        fileId: FILE_B_REMOVED.id,
                    },
                ]);

                validateInit(fixture, instance, tag);

                // The upload image button and the edit image button should be shown.
                expect(fixture.debugElement.query(By.css(UPLOAD_BUTTON_SELECTOR))).toBeTruthy();
                expect(fixture.debugElement.query(By.css(EDIT_IMAGE_BUTTON_SELECTOR))).toBeFalsy();
            }),
        );

        it('initializes properly for unset TagPropertyType.IMAGE',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<ImageTagPartProperty>([
                    {
                        type: TagPropertyType.IMAGE,
                        typeId: TagPartType.UrlImage,
                    },
                ]);

                validateInit(fixture, instance, tag);

                // The upload image button should be shown, the edit image button not.
                expect(fixture.debugElement.query(By.css(UPLOAD_BUTTON_SELECTOR))).toBeTruthy();
                expect(fixture.debugElement.query(By.css(EDIT_IMAGE_BUTTON_SELECTOR))).toBeFalsy();
            }),
        );

        it('initializes properly for set TagPropertyType.IMAGE',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<ImageTagPartProperty>([
                    {
                        type: TagPropertyType.IMAGE,
                        typeId: TagPartType.UrlImage,
                        imageId: IMAGE_A.id,
                    },
                ]);

                validateInit(fixture, instance, tag);

                // The upload image button and the edit image button should be shown.
                expect(fixture.debugElement.query(By.css(UPLOAD_BUTTON_SELECTOR))).toBeTruthy();
                expect(fixture.debugElement.query(By.css(EDIT_IMAGE_BUTTON_SELECTOR))).toBeTruthy();
            }),
        );

        it('initializes properly for set TagPropertyType.IMAGE that is no longer available',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<ImageTagPartProperty>([
                    {
                        type: TagPropertyType.IMAGE,
                        typeId: TagPartType.UrlImage,
                        imageId: IMAGE_B_REMOVED.id,
                    },
                ]);

                validateInit(fixture, instance, tag);

                // The upload image button and the edit image button should be shown.
                expect(fixture.debugElement.query(By.css(UPLOAD_BUTTON_SELECTOR))).toBeTruthy();
                expect(fixture.debugElement.query(By.css(EDIT_IMAGE_BUTTON_SELECTOR))).toBeFalsy();
            }),
        );

        it('initializes properly for set TagPropertyType.IMAGE and imagemanipulation2 disabled',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<ImageTagPartProperty>([
                    {
                        type: TagPropertyType.IMAGE,
                        typeId: TagPartType.UrlImage,
                        imageId: IMAGE_A.id,
                    },
                ]);

                setFeatures({ [Feature.IMAGE_MANIPULATION2]: false, [Feature.ENABLE_UPLOAD_IN_TAGFILL]: true });
                validateInit(fixture, instance, tag);

                // The upload image button should be shown, the edit image button not.
                expect(fixture.debugElement.query(By.css(UPLOAD_BUTTON_SELECTOR))).toBeTruthy();
                expect(fixture.debugElement.query(By.css(EDIT_IMAGE_BUTTON_SELECTOR))).toBeFalsy();
            }),
        );

        it('initializes properly for set TagPropertyType.IMAGE and enable_image_upload_in_tagfill disabled',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<ImageTagPartProperty>([
                    {
                        type: TagPropertyType.IMAGE,
                        typeId: TagPartType.UrlImage,
                        imageId: IMAGE_A.id,
                    },
                ]);

                setFeatures({ [Feature.IMAGE_MANIPULATION2]: true, [Feature.ENABLE_UPLOAD_IN_TAGFILL]: false });
                validateInit(fixture, instance, tag);

                // The edit image button should be shown, the upload image button not.
                expect(fixture.debugElement.query(By.css(UPLOAD_BUTTON_SELECTOR))).toBeFalsy();
                expect(fixture.debugElement.query(By.css(EDIT_IMAGE_BUTTON_SELECTOR))).toBeTruthy();
            }),
        );

        it('is disabled for set TagPropertyType.IMAGE and context.readOnly=true',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<ImageTagPartProperty>([
                    {
                        type: TagPropertyType.IMAGE,
                        typeId: TagPartType.UrlImage,
                        imageId: IMAGE_A.id,
                    },
                ]);

                validateInit(fixture, instance, tag, { readOnly: true });

                // None of the buttons should be shown.
                const uploadButton: DebugElement = fixture.debugElement.query(By.css(UPLOAD_BUTTON_SELECTOR));
                expect(uploadButton).toBeTruthy();
                if (uploadButton) {
                    // eslint-disable-next-line @typescript-eslint/no-unused-expressions
                    expect(uploadButton.attributes['disabled']).toBeTruthy;
                }
                expect(fixture.debugElement.query(By.css(EDIT_IMAGE_BUTTON_SELECTOR))).toBeFalsy();
            }),
        );

    });

    describe('user input handling for TagPropertyType.FILE', () => {

        it('clearing selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for a file with previously unset selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for a file with previously set selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for a file and then cancelling, with previously unset selection works and does not trigger onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for a file and then cancelling, with previously set selection works and does not trigger onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('uploading a new file without selecting a folder uploads the file to the current folder and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('uploading a new file with selecting a folder uploads the file to the current folder and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

    });

    describe('user input handling for TagPropertyType.IMAGE', () => {

        it('clearing selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for an image with previously unset selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for an image with previously set selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for an image and then cancelling, with previously unset selection works and does not trigger onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for an image and then cancelling, with previously set selection works and does not trigger onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('uploading a new image without selecting a folder uploads the file to the current folder and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('uploading a new image with selecting a folder uploads the file to the current folder and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

    });

    describe('writeChangedValues()', () => {

        it('handles writeChangedValues() correctly for TagPropertyType.FILE',
            componentTest(() => TestComponent, () => {

            }),
        );

        it('handles writeChangedValues() correctly for TagPropertyType.IMAGE',
            componentTest(() => TestComponent, () => {

            }),
        );

    });

});
