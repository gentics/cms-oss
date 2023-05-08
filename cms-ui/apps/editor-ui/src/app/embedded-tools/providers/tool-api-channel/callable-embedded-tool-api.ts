export interface CallableEmbeddedToolAPI {
    /** If implemented, the tool reports when unsaved changes are pending. */
    hasUnsavedChanges?(): Promise<boolean>;

    /** If implemented, the tool can be navigated via URLs in the UI. */
    navigate?(path: string): Promise<boolean>;

    /** If implemented, the tool supports saving/restoring its state. */
    restoreState?(state: object): Promise<boolean>;
    saveState?(): Promise<object>;
}
