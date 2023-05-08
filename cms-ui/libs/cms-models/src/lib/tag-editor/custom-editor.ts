
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
