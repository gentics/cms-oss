/* eslint-disable id-blacklist */
import { Component, NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed, tick } from '@angular/core/testing';
import { Router } from '@angular/router';
import { I18nNotificationService } from '@gentics/cms-components';
import { Page, PageResponse, ResponseCode } from '@gentics/cms-models';
import { GenticsUICoreModule, ModalService } from '@gentics/ui-core';
import { of } from 'rxjs';
import { delay } from 'rxjs/operators';
import { componentTest, configureComponentTest } from '../../../../testing';
import { Api } from '../../../core/providers/api/api.service';
import { NavigationService } from '../../../core/providers/navigation/navigation.service';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { PageVersionsModal } from './page-versions-modal.component';

describe('PageVersionsModal', () => {
    let modalService: ModalService;
    let api: MockApi;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule.forRoot(),
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: Api, useClass: MockApi },
                { provide: Router, useClass: MockRouter },
                { provide: NavigationService, useClass: MockNavigationService },
                { provide: FolderActionsService, useClass: MockFolderActions },
                { provide: I18nNotificationService, useClass: MockI18Notification },
            ],
            declarations: [
                PageVersionsModal,
                TestComponent,
            ],
            schemas: [NO_ERRORS_SCHEMA],
        });

        api = TestBed.inject(Api) as any;
        modalService = TestBed.inject(ModalService);
    });

    it('is opened with right parameters', componentTest(() => TestComponent, async (fixture, instance) => {
        api.pageSample = instance.page;

        return modalService.fromComponent(PageVersionsModal, {}, { page: instance.page as any, nodeId: instance.nodeId })
            .then(modal => {
                modal.open();

                fixture.detectChanges();
                tick();

                expect(fixture.nativeElement.querySelector('page-versions-modal')).toBeTruthy();
            });
    }),
    );

    it('shows the right label if page version is current', componentTest(() => TestComponent, async (fixture, instance) => {
        const currentPage: Partial<Page> = {
            type: 'page',
            fileName: 'pageName',
            description: 'description',
            languageVariants: {},
            versions: [
                {
                    number: '1.0',
                    timestamp: 1561452894,
                    editor: {
                        email: 'test@test.com',
                        firstName: 'Node',
                        id: 3,
                        lastName: 'Admin',
                    },
                },
                {
                    number: '0.1',
                    timestamp: 1561452512,
                    editor: {
                        email: 'test@test.com',
                        firstName: 'Node',
                        id: 3,
                        lastName: 'Admin',
                    },
                },
            ],
            currentVersion: {
                number: '1.0',
                timestamp: 1561452894,
                editor: {
                    email: 'test@test.com',
                    firstName: 'Node',
                    id: 3,
                    lastName: 'Admin',
                },
            },
        };

        api.pageSample = currentPage;

        return modalService.fromComponent(PageVersionsModal, {}, { page: currentPage as any, nodeId: instance.nodeId })
            .then(modal => {
                modal.open();

                fixture.detectChanges();
                tick();

                fixture.detectChanges();
                tick();

                expect(fixture.nativeElement.querySelectorAll('gtx-contents-list-item')[0]
                    .querySelector('.item-indicators span').className).toContain('current');
            });
    }),
    );

    it('shows the right label if page version is published', componentTest(() => TestComponent, async (fixture, instance) => {
        const publishedPage: Partial<Page> = {
            type: 'page',
            fileName: 'pageName',
            description: 'description',
            languageVariants: {},
            versions: [
                {
                    number: '1.1',
                    timestamp: 1561452894,
                    editor: {
                        email: 'test@test.com',
                        firstName: 'Node',
                        id: 3,
                        lastName: 'Admin',
                    },
                },
                {
                    number: '1.0',
                    timestamp: 1561452512,
                    editor: {
                        email: 'test@test.com',
                        firstName: 'Node',
                        id: 3,
                        lastName: 'Admin',
                    },
                },
            ],
            publishedVersion: {
                number: '1.0',
                timestamp: 1561452512,
                editor: {
                    email: 'test@test.com',
                    firstName: 'Node',
                    id: 3,
                    lastName: 'Admin',
                },
            },
        };

        api.pageSample = publishedPage;

        return modalService.fromComponent(PageVersionsModal, {}, { page: publishedPage as any, nodeId: instance.nodeId })
            .then(modal => {
                modal.open();

                fixture.detectChanges();
                tick();

                fixture.detectChanges();
                tick();

                expect(fixture.nativeElement.querySelectorAll('gtx-contents-list-item')[1]
                    .querySelector('.item-indicators span').className).toContain('published');
            });
    }),
    );

    it('shows the right label if page version is planned to be published', componentTest(() => TestComponent, async (fixture, instance) => {
        const plannedPage: Partial<Page> = {
            type: 'page',
            fileName: 'pageName',
            description: 'description',
            languageVariants: {},
            planned: true,
            versions: [
                {
                    number: '1.0',
                    timestamp: 1561452894,
                    editor: {
                        email: 'test@test.com',
                        firstName: 'Node',
                        id: 3,
                        lastName: 'Admin',
                    },
                },
                {
                    number: '0.1',
                    timestamp: 1561452512,
                    editor: {
                        email: 'test@test.com',
                        firstName: 'Node',
                        id: 3,
                        lastName: 'Admin',
                    },
                },
            ],
            timeManagement: {
                at: 1561453569,
                offlineAt: 0,
                version: {
                    number: '1.0',
                    timestamp: 1561452894,
                    editor: {
                        email: 'test@test.com',
                        firstName: 'Node',
                        id: 3,
                        lastName: 'Admin',
                    },
                },
            },
        };

        api.pageSample = plannedPage;

        return modalService.fromComponent(PageVersionsModal, {}, { page: plannedPage as any, nodeId: instance.nodeId })
            .then(modal => {
                modal.open();

                fixture.detectChanges();
                tick();

                fixture.detectChanges();
                tick();

                expect(fixture.nativeElement.querySelectorAll('gtx-contents-list-item')[0]
                    .querySelector('.item-indicators span').className).toContain('planned_online');
            });
    }),
    );

    it('shows the right label if page version is planned to be taken offline', componentTest(() => TestComponent, async (fixture, instance) => {
        const plannedPage: Partial<Page> = {
            type: 'page',
            fileName: 'pageName',
            description: 'description',
            languageVariants: {},
            planned: true,
            versions: [
                {
                    number: '1.0',
                    timestamp: 1561452894,
                    editor: {
                        email: 'test@test.com',
                        firstName: 'Node',
                        id: 3,
                        lastName: 'Admin',
                    },
                },
                {
                    number: '0.1',
                    timestamp: 1561452512,
                    editor: {
                        email: 'test@test.com',
                        firstName: 'Node',
                        id: 3,
                        lastName: 'Admin',
                    },
                },
            ],
            timeManagement: {
                at: 0,
                offlineAt: 1561453569,
            },
            currentVersion: {
                number: '1.0',
                timestamp: 1561452894,
                editor: {
                    email: 'test@test.com',
                    firstName: 'Node',
                    id: 3,
                    lastName: 'Admin',
                },
            },
        };

        api.pageSample = plannedPage;

        return modalService.fromComponent(PageVersionsModal, {}, { page: plannedPage as any, nodeId: instance.nodeId })
            .then(modal => {
                modal.open();

                fixture.detectChanges();
                tick();

                fixture.detectChanges();
                tick();

                expect(fixture.nativeElement.querySelectorAll('gtx-contents-list-item')[0]
                    .querySelector('.item-indicators span').className).toContain('planned_offline');
            });
    }),
    );

    it('shows the right label if page version is planned to be published and to be taken offline, but should be published first',
        componentTest(() => TestComponent, async (fixture, instance) => {
            const plannedPage: Partial<Page> = {
                type: 'page',
                fileName: 'pageName',
                description: 'description',
                languageVariants: {},
                planned: true,
                versions: [
                    {
                        number: '1.0',
                        timestamp: 1561452894,
                        editor: {
                            email: 'test@test.com',
                            firstName: 'Node',
                            id: 3,
                            lastName: 'Admin',
                        },
                    },
                    {
                        number: '0.1',
                        timestamp: 1561452512,
                        editor: {
                            email: 'test@test.com',
                            firstName: 'Node',
                            id: 3,
                            lastName: 'Admin',
                        },
                    },
                ],
                timeManagement: {
                    at: 1561453569,
                    offlineAt: 1561453765,
                    version: {
                        number: '1.0',
                        timestamp: 1561452894,
                        editor: {
                            email: 'test@test.com',
                            firstName: 'Node',
                            id: 3,
                            lastName: 'Admin',
                        },
                    },
                },
                currentVersion: {
                    number: '1.0',
                    timestamp: 1561452894,
                    editor: {
                        email: 'test@test.com',
                        firstName: 'Node',
                        id: 3,
                        lastName: 'Admin',
                    },
                },
            };

            api.pageSample = plannedPage;

            return modalService.fromComponent(PageVersionsModal, {}, { page: plannedPage as any, nodeId: instance.nodeId })
                .then(modal => {
                    modal.open();

                    fixture.detectChanges();
                    tick();

                    fixture.detectChanges();
                    tick();

                    expect(fixture.nativeElement.querySelectorAll('gtx-contents-list-item')[0]
                        .querySelector('.item-indicators span').className).toContain('planned_online');
                });
        }),
    );

    it('shows the right label if page version is planned to be published and to be taken offline, but should be taken offline first',
        componentTest(() => TestComponent, async (fixture, instance) => {
            const plannedPage: Partial<Page> = {
                type: 'page',
                fileName: 'pageName',
                description: 'description',
                languageVariants: {},
                planned: true,
                versions: [
                    {
                        number: '1.0',
                        timestamp: 1561452894,
                        editor: {
                            email: 'test@test.com',
                            firstName: 'Node',
                            id: 3,
                            lastName: 'Admin',
                        },
                    },
                    {
                        number: '0.1',
                        timestamp: 1561452512,
                        editor: {
                            email: 'test@test.com',
                            firstName: 'Node',
                            id: 3,
                            lastName: 'Admin',
                        },
                    },
                ],
                timeManagement: {
                    at: 1561453897,
                    offlineAt: 1561453765,
                    version: {
                        number: '1.0',
                        timestamp: 1561452894,
                        editor: {
                            email: 'test@test.com',
                            firstName: 'Node',
                            id: 3,
                            lastName: 'Admin',
                        },
                    },
                },
                currentVersion: {
                    number: '1.0',
                    timestamp: 1561452894,
                    editor: {
                        email: 'test@test.com',
                        firstName: 'Node',
                        id: 3,
                        lastName: 'Admin',
                    },
                },
            };

            api.pageSample = plannedPage;

            return modalService.fromComponent(PageVersionsModal, {}, { page: plannedPage as any, nodeId: instance.nodeId })
                .then(modal => {
                    modal.open();

                    fixture.detectChanges();
                    tick();

                    fixture.detectChanges();
                    tick();

                    expect(fixture.nativeElement.querySelectorAll('gtx-contents-list-item')[0]
                        .querySelector('.item-indicators span').className).toContain('planned_offline');
                });
        }),
    );
});

@Component({
    template: '<gtx-overlay-host></gtx-overlay-host>',
    standalone: false,
})
class TestComponent {
    nodeId = 1;
    page: Partial<Page> = {
        type: 'page',
        fileName: 'pageName',
        description: 'description',
        languageVariants: {},
        versions: [
            {
                number: '1.0',
                timestamp: 1561452894,
                editor: {
                    email: 'test@test.com',
                    firstName: 'Node',
                    id: 3,
                    lastName: 'Admin',
                },
            },
            {
                number: '0.1',
                timestamp: 1561452512,
                editor: {
                    email: 'test@test.com',
                    firstName: 'Node',
                    id: 3,
                    lastName: 'Admin',
                },
            },
        ],
    };
}

class MockApi {
    pageSample: Partial<Page>;

    folders = {
        getItem: () => {
            return of({ responseInfo: { responseCode: ResponseCode.OK }, page: this.pageSample } as PageResponse).pipe(delay(0));
        },
    };
}

class MockRouter {}

class MockNavigationService {}

class MockFolderActions {}

class MockI18Notification {}
