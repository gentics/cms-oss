/* eslint-disable @typescript-eslint/naming-convention */
/* eslint-disable @typescript-eslint/restrict-template-expressions */
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, filter, switchMap, take } from 'rxjs/operators';
import {
    FormConfigurationI18nString,
    FormEditorConfiguration,
    FormEditorConfigurationType,
    FormElementConfiguration,
    FormElementPropertyConfiguration,
    FormElementPropertyConfigurationDefault,
    FormElementPropertyConfigurationRepositoryBrowser,
    FormElementPropertyConfigurationSelect,
    FormElementPropertyConfigurationSelectableOptions,
    FormElementPropertyDefaultKeyI18nValuePair,
    FormElementPropertyOptionConfiguration,
    FormElementPropertyTypeConfiguration,
    FormElementPropertyValidatorConfiguration,
    FormPropertiesConfiguration,
} from '../../../common/models/form-editor-configuration';

const CUSTOMER_CONFIG_PATH = './../ui-conf/';

@Injectable()
export class FormEditorConfigurationService {

    constructor(private http: HttpClient) { }
    private configurationSubjects: { [key in FormEditorConfigurationType]?: BehaviorSubject<FormEditorConfiguration> } = {}

    private fetchFormEditorConfiguration = (type: FormEditorConfigurationType): Observable<FormEditorConfiguration>  => {
        let formEditorConfigurationPath = `${CUSTOMER_CONFIG_PATH}form-editor.json`;
        if (type !== 'GENERIC') {
            formEditorConfigurationPath = `${CUSTOMER_CONFIG_PATH}form-${type.toLowerCase()}-editor.json`;
        }

        return this.http.get(formEditorConfigurationPath, { responseType: 'text', params: {t: `${Date.now()}`} }).pipe(
            switchMap((formEditorConfiguration: any) => {
                try {
                    const parsedFormEditorConfiguration = JSON.parse(formEditorConfiguration);
                    try {
                        const prunedFormEditorConfiguration = this.validateFormEditorConfiguration(parsedFormEditorConfiguration);
                        return of(prunedFormEditorConfiguration);
                    } catch (error) {
                        console.error(`${error}`);
                        return throwError(error);
                    }
                } catch (_error) {
                    console.error(`Invalid JSON in ${formEditorConfigurationPath}! Default configuration used.`);
                    return throwError(_error);
                }
            }),
        );
    }

    /**
     * When this method is called with a specific type for the first time, the corresponding form configuration will be fetched.
     * Recurring calls with the same type do not cause the configuration to be refetched. A cached version is provided.
     * Since the configuration is never updated, the returned Observable completes after emitting a configuration.
     *
     * @param type of configuration needed
     * @returns Observable emitting configuration of the corresponding type
     */
    getConfiguration$ = (type: FormEditorConfigurationType): Observable<FormEditorConfiguration> => {
        if (!this.configurationSubjects[type]) {
            this.configurationSubjects[type] = new BehaviorSubject(null);
            this.fetchFormEditorConfiguration(type).subscribe((configuration: FormEditorConfiguration) => {
                this.configurationSubjects[type].next(configuration);
            }, err => {
                this.configurationSubjects[type].error(err);
            });
        }
        return this.configurationSubjects[type].asObservable().pipe(
            catchError(() => of(null)),
            filter(configuration => configuration !== null),
            take(1),
        );
    }

    private validateFormEditorConfiguration = (formEditorConfiguration: any): FormEditorConfiguration => {
        const prunedFormEditorConfiguration: FormEditorConfiguration = {
            form_properties: {},
            elements: [],
        };
        if (formEditorConfiguration) {
            if (formEditorConfiguration.form_properties) {
                const prunedFormProperties: FormPropertiesConfiguration = this.validateFormPropertiesConfiguration(
                    formEditorConfiguration.form_properties,
                );
                prunedFormEditorConfiguration.form_properties = prunedFormProperties;
            }
            if (Array.isArray(formEditorConfiguration.elements)) {
                const elements: FormElementConfiguration[] = formEditorConfiguration.elements;
                const prunedElementTypes: string[] = [];
                for (const element of elements) {
                    const prunedElement = this.validateFormElementConfiguration(element);
                    if (prunedElementTypes.includes(prunedElement.type)) {
                        throw new Error('Form editor configuration contains multiple element types of same name.');
                    }
                    prunedElementTypes.push(prunedElement.type);
                    prunedFormEditorConfiguration.elements.push(prunedElement);
                }
            } else {
                throw new Error('Form editor configuration contains invalid elements array.');
            }
        } else {
            throw new Error('Form editor configuration is undefined.');
        }
        return prunedFormEditorConfiguration;
    }

