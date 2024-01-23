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
    TagInContainer,
    TagType,
} from '@gentics/cms-models';
import { AlohaComponent } from '@gentics/aloha-models';
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

/**
 * A simplified API for Angulars FormControls.
 * Allows to manage the control is a defined manner.
 */
export interface ExposedControl<T> {
    /**
     * The current value of the control.
     */
    readonly value: T;
    /**
     * If the control is currently enabled.
     */
    readonly enabled: boolean;
    /**
     * If the control is dirty/has been touched by the user.
     */
    readonly dirty: boolean;
    /**
     * If the control is pristine/has not yet been touched by the user.
     */
    readonly pristine: boolean;
    /**
     * If the control is currently valid/has no validation errors.
     */
    readonly valid: boolean;
    /**
     * Sets/Updates the value of this control.
     */
    setValue(value: T): void;
    /**
     * Enables the control (if it is currently disabled).
     */
    enable(): void;
    /**
     * Disables the control (if it is currently enabled).
     */
    disable(): void;
    /**
     * Sets this control as dirty/touched by the user.
     */
    markAsDirty(): void;
    /**
     * Sets this control as pristine/not yet touched by the user.
     */
    markAsPristine(): void;
    /** Synchronizes the value of the control and the component and runs the validation again. */
    updateValueAndValidity(): void;
}

export interface ExposedFormControl<T> extends ExposedControl<T> {
    controls?: {
        [key in keyof T]: DynamicControlConfiguration<T[key]>;
    };
}

/**
 * Configuration for a single Control to manage it's content correctly.
 */
export interface DynamicControlConfiguration<T> {
    /**
     * The type of the component which is supposed to handle the value.
     */
    type: string;
    /**
     * Options for the component. Is component dependend.
     */
    options?: Record<string, any>;
    /**
     * Validates a control and determines if it is valid or not.
     *
     * @param controlValue The current value of the entire form.
     * @returns `null` if the control is valid, or a record of issues/errors for this control.
     */
    validate?: (controlValue: T) => null | Record<string, any>;
    /**
     * Simple callback which is called whenever the control value changes,
     * to be able to react to it's changes.
     *
     * @param controlValue The current value of the control.
     * @param control An api for the control itself to manage it.
     */
    onChange?: (controlValue: T, control: ExposedControl<T>) => void;
}

export interface OverlayElementSettings<T> {
    /**
     * Value of the form/component and what it's starting value is.
     */
    initialValue?: T;
    /**
     * If the overlay eleemnt should close when the escape key is being pressed.
     */
    closeOnEscape?: boolean;
    /**
     * If the overlay element should close when the user clicks outside of it.
     */
    closeOnOverlayClick?: boolean;
}

/**
 * Control for an opened overlay element.
 */
export interface OverlayElementControl<T> {
    /**
     * Close the overlay element if it isn't closed yet.
     */
    close: () => void;
    /**
     * @returns If the overlay element is still open.
     */
    isOpen: () => boolean;
    /**
     * The return value of the overlay element.
     */
    value: Promise<T>;
}

/**
 * Configuration for an entire Dynamic Form Modal.
 */
export interface DynamicFormModalConfiguration<T> extends OverlayElementSettings<T> {
    /**
     * Title of the Modal
     */
    title: string;
    /**
     * Controls which may manage the values of the element.
     */
    controls: {
        [key in keyof T]: DynamicControlConfiguration<T[key]>;
    };
    /**
     * Validates the entire form and determines if it is valid or not.
     *
     * @param formValue The current value of the entire form.
     * @returns `null` if the form is valid, or a record of issues/errors for this form.
     */
    validate?: (formValue: T) => null | Record<string, any>;
    /**
     * Simple callback which is called whenever a single control changes,
     * to be able to react to it's changes.
     *
     * @param formValue The current value of the form.
     * @param formControl The control of the form control itself to manage it.
     */
    onChange?: (formValue: T, formControl: ExposedControl<T>) => void;
}

/**
 * Configuration for a dynamic dropdown.
 */
export interface DynamicDropdownConfiguration<T> extends DynamicControlConfiguration<T>, OverlayElementSettings<T> {
    /**
     * If the dropdown should render a confirm button and only resolve,
     * when the confirm button has been clicked.
     * This changes the behaviour from resolving as soon as a valid value has been selected.
     */
    resolveWithConfirmButton?: boolean;
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
    openTagEditor: (tag: Tag, tagType: TagType, page: Page<Raw>) => Promise<Tag>;

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
     * Class of the error which is thrown when a overlay element has been closed.
     */
    closeErrorClass: any;

    registerComponent: (slot: string, component: AlohaComponent) => void;
    unregisterComponent: (slot: string) => void;
}
