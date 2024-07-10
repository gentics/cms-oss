/** Functions that can be implemented by a tool to provide the GCMSUI with information. */
export interface ExposableEmbeddedToolAPI {

    /** If implemented, the tool reports when unsaved changes are pending. */
    hasUnsavedChanges?(): boolean | Promise<boolean>;

    /**
     * If implemented, the tool can be navigated via URLs in the UI.
     *
     * @example
     *     URL in the UI changes to "#/tool/mytool/element/1234"
     *     passed path to navigateTool: "/element/1234"
     */
    navigate?(path: string): boolean | Promise<boolean>;

    /** If implemented, the tool supports saving/restoring its state. */
    restoreState?(state: object): void;
    saveState?(): object | Promise<object>;

}