    private validateFormPropertiesConfiguration = (formPropertiesConfiguration: any): FormPropertiesConfiguration => {
        const prunedFormPropertiesConfiguration: FormPropertiesConfiguration = {};
        if (formPropertiesConfiguration.admin_mail_options) {
            if (Array.isArray(formPropertiesConfiguration.admin_mail_options)) {
                const prunedAdminMailOptions: FormElementPropertyOptionConfiguration[] = [];
                const options: FormElementPropertyOptionConfiguration[] = formPropertiesConfiguration.admin_mail_options;
                for (const option of options) {
                    if (!!option.key && typeof option.key === 'string') {
                        if (!!option.value_i18n_ui && this.isFormElementConfigurationI18nString(option.value_i18n_ui)) {
                            prunedAdminMailOptions.push({ key: option.key, value_i18n_ui: option.value_i18n_ui });
                        } else {
                            throw new Error('Form editor configuration contains invalid option value '
                                + `'${option.value_i18n_ui}' for form property name 'admin mail options'.`);
                        }
                    } else {
                        throw new Error('Form editor configuration contains invalid option key '
                            + `'${option.key}' for form property name 'admin mail options'.`);
                    }
                }
                prunedFormPropertiesConfiguration.admin_mail_options = prunedAdminMailOptions;
            } else {
                throw new Error('Form editor configuration contains invalid configuration for form property name \'admin mail options\'.');
            }
        }
        if (formPropertiesConfiguration.template_context_options) {
            if (Array.isArray(formPropertiesConfiguration.template_context_options)) {
                const prunedTemplateContextOptions: FormElementPropertyOptionConfiguration[] = [];
                const options: FormElementPropertyOptionConfiguration[] = formPropertiesConfiguration.template_context_options;
                for (const option of options) {
                    if (!!option.key && typeof option.key === 'string') {
                        if (!!option.value_i18n_ui && this.isFormElementConfigurationI18nString(option.value_i18n_ui)) {
                            prunedTemplateContextOptions.push({ key: option.key, value_i18n_ui: option.value_i18n_ui });
                        } else {
                            throw new Error('Form editor configuration contains invalid option value '
                             + `'${option.value_i18n_ui}' for form property name 'template context options'.`);
                        }
                    } else {
                        throw new Error('Form editor configuration contains invalid option key '
                         + `'${option.key}' for form property name 'template context options'.`);
                    }
                }
                prunedFormPropertiesConfiguration.template_context_options = prunedTemplateContextOptions;
            } else {
                throw new Error('Form editor configuration contains invalid configuration for form property name \'template context options\'.');
            }
        }
        return prunedFormPropertiesConfiguration;
    }

