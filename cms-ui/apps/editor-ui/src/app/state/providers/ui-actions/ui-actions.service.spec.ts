import { TestBed } from '@angular/core/testing';
import { I18nService } from '@gentics/cms-components';
import { MockI18nService } from '@gentics/cms-components/testing';
import {
    ResponseCode,
    UsersnapSettings,
    UsersnapSettingsResponse,
    Variant,
    Version,
    VersionCompatibility,
    VersionResponse,
} from '@gentics/cms-models';
import { NgxsModule } from '@ngxs/store';
import { of } from 'rxjs';
import { ApplicationStateService } from '..';
import { MockApiBase } from '../../../../testing';
import { Api } from '../../../core/providers/api';
import { STATE_MODULES } from '../../modules';
import { TestApplicationState } from '../../test-application-state.mock';
import { UIActionsService } from './ui-actions.service';

const CMP_VERSION: Version = {
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

const usersnapSettings: UsersnapSettings = { key: 'test' };

class MockApi extends MockApiBase {
    i18n = {
        setActiveUiLanguage: jasmine.createSpy('setActiveUiLanguage').and.callFake(() => of({})),
    };

    admin = {
        getVersion: jasmine.createSpy('getVersion').and.callFake(() => of<VersionResponse>({
            ...CMP_VERSION,
            responseInfo: {
                responseCode: ResponseCode.OK,
            },
        })),

        getUsersnapSettings: jasmine.createSpy('getUsersnapSettings').and.callFake(() => of<UsersnapSettingsResponse>({
            responseInfo: {
                responseCode: ResponseCode.OK,
            },
            settings: usersnapSettings,
        })),
    };
}

describe('UiActionsService', () => {
    let uiActions: UIActionsService;
    let state: TestApplicationState;
    let apiBase: MockApi;
    let i18n: MockI18nService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: I18nService, useClass: MockI18nService },
                { provide: Api, useClass: MockApi },
                UIActionsService,
            ],
        });
        state = TestBed.inject(ApplicationStateService) as any;
        i18n = TestBed.inject(I18nService) as any;
        spyOn(i18n, 'setLanguage');
        apiBase = TestBed.inject(Api) as any;
        uiActions = TestBed.inject(UIActionsService);
    });

    describe('setActiveUiLanguageInFrontend()', () => {

        it('forwards the language change to the app state', () => {
            uiActions.setActiveUiLanguageInFrontend('de');
            expect(state.now.ui.language).toEqual('de');
        });

        it('changes the language of the i18n service', () => {
            uiActions.setActiveUiLanguageInFrontend('en');
            expect(i18n.setLanguage).toHaveBeenCalledWith('en');
        });

        it('propagates the language change to the backend', () => {
            uiActions.setActiveUiLanguageInBackend('de');
            expect(apiBase.i18n.setActiveUiLanguage).toHaveBeenCalledWith({ code: 'de' });
        });

    });

    describe('getCmsVersion()', () => {

        it('requests the CMS version from the API', () => {
            uiActions.getCmsVersion();
            expect(apiBase.admin.getVersion).toHaveBeenCalled();
        });

        it('propagates the returned version to the app state', () => {
            uiActions.getCmsVersion();
            expect(state.now.ui.cmpVersion).toEqual(CMP_VERSION);
        });

    });

    it('getUsersnapSettings() fetches the Usersnap settings and dispatches them to the app state', () => {
        uiActions.getUsersnapSettings();
        expect(state.now.ui.usersnap).toEqual(usersnapSettings);
    });

});
