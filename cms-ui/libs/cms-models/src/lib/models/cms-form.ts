/* eslint-disable @typescript-eslint/naming-convention */
import { ExternalLink } from './external-link';
import { Folder } from './folder';
import { InheritableItem } from './item';
import { PageVersion } from './page';
import { SerializableRepositoryBrowserOptions } from './repository-browser';
import { DefaultModelType, ModelType, Normalizable, Raw } from './type-util';
import { User } from './user';

// type of MIME type of form elements. The type of a specific element is used as subtype
export const FORM_ELEMENT_MIME_TYPE_TYPE = 'gtx_form_element';

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

/** Information about the translation status of a form. */
export interface FormTranslationStatus {

    /** Form id of the form with which the given form is in sync */
    formId: number;

    /** Form name of the form with which the given form is in sync */
    name: string;

    /** Version timestamp of the synchronized version */
    versionTimestamp: number;

    /** Language of the synchronized version */
    language: string;

    /** True when the form is in sync with the latest version of the other language variant, false if not */
    inSync: boolean;

    /** Version number of the form version, with which this form is in sync */
    version: string;

    /** Latest version information */
    latestVersion: {
        /** Version timestamp */
        versionTimestamp: number;
        /** Version number */
        version: string;
    };

}

/**
 * External Link Checker form list item, contains the form and the external links
 */
export interface FormWithExternalLinks<T extends ModelType = DefaultModelType> {
    editable: boolean;
    form: Form<T>;
    links: ExternalLink[];
}

/**
 * Base for form representation in the CMS.
 *
 * https://www.gentics.com/Content.Node/cmp8/guides/restapi/json_Form.html
 */
interface FormBase<T extends ModelType = DefaultModelType> extends InheritableItem<T> {

    type: 'form';

    /** Filename */
    fileName: string;

    /** Description */
    description: string;

    /** ID of the folder this form is located in */
    folderId: number;

    /**
     * Whether the form is modified (the last version of the form is not the currently published one)
     */
    modified: boolean;

    /** ID of the publisher */
    publisher?: Normalizable<T, User<Raw>, number>;

    /** Publish Date as a Unix timestamp */
    pdate?: number;

    /** Time management */
    timeManagement: FormTimeManagement;

    /** Whether the form has time management set or not. */
    planned: boolean;

    /** Whether the form is in queue for being published or taken offline */
    queued: boolean;

    /** Language variants */
    languages: string[];

    /** Whether the form is currently online. */
    online: boolean;

    /** The folder that this form is located in */
    folder?: Normalizable<T, Folder<Raw>, number>;

    /** Whether this form is a master form */
    master: boolean;

    /** True if the form is locked */
    locked: boolean;

    /** Unix timestamp, since when the form is locked, or -1 if it is not locked */
    lockedSince: number;

    /** User, who locked the form */
    lockedBy?: Normalizable<T, User<Raw>, number>;

    /** Id of internal success page */
    successPageId?: number;

    /** Id of internal success page's node */
    successNodeId?: number;

    /** Id of e-mail template page */
    mailsource_pageid?: number;

    /** Id of e-mail template page's node */
    mailsource_nodeid?: number;

    /** Form version */
    version: PageVersion;
}

/**
 * Represents a form in the CMS.
 */
export interface Form<T extends ModelType = DefaultModelType> extends FormBase<T> {
    /** Inner form data */
    data: CmsFormData;
}

/**
 * Represents a form in the CMS with frontend extensions.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface FormBO<T extends ModelType = DefaultModelType> extends FormBase<T> {
    /** Inner form data */
    data: CmsFormDataBO;
}

/**
 * The user-editable properties of a Form object.
 */
export type EditableFormProps = Partial<Pick<Form, 'name' | 'description' | 'languages' | 'successNodeId' | 'successPageId'>> & {
    data?: Partial<CmsFormData>;
};


/**
 * Form-specific data.
 */
interface CmsFormDataBase {

    /** Email */
    email?: string;

    /** Success URL */
    successurl_i18n?: CmsFormElementI18nValue<string>;

    /** Success URL
     * @deprecated old property, interpret as if all languages of successurl_i18n were set to this value
     */
    successurl?: string;

    /** Mail subject */
    mailsubject_i18n?: CmsFormElementI18nValue<string>;

    /** Mail template */
    mailtemp_i18n?: CmsFormElementI18nValue<string>;

    /** Mail template page */
    mailsource_pageid?: number;

    /** Mail template page's node */
    mailsource_nodeid?: number;

}

/**
 * Form type
 */
export enum CmsFormType {
    GENERIC = 'GENERIC',
    POLL = 'POLL',
}

/**
 * Form-specific data and form elements.
 */
export interface CmsFormData extends CmsFormDataBase {
    type?: CmsFormType;

    templateContext?: string;

    elements: CmsFormElement[];
}

/**
 * Form-specific data and form elements with frontend extensions.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface CmsFormDataBO extends CmsFormDataBase {
    /** Form type */
    type: CmsFormType;

    templateContext: string;

    elements: CmsFormElementBO[];
}

/**
 * Basic CMS form element interface base.
 */
interface CmsFormElementBase {
    /** Globally unique ID. */
    globalId?: string;
    /** Instance name of input element. */
    name: string;
    /** Input type. Generic. */
    type: string;
    /** Visibility in form */
    active: boolean;
}

/**
 * Basic CMS form element interface.
 */
export interface CmsFormElement extends CmsFormElementBase {
    [key: string]: any;
    elements: CmsFormElement[];
}

/**
 * Basic CMS form element interface with frontend extensions.
 * @deprecated Create your own application specific type/business object instead.
 */
