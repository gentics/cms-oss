import { I18nString } from './common';
import { FormCondition } from './form-conditions';

export enum FormSettingType {
    STRING = 'string',
    TRANSLATION = 'translation',
    INTEGER = 'integer',
    NUMBER = 'number',
    BOOLEAN = 'boolean',
    DATE = 'date',
    SELECT = 'select',
    OPTIONS = 'options',
    USER = 'user',
    REFERENCE = 'reference',
}

/**
 * A setting which can be configured by the form creator
 */
export type FormSettingConfiguration
    = | FormStringSetting
      | FormTranslationSetting
      | FormIntegerSetting
      | FormNumberSetting
      | FormBooleanSetting
      | FormDateSetting
      | FormSelectSetting
      | FormOptionsSetting
      | FormUserSetting
      | FormReferenceSetting;

/**
 * Simple string that can be entered
 */
export type FormStringSetting = FormBaseSetting & {
    type: FormSettingType.STRING;
    richContent?: boolean;
};

/**
 * Text which may be entered on a per language basis
 */
export type FormTranslationSetting = FormBaseSetting & {
    type: FormSettingType.TRANSLATION;
    richContent?: boolean;
};

/**
 * Integer number that can be entered
 */
export type FormIntegerSetting = FormBaseSetting & {
    type: FormSettingType.INTEGER;
};

/**
 * All numbers that can be entered
 */
export type FormNumberSetting = FormBaseSetting & {
    type: FormSettingType.NUMBER;
};

/**
 * Boolean/Checkbox that can be checked
 */
export type FormBooleanSetting = FormBaseSetting & {
    type: FormSettingType.BOOLEAN;
};

/**
 * Date that can be selected. Value will be a ISO-Date string
 */
export type FormDateSetting = FormBaseSetting & {
    type: FormSettingType.DATE;
    allowTime?: boolean;
};

/**
 * Select where one or more options can be selected
 */
export type FormSelectSetting = FormBaseSetting & {
    type: FormSettingType.SELECT;
    options: FormSelectOption[];
    multiple?: boolean;
};

/**
 * Allow the editor to create options which the user can then use
 */
export type FormOptionsSetting = FormBaseSetting & {
    type: FormSettingType.OPTIONS;
    multiple?: boolean;
};

/**
 * Select where one or more options from the user options can be selected
 */
export type FormUserSetting = FormBaseSetting & {
    type: FormSettingType.USER;
    multiple?: boolean;
};

export enum FormReferenceItemType {
    PAGE = 'page',
    FILE = 'file',
    IMAGE = 'image',
    FOLDER = 'folder',
}

export enum FormReferencePublishType {
    /**
     * If the item should be rendered and it's content is to be published.
     * Only applicable for Pages.
     */
    CONTENT = 'content',
    /**
     * Generates a URL to the item(s) which is reachable in the front-end.
     */
    URL = 'url',
    /**
     * Generates a Mesh-Link to the item(s) which can be resolved via Mesh.
     */
    MESH_LINK = 'meshlink',
}

/**
 * Picker where a reference to one or more CMS Objects can be selected
 */
export type FormReferenceSetting = FormBaseSetting & {
    type: FormSettingType.REFERENCE;
    /**
     * Which items are allowed to be selected
     */
    referenceTypes: FormReferenceItemType[];
    /**
     * How the referenced item(s) should be published into Mesh.
     */
    publishAs: FormReferencePublishType;
    /**
     * For pages, which markup-language the template of the page has to have to be selectable
     */
    allowedMarkupLanguageIds?: number[];
    /**
     * If multiple items can be picked
     */
    multiple?: boolean;
    /**
     * If only items from the current node (from where the node is), are allowed to be selected.
     */
    onlyInCurrentNode?: boolean;
    /**
     * The title in the repository-browser
     */
    titleI18n?: I18nString;
};

export type FormControlConfiguration = FormElementConfiguration & FormControlAggregateSettings;

/**
 * Aggregation allows a group of controls to be entered multiple times. The value will be aggregated into a list of objects
 */
export type FormControlAggregateSettings = {
    aggregate?: false;
} | {
    aggregate: true;
    /**
     * List of control types which are allowed to be inserted into this container
     */
    whitelist?: string[];
};

export type FormBlockConfiguration = FormElementConfiguration & FormBlockContainerSettings;

