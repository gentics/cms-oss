import { Component, DebugElement, NO_ERRORS_SCHEMA, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { CoreModule } from '@gentics/cms-components';
import { EditablePageProps, EditorPermissions, Language, Template } from '@gentics/cms-models';
import {
    getExampleFolderDataNormalized,
    getExamplePageData,
    getExampleTemplateData,
    getExampleTemplateDataNormalized,
} from '@gentics/cms-models/testing/test-data.mock';
import { CheckboxComponent, GenticsUICoreModule } from '@gentics/ui-core';
import { Observable, of } from 'rxjs';
import { componentTest } from '../../../../testing/component-test';
import { configureComponentTest } from '../../../../testing/configure-component-test';
import { mockPipes } from '../../../../testing/mock-pipe';
import { Api } from '../../../core/providers/api';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { ApplicationStateService, FeaturesActionsService, FolderActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { PagePropertiesForm } from './page-properties-form.component';

const testDataNodeId = 4;
const testDataFolderId = 1;
const testDataFolder = { ...getExampleFolderDataNormalized(), id: 123, nodeId: testDataNodeId, folderId: testDataFolderId };
const testDataPage = { ...getExamplePageData(), id: 123, nodeId: testDataNodeId, folderId: testDataFolderId };
const testDataTemplate = { ...getExampleTemplateDataNormalized(), id: 123, nodeId: testDataNodeId, folderId: testDataFolderId };
const testDataTemplateRaw = { ...getExampleTemplateData(), id: 123, nodeId: testDataNodeId, folderId: testDataFolderId };

describe('PagePropertiesForm', () => {

    let api: MockApi;
    let featuresActions: MockFeaturesActions;
    let folderActions: MockFolderActions;
    let permissions: PermissionService;
    let state: TestApplicationState;

    const getMockState = (templateList: number[] = [testDataTemplate.id]) => {
        return {
            entities: {
                folder: {
                    [testDataFolder.id]: testDataFolder,
                },
                template: {
                    [testDataTemplate.id]: testDataTemplate,
                },
            },
            folder: {
                activeNode: testDataNodeId,
                activeFolder: testDataFolderId,
                templates: {
                    list: templateList,
                },
            },
        };
    };

    beforeEach(() => {
        configureComponentTest({
            imports: [GenticsUICoreModule.forRoot(), FormsModule, ReactiveFormsModule, CoreModule],
            providers: [
                { provide: Api, useClass: MockApi },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: FeaturesActionsService, useClass: MockFeaturesActions },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                EntityResolver,
                { provide: ContextMenuOperationsService, useClass: MockContextMenuOperationsService },
                { provide: PermissionService, useClass: MockPermissionService },
            ],
            declarations: [
                TestComponent,
                PagePropertiesForm,
                ...mockPipes('i18nDate'),
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });
        api = TestBed.get(Api);
        featuresActions = TestBed.get(FeaturesActionsService);
        folderActions = TestBed.get(FolderActionsService);
        permissions = TestBed.get(PermissionService);
        state = TestBed.get(ApplicationStateService);

        state.mockState(getMockState());
    });

    describe('sanitization', () => {
        const getPageName = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('[formcontrolname="pageName"]'));

        const getSuggestedOrRequestedFileName = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('[formcontrolname="suggestedOrRequestedFileName"]'));

        it('file name suggestion works well for some pageName for english version if no file name has been entered manually',
            componentTest(() => TestComponent, (fixture, instance) => {
                const pageNameValue = 'Außenwirtschaft Oberösterreich';
                const expectedChanges = {
                    ...instance.properties,
                    pageName: pageNameValue,
                    fileName: '',
                    suggestedOrRequestedFileName: 'Aussenwirtschaft_Oberoesterreich.en.html',
                };

                fixture.detectChanges();
                tick();

                api.folders.suggestPageFileName.and.returnValue(of({ fileName: 'Aussenwirtschaft_Oberoesterreich.en.html' }));

                const pageName = getPageName(fixture);

                instance.propertiesForm.form.controls['pageName'].setValue(pageNameValue);

                // instance.onChange.calls.reset();

                pageName.triggerEventHandler('keyup', {});

                fixture.detectChanges();
                tick(600); // Wait at least 400ms for the request to be triggered
                fixture.detectChanges();

                expect(api.folders.suggestPageFileName).toHaveBeenCalledTimes(1);
                expect(api.folders.suggestPageFileName).toHaveBeenCalledWith({
                    folderId: instance.folderId,
                    nodeId: instance.nodeId,
                    templateId: instance.properties.templateId,
                    language: 'en',
                    pageName: pageNameValue,
                    fileName: '',
                });

                expect(instance.onChange).toHaveBeenCalledWith(expectedChanges);
            }),
        );

        it('file name suggestion works well for some pageName for german version if no file name has been entered manually',
            componentTest(() => TestComponent, (fixture, instance) => {
                const pageNameValue = 'Außenwirtschaft Oberösterreich';
                const expectedChanges = {
                    ...instance.properties,
                    language: 'de',
                    pageName: pageNameValue,
                    fileName: '',
                    suggestedOrRequestedFileName: 'Aussenwirtschaft_Oberoesterreich.de.html',
                };

                fixture.detectChanges();
                tick();

                api.folders.suggestPageFileName.and.returnValue(of({ fileName: 'Aussenwirtschaft_Oberoesterreich.de.html' }));

                const pageName = getPageName(fixture);

                instance.propertiesForm.form.controls['pageName'].setValue(pageNameValue);
                instance.propertiesForm.form.controls['language'].setValue('de');

                instance.onChange.calls.reset();

                pageName.triggerEventHandler('keyup', {});

                fixture.detectChanges();
                tick(600); // Wait at least 400ms for the request to be triggered
                fixture.detectChanges();

                expect(api.folders.suggestPageFileName).toHaveBeenCalledTimes(1);
                expect(api.folders.suggestPageFileName).toHaveBeenCalledWith({
                    folderId: instance.folderId,
                    nodeId: instance.nodeId,
                    templateId: instance.properties.templateId,
                    language: 'de',
                    pageName: pageNameValue,
                    fileName: '',
                });

                expect(instance.onChange).toHaveBeenCalledWith(expectedChanges);
            }),
        );

        it('sanitizing works well for some fileName for english version',
            componentTest(() => TestComponent, (fixture, instance) => {
                const fileNameValue = 'Außenwirtschaft Oberösterreich';
                const expectedChanges = {
                    ...instance.properties,
                    fileName: 'Aussenwirtschaft_Oberoesterreich.en.html',
                    suggestedOrRequestedFileName: 'Aussenwirtschaft_Oberoesterreich.en.html',
                };

                fixture.detectChanges();
                tick();

                api.folders.suggestPageFileName.and.returnValue(of({ fileName: 'Aussenwirtschaft_Oberoesterreich.en.html' }));

                const fileName = getSuggestedOrRequestedFileName(fixture);

                instance.propertiesForm.form.controls['suggestedOrRequestedFileName'].setValue(fileNameValue);

                instance.onChange.calls.reset();

                fileName.triggerEventHandler('blur', {});

                fixture.detectChanges();
                tick();
                fixture.detectChanges();

                expect(api.folders.suggestPageFileName).toHaveBeenCalledTimes(1);
                expect(api.folders.suggestPageFileName).toHaveBeenCalledWith({
                    folderId: instance.folderId,
                    nodeId: instance.nodeId,
                    templateId: instance.properties.templateId,
                    language: 'en',
                    pageName: instance.properties.pageName,
                    fileName: 'Außenwirtschaft Oberösterreich',
                });

                expect(instance.onChange).toHaveBeenCalledWith(expectedChanges);
            }),
        );
    });

    describe('niceUrl field', () => {

        const getNiceUrlInput = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('[formcontrolname="niceUrl"]'));

        function setNiceUrlStatus(active: boolean): void {
            featuresActions.checkFeature = jasmine.createSpy('checkFeature')
                .and.returnValue(Promise.resolve(active));
        }

        it('does not display niceUrl if feature is not enabled',
            componentTest(() => TestComponent, (fixture, instance) => {
                setNiceUrlStatus(false);
                fixture.detectChanges();
                tick();
                fixture.detectChanges();

                const niceUrlInput = getNiceUrlInput(fixture);
                expect(niceUrlInput).toBeNull();
            }),
        );

        it('does display niceUrl if feature is enabled',
            componentTest(() => TestComponent, (fixture, instance) => {
                setNiceUrlStatus(true);
                fixture.detectChanges();
                tick();
                fixture.detectChanges();

                const niceUrlInput = getNiceUrlInput(fixture);
                expect(niceUrlInput).not.toBeNull();
            }),
        );

        it('emits changes event when checking the niceUrl FormControl',
            componentTest(() => TestComponent, (fixture, instance) => {
                setNiceUrlStatus(true);
                fixture.detectChanges();
                tick();
                fixture.detectChanges();

                expect(instance.onChange).toHaveBeenCalled();
            }),
        );
    });

    describe('alternateUrls field', () => {

        const getAlternateUrlsDebugger = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('[formcontrolname="alternateUrls"]'));

        function setNiceUrlStatus(active: boolean): void {
            featuresActions.checkFeature = jasmine.createSpy('checkFeature')
                .and.returnValue(Promise.resolve(active));
        }

        it('does not display niceUrl if feature is not enabled',
            componentTest(() => TestComponent, (fixture, instance) => {
                setNiceUrlStatus(false);
                fixture.detectChanges();
                tick();
                fixture.detectChanges();

                const alternateUrlsInput = getAlternateUrlsDebugger(fixture);
                expect(alternateUrlsInput).toBeNull();
            }),
        );

        it('does display niceUrl if feature is enabled',
            componentTest(() => TestComponent, (fixture, instance) => {
                setNiceUrlStatus(true);
                fixture.detectChanges();
                tick();
                fixture.detectChanges();

                const alternateUrlsInput = getAlternateUrlsDebugger(fixture);
                expect(alternateUrlsInput).not.toBeNull();
            }),
        );
    });

    describe('customCdate and customEdate fields', () => {

        const getCustomCdateCell = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('.custom-creation-date'));

        const getCustomEdateCell = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('.custom-edit-date'));

        const getCustomDateCheckbox = (customDateCell: DebugElement): DebugElement => customDateCell.query(By.directive(CheckboxComponent));

        it('displays customCdate and customEdate if a page is set and checks the checkboxes if custom dates are set',
            componentTest(() => TestComponent, (fixture, instance) => {
                fixture.detectChanges();
                tick();
                fixture.detectChanges();

                const customCdateCell = getCustomCdateCell(fixture);
                expect(customCdateCell).not.toBeNull();
                const customCdateCheckbox = getCustomDateCheckbox(customCdateCell);
                expect((customCdateCheckbox.componentInstance as CheckboxComponent).checked).toBeTruthy();

                const customEdateCell = getCustomEdateCell(fixture);
                expect(customEdateCell).not.toBeNull();
                const customEdateCheckbox = getCustomDateCheckbox(customEdateCell);
                expect((customEdateCheckbox.componentInstance as CheckboxComponent).checked).toBeTruthy();
            }),
        );

        it('displays customCdate and customEdate if a page is set and does not check the checkboxes if custom dates are not set',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.properties.customCdate = 0;
                instance.properties.customEdate = 0;
                fixture.detectChanges();
                tick();
                fixture.detectChanges();

                const customCdateCell = getCustomCdateCell(fixture);
                expect(customCdateCell).not.toBeNull();
                const customCdateCheckbox = getCustomDateCheckbox(customCdateCell);
                expect((customCdateCheckbox.componentInstance as CheckboxComponent).checked).toBeFalsy();

                const customEdateCell = getCustomEdateCell(fixture);
                expect(customEdateCell).not.toBeNull();
                const customEdateCheckbox = getCustomDateCheckbox(customEdateCell);
                expect((customEdateCheckbox.componentInstance as CheckboxComponent).checked).toBeFalsy();
            }),
        );

        it('does not display customCdate and customEdate if no page is set',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.page = undefined;
                fixture.detectChanges();
                tick();
                fixture.detectChanges();

                const customCdateCell = getCustomCdateCell(fixture);
                expect(customCdateCell).toBeNull();
                const customEdateCell = getCustomEdateCell(fixture);
                expect(customEdateCell).toBeNull();
            }),
        );

        it('emits changes with custom dates set to 0, when the custom date checkboxes are unchecked',
            componentTest(() => TestComponent, (fixture, instance) => {
                const expectedChanges0: EditablePageProps = {
                    ...instance.properties,
                    customCdate: 0,
                };
                const expectedChanges1: EditablePageProps = {
                    ...expectedChanges0,
                    customEdate: 0,
                };

                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                expect(instance.onChange).toHaveBeenCalled();

                const customCdateCheckbox = getCustomDateCheckbox(getCustomCdateCell(fixture)).query(By.css('input'));
                const customEdateCheckbox = getCustomDateCheckbox(getCustomEdateCell(fixture)).query(By.css('input'));

                customCdateCheckbox.nativeElement.click();
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                expect(instance.onChange).toHaveBeenCalledTimes(2);
                expect(instance.onChange).toHaveBeenCalledWith({ ...expectedChanges0, suggestedOrRequestedFileName: '' });
                instance.onChange.calls.reset();

                customEdateCheckbox.nativeElement.click();
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                expect(instance.onChange).toHaveBeenCalledTimes(1);
                expect(instance.onChange).toHaveBeenCalledWith({ ...expectedChanges1, suggestedOrRequestedFileName: '' });
            }),
        );

        it('emits changes with custom dates set, when the custom date checkboxes are checked',
            componentTest(() => TestComponent, (fixture, instance) => {
                instance.properties.customCdate = 0;
                instance.properties.customEdate = 0;
                const expectedChanges0: EditablePageProps = {
                    ...instance.properties,
                    customCdate: instance.page.cdate,
                };
                const expectedChanges1: EditablePageProps = {
                    ...expectedChanges0,
                    customEdate: instance.page.edate,
                };

                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                expect(instance.onChange).toHaveBeenCalled();

                const customCdateCheckbox = getCustomDateCheckbox(getCustomCdateCell(fixture)).query(By.css('input'));
                const customEdateCheckbox = getCustomDateCheckbox(getCustomEdateCell(fixture)).query(By.css('input'));

                customCdateCheckbox.nativeElement.click();
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                expect(instance.onChange).toHaveBeenCalledTimes(2);
                expect(instance.onChange).toHaveBeenCalledWith({ ...expectedChanges0, suggestedOrRequestedFileName: '' });
                instance.onChange.calls.reset();

                customEdateCheckbox.nativeElement.click();
                fixture.detectChanges();
                tick();
                fixture.detectChanges();
                expect(instance.onChange).toHaveBeenCalledTimes(1);
                expect(instance.onChange).toHaveBeenCalledWith({ ...expectedChanges1, suggestedOrRequestedFileName: '' });
            }),
        );

    });

    describe('template selection', () => {

        const getNoTemplateLinkedWarning = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('[formControlName="templateId"] + .noTemplates'));

        const getLinkTemplateButton = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('[formControlName="templateId"] ~ a'));

        it('warning is shown if no templates are linked',
            componentTest(() => TestComponent, (fixture, instance) => {

                // set no linked templates
                state.mockState(getMockState([]));

                fixture.detectChanges(); // ngOnInit

                tick(); // fetch permissions and linked folders
                fixture.detectChanges();

                const noTemplateLinkedWarning = getNoTemplateLinkedWarning(fixture);
                expect(noTemplateLinkedWarning).not.toBeNull();
            }),
        );

        it('warning is not shown if templates are linked',
            componentTest(() => TestComponent, (fixture, instance) => {

                // set linked templates
                state.mockState(getMockState([testDataTemplate.id]));

                fixture.detectChanges(); // ngOnInit

                tick(); // fetch permissions and linked folders
                fixture.detectChanges();

                const noTemplateLinkedWarning = getNoTemplateLinkedWarning(fixture);
                expect(noTemplateLinkedWarning).toBeNull();
            }),
        );

        it('"link templates" button is shown if user has permissions to link templates and node has templates',
            componentTest(() => TestComponent, (fixture, instance) => {

                // set template for node
                folderActions.getAllTemplatesOfNode = jasmine.createSpy('getAllTemplatesOfNode').and.returnValue(of(
                    [testDataTemplateRaw],
                ));

                // set permission to link
                permissions.forFolder = jasmine.createSpy('forFolder').and.returnValue(of({
                    assignPermissions: null,
                    folder: null,
                    page: null,
                    template: {
                        create: false,
                        delete: false,
                        edit: false,
                        link: true,
                        localize: false,
                        unlocalize: false,
                        view: true,
                    },
                } as any as EditorPermissions));

                fixture.detectChanges(); // ngOnInit

                tick(); // fetch permissions and linked folders
                fixture.detectChanges();

                expect(folderActions.getAllTemplatesOfNode).toHaveBeenCalled();
                expect(permissions.forFolder).toHaveBeenCalled();

                const linkTemplateButton = getLinkTemplateButton(fixture);
                expect(linkTemplateButton).not.toBeNull();
            }),
        );

        it('"link templates" button is not shown if user does not have permissions to link templates yet node has templates',
            componentTest(() => TestComponent, (fixture, instance) => {

                // set template for node
                folderActions.getAllTemplatesOfNode = jasmine.createSpy('getAllTemplatesOfNode').and.returnValue(of(
                    [testDataTemplateRaw],
                ));

                // retract permission to link
                permissions.forFolder = jasmine.createSpy('forFolder').and.returnValue(of({
                    assignPermissions: null,
                    folder: null,
                    page: null,
                    template: {
                        create: false,
                        delete: false,
                        edit: false,
                        link: false,
                        localize: false,
                        unlocalize: false,
                        view: true,
                    },
                } as any as EditorPermissions));

                fixture.detectChanges(); // ngOnInit

                tick(); // fetch permissions and linked folders
                fixture.detectChanges();

                expect(folderActions.getAllTemplatesOfNode).toHaveBeenCalled();
                expect(permissions.forFolder).toHaveBeenCalled();

                const linkTemplateButton = getLinkTemplateButton(fixture);
                expect(linkTemplateButton).toBeNull();
            }),
        );
    });
});