export interface CmsFormElementBO extends CmsFormElementBase {
    /** Human-readable Element type name. */
    type_label_i18n_ui: CmsFormElementI18nString;
    /** Description for what this element is used to know for the form editor. */
    description_i18n_ui: CmsFormElementI18nString;
    /** Element property that is used as a label for this element instance */
    label_property_ui?: string;
    /** Override for the key label for options. Only useable with `SELECTABLE_OPTIONS` types. */
    key_label_i18n_ui?: CmsFormElementI18nString;
    /** Override for the value label for options. Only useable with `SELECTABLE_OPTIONS` types. */
    value_label_i18n_ui?: CmsFormElementI18nString;
    /** If TRUE element can contain child elements. */
    isContainer: boolean;
    /** Strings of element type disallowed in container. */
    cannotContain?: string[];
    /** If isContainer = TRUE, this element has child elements. */
    /** Data relevant for FormGeneratorEditingUI property menu. If not defined, element cannot be edited in UI. */
    properties?: CmsFormElementProperty[];
    /** Array of sub elements. Not mandatory in the frontend. Making it optional differentiates CmsFormElementBO from CmsFormElement */
    elements?: CmsFormElementBO[];
}

/**
 * All values of CmsFormElement whose keys are suffixed with `_i18n` need
 * to provide this format.
 * Ideally, all keys listed in parent object `Form.languages` are defined here
 * to provide translation information for form rendering.
 *
 * @example
 * languages: [ "de", "en" ],
 * ...
 *    label_i18n : {
 *        de: "Geburtsdatum",
 *        en: "Date of birth"
 *    },
 */
export interface CmsFormElementI18nValue<T> { [key: string]: T; }
export interface CmsFormElementKeyI18nValuePair {
    key: string,
    value_i18n: CmsFormElementI18nValue<string>;
}

export interface CmsFormElementPropertyOption {
    key: string,
    value_i18n_ui: CmsFormElementI18nString,
}

export interface CmsFormElementI18nString {
    [key: string]: string;
}

/**
 * number: Accepts integer numbers
 * telephone: Accepts telephone numbers matching regex pattern ^[\s\d+/]*$
 * socialsecurity: Accepts austrian social security numbers matching regex pattern ^\s*(\d{4})(?<date>\d{6})\s*$
 * date: Accepts dates entered in format yyyy-MM-dd
 * time: Accepts times entered in format HH:mm
 * email: Accepts email addresses
 * datepicker: Accepts dates entered in format dd.MM.yyyy
 */
export type CmsFormElementValidator = 'DATE' | 'EMAIL' | 'NUMBER' | 'TELEPHONE' | 'TIME';

/**
 * Define input elements necessary to edit CmsFormElement in GCMS FormGeneratorEditingUI.
 */
interface CmsFormElementPropertyBase {
    name: string;
    /** UI label for editor */
    label_i18n_ui: CmsFormElementI18nString;
    /** Description for what the property is used to know for the form editor. */
    description_i18n_ui?: CmsFormElementI18nString;
    /** If user input is requried for surrounding form to be valid. */
    required?: boolean;
    /** If the user should be able to select muliple options. */
    multiple?: boolean;
}

export interface CmsFormElementPropertyDefault extends CmsFormElementPropertyBase {
    type: CmsFormElementPropertyType.BOOLEAN
    | CmsFormElementPropertyType.NUMBER
    | CmsFormElementPropertyType.STRING;
    validation?: CmsFormElementValidator;
    value_i18n?: CmsFormElementI18nValue<string | number | boolean | null>;
}

export interface CmsFormElementPropertySelect extends CmsFormElementPropertyBase {
    type: CmsFormElementPropertyType.SELECT;
    options: CmsFormElementPropertyOption[];
    value_i18n?: CmsFormElementI18nValue<string>;
}

export interface CmsFormElementPropertySelectableOptions extends CmsFormElementPropertyBase {
    type: CmsFormElementPropertyType.SELECTABLE_OPTIONS;
    value?: CmsFormElementKeyI18nValuePair[];
    key_label_i18n_ui?: CmsFormElementI18nString;
    value_label_i18n_ui?: CmsFormElementI18nString;
}

export interface CmsFormElementPropertyRepositoryBrowser extends CmsFormElementPropertyBase {
    type: CmsFormElementPropertyType.REPOSITORY_BROWSER;
    options: SerializableRepositoryBrowserOptions;
    nodeId?: number;
    value?: number | number[];
}

export interface CmsFormElementPropertyUnsupported extends CmsFormElementPropertyBase {
    type: CmsFormElementPropertyType.UNSUPPORTED;
    value?: any;
    value_i18n?: any;
}

export interface FormElementLabelPropertyI18nValues {
    [key: string]: CmsFormElementI18nValue<string>;
}

export type CmsFormElementProperty
    = CmsFormElementPropertyDefault
    | CmsFormElementPropertySelect
    | CmsFormElementPropertySelectableOptions
    | CmsFormElementPropertyRepositoryBrowser
    | CmsFormElementPropertyUnsupported;

export enum CmsFormElementPropertyType {
    BOOLEAN = 'BOOLEAN',
    NUMBER = 'NUMBER',
    STRING = 'STRING',
    SELECT = 'SELECT',
    SELECTABLE_OPTIONS = 'SELECTABLE_OPTIONS',
    REPOSITORY_BROWSER = 'REPOSITORY_BROWSER',
    UNSUPPORTED = 'UNSUPPORTED',
}


export interface FormElementDropInformation {
    element: CmsFormElementBO,
    formId: string,
}

export interface CmsFormElementInsertionInformation {
    element: CmsFormElementBO,
    insertionType: CmsFormElementInsertionType,
}

export enum CmsFormElementInsertionType {
    INSERT = 'INSERT',
    MOVE = 'MOVE',
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
