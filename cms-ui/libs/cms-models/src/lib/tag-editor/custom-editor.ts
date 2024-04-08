import { EditableTag } from './editable-tag';

/** Describes the current size of a `CustomEditor`. */
export interface CustomEditorSize {
    width?: number;
    height?: number;
}

/**
 * This function needs to be called by a `CustomTagPropertyEditor` or `CustomTagEditor` whenever its size changes.
 * This is necessary, because it is loaded in an IFrame.
 */
export type CustomEditorSizeChangedFn = (newSize: CustomEditorSize) => void;

/** Base interface for a custom editor that is loaded inside an IFrame. */
export interface CustomEditor {

    /**
     * Registers the callback that needs to be called whenever the editor's size changes.
     */
    registerOnSizeChange(fn: CustomEditorSizeChangedFn): void;

}

export interface TagEditorOptions {
    /** If it sohuld not update/insert the DOM element. Will skip the rendering request of the tag as well. */
    skipInsert?: boolean;
    /** If the tag-fill/user should be able to delete the tag in question. */
    withDelete?: boolean;
}

export interface TagEditorResult {
    doDelete: boolean;
    tag: EditableTag;
}
