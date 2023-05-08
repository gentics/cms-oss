import {Tag, TagPropertyMap} from '../models';
import {CustomEditor} from './custom-editor';
import {EditableTag} from './editable-tag';
import {TagEditorContext} from './tag-editor-context';

/**
 * This function needs to be called every time a tag property is changed, when a tag is edited via `TagEditor.editTagLive()`.
 *
 * If a tag is edited via `TagEditor.editTag()`, this function is not used.
 * @param tagProperties The entire `TagPropertyMap` of the tag being edited with all changes applied, i.e., the current
 * state of `Tag.properties` after every change. If the current state of the tag is invalid (not all mandatory properties
 * set or some property does not pass validation), `null` may be used instead of the `TagPropertyMap`.
 */
export type TagChangedFn = (tagProperties: TagPropertyMap | null) => void;

/**
 * Base interface for a `TagEditor`.
 *
 * Note that a tag is edited either via `editTag()` or via `editTagLive()`,
 * but not both at the same time.
 */
export interface TagEditor {

    /**
     * Opens the `TagEditor` to edit the specified tag, while reporting every change via `changeFn`.
     *
     * Whenever a `TagProperty` is changed, the entire `TagPropertyMap` must be passed to `changeFn` to inform
     * the parent component of the changes. It is up to the `TagEditor` to decide when a change is 'complete' and
     * needs to be reported, e.g., when the user types in an input field, the TagEditor may decide whether
     * to report a change on every keystroke or only after the user has finished typing.
     *
     * In this mode the `TagEditor` must not display any OK/Cancel button, since this will be handled
     * by the parent component.
     *
     * An alternative to the TagChangedFn would have been to make `editTagLive()` return an Observable,
     * but in order to not force `CustomTagEditor` implementers to import RxJS, we went for the callback function.
     *
     * @param tag The tag that should be edited.
     * @param context The `TagEditorContext` that provides further information about the tag.
     * @param changeFn This function must be called with the entire `TagPropertyMap` of the tag whenever a change
     * has been made. If the current tag state is invalid, `null` may be used instead of the `TagPropertyMap`.
     */
    editTagLive(tag: EditableTag, context: TagEditorContext, onChangeFn: TagChangedFn): void;

    /**
     * Opens the TagEditor for editing the specified tag.
     *
     * This method is optional for a `CustomTagEditor` and may be omitted. In that case
     * the parent component will implement this method using `editTagLive()`.
     *
     * @param tag The tag that should be edited.
     * @param context The `TagEditorContext` that provides further information about the tag.
     * @returns A Promise, which will be resolved with the modified tag if the user clicks the OK button
     * or which will be rejected if the user clicks Cancel button.
     */
    editTag?(tag: EditableTag, context: TagEditorContext): Promise<EditableTag>;

}

/**
 * Base interface for a custom `TagEditor` (a `TagEditor` that is loaded inside an IFrame by `CustomTagEditorHost`).
 *
 * By the time the `load` event of the IFrame window fires, the global `window.GcmsCustomTagEditor` variable
 * has to be set to the instance of the custom `TagEditor`.
 *
 * The IFrame element has the `data-gcms-ui-styles` set to a URL from which the basic styles
 * of the GCMS UI can be loaded. The URL can be accessed via `window.frameElement.dataset.gcmsUiStyles`.
 * Loading these styles is optional.
 */
export interface CustomTagEditor extends TagEditor, CustomEditor { }

/**
 * For TagEditors that are part of the GCMSUI, all methods are required.
 */
export type CompleteTagEditor = {
    [K in keyof TagEditor] - ?: TagEditor[K];
};

/**
 * The interface that the window that is hosted inside the IFrame has to implement.
 * By the time the `load` event fires, the global `window.GcmsCustomTagEditor` variable
 * has to be set to the instance of the `CustomTagEditor`.
 */
export interface WindowWithCustomTagEditor {
    /**
     * The `CustomTagEditor` provided by this window. This must have been set
     * by the time the `load` event fires.
     */
    GcmsCustomTagEditor: CustomTagEditor;
}

export type EntityType = 'page' | 'folder' | 'form' | 'image' | 'file' | 'node' | 'template';

export interface TagEditorChange {
    modified: boolean;
    valid: boolean;
    entityType: EntityType;
    entityId: string | number;
    nodeId?: number;
    tagName: string;
    tag: Tag;
}

export interface TagEditorChangeMessage extends TagEditorChange {
    type: 'tag-editor-change';
}
