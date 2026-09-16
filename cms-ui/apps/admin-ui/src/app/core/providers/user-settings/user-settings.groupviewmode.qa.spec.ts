/*
 * QA spec: persistence round trip of the new `groupViewMode` user setting.
 *
 * Developer acceptance criterion 1 ("`admin_groupViewMode` is persisted and restored, list stays
 * the default") was reported as "verified by code reading only". This spec exercises the actual
 * wiring - `SetUserSettingAction` -> app state -> `UserSettingsService` -> `ServerStorageService`
 * and back - with the real ngxs state and the real service, only the HTTP layer is mocked.
 *
 * Brief mapping: developer AC 1, security T8 (round trip) and T9 (per user isolation).
 */
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { I18nService } from '@gentics/cms-components';
import { INITIAL_AUTH_STATE } from '@gentics/cms-components/auth';
import { MockI18nService } from '@gentics/cms-components/testing';
import { ActionType, ofActionDispatched } from '@ngxs/store';
import { of } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { createDelayedObservable } from '../../../../testing';
import { InterfaceOf, ObservableStopper } from '../../../common';
import { AppStateService } from '../../../state';
import { SetUISettings, SetUserSettingAction } from '../../../state/ui/ui.actions';
import { GROUP_VIEW_MODE_LIST, GROUP_VIEW_MODE_TREE, INITIAL_USER_SETTINGS } from '../../../state/ui/ui.state';
import { TestAppState, assembleTestAppStateImports } from '../../../state/utils/test-app-state';
import { EditorUiLocalStorageService } from '../editor-ui-local-storage';
import { LanguageHandlerService } from '../language-handler/language-handler.service';
import { ServerStorageService } from '../server-storage';
import { UI_SETTINGS_DEBOUNCE_MS, UserSettingsService } from './user-settings.service';

const USER_ID = 2;
const OTHER_USER_ID = 3;
const SERVER_KEY = 'admin_groupViewMode';

class MockEditorLocalStorage implements Partial<InterfaceOf<EditorUiLocalStorageService>> {
    getUiLanguage = jasmine.createSpy('getUiLanguage').and.returnValue('en');
    setUiLanguage = jasmine.createSpy('setUiLanguage').and.stub();
}

class MockServerStorageService implements Partial<InterfaceOf<ServerStorageService>> {
    getAll = jasmine.createSpy('getAll').and.returnValue(createDelayedObservable({}));
    set = jasmine.createSpy('set').and.callFake(() => Promise.resolve());
}

class MockLanguageHandlerService implements Partial<InterfaceOf<LanguageHandlerService>> {
    getActiveBackendLanguage = jasmine.createSpy('getActiveBackendLanguage').and.returnValue(of('de'));
}

