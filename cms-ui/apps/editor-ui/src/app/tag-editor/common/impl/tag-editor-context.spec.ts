import { getExampleEditableTag, getMockTagEditorTranslator } from '@editor-ui/testing/test-tag-editor-data.mock';
import { GcmsUiServices, TagEditorContext, Translator, VariableTagEditorContext } from '@gentics/cms-models';
import { getExampleNodeData, getExamplePageData } from '@gentics/cms-models/testing/test-data.mock';
import { BehaviorSubject, Observable } from 'rxjs';
import { TagEditorContextImpl } from './tag-editor-context-impl';

const READ_ONLY = false;
const SID = 7890;
const VAR_CONTEXTS: VariableTagEditorContext[] = [
    { uiLanguage: 'de' },
    { uiLanguage: 'en' },
    { uiLanguage: 'de' },
];

const PAGE = getExamplePageData();
const TAG = getExampleEditableTag();
// const FOLDER = getExampleFolderData();
// const IMAGE = getExampleImageData();
const NODE = getExampleNodeData();

/** Asserts that the specified TagEditorContexts are equal. */
export function assertTagEditorContextsEqual(expected: TagEditorContext, actual: TagEditorContext): void {
    if (!expected) {
        expect(actual).toBeFalsy();
        return;
    } else {
        expect(actual).toBeTruthy();
    }

    // Create clones, because the unset page/folder/image/file properties
    // are simply not set in an original context, but undefined in a clone.
    const expectedClone = expected.clone();
    const actualClone = actual.clone();
    delete expectedClone.variableContext;
    delete actualClone.variableContext;
    expect(expectedClone).toEqual(actualClone);
    expect(!!expected.variableContext).toBe(!!actual.variableContext);
}

