import { Injectable } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { AuthStateModel } from '@gentics/cms-components/auth';
import { isEqual } from 'lodash-es';
import { combineLatest } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, map, pairwise, startWith, switchMap, takeUntil } from 'rxjs/operators';
import { objectDiff } from '../../../common';
import { InitializableServiceBase } from '../../../shared/providers/initializable-service-base/initializable-service.base';
import { AppStateService } from '../../../state/providers/app-state/app-state.service';
import { SetBackendLanguage, SetUILanguage, SetUISettings } from '../../../state/ui/ui.actions';
import { INITIAL_USER_SETTINGS, UIStateModel, UIStateSettings } from '../../../state/ui/ui.state';
import { EditorUiLocalStorageService } from '../editor-ui-local-storage/editor-ui-local-storage.service';
import { LanguageHandlerService } from '../language-handler/language-handler.service';
import { ServerStorageService } from '../server-storage';

export type UserSettingName = keyof UIStateSettings;
const USER_SETTING_NAMES = Object.keys(INITIAL_USER_SETTINGS) as UserSettingName[];

export const UI_SETTINGS_DEBOUNCE_MS = 50;
export const ADMIN_UI_SETTINGS_PREFIX = 'admin_';
const SETTINGS_WITHOUT_PREFIX: UserSettingName[] = ['uiLanguage'];

const SERVER_SETTING_NAMES = USER_SETTING_NAMES.map((key) => SETTINGS_WITHOUT_PREFIX.includes(key) ? key : `${ADMIN_UI_SETTINGS_PREFIX}${key}`);

@Injectable()
export class UserSettingsService extends InitializableServiceBase {

    /**
     * Whenever this is true, any changes coming from the app state should not be
     * persisted to the CMS, because we are currently loading settings from there.
     *
     * Should be true until settings are loaded first.
     */
    private loading = true;

    constructor(
        private appState: AppStateService,
        private editorLocalStorage: EditorUiLocalStorageService,
        private i18n: I18nService,
        private serverStorage: ServerStorageService,
        private languageHandler: LanguageHandlerService,
    ) {
        super();
    }

    protected onServiceInit(): void {
        this.initUiLanguage();
        this.loadUserSettingsOnLogin();
        this.saveSettingsOnChange();
    }

    private initUiLanguage(): void {
        let uiLang = this.editorLocalStorage.getUiLanguage();
        if (!uiLang) {
            uiLang = this.i18n.inferUserLanguage();
        }
        this.appState.dispatch(new SetUILanguage(uiLang));
        this.appState.dispatch(new SetBackendLanguage(uiLang));

        this.appState.select((state) => state.ui).pipe(
            distinctUntilChanged((a: UIStateModel, b: UIStateModel) => a.language === b.language),
            map((ui) => ui.language),
            takeUntil(this.stopper.stopper$),
        ).subscribe((language) => {
            this.i18n.setLanguage(language);
            this.editorLocalStorage.setUiLanguage(language);
        });
    }

    /**
     * Loads the user's settings when he logs in.
     */
    private loadUserSettingsOnLogin(): void {
        this.appState.select((state) => state.auth).pipe(
            // Only trigger when logged in state changed...
            distinctUntilChanged((a: AuthStateModel, b: AuthStateModel) => a.isLoggedIn === b.isLoggedIn && a.sid === b.sid),
            // ... and only go further if user is logged in
            filter((auth) => auth.isLoggedIn === true && !!auth.sid),
            // Then get all keys
            switchMap(() => this.serverStorage.getAll()),
            takeUntil(this.stopper.stopper$),
        ).subscribe((data) => {
            if (this.serverStorage.supported !== false) {
                this.loading = true;

                const settings = Object.keys(data)
                    .filter((key) => SERVER_SETTING_NAMES.includes(key))
                    .reduce((r, k) => ({ ...r, [this.convertFromServerKey(k)]: data[k] }), {});

                this.appState.dispatch(new SetUISettings(settings))
                    .toPromise()
                    .then(() => {
                        this.loading = false;
                    });
            }

            /**
             * UI language used to be hardcoded and is now available via `i18n`endpoint.
             * This method fetches all available and current active UI language and stores it to state and localstorage.
             * In case fetching fails, fallback language logic should be in place by localstorage and browser language.
             */
            this.languageHandler.getActiveBackendLanguage().subscribe((language) => {
                this.appState.dispatch(new SetUILanguage(language));
                this.appState.dispatch(new SetBackendLanguage(language));
            });
        });
    }

    /**
     * Subscribes to all settings changes in the app state and saves changes to the server.
     */
    private saveSettingsOnChange(): void {
        combineLatest([this.appState.select((state) => state.ui), this.appState.select((state) => state.auth)]).pipe(
            distinctUntilChanged(([uiA, authA], [uiB, authB]) => uiA.language === uiB.language
              && isEqual(uiA.settings[authA.user?.id], uiB.settings[authB.user?.id]),
            ),
            filter(([, auth]) => auth.isLoggedIn && !this.loading),
            debounceTime(50),
            startWith([this.appState.now.ui]),
            pairwise(),
            takeUntil(this.stopper.stopper$),
        ).subscribe(([[uiPrev], [ui, auth]]) => {
            const currentUserId = (auth as AuthStateModel).user?.id;
            // Only store the settings if server storage is supported, the user is still logged in,
            // and the currentUserId is still the same as the one before the debounce.
            if (
                this.serverStorage.supported !== false
                && this.appState.now.auth.isLoggedIn
                && this.appState.now.auth.user?.id === currentUserId
            ) {
                const previousSettings = uiPrev.settings[currentUserId] || {};
                const currentSettings = ui.settings[currentUserId];
                const changedSettings: UIStateSettings = objectDiff(currentSettings, previousSettings);

                for (const setting in changedSettings) {

                    if (changedSettings.hasOwnProperty(setting)) {
                        const keyOnServer = this.convertToServerKey(setting);
                        this.serverStorage.set(keyOnServer, changedSettings[setting]);
                    }
                }
            }
        });
    }

    /**
     * Adds prefix to setting key which not excluded
     * @param key Setting key
     */
    private convertToServerKey(key: string): string {
        return SETTINGS_WITHOUT_PREFIX.includes(key as UserSettingName) ? key : `${ADMIN_UI_SETTINGS_PREFIX}${key}`;
    }

    /**
     * Removes prefix from setting key which not excluded
     * @param key Setting key
     */
    private convertFromServerKey(key: string): string {
        return key.startsWith(ADMIN_UI_SETTINGS_PREFIX) ? key.substring(ADMIN_UI_SETTINGS_PREFIX.length) : key;
    }
}