@Component({
    template: `
        <page-properties-form #propertiesForm
                              [page]="page"
                              [enableFileNameSuggestion]="true"
                              [folderId]="folderId"
                              [nodeId]="nodeId"
                              [properties]="properties"
                              [templates]="templates"
                              [languages]="languages"
                              (changes)="onChange($event)"></page-properties-form>
    `,
})
class TestComponent {
    @ViewChild('propertiesForm', { static: true })
    propertiesForm: PagePropertiesForm;

    page = testDataPage;
    folderId = testDataFolderId;
    nodeId = testDataNodeId;
    properties: EditablePageProps = {
        pageName: 'pageName',
        fileName: '',
        description: 'description',
        niceUrl: 'niceUrl',
        alternateUrls: [],
        language: 'en',
        templateId: testDataTemplate.id,
        customCdate: new Date().valueOf() - 10000,
        customEdate: new Date().valueOf() - 100,
        priority: 50,
    };
    templates: Template[] = [testDataTemplate];
    languages: Language[] = [];
    onChange = jasmine.createSpy('onChange').and.stub();
}

class MockApi {
    folders = {
        suggestPageFileName: jasmine.createSpy('suggestPageFileName'),
    };
    permissions = {
        getFolderPermissions: jasmine.createSpy('getFolderPermissions'),
    };
}

class MockErrorHandler {}

class MockFeaturesActions {
    checkFeature(): Promise<boolean> { return Promise.resolve(true); }
}

class MockFolderActions {
    getAllTemplatesOfNode(): Observable<Template[]> {
        return of([]);
    }
}

class MockContextMenuOperationsService {

}

class MockPermissionService {
    forFolder(): Observable<EditorPermissions> {
        return of({
            assignPermissions: null,
            folder: null,
            page: null,
            template: {
                create: true,
                delete: true,
                edit: true,
                link: true,
                localize: true,
                unlocalize: true,
                view: true,
            },
        } as any as EditorPermissions);
    }
}