export type FormBlockContainerSettings = {
    container?: false;
} | {
    /**
     * If this block may contain other elements. Used for grouping together related elements
     */
    container: true;
    /**
     * List of element types which are allowed to be inserted into this container
     */
    whitelist?: string[];
};

/**
 * Configurations of all form types
 */
export interface FormConfiguration {
    [typeName: string]: FormTypeConfiguration;
}

/**
 * The configuration for a single form type
 */
export interface FormTypeConfiguration {
    /**
     * The type/ID of the form configuration
     */
    type: string;
    /**
     * The human readable name/label for this configuration
     */
    nameI18n?: I18nString;
    /**
     * If the form should only be able to be submitted once per user
     */
    submitOnce?: boolean;
    /**
     * If this configuration is to be used for external forms
     */
    external?: boolean;
    /**
     * The name of the mesh plugin which is to handle this form type
     */
    pluginName: string;
    /**
     * Configuration which is passed to the Plugin to configure the behaviour for this type
     */
    pluginConfiguration?: Record<string, unknown>;
    /**
     * All available controls, where the key is the ID of the control
     */
    controls: Record<string, FormControlConfiguration>;
    /**
     * All available blocks, where the key is the ID of the block
     */
    blocks?: Record<string, FormBlockConfiguration>;
    /**
     * Options which can be selected for user reference information
     */
    userOptions?: FormSelectOption[];
    /**
     * Which styling variations the form-type has
     */
    templateOptions?: FormSelectOption[];
    /**
     * Options which determine which email templates are available, which will be sent to the user upon submitting a form
     */
    userEmailTemplateOptions?: FormSelectOption[];
    /**
     * Options which determine which email templates are available, which will be sent to the admin emails, when a end-user submits a form
     */
    adminEmailTemplateOptions?: FormSelectOption[];
    /**
     * Which form flows this type can use.
     * The first element in this array is will be used as default value.
     */
    flows: FormFlow[];
}

/**
 * Configuration for a single element
 */
export interface FormElementConfiguration {
    /**
     * The label for this element in various languages
     */
    labelI18n: I18nString;
    /**
     * The description for this element in various languages
     */
    descriptionI18n?: I18nString;
    /**
     * The settings which an editor can configure when inserting this element
     */
    settings?: FormSettingConfiguration[];
}

/**
 * Common properties for all setting types
 */
export interface FormBaseSetting {
    type: FormSettingType;
    /**
     * The ID of the setting. Will be used to read the settings value in the backend or form-gen
     */
    id: string;
    /**
     * The label for this setting in various languages
     */
    labelI18n: I18nString;
    /**
     * The description for this setting in various languages
     */
    descriptionI18n?: I18nString;
    /**
     * If the editor is required to fill out the setting
     */
    required?: boolean;
    /**
     * If this setting is only to be processed/used in the backend/mesh-plugin, and not to be forwarded to form-gen
     */
    backend?: boolean;
    /**
     * Which condition needs to be met, in order for this setting to be displayed to the editor.
     */
    condition?: FormCondition;
}

export interface FormSelectOption {
    value: string;
    labelI18n: I18nString;
}

export interface FormFlow {
    /** The ID of the flow */
    id: string;
    /**
     * The label/name of this flow which is displayed to the editor.
     */
    labelI18n: I18nString;
    /**
     * The translation key to use to get the name of this flow for form-gen, and is shown to the front-end user.
     */
    nameTranslationKey: string;
    /**
     * The individual steps for the flow
     */
    steps: FormFlowStep[];
}

export interface FormFlowStep {
    /** The translation key for the name of this flow step */
    nameTranslationKey: string;
    /** The translation key for the button label which leads to this flow step */
    buttonTextTranslationKey: string;
    /** The translation key for the description of this flow step */
    descriptionTranslationKey?: string;
    /** The React Native component class which renders this step */
    reactClass: string;
    /** Additional buttons which are displayed in the footer */
    buttons?: FormFlowButton[];
    /** Variables which are passed to the screen component as props */
    variables?: FormFlowVariable[];
}

export interface FormFlowVariable {
    name: string;
    value: any;
}

export interface FormFlowButton {
    /** The React Native component class which renders this button */
    reactClass: string;
    /** The translation key for the label of this button */
    labelTranslationKey: string;
    /** Hides the button on mobile devices (if this button implementation has this feature) */
    mobileDisabled?: boolean;
}