    private validateFormElementConfiguration = (formElementConfiguration: any): FormElementConfiguration => {
        let prunedFormElementConfigurationType: string = null;
        let prunedFormElementConfigurationTypeLabelI18NUi: FormConfigurationI18nString = null;
        let prunedFormElementConfigurationDescriptionI18NUi: FormConfigurationI18nString = null;
        let prunedFormElementConfigurationIsContainer: boolean = null;

        if (!!formElementConfiguration.type && typeof formElementConfiguration.type === 'string') {
            prunedFormElementConfigurationType = formElementConfiguration.type;
        } else {
            throw new Error(`Form editor configuration contains invalid element type '${formElementConfiguration.type}'.`);
        }

        if (!!formElementConfiguration.type_label_i18n_ui &&
            this.isFormElementConfigurationI18nString(formElementConfiguration.type_label_i18n_ui)) {
            prunedFormElementConfigurationTypeLabelI18NUi = formElementConfiguration.type_label_i18n_ui;
        } else {
            throw new Error(`Form editor configuration contains invalid element i18n UI label for element type '${formElementConfiguration.type}'.`);
        }

        if (!!formElementConfiguration.description_i18n_ui &&
            this.isFormElementConfigurationI18nString(formElementConfiguration.description_i18n_ui)) {
            prunedFormElementConfigurationDescriptionI18NUi = formElementConfiguration.description_i18n_ui;
        } else {
            throw new Error(`Form editor configuration contains invalid element i18n UI description for element type '${formElementConfiguration.type}'.`);
        }

        if (typeof formElementConfiguration.is_container === 'boolean') {
            prunedFormElementConfigurationIsContainer = formElementConfiguration.is_container;
        } else {
            throw new Error(`Form editor configuration contains invalid container flag for element type '${formElementConfiguration.type}'.`);
        }

        const prunedFormElementConfiguration: FormElementConfiguration = {
            type: prunedFormElementConfigurationType,
            type_label_i18n_ui: prunedFormElementConfigurationTypeLabelI18NUi,
            description_i18n_ui: prunedFormElementConfigurationDescriptionI18NUi,
            is_container: prunedFormElementConfigurationIsContainer,
            properties: [],
        }
        if (prunedFormElementConfiguration.is_container && !!formElementConfiguration.cannot_contain) {
            if (Array.isArray(formElementConfiguration.cannot_contain)) {
                prunedFormElementConfiguration.cannot_contain = [];
                for (const elementType of formElementConfiguration.cannot_contain) {
                    if (!!elementType && typeof elementType === 'string') {
                        prunedFormElementConfiguration.cannot_contain.push(elementType);
                    } else {
                        throw new Error(`Form editor configuration contains invalid disallowed contained type '${elementType}' for element type '${formElementConfiguration.type}'.`);
                    }
                }
            } else {
                throw new Error(`Form editor configuration contains invalid set of disallowed contained types for element type '${formElementConfiguration.type}'.`);
            }
        }

        if (Array.isArray(formElementConfiguration.properties)) {
            const properties: FormElementPropertyConfiguration[] = formElementConfiguration.properties;
            const prunedPropertyNames: string[] = [];
            for (const property of properties) {
                const prunedElementProperty = this.validateFormElementPropertyConfiguration(property, formElementConfiguration.type);
                if (prunedPropertyNames.includes(prunedElementProperty.name)) {
                    throw new Error('Form editor configuration contains multiple element properties of same name.');
                }
                prunedPropertyNames.push(prunedElementProperty.name);
                prunedFormElementConfiguration.properties.push(prunedElementProperty);
            }
        } else {
            throw new Error(`Form editor configuration contains invalid properties for element type '${formElementConfiguration.type}'.`);
        }

        if (formElementConfiguration.label_property_ui) {
            if (this.isValidFormElementPropertyLabelUi(formElementConfiguration.label_property_ui, prunedFormElementConfiguration.properties)) {
                prunedFormElementConfiguration.label_property_ui = formElementConfiguration.label_property_ui;
            } else {
                throw new Error(`Form editor configuration contains invalid element label property for element type '${formElementConfiguration.type}'.`);
            }
        }

        return prunedFormElementConfiguration;
    }