describe('UserSettingsService - groupViewMode (QA)', () => {

    let appState: TestAppState;
    let userSettings: UserSettingsService;
    let serverStorage: MockServerStorageService;
    let stopper: ObservableStopper;
    let dispatchedSettings: SetUISettings[];

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                UserSettingsService,
                { provide: AppStateService, useClass: TestAppState },
                { provide: EditorUiLocalStorageService, useClass: MockEditorLocalStorage },
                { provide: ServerStorageService, useClass: MockServerStorageService },
                { provide: LanguageHandlerService, useClass: MockLanguageHandlerService },
                { provide: I18nService, useClass: MockI18nService },
            ],
        });

        appState = TestBed.inject(AppStateService) as any;
        userSettings = TestBed.inject(UserSettingsService);
        serverStorage = TestBed.inject(ServerStorageService) as any;
        stopper = new ObservableStopper();
        dispatchedSettings = [];

        appState.trackActions().pipe(
            ofActionDispatched(SetUISettings as ActionType),
            takeUntil(stopper.stopper$),
        ).subscribe((action) => dispatchedSettings.push(action));
    });

    afterEach(() => {
        stopper.stop();
    });

    function login(userId: number = USER_ID): void {
        appState.mockState({
            auth: {
                isLoggedIn: true,
                user: { id: userId },
                sid: userId + 1,
            },
        });
        tick();
        tick(UI_SETTINGS_DEBOUNCE_MS);
    }

    it('ships "list" as the default, so the flat list stays the default view', () => {
        expect(INITIAL_USER_SETTINGS.groupViewMode).toBe(GROUP_VIEW_MODE_LIST);
    });

    it('persists a switch to the tree view under the prefixed server key admin_groupViewMode', fakeAsync(() => {
        userSettings.init();
        login();
        serverStorage.set.calls.reset();

        appState.dispatch(new SetUserSettingAction('groupViewMode', GROUP_VIEW_MODE_TREE));
        tick(UI_SETTINGS_DEBOUNCE_MS);
        tick();

        // Exactly one write for this key (the unrelated `uiLanguage` write of the login is filtered out).
        expect(serverStorage.set.calls.allArgs().filter((args) => args[0] === SERVER_KEY))
            .toEqual([[SERVER_KEY, GROUP_VIEW_MODE_TREE]]);
        expect(appState.now.ui.settings[USER_ID].groupViewMode).toBe(GROUP_VIEW_MODE_TREE);
    }));

    it('persists the switch back to the list explicitly, it does not just drop the key', fakeAsync(() => {
        userSettings.init();
        login();

        appState.dispatch(new SetUserSettingAction('groupViewMode', GROUP_VIEW_MODE_TREE));
        tick(UI_SETTINGS_DEBOUNCE_MS);
        tick();
        serverStorage.set.calls.reset();

        appState.dispatch(new SetUserSettingAction('groupViewMode', GROUP_VIEW_MODE_LIST));
        tick(UI_SETTINGS_DEBOUNCE_MS);
        tick();

        expect(serverStorage.set).toHaveBeenCalledWith(SERVER_KEY, GROUP_VIEW_MODE_LIST);
    }));

    it('restores the stored value on the next login and strips the admin_ prefix', fakeAsync(() => {
        serverStorage.getAll.and.returnValue(createDelayedObservable({ [SERVER_KEY]: GROUP_VIEW_MODE_TREE }));

        userSettings.init();
        login();

        expect(dispatchedSettings.length).toBe(1);
        expect(dispatchedSettings[0].settings).toEqual({ groupViewMode: GROUP_VIEW_MODE_TREE });
        expect(appState.now.ui.settings[USER_ID].groupViewMode).toBe(GROUP_VIEW_MODE_TREE);
    }));

    it('ignores an unprefixed groupViewMode key coming from the server', fakeAsync(() => {
        serverStorage.getAll.and.returnValue(createDelayedObservable({ groupViewMode: GROUP_VIEW_MODE_TREE }));

        userSettings.init();
        login();

        expect(dispatchedSettings[0].settings).toEqual({});
    }));

    it('passes a tampered value through to the state unvalidated - the component has to defend (security T8)', fakeAsync(() => {
        serverStorage.getAll.and.returnValue(createDelayedObservable({ [SERVER_KEY]: 'treeX' }));

        userSettings.init();
        login();

        // Documents *why* the F6 validation in GroupMasterComponent is needed: the settings
        // service stores whatever the server returned.
        expect(appState.now.ui.settings[USER_ID].groupViewMode as any).toBe('treeX');
    }));

    it('keeps the setting of two users apart (security T9)', fakeAsync(() => {
        serverStorage.getAll.and.returnValue(createDelayedObservable({ [SERVER_KEY]: GROUP_VIEW_MODE_TREE }));
        userSettings.init();
        login(USER_ID);

        appState.mockState({ auth: INITIAL_AUTH_STATE });
        tick();

        serverStorage.getAll.and.returnValue(createDelayedObservable({}));
        login(OTHER_USER_ID);

        // Note: ngxs' `patchUserSettings` replaces the whole map when the new user has no entry
        // yet (`ui.state.ts:103-116`), so the first user's entry is dropped. Pre-existing
        // behaviour, and it errs on the safe side for this criterion.
        // What T9 is about: the second user must not inherit the first user's view mode.
        expect(appState.now.ui.settings[OTHER_USER_ID]?.groupViewMode).toBeUndefined();
    }));
});
