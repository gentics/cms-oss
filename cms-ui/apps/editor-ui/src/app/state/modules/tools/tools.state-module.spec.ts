import { TestBed } from '@angular/core/testing';
import { EmbeddedTool } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { ToolsState } from '../../../common/models/tools-state';
import { ApplicationStateService } from '../../providers';
import { TestApplicationState } from '../../test-application-state.mock';
import { STATE_MODULES } from '../state-modules';
import {
    CloseToolAction,
    HideToolsAction,
    OpenToolAction,
    StartToolsFetchingAction,
    ToolBreadcrumbAction,
    ToolNavigationAction,
    ToolsFetchingErrorAction,
    ToolsFetchingSuccessAction,
} from './tools.actions';

describe('ToolsStateModule', () => {

    let state: TestApplicationState;
    let mockTool: EmbeddedTool;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        state = TestBed.get(ApplicationStateService);

        mockTool = {
            id: 123,
            key: 'my-awesome-tool',
            name: {
                en: 'My awesome tool',
                de: 'Mein tolles Werkzeug',
            },
            iconUrl: '/some-icon.png',
            newtab: false,
            toolUrl: 'http://my-awesome-tool.com/',
        };
    });

    it('sets the correct initial state', () => {
        const expectedInitialState: ToolsState = {
            active: [],
            available: [],
            breadcrumbs: {},
            fetching: false,
            received: false,
            subpath: { },
            visible: undefined,
        };
        expect(state.now.tools).toEqual(expectedInitialState);
    });

    it('fetchToolsStart works', () => {
        expect(state.now.tools.fetching).toBe(false);
        state.dispatch(new StartToolsFetchingAction());
        expect(state.now.tools.fetching).toBe(true);
    });

    it('fetchToolSuccess works', () => {
        state.mockState({
            tools: {
                available: [],
                fetching: true,
                received: false,
            },
        });

        state.dispatch(new ToolsFetchingSuccessAction([mockTool]));

        expect(state.now.tools.fetching).toBe(false);
        expect(state.now.tools.received).toBe(true);
        expect(state.now.tools.available).toEqual([mockTool]);
    });

    it('fetchToolError works', () => {
        state.mockState({
            tools: {
                fetching: true,
            },
        });
        state.dispatch(new ToolsFetchingErrorAction());

        expect(state.now.tools.fetching).toBe(false);
    });

    it('open works', () => {
        state.mockState({
            tools: {
                available: [mockTool],
                visible: undefined,
                active: [],
            },
        });

        const subPath = 'url-in-tool';
        state.dispatch(new OpenToolAction(mockTool.key, subPath));

        expect(state.now.tools.available).toEqual([mockTool], 'available != expected');
        expect(state.now.tools.visible).toEqual(mockTool.key);
        expect(state.now.tools.active).toEqual([mockTool.key], 'running != expected');
        expect(state.now.tools.subpath).toEqual({ [mockTool.key]: subPath });
    });

    it('close works', () => {
        state.mockState({
            tools: {
                available: [mockTool],
                active: [mockTool.key],
            },
        });

        state.dispatch(new CloseToolAction(mockTool.key));

        expect(state.now.tools.active).toEqual([]);
        expect(state.now.tools.available).toEqual([mockTool]);
    });

    it('navigateInTool works', () => {
        state.mockState({
            tools: {
                available: [mockTool],
                active: [mockTool.key],
                visible: mockTool.key,
                subpath: {
                    [mockTool.key]: '',
                },
            },
        });

        const toolPath = 'some/url/in/tool';
        state.dispatch(new ToolNavigationAction(mockTool.key, toolPath));

        expect(state.now.tools.subpath).toEqual({
            [mockTool.key]: toolPath,
        });
    });

    it('hideAll works', () => {
        state.mockState({
            tools: {
                visible: mockTool.key,
            },
        });

        state.dispatch(new HideToolsAction());

        expect(state.now.tools.visible).toBeUndefined();
    });

    it('setBreadcrumbs works', () => {
        state.mockState({
            tools: {
                breadcrumbs: {},
                visible: mockTool.key,
            },
        });

        state.dispatch(new ToolBreadcrumbAction(mockTool.key, [
            { text: 'First breadcrumb', url: '1' },
            { text: 'Second breadcrumb', url: '1/2' },
            { text: 'Third breadcrumb', url: '1/2/3' },
        ]));

        expect(state.now.tools.breadcrumbs).toEqual({
            [mockTool.key]: [
                { text: 'First breadcrumb', url: '1' },
                { text: 'Second breadcrumb', url: '1/2' },
                { text: 'Third breadcrumb', url: '1/2/3' },
            ],
        });
    });

});
