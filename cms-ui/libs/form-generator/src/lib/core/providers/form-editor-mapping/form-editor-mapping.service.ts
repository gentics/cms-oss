import { Injectable } from '@angular/core';
import {
    CmsFormElement,
    CmsFormElementBO,
    CmsFormElementPropertyType,
    CmsFormElementPropertyUnsupported,
    CmsFormType,
    Form,
    FormBO,
    Normalized,
    Raw,
} from '@gentics/cms-models';
import {
    FormEditorConfiguration,
    FormElementConfiguration,
    FormElementPropertyOptionConfiguration,
    FormElementPropertyTypeConfiguration,
    UNKNOWN_ELEMENT_TYPE,
} from '../../../common/models/form-editor-configuration';
import { cloneDeep } from 'lodash-es';

@Injectable()
export class FormEditorMappingService {

    mapFormBOToForm = (form: FormBO<Raw | Normalized>): Form<Raw | Normalized> => {
        form = cloneDeep(form);
        if (form) {
            const mappedForm: Form<Raw | Normalized> = {
                id: form.id,
                globalId: form.globalId,
                name: form.name,
                masterNodeId: form.masterNodeId,
                creator: form.creator,
                cdate: form.cdate,
                editor: form.editor,
                edate: form.edate,
                type: form.type,
                deleted: form.deleted,
                masterDeleted: form.masterDeleted,
                folderDeleted: form.folderDeleted,
                usage: form.usage,
                excluded: form.excluded,
                inherited: form.inherited,
                inheritedFrom: form.inheritedFrom,
                inheritedFromId: form.inheritedFromId,
                masterNode: form.masterNode,
                disinheritDefault: form.disinheritDefault,
                disinherited: form.disinherited,
                disinherit: form.disinherit,
                inheritable: form.inheritable,
                fileName: form.fileName,
                description: form.description,
                folderId: form.folderId,
                modified: form.modified,
                publisher: form.publisher,
                pdate: form.pdate,
                timeManagement: form.timeManagement,
                planned: form.planned,
                queued: form.queued,
                languages: form.languages,
                online: form.online,
                folder: form.folder,
                master: form.master,
                locked: form.locked,
                lockedSince: form.lockedSince,
                lockedBy: form.lockedBy,
                version: form.version,
                successPageId: form.successPageId,
                successNodeId: form.successNodeId,
                data: { elements: [] },
            };
            if (form.data) {
                mappedForm.data.email = form.data.email;
                mappedForm.data.successurl = form.data.successurl;  /* old property that must not be removed or altered */
                mappedForm.data.successurl_i18n = form.data.successurl_i18n;
                mappedForm.data.mailsubject_i18n = form.data.mailsubject_i18n;
                mappedForm.data.mailsource_pageid = form.data.mailsource_pageid;
                mappedForm.data.mailsource_nodeid = form.data.mailsource_nodeid;
                mappedForm.data.mailtemp_i18n = form.data.mailtemp_i18n;
                mappedForm.data.type = form.data.type;
                mappedForm.data.templateContext = form.data.templateContext;
            }
            if (form.data && form.data.elements) {
                mappedForm.data.elements = form.data.elements.map(this.mapFormElementBOToFormElement);
            }
            return mappedForm;
        } else {
            return undefined;
        }
    };

    private mapFormElementBOToFormElement = (formElement: CmsFormElementBO): CmsFormElement => {
        const mappedFormElement: CmsFormElement = {
            globalId: formElement.globalId,
            name: formElement.name,
            type: formElement.type,
            active: formElement.active,
            elements: [],
        };
        if (formElement && formElement.elements) {
            mappedFormElement.elements = formElement.elements.map(this.mapFormElementBOToFormElement);
        }
        const valuesI18n = !formElement.properties ? {} : formElement.properties.reduce((values, property) => {
            if (property.type === CmsFormElementPropertyType.UNSUPPORTED) {
                if (typeof property.value_i18n === 'object') {
                    values[`${property.name}_i18n`] = property.value_i18n;
                } else {
                    values[`${property.name}`] = property.value;
                }
            } else if (property.type === CmsFormElementPropertyType.SELECTABLE_OPTIONS) {
                values[`${property.name}`] = property.value;
            } else if (property.type === CmsFormElementPropertyType.REPOSITORY_BROWSER) {
                values[`${property.name}_pageid`] = property.value;
                values[`${property.name}_nodeid`] = property.nodeId;
            } else {
                values[`${property.name}_i18n`] = property.value_i18n;
            }
            return values;
        }, {});
        return Object.assign(mappedFormElement, valuesI18n);
    };