describe('TagEditorContextImpl', () => {

    let variableContext$: BehaviorSubject<VariableTagEditorContext>;
    let translator: Translator;
    let gcmsUiServices: GcmsUiServices;

    beforeEach(() => {
        variableContext$ = new BehaviorSubject(VAR_CONTEXTS[0]);
        translator = getMockTagEditorTranslator();
        gcmsUiServices = {
            openRepositoryBrowser: jasmine.createSpy('openRepositoryBrowser'),
            openImageEditor: jasmine.createSpy('openImageEditor'),
            openUploadModal: jasmine.createSpy('openUploadModal'),
            restRequestDELETE: jasmine.createSpy('restRequestDELETE'),
            restRequestGET: jasmine.createSpy('restRequestGET'),
            restRequestPOST: jasmine.createSpy('restRequestPOST'),
        };
    });

    it('TagEditorContext.create() works', () => {
        const context = TagEditorContextImpl.create(TAG, READ_ONLY, PAGE, NODE, SID, translator, variableContext$, gcmsUiServices, false);
        expect(context.editedTag).toBe(TAG);
        expect(context.readOnly).toBe(READ_ONLY);
        expect(context.page).toBe(PAGE);
        expect(context.node).toBe(NODE);
        expect(context.sid).toBe(SID);
        expect(context.translator).toBe(translator);
        expect(context.validator).toBeTruthy();
        expect(context.variableContext).toBe(variableContext$);
        expect(context.gcmsUiServices).toBe(gcmsUiServices);
        expect(context.withDelete).toBe(false);
        expect(context.file).toBeUndefined();
        expect(context.folder).toBeUndefined();
        expect(context.image).toBeUndefined();
    });

    it('variableContext observable works', () => {
        let emissionsCount = 0;
        let expectedVarContext: VariableTagEditorContext = VAR_CONTEXTS[0];

        const context = TagEditorContextImpl.create(TAG, READ_ONLY, PAGE, NODE, SID, translator, variableContext$, gcmsUiServices, true);
        expect(context.withDelete).toBe(true);
        context.variableContext.subscribe(varContext => {
            ++emissionsCount;
            expect(varContext).toBe(expectedVarContext);
        });
        expect(emissionsCount).toEqual(1);

        expectedVarContext = VAR_CONTEXTS[1];
        variableContext$.next(VAR_CONTEXTS[1]);
        expect(emissionsCount).toEqual(2);

        expectedVarContext = VAR_CONTEXTS[2];
        variableContext$.next(VAR_CONTEXTS[2]);
        expect(emissionsCount).toEqual(3);
    });

    it('clone() works', () => {
        const src = TagEditorContextImpl.create(TAG, READ_ONLY, PAGE, NODE, SID, translator, variableContext$, gcmsUiServices, true);
        const clone = src.clone();

        // The following properties should be deep copies.
        expect(clone.editedTag).toEqual(src.editedTag);
        expect(clone.editedTag).not.toBe(src.editedTag);
        expect(clone.page).toEqual(src.page);
        expect(clone.page).not.toBe(src.page);
        expect(clone.validator).toEqual(src.validator);
        expect(clone.validator).not.toBe(src.validator);

        // The Observable should also not be the same.
        expect(clone.variableContext instanceof Observable).toBeTruthy();
        expect(clone.variableContext).not.toBe(src.variableContext);

        // SID and translator are immutable and thus the same, the others are undefined.
        expect(clone.readOnly).toBe(READ_ONLY);
        expect(clone.sid).toBe(SID);
        expect(clone.translator).toBe(translator);
        expect(clone.gcmsUiServices).toEqual(gcmsUiServices);
        expect(clone.gcmsUiServices).not.toBe(gcmsUiServices);
        expect(clone.withDelete).toEqual(src.withDelete);
        expect(clone.file).toBeUndefined();
        expect(clone.folder).toBeUndefined();
        expect(clone.image).toBeUndefined();
    });

    it('openRepositoryBrowser() works in a clone', () => {
        const src = TagEditorContextImpl.create(TAG, READ_ONLY, PAGE, NODE, SID, translator, variableContext$, gcmsUiServices, false);
        const clone = src.clone();

        clone.gcmsUiServices.openRepositoryBrowser({ allowedSelection: 'page', selectMultiple: false });

        expect(gcmsUiServices.openRepositoryBrowser).toHaveBeenCalledWith({ allowedSelection: 'page', selectMultiple: false });
    });

    it('openImageEditor() works in a clone', () => {
        const src = TagEditorContextImpl.create(TAG, READ_ONLY, PAGE, NODE, SID, translator, variableContext$, gcmsUiServices, false);
        const clone = src.clone();

        clone.gcmsUiServices.openImageEditor({ nodeId: 1, imageId: 415 });

        expect(gcmsUiServices.openImageEditor).toHaveBeenCalledWith({ nodeId: 1, imageId: 415 });
    });

    it('cloned observables work', () => {
        let emissionsCountSrc = 0;
        let emissionsCountClone1 = 0;
        let emissionsCountClone2 = 0;
        let expectedVarContext: VariableTagEditorContext = VAR_CONTEXTS[0];

        const src = TagEditorContextImpl.create(TAG, READ_ONLY, PAGE, NODE, SID, translator, variableContext$, gcmsUiServices, false);
        const clone1 = src.clone();
        const clone2 = clone1.clone();

        src.variableContext.subscribe(varContext => {
            ++emissionsCountSrc;
            expect(varContext).toBe(expectedVarContext);
        });
        expect(emissionsCountSrc).toEqual(1);

        clone1.variableContext.subscribe(varContext => {
            ++emissionsCountClone1;
            expect(varContext).toEqual(expectedVarContext);
            expect(varContext).not.toBe(expectedVarContext);
        });
        expect(emissionsCountClone1).toEqual(1);

        clone2.variableContext.subscribe(varContext => {
            ++emissionsCountClone2;
            expect(varContext).toEqual(expectedVarContext);
            expect(varContext).not.toBe(expectedVarContext);
        });
        expect(emissionsCountClone2).toEqual(1);

        expectedVarContext = VAR_CONTEXTS[1];
        variableContext$.next(VAR_CONTEXTS[1]);
        expect(emissionsCountSrc).toEqual(2);
        expect(emissionsCountClone1).toEqual(2);
        expect(emissionsCountClone2).toEqual(2);

        expectedVarContext = VAR_CONTEXTS[2];
        variableContext$.next(VAR_CONTEXTS[2]);
        expect(emissionsCountSrc).toEqual(3);
        expect(emissionsCountClone1).toEqual(3);
        expect(emissionsCountClone2).toEqual(3);
    });

    it('observables work if clone subscribes first', () => {
        let emissionsCountSrc = 0;
        let emissionsCountClone1 = 0;
        let emissionsCountClone2 = 0;
        let expectedVarContext: VariableTagEditorContext = VAR_CONTEXTS[0];

        const src = TagEditorContextImpl.create(TAG, READ_ONLY, PAGE, NODE, SID, translator, variableContext$, gcmsUiServices, false);
        const clone1 = src.clone();
        const clone2 = clone1.clone();

        // Subscribe to the second clone first
        clone2.variableContext.subscribe(varContext => {
            ++emissionsCountClone2;
            expect(varContext).toEqual(expectedVarContext);
            expect(varContext).not.toBe(expectedVarContext);
        });
        expect(emissionsCountClone2).toEqual(1);

        // Emit a new value.
        expectedVarContext = VAR_CONTEXTS[1];
        variableContext$.next(VAR_CONTEXTS[1]);
        expect(emissionsCountClone2).toEqual(2);

        // Now subscribe to the src observable.
        src.variableContext.subscribe(varContext => {
            ++emissionsCountSrc;
            expect(varContext).toBe(expectedVarContext);
        });
        expect(emissionsCountSrc).toEqual(1);

        // Emit another value.
        expectedVarContext = VAR_CONTEXTS[2];
        variableContext$.next(VAR_CONTEXTS[2]);
        expect(emissionsCountClone2).toEqual(3);
        expect(emissionsCountSrc).toEqual(2);

        // Now subscribe to clone1.
        clone1.variableContext.subscribe(varContext => {
            ++emissionsCountClone1;
            expect(varContext).toEqual(expectedVarContext);
            expect(varContext).not.toBe(expectedVarContext);
        });
        expect(emissionsCountClone1).toEqual(1);
    });

});
