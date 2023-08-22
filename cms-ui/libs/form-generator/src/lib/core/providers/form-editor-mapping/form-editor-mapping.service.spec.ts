/* eslint-disable no-useless-escape */
/* eslint-disable id-blacklist */
/* eslint-disable @typescript-eslint/naming-convention */
import { TestBed } from '@angular/core/testing';
import {
    CmsFormElement,
    CmsFormElementBO,
    CmsFormElementPropertyDefault,
    CmsFormElementPropertySelectableOptions,
    CmsFormElementPropertyType,
    CmsFormElementPropertyUnsupported,
    CmsFormType,
    Form,
    FormBO,
} from '@gentics/cms-models';
import { cloneDeep } from 'lodash-es';
import {
    FormEditorConfiguration,
    FormElementPropertyTypeConfiguration,
    FormElementPropertyValidatorConfiguration,
} from '../../../common/models/form-editor-configuration';
import { FormEditorMappingService } from './form-editor-mapping.service';

describe('FormEditorMappingService', () => {

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                FormEditorMappingService,
            ],
        });

    });

    it('should be created', () => {
        const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
        expect(service).toBeTruthy();
    });

    describe('mapFormBOToForm', () => {

        const types: (CmsFormType)[] = [
            CmsFormType.GENERIC,
            CmsFormType.POLL,
        ];

        for (const type of types) {
            it(`transfers basic form data for type ${type}`, () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const BASIC_FORM_BO_WITH_TYPE_OVERRIDDEN: FormBO = cloneDeep(BASIC_FORM_BO);
                const BASIC_FORM_WITH_TYPE_OVERRIDDEN: Form = cloneDeep(BASIC_FORM);
                BASIC_FORM_BO_WITH_TYPE_OVERRIDDEN.data.type = type;
                BASIC_FORM_WITH_TYPE_OVERRIDDEN.data.type = type;
                const mappedForm = service.mapFormBOToForm(BASIC_FORM_BO_WITH_TYPE_OVERRIDDEN);
                expect(mappedForm).toEqual(BASIC_FORM_WITH_TYPE_OVERRIDDEN);
            });
        }

        describe('maps elements correctly', () => {
            it('maps form with element of known type that has no properties and has no encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_2_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_2));
                expect(mappedForm).toEqual(form);
            });

            it('maps form with element of known type that has properties and has no encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_1_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_1));
                expect(mappedForm).toEqual(form);
            });

            it('maps form with element of known type that has no properties and has encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_3_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_3));
                expect(mappedForm).toEqual(form);
            });

            it('maps form with element of known type that has properties and has encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_1_BO));
                formBO.data.elements[0].elements.push(cloneDeep(ELEMENT_3_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_1));
                form.data.elements[0].elements.push(cloneDeep(ELEMENT_3));
                expect(mappedForm).toEqual(form);
            });

            it('maps form with element of unknown type that has no properties and has no encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_2_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_2));
                expect(mappedForm).toEqual(form);
            });

            it('maps form with element of unknown type that has properties and has no encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_1_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_1));
                expect(mappedForm).toEqual(form);
            });

            it('maps form with element of unknown type that has no properties and has encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_2_BO));
                formBO.data.elements[0].elements.push(cloneDeep(ELEMENT_3_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_2));
                form.data.elements[0].elements.push(cloneDeep(ELEMENT_3));
                expect(mappedForm).toEqual(form);
            });

            it('maps form with element of unknown type that has properties and has encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_1_BO));
                formBO.data.elements[0].elements.push(cloneDeep(ELEMENT_3_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_1));
                form.data.elements[0].elements.push(cloneDeep(ELEMENT_3));
                expect(mappedForm).toEqual(form);
            });
        });

        describe('maps properties correctly', () => {
            it('maps element where properties previously matching the configuration', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_1_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_1));
                expect(mappedForm).toEqual(form);
            });

            it('maps element where one property was missing', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_1_WITH_MISSING_PROPERTY_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_1_WITH_MISSING_PROPERTY_MAPPED));
                expect(mappedForm).toEqual(form);
            });

            it('maps element that contained one additional property', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_1_WITH_ADDITIONAL_PROPERTY_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_1_WITH_ADDITIONAL_PROPERTY_MAPPED));
                expect(mappedForm).toEqual(form);
            });

            it('maps element with known property that has a top level i18n value', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_FOR_PROPERTY_TESTS_BO));
                formBO.data.elements[0].properties.push(cloneDeep(PROPERTY_WITH_TOP_LEVEL_I18N_VALUE_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_FOR_PROPERTY_TESTS));
                form.data.elements[0]['name2_i18n'] = PROPERTY_WITH_TOP_LEVEL_I18N_VALUE_BO.value_i18n;
                expect(mappedForm).toEqual(form);
            });

            it('maps element with known property that has a top level non-i18n value', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_FOR_PROPERTY_TESTS_BO));
                formBO.data.elements[0].properties.push(cloneDeep(PROPERTY_WITH_TOP_LEVEL_NON_I18N_VALUE_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_FOR_PROPERTY_TESTS));
                form.data.elements[0]['name5'] = PROPERTY_WITH_TOP_LEVEL_NON_I18N_VALUE_BO.value;
                expect(mappedForm).toEqual(form);
            });

            it('maps element with unknown property that has a top level i18n value', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_FOR_UNKNOWN_PROPERTY_TESTS_BO));
                formBO.data.elements[0].properties.push(cloneDeep(UNKNOWN_PROPERTY_WITH_TOP_LEVEL_I18N_VALUE_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_FOR_UNKNOWN_PROPERTY_TESTS));
                form.data.elements[0]['name2_i18n'] = UNKNOWN_PROPERTY_WITH_TOP_LEVEL_I18N_VALUE_BO.value_i18n;
                expect(mappedForm).toEqual(form);
            });

            it('maps element with unknown property that has a top level non-i18n value', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_FOR_UNKNOWN_PROPERTY_TESTS_BO));
                formBO.data.elements[0].properties.push(cloneDeep(UNKNOWN_PROPERTY_WITH_TOP_LEVEL_NON_I18N_VALUE_BO));
                const mappedForm = service.mapFormBOToForm(formBO);

                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_FOR_UNKNOWN_PROPERTY_TESTS));
                form.data.elements[0]['name5'] = UNKNOWN_PROPERTY_WITH_TOP_LEVEL_NON_I18N_VALUE_BO.value;
                expect(mappedForm).toEqual(form);
            });

        });

    });

    describe('mapFormToFormBO', () => {

        const types: (CmsFormType | undefined)[] = [
            undefined,
            CmsFormType.GENERIC,
            CmsFormType.POLL,
        ];

        const templateContexts: (string | undefined)[] = [
            undefined,
            '',
        ];

        for (const type of types) {
            for (const templateContext of templateContexts) {
                it(`transfers basic form data for ${type === undefined ? 'no specified type' : `type ${type}`}
                 and ${templateContext === undefined ? 'no specified template context' : `template context ${type}`}`, () => {
                    const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                    const BASIC_FORM_WITH_TYPE_OVERRIDDEN: Form = cloneDeep(BASIC_FORM);
                    const BASIC_FORM_BO_WITH_TYPE_OVERRIDDEN: FormBO = cloneDeep(BASIC_FORM_BO);
                    if (type !== undefined) {
                        BASIC_FORM_WITH_TYPE_OVERRIDDEN.data.type = type;
                        BASIC_FORM_BO_WITH_TYPE_OVERRIDDEN.data.type = type;
                    } else {
                        // maps to default value in case no type is set
                        delete BASIC_FORM_WITH_TYPE_OVERRIDDEN.data.type;
                        BASIC_FORM_BO_WITH_TYPE_OVERRIDDEN.data.type = CmsFormType.GENERIC;
                    }
                    if (templateContext !== undefined) {
                        BASIC_FORM_WITH_TYPE_OVERRIDDEN.data.templateContext = templateContext;
                        BASIC_FORM_BO_WITH_TYPE_OVERRIDDEN.data.templateContext = templateContext;
                    } else {
                        // maps to default value in case no type is set
                        delete BASIC_FORM_WITH_TYPE_OVERRIDDEN.data.templateContext;
                        BASIC_FORM_BO_WITH_TYPE_OVERRIDDEN.data.templateContext = '';
                    }
                    const mappedForm = service.mapFormToFormBO(BASIC_FORM_WITH_TYPE_OVERRIDDEN, CONFIGURATION);
                    expect(mappedForm).toEqual(BASIC_FORM_BO_WITH_TYPE_OVERRIDDEN);
                });
            }
        }

        describe('maps elements correctly', () => {
            it('maps form with element of known type that has no properties and has no encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_2));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_2_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps form with element of known type that has properties and has no encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_1));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_1_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps form with element of known type that has no properties and has encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_3));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_3_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps form with element of known type that has properties and has encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_1));
                form.data.elements[0].elements.push(cloneDeep(ELEMENT_3));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_1_BO));
                formBO.data.elements[0].elements.push(cloneDeep(ELEMENT_3_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps form with element of unknown type that has no properties and has no encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_2));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_2_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps form with element of unknown type that has properties and has no encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_1));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_1_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps form with element of unknown type that has no properties and has encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_2));
                form.data.elements[0].elements.push(cloneDeep(ELEMENT_3));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_2_BO));
                formBO.data.elements[0].elements.push(cloneDeep(ELEMENT_3_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps form with element of unknown type that has properties and has encapsuled elements', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_1));
                form.data.elements[0].elements.push(cloneDeep(ELEMENT_3));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_UNKNOWN_TYPE_1_BO));
                formBO.data.elements[0].elements.push(cloneDeep(ELEMENT_3_BO));
                expect(mappedForm).toEqual(formBO);
            });
        });

        describe('maps properties correctly', () => {
            it('maps element with properties matching the configuration', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_1));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_1_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps element with one property missing', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_1_WITH_MISSING_PROPERTY));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_1_WITH_MISSING_PROPERTY_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps element with one additional property', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_1_WITH_ADDITIONAL_PROPERTY));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_1_WITH_ADDITIONAL_PROPERTY_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps element with properties out of order', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_1_WITH_PROPERTIES_OUT_OF_ORDER));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_1_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps element with one missing property, one additional property and the matching properties out of order', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_1_WITH_MISSING_PROPERTY_AND_ADDITIONAL_PROPERTY_AND_PROPERTIES_OUT_OF_ORDER));
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_1_WITH_MISSING_PROPERTY_AND_ADDITIONAL_PROPERTY_AND_PROPERTIES_OUT_OF_ORDER_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps element with known property that has a top level i18n value', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_FOR_PROPERTY_TESTS));
                form.data.elements[0]['name2_i18n'] = PROPERTY_WITH_TOP_LEVEL_I18N_VALUE_BO.value_i18n;
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_FOR_PROPERTY_TESTS_BO));
                formBO.data.elements[0].properties.push(cloneDeep(PROPERTY_WITH_TOP_LEVEL_I18N_VALUE_BO));
                const unusedNonI18nProperty = cloneDeep(PROPERTY_WITH_TOP_LEVEL_NON_I18N_VALUE_BO);
                unusedNonI18nProperty.value = undefined;
                formBO.data.elements[0].properties.push(unusedNonI18nProperty);
                expect(mappedForm).toEqual(formBO);
            });

            it('maps element with known property that has a top level non-i18n value', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_FOR_PROPERTY_TESTS));
                form.data.elements[0]['name5'] = PROPERTY_WITH_TOP_LEVEL_NON_I18N_VALUE_BO.value;
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_FOR_PROPERTY_TESTS_BO));
                const unusedI18nProperty = cloneDeep(PROPERTY_WITH_TOP_LEVEL_I18N_VALUE_BO);
                unusedI18nProperty.value_i18n = undefined;
                formBO.data.elements[0].properties.push(unusedI18nProperty);
                formBO.data.elements[0].properties.push(cloneDeep(PROPERTY_WITH_TOP_LEVEL_NON_I18N_VALUE_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps element with unknown property that has a top level i18n value', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_FOR_UNKNOWN_PROPERTY_TESTS));
                form.data.elements[0]['name2_i18n'] = UNKNOWN_PROPERTY_WITH_TOP_LEVEL_I18N_VALUE_BO.value_i18n;
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_FOR_UNKNOWN_PROPERTY_TESTS_BO));
                formBO.data.elements[0].properties.push(cloneDeep(UNKNOWN_PROPERTY_WITH_TOP_LEVEL_I18N_VALUE_BO));
                expect(mappedForm).toEqual(formBO);
            });

            it('maps element with unknown property that has a top level non-i18n value', () => {
                const service: FormEditorMappingService = TestBed.inject(FormEditorMappingService);
                const form: Form = cloneDeep(BASIC_FORM);
                form.data.elements.push(cloneDeep(ELEMENT_FOR_UNKNOWN_PROPERTY_TESTS));
                form.data.elements[0]['name5'] = UNKNOWN_PROPERTY_WITH_TOP_LEVEL_NON_I18N_VALUE_BO.value;
                const mappedForm = service.mapFormToFormBO(form, CONFIGURATION);

                const formBO: FormBO = cloneDeep(BASIC_FORM_BO);
                formBO.data.elements.push(cloneDeep(ELEMENT_FOR_UNKNOWN_PROPERTY_TESTS_BO));
                formBO.data.elements[0].properties.push(cloneDeep(UNKNOWN_PROPERTY_WITH_TOP_LEVEL_NON_I18N_VALUE_BO));
                expect(mappedForm).toEqual(formBO);
            });
        });
    });

    // for better readability of the file, larger constants are placed at the bottom against convention

    const CONFIGURATION: FormEditorConfiguration = {
        form_properties: {},
        elements: [{
            type: 'element1',
            type_label_i18n_ui: {
                de: 'Element Eins',
            },
            description_i18n_ui: {
                de: 'Erstes Element',
            },
            label_property_ui: 'name1',
            is_container: true,
            cannot_contain: ['element2'],
            properties: [{
                name: 'name1',
                type: FormElementPropertyTypeConfiguration['BOOLEAN'],
                label_i18n_ui: {
                    en: 'Boolean',
                },
                description_i18n_ui: {
                    en: 'Boolean Value',
                },
                required: true,
                default_value_i18n: {
                    de: true,
                    en: false,
                },
            }, {
                name: 'name2',
                type: FormElementPropertyTypeConfiguration['NUMBER'],
                label_i18n_ui: {
                    en: 'Number',
                },
                validator: FormElementPropertyValidatorConfiguration['NUMBER'],
                required: false,
                default_value_i18n: {
                    de: 23,
                    en: 0,
                    it: -1,
                },
            }, {
                name: 'name3',
                type: FormElementPropertyTypeConfiguration['STRING'],
                label_i18n_ui: {
                    en: 'String',
                },
                validator: FormElementPropertyValidatorConfiguration['DATE'],
            }, {
                name: 'name4',
                type: FormElementPropertyTypeConfiguration['SELECT'],
                label_i18n_ui: {
                    en: 'Select',
                },
                options: [{
                    key: 'key',
                    value_i18n_ui: {
                        de: 'Wert 1',
                    },
                }],
                default_value_i18n: {
                    de: 'key',
                },
            }, {
                name: 'name5',
                type: FormElementPropertyTypeConfiguration['SELECTABLE_OPTIONS'],
                key_label_i18n_ui: undefined,
                value_label_i18n_ui: undefined,
                label_i18n_ui: {
                    en: 'Selectable Options',
                },
                default_value: [{
                    key: 'key',
                    value_i18n: {
                        de: 'Deutsch',
                    },
                }],
            }],
        }, {
            type: 'element2',
            type_label_i18n_ui: {
                de: 'Element Zwei',
            },
            description_i18n_ui: {
                de: 'Zweites Element',
            },
            is_container: false,
            properties: [],
        }, {
            type: 'element3',
            type_label_i18n_ui: {
                de: 'Element Drei',
            },
            description_i18n_ui: {
                de: 'Drittes Element',
            },
            is_container: false,
            properties: [],
        }, {
            type: 'elementPropertyTests',
            type_label_i18n_ui: {
                de: 'Element Eigenschaften',
            },
            description_i18n_ui: {
                de: 'Eigenschaften Element',
            },
            is_container: false,
            properties: [{
                name: 'name2',
                type: FormElementPropertyTypeConfiguration['NUMBER'],
                label_i18n_ui: {
                    en: 'Number',
                },
                validator: FormElementPropertyValidatorConfiguration['NUMBER'],
                required: false,
            }, {
                name: 'name5',
                type: FormElementPropertyTypeConfiguration['SELECTABLE_OPTIONS'],
                key_label_i18n_ui: undefined,
                value_label_i18n_ui: undefined,
                label_i18n_ui: {
                    en: 'Selectable Options',
                },
            }],
        }, {
            type: 'elementUnknownPropertyTests',
            type_label_i18n_ui: {
                de: 'Element Eigenschaften',
            },
            description_i18n_ui: {
                de: 'Eigenschaften Element',
            },
            is_container: false,
            properties: [],
        }],
    };

    const BASIC_FORM: Form = {
        id: 1230,
        globalId: 'globalId',
        name: 'Form',
        masterNodeId: 1231,
        creator: 1,
        cdate: 1232,
        editor: -1,
        edate: 1233,
        type: 'form',
        deleted: {
            at: 0,
            by: undefined,
        },
        masterDeleted: {
            at: 0,
            by: undefined,
        },
        folderDeleted: {
            at: 0,
            by: undefined,
        },
        usage: undefined,
        excluded: true,
        inherited: true,
        inheritedFrom: 'inheritedFrom',
        inheritedFromId: 1229,
        masterNode: 'masterNode',
        disinheritDefault: true,
        disinherited: true,
        fileName: 'fileName',
        disinherit: [1],
        inheritable: [2],
        description: 'description',
        folderId: 1,
        modified: false,
        publisher: 2,
        pdate: 1234,
        timeManagement: {
            at: 1235,
            offlineAt: 1236,
        },
        planned: true,
        queued: true,
        languages: ['de', 'en'],
        online: true,
        folder: undefined,
        master: true,
        locked: true,
        lockedSince: 1237,
        lockedBy: 3,
        version: {
            editor: {
                email: 'nowhere@gentics.com',
                firstName: 'Node',
                id: 1238,
                lastName: 'Admin',
            },
            number: '0.5',
            timestamp: 1239,
        },
        successPageId: 23,
        successNodeId: 24,
        data: {
            email: 'email',
            successurl_i18n: {
                en: 'successurl_en',
                de: 'successurl_de',
            },
            successurl: 'successurl',
            mailsubject_i18n: {
                de: 'Betreff',
                en: 'Subject',
            },
            mailtemp_i18n: {
                de: 'Vorlage',
                en: 'Template',
            },
            mailsource_nodeid: undefined,
            mailsource_pageid: undefined,
            elements: [],
            type: CmsFormType.GENERIC,
            templateContext: '',
        },
    }

    const ELEMENT_1: CmsFormElement = {
        globalId: 'globalId1',
        name: 'Element of type element1',
        type: 'element1',
        active: true,
        elements: [],
        name1_i18n: {
            de: false,
            en: true,
        },
        name2_i18n: {
            de: 50,
            en: -3,
        },
        name3_i18n: undefined,
        name4_i18n: {
            de: 'key',
            en: 'key',
        },
        name5: [{
            key: 'some_key',
            value_i18n: {
                de: 'Ein Schlüssel',
                en: 'Some Key',
            },
        }],
    }


    const ELEMENT_1_WITH_MISSING_PROPERTY: CmsFormElement = {
        globalId: 'globalId1',
        name: 'Element of type element1',
        type: 'element1',
        active: true,
        elements: [],
        name1_i18n: {
            de: false,
            en: true,
        },
        name2_i18n: {
            de: 50,
            en: -3,
        },
        name3_i18n: undefined,
        name5: [{
            key: 'some_key',
            value_i18n: {
                de: 'Ein Schlüssel',
                en: 'Some Key',
            },
        }],
    }

    const ELEMENT_1_WITH_MISSING_PROPERTY_MAPPED: CmsFormElement = {
        globalId: 'globalId1',
        name: 'Element of type element1',
        type: 'element1',
        active: true,
        elements: [],
        name1_i18n: {
            de: false,
            en: true,
        },
        name2_i18n: {
            de: 50,
            en: -3,
        },
        name3_i18n: undefined,
        name4_i18n: undefined,
        name5: [{
            key: 'some_key',
            value_i18n: {
                de: 'Ein Schlüssel',
                en: 'Some Key',
            },
        }],
    }

    const ELEMENT_1_WITH_ADDITIONAL_PROPERTY: CmsFormElement = {
        globalId: 'globalId1',
        name: 'Element of type element1',
        type: 'element1',
        active: true,
        elements: [],
        name1_i18n: {
            de: false,
            en: true,
        },
        name2_i18n: {
            de: 50,
            en: -3,
        },
        name3_i18n: undefined,
        name4_i18n: {
            de: 'key',
            en: 'key',
        },
        name4_2_i18n: {
            de: 'bla',
            en: 'bla',
        },
        name5: [{
            key: 'some_key',
            value_i18n: {
                de: 'Ein Schlüssel',
                en: 'Some Key',
            },
        }],
    }

    const ELEMENT_1_WITH_ADDITIONAL_PROPERTY_MAPPED: CmsFormElement = {
        globalId: 'globalId1',
        name: 'Element of type element1',
        type: 'element1',
        active: true,
        elements: [],
        name1_i18n: {
            de: false,
            en: true,
        },
        name2_i18n: {
            de: 50,
            en: -3,
        },
        name3_i18n: undefined,
        name4_i18n: {
            de: 'key',
            en: 'key',
        },
        name5: [{
            key: 'some_key',
            value_i18n: {
                de: 'Ein Schlüssel',
                en: 'Some Key',
            },
        }],
        name4_2_i18n: {
            de: 'bla',
            en: 'bla',
        },
    }

    const ELEMENT_1_WITH_PROPERTIES_OUT_OF_ORDER: CmsFormElement = {
        globalId: 'globalId1',
        name: 'Element of type element1',
        type: 'element1',
        active: true,
        elements: [],
        name5: [{
            key: 'some_key',
            value_i18n: {
                de: 'Ein Schlüssel',
                en: 'Some Key',
            },
        }],
        name4_i18n: {
            de: 'key',
            en: 'key',
        },
        name3_i18n: undefined,
        name2_i18n: {
            de: 50,
            en: -3,
        },
        name1_i18n: {
            de: false,
            en: true,
        },
    }

    const ELEMENT_1_WITH_MISSING_PROPERTY_AND_ADDITIONAL_PROPERTY_AND_PROPERTIES_OUT_OF_ORDER: CmsFormElement = {
        globalId: 'globalId1',
        name: 'Element of type element1',
        type: 'element1',
        active: true,
        elements: [],
        name5: [{
            key: 'some_key',
            value_i18n: {
                de: 'Ein Schlüssel',
                en: 'Some Key',
            },
        }],
        name4_2_i18n: {
            de: 'bla',
            en: 'bla',
        },
        name3_i18n: undefined,
        name2_i18n: {
            de: 50,
            en: -3,
        },
        name1_i18n: {
            de: false,
            en: true,
        },
    }

    const ELEMENT_2: CmsFormElement = {
        globalId: 'globalId2',
        name: 'Element of type element2',
        type: 'element2',
        active: false,
        elements: [],
    }

    const ELEMENT_3: CmsFormElement = {
        globalId: 'globalId3',
        name: 'Element of type element3',
        type: 'element3',
        active: false,
        elements: [ELEMENT_2],
    }

    const ELEMENT_UNKNOWN_TYPE_1: CmsFormElement = {
        globalId: 'globalIdUnknown',
        name: 'Element of unknown type',
        type: 'elementUnknown',
        active: false,
        elements: [],
        name2_i18n: {
            de: 50,
            en: -3,
        },
    }

    const ELEMENT_UNKNOWN_TYPE_2: CmsFormElement = {
        globalId: 'globalIdUnknown',
        name: 'Element of unknown type',
        type: 'elementUnknown',
        active: false,
        elements: [],
    }

    const ELEMENT_FOR_PROPERTY_TESTS: CmsFormElement = {
        globalId: 'globalIdPropertyTests',
        name: 'Element of some type',
        type: 'elementPropertyTests',
        active: false,
        elements: [],
    }

    const ELEMENT_FOR_UNKNOWN_PROPERTY_TESTS: CmsFormElement = {
        globalId: 'globalIdUnknownPropertyTests',
        name: 'Element of some type',
        type: 'elementUnknownPropertyTests',
        active: false,
        elements: [],
    }

    const BASIC_FORM_BO: FormBO = {
        id: 1230,
        globalId: 'globalId',
        name: 'Form',
        masterNodeId: 1231,
        creator: 1,
        cdate: 1232,
        editor: -1,
        edate: 1233,
        type: 'form',
        deleted: {
            at: 0,
            by: undefined,
        },
        masterDeleted: {
            at: 0,
            by: undefined,
        },
        folderDeleted: {
            at: 0,
            by: undefined,
        },
        usage: undefined,
        excluded: true,
        inherited: true,
        inheritedFrom: 'inheritedFrom',
        inheritedFromId: 1229,
        masterNode: 'masterNode',
        disinheritDefault: true,
        disinherited: true,
        fileName: 'fileName',
        disinherit: [1],
        inheritable: [2],
        description: 'description',
        folderId: 1,
        modified: false,
        publisher: 2,
        pdate: 1234,
        timeManagement: {
            at: 1235,
            offlineAt: 1236,
        },
        planned: true,
        queued: true,
        languages: ['de', 'en'],
        online: true,
        folder: undefined,
        master: true,
        locked: true,
        lockedSince: 1237,
        lockedBy: 3,
        version: {
            editor: {
                email: 'nowhere@gentics.com',
                firstName: 'Node',
                id: 1238,
                lastName: 'Admin',
            },
            number: '0.5',
            timestamp: 1239,
        },
        successPageId: 23,
        successNodeId: 24,
        data: {
            email: 'email',
            successurl_i18n: {
                en: 'successurl_en',
                de: 'successurl_de',
            },
            successurl: 'successurl',
            mailsource_nodeid: undefined,
            mailsource_pageid: undefined,
            mailsubject_i18n: {
                de: 'Betreff',
                en: 'Subject',
            },
            mailtemp_i18n: {
                de: 'Vorlage',
                en: 'Template',
            },
            elements: [],
            type: CmsFormType.GENERIC,
            templateContext: '',
        },
    }

    const ELEMENT_1_BO: CmsFormElementBO = {
        globalId: 'globalId1',
        name: 'Element of type element1',
        type: 'element1',
        active: true,
        type_label_i18n_ui: {
            de: 'Element Eins',
        },
        description_i18n_ui: {
            de: 'Erstes Element',
        },
        label_property_ui: 'name1',
        isContainer: true,
        cannotContain: ['element2'],
        elements: [],
        properties: [{
            name: 'name1',
            type: CmsFormElementPropertyType['BOOLEAN'],
            label_i18n_ui: {
                en: 'Boolean',
            },
            description_i18n_ui: {
                en: 'Boolean Value',
            },
            required: true,
            validation: undefined,
            value_i18n: {
                de: false,
                en: true,
            },
        }, {
            name: 'name2',
            type: CmsFormElementPropertyType['NUMBER'],
            label_i18n_ui: {
                en: 'Number',
            },
            description_i18n_ui: undefined,
            validation: 'NUMBER',
            required: false,
            value_i18n: {
                de: 50,
                en: -3,
            },
        }, {
            name: 'name3',
            type: CmsFormElementPropertyType['STRING'],
            label_i18n_ui: {
                en: 'String',
            },
            description_i18n_ui: undefined,
            required: undefined,
            validation: 'DATE',
            value_i18n: undefined,
        }, {
            name: 'name4',
            type: CmsFormElementPropertyType['SELECT'],
            label_i18n_ui: {
                en: 'Select',
            },
            description_i18n_ui: undefined,
            required: undefined,
            multiple: false,
            options: [{
                key: 'key',
                value_i18n_ui: {
                    de: 'Wert 1',
                },
            }],
            value_i18n: {
                de: 'key',
                en: 'key',
            },
        }, {
            name: 'name5',
            type: CmsFormElementPropertyType['SELECTABLE_OPTIONS'],
            key_label_i18n_ui: undefined,
            value_label_i18n_ui: undefined,
            label_i18n_ui: {
                en: 'Selectable Options',
            },
            description_i18n_ui: undefined,
            required: undefined,
            value: [{
                key: 'some_key',
                value_i18n: {
                    de: 'Ein Schlüssel',
                    en: 'Some Key',
                },
            }],
        }],
    }

    const ELEMENT_1_WITH_MISSING_PROPERTY_BO: CmsFormElementBO = {
        globalId: 'globalId1',
        name: 'Element of type element1',
        type: 'element1',
        active: true,
        type_label_i18n_ui: {
            de: 'Element Eins',
        },
        description_i18n_ui: {
            de: 'Erstes Element',
        },
        label_property_ui: 'name1',
        isContainer: true,
        cannotContain: ['element2'],
        elements: [],
        properties: [{
            name: 'name1',
            type: CmsFormElementPropertyType['BOOLEAN'],
            label_i18n_ui: {
                en: 'Boolean',
            },
            description_i18n_ui: {
                en: 'Boolean Value',
            },
            required: true,
            validation: undefined,
            value_i18n: {
                de: false,
                en: true,
            },
        }, {
            name: 'name2',
            type: CmsFormElementPropertyType['NUMBER'],
            label_i18n_ui: {
                en: 'Number',
            },
            description_i18n_ui: undefined,
            validation: 'NUMBER',
            required: false,
            value_i18n: {
                de: 50,
                en: -3,
            },
        }, {
            name: 'name3',
            type: CmsFormElementPropertyType['STRING'],
            label_i18n_ui: {
                en: 'String',
            },
            description_i18n_ui: undefined,
            required: undefined,
            validation: 'DATE',
            value_i18n: undefined,
        }, {
            name: 'name4',
            type: CmsFormElementPropertyType['SELECT'],
            label_i18n_ui: {
                en: 'Select',
            },
            description_i18n_ui: undefined,
            required: undefined,
            multiple: false,
            options: [{
                key: 'key',
                value_i18n_ui: {
                    de: 'Wert 1',
                },
            }],
            value_i18n: undefined,
        }, {
            name: 'name5',
            type: CmsFormElementPropertyType['SELECTABLE_OPTIONS'],
            key_label_i18n_ui: undefined,
            value_label_i18n_ui: undefined,
            label_i18n_ui: {
                en: 'Selectable Options',
            },
            description_i18n_ui: undefined,
            required: undefined,
            value: [{
                key: 'some_key',
                value_i18n: {
                    de: 'Ein Schlüssel',
                    en: 'Some Key',
                },
            }],
        }],
    }

    const ELEMENT_1_WITH_ADDITIONAL_PROPERTY_BO: CmsFormElementBO = {
        globalId: 'globalId1',
        name: 'Element of type element1',
        type: 'element1',
        active: true,
        type_label_i18n_ui: {
            de: 'Element Eins',
        },
        description_i18n_ui: {
            de: 'Erstes Element',
        },
        label_property_ui: 'name1',
        isContainer: true,
        cannotContain: ['element2'],
        elements: [],
        properties: [{
            name: 'name1',
            type: CmsFormElementPropertyType['BOOLEAN'],
            label_i18n_ui: {
                en: 'Boolean',
            },
            description_i18n_ui: {
                en: 'Boolean Value',
            },
            required: true,
            validation: undefined,
            value_i18n: {
                de: false,
                en: true,
            },
        }, {
            name: 'name2',
            type: CmsFormElementPropertyType['NUMBER'],
            label_i18n_ui: {
                en: 'Number',
            },
            description_i18n_ui: undefined,
            validation: 'NUMBER',
            required: false,
            value_i18n: {
                de: 50,
                en: -3,
            },
        }, {
            name: 'name3',
            type: CmsFormElementPropertyType['STRING'],
            label_i18n_ui: {
                en: 'String',
            },
            description_i18n_ui: undefined,
            required: undefined,
            validation: 'DATE',
            value_i18n: undefined,
        }, {
            name: 'name4',
            type: CmsFormElementPropertyType['SELECT'],
            label_i18n_ui: {
                en: 'Select',
            },
            description_i18n_ui: undefined,
            required: undefined,
            multiple: false,
            options: [{
                key: 'key',
                value_i18n_ui: {
                    de: 'Wert 1',
                },
            }],
            value_i18n: {
                de: 'key',
                en: 'key',
            },
        }, {
            name: 'name5',
            type: CmsFormElementPropertyType['SELECTABLE_OPTIONS'],
            key_label_i18n_ui: undefined,
            value_label_i18n_ui: undefined,
            label_i18n_ui: {
                en: 'Selectable Options',
            },
            description_i18n_ui: undefined,
            required: undefined,
            value: [{
                key: 'some_key',
                value_i18n: {
                    de: 'Ein Schlüssel',
                    en: 'Some Key',
                },
            }],
        }, {
            name: 'name4_2',
            type: CmsFormElementPropertyType['UNSUPPORTED'],
            description_i18n_ui: {
                en: 'A property of unknown name \"name4_2\". This property is not present in the form editor configuration.',
                de: 'Eine unbekannte Eigenschaft namens \"name4_2\". Diese Eigenschaft ist nicht in der Form-Editor-Konfiguration vorhanden.',
            },
            label_i18n_ui: {
                en: 'Unknown Property (name4_2)',
                de: 'Unbekannte Eigenschaft (name4_2)',
            },
            required: false,
            value_i18n: {
                de: 'bla',
                en: 'bla',
            },
        }],
    }

    const ELEMENT_1_WITH_MISSING_PROPERTY_AND_ADDITIONAL_PROPERTY_AND_PROPERTIES_OUT_OF_ORDER_BO: CmsFormElementBO = {
        globalId: 'globalId1',
        name: 'Element of type element1',
        type: 'element1',
        active: true,
        type_label_i18n_ui: {
            de: 'Element Eins',
        },
        description_i18n_ui: {
            de: 'Erstes Element',
        },
        label_property_ui: 'name1',
        isContainer: true,
        cannotContain: ['element2'],
        elements: [],
        properties: [{
            name: 'name1',
            type: CmsFormElementPropertyType['BOOLEAN'],
            label_i18n_ui: {
                en: 'Boolean',
            },
            description_i18n_ui: {
                en: 'Boolean Value',
            },
            required: true,
            validation: undefined,
            value_i18n: {
                de: false,
                en: true,
            },
        }, {
            name: 'name2',
            type: CmsFormElementPropertyType['NUMBER'],
            label_i18n_ui: {
                en: 'Number',
            },
            description_i18n_ui: undefined,
            validation: 'NUMBER',
            required: false,
            value_i18n: {
                de: 50,
                en: -3,
            },
        }, {
            name: 'name3',
            type: CmsFormElementPropertyType['STRING'],
            label_i18n_ui: {
                en: 'String',
            },
            description_i18n_ui: undefined,
            required: undefined,
            validation: 'DATE',
            value_i18n: undefined,
        }, {
            name: 'name4',
            type: CmsFormElementPropertyType['SELECT'],
            label_i18n_ui: {
                en: 'Select',
            },
            description_i18n_ui: undefined,
            required: undefined,
            multiple: false,
            options: [{
                key: 'key',
                value_i18n_ui: {
                    de: 'Wert 1',
                },
            }],
            value_i18n: undefined,
        }, {
            name: 'name5',
            type: CmsFormElementPropertyType['SELECTABLE_OPTIONS'],
            key_label_i18n_ui: undefined,
            value_label_i18n_ui: undefined,
            label_i18n_ui: {
                en: 'Selectable Options',
            },
            description_i18n_ui: undefined,
            required: undefined,
            value: [{
                key: 'some_key',
                value_i18n: {
                    de: 'Ein Schlüssel',
                    en: 'Some Key',
                },
            }],
        }, {
            name: 'name4_2',
            type: CmsFormElementPropertyType['UNSUPPORTED'],
            description_i18n_ui: {
                en: 'A property of unknown name \"name4_2\". This property is not present in the form editor configuration.',
                de: 'Eine unbekannte Eigenschaft namens \"name4_2\". Diese Eigenschaft ist nicht in der Form-Editor-Konfiguration vorhanden.',
            },
            label_i18n_ui: {
                en: 'Unknown Property (name4_2)',
                de: 'Unbekannte Eigenschaft (name4_2)',
            },
            required: false,
            value_i18n: {
                de: 'bla',
                en: 'bla',
            },
        }],
    }

    const ELEMENT_2_BO: CmsFormElementBO = {
        globalId: 'globalId2',
        name: 'Element of type element2',
        type: 'element2',
        active: false,
        type_label_i18n_ui: {
            de: 'Element Zwei',
        },
        description_i18n_ui: {
            de: 'Zweites Element',
        },
        label_property_ui: undefined,
        isContainer: false,
        cannotContain: undefined,
        elements: [],
        properties: [],
    }

    const ELEMENT_3_BO: CmsFormElementBO = {
        globalId: 'globalId3',
        name: 'Element of type element3',
        type: 'element3',
        active: false,
        type_label_i18n_ui: {
            de: 'Element Drei',
        },
        description_i18n_ui: {
            de: 'Drittes Element',
        },
        label_property_ui: undefined,
        isContainer: false,
        cannotContain: undefined,
        elements: [ELEMENT_2_BO],
        properties: [],
    }

    const ELEMENT_UNKNOWN_TYPE_1_BO: CmsFormElementBO = {
        globalId: 'globalIdUnknown',
        name: 'Element of unknown type',
        type: 'elementUnknown',
        active: false,
        type_label_i18n_ui: {
            en: 'Unknown Type (elementUnknown)',
            de: 'Unbekannter Typ (elementUnknown)',
        },
        description_i18n_ui: {
            en: 'An element of unknown type "elementUnknown". This element type is not present in the form editor configuration.',
            de: 'Ein Element des unbekannten Typs "elementUnknown". Dieser Elementtyp ist nicht in der Form-Editor-Konfiguration vorhanden.',
        },
        label_property_ui: undefined,
        isContainer: false,
        cannotContain: undefined,
        elements: [],
        properties: [{
            name: 'name2',
            type: CmsFormElementPropertyType['UNSUPPORTED'],
            description_i18n_ui: {
                en: 'A property of unknown name \"name2\". This property is not present in the form editor configuration.',
                de: 'Eine unbekannte Eigenschaft namens \"name2\". Diese Eigenschaft ist nicht in der Form-Editor-Konfiguration vorhanden.',
            },
            label_i18n_ui: {
                en: 'Unknown Property (name2)',
                de: 'Unbekannte Eigenschaft (name2)',
            },
            required: false,
            value_i18n: {
                de: 50,
                en: -3,
            },
        }],
    }

    const ELEMENT_UNKNOWN_TYPE_2_BO: CmsFormElementBO = {
        globalId: 'globalIdUnknown',
        name: 'Element of unknown type',
        type: 'elementUnknown',
        active: false,
        type_label_i18n_ui: {
            en: 'Unknown Type (elementUnknown)',
            de: 'Unbekannter Typ (elementUnknown)',
        },
        description_i18n_ui: {
            en: 'An element of unknown type "elementUnknown". This element type is not present in the form editor configuration.',
            de: 'Ein Element des unbekannten Typs "elementUnknown". Dieser Elementtyp ist nicht in der Form-Editor-Konfiguration vorhanden.',
        },
        label_property_ui: undefined,
        isContainer: false,
        cannotContain: undefined,
        elements: [],
        properties: [],
    }

    const ELEMENT_FOR_PROPERTY_TESTS_BO: CmsFormElementBO = {
        globalId: 'globalIdPropertyTests',
        name: 'Element of some type',
        type: 'elementPropertyTests',
        active: false,
        type_label_i18n_ui: {
            de: 'Element Eigenschaften',
        },
        description_i18n_ui: {
            de: 'Eigenschaften Element',
        },
        label_property_ui: undefined,
        isContainer: false,
        cannotContain: undefined,
        elements: [],
        properties: [],
    }

    const ELEMENT_FOR_UNKNOWN_PROPERTY_TESTS_BO: CmsFormElementBO = {
        globalId: 'globalIdUnknownPropertyTests',
        name: 'Element of some type',
        type: 'elementUnknownPropertyTests',
        active: false,
        type_label_i18n_ui: {
            de: 'Element Eigenschaften',
        },
        description_i18n_ui: {
            de: 'Eigenschaften Element',
        },
        label_property_ui: undefined,
        isContainer: false,
        cannotContain: undefined,
        elements: [],
        properties: [],
    }

    const PROPERTY_WITH_TOP_LEVEL_I18N_VALUE_BO: CmsFormElementPropertyDefault = {
        name: 'name2',
        type: CmsFormElementPropertyType['NUMBER'],
        label_i18n_ui: {
            en: 'Number',
        },
        description_i18n_ui: undefined,
        validation: 'NUMBER',
        required: false,
        value_i18n: {
            de: 50,
            en: -3,
        },
    }

    const PROPERTY_WITH_TOP_LEVEL_NON_I18N_VALUE_BO: CmsFormElementPropertySelectableOptions = {
        name: 'name5',
        type: CmsFormElementPropertyType['SELECTABLE_OPTIONS'],
        key_label_i18n_ui: undefined,
        value_label_i18n_ui: undefined,
        label_i18n_ui: {
            en: 'Selectable Options',
        },
        description_i18n_ui: undefined,
        required: undefined,
        value: [{
            key: 'some_key',
            value_i18n: {
                de: 'Ein Schlüssel',
                en: 'Some Key',
            },
        }],
    }

    const UNKNOWN_PROPERTY_WITH_TOP_LEVEL_I18N_VALUE_BO: CmsFormElementPropertyUnsupported = {
        name: 'name2',
        type: CmsFormElementPropertyType['UNSUPPORTED'],
        description_i18n_ui: {
            en: 'A property of unknown name \"name2\". This property is not present in the form editor configuration.',
            de: 'Eine unbekannte Eigenschaft namens \"name2\". Diese Eigenschaft ist nicht in der Form-Editor-Konfiguration vorhanden.',
        },
        label_i18n_ui: {
            en: 'Unknown Property (name2)',
            de: 'Unbekannte Eigenschaft (name2)',
        },
        required: false,
        value_i18n: {
            de: 50,
            en: -3,
        },
    }

    const UNKNOWN_PROPERTY_WITH_TOP_LEVEL_NON_I18N_VALUE_BO: CmsFormElementPropertyUnsupported = {
        name: 'name5',
        type: CmsFormElementPropertyType['UNSUPPORTED'],
        description_i18n_ui: {
            en: 'A property of unknown name \"name5\". This property is not present in the form editor configuration.',
            de: 'Eine unbekannte Eigenschaft namens \"name5\". Diese Eigenschaft ist nicht in der Form-Editor-Konfiguration vorhanden.',
        },
        label_i18n_ui: {
            en: 'Unknown Property (name5)',
            de: 'Unbekannte Eigenschaft (name5)',
        },
        required: false,
        value: [{
            key: 'some_key',
            value_i18n: {
                de: 'Ein Schlüssel',
                en: 'Some Key',
            },
        }],
    }

});
