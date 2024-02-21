import { Component, Pipe, PipeTransform, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowseBoxComponent } from '@gentics/cms-components';
import { EditableTag, FolderResponse, PageResponse, PageTagPartProperty, TagEditorContext, TagPart, TagPartType, TagPropertyType } from '@gentics/cms-models';
import { getExamplePageData } from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { Observable, of, throwError } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../../testing';
import { getMockedTagEditorContext, mockEditableTag } from '../../../../../testing/test-tag-editor-data.mock';
import { ApiBase } from '../../../../core/providers/api';
import { MockApiBase } from '../../../../core/providers/api/api-base.mock';
import { I18nService } from '../../../../core/providers/i18n/i18n.service';
import { EditorOverlayService } from '../../../../editor-overlay/providers/editor-overlay.service';
import { FilePropertiesForm } from '../../../../shared/components/file-properties-form/file-properties-form.component';
import { DynamicDisableDirective } from '../../../../shared/directives/dynamic-disable/dynamic-disable.directive';
import { FileSizePipe } from '../../../../shared/pipes/file-size/file-size.pipe';
import { RepositoryBrowserClient } from '../../../../shared/providers/repository-browser-client/repository-browser-client.service';
import { ApplicationStateService, FolderActionsService } from '../../../../state';
import { TestApplicationState } from '../../../../state/test-application-state.mock';
import { TagPropertyLabelPipe } from '../../../pipes/tag-property-label/tag-property-label.pipe';
import { TagPropertyEditorResolverService } from '../../../providers/tag-property-editor-resolver/tag-property-editor-resolver.service';
import { ValidationErrorInfoComponent } from '../../shared/validation-error-info/validation-error-info.component';
import { TagPropertyEditorHostComponent } from '../../tag-property-editor-host/tag-property-editor-host.component';
import { PageUrlTagPropertyEditor } from './page-url-tag-property-editor.component';

const PAGE_A = getExamplePageData({ id: 95 });
const PAGE_B_REMOVED = getExamplePageData({ id: -10 });

/**
 * We don't add the PageUrlTagPropertyEditor directly to the template, but instead have it
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

@Pipe({ name: 'i18n' })
class MockI18nPipe implements PipeTransform {
    transform(query: string, ...args: any[]): string {
        return query;
    }
}

class MockFolderActions { }

/**
 * TODO: Implement tests after feature release in Dec 2018.
 */
