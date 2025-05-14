import { TestBed, fakeAsync } from '@angular/core/testing';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { EditorOverlayService } from '@editor-ui/app/editor-overlay/providers/editor-overlay.service';
import { RepositoryBrowserClient, UserAgentRef } from '@editor-ui/app/shared/providers';
import { ApplicationStateService, STATE_MODULES, SetUILanguageAction } from '@editor-ui/app/state';
import { TagEditorContext, VariableTagEditorContext } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    Node,
    Page,
    Raw,
    StringTagPartProperty,
    Tag,
    TagType,
} from '@gentics/cms-models';
import { getExampleNodeData, getExamplePageData } from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { ApiBase } from '@gentics/cms-rest-clients-angular';
import { ModalService } from '@gentics/ui-core';
import { TranslateService } from '@ngx-translate/core';
import { NgxsModule } from '@ngxs/store';
import { cloneDeep } from 'lodash-es';
import { NEVER, Observable } from 'rxjs';
import { getExampleEditableTag } from '../../../../testing/test-tag-editor-data.mock';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { TagEditorContextImpl } from '../../common/impl/tag-editor-context-impl';
import { TranslatorImpl } from '../../common/impl/translator-impl';
import { TagEditorModal } from '../../components/tag-editor-modal/tag-editor-modal.component';
import { EditTagInfo, TagEditorService } from './tag-editor.service';

