import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowseBoxComponent } from '@gentics/cms-components';
import { EditableTag, PageTagTagPartProperty, TagEditorContext, TagPart, TagPartType, TagPropertyType, TemplateTagTagPartProperty } from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { cloneDeep } from 'lodash-es';
import { Observable, of, throwError } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../../testing';
import { getExamplePageData, getExampleTemplateData } from '../../../../../testing/test-data.mock';
import { getMockedTagEditorContext, mockEditableTag } from '../../../../../testing/test-tag-editor-data.mock';
import { Api, ApiBase } from '../../../../core/providers/api';
import { MockApiBase } from '../../../../core/providers/api/api-base.mock';
import { I18nService } from '../../../../core/providers/i18n/i18n.service';
import { EditorOverlayService } from '../../../../editor-overlay/providers/editor-overlay.service';
import { RepositoryBrowserClient } from '../../../../shared/providers/repository-browser-client/repository-browser-client.service';
import { ApplicationStateService, FolderActionsService } from '../../../../state';
import { TestApplicationState } from '../../../../state/test-application-state.mock';
import { TagPropertyLabelPipe } from '../../../pipes/tag-property-label/tag-property-label.pipe';
import { TagPropertyEditorResolverService } from '../../../providers/tag-property-editor-resolver/tag-property-editor-resolver.service';
import { ValidationErrorInfo } from '../../shared/validation-error-info/validation-error-info.component';
import { TagPropertyEditorHostComponent } from '../../tag-property-editor-host/tag-property-editor-host.component';
import { TagRefTagPropertyEditor } from './tagref-tag-property-editor.component';

const PAGE_A = getExamplePageData({ id: 95 });
const PAGE_B_REMOVED = getExamplePageData({ id: -10 });

const TEMPLATE_A = getExampleTemplateData({ id: 115, masterId: undefined });
const TEMPLATE_B_REMOVED = getExampleTemplateData({ id: -11, masterId: undefined });

const TAG_TYPE = {
    constructId: 65,
    name: 'Aloha Text (long)',
};

/**
 * We don't add the CheckboxTagPropertyEditor directly to the template, but instead have it
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

/**
 * TODO: Implement tests after feature release in Dec 2018.
 */
