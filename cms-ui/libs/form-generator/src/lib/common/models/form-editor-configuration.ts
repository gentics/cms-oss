import { SerializableRepositoryBrowserOptions } from '@gentics/cms-models';

/**
 * A customer configuration that allows defining the available types of form elements.
 * It is loaded from /.Node/customer-config/config/form-editor.json and ignored if missing.
 */
export interface FormEditorConfiguration {
    form_properties: FormPropertiesConfiguration;
    elements: FormElementConfiguration[];
}

// can be changed to enum and then automatically converted to union type with `${FormEditorConfigurationType}` in TS 4.1
export type FormEditorConfigurationType = 'GENERIC' | 'POLL';

export interface FormPropertiesConfiguration {
    admin_mail_options?: FormElementPropertyOptionConfiguration[];
    template_context_options?: FormElementPropertyOptionConfiguration[];
}

export interface FormElementConfiguration {
    type: string;
    type_label_i18n_ui: FormConfigurationI18nString;
    description_i18n_ui: FormConfigurationI18nString;
    label_property_ui?: string;
    is_container: boolean;
    cannot_contain?: string[];
    properties: FormElementPropertyConfiguration[];
}

export const UNKNOWN_ELEMENT_TYPE = 'UNKNOWN';

interface FormElementPropertyConfigurationBase {
    name: string;
    label_i18n_ui: FormConfigurationI18nString;
    description_i18n_ui?: FormConfigurationI18nString;
    required?: boolean;
}

export interface FormElementPropertyConfigurationDefault extends FormElementPropertyConfigurationBase {
    type: FormElementPropertyTypeConfiguration.BOOLEAN
        | FormElementPropertyTypeConfiguration.NUMBER
        | FormElementPropertyTypeConfiguration.STRING;
    validator?: FormElementPropertyValidatorConfiguration;
    default_value_i18n?: FormElementPropertyDefaultI18nValue<string | number | boolean | null>;
}

export interface FormElementPropertyConfigurationSelect extends FormElementPropertyConfigurationBase {
    type: FormElementPropertyTypeConfiguration.SELECT;
    options: FormElementPropertyOptionConfiguration[];
    default_value_i18n?: FormElementPropertyDefaultI18nValue<string>;
    multiple?: boolean;
}

export interface FormElementPropertyConfigurationSelectableOptions extends FormElementPropertyConfigurationBase {
    type: FormElementPropertyTypeConfiguration.SELECTABLE_OPTIONS;
    default_value?: FormElementPropertyDefaultKeyI18nValuePair[];
    key_label_i18n_ui?: FormConfigurationI18nString;
    value_label_i18n_ui?: FormConfigurationI18nString;
}

export interface FormElementPropertyConfigurationRepositoryBrowser extends FormElementPropertyConfigurationBase {
    type: FormElementPropertyTypeConfiguration.REPOSITORY_BROWSER;
    options: SerializableRepositoryBrowserOptions;
    default_value?: number | number[] | null;
    required?: boolean;
}

export type FormElementPropertyConfiguration
    = FormElementPropertyConfigurationDefault
    | FormElementPropertyConfigurationSelect
    | FormElementPropertyConfigurationSelectableOptions
    | FormElementPropertyConfigurationRepositoryBrowser;

export enum FormElementPropertyTypeConfiguration {
    BOOLEAN = 'BOOLEAN',
    NUMBER = 'NUMBER',
    STRING = 'STRING',
    SELECT = 'SELECT',
    SELECTABLE_OPTIONS = 'SELECTABLE_OPTIONS',
    REPOSITORY_BROWSER = 'REPOSITORY_BROWSER',
}

export enum FormElementPropertyValidatorConfiguration {
    DATE = 'DATE',
    EMAIL = 'EMAIL',
    NUMBER = 'NUMBER',
    TELEPHONE = 'TELEPHONE',
    TIME = 'TIME',
}

export interface FormElementPropertyOptionConfiguration {
    key: string;
    value_i18n_ui: FormConfigurationI18nString;
}

export interface FormConfigurationI18nString {
    [key: string]: string;
}

export interface FormElementPropertyDefaultI18nValue<T> {
    [key: string]: T;
}

export interface FormElementPropertyDefaultKeyI18nValuePair {
    key: string;
    value_i18n: FormElementPropertyDefaultI18nValue<string>;
}