describe('PageUrlTagPropertyEditor', () => {

    let client: GCMSTestRestClientService;
    let pageGet: jasmine.Spy<jasmine.Func>;
    let folderGet: jasmine.Spy<jasmine.Func>;

    let pageReturnValue: Observable<PageResponse>;
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
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                TagPropertyEditorResolverService,
            ],
            declarations: [
                BrowseBoxComponent,
                DynamicDisableDirective,
                PageUrlTagPropertyEditor,
                FilePropertiesForm,
                FileSizePipe,
                TagPropertyEditorHostComponent,
                TagPropertyLabelPipe,
                TestComponent,
                ValidationErrorInfoComponent,
                MockI18nPipe,
            ],
        });

        client = TestBed.inject(GCMSRestClientService) as any;
        pageGet = client.page.get = jasmine.createSpy('page.get', client.page.get).and.callFake(() => pageReturnValue);
        folderGet = client.folder.get = jasmine.createSpy('folder.get', client.folder.get).and.callFake(() => folderReturnValue);
    });

    describe('initialization', () => {

        beforeEach(() => {
            client.reset();
            pageGet.calls.reset();
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
            const tagProperty = tag.properties[tagPart.keyword] as PageTagPartProperty;
            const origTagProperty = cloneDeep(tagProperty);

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(PageUrlTagPropertyEditor));
            expect(editorElement).toBeTruthy();

            // Set up the return values for folderApi.getItem().
            if (origTagProperty.pageId) {
                // If an initial page is set, we need to add a response for loading the page.
                if (origTagProperty.pageId === PAGE_A.id) {
                    // Simulated existing page
                    pageReturnValue = of({
                        page: getExamplePageData({ id: origTagProperty.pageId }),
                    } as any)
                } else {
                    // Simulated removed page
                    pageReturnValue = throwError({
                        messages: [ {
                            message: 'The specified page was not found.',
                            type: 'CRITICAL',
                        } ],
                        responseInfo: {
                            responseCode: 'NOTFOUND',
                            responseMessage: 'The specified page was not found.',
                        },
                    });
                }
            } else {
                // Simulated removed page
                pageReturnValue = throwError({
                    messages: [ {
                        message: 'The specified page was not found.',
                        type: 'CRITICAL',
                    } ],
                    responseInfo: {
                        responseCode: 'NOTFOUND',
                        responseMessage: 'The specified page was not found.',
                    },
                });
            }

            const editor: PageUrlTagPropertyEditor = editorElement.componentInstance;
            editor.initTagPropertyEditor(tagPart, tag, tagProperty, context);
            editor.registerOnChange(() => null);
            fixture.detectChanges(); // detect changes after calling initTagPropertyEditor() (!= ngOnInit) s.t. the displayValue$ observable is subscribed to
            tick();

            // Make sure that the label is displayed correctly.
            const labelElement = editorElement.query(By.css('.tag-prop-label'));
            expect(labelElement).toBeTruthy();
            expect(labelElement.nativeElement.textContent).toEqual(tagPart.name); // Here it is expected that the element is not mandatory.

            // Make sure that the BrowseBox displays the correct initial values.
            const browseBoxElement = editorElement.query(By.directive(BrowseBoxComponent));
            expect(browseBoxElement).toBeTruthy();
            const browseBox = browseBoxElement.componentInstance as BrowseBoxComponent;

            expect(browseBox.label).toEqual('tag_editor.internal_page');
            expect(browseBox.disabled).toBe(context.readOnly);


            if (origTagProperty.pageId) {
                if (origTagProperty.pageId === PAGE_A.id) {
                    expect(browseBox.displayValue).toEqual(PAGE_A.name);
                } else {
                    // Simulated removed file
                    expect(browseBox.displayValue).toEqual('editor.page_not_found');
                }
            } else {
                expect(browseBox.displayValue).toEqual('editor.page_no_selection');
            }

            // If an image was pre-selected, make sure that is has been loaded.
            if (origTagProperty.pageId) {
                expect(pageGet).toHaveBeenCalledWith(origTagProperty.pageId, { nodeId: context.node.id });
            } else {
                expect(pageGet).not.toHaveBeenCalled();
            }
        }

        it('initializes properly for unset TagPropertyType.PAGE and internal page',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<PageTagPartProperty>([
                    {
                        type: TagPropertyType.PAGE,
                        typeId: TagPartType.UrlPage,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for set internal page and internal page',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<PageTagPartProperty>([
                    {
                        type: TagPropertyType.PAGE,
                        typeId: TagPartType.UrlPage,
                        pageId: PAGE_A.id,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for set internal page that is no longer available and internal page',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<PageTagPartProperty>([
                    {
                        type: TagPropertyType.PAGE,
                        typeId: TagPartType.UrlPage,
                        pageId: PAGE_B_REMOVED.id,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for set external page',
            componentTest(() => TestComponent, (fixture, instance) => {
            }),
        );

    });

    describe('user input handling', () => {

        it('clearing interal page selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for an internal page with previously unset value works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for an internal page with previously set internal page works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for an internal page with previously set external page works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for an internal page and then cancelling, with previously unset value works and does not trigger onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for an internal page and then cancelling, with previously set internal page works and does not trigger onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for an internal page and then cancelling, with previously set external page works and does not trigger onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('setting an external page with previously unset value works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('setting an external page with previously set internal page works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('setting an external page with previously set external page works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

    });

    describe('writeChangedValues()', () => {

        it('handles writeChangedValues() correctly for internal page',
            componentTest(() => TestComponent, () => {

            }),
        );

        it('handles writeChangedValues() correctly for external page',
            componentTest(() => TestComponent, () => {

            }),
        );

    });

});
