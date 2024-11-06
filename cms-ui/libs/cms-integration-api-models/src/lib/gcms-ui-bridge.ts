import {
    AlohaComponent,
    DynamicDialogConfiguration,
    DynamicDropdownConfiguration,
    DynamicFormModalConfiguration,
    OverlayElementControl,
} from '@gentics/aloha-models';
import { EditMode, GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import {
    File,
    FileOrImage,
    Folder,
    Form,
    Image,
    ItemInNode,
    Language,
    Node,
    Normalized,
    Page,
    Raw,
    Tag,
    TagInContainer,
    TagType,
} from '@gentics/cms-models';
import { GCMSRestClient } from '@gentics/cms-rest-client';
import { RepositoryBrowserOptions } from './repository-browser';
import { TagEditorOptions, TagEditorResult } from './tag-editor/custom-editor';

/**
 * The part of the AppState that is exposed to editor IFrames using the `window.GCMSUI` object.
 */
export interface ExposedPartialState {
    currentItem: Folder<Normalized> | Page<Normalized> | File<Normalized> | Form<Normalized> | Image<Normalized> | Node<Normalized>;
    editMode: EditMode;
    pageLanguage?: Language;
    sid: number;
    uiLanguage: GcmsUiLanguage;
    uiVersion: string;
    userId: number;
}

export type StateChangedHandler = (state: ExposedPartialState) => any;

/**
 * Enum which defines why the modal has been closed.
 */
export enum ModalClosingReason {
    /**
     * When the modal has been closed by the user by pressing the Escape button.
     * Only available when the modal has been opened with `closeOnEscape=true`.
     */
    ESCAPE = 'escape',
    /**
     * When the modal has been closed by the user by clicking on the overlay.
     * Only available when the modal has been opened with `closeOnOverlayClick=true`.
     */
    OVERLAY_CLICK = 'overlay-click',
    /**
     * Canceled by the Modals `cancelFn` function.
     */
    CANCEL = 'cancel',
    /**
     * When the modal has been closed by the API.
     */
    API = 'api',
    /**
     * Closed because of a thrown error in the modal.
     */
    ERROR = 'error',
}

/**
 * Special error which is thrown when a Modal is being closed.
 */
export class ModalCloseError extends Error {

    public reason: ModalClosingReason;
    public cause?: Error;

    constructor(
        reasonOrError: ModalClosingReason | Error,
        message?: string,
    ) {
        if (reasonOrError instanceof Error) {
            super(message);
            this.reason = ModalClosingReason.ERROR;
            this.cause = reasonOrError;
        } else {
            super(message);
            this.reason = reasonOrError;
        }
    }
}

/**
 * Used to interact with the GCMS UI from editor IFrames.
 *
 * An object implementing this interface is exposed as `window.GCMSUI` in all editor IFrames of the GCMS UI content-frame.
 */
export interface GcmsUiBridge {

    // Internal implementation
    // --------------------------------------------

    /**
     * Internal function, do not use by yourself!
     * Executes the pre-load script in the context of the IFrame.
     * This method is called by the code inside the IFrame when the `DOMContentLoaded` event is fired.
     */
    runPreLoadScript: () => void;
    /**
     * Internal function, do not use by yourself!
     * Executes the post-load script in the context of the IFrame.
     * This method is called by the code inside the IFrame when the `load` event is fired.
     */
    runPostLoadScript: () => void;

    // General purpose
    // --------------------------------------------

    /**
     * The URL of the styles of the GCMS UI.
     * @deprecated The styles should be placed in the respective projects and loaded
     * from there instead from this url.
     */
    gcmsUiStylesUrl: string;
    /** An object containing useful information about the current state of the UI */
    appState: ExposedPartialState;
    /** Paths to various endpoints in use by the UI */
    paths: {
        apiBaseUrl: string;
        alohapageUrl: string;
        imagestoreUrl: string;
    };
    /** Registers a callback which is invoked whenever the contents of the appState change */
    onStateChange: (handler: StateChangedHandler) => void;
    /**
     * Tells the editor whether the page content has been modified. When set to `true`, the
     * "save" button will be enabled.
     */
    setContentModified: (modified: boolean) => void;
    /**
     * Opens the debug-tool which allows the dumping of the current application state
     * and other information to be able to debug errors.
     * This should only be used in emergencies and only, if you know what you're doing.
     */
    callDebugTool: () => void;

    // REST API
    // --------------------------------------------

    /**
     * Makes a GET request to an endpoint of the GCMS REST API and returns the parsed JSON object.
     * The endpoint should not include the base URL of the REST API, but just the endpoint as per
     * the documentation, e.g. `/folder/create`.
     */
    restRequestGET: (endpoint: string, params?: object) => Promise<object>;
    /**
     * Makes a POST request to an endpoint of the GCMS REST API and returns the parsed JSON object.
     * The endpoint should not include the base URL of the REST API, but just the endpoint as per
     * the documentation, e.g. `/folder/create`.
     */
    restRequestPOST: (endpoint: string, data: object, params?: object) => Promise<object>;
    /*
     * Makes a DELETE request to an endpoint of the GCMS REST API and returns the parsed JSON object.
     * The endpoint should not include the base URL of the REST API, but just the endpoint as per
     * the documentation, e.g. `/folder/create`.
     */
    restRequestDELETE: (endpoint: string, params?: object) => Promise<object | void>;
    /**
     * Client for interacting with all the GCMS APIs.
     */
    restClient: GCMSRestClient;

    // UI Actions
    // --------------------------------------------

    /**
     * Opens the image editor for the specified image.
     * @returns a promise, which will resolve to either the edited image
     * (this may be a copy of the original image as well) or to void if the
     * user canceled the edit or an error occurred.
     */
    editImage: (nodeId: number, imageId: number) => Promise<Image<Raw> | void>;
    /**
     * Opens the repository browser window.
     * Returns Promise which resolves to `ItemInNode | TagInContainer` if `selectMultiple` is false and
     * to `(ItemInNode | TagInContainer)[]` if `selectMultiple` is true.
     * The Promise resolves to null if user clicks on cancel button.
     */
    openRepositoryBrowser: (options: RepositoryBrowserOptions) => Promise<(ItemInNode | TagInContainer) | (ItemInNode | TagInContainer)[]>;
    /**
     * Opens a tag editor for the specified tag.
     * Based on the configuration of the TagType, either the GenticsTagEditor or
     * a custom tag editor is used.
     *
     * @param tag The tag to be edited - the property tag.tagType must be set.
     * @param context The current context.
     * @returns A promise, which when the user clicks OK, resolves and returns a copy of the edited tag
     * and when the user clicks Cancel, rejects.
     */
    openTagEditor: (tag: Tag, tagType: TagType, page: Page<Raw>, options?: TagEditorOptions) => Promise<TagEditorResult>;
    /**
     * Opens an the upload modal to allow the user to upload files/images to a specified folder.
     *
     * @param uploadType The type the user should be allowed to upload. Either 'image' or 'file'.
     * @param destinationFolder The folder to where the file/image should be uploaded to.
     * @param allowFolderSelection If the user should be allowed to change the destination folder.
     * @returns A Promise for the uploaded file/image.
     */
    openUploadModal: (uploadType: 'image' | 'file', destinationFolder?: Folder, allowFolderSelection?: boolean) => Promise<FileOrImage>;
    /**
     * Opens a modal with a dynamic form configuration.
     * @param configuration The configuration to create and handle the modal/form.
     * @returns A Promise which resolves once the Modal has been successfully closed.
     * A rejection (`ModalCloseError`) is being thrown when the user cancels the modal.
     */
    openDynamicModal: <T>(configuration: DynamicFormModalConfiguration<T>) => Promise<OverlayElementControl<T>>;
    /**
     * Opens a dropdown with a dynamic component.
     * @param configuration The configuration to create the dropdown.
     * @param componentSlot The slot where the dropdown should be opened to.
     * @returns A Promise which resolves once the Dropdown has been successfully closed.
     * A rejection (`ModalCloseError`) is being thrown when the user cancels the modal.
     */
    openDynamicDropdown: <T>(configuration: DynamicDropdownConfiguration<T>, componentSlot?: string) => Promise<OverlayElementControl<T>>;
    /**
     * Displays a Dialog to the user to interact with.
     * @param configuration The configuration for the dialog.
     * @returns A Promise which resolves/rejects once the User clicks one of the configured buttons.
     */
    openDialog: <T>(configuration: DynamicDialogConfiguration<T>) => Promise<OverlayElementControl<T>>;
    /**
     * Attempts to focus the specified editor-tab. If it should not be found, it'll do nothing.
     * @param tabId The id of the tab that should be focused.
     */
    focusEditorTab: (tabId: string) => void;
    /**
     * Class of the error which is thrown when a overlay element has been closed.
     */
    closeErrorClass: typeof ModalCloseError;

    // Aloha Surface Integration
    // --------------------------------------------

    /**
     * Registers/Binds a component to the specified slot.
     * Will override any component previously bound to it.
     *
     * @param slot The slot where the component should be registered as
     * @param component The component definition
     */
    registerComponent: (slot: string, component: AlohaComponent) => void;
    /**
     * Unregisters a component from the given slot.
     *
     * @param slot The slot which has been registered
     */
    unregisterComponent: (slot: string) => void;
}