    private validateFormElementPropertyConfiguration = (formElementPropertyConfiguration: any, elementType: string): FormElementPropertyConfiguration => {
        let prunedFormElementPropertyConfigurationName: string = null;
        let prunedFormElementPropertyConfigurationType: FormElementPropertyTypeConfiguration = null;
        let prunedFormElementPropertyConfigurationLabelI18NUi: FormConfigurationI18nString = null;

        if (!!formElementPropertyConfiguration.name && typeof formElementPropertyConfiguration.name === 'string') {
            if (['globalId', 'name', 'type', 'active', 'elements'].includes(formElementPropertyConfiguration.name)) {
                throw new Error('Form editor configuration contains invalid property name '
                    + `'${formElementPropertyConfiguration.name}' for element type '${elementType}'. Name must not be 'globalId', 'name', 'type', 'active' or 'elements'.`);
            }
            prunedFormElementPropertyConfigurationName = formElementPropertyConfiguration.name;
        } else {
            throw new Error('Form editor configuration contains invalid property name '
                + `'${formElementPropertyConfiguration.name}' for element type '${elementType}'.`);
        }

        if (!!formElementPropertyConfiguration.type && this.isFormElementPropertyTypeConfiguration(formElementPropertyConfiguration.type)) {
            prunedFormElementPropertyConfigurationType = formElementPropertyConfiguration.type;
        } else {
            throw new Error('Form editor configuration contains invalid property type '
                + `'${formElementPropertyConfiguration.type}' for property '${formElementPropertyConfiguration.name}' in element type '${elementType}'.`);
        }

        if (!!formElementPropertyConfiguration.label_i18n_ui &&
            this.isFormElementConfigurationI18nString(formElementPropertyConfiguration.label_i18n_ui)) {
            prunedFormElementPropertyConfigurationLabelI18NUi = formElementPropertyConfiguration.label_i18n_ui;
        } else {
            throw new Error('Form editor configuration contains invalid property i18n UI label '
                + `'${formElementPropertyConfiguration.label_i18n_ui}' for property '${formElementPropertyConfiguration.name}' in element type '${elementType}'.`);
        }

        let prunedFormElementPropertyConfiguration: FormElementPropertyConfiguration;

        switch (prunedFormElementPropertyConfigurationType) {
            case FormElementPropertyTypeConfiguration.SELECT: {
                const prunedOptions: FormElementPropertyOptionConfiguration[] = [];
                if (Array.isArray(formElementPropertyConfiguration.options)) {
                    const options: FormElementPropertyOptionConfiguration[] = formElementPropertyConfiguration.options;
                    for (const option of options) {
                        if (!!option.key && typeof option.key === 'string') {
                            if (!!option.value_i18n_ui && this.isFormElementConfigurationI18nString(option.value_i18n_ui)) {
                                prunedOptions.push({ key: option.key, value_i18n_ui: option.value_i18n_ui });
                            } else {
                                throw new Error('Form editor configuration contains invalid option value '
                                    + `'${option.value_i18n_ui}' for property name '${formElementPropertyConfiguration.name}' `
                                    + `in element type '${elementType}'.`);
                            }
                        } else {
                            throw new Error('Form editor configuration contains invalid option key '
                                + `'${option.key}' for property name '${formElementPropertyConfiguration.name}' in element type '${elementType}'.`);
                        }
                    }
                } else {
                    throw new Error('Form editor configuration contains invalid options for property name '
                        + `'${formElementPropertyConfiguration.name}' of type 'SELECT' in element type '${elementType}'.`);
                }

                const prunedFormElementPropertyConfigurationSelect: FormElementPropertyConfigurationSelect = {
                    name: prunedFormElementPropertyConfigurationName,
                    type: prunedFormElementPropertyConfigurationType,
                    label_i18n_ui: prunedFormElementPropertyConfigurationLabelI18NUi,
                    options: prunedOptions,
                }

                if (formElementPropertyConfiguration.default_value_i18n) {
                    if (this.isFormElementPropertyDefaultValueI18n(formElementPropertyConfiguration.default_value_i18n, ['string'])) {
                        prunedFormElementPropertyConfigurationSelect.default_value_i18n = formElementPropertyConfiguration.default_value_i18n;
                    } else {
                        throw new Error('Form editor configuration contains invalid property default value '
                            + `'${formElementPropertyConfiguration.default_value_i18n}' for property '${formElementPropertyConfiguration.name}' in element type '${elementType}'.`);
                    }
                }

                prunedFormElementPropertyConfiguration = prunedFormElementPropertyConfigurationSelect;
                break;
            }

            case FormElementPropertyTypeConfiguration.SELECTABLE_OPTIONS: {
                const prunedFormElementPropertyConfigurationSelectableOptions: FormElementPropertyConfigurationSelectableOptions = {
                    name: prunedFormElementPropertyConfigurationName,
                    type: prunedFormElementPropertyConfigurationType,
                    label_i18n_ui: prunedFormElementPropertyConfigurationLabelI18NUi,
                };

                const labelOverrideProperties = ['key_label_i18n_ui', 'value_label_i18n_ui'];
                for (const labelOverride of labelOverrideProperties) {
                    if (formElementPropertyConfiguration[labelOverride]) {
                        if (this.isFormElementConfigurationI18nString(formElementPropertyConfiguration[labelOverride])) {
                            prunedFormElementPropertyConfigurationSelectableOptions[labelOverride] = formElementPropertyConfiguration[labelOverride];
                        } else {
                            throw new Error('Form editor configuration contains invalid option value '
                                + `'${labelOverride}' for property name '${formElementPropertyConfiguration.name}' in element type '${elementType}'.`);
                        }
                    }
                }

                if (formElementPropertyConfiguration.default_value) {
                    if (Array.isArray(formElementPropertyConfiguration.default_value)) {
                        prunedFormElementPropertyConfigurationSelectableOptions.default_value = [];
                        const keyValuePairs: FormElementPropertyDefaultKeyI18nValuePair[] = formElementPropertyConfiguration.default_value;
                        for (const keyValuePair of keyValuePairs) {
                            if (!!keyValuePair.key && typeof keyValuePair.key === 'string') {
                                if (!!keyValuePair.value_i18n && this.isFormElementPropertyDefaultValueI18n(keyValuePair.value_i18n, ['string'])) {
                                    prunedFormElementPropertyConfigurationSelectableOptions
                                        .default_value
                                        .push({ key: keyValuePair.key, value_i18n: keyValuePair.value_i18n });
                                } else {
                                    throw new Error('Form editor configuration contains invalid property default value '
                                        + `'${keyValuePair.value_i18n}' for property name '${formElementPropertyConfiguration.name}' in element type '${elementType}'.`);
                                }
                            } else {
                                throw new Error('Form editor configuration contains invalid property default value key '
                                    + `'${keyValuePair.key}' for property name '${formElementPropertyConfiguration.name}' in element type '${elementType}'.`);
                            }
                        }
                    } else {
                        throw new Error('Form editor configuration contains invalid property default value '
                            + `'${formElementPropertyConfiguration.default_value}' for property '${formElementPropertyConfiguration.name}' in element type '${elementType}'.`);
                    }
                }

                prunedFormElementPropertyConfiguration = prunedFormElementPropertyConfigurationSelectableOptions;
                break;
            }

            case FormElementPropertyTypeConfiguration.REPOSITORY_BROWSER: {
                // TODO Proper Repository Browser config validation
                const prunedFormElementPropertyConfigurationRepositoryBrowser: FormElementPropertyConfigurationRepositoryBrowser = {
                    name: prunedFormElementPropertyConfigurationName,
                    type: prunedFormElementPropertyConfigurationType,
                    label_i18n_ui: prunedFormElementPropertyConfigurationLabelI18NUi,
                    options: formElementPropertyConfiguration.options,
                }

                prunedFormElementPropertyConfiguration = prunedFormElementPropertyConfigurationRepositoryBrowser;
                break;
            }

            case FormElementPropertyTypeConfiguration.BOOLEAN:
            case FormElementPropertyTypeConfiguration.NUMBER:
            case FormElementPropertyTypeConfiguration.STRING:
            default: {
                const prunedFormElementPropertyConfigurationDefault: FormElementPropertyConfigurationDefault = {
                    name: prunedFormElementPropertyConfigurationName,
                    type: prunedFormElementPropertyConfigurationType,
                    label_i18n_ui: prunedFormElementPropertyConfigurationLabelI18NUi,
                }
                if (formElementPropertyConfiguration.validator) {
                    if (this.isFormElementPropertyValidatorConfiguration(formElementPropertyConfiguration.validator)) {
                        prunedFormElementPropertyConfigurationDefault.validator = formElementPropertyConfiguration.validator;
                    } else {
                        throw new Error('Form editor configuration contains invalid property validator '
                            + `'${formElementPropertyConfiguration.validator}' for property '${formElementPropertyConfiguration.name}' in element type '${elementType}'.`);
                    }
                }

                if (formElementPropertyConfiguration.default_value_i18n) {
                    if (this.isFormElementPropertyDefaultValueI18n(
                        formElementPropertyConfiguration.default_value_i18n,
                        [prunedFormElementPropertyConfigurationType.toLowerCase()],
                    )) {
                        prunedFormElementPropertyConfigurationDefault.default_value_i18n = formElementPropertyConfiguration.default_value_i18n;
                    } else {
                        throw new Error('Form editor configuration contains invalid property default value '
                            + `'${formElementPropertyConfiguration.default_value_i18n}' for property '${formElementPropertyConfiguration.name}' in element type '${elementType}'.`);
                    }
                }

                prunedFormElementPropertyConfiguration = prunedFormElementPropertyConfigurationDefault;
                break;
            }
        }

        if (formElementPropertyConfiguration.description_i18n_ui) {
            if (this.isFormElementConfigurationI18nString(formElementPropertyConfiguration.description_i18n_ui)) {
                prunedFormElementPropertyConfiguration.description_i18n_ui = formElementPropertyConfiguration.description_i18n_ui;
            } else {
                throw new Error('Form editor configuration contains invalid property description '
                    + `'${formElementPropertyConfiguration.description_i18n_ui}' for property '${formElementPropertyConfiguration.name}' in element type '${elementType}'.`);
            }
        }

        if (formElementPropertyConfiguration.required !== undefined && formElementPropertyConfiguration !== null) {
            if (typeof formElementPropertyConfiguration.required === 'boolean') {
                prunedFormElementPropertyConfiguration.required = formElementPropertyConfiguration.required;
            } else {
                throw new Error('Form editor configuration contains invalid property required setting '
                    + `for property '${formElementPropertyConfiguration.name}' in element type '${elementType}'.`);
            }
        }
        return prunedFormElementPropertyConfiguration;
    }

