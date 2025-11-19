import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NavigationEnd, Router } from '@angular/router';
import { I18nService, KeycloakService, WindowRef } from '@gentics/cms-components';
import { MockI18nService } from '@gentics/cms-components/testing';
import { EmbeddedTool } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { GCMSTestRestClientService } from '@gentics/cms-rest-client-angular/testing';
import { ModalService } from '@gentics/ui-core';
import { NgxsModule } from '@ngxs/store';
import { Subject } from 'rxjs';
import { Api, ApiBase } from '../../../core/providers/api';
import { MockApiBase } from '../../../core/providers/api/api-base.mock';
import { ApplicationStateService, STATE_MODULES } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { ToolApiChannelService } from '../tool-api-channel/tool-api-channel.service';
import { EmbeddedToolsService } from './embedded-tools.service';

describe('EmbeddedToolsService', () => {
    let api: Api;
    let router: MockRouter;
    let service: EmbeddedToolsService;
    let state: TestApplicationState;
    let testTool: EmbeddedTool;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: ApiBase, useClass: MockApiBase },
                { provide: ModalService, useClass: MockModalService },
                { provide: Router, useClass: MockRouter },
                { provide: I18nService, useClass: MockI18nService },
                { provide: ToolApiChannelService, useClass: MockApiChannelService },
                { provide: WindowRef, useClass: MockWindowRef },
                { provide: GCMSRestClientService, useClass: GCMSTestRestClientService },
                KeycloakService,
                Api,
                EmbeddedToolsService,
            ],
        });

        state = TestBed.inject(ApplicationStateService) as any;
        api = TestBed.inject(Api);
        spyOn(api.admin, 'getAvailableEmbeddedTools').and.callThrough();

        testTool = {
            id: 1,
            key: 'testtool',
            name: 'tool name',
            newtab: false,
            iconUrl: '',
            toolUrl: 'tool-url',
        };

        router = TestBed.inject(Router) as any;
        service = TestBed.inject(EmbeddedToolsService);
    });

    function pretendUserWasLoggedIn(): void {
        state.mockState({
            auth: {
                currentUserId: 1,
            },
        });
    }

    describe('loadAvailableToolsWhenLoggedIn', () => {

        it('does not send any requests or update the tools state until a user is logged in', () => {
            service.loadAvailableToolsWhenLoggedIn();

            expect(api.admin.getAvailableEmbeddedTools).not.toHaveBeenCalled();
            expect(state.now.tools.visible).toBeUndefined();
            expect(state.now.tools.received).toEqual(false);
        });

        it('loads the tools from the server when a user is logged in', () => {
            service.loadAvailableToolsWhenLoggedIn();
            pretendUserWasLoggedIn();
            expect(api.admin.getAvailableEmbeddedTools).toHaveBeenCalled();
        });

        it('updates the state on success', fakeAsync(() => {
            const responseSubject = new Subject<{ tools: EmbeddedTool[] }>();
            api.admin.getAvailableEmbeddedTools = (() => responseSubject as any);

            service.loadAvailableToolsWhenLoggedIn();

            expect(state.now.tools.fetching).toBe(false);
            expect(state.now.tools.received).toBe(false);

            pretendUserWasLoggedIn();
            responseSubject.next({ tools: [testTool] });
            tick();

            expect(state.now.tools.available).toEqual([testTool]);
            expect(state.now.tools.received).toEqual(true);
        }));

        it('updates the state on error', () => {
            const responseSubject = new Subject<{ tools: any }>();
            api.admin.getAvailableEmbeddedTools = (() => responseSubject as any);

            service.loadAvailableToolsWhenLoggedIn();
            pretendUserWasLoggedIn();

            expect(state.now.tools.fetching).toEqual(true);
            expect(state.now.tools.received).toEqual(false);

            responseSubject.error({ message: 'some error' });

            expect(state.now.tools.fetching).toEqual(false);
        });

    });

    describe('updateStateWhenRouteChanges', () => {

        it('updates the state when navigating to a tool', () => {
            state.mockState({
                tools: {
                    available: [testTool],
                    active: [],
                    received: true,
                },
            });
            service.updateStateWhenRouteChanges();

            router.mockNavigation('/tools/' + testTool.key);

            expect(state.now.tools.active).toEqual([testTool.key]);
        });

        it('hides any active tool when navigating away from tools', () => {
            state.mockState({
                tools: {
                    available: [testTool],
                    active: [testTool.key],
                    received: true,
                    visible: testTool.key,
                },
            });
            service.updateStateWhenRouteChanges();

            router.mockNavigation('/editor');

            expect(state.now.tools.visible).toEqual(undefined);
        });

        it('navigates inside an active tool', () => {
            state.mockState({
                tools: {
                    available: [testTool],
                    active: [testTool.key],
                    received: true,
                    subpath: { },
                    visible: testTool.key,
                },
            });
            const pathChange = 'some-path';

            router.mockNavigation(`/tools/${testTool.key}`);
            service.updateStateWhenRouteChanges();
            router.mockNavigation(`/tools/${testTool.key}/${pathChange}`);

            expect(state.now.tools.subpath[testTool.key]).toEqual(pathChange);
        });

        it('shows the overview when navigating to it', () => {
            service.updateStateWhenRouteChanges();
            router.mockNavigation('/tools');
            expect(state.now.tools.visible).toEqual(undefined);
        });

    });

});

class MockRouter {
    events = new Subject<NavigationEnd>();
    url = '';
    navigateByUrl = jasmine.createSpy('navigateByUrl');

    mockNavigation(url: string): void {
        this.url = url;
        this.events.next(new NavigationEnd(1, url, url));
    }
}

class MockModalService {
    closeLastDialog: (result: any) => void;

    constructor() {
        spyOn(this as any, 'dialog').and.callThrough();
    }

    dialog(config: any): Promise<any> {
        return Promise.resolve({
            open: () => new Promise<any>(resolve => {
                this.closeLastDialog = resolve;
            }),
        });
    }
}

class MockApiChannelService {
    registerToolsService(): void { }
    connect(toolKey: string, window: Window): void { }
    getApi(): void { }
}

class MockWindowRef {
    lastOpenedWindow: MockTabWindow;
    nativeWindow = {
        open: jasmine.createSpy('open')
            .and.callFake(() => this.lastOpenedWindow = new MockTabWindow()),
    };
}

class MockTabWindow {
    close = jasmine.createSpy('close');
    focus = jasmine.createSpy('focus');
    location: { href: string; };
}
