import { Component, DebugElement, NO_ERRORS_SCHEMA, ViewChild } from '@angular/core';
import { ComponentFixture, flush, TestBed, tick } from '@angular/core/testing';
import { FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { BrowseBoxComponent, CmsComponentsModule, GCMS_UI_SERVICES_PROVIDER, I18nInputComponent } from '@gentics/cms-components';
import { RepositoryBrowserOptions } from '@gentics/cms-integration-api-models';
import {
    EditableFormData,
    Form,
    FormSettingType,
    FormTypeConfiguration,
    FormTypeConfigurationListResponse,
    ItemInNode,
    Language,
    Page,
    PageResponse,
    Raw,
    ResponseCode,
} from '@gentics/cms-models';
import { getExamplePageData } from '@gentics/cms-models/testing';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { FormProperties, GenticsUICoreModule, SelectComponent } from '@gentics/ui-core';
import { of } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { Api } from '../../../core/providers/api';
import { GcmsUiServicesProvider } from '../../../core/providers/gcms-ui-services/gcms-ui-services.service';
import { ApplicationStateService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { RepositoryBrowserClient } from '../../providers/repository-browser-client/repository-browser-client.service';
import { SelectedItemHelper } from '../../util/selected-item-helper/selected-item-helper';
import { FormPropertiesComponent, FormPropertiesMode } from './form-properties.component';

type PageWithNodeId = ItemInNode<Page<Raw>>;

describe('FormProperties', () => {

    let client: GCMSTestRestClientService;
    function setupClient(): void {
        client = TestBed.inject(GCMSRestClientService) as any;

        spyOn(client.form, 'listConfigurations').and.callFake(() => {
            return of({
                responseInfo: {
                    responseCode: ResponseCode.OK,
                },
                items: [],
                hasMoreItems: false,
                numItems: 0,
            } as FormTypeConfigurationListResponse);
        });
    }

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
                CmsComponentsModule,
                FormsModule,
                ReactiveFormsModule,
            ],
            providers: [
                { provide: Api, useClass: MockApi },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: RepositoryBrowserClient, useClass: MockRepositoryBrowserClient },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                { provide: GcmsUiServicesProvider, useValue: null },
                { provide: GCMS_UI_SERVICES_PROVIDER, useValue: null },
            ],
            declarations: [
                TestComponent,
                FormPropertiesComponent,
                I18nInputComponent,
                BrowseBoxComponent,
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });
    });

    describe('language selection', () => {
        const getLanguageSelectionElement = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('[formcontrolname="languages"]'));

        it('does show language selection if two languages are available',
            componentTest(() => TestComponent, (fixture, instance) => {
                setupClient();
                instance.isMultiLang = true;
                instance.languages = [
                    {
                        code: 'de',
                        id: 1,
                        name: 'Deutsch',
                    },
                    {
                        code: 'en',
                        id: 2,
                        name: 'Englisch',
                    },
                ];
                fixture.detectChanges();
                tick();
                fixture.whenRenderingDone();
                tick();

                const languageSelectionElement = getLanguageSelectionElement(fixture);
                expect(languageSelectionElement?.nativeElement).not.toBeNull();
                const langSelect: SelectComponent = languageSelectionElement.componentInstance;
                expect(langSelect.optionGroups[0].options.length).toEqual(2);
                flush();
            }),
        );

        it('does not show language selection if no languages are available',
            componentTest(() => TestComponent, (fixture, instance) => {
                setupClient();
                const languageSelectionElement = getLanguageSelectionElement(fixture);
                expect(languageSelectionElement).toBeNull();
            }),
        );
    });

    describe('form settings', () => {
        const TYPE_WITH_SETTINGS = 'withSettings';
        const TYPE_WITH_PATH_SETTINGS = 'withPathSettings';
        const TYPE_WITHOUT_SETTINGS = 'withoutSettings';

        const CONFIGURATIONS: FormTypeConfiguration[] = [
            {
                type: TYPE_WITH_SETTINGS,
                pluginName: 'forms',
                controls: {},
                flows: [],
                settings: [
                    {
                        type: FormSettingType.STRING,
                        id: 'requiredSetting',
                        labelI18n: { en: 'Required setting' },
                        required: true,
                    },
                    {
                        type: FormSettingType.BOOLEAN,
                        id: 'optionalSetting',
                        labelI18n: { en: 'Optional setting' },
                    },
                ],
            },
            {
                type: TYPE_WITH_PATH_SETTINGS,
                pluginName: 'forms',
                controls: {},
                flows: [],
                settings: [
                    {
                        type: FormSettingType.STRING,
                        id: 'pathSetting',
                        labelI18n: { en: 'Path setting' },
                        propertyPath: 'custom.value',
                    },
                ],
            },
            {
                type: TYPE_WITHOUT_SETTINGS,
                pluginName: 'forms',
                controls: {},
                flows: [],
            },
        ];

        beforeEach(() => {
            configureComponentTest({
                imports: [GenticsUICoreModule.forRoot(), FormsModule, ReactiveFormsModule],
                providers: [
                    { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                ],
                declarations: [FormPropertiesComponent],
                schemas: [NO_ERRORS_SCHEMA],
            });
        });

        function setupConfigurations(): void {
            const restClient = TestBed.inject(GCMSRestClientService);
            spyOn(restClient.form, 'listConfigurations').and.returnValue(of({
                responseInfo: {
                    responseCode: ResponseCode.OK,
                },
                items: CONFIGURATIONS,
                hasMoreItems: false,
                numItems: CONFIGURATIONS.length,
            } as FormTypeConfigurationListResponse));
        }

        function initialize(fixture: ComponentFixture<FormPropertiesComponent>, instance: FormPropertiesComponent, data: Partial<EditableFormData> = {}): void {
            setupConfigurations();
            instance.mode = FormPropertiesMode.CREATE;
            instance.value = { data } as any;
            fixture.detectChanges();
            tick();
            fixture.detectChanges();
        }

        function selectType(fixture: ComponentFixture<FormPropertiesComponent>, instance: FormPropertiesComponent, type: string): void {
            instance.setActiveFormType(type);
            fixture.detectChanges();
            tick();
        }

        const getSettingsElement = (fixture: ComponentFixture<FormPropertiesComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('gtx-dynamic-form-settings'));

        it('shows the form settings after a form type with settings is selected',
            componentTest(() => FormPropertiesComponent, (fixture, instance) => {
                initialize(fixture, instance);
                expect(getSettingsElement(fixture)).toBeNull();

                selectType(fixture, instance, TYPE_WITH_SETTINGS);

                expect(getSettingsElement(fixture)).not.toBeNull();
                expect(instance.formSettings.map((setting) => setting.propertyPath))
                    .toEqual(['formSettings.requiredSetting', 'formSettings.optionalSetting']);
                flush();
            }),
        );

        it('does not show form settings for a form type without settings',
            componentTest(() => FormPropertiesComponent, (fixture, instance) => {
                initialize(fixture, instance);
                selectType(fixture, instance, TYPE_WITHOUT_SETTINGS);

                expect(getSettingsElement(fixture)).toBeNull();
                expect(instance.formData.contains('formSettings')).toBeFalse();
                flush();
            }),
        );

        it('stores the values of the form settings in data.formSettings',
            componentTest(() => FormPropertiesComponent, (fixture, instance) => {
                initialize(fixture, instance);
                selectType(fixture, instance, TYPE_WITH_SETTINGS);

                instance.updateFormSettingsData({ formSettings: { requiredSetting: 'value', optionalSetting: true } } as any);
                tick();

                expect(instance.form.value.data.formSettings).toEqual({ requiredSetting: 'value', optionalSetting: true });
                expect(instance.formData.errors).toBeNull();
                flush();
            }),
        );

        it('initializes the form settings from the form data',
            componentTest(() => FormPropertiesComponent, (fixture, instance) => {
                initialize(fixture, instance, { formSettings: { requiredSetting: 'existing' } });
                selectType(fixture, instance, TYPE_WITH_SETTINGS);

                expect((instance.formSettingsData as any).formSettings).toEqual({ requiredSetting: 'existing' });
                expect(instance.form.value.data.formSettings).toEqual({ requiredSetting: 'existing' });
                flush();
            }),
        );

        it('is invalid when a required form setting has no value',
            componentTest(() => FormPropertiesComponent, (fixture, instance) => {
                initialize(fixture, instance);
                selectType(fixture, instance, TYPE_WITH_SETTINGS);

                instance.updateFormSettingsData({ formSettings: { optionalSetting: true } } as any);
                tick();

                expect(instance.formData.errors).toEqual({ formSettingsRequired: ['requiredSetting'] });
                expect(instance.formData.valid).toBeFalse();
                flush();
            }),
        );

        it('stores the values of form settings with a propertyPath relative to the form data',
            componentTest(() => FormPropertiesComponent, (fixture, instance) => {
                initialize(fixture, instance);
                selectType(fixture, instance, TYPE_WITH_PATH_SETTINGS);

                instance.updateFormSettingsData({ custom: { value: 'path value' } } as any);
                tick();

                expect((instance.form.value.data as any).custom).toEqual({ value: 'path value' });
                expect(instance.form.value.data.formSettings).toBeUndefined();
                flush();
            }),
        );

        it('removes the form settings of the previously selected form type',
            componentTest(() => FormPropertiesComponent, (fixture, instance) => {
                initialize(fixture, instance);
                selectType(fixture, instance, TYPE_WITH_SETTINGS);
                expect(instance.formData.contains('formSettings')).toBeTrue();

                selectType(fixture, instance, TYPE_WITHOUT_SETTINGS);

                expect(instance.formData.contains('formSettings')).toBeFalse();
                expect(instance.formData.errors).toBeNull();
                flush();
            }),
        );
    });

    describe('email template', () => {
        function callRepositoryBrowser(page: PageWithNodeId) {
            repositoryBrowserClient.openRepositoryBrowser = jasmine.createSpy('openRepositoryBrowser')
                .and.returnValue(Promise.resolve(page));
        }
        let repositoryBrowserClient: MockRepositoryBrowserClient;

        const TEST_PAGE = getExamplePageWithNodeId({ pageId: 111, nodeId: 11 });
        const TEST_PAGE_WITH_NO_VALUES = getExamplePageWithNodeId({ pageId: null, nodeId: null });

        beforeEach(() => {
            configureComponentTest({
                imports: [GenticsUICoreModule.forRoot(), FormsModule, ReactiveFormsModule],
                providers: [
                    { provide: SelectedItemHelper, useClass: MockSelectedItemHelper },
                ],
                declarations: [
                    TestComponent,
                    FormPropertiesComponent,
                ],
                schemas: [NO_ERRORS_SCHEMA],
            });
            repositoryBrowserClient = new MockRepositoryBrowserClient();
            setupClient();
        });

        it('useEmailPageTemplate should only be true if mailsource_pageid is set',
            componentTest(() => FormPropertiesComponent, (fixture, instance) => {
                const restClient = TestBed.inject(GCMSRestClientService);
                restClient.page.get = () => of({
                    page: TEST_PAGE,
                    responseInfo: {
                        responseCode: ResponseCode.OK,
                        responseMessage: 'mocked',
                    },
                    messages: [],
                } as PageResponse);

                instance.value = {
                    data: {
                        adminEmailNodeId: TEST_PAGE.nodeId,
                        adminEmailPageId: TEST_PAGE.id,
                    },
                } as Form;
                fixture.detectChanges();
                tick();

                expect(instance.useEmailPageTemplate).toBeTruthy();
            }),
        );

        it('openRepositoryBrowser returns the correct Promise and setEmailTemplatePage sets the correct ID',
            componentTest(() => FormPropertiesComponent, (fixture) => {
                const options: RepositoryBrowserOptions = { selectMultiple: false, allowedSelection: 'page' };
                const instance: FormPropertiesComponent = fixture.componentInstance;
                instance.ngOnInit();

                callRepositoryBrowser(TEST_PAGE);
                repositoryBrowserClient.openRepositoryBrowser(options)
                    .then((selectedTemplatePage: ItemInNode<Page<Raw>>) => {
                        instance.setEmailTemplatePage(selectedTemplatePage);
                    });
                tick();
                expect((instance.form.controls.data as FormGroup<FormProperties<EditableFormData>>).controls.adminEmailPageId.value).toBe(111);
            }),
        );
    });
});

@Component({
    template: `
        <gtx-form-properties
            #propertiesForm
            [isMultiLang]="isMultiLang"
            [languages]="languages"
            [value]="value"
        ></gtx-form-properties>
    `,
    standalone: false,
})
class TestComponent {
    @ViewChild('propertiesForm', { static: true })
    propertiesForm: FormPropertiesComponent;

    isMultiLang = true;
    languages: Language[] = [];
    value = {};
}

class MockApi {}

class MockRepositoryBrowserClient {
    openRepositoryBrowser = jasmine.createSpy('openRepositoryBrowser');
}

class MockSelectedItemHelper {
    setSelectedItem(item: any): void;
    setSelectedItem(item: number | any, nodeId?: number): void;
    setSelectedItem(itemId: number, nodeId?: number): void {
        // noop
    }
}

function getExamplePageWithNodeId({ pageId, nodeId }: { pageId: number; nodeId: number }): PageWithNodeId {
    const page: PageWithNodeId = getExamplePageData({ id: pageId }) as any;
    page.nodeId = nodeId;
    return page;
}
