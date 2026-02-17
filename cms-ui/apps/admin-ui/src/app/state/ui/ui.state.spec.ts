import { fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { AuthenticationModule } from '@gentics/cms-components/auth';
import { Update, UsersnapSettings, Variant, VersionCompatibility } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { AppStateService } from '../providers/app-state/app-state.service';
import { TEST_APP_STATE, TestAppState } from '../utils/test-app-state';
import {
    CloseEditor,
    DisableFocusMode,
    EnableFocusMode,
    FocusEditor,
    FocusList,
    OpenEditor,
    SetCmpVersion,
    SetCmsUpdates,
    SetUIFocusEntity,
    SetUILanguage,
    SetUIVersion,
    SetUsersnapSettings,
    SwitchEditorTab,
} from './ui.actions';
import {
    INITIAL_UI_STATE,
    UIStateModel,
    UIStateModule,
} from './ui.state';

const CMP_VERSION = {
    cmpVersion: '7.8',
    version: '5.38.0',
    variant: Variant.OPEN_SOURCE,
    nodeInfo: {
        'CMPNode Java 2': {
            meshVersion: '1.5.0',
            portalType: 'Gentics Portal | java',
            portalVersion: '1.2.9',
            compatibility: VersionCompatibility.NOT_SUPPORTED,
        },
        'CMPNode PHP': {
            meshVersion: '1.6.0',
            portalType: 'Gentics Portal | php',
            portalVersion: '1.2.0',
            compatibility: VersionCompatibility.SUPPORTED,
        },
        'CMPNode Java': {
            meshVersion: '1.6.0',
            compatibility: VersionCompatibility.UNKNOWN,
        },
    },
};

const UI_VERSION = '4.7.1.1';
const UPDATES: Update[] = [
    {
        version: '5.99.9',
        changelogUrl: null,
    }, {
        version: '5.98.1',
        changelogUrl: '',
    },
];

describe('UiStateModule', () => {

    let appState: TestAppState;

    beforeEach(waitForAsync(() => {
        TestBed.configureTestingModule({
            imports: [
                NgxsModule.forRoot([UIStateModule]),
                AuthenticationModule.forRoot(),
            ],
            providers: [TEST_APP_STATE],
        }).compileComponents();
        appState = TestBed.get(AppStateService);
    }));

    it('sets the correct initial state', () => {
        appState.selectOnce(state => state.ui).subscribe(uiState => {
            expect(uiState).toEqual(INITIAL_UI_STATE);
        });
    });

    it('SetCmsVersion works', () => {
        appState.dispatch(new SetCmpVersion(CMP_VERSION));
        expect(appState.snapshot().ui).toEqual(jasmine.objectContaining<UIStateModel>({
            cmpVersion: CMP_VERSION,
        }));
    });

    it('SetCmsUpdates works', () => {
        appState.dispatch(new SetCmsUpdates(UPDATES));
        expect(appState.snapshot().ui).toEqual(jasmine.objectContaining<UIStateModel>({
            cmsUpdates: UPDATES,
        }));
    });

    it('SetUIVersion works', () => {
        appState.dispatch(new SetUIVersion(UI_VERSION));
        expect(appState.snapshot().ui).toEqual(jasmine.objectContaining<UIStateModel>({
            uiVersion: UI_VERSION,
        }));
    });

    it('SetUILanguage works when logged out', fakeAsync(() => {
        appState.mockState({
            auth: {
                isLoggedIn: false,
            },
        });

        let actionApplied = false;
        appState.dispatch(new SetUILanguage('de')).toPromise().then(() => {
            expect(appState.snapshot().ui).toEqual(jasmine.objectContaining<UIStateModel>({
                language: 'de',
            }));
            expect(appState.snapshot().ui.settings).toEqual({});
            actionApplied = true;
        });

        tick();
        expect(actionApplied).toBe(true);
    }));

    it('SetUILanguage works when logged in',  fakeAsync(() => {
        appState.mockState({
            auth: {
                isLoggedIn: true,
                user: {
                    id: 5,
                },
            },
        });

        let actionApplied = false;
        appState.dispatch(new SetUILanguage('de')).toPromise().then(() => {
            expect(appState.snapshot().ui).toEqual(jasmine.objectContaining<UIStateModel>({
                language: 'de',
                settings: {
                    5: {
                        uiLanguage: 'de',
                    },
                },
            }));
            actionApplied = true;
        });

        tick();
        expect(actionApplied).toBe(true);
    }));

    it('FocusEditor works', fakeAsync(() => {
        appState.dispatch(new FocusEditor());
        expect(appState.now.ui).toEqual({
            ...INITIAL_UI_STATE,
            editorIsFocused: true,
        });
    }));

    it('OpenEditor works', fakeAsync(() => {
        appState.dispatch(new OpenEditor());
        expect(appState.now.ui).toEqual({
            ...INITIAL_UI_STATE,
            editorIsOpen: true,
            editorIsFocused: true,
        });
    }));

    it('CloseEditor works', fakeAsync(() => {
        appState.mockState({
            ui: {
                editorIsOpen: true,
                editorIsFocused: true,
                editorTab: 'properties',
            },
        });
        appState.dispatch(new CloseEditor());
        expect(appState.now.ui).toEqual({
            ...INITIAL_UI_STATE,
            editorIsOpen: false,
            editorIsFocused: false,
            editorTab: undefined,
        });
    }));

    it('FocusList works', fakeAsync(() => {
        appState.mockState({
            ui: {
                editorIsOpen: true,
                editorIsFocused: true,
            },
        });
        appState.dispatch(new FocusList());
        expect(appState.now.ui).toEqual({
            ...INITIAL_UI_STATE,
            editorIsOpen: true,
            editorIsFocused: false,
        });
    }));

    it('EnableFocusMode works', fakeAsync(() => {
        appState.dispatch(new EnableFocusMode());
        expect(appState.now.ui).toEqual({
            ...INITIAL_UI_STATE,
            focusMode: true,
        });
    }));

    it('DisableFocusMode works', fakeAsync(() => {
        appState.dispatch(new DisableFocusMode());
        expect(appState.now.ui).toEqual({
            ...INITIAL_UI_STATE,
            focusMode: false,
        });
    }));

    describe('SetUIFocusEntity works', () => {
        it('should work without node-id', fakeAsync(() => {
            appState.dispatch(new SetUIFocusEntity('group', 4711));
            expect(appState.now.ui).toEqual({
                ...INITIAL_UI_STATE,
                focusEntityType: 'group',
                focusEntityId: 4711,
                focusEntityNodeId: null,
            });
        }));

        it('should work with node-id', fakeAsync(() => {
            appState.dispatch(new SetUIFocusEntity('template', 1337, 1));
            expect(appState.now.ui).toEqual({
                ...INITIAL_UI_STATE,
                focusEntityType: 'template',
                focusEntityId: 1337,
                focusEntityNodeId: 1,
            });
        }));
    });

    it('setUsersnapSettings works', fakeAsync(() => {
        const settings: UsersnapSettings = { key: 'test' };
        appState.dispatch(new SetUsersnapSettings(settings));
        expect(appState.now.ui).toEqual({
            ...INITIAL_UI_STATE,
            usersnap: settings,
        });
    }));

    it('SwitchEditorTab works', fakeAsync(() => {
        appState.mockState({
            ui: {
                editorIsOpen: true,
                editorIsFocused: true,
            },
        });

        appState.dispatch(new SwitchEditorTab('properties'));
        expect(appState.now.ui).toEqual({
            ...INITIAL_UI_STATE,
            editorIsOpen: true,
            editorIsFocused: true,
            editorTab: 'properties',
        });
    }));

});
