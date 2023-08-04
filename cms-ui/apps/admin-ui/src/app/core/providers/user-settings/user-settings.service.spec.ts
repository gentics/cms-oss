import { InterfaceOf, ObservableStopper } from '@admin-ui/common';
import { AppStateService, INITIAL_AUTH_STATE } from '@admin-ui/state';
import { TestAppState, assembleTestAppStateImports } from '@admin-ui/state/utils/test-app-state';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ActionType, ofActionDispatched } from '@ngxs/store';
import { of } from 'rxjs';
import { first, takeUntil } from 'rxjs/operators';
import { createDelayedObservable } from '../../../../testing';
import { SetUILanguage, SetUISettings } from '../../../state/ui/ui.actions';
import { EditorUiLocalStorageService } from '../editor-ui-local-storage';
import { I18nService } from '../i18n';
import { MockI18nServiceWithSpies } from '../i18n/i18n.service.mock';
import { LanguageHandlerService } from '../language-handler/language-handler.service';
import { ServerStorageService } from '../server-storage';
import { UI_SETTINGS_DEBOUNCE_MS, UserSettingsService } from './user-settings.service';

const LOCAL_STORAGE_UI_LANGUAGE = 'en';
const I18N_SERVICE_INFERRED_LANGUAGE = 'de';
const MOCK_SERVER_SETTINGS = {
    uiLanguage: 'en',
};
const USER_ID = 2;

class MockEditorLocalStorage implements Partial<InterfaceOf<EditorUiLocalStorageService>> {
    getUiLanguage = jasmine.createSpy('getUiLanguage').and.returnValue(LOCAL_STORAGE_UI_LANGUAGE);
    setUiLanguage = jasmine.createSpy('setUiLanguage').and.stub();
}

class MockServerStorageService implements Partial<InterfaceOf<ServerStorageService>> {
    getAll = jasmine.createSpy('getAll').and.returnValue(createDelayedObservable(MOCK_SERVER_SETTINGS));
    set = jasmine.createSpy('set').and.callFake(() => Promise.resolve());
}

class MockLanguageHandlerService implements Partial<InterfaceOf<LanguageHandlerService>> {
    getActiveBackendLanguage = jasmine.createSpy('getActiveBackendLanguage').and.returnValue(of('de').pipe(first()));
}

