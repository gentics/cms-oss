import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowseBoxComponent } from '@gentics/cms-components';
import { TagEditorContext } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    FolderResponse,
    FolderTagPartProperty,
    ResponseCode,
    TagPart,
    TagPartType,
    TagPropertyType,
} from '@gentics/cms-models';
import { getExampleFolderData } from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { Observable, of, throwError } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../../testing';
import { getMockedTagEditorContext, mockEditableTag } from '../../../../../testing/test-tag-editor-data.mock';
import { ApiBase } from '../../../../core/providers/api';
import { MockApiBase } from '../../../../core/providers/api/api-base.mock';
import { UploadConflictService } from '../../../../core/providers/upload-conflict/upload-conflict.service';
import { EditorOverlayService } from '../../../../editor-overlay/providers/editor-overlay.service';
import { FilePropertiesComponent } from '../../../../shared/components/file-properties/file-properties.component';
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
import { FolderUrlTagPropertyEditor } from './folder-url-tag-property-editor.component';

const FOLDER_A = getExampleFolderData({ id: 115 });
const FOLDER_B_REMOVED = getExampleFolderData({ id: -10 });

/**
 * We don't add the FolderUrlTagPropertyEditor directly to the template, but instead have it
 * created dynamically just like in the real use cases.
 *
 * This also tests if the mappings in the TagPropertyEditorResolverService are correct.
 */
@Component({
    template: `
        <tag-property-editor-host #tagPropEditorHost [tagPart]="tagPart"></tag-property-editor-host>
    `,
    standalone: false,
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
 */
describe('FolderUrlTagPropertyEditor', () => {

    let client: GCMSTestRestClientService;
    let folderGet: jasmine.Spy<jasmine.Func>;

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
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: UploadConflictService, useClass: MockUploadConflictService },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                TagPropertyEditorResolverService,
            ],
            declarations: [
                BrowseBoxComponent,
                DynamicDisableDirective,
                ExpansionButtonComponent,
                FolderUrlTagPropertyEditor,
                FilePropertiesComponent,
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
        folderGet = client.folder.get = jasmine.createSpy('folder.get', client.folder.get).and.callFake(() => folderReturnValue);
    });

    beforeEach(() => {
        client.reset();
        folderGet.calls.reset();

        // Default value
        folderReturnValue = of({
            folder: { id: -99 },
        } as any);
    });

    describe('initialization', () => {

        function validateInit(
            fixture: ComponentFixture<TestComponent>,
            instance: TestComponent,
            tag: EditableTag,
            contextInfo?: Partial<TagEditorContext>,
        ): void {
            const context = getMockedTagEditorContext(tag, contextInfo);
            const tagPart = tag.tagType.parts[0];
            const tagProperty = tag.properties[tagPart.keyword] as FolderTagPartProperty;
            const origTagProperty = cloneDeep(tagProperty);

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(FolderUrlTagPropertyEditor));
            expect(editorElement).toBeTruthy();

            // Set up the return values for folder.get
            if (origTagProperty.folderId) {
                // If an initial folder is set, we need to add a response for loading the folder.
                if (origTagProperty.folderId === FOLDER_A.id) {
                    // Simulated existing folder
                    folderReturnValue = of({
                        folder: getExampleFolderData({ id: origTagProperty.folderId }),
                    } as any);
                } else {
                    // Simulated removed folder
                    folderReturnValue = throwError({
                        messages: [{
                            message: 'The specified folder was not found.',
                            type: 'CRITICAL',
                        }],
                        responseInfo: {
                            responseCode: ResponseCode.NOT_FOUND,
                            responseMessage: 'The specified folder was not found.',
                        },
                    });
                }
            } else {
                // Simulated removed folder
                folderReturnValue = throwError({
                    messages: [{
                        message: 'The specified folder was not found.',
                        type: 'CRITICAL',
                    }],
                    responseInfo: {
                        responseCode: ResponseCode.NOT_FOUND,
                        responseMessage: 'The specified folder was not found.',
                    },
                });
            }

            const editor: FolderUrlTagPropertyEditor = editorElement.componentInstance;
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

            if (origTagProperty.folderId) {
                if (origTagProperty.folderId === FOLDER_A.id) {
                    expect(browseBox.displayValue).toEqual(FOLDER_A.name);
                } else {
                    // Simulated removed file
                    expect(browseBox.displayValue).toEqual('editor.folder_not_found');
                }
            } else {
                expect(browseBox.displayValue).toEqual('editor.folder_no_selection');
            }

            // If an image was pre-selected, make sure that is has been loaded.
            if (origTagProperty.folderId) {
                expect(folderGet).toHaveBeenCalledWith(origTagProperty.folderId, { nodeId: context.node.id });
            }
        }

        it('initializes properly for unset folder',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<FolderTagPartProperty>([
                    {
                        type: TagPropertyType.FOLDER,
                        typeId: TagPartType.UrlFolder,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for set folder',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<FolderTagPartProperty>([
                    {
                        type: TagPropertyType.FOLDER,
                        typeId: TagPartType.UrlFolder,
                        folderId: FOLDER_A.id,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for set folder that is no longer available',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<FolderTagPartProperty>([
                    {
                        type: TagPropertyType.FOLDER,
                        typeId: TagPartType.UrlFolder,
                        folderId: FOLDER_B_REMOVED.id,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

    });

    describe('user input handling', () => {

        it('clearing selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
                // Also make sure that a new file cannot be uploaded in and that no subfolder can be created in this case.
            }),
        );

        it('browsing for a folder with previously unset selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for a folder with previously set selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for a folder and then cancelling, with previously unset selection works and does not trigger onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for a folder and then cancelling, with previously set selection works and does not trigger onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('uploading multiple new files to the selected folder works',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('creating a subfolder works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

    });

    describe('writeChangedValues()', () => {

        it('handles writeChangedValues() correctly',
            componentTest(() => TestComponent, () => {

            }),
        );

    });

});
