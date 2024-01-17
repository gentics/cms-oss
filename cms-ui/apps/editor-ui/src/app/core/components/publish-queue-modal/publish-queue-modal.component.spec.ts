import { Component, Injectable, ViewChild } from '@angular/core';
import { TestBed, fakeAsync, waitForAsync } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { PublishQueueModal } from '@editor-ui/app/core/components/publish-queue-modal/publish-queue-modal.component';
import { Api, FolderApi } from '@editor-ui/app/core/providers/api';
import { EntityResolver } from '@editor-ui/app/core/providers/entity-resolver/entity-resolver';
import { MockErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.mock';
import { ErrorHandler } from '@editor-ui/app/core/providers/error-handler/error-handler.service';
import { NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { ApplicationStateService, FolderActionsService, PublishQueueActionsService } from '@editor-ui/app/state';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { BaseListResponse, Normalized, Page, PageRequestOptions, ResponseCode } from '@gentics/cms-models';
import { getExamplePageData, getExamplePageDataNormalized } from '@gentics/cms-models/testing/test-data.mock';
import { of } from 'rxjs';
import { componentTest, configureComponentTest } from '../../../../testing';

let appState: TestApplicationState;
let testPages: Page<Normalized>[];
let entityAmount: number;

describe('PublishQueueModal is created ok', () => {

    let navigationService: NavigationService;

    @Component({
        template: `
          <publish-queue>
          </publish-queue>`,
    })
    class TestComponent {
        @ViewChild(PublishQueueModal, {static: true}) publishQueueModal: PublishQueueModal;

        navigate(): void {
        }
    }

    beforeEach(waitForAsync(() => {
        configureComponentTest({
            imports: [RouterTestingModule],
            providers: [
                FolderActionsService,
                PublishQueueModal,
                NavigationService,
                {provide: ApplicationStateService, useClass: TestApplicationState},
                {provide: Api, useClass: MockApi},
                {provide: ErrorHandler, useClass: MockErrorHandler},
                {provide: PublishQueueActionsService, useClass: MockPublishQueueActions},
                {provide: FolderActionsService, useClass: MockFolderActions},
                {provide: EntityResolver, useClass: MockEntityResolver},
            ],
            declarations: [
                PublishQueueModal,
                TestComponent,
            ],
        });

        appState = TestBed.get(ApplicationStateService);
        navigationService = TestBed.inject(NavigationService);
    }));

    it('is created ok',
        componentTest(() => TestComponent, (fixture, testComponent) => {
            fixture.detectChanges();
            expect(testComponent.publishQueueModal).toBeDefined();
        }),
    );
})

describe('PublishQueueModal test approve method', () => {
    let folderActions: FolderActionsService;
    let navigationService: NavigationService;

    beforeEach(waitForAsync(() => {
        configureComponentTest({
            imports: [RouterTestingModule],
            providers: [
                FolderActionsService,
                PublishQueueModal,
                NavigationService,
                {provide: ApplicationStateService, useClass: TestApplicationState},
                {provide: Api, useClass: MockApi},
                {provide: ErrorHandler, useClass: MockErrorHandler},
                {provide: FolderActionsService, useClass: MockFolderActions},
                {provide: PublishQueueActionsService, useClass: MockPublishQueueActions},
                {provide: FolderActionsService, useClass: MockFolderActions},
                {provide: EntityResolver, useClass: MockEntityResolver},
            ],
            declarations: [
                PublishQueueModal,
            ],
        });
        appState = TestBed.get(ApplicationStateService);
        folderActions = TestBed.inject(FolderActionsService);
        navigationService = TestBed.inject(NavigationService);
    }));

    it('calls the correct api method with right parameters', componentTest(() => PublishQueueModal, (fixture, testComponent) => {
        const firstPageId = 123;
        const secondPageId = 456;
        const pages: Page[] = [getExamplePageData({id: firstPageId}), getExamplePageData({id: secondPageId})];

        // closeFn isn't bound unless it is opened via the modal-service.
        // therefore register it ourself with a noop function
        testComponent.registerCloseFn(() => {});
        testComponent.selectedPages = pages;
        testComponent.approve();

        expect(folderActions.pageQueuedApprove).toHaveBeenCalledWith(pages);
    }));
});

describe('PublishQueueModal test pageClicked method', () => {

    let api: Api;
    let folderActions: FolderActionsService;
    let navigationService: NavigationService;

    @Injectable()
    class MockApi {
        defaultResponse: BaseListResponse = {
            responseInfo: {
                responseCode: ResponseCode.OK,
                responseMessage: '',
            },
            messages: [],
            hasMoreItems: false,
            numItems: 0,
        };

        folders: Partial<FolderApi>;

        constructor(
            private entityResolver: EntityResolver,
        ) {
            // prepare test data
            generateTestData();

            const pageResponse = {...this.defaultResponse, pages: testPages.map(e => this.entityResolver.denormalizeEntity('page', e))};

            this.folders = {
                getItem: jasmine.createSpy('getItem').and.returnValue(of(pageResponse) as any),
            };
        }

    }

    beforeEach(waitForAsync(() => {
        configureComponentTest({
            imports: [RouterTestingModule],
            providers: [
                FolderActionsService,
                PublishQueueModal,
                NavigationService,
                {provide: ApplicationStateService, useClass: TestApplicationState},
                {provide: Api, useClass: MockApi},
                {provide: ErrorHandler, useClass: MockErrorHandler},
                {provide: FolderActionsService, useClass: MockFolderActions},
                {provide: PublishQueueActionsService, useClass: MockPublishQueueActions},
                {provide: FolderActionsService, useClass: MockFolderActions},
                {provide: EntityResolver, useClass: MockEntityResolver},
            ],
            declarations: [
                PublishQueueModal,
            ],
        });

        appState = TestBed.get(ApplicationStateService);
        api = TestBed.inject(Api);
        folderActions = TestBed.inject(FolderActionsService);
        navigationService = TestBed.inject(NavigationService);
    }));

    it('calls api folders getitem', componentTest(() => PublishQueueModal, (fixture, testComponent) => {
        const firstPageId = 123;
        const page: Page = getExamplePageData({id: firstPageId});
        spyOn(navigationService, 'instruction').and.returnValue({
            navigate: () => Promise.resolve(true),
        } as any);
        testComponent.pageClicked(page);
        expect(api.folders.getItem).toHaveBeenCalled();
    }));

    it('calls api folders getitem with correct params', componentTest(() => PublishQueueModal, (fixture, testComponent) => {
        const firstPageId = 123;
        const page: Page = getExamplePageData({id: firstPageId});
        const options: PageRequestOptions = {
            nodeId: 1,
            versioninfo: true,
        };
        spyOn(navigationService, 'instruction').and.returnValue({
            navigate: () => Promise.resolve(true),
        } as any);
        testComponent.pageClicked(page);
        expect(api.folders.getItem).toHaveBeenCalledWith(page.id, 'page', options);
    }));
});

class MockApi {
}

class MockEntityResolver {
}

class MockFolderActions {
    pageQueuedApprove = jasmine.createSpy('pageQueuedApprove');
}

class MockPublishQueueActions {
    getQueue = jasmine.createSpy('getQueue');
}

function generateTestData(): void {
    // prepare test data
    entityAmount = 5;
    testPages = new Array(entityAmount).map((id: number) => ({...getExamplePageDataNormalized({id}), name: `test-page-${id}`}));
}
