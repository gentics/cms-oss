/** API methods as provided by the UI frame via MessageChannel */
export interface ExposedGCMSUIAPI {

    /** Navigates the UI ItemList to a specific folder. */
    navigateToFolder(folderId: number, nodeId?: number): Promise<boolean>;

    /** Refreshes the items of the current folder. */
    refreshCurrentFolder(): Promise<boolean>;

    /** Opens the properties of the node with the given id in the editor. */
    nodeProperties(nodeId: number): Promise<boolean>;

    /** Opens the properties of the folder with the given id in the editor. */
    folderProperties(folderId: number, nodeId?: number): Promise<boolean>;

    /** Opens the object properties of the folder with the given id in the editor. */
    folderObjectProperties(folderId: number, nodeId?: number): Promise<boolean>;

    /** Previews the page with the given id in the editor. */
    previewPage(pageId: number, nodeId?: number): Promise<boolean>;

    /** Opens the properties of the page with the given id in the editor. */
    pageProperties(pageId: number, nodeId?: number): Promise<boolean>;

    /** Opens the object properties of the page with the given id in the editor. */
    pageObjectProperties(pageId: number, nodeId?: number): Promise<boolean>;

    /** Opens the page with the given id in the editor. */
    editPage(pageId: number, nodeId?: number): Promise<boolean>;

    /** Opens the preview of the file with the given id in the editor. */
    previewFile(fileId: number, nodeId?: number): Promise<boolean>;

    /** Opens the properties of the file with the given id in the editor. */
    fileProperties(fileId: number, nodeId?: number): Promise<boolean>;

    /** Opens the object properties of the file with the given id in the editor. */
    fileObjectProperties(fileId: number, nodeId?: number): Promise<boolean>;

    /** Opens the preview of the image with the given id in the editor. */
    previewImage(imageId: number, nodeId?: number): Promise<boolean>;

    /** Opens the properties of the image with the given id in the editor. */
    imageProperties(imageId: number, nodeId?: number): Promise<boolean>;

    /** Opens the object properties of the image with the given id in the editor. */
    imageObjectProperties(imageId: number, nodeId?: number): Promise<boolean>;

    /** Opens the image with the given id in the editor. */
    editImage(imageId: number, nodeId?: number): Promise<boolean>;

    /** Opens the "Publish Queue" modal. */
    openPublishQueue(): Promise<void>;

    /** Opens the "Wastebin" modal. */
    openWastebin(nodeId?: number): Promise<void>;

    /** Opens the "New message" modal. */
    openMessageComposer(): Promise<void>;

    /** Opens the message inbox. */
    openMessageInbox(): Promise<void>;

    /** Opens the repository browser. */
    openRepositoryBrowser(options: RepositoryBrowserOptions): Promise<Array<any>>;

    /** Displays a modal dialog to the user. */
    showDialog(config: DialogConfig): Promise<any>;

    /** Displays a notification to the user. */
    showNotification(options: NotificationOptions): Promise<string | undefined>;

    /** Closes the embedded tool. */
    close(): Promise<void>;

    /**
     * Informs the UI that the tool has navigated to a sub-URL.
     * Can be used with {@link ExposedEmbeddedToolAPI#navigate} to navigate within the tool.
     *
     * @example
     *     api.ui.navigated('/products/2');
     *     // => UI frame changes URL to '#/tools/mytool/products/2'
     */
    navigated(path: string, replace?: boolean): Promise<void>;

    /**
     * Updates the breadcrumb for the tool in the UI.
     * The text of the first breadcrumb is ignored and replaced by the tool name.
     *
     * @example
     *     api.ui.provideBreadcrumb([
     *        { text: '', url: '' },
     *        { text: 'Books', url: 'books' },
     *        { text: 'The Great Gatsby', url: 'books/978-9176371213' }
     *     ]);
     *     // => UI toolbar will show "Book Tool > Books > The Great Gatsby"
     */
    provideBreadcrumbs(breadcrumbs: ToolBreadcrumb[]): void | Promise<void>;
}

export interface DialogConfig {
    title: string;
    body?: string;
    buttons: Array<{
        label: string;
        type?: 'default' | 'secondary' | 'success'| 'warning' | 'alert';
        flat?: boolean;
        // If specified, will be returned as the
        // value of the resolved promise (or the reason if rejected).
        returnValue?: any;
        // If true, clicking the button will cause
        // the promise to reject rather than resolve
        shouldReject?: boolean;
    }>;
}

export interface NotificationOptions {
    message: string;
    type?: 'default' | 'alert' | 'success';
    delay?: number;
    dismissOnClick?: boolean;
    action?: {
        label: string;
        dismiss?: boolean;
        result?: any;
    };
}

export type AllowedItemSelectionType = 'page' | 'folder' | 'image' | 'file' | 'template';
export type AllowedTagSelectionType = 'contenttag' | 'templatetag';
export type AllowedSelectionType = AllowedItemSelectionType | AllowedTagSelectionType;

export interface RepositoryBrowserOptions {
    allowedSelection: AllowedSelectionType | Array<AllowedSelectionType>;
    onlyInCurrentNode?: boolean;
    selectMultiple: boolean;
    startNode?: number;
    startFolder?: number;
    submitLabel?: string;
    title?: string;
}

export interface ToolBreadcrumb {
    text: string;
    url: string;
}
