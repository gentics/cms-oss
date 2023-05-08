import { Injectable } from '@angular/core';
import { StateContext } from '@ngxs/store';
import { patch } from '@ngxs/store/operators';
import { FALLBACK_LANGUAGE } from '../../../common/config/config';
import { Alert, Alerts, UIMode, UIState } from '../../../common/models';
import { ActionDefinition, AppStateBranch } from '../../state-utils';
import {
    BreadcrumbLocation,
    SetAvailableUILanguageAction,
    SetBackendLanguageAction,
    SetBreadcrumbExpandedAction,
    SetBrokenLinksCountAction,
    SetCMPVersionAction,
    SetHideExtrasAction,
    SetUILanguageAction,
    SetUIModeAction,
    SetUIOverridesAction,
    SetUIVersionAction,
    SetUsersnapSettingsAction,
    UI_STATE_KEY,
} from './ui.actions';

const INITIAL_UI_STATE: UIState = {
    mode: UIMode.EDIT,
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
    usersnap: {
        key: undefined,
    },
    hideExtras: false,
};

@AppStateBranch<UIState>({
    name: UI_STATE_KEY,
    defaults: INITIAL_UI_STATE,
})
@Injectable()
export class UIStateModule {

    @ActionDefinition(SetBreadcrumbExpandedAction)
    handleSetBreadcrumbExpandedAction(ctx: StateContext<UIState>, action: SetBreadcrumbExpandedAction): void {
        switch (action.location) {
            case BreadcrumbLocation.CONTENT_FRAME:
                ctx.patchState({
                    contentFrameBreadcrumbsExpanded: action.isExpanded,
                });
                break;

            case BreadcrumbLocation.ITEM_LIST:
                ctx.patchState({
                    itemListBreadcrumbsExpanded: action.isExpanded,
                });
                break;

            case BreadcrumbLocation.CONTENT_REPOSITORY:
                ctx.patchState({
                    repositoryBrowserBreadcrumbsExpanded: action.isExpanded,
                });
                break;
        }
    }

    @ActionDefinition(SetCMPVersionAction)
    handleSetCMPVersionAction(ctx: StateContext<UIState>, action: SetCMPVersionAction): void {
        ctx.patchState({
            cmpVersion: action.version,
        });
    }

    @ActionDefinition(SetUIVersionAction)
    setUiVersion(ctx: StateContext<UIState>, action: SetUIVersionAction): void {
        ctx.patchState({
            uiVersion: action.version,
        });
    }

    @ActionDefinition(SetUIOverridesAction)
    handleSetUIOverridesAction(ctx: StateContext<UIState>, action: SetUIOverridesAction): void {
        ctx.patchState({
            overrides: action.overrides,
            overridesReceived: true,
        });
    }

    @ActionDefinition(SetBackendLanguageAction)
    handleSetBackendLanguageAction(ctx: StateContext<UIState>, action: SetBackendLanguageAction): void {
        ctx.patchState({
            backendLanguage: action.language,
        });
    }

    @ActionDefinition(SetUILanguageAction)
    handleSetUILanguageAction(ctx: StateContext<UIState>, action: SetUILanguageAction): void {
        ctx.patchState({
            language: action.language,
        });
    }

    @ActionDefinition(SetAvailableUILanguageAction)
    handleSetAvailableUILanguageAction(ctx: StateContext<UIState>, action: SetAvailableUILanguageAction): void {
        ctx.patchState({
            availableUiLanguages: action.languages,
        });
    }

    @ActionDefinition(SetBrokenLinksCountAction)
    handleSetBrokenLinksCountAction(ctx: StateContext<UIState>, action: SetBrokenLinksCountAction): void {
        ctx.setState(patch<UIState>({
            alerts: patch<Alerts>({
                linkChecker: patch<Alert>({
                    brokenLinksCount: action.count,
                }),
            }),
        }));
    }

    @ActionDefinition(SetUsersnapSettingsAction)
    handleSetUsersnapSettingsAction(ctx: StateContext<UIState>, action: SetUsersnapSettingsAction): void {
        ctx.patchState({
            usersnap: action.settings,
        });
    }

    @ActionDefinition(SetHideExtrasAction)
    handleSetHideExtrasAction(ctx: StateContext<UIState>, action: SetHideExtrasAction): void {
        ctx.patchState({
            hideExtras: action.doHide,
        });
    }

    @ActionDefinition(SetUIModeAction)
    handleSetUIModeAction(ctx: StateContext<UIState>, action: SetUIModeAction): void {
        ctx.patchState({
            mode: action.mode,
        });
    }
}