    mapFormToFormBO = (
        form: Form<Raw | Normalized>,
        formEditorConfiguration: FormEditorConfiguration,
        ignoreMissingElementTypes: boolean = true,
        ignoreMissingElementPropertyTypes: boolean = true,
    ): FormBO<Raw | Normalized> => {
        form = cloneDeep(form);
        if (form && formEditorConfiguration) {
            const mappedForm: FormBO<Raw | Normalized> = {
                id: form.id,
                globalId: form.globalId,
                name: form.name,
                masterNodeId: form.masterNodeId,
                creator: form.creator,
                cdate: form.cdate,
                editor: form.editor,
                edate: form.edate,
                type: form.type,
                deleted: form.deleted,
                masterDeleted: form.masterDeleted,
                folderDeleted: form.folderDeleted,
                usage: form.usage,
                excluded: form.excluded,
                inherited: form.inherited,
                inheritedFrom: form.inheritedFrom,
                inheritedFromId: form.inheritedFromId,
                masterNode: form.masterNode,
                disinheritDefault: form.disinheritDefault,
                disinherited: form.disinherited,
                disinherit: form.disinherit,
                inheritable: form.inheritable,
                fileName: form.fileName,
                description: form.description,
                folderId: form.folderId,
                modified: form.modified,
                publisher: form.publisher,
                pdate: form.pdate,
                timeManagement: form.timeManagement,
                planned: form.planned,
                queued: form.queued,
                languages: form.languages,
                online: form.online,
                folder: form.folder,
                master: form.master,
                locked: form.locked,
                lockedSince: form.lockedSince,
                lockedBy: form.lockedBy,
                successPageId: form.successPageId,
                successNodeId: form.successNodeId,
                version: form.version,
                data: {
                    type: CmsFormType.GENERIC,
                    templateContext: '',
                    elements: [],
                },
            };
            if (form.data) {
                mappedForm.data.email = form.data.email;
                mappedForm.data.successurl = form.data.successurl; /* old property that must not be removed or altered */
                mappedForm.data.successurl_i18n = form.data.successurl_i18n;
                mappedForm.data.mailsubject_i18n = form.data.mailsubject_i18n;
                mappedForm.data.mailsource_pageid = form.data.mailsource_pageid;
                mappedForm.data.mailsource_nodeid = form.data.mailsource_nodeid;
                mappedForm.data.mailtemp_i18n = form.data.mailtemp_i18n;
                mappedForm.data.type = !!form.data.type ? form.data.type : CmsFormType.GENERIC;
                mappedForm.data.templateContext = !!form.data.templateContext ? form.data.templateContext : '';
            }
            if (form.data && form.data.elements) {
                mappedForm.data.elements = form.data.elements.map(element => this.mapFormElementToFormElementBO(
                    element,
                    formEditorConfiguration,
                    ignoreMissingElementTypes,
                    ignoreMissingElementPropertyTypes,
                ));
            }
            return mappedForm;
        } else {
            return undefined;
        }
    };

