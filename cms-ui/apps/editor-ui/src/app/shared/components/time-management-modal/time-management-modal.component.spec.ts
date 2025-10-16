/* eslint-disable id-blacklist */
import { ChangeDetectorRef, Component, EventEmitter, NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { SetUILanguageAction } from '@editor-ui/app/state';
import { Form, FormRequestOptions, Normalized, Page, PageRequestOptions } from '@gentics/cms-models';
import { getExampleFormDataNormalized, getExamplePageDataNormalized } from '@gentics/cms-models/testing/test-data.mock';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { LangChangeEvent, TranslateService } from '@ngx-translate/core';
import { NEVER, Observable, of } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';
import { MockErrorHandler } from '../../../core/providers/error-handler/error-handler.mock';
import { ErrorHandler } from '../../../core/providers/error-handler/error-handler.service';
import { I18nService } from '../../../core/providers/i18n/i18n.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { PermissionService } from '../../../core/providers/permissions/permission.service';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { I18nDatePipe } from '../../pipes/i18n-date/i18n-date.pipe';
import { TimeManagementModal } from './time-management-modal.component';

const MOCK_NODEID = 1;

describe('TimeManagementModal', () => {

    let modalService: ModalService;
    let folderActions: MockFolderActions;
    let state: TestApplicationState;
    let translateService: TranslateService;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
            providers: [
                { provide: ChangeDetectorRef, useClass: MockChangeDetector },
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: ErrorHandler, useClass: MockErrorHandler },
                { provide: TranslateService, useClass: MockTranslateService },
                { provide: I18nService, useClass: MockI18nService },
                { provide: PermissionService, useClass: MockPermissionService },
                { provide: NavigationService, useClass: MockNavigationService },
                ModalService,
            ],
            declarations: [
                TimeManagementModal,
                TestComponent,
                I18nDatePipe,
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });

        modalService = TestBed.get(ModalService);
        folderActions = TestBed.get(FolderActionsService);
        state = TestBed.get(ApplicationStateService);
        translateService = TestBed.inject(TranslateService);

        expect(state instanceof ApplicationStateService).toBeTruthy();

        translateService.currentLang = 'de';
    });

    it('is displayed for pages',
        componentTest(() => TestComponent, async (fixture, instance) => {

            // prepare test data
            const pageSample: Page<Normalized> = {
                ...getExamplePageDataNormalized({ id: 1 }),
                online: true,
                modified: false,
                planned: false,
                inherited: false,
                timeManagement: {
                    at: 0,
                    offlineAt: 0,
                },
            };

            // set state accordingly
            state.mockState({
                entities: {
                    page: {
                        1: pageSample,
                    },
                },
            });

            // define page to be returned by backend
            folderActions.pageSample = pageSample;

            state.dispatch(new SetUILanguageAction('en'));
            tick();

            // display modal with prepared page data
            return modalService.fromComponent(TimeManagementModal, {}, { item: pageSample, currentNodeId: MOCK_NODEID })
                .then(modal => {
                    modal.open();

                    fixture.detectChanges();
                    tick();

                    expect(fixture.nativeElement.querySelector('time-management-modal')).toBeTruthy();
                });
        }),
    );

    it('is displayed for forms',
        componentTest(() => TestComponent, async (fixture, instance) => {

            // prepare test data
            const formSample: Form<Normalized> = {
                ...getExampleFormDataNormalized({ id: 1 }),
                online: true,
                modified: false,
                planned: false,
                inherited: false,
                timeManagement: {
                    at: 0,
                    offlineAt: 0,
                },
            };

            // set state accordingly
            state.mockState({
                entities: {
                    form: {
                        1: formSample,
                    },
                },
            });

            state.dispatch(new SetUILanguageAction('en'));
            tick();

            // define page to be returned by backend
            folderActions.formSample = formSample;

            // display modal with prepared page data
            return modalService.fromComponent(TimeManagementModal, {}, { item: formSample, currentNodeId: MOCK_NODEID })
                .then(modal => {
                    modal.open();

                    fixture.detectChanges();
                    tick();

                    expect(fixture.nativeElement.querySelector('time-management-modal')).toBeTruthy();
                });
        }),
    );

    it('displays correct data for pages',
        componentTest(() => TestComponent, async (fixture, instance) => {

            // prepare test data
            const pageSample: Page<Normalized> = {
                ...getExamplePageDataNormalized({ id: 1 }),
                online: true,
                modified: false,
                planned: true,
                inherited: false,
                timeManagement: {
                    // page is planned to be published for v1.0
                    at: 1561901595,
                    offlineAt: 0,
                    version: {
                        number: '1.0',
                        timestamp: 1370335470,
                        editor: {
                            id: 1000,
                            firstName: 'firstNameTEST01',
                            lastName: 'lastNameTEST01',
                            email: 'emailTEST01',
                        },
                    },
                    // a user without publish permission has requested this page to be taken offline
                    queuedOffline: {
                        at: 1561901635,
                        user: {
                            id: 2000,
                            firstName: 'firstNameTEST02',
                            lastName: 'lastNameTEST02',
                            email: 'emailTEST02',
                        },
                    },
                },
                // page has been edited after timemanagement has been set
                // therefore a timemanagement is set for v1.0 but a v1.1 does exist without timemanagement
                versions: [
                    {
                        number: '1.0',
                        timestamp: 1370335470,
                        editor: {
                            id: 1000,
                            firstName: 'firstNameTEST01',
                            lastName: 'lastNameTEST01',
                            email: 'emailTEST01',
                        },
                    },
                    {
                        number: '1.1',
                        timestamp: 1556036153,
                        editor: {
                            id: 1000,
                            firstName: 'firstNameTEST01',
                            lastName: 'lastNameTEST01',
                            email: 'emailTEST01',
                        },
                    },
                ],
            };

            // set state accordingly
            state.mockState({
                entities: {
                    page: {
                        1: pageSample,
                    },
                },
            });

            state.dispatch(new SetUILanguageAction('en'));
            tick();

            // define page to be returned by backend
            folderActions.pageSample = pageSample;

            // display modal with prepared page data
            return modalService.fromComponent(TimeManagementModal, {}, { item: pageSample, currentNodeId: MOCK_NODEID })
                .then(modal => {
                    modal.open();

                    fixture.detectChanges();
                    tick();

                    // check is modal visible
                    expect(fixture.nativeElement.querySelector('time-management-modal')).toBeTruthy();

                    // check modal contents
                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-pagename]')
                            .getAttribute('data-pagename'),
                    ).toEqual('Braintribe Mashup Demo');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-entry="existing-publish-at"]')
                            .getAttribute('data-value'),
                    ).toEqual('1561901595');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-entry="existing-publish-at"]')
                            .getAttribute('data-version'),
                    ).toEqual('1.0');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-entry="existing-offline-at"]')
                            .getAttribute('data-value'),
                    ).toEqual('0');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-entry="queued-offline"]')
                            .getAttribute('data-value'),
                    ).toEqual('1561901635');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-entry="queued-offline"]')
                            .getAttribute('data-first-name'),
                    ).toEqual('firstNameTEST02');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-entry="queued-offline"]')
                            .getAttribute('data-last-name'),
                    ).toEqual('lastNameTEST02');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-control="existing_version"]')
                            .getAttribute('data-value'),
                    ).toEqual('1.0');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-control="current_version"]')
                            .getAttribute('data-value'),
                    ).toEqual('1.1');
                });
        }),
    );

    it('displays correct data for forms',
        componentTest(() => TestComponent, async (fixture, instance) => {

            // prepare test data
            const formSample: Form<Normalized> = {
                ...getExampleFormDataNormalized({ id: 1 }),
                online: true,
                modified: false,
                planned: true,
                inherited: false,
                timeManagement: {
                    // form is planned to be published for v1.0
                    at: 1561901595,
                    offlineAt: 0,
                    version: {
                        number: '1.0',
                        timestamp: 1370335470,
                        editor: {
                            id: 1000,
                            firstName: 'firstNameTEST01',
                            lastName: 'lastNameTEST01',
                            email: 'emailTEST01',
                        },
                    },
                    // a user without publish permission has requested this page to be taken offline
                    queuedOffline: {
                        at: 1561901635,
                        user: {
                            id: 2000,
                            firstName: 'firstNameTEST02',
                            lastName: 'lastNameTEST02',
                            email: 'emailTEST02',
                        },
                    },
                },
                version: {
                    number: '1.1',
                    timestamp: 1556036153,
                    editor: {
                        id: 1000,
                        firstName: 'firstNameTEST01',
                        lastName: 'lastNameTEST01',
                        email: 'emailTEST01',
                    },
                },
            };

            // set state accordingly
            state.mockState({
                entities: {
                    form: {
                        1: formSample,
                    },
                },
            });

            state.dispatch(new SetUILanguageAction('en'));
            tick();

            // define page to be returned by backend
            folderActions.formSample = formSample;

            // display modal with prepared page data
            return modalService.fromComponent(TimeManagementModal, {}, { item: formSample, currentNodeId: MOCK_NODEID })
                .then(modal => {
                    modal.open();

                    fixture.detectChanges();
                    tick();

                    // check is modal visible
                    expect(fixture.nativeElement.querySelector('time-management-modal')).toBeTruthy();

                    // check modal contents
                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-pagename]')
                            .getAttribute('data-pagename'),
                    ).toEqual('Form');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-entry="existing-publish-at"]')
                            .getAttribute('data-value'),
                    ).toEqual('1561901595');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-entry="existing-publish-at"]')
                            .getAttribute('data-version'),
                    ).toEqual('1.0');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-entry="existing-offline-at"]')
                            .getAttribute('data-value'),
                    ).toEqual('0');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-entry="queued-offline"]')
                            .getAttribute('data-value'),
                    ).toEqual('1561901635');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-entry="queued-offline"]')
                            .getAttribute('data-first-name'),
                    ).toEqual('firstNameTEST02');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-entry="queued-offline"]')
                            .getAttribute('data-last-name'),
                    ).toEqual('lastNameTEST02');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-control="existing_version"]')
                            .getAttribute('data-value'),
                    ).toEqual('1.0');

                    expect(
                        fixture.nativeElement
                            .querySelector('time-management-modal [data-control="current_version"]')
                            .getAttribute('data-value'),
                    ).toEqual('1.1');
                });
        }),
    );

});

