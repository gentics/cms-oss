/**
 * **Important:**
 * All configuration set by the user must be saved in this branch of the application state
 */

import { Injectable } from '@angular/core';
import { GcmsUiLanguage } from '@gentics/cms-integration-api-models';
import { EntityIdType, NormalizableEntityType, Update, UsersnapSettings, Version } from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import { StateOperator, iif, patch } from '@ngxs/store/operators';
import { FALLBACK_LANGUAGE } from '../../common/config/config';
import { AppStateService } from '../providers/app-state/app-state.service';
import { ActionDefinition, AppStateBranch, defineInitialState } from '../utils/state-utils';
import {
    CloseEditor,
    DisableFocusMode,
    EnableFocusMode,
    FocusEditor,
    FocusList,
    OpenEditor,
    SetBackendLanguage,
    SetCmpVersion,
    SetCmsUpdates,
    SetUIFocusEntity,
    SetUILanguage,
    SetUISettings,
    SetUIVersion,
    SetUserSettingAction,
    SetUsersnapSettings,
    SwitchEditorTab,
} from './ui.actions';

export interface UIStateModel {
    backendLanguage: GcmsUiLanguage;
    language: GcmsUiLanguage;
    cmsUpdates: Update[];
    cmpVersion: Version;
    uiVersion: string;
    editorIsFocused?: boolean;
    editorIsOpen?: boolean;
    editorTab?: string;
    focusMode?: boolean;
    focusEntityId?: EntityIdType;
    focusEntityType?: NormalizableEntityType;
    focusEntityNodeId?: number;
    settings: UIUserStateSettings;
    usersnap: UsersnapSettings;
}

export interface UIUserStateSettings {
    [userId: number]: UIStateSettings
}

export interface UIStateSettings {
    uiLanguage?: GcmsUiLanguage;
    pollContentMaintenance?: boolean;
}

export const INITIAL_USER_SETTINGS: UIStateSettings = {
    uiLanguage: FALLBACK_LANGUAGE,
    pollContentMaintenance: false,
};

export const INITIAL_UI_STATE = defineInitialState<UIStateModel>({
    backendLanguage: FALLBACK_LANGUAGE,
    language: FALLBACK_LANGUAGE,
    cmsUpdates: [],
    cmpVersion: undefined,
    uiVersion: undefined,
    editorIsFocused: false,
    editorIsOpen: false,
    editorTab: undefined,
    focusMode: false,
    focusEntityId: undefined,
    focusEntityType: undefined,
    settings: {},
    usersnap: {
        key: undefined,
    },
});

@AppStateBranch({
    name: 'ui',
    defaults: INITIAL_UI_STATE,
})
@Injectable()
export class UIStateModule {

    constructor(private appState: AppStateService) {}

    private patchUserSettings(ctx: StateContext<UIStateModel>, diff: Partial<UIStateSettings>): StateOperator<UIUserStateSettings> {
        const auth = this.appState.now.auth;
        const state = ctx.getState();

        return iif<UIUserStateSettings>(auth.isLoggedIn, iif(state.settings?.[auth.currentUserId] != null,
            patch({
                [auth.currentUserId]: patch(diff),
            }),
            {
                [auth.currentUserId]: diff,
            },
        ));
    }

    @ActionDefinition(SetCmpVersion)
    setCmpVersion(ctx: StateContext<UIStateModel>, action: SetCmpVersion): void {
        ctx.setState(patch({
            cmpVersion: action.version,
        }));
    }

    @ActionDefinition(SetUIVersion)
    setUiVersion(ctx: StateContext<UIStateModel>, action: SetUIVersion): void {
        ctx.setState(patch({
            uiVersion: action.version,
        }));
    }

    @ActionDefinition(SetCmsUpdates)
    setCmsUpdates(ctx: StateContext<UIStateModel>, action: SetCmsUpdates): void {
        ctx.setState(patch({
            cmsUpdates: action.available,
        }));
    }

    @ActionDefinition(SetBackendLanguage)
    setBackendLanguage(ctx: StateContext<UIStateModel>, action: SetBackendLanguage): void {
        const state = ctx.getState();

        ctx.setState(patch({
            backendLanguage: action.backendLanguage || state.backendLanguage || FALLBACK_LANGUAGE,
        }));
    }

    @ActionDefinition(SetUILanguage)
    setUiLanguage(ctx: StateContext<UIStateModel>, action: SetUILanguage): void {
        const state = ctx.getState();

        ctx.setState(patch({
            language: action.language || state.language || FALLBACK_LANGUAGE,
            settings: this.patchUserSettings(ctx, {
                uiLanguage: action.language,
            }),
        }));
    }

    @ActionDefinition(SetUISettings)
    setUISettings(ctx: StateContext<UIStateModel>, action: SetUISettings): void {
        ctx.setState(patch({
            settings: this.patchUserSettings(ctx, action.settings),
        }));
    }

    @ActionDefinition(FocusEditor)
    focusEditor(ctx: StateContext<UIStateModel>): void {
        ctx.setState(patch({
            editorIsFocused: true,
        }));
    }

    @ActionDefinition(OpenEditor)
    openEditor(ctx: StateContext<UIStateModel>): void {
        ctx.setState(patch({
            editorIsOpen: true,
            editorIsFocused: true,
        }));
    }

    @ActionDefinition(CloseEditor)
    closeEditor(ctx: StateContext<UIStateModel>): void {
        ctx.setState(patch({
            editorIsOpen: false,
            editorIsFocused: false,
            editorTab: undefined,
        }));
    }

    @ActionDefinition(FocusList)
    focusList(ctx: StateContext<UIStateModel>): void {
        ctx.setState(patch({
            editorIsFocused: false,
        }));
    }

    @ActionDefinition(EnableFocusMode)
    enableFocusMode(ctx: StateContext<UIStateModel>): void {
        ctx.setState(patch({
            focusMode: true,
        }));
    }

    @ActionDefinition(DisableFocusMode)
    disableFocusMode(ctx: StateContext<UIStateModel>): void {
        ctx.setState(patch({
            focusMode: false,
        }));
    }

    @ActionDefinition(SetUIFocusEntity)
    setUIFocusedEntity(ctx: StateContext<UIStateModel>, action: SetUIFocusEntity): void {
        ctx.setState(patch({
            focusEntityType: action.focusEntityType as any,
            focusEntityId: action.focusEntityId,
            focusEntityNodeId: action.focusEntityNodeId || null,
        }));
    }

    @ActionDefinition(SetUsersnapSettings)
    setUsersnapSettings(ctx: StateContext<UIStateModel>, action: SetUsersnapSettings): void {
        ctx.setState(patch({
            usersnap: action.settings,
        }));
    }

    @ActionDefinition(SwitchEditorTab)
    switchEditorTab(ctx: StateContext<UIStateModel>, action: SwitchEditorTab): void {
        ctx.setState(patch({
            editorTab: action.tabId,
        }));
    }

    @ActionDefinition(SetUserSettingAction)
    handleSetUserSettingAction<T extends keyof UIStateSettings>(ctx: StateContext<UIStateModel>, action: SetUserSettingAction<T>): void {
        const auth = this.appState.now.auth;

        if (!auth.isLoggedIn || !auth.currentUserId) {
            return;
        }

        ctx.setState(patch({
            settings: patch<UIUserStateSettings>({
                [auth.currentUserId]: patch({
                    [action.setting]: action.value,
                }),
            }),
        }));
    }
}
