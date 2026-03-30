import { I18nString } from './common';
import { InheritableItem, ItemVersion } from './item';
import { Raw } from './type-util';
import { User } from './user';

/*
 * Temporary Compatibility types for renaming values
 */
/** @deprecated */
export type IForm = Form;
/** @deprecated */
export type ISchema = FormSchema;
/** @deprecated */
export type ISchemaFieldProperties = FormSchemaProperty;
/** @deprecated */
export type ISchemaFields = FormSchemaProperties;
/** @deprecated */
export type IValidation = FormPropertyValidation;
/** @deprecated */
export type UiSchema = FormUISchema;
/** @deprecated */
export type Element = FormElement;
/** @deprecated */
export type IFormGridOptions = FormGridOptions;
/** @deprecated */
export type IFormGridImageOptions = FormGridImageOptions;
/** @deprecated */
export type IFormgridType = FormgridType;
/** @deprecated */
export interface IFoundElement {
    element: Element | undefined;
    pageInUiSchema: number;
}

export interface EditableFormProperties {
    /**
     * The form-type of the form. Used to identify which form-configuration
     * has has to be used for this form.
     * May only be edited when creating a new form.
     */
    formType: string;
    /**
     * The name of the form
     */
    name: string;
    /**
     * Filename of the from for publishing
     */
    fileName: string;
    /**
     * Description of the form
     */
    description: string;
    /**
     * The languages the form supports.
     */
    languages: string[];
    /**
     * The schema definition of the form
     */
    schema: FormSchema;
    /**
     * The ui-schema definition of the form
     */
    uiSchema: FormUISchema;

    /* INTERNAL FORMS
     * ===================================================================== */

    /**
     * The URLs mapped by the language, where to redirect to,
     * when a user has successfully submitted the form.
     * Must be null/empty if {@link successPageId}/{@link successNodeId} is used.
     */
    successUrlI18n: I18nString;
    /**
     * The ID of the page, where to redirect to,
     * when a user has successfully submitted the form.
     * Must be null/0 if {@link successUrlI18n} is used.
     * Must be used with {@link successNodeId}.
     */
    successPageId: number;
    /**
     * The ID of the node in which {@link successPageId} is to be loaded from.
     */
    successNodeId: number;
    /**
     * The email address which should receive the administrator emails.
     */
    adminEmailAddress: string;
    /**
     * The email-template to use when sending an email to the administrator addresses,
     * configurable per language.
     * Emails to the administrator are sent whenever a user submits a form.
     * Must be null/empty if {@link adminEmailPageId}/{@link adminEmailNodeId} is used.
     */
    adminEmailTemplate: I18nString;
    /**
     * The page to use when sending an email to the administrator addresses.
     * Emails to the administrator are sent whenever a user submits a form.
     * Must be null/0 if {@link adminEmailTemplate} is used.
     * Must be used with {@link adminEmailNodeId}.
     */
    adminEmailPageId: number;
    /**
     * The ID of the node in which {@link adminEmailPageId} is to be loaded from.
     */
    adminEmailNodeId: number;
    /**
     * The subject of the email which is send to an administrator, configurable per language.
     * Emails to the administrator are sent whenever a user submits a form.
     */
    adminEmailSubject: I18nString;
}

export interface Form extends InheritableItem, EditableFormProperties {
    /**
     * Which type of item this is.
     */
    readonly type: 'form';
    /**
     * The folderId in which the form resides in
     */
    readonly folderId: number;
    /**
     * The translated label of `formType`
     */
    readonly formTypeLabel: string;
    /**
     * Whether the form is modified (the last version of the form is not the currently published one)
     */
    readonly modified: boolean;
    /**
     * Whether the form has time management set or not.
     */
    readonly planned: boolean;
    /**
     * Whether the form is in queue for being published or taken offline
     */
    readonly queued: boolean;
    /**
     * Whether the form is currently online.
     */
    readonly online: boolean;
    /**
     * Whether this form is a master form.
     */
    readonly master: boolean;
    /**
     * True if the form is locked.
     */
    readonly locked: boolean;
    /**
     * The user who published the forms most recent version
     */
    readonly publisher?: User<Raw>;
    /**
     * Publish Date as a Unix timestamp.
     */
    readonly pdate?: number;
    /**
     * Unix timestamp, since when the form is locked, or -1 if it is not locked.
     */
    readonly lockedSince: number;
    /**
     * User, who locked the form.
     */
    readonly lockedBy?: User<Raw>;
    /**
     * The currently set time management for the form.
     */
    readonly timeManagement: FormTimeManagement;
    /**
     * Most recent version of the Form.
     */
    readonly version: ItemVersion;