@Component({
    template: '<gtx-overlay-host></gtx-overlay-host>',
})
class TestComponent {

}

class MockChangeDetector {
    markForCheck(): void { }
    detectChanges(): void { }
}

class MockFolderActions {
    pageSample: Page;
    formSample: Form;

    getPage(pageId: number, options?: PageRequestOptions): Promise<Page> {
        return Promise.resolve(this.pageSample);
    }

    getForm(formId: number, options?: FormRequestOptions): Promise<Form> {
        return Promise.resolve(this.formSample);
    }
}

class MockI18nService {
    transform(): Observable<any> {
        return NEVER;
    }

    translate(key: string | string[], params?: any): string {
        if (Array.isArray(key)) {
            return key.join('-');
        } else {
            return key;
        }
    }
}
class MockTranslateService {
    onLangChange = new EventEmitter<LangChangeEvent>();
    get currentLang(): string {
        return this.lang;
    }
    set currentLang(lang: string) {
        this.onLangChange.emit({
            lang: this.lang = lang,
            translations: {},
        });
    }
    get():  Observable<string | any> { return of(this.currentLang); }
    private lang: string;
}

class MockPermissionService {
    forItemInLanguage(): Observable<any> {
        return NEVER;
    }
    forItem(): Observable<any> {
        return NEVER;
    }
}

class MockNavigationService {
    instruction(instruction: any): any {
        return {
            navigate: (extras: any) => Promise.resolve(true),
        };
    }
}