describe('TagEditorService', () => {

    let state: TestApplicationState;
    let tagEditorService: TagEditorService;
    let userAgentRef: MockUserAgentRef;
    let entityResolver: MockEntityResolver;
    let editorOverlayService: MockEditorOverlayService;
    let repositoryBrowserClient: MockRepositoryBrowserClient;
    let modalService: ModalService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            declarations: [
                TagEditorModal,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: EditorOverlayService, useClass: MockEditorOverlayService },
                { provide: EntityResolver, useClass: MockEntityResolver },
                { provide: RepositoryBrowserClient, useClass: MockRepositoryBrowserClient },
                { provide: TranslateService, useClass: MockTranslateService },
                { provide: UserAgentRef, useClass: MockUserAgentRef },
                { provide: ModalService, useClass: MockModalService },
                { provide: ApiBase, useClass: MockBaseApiService },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                TagEditorService,
            ],
        });

        state = TestBed.inject(ApplicationStateService) as any;
        editorOverlayService = TestBed.inject(EditorOverlayService) as any;
        userAgentRef = TestBed.inject(UserAgentRef) as any;
        entityResolver = TestBed.inject(EntityResolver) as any;
        repositoryBrowserClient = TestBed.inject(RepositoryBrowserClient) as any;
        tagEditorService = TestBed.inject(TagEditorService);
        modalService = TestBed.inject(ModalService);

        state.mockState({
            editor: {
                nodeId: 4711,
            },
            ui: {
                language: 'en',
            },
        });
    });

    /**
     * Generates initialization data for creating a new TagEditorContext. For convenience,
     * the return value is typed, such that tagOwner is always a page.
     */
    function getTagEditorContextInitData(): EditTagInfo & { tagOwner: Page<Raw> } {
        const data = getTagEditorInitData();
        return {
            tag: data.tag,
            tagType: data.tagType,
            tagOwner: data.page,
            node: getExampleNodeData({ id: state.now.editor.nodeId }),
            readOnly: false,
            withDelete: false,
        };
    }

    /** Checks that the tagEditorContext has been set up as expected */
    function checkTagEditorContext(tagEditorContext: TagEditorContext, expectedData: EditTagInfo): void {
        const expectedEditableTag: EditableTag = {
            ...expectedData.tag,
            tagType: expectedData.tagType,
        };
        const expectedVarContext: VariableTagEditorContext = {
            uiLanguage: state.now.ui.language,
        };

        expect(tagEditorContext instanceof TagEditorContextImpl).toBeTruthy();
        expect(tagEditorContext.editedTag).toEqual(expectedEditableTag);
        expect(tagEditorContext.page).toEqual(expectedData.tagOwner as any);
        expect(tagEditorContext.node).toEqual(expectedData.node);
        expect(tagEditorContext.readOnly).toBe(expectedData.readOnly);
        expect(tagEditorContext.sid).toBe(state.now.auth.sid);
        expect(tagEditorContext.translator instanceof TranslatorImpl).toBeTruthy();
        let actualContext: VariableTagEditorContext = null;
        tagEditorContext.variableContext.subscribe(context => actualContext = context);
        expect(actualContext).toEqual(expectedVarContext);
    }

    it('openTagEditor() opens the modal correctly', fakeAsync(async () => {
        const data = getTagEditorContextInitData();
        const copy = structuredClone(data);

        const modalSpy = spyOn(modalService, 'fromComponent').and.callFake(() => {
            return Promise.resolve({
                element: null,
                instance: null,
                open: () => Promise.resolve({
                    doDelete: false,
                    tag: createEditedTag({
                        ...copy.tag,
                        tagType: copy.tagType,
                    }),
                }),
            });
        });

        const editorResult = await tagEditorService.openTagEditor(data.tag, data.tagType, data.tagOwner, { withDelete: false });

        expect(modalSpy).toHaveBeenCalled();

        // Make sure that the returned promise resolves to a tag without the tagType property set
        const expectedFinalTag = structuredClone(data.tag);
        (expectedFinalTag.properties['property0'] as StringTagPartProperty).stringValue = 'modified value';

        expect(editorResult.doDelete).toEqual(false);
        expect(editorResult.tag).toEqual(expectedFinalTag as any);
    }));

    it('openTagEditor() uses the tagEditorHost to open the tag editor and relays a promise rejection correctly', fakeAsync(async () => {
        const data = getTagEditorInitData();
        const openTagEditorSpy = spyOn(tagEditorService, 'openTagEditor').and.returnValue(Promise.reject(undefined));

        try {
            await tagEditorService.openTagEditor(data.tag, data.tagType, data.page);
            expect(false).toBe(true); // Should always fail
        } catch (error) {
            expect(error).toBeUndefined();
        }

        expect(openTagEditorSpy).toHaveBeenCalled();
    }));

    it('openRepositoryBrowser() works fine with right parameters', fakeAsync(() => {
        const data = getTagEditorContextInitData();
        const tagEditorContext = tagEditorService.createTagEditorContext(data);

        const openRepositoryBrowserSpy: any = spyOn(repositoryBrowserClient, 'openRepositoryBrowser');

        tagEditorContext.gcmsUiServices.openRepositoryBrowser({ allowedSelection: 'page', selectMultiple: false });

        expect(openRepositoryBrowserSpy).toHaveBeenCalledWith({ allowedSelection: 'page', selectMultiple: false });
    }));

    it('openImageEditor() works fine with right parameters', fakeAsync(() => {
        const data = getTagEditorContextInitData();
        const tagEditorContext = tagEditorService.createTagEditorContext(data);

        const openImageEditorSpy: any = spyOn(editorOverlayService, 'editImage');

        tagEditorContext.gcmsUiServices.openImageEditor({ nodeId: 1, imageId: 415 });

        expect(openImageEditorSpy).toHaveBeenCalledWith({ nodeId: 1, itemId: 415 });
    }));

    it('createTagEditorContext() creates a new TagEditorContext with the correct values', fakeAsync(() => {
        const data = getTagEditorContextInitData();
        const expectedData = cloneDeep(data);

        const tagEditorContext = tagEditorService.createTagEditorContext(data);
        checkTagEditorContext(tagEditorContext, expectedData);
    }));

    it('createTagEditorContext() denormalizes the entities when creating a new TagEditorContext', fakeAsync(() => {
        const data = getTagEditorContextInitData();
        const origPage = data.tagOwner;
        const origNode = data.node;

        const tagEditorContext = tagEditorService.createTagEditorContext(data);
        expect(entityResolver.denormalizeEntity).toHaveBeenCalledTimes(2);
        expect(entityResolver.denormalizeEntity.calls.argsFor(0)).toEqual(['page', origPage]);
        expect(entityResolver.denormalizeEntity.calls.argsFor(1)).toEqual(['node', origNode]);

        expect(tagEditorContext.page).not.toBe(origPage);
        expect(tagEditorContext.page).toBe(entityResolver.denormalizeEntity.calls.all()[0].returnValue);
        expect(tagEditorContext.node).not.toBe(origNode);
        expect(tagEditorContext.node).toBe(entityResolver.denormalizeEntity.calls.all()[1].returnValue);
    }));

    it('createTagEditorContext() creates a new TagEditorContext for readOnly=true', fakeAsync(() => {
        const data = getTagEditorContextInitData();
        data.readOnly = true;
        const expectedData = cloneDeep(data);

        const tagEditorContext = tagEditorService.createTagEditorContext(data);
        checkTagEditorContext(tagEditorContext, expectedData);
    }));

    it('createTagEditorContext() handles cyclic references in the page object correctly', fakeAsync(() => {
        const data = getTagEditorContextInitData();
        const expectedData = cloneDeep(data);
        const pageVariants = [ data.tagOwner ];
        const languageVariants = { 1: data.tagOwner as any };
        data.tagOwner.pageVariants = [ ...pageVariants ];
        data.tagOwner.languageVariants = { ...languageVariants };

        let tagEditorContext: TagEditorContext;
        expect(() => tagEditorContext = tagEditorService.createTagEditorContext(data)).not.toThrow();
        checkTagEditorContext(tagEditorContext, expectedData);

        // Make sure that the original page has not been modified.
        expect(data.tagOwner.pageVariants).toEqual(pageVariants);
        expect(data.tagOwner.languageVariants).toEqual(languageVariants);
    }));

    it('createTagEditorContext() sets up variableContext correctly such that it emits updates', fakeAsync(() => {
        const data = getTagEditorContextInitData();
        state.dispatch(new SetUILanguageAction('en'));

        const tagEditorContext = tagEditorService.createTagEditorContext(data);

        const expectedVarContext: VariableTagEditorContext = { uiLanguage: state.now.ui.language };
        let actualVarContext: VariableTagEditorContext;
        let subscriberCalled = 0;
        tagEditorContext.variableContext.subscribe(varContext => {
            ++subscriberCalled;
            actualVarContext = varContext;
        });
        expect(subscriberCalled).toBe(1);
        expect(actualVarContext).toEqual(expectedVarContext);

        state.dispatch(new SetUILanguageAction('de'));
        expectedVarContext.uiLanguage = 'de';
        expect(subscriberCalled).toBe(2);
        expect(actualVarContext).toEqual(expectedVarContext);
    }));

    it('createTagEditorContext() applies polyfills in IE11 if the tagOwner object comes from an IFrame', fakeAsync(() => {
        const data = getTagEditorContextInitData();
        const expectedData = cloneDeep(data);
        data.tagOwnerFromIFrame = true;
        userAgentRef.isIE11 = true;

        const tagEditorContext = tagEditorService.createTagEditorContext(data);
        checkTagEditorContext(tagEditorContext, expectedData);
        expect(tagEditorContext.editedTag).not.toBe(data.tag as any);
        expect(tagEditorContext.editedTag.tagType).not.toBe(data.tagType);

        expect(tagEditorContext.page).toEqual(data.tagOwner);
        expect(tagEditorContext.page).not.toBe(data.tagOwner);
    }));

    it('createTagEditorContext() does not apply polyfills in IE11 if the tagOwner object does not come from an IFrame', fakeAsync(() => {
        const data = getTagEditorContextInitData();
        const expectedData = cloneDeep(data);
        userAgentRef.isIE11 = true;

        const tagEditorContext = tagEditorService.createTagEditorContext(data);
        checkTagEditorContext(tagEditorContext, expectedData);
        expect(tagEditorContext.editedTag.tagType).toBe(data.tagType);
    }));

});