    /* EXTERNAL FORMS
     * ===================================================================== */

    /**
     * The foreign/external ID of an external form.
     */
    readonly externalId?: string;
    /**
     * The foreign/external Version of an external form.
     */
    readonly externalVersion?: string;
}

export type FormInNode = Form & { nodeId: number };

export type FormStatus = 'published' | 'edited' | 'offline' | 'queue' | 'timeframe' | 'publishat';

/** Time Management of forms */
export interface FormTimeManagement {

    /** Unix timestamp at which the form will be published */
    at: number;

    /** Unix timestamp at which the form will be taken offline  */
    offlineAt: number;

    /** Queued time management for publishing the form */
    queuedPublish?: FormQueuedActionPublish;

    /** Queued time management for taking the form offline */
    queuedOffline?: FormQueuedActionTakeOffline;

    /** Form Version that will be published at the timestamp */
    version?: FormVersion;

    /* The user that planned to publish */
    futurePublisher?: User;

    /* The user that planned to upublish */
    futureUnpublisher?: User;
}

/** Superinterface for queued FormTimeManagement actions/ */
export interface FormQueuedAction {

    /** Unix timestamp at which the form shall be published/taken offline */
    at: number;

    /** User who put the form into the queue */
    user: User<Raw>;

}

/** Queued time management for taking a form offline */
export interface FormQueuedActionTakeOffline extends FormQueuedAction { }

/** Queued time management for publishing a form */
export interface FormQueuedActionPublish extends FormQueuedActionTakeOffline {

    /** Form Version that will be published at the timestamp */
    version: FormVersion;

}

export interface FormQueuedActionRequestPublishAt {
    at: number;
    alllang: boolean;
    keepVersion: boolean;
}

export interface FormQueuedActionRequestTakeOfflineAt {
    at: number;
    alllang: boolean;
}

export interface FormQueuedActionRequestClear {
    form: {
        id: number;
    };
    unlock: boolean;
    clearPublishAt?: boolean;
    clearOfflineAt?: boolean;
}

/**
 * Represents a form version in the CMS
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_FormVersion.html
 */
export interface FormVersion {
    /** Version number */
    // eslint-disable-next-line id-blacklist
    number: string;

    /** Version timestamp */
    timestamp: number;

    /** Editor of the version */
    editor: User<Raw>;
}

export interface FormDownloadInfo {
    /** If the download has been created/ready to download. */
    downloadReady: boolean;
    /** If the download is still being created. */
    requestPending: boolean;
    /** The UUID of the download when it's ready. */
    downloadUuid?: string;
    /** ISO Date string for when the download has been created. */
    downloadTimestamp?: string;
    /** Optional error */
    error?: string;
}

export interface FormSchema {
    /**
     * The Key of the form? Basically the ID
     * @deprecated Use `form.externalId` or `form.globalId` instead
     */
    key: string;
    /**
     * The version of the form
     * @deprecated Use `form.externalVersion` instead
     */
    version: string;
    /**
     * The property/control definitions of this form.
     * The key is the ID of the control.
     */
    properties: FormSchemaProperties;
}

export interface FormUISchema {
    /**
     * The Key of the form? Basically the ID
     * @deprecated Use `form.externalId` or `form.globalId` instead
     */
    key: string;
    /**
     * The version of the form
     * @deprecated Use `form.externalVersion` instead
     */
    version: string;
    /**
     * The pages of this form, which define the layout and the setting-values of the elements
     */
    pages: FormPage[];
    /**
     * Settings from the Form Grid
     */
    formGrid: {
        width: number;
        widthOptimized: boolean;
        flow: string;
    };
}

export type FormSchemaProperties = Record<string, FormSchemaProperty>;

export interface FormSchemaProperty {
    /**
     * The type of this property
     */
    type: string;
    /**
     * The name of the property, only visible to the editor
     */
    name?: string;
    /**
     * If changes to this property have to sync with the backend.
     * Usually used if other properties depend on it (validation/visibility),
     * or because the value should be cached (in case if the user disconnects).
     */
    updateOnChange?: boolean;
    /**
     * Validation settings for this property.
     */
    validation?: FormPropertyValidation;
    /**
     * If this property is mandatory to fill out by a user.
     */
    mandatory?: boolean;
    /**
     * If the value is a multi-value (example: a multi-select)
     */
    isList?: boolean;
    /**
     * If the property value is to be handled "raw".
     * No further information available however, what exactly that means.
     */
    isRaw?: boolean;
    /**
     * Unknown
     */
    formPage?: number;

    /*
     * Type Specific settings
     */

