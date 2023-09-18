import { Component } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CoreModule } from '@gentics/cms-components';
import { EditorPermissions, Page, Raw, Template } from '@gentics/cms-models';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { Observable, of } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { mockPipes } from '../../../../testing/mock-pipe';
import { Api } from '../../../core/providers/api';
import { ContextMenuOperationsService } from '../../../core/providers/context-menu-operations/context-menu-operations.service';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { PagePropertiesForm } from '../../../shared/components';
import { DynamicDisableDirective } from '../../../shared/directives';
import { ApplicationStateService, FeaturesActionsService, FolderActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { CreatePageModalComponent } from './create-page-modal.component';

xdescribe('CreatePageModal', () => {

    let state: TestApplicationState;
    let folderActions: MockFolderActions;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                FormsModule,
                GenticsUICoreModule.forRoot(),
                ReactiveFormsModule,
                CoreModule,
            ],
            providers: [
                { provide: Api, useClass: MockApi },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: FeaturesActionsService, useClass: MockFeaturesActions },
                { provide: ContextMenuOperationsService, useClass: MockContextMenuOperationsService },
                { provide: PermissionService, useClass: MockPermissionService },
                EntityResolver,
            ],
            declarations: [
                CreatePageModalComponent,
                DynamicDisableDirective,
                PagePropertiesForm,
                TestComponent,
                mockPipes('i18nDate'),
            ],
        });

        state = TestBed.get(ApplicationStateService);
        expect(state instanceof TestApplicationState).toBe(true);

        folderActions = TestBed.get(FolderActionsService);
        expect(folderActions instanceof MockFolderActions).toBe(true);
    });

    it('uses the template the user selected',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            state.mockState({
                folder: {
                    ...state.now.folder,
                    activeFolder: 123,
                    activeNode: 456,
                    activeNodeLanguages: {
                        list: [],
                    },
                    folders: {
                        creating: false,
                    },
                    templates: {
                        list: [1, 4, 22, 23],
                    },
                },
                entities: {
                    language: {},
                    template: {
                        1: {
                            name: 'Contentpage',
                            type: 'template',
                            id: 1,
                        },
                        4: {
                            name: 'Wikipage',
                            type: 'template',
                            id: 4,
                        },
                        22: {
                            name: 'Empty template',
                            type: 'template',
                            id: 22,
                        },
                        23: {
                            name: 'Simple page',
                            type: 'template',
                            id: 23,
                        },
                    },
                },
            });

            testComponent.showModal = true;
            fixture.detectChanges();
            tick();

            const dropdownTrigger = fixture.nativeElement.querySelector('gtx-dropdown-trigger') as HTMLElement;
            dropdownTrigger.click();
            fixture.detectChanges();
            tick(1000);

            const dropdownOptions: HTMLElement[] = Array.from(document.querySelectorAll('gtx-dropdown-content .select-option')) ;
            expect(dropdownOptions.length).toBe(4);
            expect(dropdownOptions.map(option => option.innerText.trim())).toEqual([
                'Contentpage',
                'Wikipage',
                'Empty template',
                'Simple page',
            ]);

            // Select "Simple page"
            dropdownOptions[3].click();
            fixture.detectChanges();
            tick();

            // The display value should now be updated
            const displayValue: HTMLElement = fixture.nativeElement.querySelector('gtx-dropdown-list gtx-dropdown-trigger .view-value div');
            expect(displayValue.innerText).toBe('Simple page');

            // Pages also need a name
            const nameInput: HTMLInputElement = fixture.nativeElement.querySelector('gtx-input[formcontrolname="pageName"] input');
            expect(nameInput).toBeDefined();
            nameInput.value = 'A new test page';
            const inputEvent = document.createEvent('Event');
            inputEvent.initEvent('input', true, false);
            nameInput.dispatchEvent(inputEvent);
            fixture.detectChanges();
            tick();

            const okayButton: HTMLButtonElement = fixture.nativeElement.querySelector('.modal-footer gtx-button:not([flat]) button');
            expect(okayButton).toBeDefined();
            okayButton.click();
            tick();

            expect(folderActions.createNewPage).toHaveBeenCalledWith({
                pageName: 'A new test page',
                fileName: '',
                description: '',
                priority: 1,
                folderId: 123,
                niceUrl: '',
                alternateUrls: [],
                nodeId: 456,
                language: null,
                templateId: 23,
            });
        }),
    );

});

@Component({
    template: `
        <create-page-modal *ngIf="showModal"></create-page-modal>
        <gtx-overlay-host></gtx-overlay-host>
    `,
})
class TestComponent {
    showModal = false;
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

class MockFolderActions implements Partial<FolderActionsService> {
    constructor() {
        spyOn(this as any, 'createNewPage').and.callThrough();
    }
    createNewPage(page: {
        pageName: string;
        fileName: string;
        description: string;
        language: string;
        priority: number;
        templateId: number;
        folderId: number;
        nodeId: number;
        niceUrl: string;
        alternateUrls?: string[];
    }): Promise<Page<Raw> | void> {
        return Promise.resolve(page as any);
    }
    getAllTemplatesOfNode(): Observable<Template[]> {
        return of([]);
    }
}

class MockFeaturesActions {
    // tslint:disable-next-line:typedef
    checkFeature() { return Promise.resolve(true); }
}

class MockContextMenuOperationsService {

}

class MockPermissionService {
    forFolder(): Observable<EditorPermissions> {
        return of({
            assignPermissions: null,
            folder: null,
            page: null,
            templates: {
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