describe('UserSettingsService', () => {

    let appState: TestAppState;
    let userSettings: UserSettingsService;
    let stopper: ObservableStopper;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [
                assembleTestAppStateImports(),
            ],
            providers: [
                UserSettingsService,
                TestAppState,
                { provide: AppStateService, useExisting: TestAppState },
                MockEditorLocalStorage,
                { provide: EditorUiLocalStorageService, useExisting: MockEditorLocalStorage },
                MockI18nServiceWithSpies,
                { provide: I18nService, useExisting: MockI18nServiceWithSpies },
                MockServerStorageService,
                { provide: ServerStorageService, useExisting: MockServerStorageService },
                { provide: LanguageHandlerService, useClass: MockLanguageHandlerService },
            ],
        });

        appState = TestBed.inject(TestAppState);
        stopper = new ObservableStopper();
        userSettings = TestBed.inject(UserSettingsService);
    });

    afterEach(() => {
        stopper.stop();
    });

    describe('UI Language', () => {

        let editorLocalStorage: MockEditorLocalStorage;
        let i18n: MockI18nServiceWithSpies;

        beforeEach(() => {
            editorLocalStorage = TestBed.inject(MockEditorLocalStorage);
            i18n = TestBed.inject(MockI18nServiceWithSpies);
            i18n.inferUserLanguage.and.returnValue(I18N_SERVICE_INFERRED_LANGUAGE);
        });

        it('reads the UI language from the local storage and sets it in the AppState', () => {
            let setUiLanguageDispatched = false;
            appState.trackActions().pipe(
                ofActionDispatched(SetUILanguage as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe((action: SetUILanguage) => {
                expect(action.language).toEqual(LOCAL_STORAGE_UI_LANGUAGE);
                setUiLanguageDispatched = true;
            });

            userSettings.init();

            expect(editorLocalStorage.getUiLanguage).toHaveBeenCalledTimes(1);
            expect(setUiLanguageDispatched).toBe(true);
        });

        it('infers the UI language if the local storage does not contain that setting', () => {
            editorLocalStorage.getUiLanguage.and.returnValue(null);

            let setUiLanguageDispatched = false;
            appState.trackActions().pipe(
                ofActionDispatched(SetUILanguage as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe((action: SetUILanguage) => {
                expect(action.language).toEqual(I18N_SERVICE_INFERRED_LANGUAGE);
                setUiLanguageDispatched = true;
            });

            userSettings.init();

            expect(i18n.inferUserLanguage).toHaveBeenCalledTimes(1);
            expect(setUiLanguageDispatched).toBe(true);
        });

        it('writes the UI language to the local storage when it changes', () => {
            appState.mockState({
                ui: {
                    language: 'en',
                },
            });

            userSettings.init();
            expect(editorLocalStorage.setUiLanguage).toHaveBeenCalledTimes(1);
            expect(editorLocalStorage.setUiLanguage).toHaveBeenCalledWith('en');
            editorLocalStorage.setUiLanguage.calls.reset();

            appState.dispatch(new SetUILanguage('de'));
            expect(editorLocalStorage.setUiLanguage).toHaveBeenCalledTimes(1);
            expect(editorLocalStorage.setUiLanguage).toHaveBeenCalledWith('de');
        });

    });

    describe('ServerStorage', () => {

        let serverStorage: MockServerStorageService;
        let dispatchedActions: SetUISettings[];

        beforeEach(() => {
            serverStorage = TestBed.inject(MockServerStorageService);
            dispatchedActions = [];

            appState.trackActions().pipe(
                ofActionDispatched(SetUISettings as ActionType),
                takeUntil(stopper.stopper$),
            ).subscribe(action => dispatchedActions.push(action));
        });

        function simulateLogin(userId: number, runTick: boolean = true): void {
            appState.mockState({
                auth: {
                    isLoggedIn: true,
                    currentUserId: userId,
                    sid: userId + 1,
                },
            });
            if (runTick) {
                tick();
                tick(UI_SETTINGS_DEBOUNCE_MS);
            }
        }

        it('loads settings from serverStorage when a user logs in', fakeAsync(() => {
            userSettings.init();
            tick();

            // Settings should not be fetched if no user is logged in.
            expect(serverStorage.getAll).not.toHaveBeenCalled();
            expect(dispatchedActions.length).toBe(0);

            simulateLogin(USER_ID);
            expect(serverStorage.getAll).toHaveBeenCalledTimes(1);
            expect(dispatchedActions.length).toBe(1);
            expect(dispatchedActions[0].settings).toEqual(MOCK_SERVER_SETTINGS);
        }));

        it('re-loads all settings from localStorage and serverStorage when a different user logs in', fakeAsync(() => {
            userSettings.init();
            simulateLogin(USER_ID);

            appState.mockState({
                auth: INITIAL_AUTH_STATE,
            });
            tick();

            const userBSettings = { uiLanguage: 'de' };
            serverStorage.getAll.and.returnValue(createDelayedObservable(userBSettings));
            simulateLogin(USER_ID + 1);

            expect(serverStorage.getAll).toHaveBeenCalledTimes(2);
            expect(dispatchedActions.length).toBe(2);
            expect(dispatchedActions[1].settings).toEqual(userBSettings);
        }));

        it('saves a setting to the server when it changes and the user is logged in', fakeAsync(() => {
            userSettings.init();
            simulateLogin(USER_ID);

            appState.dispatch(new SetUISettings({ uiLanguage: 'test' }));
            tick(UI_SETTINGS_DEBOUNCE_MS);
            // Resolve the promise returned by serverStorage.set()
            tick();

            expect(serverStorage.set).toHaveBeenCalledTimes(1);
            // No prefix as it has been exempted
            expect(serverStorage.set).toHaveBeenCalledWith('uiLanguage', 'test');
        }));

        it('uses a debounce time', fakeAsync(() => {
            userSettings.init();
            simulateLogin(USER_ID);

            appState.dispatch(new SetUILanguage('en'));
            expect(serverStorage.set).not.toHaveBeenCalled();

            appState.dispatch(new SetUILanguage('de'));
            expect(serverStorage.set).not.toHaveBeenCalled();

            appState.dispatch(new SetUILanguage('en'));
            tick(UI_SETTINGS_DEBOUNCE_MS - 1);
            expect(serverStorage.set).not.toHaveBeenCalled();

            tick(UI_SETTINGS_DEBOUNCE_MS);
            // Resolve the promise returned by serverStorage.set()
            tick();

            expect(serverStorage.set).toHaveBeenCalledTimes(1);
            expect(serverStorage.set).toHaveBeenCalledWith(`uiLanguage`, 'en');
        }));

        it('does not save settings, while they are being loaded from the server', fakeAsync(() => {
            userSettings.init();
            simulateLogin(USER_ID, false);

            appState.dispatch(new SetUISettings({ uiLanguage: 'test' }));
            tick(UI_SETTINGS_DEBOUNCE_MS);

            expect(serverStorage.set).not.toHaveBeenCalled();
        }));

        it('does not save settings if the user logs out before the debounce time', fakeAsync(() => {
            userSettings.init();
            simulateLogin(USER_ID);

            appState.dispatch(new SetUILanguage('en'));
            expect(serverStorage.set).not.toHaveBeenCalled();

            appState.mockState({
                auth: INITIAL_AUTH_STATE,
            });
            tick();

            tick(UI_SETTINGS_DEBOUNCE_MS);
            expect(serverStorage.set).not.toHaveBeenCalled();
        }));

    });

});