    /**
     * If this control is for date inputs, if only dates instead
     * of date-time should be selectable.
     */
    dateOnly?: boolean;
    /**
     * For numbers, which unit the number value should represent.
     */
    unit?: FormPropertyUnit;
    /**
     * For numbers, how many fraction digits are supported.
     * i.E: 2 = 0.01, 5 = 0.00001, 0 = 0 (integers only)
     */
    precision?: number;
    /**
     * How many characters need to be entered before a search can be used.
     */
    searchReferenceValueMinLength?: number;
    /**
     * How many characters need to be entered before a autocomplete can be used.
     */
    autoCompleteMinLength?: number;
    /**
     * For nested properties (aggregate).
     * Which sub-properties this property manages.
     */
    properties?: FormSchemaProperties;
}

export interface FormPage {
    /**
     * The name of the page, which is displayed to the editor and user.
     */
    pagename: I18nString;
    /**
     * The elements which are to be displayed in order.
     */
    elements: FormElement[];
}

export interface FormElement {
    /**
     * The ID of this element.
     * If this element is a control/property, then it has to be the ID of the property.
     */
    id: string;
    /**
     * Which type this element is. Can mostly be ignored, the relevant
     * type is saved in `formGridOptions.type` instead.
     */
    type: 'property' | 'aggregate';
    /**
     * The label of this element in all languages.
     */
    label: I18nString;
    /**
     * The description of this element in all languages.
     */
    description?: I18nString;
    /**
     * If a user uploads a CSV which contains this element's ID,
     * that the value of the CSV can be used to fill in this element's value.
     */
    csvUploadEnabled?: boolean;
    /**
     * Options from the Form Grid
     */
    formGridOptions?: FormGridOptions;
    /**
     * Sub-Elements, when this is a container block or a aggregate container.
     */
    elements?: Element[];
    /**
     * On which page this element belongs to
     */
    uiSchemaPage: number;
    /**
     * Unknown
     */
    editModeDialogFormID?: string;
}

export type ImageObjectFit = 'contain' | 'cover' | 'fill' | 'none' | 'scale-down';
export type ImageAlign = 'start' | 'center' | 'end' | 'stretch';

export interface FormGridImageOptions {
    /** URL to the image */
    url: string;
    /** Which (max) width the image should have */
    width?: number | 'auto';
    /** Which (max) height the image should have */
    height?: number | 'auto';
    objectFit?: ImageObjectFit;
    align?: ImageAlign;
    alt: I18nString;
    caption?: I18nString;
}

export interface FormGridOptions {
    /** @deprecated Will be removed */
    dependsOn?: string;

    /**
     * How many columns this element should use in the grid
     */
    numberOfColumns?: number;
    /**
     * How many rows this element should use in the grid
     */
    numberOfRows?: number;
    /**
     * How many lines (textarea) should be displayed
     */
    numberOfLines?: number;
    /** @deprecated Will be removed */
    hideListicons?: boolean;

    disableAdd?: boolean;
    disableDelete?: boolean;
    showFilter?: boolean;
    showOnlyFirstLine?: boolean;
    type?: string;
    value?: I18nString;
    valueSummary?: I18nString;
    replacementText?: boolean;
    ocr?: boolean;
    button?: boolean;
    buttonIsPrimary?: boolean;
    radio?: boolean;
    radioColumns?: number;
    dropdown?: boolean;

    numbersAfterPoint?: string;
    commaSeperator?: string;
    thousandsSeperatorCharacter?: string;
    thousandsSeperator?: boolean;
    currency?: string;
    numberPlaceholder?: string;
    numberStep?: number;

    fileExtensionsNotAllowed?: string[];

    alignment?: 'left' | 'center' | 'right';

    alignmentUnit?: 'left' | 'center' | 'right';

    tableRowMaxLineHeight?: number;
    tableColumnWidthOptimized?: boolean;

    buttonDisplayStyle?: 'icon' | 'text' | 'icontext';

    /**
     * If a overlay/popup modal should be shown before/after editing this element.
     */
    overlayOptions?: {
        isShowOverlay: boolean;
        texts: {
            textPre: I18nString;
            textPost: I18nString;
        };
    };

    image?: FormGridImageOptions;
    inForm?: boolean;
    inSummary?: boolean;
}

/**
 * Settings for number values, which define how a unit (i.E. kg/m/W/€/...) are to be displayed.
 * Defined as a placeholder for now, as the structure is yet to be determined.
 */
export interface FormPropertyUnit {}

export interface FormPropertyValidation {
    maxValue?: number;
    minValue?: number;
    maxLength?: number;
    minLength?: number;
    regexValidation?: {
        regex: string;
        errorMessage: I18nString;
    };
}

export interface FormgridType {
    name: string;
    value: string;
    options?: any;
}
