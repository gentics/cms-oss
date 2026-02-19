import { Component, DebugElement, NO_ERRORS_SCHEMA, ViewChild } from '@angular/core';
import { ComponentFixture, flush, TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { EditorPermissions } from '@editor-ui/app/common/models';
import { CoreModule } from '@gentics/cms-components';
import { EditablePageProps, Feature, Language, PermissionListResponse, ResponseCode, SuggestPageFileNameResponse, Template } from '@gentics/cms-models';
import {
    getExampleFolderDataNormalized,
    getExamplePageData,
    getExampleTemplateData,
    getExampleTemplateDataNormalized,
} from '@gentics/cms-models/testing/test-data.mock';
import { GCMSRestClientModule, GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { mockPipes } from '@gentics/ui-core/testing';
import { Observable, of } from 'rxjs';
import { componentTest } from '../../../../testing/component-test';
import { configureComponentTest } from '../../../../testing/configure-component-test';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { ApplicationStateService, FolderActionsService, SetFeatureAction } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { PagePropertiesComponent } from './page-properties.component';

const testDataNodeId = 4;
const testDataFolderId = 1;
const testDataFolder = { ...getExampleFolderDataNormalized(), id: 123, nodeId: testDataNodeId, folderId: testDataFolderId };
const testDataPage = { ...getExamplePageData(), id: 123, nodeId: testDataNodeId, folderId: testDataFolderId };
const testDataTemplate = { ...getExampleTemplateDataNormalized(), id: 123, nodeId: testDataNodeId, folderId: testDataFolderId };
const testDataTemplateRaw = { ...getExampleTemplateData(), id: 123, nodeId: testDataNodeId, folderId: testDataFolderId };

function suggestNameResponse(name: string): SuggestPageFileNameResponse {
    return {
        fileName: name,
        messages: [],
        responseInfo: {
            responseCode: ResponseCode.OK,
            responseMessage: '',
        },
    };
}

function templateListResponse(templates: Template[]): PermissionListResponse<Template> {
    return {
        hasMoreItems: false,
        items: templates,
        numItems: templates.length,
        messages: [],
        responseInfo: {
            responseCode: ResponseCode.OK,
            responseMessage: '',
        },
    };
}

describe('PagePropertiesComponent', () => {

    let permissions: PermissionService;
    let state: TestApplicationState;
    let client: GCMSRestClientService;

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

    function setNiceUrlStatus(active: boolean): void {
        state.dispatch(new SetFeatureAction(Feature.NICE_URLS, active));
    }

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
                FormsModule,
                ReactiveFormsModule,
                CoreModule,
                GCMSRestClientModule,
            ],
            providers: [
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                EntityResolver,
                { provide: ContextMenuOperationsService, useClass: MockContextMenuOperationsService },
                { provide: PermissionService, useClass: MockPermissionService },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
            ],
            declarations: [
                TestComponent,
                PagePropertiesComponent,
                ...mockPipes('i18nDate'),
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });

        permissions = TestBed.inject(PermissionService);
        state = TestBed.inject(ApplicationStateService) as any;
        client = TestBed.inject(GCMSRestClientService);

        spyOn(client.page, 'suggestFileName').and.callFake(() => of(suggestNameResponse('')));
        spyOn(client.permission, 'getInstance').and.callThrough();
        spyOn(client.node, 'listTemplates').and.callFake(() => of(templateListResponse([testDataTemplateRaw])));

        state.mockState(getMockState());
    });

    describe('niceUrl field', () => {

        const getNiceUrlInput = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('[formcontrolname="niceUrl"]'));

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
                fixture.whenRenderingDone();
                flush();

                const niceUrlInput = getNiceUrlInput(fixture);
                expect(niceUrlInput).not.toBeNull();
            }),
        );

        it('emits changes event when checking the niceUrl FormControl',
            componentTest(() => TestComponent, (fixture, instance) => {
                setNiceUrlStatus(true);
                fixture.detectChanges();
                tick(600);
                fixture.detectChanges();

                expect(instance.onChange).toHaveBeenCalled();
            }),
        );
    });

    describe('alternateUrls field', () => {

        const getAlternateUrlsDebugger = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('[formcontrolname="alternateUrls"]'));

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

    describe('template selection', () => {

        const getNoTemplateLinkedWarning = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('.template-select ~ .no-templates'));

        const getLinkTemplateButton = (fixture: ComponentFixture<TestComponent>): DebugElement | null =>
            fixture.debugElement.query(By.css('.template-select ~ .link-templates'));

        it('should show a warning when no templates are linked',
            componentTest(() => TestComponent, (fixture, instance) => {
                fixture.detectChanges(); // ngOnInit

                instance.templates = [];

                tick(); // fetch permissions and linked folders
                fixture.detectChanges();

                const noTemplateLinkedWarning = getNoTemplateLinkedWarning(fixture);
                expect(noTemplateLinkedWarning).not.toBeNull();
            }),
        );

        it('warning is not shown if templates are linked',
            componentTest(() => TestComponent, (fixture, instance) => {

                fixture.detectChanges(); // ngOnInit

                tick(); // fetch permissions and linked folders
                fixture.detectChanges();

                const noTemplateLinkedWarning = getNoTemplateLinkedWarning(fixture);
                expect(noTemplateLinkedWarning).toBeNull();
            }),
        );

        it('"link templates" button is shown if user has permissions to link templates and node has templates',
            componentTest(() => TestComponent, (fixture, instance) => {

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

                expect(client.node.listTemplates).toHaveBeenCalled();
                expect(permissions.forFolder).toHaveBeenCalled();

                const linkTemplateButton = getLinkTemplateButton(fixture);
                expect(linkTemplateButton).not.toBeNull();
            }),
        );

        it('"link templates" button is not shown if user does not have permissions to link templates yet node has templates',
            componentTest(() => TestComponent, (fixture, instance) => {

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

                expect(client.node.listTemplates).toHaveBeenCalled();
                expect(permissions.forFolder).toHaveBeenCalled();

                const linkTemplateButton = getLinkTemplateButton(fixture);
                expect(linkTemplateButton).toBeNull();
            }),
        );
    });
});

@Component({
    template: `
        <gtx-page-properties
            #propertiesForm
            [page]="page"
            [folderId]="folderId"
            [nodeId]="nodeId"
            [value]="properties"
            [templates]="templates"
            [languages]="languages"
            (valueChange)="onChange($event)"
        ></gtx-page-properties>
    `,
    standalone: false,
})
class TestComponent {
    @ViewChild('propertiesForm', { static: true })
    propertiesForm: PagePropertiesComponent;

    page = testDataPage;
    folderId = testDataFolderId;
    nodeId = testDataNodeId;
    properties: EditablePageProps = {
        name: 'pageName',
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

class MockErrorHandler {}

class MockFolderActions {}

class MockContextMenuOperationsService {}

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
