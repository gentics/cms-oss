/* eslint-disable no-useless-escape */
/* eslint-disable @typescript-eslint/naming-convention */
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CmsFormType } from '@gentics/cms-models';
import { FormEditorConfiguration, FormElementPropertyTypeConfiguration, FormElementPropertyValidatorConfiguration } from '../../../common';
import { FormEditorConfigurationService } from './form-editor-configuration.service';

/**
 * FormEditorConfigurationService tests
 * Tests whether FormEditorConfigurationService correctly returns the desired
 * configuration or if it returns a default configuration when desired.
 *
 * Using java objects for the input configuration (instead of a string containing JSON)
 * we also implicitly test whether the configuration is parsed correctly.
 *
 * If there are not multiple positive cases for a setting, it will implicitly be tested
 * in 'if a correct configuration is found, emit the supplied configuration'. However,
 * settings where there are multiple valid options are explicitly tested in its section.
 */
describe('FormEditorConfigurationService', () => {

    const CUSTOMER_CONFIG_PATH = './../ui-conf/';
    const NO_VALUE = Symbol('no-value-loaded');

    let formEditorConfigurationService: FormEditorConfigurationService;
    let httpTestingController: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                HttpClientTestingModule,
            ],
            providers: [
                FormEditorConfigurationService,
            ],
        });

        formEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
        httpTestingController = TestBed.inject(HttpTestingController);

        /**
         * inject special default configuration to make sure it differs from test data!
         * usually bad practice to interfere with non-public API during testing, but here we try
         * to keep the tests meaningful no matter the used DEFAULT_CONFIGURATION.
         */
        (FormEditorConfigurationService as any).DEFAULT_CONFIGURATION = DEFAULT_CONFIGURATION;
    });

    it('should be created', () => {
        const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
        expect(service).toBeTruthy();
    });

    describe('getConfiguration$', () => {
        it('if no configuration is found, emit a default configuration', () => {
            const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
            let result: FormEditorConfiguration | symbol = NO_VALUE;
            const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                result = response;
            });

            expectExactlyOneCallForConfigurationAndRespond('form-editor.json', '404');
            subscription.unsubscribe();
            expect(result).toEqual(NO_VALUE);
        });

        it('if a network error is encountered, emit a default configuration', () => {
            const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
            let result: FormEditorConfiguration | symbol = NO_VALUE;
            const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                result = response;
            });

            expectExactlyOneCallForConfigurationAndRespond('form-editor.json', 'NETWORK_ERROR');
            subscription.unsubscribe();
            expect(result).toEqual(NO_VALUE);
        });

        it('if invalid JSON is found, emit a default configuration', () => {
            const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
            let result: FormEditorConfiguration | symbol = NO_VALUE;
            const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                result = response;
            });

            const BASIC_CONFIGURATION = '{[' as unknown as FormEditorConfiguration;
            expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
            subscription.unsubscribe();
            expect(result).toEqual(NO_VALUE);
        });

        it('if a faulty configuration is found, emit a default configuration', () => {
            const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
            let result: FormEditorConfiguration | symbol = NO_VALUE;
            const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                result = response;
            });

            const BASIC_CONFIGURATION = {} as FormEditorConfiguration;
            expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
            subscription.unsubscribe();
            expect(result).toEqual(NO_VALUE);
        });

        it('if a correct configuration is found, emit the supplied configuration', () => {
            const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
            let result: FormEditorConfiguration | symbol = NO_VALUE;
            const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                result = response;
            });

            const BASIC_CONFIGURATION: FormEditorConfiguration = {
                form_properties: {},
                elements: [{
                    type: 'basic_non_default_element',
                    type_label_i18n_ui: {
                        de: 'Deutsch',
                    },
                    description_i18n_ui: {
                        de: 'Deutsch',
                    },
                    is_container: false,
                    properties: [{
                        name: 'name1',
                        type: FormElementPropertyTypeConfiguration['BOOLEAN'],
                        label_i18n_ui: {
                            de: 'Deutsch',
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
                            de: 'Deutsch',
                        },
                        default_value_i18n: {
                            de: 23,
                            en: 0,
                            it: -1,
                        },
                    }, {
                        name: 'name3',
                        type: FormElementPropertyTypeConfiguration['STRING'],
                        label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        validator: FormElementPropertyValidatorConfiguration['DATE'],
                        default_value_i18n: {
                            de: '23.12.2021',
                            en: '',
                        },
                    }, {
                        name: 'name4',
                        type: FormElementPropertyTypeConfiguration['SELECT'],
                        label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        multiple: false,
                        options: [{
                            key: 'key',
                            value_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                        default_value_i18n: {
                            de: 'key',
                        },
                    }, {
                        name: 'name5',
                        type: FormElementPropertyTypeConfiguration['SELECTABLE_OPTIONS'],
                        label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        default_value: [{
                            key: 'key',
                            value_i18n: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                }],
            };
            expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
            subscription.unsubscribe();
            expect(result).toEqual(BASIC_CONFIGURATION);
        });

        describe('for different form types', () => {
            const typeConfigPairs: { type: CmsFormType, config: FormEditorConfiguration, configFileName: string }[] = [{
                type: CmsFormType.GENERIC,
                config: {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element_found_in_generic_configs',
                        type_label_i18n_ui: {},
                        description_i18n_ui: {},
                        is_container: false,
                        properties: [],
                    }],
                },
                configFileName: 'form-editor.json',
            }, {
                type: CmsFormType.POLL,
                config: {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element_found_in_poll_configs',
                        type_label_i18n_ui: {},
                        description_i18n_ui: {},
                        is_container: false,
                        properties: [],
                    }],
                },
                configFileName: 'form-poll-editor.json',
            }];

            for (const typeConfigPair of typeConfigPairs) {

                it(`if a faulty configuration for type ${typeConfigPair.type} is found, emit a default configuration`, () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$(typeConfigPair.type).subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION = {} as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond(typeConfigPair.configFileName, BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it(`if a correct configuration for type ${typeConfigPair.type} is found, emit the supplied configuration`, () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$(typeConfigPair.type).subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = typeConfigPair.config;
                    expectExactlyOneCallForConfigurationAndRespond(typeConfigPair.configFileName, BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(BASIC_CONFIGURATION);
                });
            }
        });
    });

    describe('form editor configuration parsing', () => {
        it('if no elements property is found, emit a default configuration', () => {
            const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
            let result: FormEditorConfiguration | symbol = NO_VALUE;
            const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                result = response;
            });

            const BASIC_CONFIGURATION: FormEditorConfiguration = {
                form_properties: {},
                notElements: [],
            } as unknown as FormEditorConfiguration;
            expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
            subscription.unsubscribe();
            expect(result).toEqual(NO_VALUE);
        });

        it('if elements property is not an array, emit a default configuration', () => {
            const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
            let result: FormEditorConfiguration | symbol = NO_VALUE;
            const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                result = response;
            });

            const BASIC_CONFIGURATION: FormEditorConfiguration = {
                form_properties: {},
                elements: 23,
            } as unknown as FormEditorConfiguration;
            expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
            subscription.unsubscribe();
            expect(result).toEqual(NO_VALUE);
        });

        it('if elements property is an array, emit the supplied configuration', () => {
            const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
            let result: FormEditorConfiguration | symbol = NO_VALUE;
            const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                result = response;
            });

            const BASIC_CONFIGURATION: FormEditorConfiguration = {
                form_properties: {},
                elements: [],
            };
            expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
            subscription.unsubscribe();
            expect(result).toEqual(BASIC_CONFIGURATION);
        });

        it('if no form_properties property is found, emit the supplied configuration with form_properties set to an empty object', () => {
            const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
            let result: FormEditorConfiguration | symbol = NO_VALUE;
            const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                result = response;
            });

            const BASIC_CONFIGURATION: FormEditorConfiguration = {
                elements: [],
            } as unknown as FormEditorConfiguration;
            expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
            subscription.unsubscribe();
            expect(result).toEqual({
                form_properties: {},
                elements: [],
            });
        });
    });

    describe('form element configuration parsing', () => {
        describe('form element type', () => {
            it('if no element type is found, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element type is not a string, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });


                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 23,
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if there are multiple element types of the same name, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [],
                    }, {
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });
        });

        describe('form element type label i18n', () => {
            it('if no element type label i18n is found, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element type label i18n is null, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: null,
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element type label i18n is an object, but not a correct form configuration string, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });


                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 23,
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });
        });

        describe('form element description i18n', () => {
            it('if no element description i18n is found, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element description i18n is null, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: null,
                        is_container: false,
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element description i18n is an object, but not a correct form configuration string, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 23,
                        },
                        is_container: false,
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });
        });

        describe('form element label property ui', () => {
            it('if no element label property ui setting is found, emit the supplied configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });

            it('if element label property ui is not a string, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });
                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        label_property_ui: 23,
                        is_container: false,
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element label property ui is a string that does not match a property name, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        label_property_ui: 'name2',
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: FormElementPropertyTypeConfiguration['BOOLEAN'],
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            required: true,
                            default_value_i18n: {
                                de: true,
                                en: false,
                            },
                        }, {
                            name: 'name3',
                            type: FormElementPropertyTypeConfiguration['STRING'],
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            validator: FormElementPropertyValidatorConfiguration['DATE'],
                            default_value_i18n: {
                                de: '23.12.2021',
                                en: '',
                            },
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if element label property ui is a string that does match a property name, but that property is not of type STRING,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        label_property_ui: 'name1',
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: FormElementPropertyTypeConfiguration['BOOLEAN'],
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            required: true,
                            default_value_i18n: {
                                de: true,
                                en: false,
                            },
                        }, {
                            name: 'name3',
                            type: FormElementPropertyTypeConfiguration['STRING'],
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            validator: FormElementPropertyValidatorConfiguration['DATE'],
                            default_value_i18n: {
                                de: '23.12.2021',
                                en: '',
                            },
                        }],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if element label property ui is a string that does match a property name and property is of type STRING,
             emit the supplied configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        label_property_ui: 'name3',
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: FormElementPropertyTypeConfiguration['BOOLEAN'],
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            required: true,
                            default_value_i18n: {
                                de: true,
                                en: false,
                            },
                        }, {
                            name: 'name3',
                            type: FormElementPropertyTypeConfiguration['STRING'],
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            validator: FormElementPropertyValidatorConfiguration['DATE'],
                            default_value_i18n: {
                                de: '23.12.2021',
                                en: '',
                            },
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                // cannot_contain is not parsed, due to is_container being false
                delete BASIC_CONFIGURATION.elements[0].cannot_contain;
                expect(result).toEqual(BASIC_CONFIGURATION);
            });
        });

        describe('form element container setting', () => {
            it('if no element container setting is found, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element container setting is boolean (false), emit the supplied configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });


                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });

            it('if element container setting is boolean (true), emit the supplied configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });


                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: true,
                        properties: [],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });

        });

        describe('form element cannot contain', () => {
            it('if no element cannot contain setting is found, emit the supplied configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: true,
                        properties: [],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });

            it('if element cannot contain setting is not an array, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });
                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: true,
                        cannot_contain: 23,
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element cannot contain setting is an array that does not only contain strings, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: true,
                        cannot_contain: ['element type', 23],
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element cannot contain setting is an array that only contains strings, emit the supplied configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: true,
                        cannot_contain: ['element type',  'another element type'],
                        properties: [],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });

            it('if element cannot contain setting is faulty but container setting is false anyway, emit the supplied configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        cannot_contain: 23,
                        properties: [],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                // cannot_contain is not parsed, due to is_container being false
                delete BASIC_CONFIGURATION.elements[0].cannot_contain;
                expect(result).toEqual(BASIC_CONFIGURATION);
            });

        });

        describe('form element properties', () => {
            it('if no element properties are found, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element properties value is not an array, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: 23,
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element properties are in an array that does not only contain properties, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [23],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element properties are in an array that only contains properties, emit the supplied configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });
        });
    });

    describe('form element property configuration errors', () => {

        describe('form element property name', () => {

            it('if no element property name is found, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element property name is not a string, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 23,
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element property name is illegal (globalId), emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'globalId',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element property name is illegal (name), emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element property name is illegal (type), emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'type',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element property name is illegal (active), emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'active',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element property name is illegal (elements), emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'elements',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if there are multiple element properties of the same name, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }, {
                            name: 'name1',
                            type: 'STRING',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });
        });

        describe('form element property type', () => {
            it('if no element property type is found, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element property type is not a valid value, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: 'INVALID_TYPE',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element property type is a valid value, emit the supplied configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: 'BOOLEAN',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }, {
                            name: 'name2',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }, {
                            name: 'name3',
                            type: 'STRING',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }, {
                            name: 'name4',
                            type: 'SELECT',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            multiple: false,
                            options: [],
                        }, {
                            name: 'name5',
                            type: 'SELECTABLE_OPTIONS',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });
        });

        describe('form element property label i18n', () => {
            it('if no element property label i18n is found, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: 'NUMBER',
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element property label i18n is null, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: 'NUMBER',
                            label_i18n_ui: null,
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it('if element property label i18n is an object, but not a correct form configuration string, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 23,
                            },
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });
        });

        describe('form element property description i18n', () => {
            it('if no element property description i18n is found, emit the supplied configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });

            it('if element property description i18n is an object, but not a correct form configuration string, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 23,
                            },
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });
        });

        describe('form element property required', () => {
            it('if no element property required setting is found, emit the supplied configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    }],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });

            it('if element property required setting is boolean (false), emit the supplied configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });


                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: false,
                        properties: [{
                            name: 'name1',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            required: false,
                        }],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });

            it('if element property required setting is boolean (true), emit the supplied configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });


                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {},
                    elements: [{
                        type: 'basic_non_default_element',
                        type_label_i18n_ui: {
                            de: 'Deutsch',
                        },
                        description_i18n_ui: {
                            de: 'Deutsch',
                        },
                        is_container: true,
                        properties: [{
                            name: 'name1',
                            type: 'NUMBER',
                            label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            required: true,
                        }],
                    }],
                } as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });
        });


        describe('form element property type BOOLEAN | NUMBER | STRING', () => {
            describe('form element property validator', () => {
                it('if no element property validator is found, emit the supplied configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'BOOLEAN',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                            }, {
                                name: 'name2',
                                type: 'NUMBER',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                            }, {
                                name: 'name3',
                                type: 'STRING',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(BASIC_CONFIGURATION);
                });

                it('if element property validator is not a valid value, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'BOOLEAN',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                validator: 'INVALID_VALIDATOR',
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it('if element property validator is a valid value, emit the supplied configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'BOOLEAN',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                validator: 'DATE',
                            }, {
                                name: 'name2',
                                type: 'NUMBER',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                validator: 'EMAIL',
                            }, {
                                name: 'name3',
                                type: 'STRING',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                validator: 'NUMBER',
                            }, {
                                name: 'name4',
                                type: 'NUMBER',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                validator: 'TELEPHONE',
                            }, {
                                name: 'name5',
                                type: 'STRING',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                validator: 'TIME',
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(BASIC_CONFIGURATION);
                });
            });

            describe('form element property default value i18n', () => {
                it('if no element property default value i18n is found, emit the supplied configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'BOOLEAN',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                            }, {
                                name: 'name2',
                                type: 'NUMBER',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                            }, {
                                name: 'name3',
                                type: 'STRING',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                            }],
                        }],
                    } as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(BASIC_CONFIGURATION);
                });

                it('if element property default value i18n is an object, but not a correct form default value boolean, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'BOOLEAN',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                default_value_i18n: {
                                    de: 23,
                                    en: 'English',
                                },
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it('if element property default value i18n is an object, but not a correct form default value number, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'BOOLEAN',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                default_value_i18n: {
                                    de: true,
                                    en: 'English',
                                },
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it('if element property default value i18n is an object, but not a correct form default value string, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'BOOLEAN',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                default_value_i18n: {
                                    de: 23,
                                    en: false,
                                },
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });
            });
        });

        describe('form element property type SELECT', () => {
            describe('form element property options', () => {
                it('if no element property options is found, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECT',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it('if element property options value is not an array, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECT',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                options: 23,
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it('if element property options is an array that does contain other objects than option configurations, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });
                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECT',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                options: [23],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it('if element property options is an array that does contain option configurations without keys, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECT',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                options: [{
                                    value_i18n_ui: {
                                        de: 'Deutsch',
                                    },
                                }],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it('if element property options is an array that does contain option configurations with invalid keys, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECT',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                options: [{
                                    key: 23,
                                    value_i18n_ui: {
                                        de: 'Deutsch',
                                    },
                                }],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it('if element property options is an array that does contain option configurations without values i18n, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECT',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                options: [{
                                    key: 'key',
                                }],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it(`if element property options is an array that does contain option configurations with untranslated values,
                 emit a default configuration`, () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECT',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                options: [{
                                    key: 'key',
                                    value_i18n_ui: 23,
                                }],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it(`if element property options is an array that does contain option configurations with invalid values i18n,
                 emit a default configuration`, () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECT',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                options: [{
                                    key: 'key',
                                    value_i18n_ui: {
                                        de: 23,
                                    },
                                }],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it('if element property options is an array that does contain option configurations, emit the supplied configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });
                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECT',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                multiple: false,
                                options: [{
                                    key: 'key',
                                    value_i18n_ui: {
                                        de: 'Deutsch',
                                    },
                                }],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(BASIC_CONFIGURATION);
                });
            });

            describe('form element property default value i18n', () => {
                it('if no element property default value i18n is found, emit the supplied configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECT',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                multiple: false,
                                options: [{
                                    key: 'key',
                                    value_i18n_ui: {
                                        de: 'Deutsch',
                                    },
                                }],
                            }],
                        }],
                    } as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(BASIC_CONFIGURATION);
                });

                it('if element property default value i18n is an object, but not a correct default value string, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECT',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                default_value_i18n: {
                                    de: 23,
                                },
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });
            });
        });

        describe('form element property type SELECTABLE_OPTIONS', () => {
            describe('form element property default value', () => {
                it('if no element property default value is found, emit the supplied configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECTABLE_OPTIONS',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                            }],
                        }],
                    } as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(BASIC_CONFIGURATION);
                });

                it('if element property default value value is not an array, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECTABLE_OPTIONS',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                default_value: 23,
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it(`if element property default value is an array that does contain other objects than default key value pairs,
                 emit a default configuration`, () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECTABLE_OPTIONS',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                default_value: [23],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it('if element property default value is an array that does contain default key value pairs without keys, emit a default configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECTABLE_OPTIONS',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                default_value: [{
                                    value_i18n: {
                                        de: 'Deutsch',
                                    },
                                }],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it(`if element property default value is an array that does contain default key value pairs with invalid keys,
             emit a default configuration`, () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECTABLE_OPTIONS',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                default_value: [{
                                    key: 23,
                                    value_i18n: {
                                        de: 'Deutsch',
                                    },
                                }],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it(`if element property default value is an array that does contain default key value pairs without values i18n,
                 emit a default configuration`, () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECTABLE_OPTIONS',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                default_value: [{
                                    key: 'key',
                                }],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it(`if element property default value is an array that does contain default key value pairs with untranslated values,
                    emit a default configuration`, () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECTABLE_OPTIONS',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                default_value: [{
                                    key: 'key',
                                    value_i18n: 23,
                                }],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it(`if element property default value is an array that does contain default key value pairs with invalid values i18n,
                    emit a default configuration`, () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });

                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECTABLE_OPTIONS',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                default_value: [{
                                    key: 'key',
                                    value_i18n: {
                                        de: 23,
                                    },
                                }],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(NO_VALUE);
                });

                it('if element property default value is an array that does contain default key value pairs, emit the supplied configuration', () => {
                    const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                    let result: FormEditorConfiguration | symbol = NO_VALUE;
                    const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                        result = response;
                    });
                    const BASIC_CONFIGURATION: FormEditorConfiguration = {
                        form_properties: {},
                        elements: [{
                            type: 'basic_non_default_element',
                            type_label_i18n_ui: {
                                de: 'Deutsch',
                            },
                            description_i18n_ui: {
                                de: 'Deutsch',
                            },
                            is_container: false,
                            properties: [{
                                name: 'name1',
                                type: 'SELECTABLE_OPTIONS',
                                label_i18n_ui: {
                                    de: 'Deutsch',
                                },
                                default_value: [{
                                    key: 'key',
                                    value_i18n: {
                                        de: 'Deutsch',
                                    },
                                }],
                            }],
                        }],
                    } as unknown as FormEditorConfiguration;
                    expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                    subscription.unsubscribe();
                    expect(result).toEqual(BASIC_CONFIGURATION);
                });
            });
        });

    });

    describe('form property configuration errors', () => {

        describe('form property \'admin_mail_options\'', () => {

            it('if form property \'admin_mail_options\' is not an array, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        admin_mail_options: 23,
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property \'admin_mail_options\' is an array that does contain other objects than option configurations,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });
                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        admin_mail_options: [23],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property \'admin_mail_options\' is an array that does contain option configurations without keys,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        admin_mail_options: [{
                            value_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property \'admin_mail_options\' is an array that does contain option configurations with invalid keys,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        admin_mail_options: [{
                            key: 23,
                            value_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property \'admin_mail_options\' is an array that does contain option configurations without values i18n,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        admin_mail_options: [{
                            key: 'key',
                        }],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property 'admin_mail_options' is an array that does contain option configurations with untranslated values,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        admin_mail_options: [{
                            key: 'key',
                            value_i18n_ui: 23,
                        }],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property 'admin_mail_options' is an array that does contain option configurations with invalid values i18n,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        admin_mail_options: [{
                            key: 'key',
                            value_i18n_ui: {
                                de: 23,
                            },
                        }],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property \'admin_mail_options\' is an array that does contain option configurations,
             emit the supplied configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });
                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        admin_mail_options: [{
                            key: 'key',
                            value_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });
        });

        describe('form property \'template_context_options\'', () => {

            it('if form property \'template_context_options\' is not an array, emit a default configuration', () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        template_context_options: 23,
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property \'template_context_options\' is an array that does contain other objects than option configurations,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });
                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        template_context_options: [23],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property \'template_context_options\' is an array that does contain option configurations without keys,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        template_context_options: [{
                            value_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property \'template_context_options\' is an array that does contain option configurations with invalid keys,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        template_context_options: [{
                            key: 23,
                            value_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property \'template_context_options\' is an array that does contain option configurations without values i18n,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        template_context_options: [{
                            key: 'key',
                        }],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property 'template_context_options' is an array that does contain option configurations with untranslated values,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        template_context_options: [{
                            key: 'key',
                            value_i18n_ui: 23,
                        }],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property 'template_context_options' is an array that does contain option configurations with invalid values i18n,
             emit a default configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });

                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        template_context_options: [{
                            key: 'key',
                            value_i18n_ui: {
                                de: 23,
                            },
                        }],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(NO_VALUE);
            });

            it(`if form property \'template_context_options\' is an array that does contain option configurations,
             emit the supplied configuration`, () => {
                const service: FormEditorConfigurationService = TestBed.inject(FormEditorConfigurationService);
                let result: FormEditorConfiguration | symbol = NO_VALUE;
                const subscription = service.getConfiguration$('GENERIC').subscribe(response => {
                    result = response;
                });
                const BASIC_CONFIGURATION: FormEditorConfiguration = {
                    form_properties: {
                        template_context_options: [{
                            key: 'key',
                            value_i18n_ui: {
                                de: 'Deutsch',
                            },
                        }],
                    },
                    elements: [],
                } as unknown as FormEditorConfiguration;
                expectExactlyOneCallForConfigurationAndRespond('form-editor.json', BASIC_CONFIGURATION);
                subscription.unsubscribe();
                expect(result).toEqual(BASIC_CONFIGURATION);
            });
        });

    });


    const expectExactlyOneCallForConfigurationAndRespond = (
        expectedConfigFileName: string,
        response: FormEditorConfiguration | '404' | 'NETWORK_ERROR',
    ): void => {
        const request = httpTestingController.expectOne(request => request.url === `${CUSTOMER_CONFIG_PATH}${expectedConfigFileName}`);
        expect(request.request.method).toEqual('GET');
        if (response === '404') {
            request.flush(null, {status: 404, statusText: 'Not Found'});
        } else if (response === 'NETWORK_ERROR') {
            request.error(new ErrorEvent('error'));
        } else {
            request.flush(response);
        }
        httpTestingController.verify();
    }

    // for better readability of the file, larger constants are placed at the bottom against convention

    const DEFAULT_CONFIGURATION: FormEditorConfiguration = {
        form_properties: {},
        elements: [{
            type: 'basic_default_element',
            type_label_i18n_ui: {},
            description_i18n_ui: {},
            is_container: false,
            properties: [],
        }],
    };

});