    private isValidFormElementPropertyLabelUi = (formElementPropertyLabelUi: any, formElementProperties: FormElementPropertyConfiguration[]):
    boolean => {
        for (const formElementProperty of formElementProperties) {
            if (formElementProperty.type === FormElementPropertyTypeConfiguration.STRING
                && formElementPropertyLabelUi === formElementProperty.name) {
                return true;
            }
        }
        return false;
    }


    // type predicates

    private isFormElementPropertyTypeConfiguration = (formElementPropertyType: any):
        formElementPropertyType is FormElementPropertyTypeConfiguration => {
        for (const type of Object.values(FormElementPropertyTypeConfiguration)) {
            if (formElementPropertyType === type) {
                return true;
            }
        }
        return false;
    }

    private isFormElementPropertyValidatorConfiguration = (formElementPropertyValidator: any):
        formElementPropertyValidator is FormElementPropertyValidatorConfiguration => {
        for (const validator of Object.values(FormElementPropertyValidatorConfiguration)) {
            if (formElementPropertyValidator === validator) {
                return true;
            }
        }
        return false;
    }

    private isFormElementConfigurationI18nString = (formElementConfigurationI18nString: any):
        formElementConfigurationI18nString is FormConfigurationI18nString => {
        if (typeof formElementConfigurationI18nString === 'object' && formElementConfigurationI18nString !== null) {
            for (const language of Object.keys(formElementConfigurationI18nString)) {
                if (typeof formElementConfigurationI18nString[language] !== 'string') {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private isFormElementPropertyDefaultValueI18n = (formElementConfigurationI18nString: any, types: string[]):
        formElementConfigurationI18nString is FormConfigurationI18nString => {
        if (typeof formElementConfigurationI18nString === 'object' && formElementConfigurationI18nString !== null) {
            for (const language of Object.keys(formElementConfigurationI18nString)) {
                if (!types.includes(typeof formElementConfigurationI18nString[language])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