function getTagEditorInitData(): { tag: Tag, tagType: TagType, page: Page<Raw>} {
    const tag = getExampleEditableTag();
    const tagType = tag.tagType;
    delete tag.tagType;
    const page = getExamplePageData();
    delete page.pageVariants;
    delete page.languageVariants;
    return {
        tag,
        tagType,
        page,
    };
}

function createEditedTag(editableTag: EditableTag): EditableTag {
    const editedTag = cloneDeep(editableTag);
    (editedTag.properties['property0'] as StringTagPartProperty).stringValue = 'modified value';
    return editedTag;
}

class MockEntityResolver {
    denormalizeEntity = jasmine.createSpy('denormalizeEntity').and.callFake((type: string, entity: any) => ({
        ...entity,
    }));

    getNode(id: number): Node {
        return getExampleNodeData({ id });
    }
}

class MockEditorOverlayService {
    editImage(): void { }
}

class MockRepositoryBrowserClient {
    openRepositoryBrowser(): void { }
}

class MockTranslateService { }

class MockUserAgentRef {
    isIE11 = false;
}

class MockModalService {
    fromComponent(): Promise<void> {
        return Promise.resolve();
    }
}

class MockBaseApiService {
    get(): Observable<never> {
        return NEVER;
    }
    post(): Observable<never> {
        return NEVER;
    }
    delete(): Observable<never> {
        return NEVER;
    }
}