describe('TagRefTagPropertyEditor', () => {

    let getItemSpy: jasmine.Spy;
    let getTagTypeSpy: jasmine.Spy;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
                FormsModule,
                ReactiveFormsModule,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: ApiBase, useClass: MockApiBase },
                { provide: EditorOverlayService, useClass: MockEditorOverlayService },
                { provide: RepositoryBrowserClient, useClass: MockRepositoryBrowserClientService },
                { provide: I18nService, useClass: MockI18nService },
                { provide: FolderActionsService, useClass: MockFolderActions },
                Api,
                TagPropertyEditorResolverService,
            ],
            declarations: [
                BrowseBoxComponent,
                TagPropertyEditorHostComponent,
                TagPropertyLabelPipe,
                TagRefTagPropertyEditor,
                TestComponent,
                ValidationErrorInfo,
            ],
        });
    });

    beforeEach(() => {
        const api = TestBed.get(Api) as Api;
        getItemSpy = spyOn(api.folders, 'getItem');
        getTagTypeSpy = spyOn(api.tagType, 'getTagType');
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
            const tagProperty = tag.properties[tagPart.keyword] as PageTagTagPartProperty | TemplateTagTagPartProperty;
            const origTagProperty = cloneDeep(tagProperty);

            instance.tagPart = tagPart;
            fixture.detectChanges();
            tick();

            const editorElement = fixture.debugElement.query(By.directive(TagRefTagPropertyEditor));
            expect(editorElement).toBeTruthy();

            // Set up the return values for folderApi.getItem().
            const getItemReturnValues: Observable<any>[] = [];
            if (origTagProperty.type === TagPropertyType.PAGETAG) {
                if (origTagProperty.pageId) {
                    // If an initial page is set, we need to add a response for loading the page.
                    if (origTagProperty.pageId === PAGE_A.id) {
                        // Simulated existing page
                        getItemReturnValues.push(
                            of({
                                page: getExamplePageData({ id: origTagProperty.pageId }),
                            }),
                            of({
                                page: getExamplePageData({ id: origTagProperty.pageId }),
                            }),
                        );
                        getTagTypeSpy.and.returnValue(of({ construct: TAG_TYPE }));
                    } else {
                        // Simulated removed page
                        getItemReturnValues.push(
                            throwError({
                                messages: [ {
                                    message: 'The specified page was not found.',
                                    type: 'CRITICAL',
                                } ],
                                responseInfo: {
                                    responseCode: 'NOTFOUND',
                                    responseMessage: 'The specified page was not found.',
                                },
                            }),
                            throwError({
                                messages: [ {
                                    message: 'The specified page was not found.',
                                    type: 'CRITICAL',
                                } ],
                                responseInfo: {
                                    responseCode: 'NOTFOUND',
                                    responseMessage: 'The specified page was not found.',
                                },
                            }),
                        );
                    }
                } else {
                    // Simulated removed page
                    getItemReturnValues.push(
                        throwError({
                            messages: [ {
                                message: 'The specified page was not found.',
                                type: 'CRITICAL',
                            } ],
                            responseInfo: {
                                responseCode: 'NOTFOUND',
                                responseMessage: 'The specified page was not found.',
                            },
                        }),
                    );
                }
            } else if (origTagProperty.type === TagPropertyType.TEMPLATETAG) {
                if (origTagProperty.templateId) {
                    // If an initial template is set, we need to add a response for loading the template.
                    if (origTagProperty.templateId === TEMPLATE_A.id) {
                        // Simulated existing template
                        getItemReturnValues.push(
                            of({
                                template: getExampleTemplateData({ id: origTagProperty.templateId, masterId: undefined }),
                            }),
                            of({
                                template: getExampleTemplateData({ id: origTagProperty.templateId, masterId: undefined }),
                            }),
                        );
                        getTagTypeSpy.and.returnValue(of({ construct: TAG_TYPE }));
                    } else {
                        // Simulated removed template
                        getItemReturnValues.push(
                            throwError({
                                messages: [ {
                                    message: `Could not find template with ID ${origTagProperty.templateId}.`,
                                    type: 'WARNING',
                                } ],
                                responseInfo: {
                                    responseCode: 'NOTFOUND',
                                    responseMessage: `Could not find template with ID ${origTagProperty.templateId}.`,
                                },
                            }),
                            throwError({
                                messages: [ {
                                    message: `Could not find template with ID ${origTagProperty.templateId}.`,
                                    type: 'WARNING',
                                } ],
                                responseInfo: {
                                    responseCode: 'NOTFOUND',
                                    responseMessage: `Could not find template with ID ${origTagProperty.templateId}.`,
                                },
                            }),
                        );
                    }
                } else {
                    // Simulated removed template
                    getItemReturnValues.push(
                        throwError({
                            messages: [ {
                                message: `Could not find template with ID ${origTagProperty.templateId}.`,
                                type: 'WARNING',
                            } ],
                            responseInfo: {
                                responseCode: 'NOTFOUND',
                                responseMessage: `Could not find template with ID ${origTagProperty.templateId}.`,
                            },
                        }),
                    );
                }
            }
            getItemSpy.and.returnValues(...getItemReturnValues);

            const editor: TagRefTagPropertyEditor = editorElement.componentInstance;
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

            if (origTagProperty.type === TagPropertyType.PAGETAG) {
                if (origTagProperty.pageId) {
                    const tag = PAGE_A.tags['content'];
                    if (origTagProperty.pageId === PAGE_A.id && origTagProperty.contentTagId === tag.id) {
                        expect(browseBox.displayValue).toEqual(`${tag.name} - ${TAG_TYPE.name}`);
                        expect(getTagTypeSpy.calls.argsFor(0)[0]).toEqual(tag.constructId, 'wrong ID used for tag type');
                    } else {
                        // Simulated removed tag
                        expect(browseBox.displayValue).toEqual('editor.tag_not_found_in_page');
                    }
                } else {
                    expect(browseBox.displayValue).toEqual('editor.tag_no_selection');
                }

                // If an image was pre-selected, make sure that is has been loaded.
                let callIndex = 1;
                if (origTagProperty.pageId) {
                    expect(getItemSpy.calls.argsFor(0)[0]).toEqual(origTagProperty.pageId, 'wrong page ID loaded');
                    expect(getItemSpy.calls.argsFor(0)[1]).toEqual('page', 'wrong item type loaded');
                    expect(getItemSpy.calls.argsFor(0)[2]).toEqual({ folder: false, template: false }, 'wrong options');
                    ++callIndex;
                }

                expect(getItemSpy.calls.count()).toBe(callIndex);


            } else if (origTagProperty.type === TagPropertyType.TEMPLATETAG) {
                if (origTagProperty.templateId) {
                    const tag = TEMPLATE_A.templateTags['content'];
                    if (origTagProperty.templateId === TEMPLATE_A.id && origTagProperty.templateTagId === tag.id) {
                        expect(browseBox.displayValue).toEqual(`${tag.name} - ${TAG_TYPE.name}`);
                        expect(getTagTypeSpy.calls.argsFor(0)[0]).toEqual(tag.constructId, 'wrong ID used for tag type');
                    } else {
                        // Simulated removed tag
                        expect(browseBox.displayValue).toEqual('editor.tag_not_found_in_template');
                    }
                } else {
                    expect(browseBox.displayValue).toEqual('editor.tag_no_selection');
                }

                // If an image was pre-selected, make sure that is has been loaded.
                let callIndex = 1;
                if (origTagProperty.templateId) {
                    expect(getItemSpy.calls.argsFor(0)[0]).toEqual(origTagProperty.templateId, 'wrong template ID loaded');
                    expect(getItemSpy.calls.argsFor(0)[1]).toEqual('template', 'wrong item type loaded');
                    ++callIndex;
                }

                expect(getItemSpy.calls.count()).toBe(callIndex);
            }


            getItemSpy.and.stub();
            getItemSpy.calls.reset();
            getTagTypeSpy.and.stub();
            getTagTypeSpy.calls.reset();
        }

        it('initializes properly for unset PAGETAG',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<PageTagTagPartProperty>([
                    {
                        type: TagPropertyType.PAGETAG,
                        typeId: TagPartType.TagPage,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for set PAGETAG',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<PageTagTagPartProperty>([
                    {
                        type: TagPropertyType.PAGETAG,
                        typeId: TagPartType.TagPage,
                        pageId: PAGE_A.id,
                        contentTagId: PAGE_A.tags['content'].id,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for set PAGETAG with page that is no longer available',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<PageTagTagPartProperty>([
                    {
                        type: TagPropertyType.PAGETAG,
                        typeId: TagPartType.TagPage,
                        pageId: PAGE_B_REMOVED.id,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for set PAGETAG with page that is no longer available',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<PageTagTagPartProperty>([
                    {
                        type: TagPropertyType.PAGETAG,
                        typeId: TagPartType.TagPage,
                        pageId: PAGE_A.id,
                        contentTagId: -10,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for unset TEMPLATETAG',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<TemplateTagTagPartProperty>([
                    {
                        type: TagPropertyType.TEMPLATETAG,
                        typeId: TagPartType.TagTemplate,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for set TEMPLATETAG',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<TemplateTagTagPartProperty>([
                    {
                        type: TagPropertyType.TEMPLATETAG,
                        typeId: TagPartType.TagTemplate,
                        templateId: TEMPLATE_A.id,
                        templateTagId: TEMPLATE_A.templateTags['content'].id,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for set TEMPLATETAG with template that is no longer available',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<TemplateTagTagPartProperty>([
                    {
                        type: TagPropertyType.TEMPLATETAG,
                        typeId: TagPartType.TagTemplate,
                        templateId: TEMPLATE_B_REMOVED.id,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

        it('initializes properly for set TEMPLATETAG with tag that is no longer available',
            componentTest(() => TestComponent, (fixture, instance) => {
                const tag: EditableTag = mockEditableTag<TemplateTagTagPartProperty>([
                    {
                        type: TagPropertyType.TEMPLATETAG,
                        typeId: TagPartType.TagTemplate,
                        templateId: TEMPLATE_A.id,
                        templateTagId: -10,
                    },
                ]);

                validateInit(fixture, instance, tag);
            }),
        );

    });

    // It is probably best to define a function that can handle the browsing tests for PAGETAG and TEMPLATETAG
    describe('user input handling for PAGETAG', () => {

        it('clearing selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for PAGETAG with previously unset selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for PAGETAG with previously set selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for PAGETAG and then cancelling, with previously unset selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for PAGETAG and then cancelling, with previously set selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

    });

    describe('user input handling for TEMPLATETAG', () => {

        it('clearing selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for TEMPLATETAG with previously unset selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for TEMPLATETAG with previously set selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for TEMPLATETAG and then cancelling, with previously unset selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

        it('browsing for TEMPLATETAG and then cancelling, with previously set selection works and triggers onChangeFn',
            componentTest(() => TestComponent, () => {
            }),
        );

    });

    describe('writeChangedValues()', () => {

        it('handles writeChangedValues() correctly for PAGETAG',
            componentTest(() => TestComponent, () => {

            }),
        );

        it('handles writeChangedValues() correctly for TEMPLATETAG',
            componentTest(() => TestComponent, () => {

            }),
        );

    });

});
