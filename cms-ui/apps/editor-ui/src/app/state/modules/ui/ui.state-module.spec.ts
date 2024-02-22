import { TestBed } from '@angular/core/testing';
import { GtxVersion, GtxVersionCompatibility, UsersnapSettings } from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { FALLBACK_LANGUAGE } from '../../../common/config/config';
import { UIMode, UIState } from '../../../common/models';
import { ApplicationStateService } from '../../providers';
import { TestApplicationState } from '../../test-application-state.mock';
import { STATE_MODULES } from '../state-modules';
import { SetCMPVersionAction, SetUILanguageAction, SetUIOverridesAction, SetUIVersionAction, SetUsersnapSettingsAction } from './ui.actions';

const CMP_VERSION: GtxVersion = {
    cmpVersion: '7.8',
    version: '5.38.0',
    nodeInfo: {
        'CMPNode Java 2': {
            meshVersion: '1.5.0',
            portalType: 'Gentics Portal | java',
            portalVersion: '1.2.9',
            compatibility: GtxVersionCompatibility.NOT_SUPPORTED,
        },
        'CMPNode PHP': {
            meshVersion: '1.6.0',
            portalType: 'Gentics Portal | php',
            portalVersion: '1.2.0',
            compatibility: GtxVersionCompatibility.SUPPORTED,
        },
        'CMPNode Java': {
            meshVersion: '1.6.0',
            compatibility: GtxVersionCompatibility.UNKNOWN,
        },
    },
};

describe('UIStateModule', () => {

    let appState: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        appState = TestBed.get(ApplicationStateService);
    });

    it('sets the correct initial state', () => {
        expect(appState.now.ui).toEqual({
            alerts: {},
            contentFrameBreadcrumbsExpanded: false,
            itemListBreadcrumbsExpanded: false,
            repositoryBrowserBreadcrumbsExpanded: false,
            cmpVersion: undefined,
            backendLanguage: FALLBACK_LANGUAGE,
            language: FALLBACK_LANGUAGE,
            availableUiLanguages: [],
            overrides: {},
            overridesReceived: false,
            uiVersion: undefined,
            usersnap: { key: undefined },
            hideExtras: false,
            mode: UIMode.EDIT,
            overlayCount: 0,
            constructFavourites: [],
            tagEditorOpen: false,
        });
    });

    it('setCmsVersion works', () => {
        appState.dispatch(new SetCMPVersionAction(CMP_VERSION));
        expect(appState.now.ui.cmpVersion).toBe(CMP_VERSION);
    });

    it('setUiLanguage works', () => {
        appState.dispatch(new SetUILanguageAction('de'));
        expect(appState.now.ui.language).toBe('de');

        appState.dispatch(new SetUILanguageAction('en'));
        expect(appState.now.ui.language).toBe('en');
    });

    it('setUiOverrides works', () => {
        appState.dispatch(new SetUIOverridesAction({
            newFileButton: {
                disable: true,
            },
        }));
        expect(appState.now.ui.overrides).toEqual({
            newFileButton: {
                disable: true,
            },
        });
        expect(appState.now.ui.overridesReceived).toBe(true);
    });

    it('setUiVersion works', () => {
        const version = '1.6.2-d34db33';
        appState.dispatch(new SetUIVersionAction(version));
        expect(appState.now.ui.uiVersion).toBe(version);
    });

    it('setUsersnapSettings works', () => {
        const settings: UsersnapSettings = { key: 'test' };
        appState.dispatch(new SetUsersnapSettingsAction(settings));
        expect(appState.now.ui.usersnap).toEqual(settings);
    });

});
