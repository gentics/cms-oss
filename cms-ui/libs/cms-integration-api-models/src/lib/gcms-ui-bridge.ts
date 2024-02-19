import {
    AlohaComponent,
    DynamicDialogConfiguration,
    DynamicDropdownConfiguration,
    DynamicFormModalConfiguration,
    OverlayElementControl,
} from '@gentics/aloha-models';
import {
    EditMode,
    File,
    Folder,
    Form,
    GcmsUiLanguage,
    Image,
    ItemInNode,
    Language,
    Node,
    Normalized,
    Page,
    Raw,
    RepositoryBrowserOptions,
    Tag,
    TagEditorResult,
    TagInContainer,
    TagType,
} from '@gentics/cms-models';
import { GCMSRestClient } from '@gentics/cms-rest-client';

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
 * Used to interact with the GCMS UI from editor IFrames.
 *
 * An object implementing this interface is exposed as `window.GCMSUI` in all editor IFrames of the GCMS UI content-frame.
 */
export interface GcmsUiBridge {
    /**
     * Executes the pre-load script in the context of the IFrame.
     * This method is called by the code inside the IFrame when the `DOMContentLoaded` event is fired.
     */
    runPreLoadScript: () => void;
    /**
     * Executes the post-load script in the context of the IFrame.
     * This method is called by the code inside the IFrame when the `load` event is fired.
     */
    runPostLoadScript: () => void;
    /**
     * Opens the repository browser window.
     * Returns Promise which resolves to `ItemInNode | TagInContainer` if `selectMultiple` is false and
     * to `(ItemInNode | TagInContainer)[]` if `selectMultiple` is true.
     * The Promise resolves to null if user clicks on cancel button.
     */
    openRepositoryBrowser: (options: RepositoryBrowserOptions) => Promise<(ItemInNode | TagInContainer) | (ItemInNode | TagInContainer)[]>;
    /**
     * The URL of the styles of the GCMS UI.
     */
    gcmsUiStylesUrl: string;
    /** An object containing useful information about the current state of the UI */
    appState: ExposedPartialState;
    /** Registers a callback which is invoked whenever the contents of the appState change */
    onStateChange: (handler: StateChangedHandler) => void;
    /** Paths to various endpoints in use by the UI */
    paths: {
        apiBaseUrl: string;
        alohapageUrl: string;
        imagestoreUrl: string;
    };
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
    /**
     * Client for interacting with all the GCMS APIs.
     */
    restClient: GCMSRestClient;

    /**
     * Tells the editor whether the page content has been modified. When set to `true`, the
     * "save" button will be enabled.
     */
    setContentModified: (modified: boolean) => void;
    /**
     * Opens the image editor for the specified image.
     * @returns a promise, which will resolve to either the edited image
     * (this may be a copy of the original image as well) or to void if the
     * user canceled the edit or an error occurred.
     */
    editImage: (nodeId: number, imageId: number) => Promise<Image<Raw> | void>;
    callDebugTool: () => void;
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
    openTagEditor: (tag: Tag, tagType: TagType, page: Page<Raw>, withDelete?: boolean) => Promise<TagEditorResult>;

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
     * Class of the error which is thrown when a overlay element has been closed.
     */
    closeErrorClass: any;

    registerComponent: (slot: string, component: AlohaComponent) => void;
    unregisterComponent: (slot: string) => void;
}