    private mapFormElementToFormElementBO = (
        formElement: CmsFormElement,
        formEditorConfiguration: FormEditorConfiguration,
        ignoreMissingElementTypes: boolean = true,
        ignoreMissingElementPropertyTypes: boolean = true,
    ): CmsFormElementBO => {
        let correspondingFormElementConfiguration: FormElementConfiguration;
        for (const elementConfiguration of formEditorConfiguration.elements) {
            if (elementConfiguration.type === formElement.type) {
                correspondingFormElementConfiguration = elementConfiguration;
                break;
            }
        }
        if (!correspondingFormElementConfiguration) {
            if (!ignoreMissingElementTypes) {
                throw new Error(`Form editor configuration does not contain element type ${formElement.type}.`);
            } else {
                correspondingFormElementConfiguration = {
                    type: UNKNOWN_ELEMENT_TYPE,
                    type_label_i18n_ui: {
                        en: `Unknown Type (${formElement.type})`,
                        de: `Unbekannter Typ (${formElement.type})`,
                    },
                    description_i18n_ui: {
                        en: `An element of unknown type "${formElement.type}". This element type is not present in the form editor configuration.`,
                        de: `Ein Element des unbekannten Typs "${formElement.type}". Dieser Elementtyp ist nicht in der Form-Editor-Konfiguration vorhanden.`,
                    },
                    is_container: false,
                    properties: [],
                }
            }
        }
        const mappedFormElement: CmsFormElementBO = {
            globalId: formElement.globalId,
            name: formElement.name,
            type: formElement.type,
            active: formElement.active,
            type_label_i18n_ui: correspondingFormElementConfiguration.type_label_i18n_ui,
            description_i18n_ui: correspondingFormElementConfiguration.description_i18n_ui,
            label_property_ui: correspondingFormElementConfiguration.label_property_ui,
            isContainer: correspondingFormElementConfiguration.is_container,
            cannotContain: correspondingFormElementConfiguration.cannot_contain,
            elements: [],
        };
        if (formElement && formElement.elements) {
            mappedFormElement.elements = formElement.elements.map(element => this.mapFormElementToFormElementBO(
                element,
                formEditorConfiguration,
                ignoreMissingElementTypes,
                ignoreMissingElementPropertyTypes,
            ));
        }
        // remove known element fields, such that only possible property names remain
        const propertyValueNames: string[] = Object.keys(formElement).filter(propertyValue => ![
            'globalId',
            'name',
            'type',
            'active',
            'elements',
        ].includes(propertyValue));

        mappedFormElement.properties = [];

        for (const elementPropertyConfiguration of correspondingFormElementConfiguration.properties) {
            let indexOfCorrespondingPropertyValueName = -1;
            if (elementPropertyConfiguration.type === FormElementPropertyTypeConfiguration.SELECTABLE_OPTIONS) {
                indexOfCorrespondingPropertyValueName = propertyValueNames.indexOf(elementPropertyConfiguration.name);
            } else if (elementPropertyConfiguration.type === FormElementPropertyTypeConfiguration.REPOSITORY_BROWSER) {
                indexOfCorrespondingPropertyValueName = propertyValueNames.indexOf(`${elementPropertyConfiguration.name}_pageid`);
                // Remove hidden nodeid property from list
                propertyValueNames.splice(propertyValueNames.indexOf(`${elementPropertyConfiguration.name}_nodeid`), 1);
            } else {
                indexOfCorrespondingPropertyValueName = propertyValueNames.indexOf(`${elementPropertyConfiguration.name}_i18n`);
            }
            let propertyValueName: string;
            if (indexOfCorrespondingPropertyValueName >= 0) {
                propertyValueName = propertyValueNames.splice(indexOfCorrespondingPropertyValueName, 1)[0];
            }

            switch (elementPropertyConfiguration.type) {
                case FormElementPropertyTypeConfiguration.SELECT:
                    mappedFormElement.properties.push({
                        name: elementPropertyConfiguration.name,
                        type: CmsFormElementPropertyType[elementPropertyConfiguration.type],
                        description_i18n_ui: elementPropertyConfiguration.description_i18n_ui,
                        label_i18n_ui: elementPropertyConfiguration.label_i18n_ui,
                        required: elementPropertyConfiguration.required,
                        value_i18n: formElement[propertyValueName],
                        multiple: elementPropertyConfiguration.multiple ?? false,
                        options: elementPropertyConfiguration.options.map((optionConfiguration: FormElementPropertyOptionConfiguration) => {
                            return { key: optionConfiguration.key, value_i18n_ui: optionConfiguration.value_i18n_ui };
                        }),
                    });
                    break;
                case FormElementPropertyTypeConfiguration.SELECTABLE_OPTIONS:
                    mappedFormElement.properties.push({
                        name: elementPropertyConfiguration.name,
                        type: CmsFormElementPropertyType[elementPropertyConfiguration.type],
                        description_i18n_ui: elementPropertyConfiguration.description_i18n_ui,
                        label_i18n_ui: elementPropertyConfiguration.label_i18n_ui,
                        key_label_i18n_ui: elementPropertyConfiguration.key_label_i18n_ui,
                        value_label_i18n_ui: elementPropertyConfiguration.value_label_i18n_ui,
                        required: elementPropertyConfiguration.required,
                        value: formElement[propertyValueName],
                    });
                    break;
                case FormElementPropertyTypeConfiguration.REPOSITORY_BROWSER:
                    mappedFormElement.properties.push({
                        name: elementPropertyConfiguration.name,
                        type: CmsFormElementPropertyType[elementPropertyConfiguration.type],
                        description_i18n_ui: elementPropertyConfiguration.description_i18n_ui,
                        label_i18n_ui: elementPropertyConfiguration.label_i18n_ui,
                        required: elementPropertyConfiguration.required,
                        value: formElement[propertyValueName],
                        nodeId: formElement[elementPropertyConfiguration.name + '_nodeid'],
                        options: elementPropertyConfiguration.options,
                    });
                    break;
                case FormElementPropertyTypeConfiguration.BOOLEAN:
                case FormElementPropertyTypeConfiguration.NUMBER:
                case FormElementPropertyTypeConfiguration.STRING:
                default:
                    mappedFormElement.properties.push({
                        name: elementPropertyConfiguration.name,
                        type: CmsFormElementPropertyType[elementPropertyConfiguration.type],
                        description_i18n_ui: elementPropertyConfiguration.description_i18n_ui,
                        label_i18n_ui: elementPropertyConfiguration.label_i18n_ui,
                        required: elementPropertyConfiguration.required,
                        validation: elementPropertyConfiguration.validator,
                        value_i18n: formElement[propertyValueName],
                    });
                    break;
            }
        }

        const remainingPropertyValueNames = propertyValueNames;
        if (remainingPropertyValueNames.length > 0 && !ignoreMissingElementPropertyTypes) {
            throw new Error(`Form editor configuration does not contain element property ${remainingPropertyValueNames[0]}.`);
        }
        mappedFormElement.properties.push(...remainingPropertyValueNames.map((propertyValueName: string): CmsFormElementPropertyUnsupported => {
            const mappedPropertyValueName: string = propertyValueName.endsWith('_i18n') ? propertyValueName.slice(0, -5) : propertyValueName;
            const formElementProperty: CmsFormElementPropertyUnsupported =  {
                name: mappedPropertyValueName,
                type: CmsFormElementPropertyType.UNSUPPORTED,
                description_i18n_ui: {
                    en: `A property of unknown name "${mappedPropertyValueName}". This property is not present in the form editor configuration.`,
                    de: `Eine unbekannte Eigenschaft namens "${mappedPropertyValueName}". Diese Eigenschaft ist nicht in der Form-Editor-Konfiguration vorhanden.`,
                },
                label_i18n_ui: {
                    en: `Unknown Property (${mappedPropertyValueName})`,
                    de: `Unbekannte Eigenschaft (${mappedPropertyValueName})`,
                },
                required: false,
            };
            if (propertyValueName.endsWith('_i18n')) {
                formElementProperty.value_i18n = formElement[propertyValueName];
            } else {
                formElementProperty.value = formElement[propertyValueName];
            }
            return formElementProperty;
        }));
        return mappedFormElement;
    };

}
