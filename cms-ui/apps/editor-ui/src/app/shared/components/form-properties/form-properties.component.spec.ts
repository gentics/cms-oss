import { Component, DebugElement, NO_ERRORS_SCHEMA, ViewChild } from '@angular/core';
import { ComponentFixture, flush, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { Api } from '@editor-ui/app/core/providers/api';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { ApplicationStateService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { mockPipes } from '@editor-ui/testing/mock-pipe';
import { RepositoryBrowserOptions } from '@gentics/cms-integration-api-models';
import { ItemInNode, Language, Page, Raw } from '@gentics/cms-models';
import { getExamplePageData } from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import {
    FormEditorConfiguration,
    FormEditorConfigurationService,
    FormEditorService,
} from '@gentics/form-generator';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { Observable, of } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { RepositoryBrowserClient } from '../../providers/repository-browser-client/repository-browser-client.service';
import { SelectedItemHelper } from '../../util/selected-item-helper/selected-item-helper';
import { FormPropertiesComponent } from './form-properties-form.component';

type PageWithNodeId = ItemInNode<Page<Raw>>;

describe('FormPropertiesForm', () => {

    beforeEach(() => {
        configureComponentTest({
            imports: [GenticsUICoreModule.forRoot(), FormsModule, ReactiveFormsModule],
            providers: [
                { provide: Api, useClass: MockApi },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: FormEditorConfigurationService, useClass: MockFormEditorConfigurationService },
                { provide: FormEditorService, useClass: MockFormEditorService },
                { provide: RepositoryBrowserClient, useClass: MockRepositoryBrowserClient },
                { provide: I18nService, useClass: TestI18nService },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
            ],
            declarations: [
                TestComponent,
                FormPropertiesComponent,
                mockPipes('i18n'),
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });

    });

    describe('language selection', () => {
        const getLanguageSelectionElement = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('[formcontrolname="languages"]'));

        it('does show language selection if two languages are available',
            componentTest(() => TestComponent, (fixture, instance) => {

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
                const languageSelectionElement = getLanguageSelectionElement(fixture);
                expect(languageSelectionElement?.nativeElement).not.toBeNull();
                expect(languageSelectionElement.componentInstance.optionGroups[0].options.length).toEqual(2);
                flush();
            }),
        );

        it('does not show language selection if only one languages is available',
            componentTest(() => TestComponent, (fixture, instance) => {

                instance.languages = [
                    {
                        code: 'de',
                        id: 1,
                        name: 'Deutsch',
                    },
                ];
                fixture.detectChanges();
                tick();

                const languageSelectionElement = getLanguageSelectionElement(fixture);
                expect(languageSelectionElement).toBeNull();
                flush();
            }),
        );

        it('does not show language selection if no languages are available',
            componentTest(() => TestComponent, (fixture, instance) => {

                const languageSelectionElement = getLanguageSelectionElement(fixture);
                expect(languageSelectionElement).toBeNull();
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
                    mockPipes('i18n'),
                ],
                schemas: [NO_ERRORS_SCHEMA],
            })
            repositoryBrowserClient = new MockRepositoryBrowserClient();

        })

        it('useEmailPageTemplate should only be true if mailsource_pageid is set',
            componentTest(() => FormPropertiesComponent, (fixture, instance) => {
                if (instance.properties.data == null) {
                    instance.properties.data = {};
                }
                instance.properties.data.mailtemp_i18n = undefined;
                spyOn(instance, 'initSelectedItemHelper').and.returnValue(new MockSelectedItemHelper() as any);
                spyOn(instance, 'trackDisplayValue').and.returnValue(of('page'));

                const options: RepositoryBrowserOptions = { selectMultiple: false, allowedSelection: 'page' };
                instance.ngOnInit();

                callRepositoryBrowser(TEST_PAGE);
                repositoryBrowserClient.openRepositoryBrowser(options)
                    .then((selectedTemplatePage: ItemInNode<Page<Raw>>) => {
                        instance.setEmailTemplatePage(selectedTemplatePage);
                    });
                tick();
                expect(instance.useEmailPageTemplate).toBeTruthy();

                instance.properties.data.mailtemp_i18n = null;

                callRepositoryBrowser(TEST_PAGE_WITH_NO_VALUES);
                repositoryBrowserClient.openRepositoryBrowser(options)
                    .then((selectedTemplatePage: ItemInNode<Page<Raw>>) => {
                        instance.setEmailTemplatePage(selectedTemplatePage);
                    });
                tick();
                fixture.detectChanges();

                expect(instance.useEmailPageTemplate).toBeFalsy();
                flush();
            }),
        );

        it('openRepositoryBrowser returns the correct Promise and setEmailTemplatePage sets the correct ID',
            componentTest(() => FormPropertiesComponent, fixture => {
                const options: RepositoryBrowserOptions = { selectMultiple: false, allowedSelection: 'page' };
                const instance: FormPropertiesComponent = fixture.componentInstance;
                instance.ngOnInit();

                callRepositoryBrowser(TEST_PAGE);
                repositoryBrowserClient.openRepositoryBrowser(options)
                    .then((selectedTemplatePage: ItemInNode<Page<Raw>>) => {
                        instance.setEmailTemplatePage(selectedTemplatePage);
                    });
                tick();
                expect(instance.dataGroup.controls.mailsource_pageid.value).toBe(111);
            }),
        );
    })
});

@Component({
    template: `
        <gtx-form-properties
            #propertiesForm
            [isMultiLang]="isMultiLang"
            [languages]="languages"
        ></gtx-form-properties>
    `,
})
class TestComponent {
    @ViewChild('propertiesForm', { static: true })
    propertiesForm: FormPropertiesComponent;
    isMultiLang = true;
    languages: Language[] = [];
}

class MockFormEditorConfigurationService {
    getConfiguration$: () => Observable<FormEditorConfiguration> = () => of({
        form_properties: {},
        elements: [],
    });
}

class MockFormEditorService {
    activeUiLanguageCode: string;
}

class MockApi {}

class MockRepositoryBrowserClient {
    openRepositoryBrowser = jasmine.createSpy('openRepositoryBrowser');
}

class TestI18nService {
    translate(key: string): string {
        return key;
    }
}

class MockSelectedItemHelper {
    setSelectedItem(item: any): void;
    setSelectedItem(item: number | any, nodeId?: number): void;
    setSelectedItem(itemId: number, nodeId?: number): void {
        // noop
    }
}

function getExamplePageWithNodeId({ pageId, nodeId }: { pageId: number, nodeId: number }): PageWithNodeId {
    const page: PageWithNodeId = getExamplePageData({ id: pageId }) as any;
    page.nodeId = nodeId;
    return page;
}
