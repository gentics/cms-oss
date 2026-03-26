import { I18nString } from './common';
import { Condition } from './form-conditions';

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
export type FormStringSetting = FormBaseSetting;

/**
 * Text which may be entered on a per language basis
 */
export type FormTranslationSetting = FormBaseSetting;

/**
 * Integer number that can be entered
 */
export type FormIntegerSetting = FormBaseSetting;

/**
 * All numbers that can be entered
 */
export type FormNumberSetting = FormBaseSetting;

/**
 * Boolean/Checkbox that can be checked
 */
export type FormBooleanSetting = FormBaseSetting;

/**
 * Date that can be selected. Value will be a ISO-Date string
 */
export type FormDateSetting = FormBaseSetting;

/**
 * Select where one or more options can be selected
 */
export type FormSelectSetting = FormBaseSetting;

/**
 * Allow the editor to create options which the user can then use
 */
export type FormOptionsSetting = FormBaseSetting;

/**
 * Select where one or more options from the user options can be selected
 */
export type FormUserSetting = FormBaseSetting;

/**
 * Picker where a reference to one or more CMS Objects can be selected
 */
export type FormReferenceSetting = FormBaseSetting;

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
     * If this configuration is to be used for internal forms
     */
    internal?: boolean;
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
    condition?: Condition;
}

export interface FormSelectOption {
    value: string;
    label: I18nString;
}
