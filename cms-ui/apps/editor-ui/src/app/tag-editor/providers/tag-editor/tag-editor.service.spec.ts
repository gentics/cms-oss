import { TestBed, fakeAsync } from '@angular/core/testing';
import { ApplicationStateService, STATE_MODULES, SetUILanguageAction } from '@editor-ui/app/state';
import {
    EditableTag,
    Node,
    Page,
    Raw,
    StringTagPartProperty,
    Tag,
    TagEditorContext,
    TagType,
    VariableTagEditorContext,
} from '@gentics/cms-models';
import { getExampleNodeData, getExamplePageData } from '@gentics/cms-models/testing/test-data.mock';
import { NgxsModule } from '@ngxs/store';
import { cloneDeep } from 'lodash-es';
import { NEVER, Observable } from 'rxjs';
import { getExampleEditableTag } from '../../../../testing/test-tag-editor-data.mock';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { TagEditorContextImpl } from '../../common/impl/tag-editor-context-impl';
import { TranslatorImpl } from '../../common/impl/translator-impl';
import { TagEditorOverlayHostComponent } from '../../components/tag-editor-overlay-host/tag-editor-overlay-host.component';
import { EditTagInfo, TagEditorService } from './tag-editor.service';

describe('TagEditorService', () => {

    let state: TestApplicationState;
    let tagEditorService: TagEditorService;
    let tagEditorOverlayHost: TagEditorOverlayHostComponent;
    let userAgentRef: MockUserAgentRef;
    let entityResolver: MockEntityResolver;
    let editorOverlayService: MockEditorOverlayService;
    let repositoryBrowserClient: MockRepositoryBrowserClient;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });

        state = TestBed.get(ApplicationStateService);
        editorOverlayService = new MockEditorOverlayService();
        userAgentRef = new MockUserAgentRef();
        entityResolver = new MockEntityResolver();
        repositoryBrowserClient = new MockRepositoryBrowserClient();
        tagEditorService = new TagEditorService(
            state,
            editorOverlayService as any,
            entityResolver as any,
            repositoryBrowserClient as any,
            new MockTranslateService() as any,
            userAgentRef as any,
            new MockModalService() as any,
            new MockBaseApiService() as any,
        );
        tagEditorOverlayHost = <any> new MockTagEditorOverlayHost();
        tagEditorService.registerTagEditorOverlayHost(tagEditorOverlayHost);
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

    it('openTagEditor() uses the tagEditorOverlayHost to open the tag editor and resolves the result promise correctly', fakeAsync(async () => {
        const data = getTagEditorContextInitData();
        const expectedData = cloneDeep(data);
        expectedData.tagOwnerFromIFrame = true;
        expect((<any> data.tag)['tagType']).toBeUndefined();
        const createTagEditorContextSpy = spyOn(tagEditorService, 'createTagEditorContext').and.callThrough();
        const openTagEditorSpy = spyOn(tagEditorOverlayHost, 'openTagEditor')
            .and.callFake((editableTag: EditableTag, _context: TagEditorContext) => {
                return Promise.resolve({
                    doDelete: false,
                    tag: createEditedTag(editableTag),
                });
            });

        const expectedEditableTag: EditableTag = {
            ...expectedData.tag,
            tagType: expectedData.tagType,
        };

        const editorResult = await tagEditorService.openTagEditor(data.tag, data.tagType, data.tagOwner, false);

        expect(createTagEditorContextSpy).toHaveBeenCalledWith(expectedData);
        expect(openTagEditorSpy).toHaveBeenCalled();
        expect(openTagEditorSpy.calls.argsFor(0)[0]).toEqual(expectedEditableTag);
        const tagEditorContext: TagEditorContext = openTagEditorSpy.calls.argsFor(0)[1];
        checkTagEditorContext(tagEditorContext, expectedData);

        // Make sure that the returned promise resolves to a tag without the tagType property set
        const expectedFinalTag: Tag = {
            ...expectedData.tag,
        };
        (expectedFinalTag.properties['property0'] as StringTagPartProperty).stringValue = 'modified value';

        expect(editorResult.doDelete).toEqual(false);
        expect(editorResult.tag).toEqual(expectedFinalTag as any);
    }));

    it('openTagEditor() uses the tagEditorOverlayHost to open the tag editor and relays a promise rejection correctly', fakeAsync(async () => {
        const data = getTagEditorInitData();
        const openTagEditorSpy = spyOn(tagEditorOverlayHost, 'openTagEditor').and.returnValue(Promise.reject(undefined));

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

    it('unregisterTagEditorOverlayHost() works', fakeAsync(async () => {
        const data = getTagEditorInitData();
        const openTagEditorSpy = spyOn(tagEditorOverlayHost, 'openTagEditor')
            .and.callFake((editableTag: EditableTag, _context: TagEditorContext) => {
                return Promise.resolve({
                    doDelete: false,
                    tag: createEditedTag(editableTag),
                });
            });

        const editorResult = await tagEditorService.openTagEditor(data.tag, data.tagType, data.page);
        expect(openTagEditorSpy).toHaveBeenCalled();
        expect(editorResult).toBeTruthy();

        expect(() => tagEditorService.unregisterTagEditorOverlayHost(tagEditorOverlayHost)).not.toThrow();
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

class MockTagEditorOverlayHost {
    openTagEditor(): void { }
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

