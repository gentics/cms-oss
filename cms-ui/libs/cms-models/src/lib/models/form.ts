import { I18nString } from './common';
import { InheritableItem } from './item';

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

export interface EdtiableFormProperties {
    /**
     * The name of the form
     */
    name: string;
    /**
     * Description of the form
     */
    description: string;
    /**
     * The languages the form supports.
     */
    languages: string[];
}

interface FormBase extends InheritableItem, EdtiableFormProperties {
    /**
     * The folderId in which the form resides in
     */
    readonly folderId: number;
    /**
     * The translated label of `formType`
     */
    readonly formTypeLabel: string;
    /**
     * The schema definition of the form
     */
    schema: FormSchema;
    /**
     * The ui-schema definition of the form
     */
    uiSchema: UiSchema;
}

export interface InternalForm extends FormBase {
    /**
     * The form-type of the form. Used to identify which form-configuration
     * has has to be used for this form.
     */
    formType: string;
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

export interface ExternalForm extends FormBase {
    /**
     * The foreign/external ID of an external form.
     */
    readonly externalId?: string;
    /**
     * The foreign/external Version of an external form.
     */
    readonly externalVersion?: string;
    /**
     * The form-type of the form. Used to identify which form-configuration
     * has has to be used for this form.
     */
    readonly formType: string;
}

export type Form = InternalForm | ExternalForm;

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
     * @deprecated Use `form.name` instead?
     */
    formname: I18nString;
    /**
     * @deprecated Use `form.description` instead?
     */
    formdescription: I18nString;
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

    /** @deprecated Use `formGrid.width` instead */
    formwidth: number;
    /** @deprecated Use `formGrid.widthOptimized` instead */
    formwidthOptimized: boolean;
    /** @deprecated Use `formGrid.flow` instead */
    formFlowTemplateKey?: string;
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
    elements: Element[];
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

export interface FormFlow {
    key: string;
    name: string;

    formFlowSteps: FormFlowStep[];
}

export interface FormFlowStep {
    name: string;
    description: string;

    buttonText: string;
    reactClass: string;
    buttonDefinition: FormFlowButton[];
    variables: any[];
}

export interface FormFlowButton {
    btnComponent: string;
    translateKey: string;
    mobileDisabled?: boolean;
}

export interface FormgridType {
    name: string;
    value: string;
    options?: any;
}
