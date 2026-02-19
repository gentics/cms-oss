import { Injectable } from '@angular/core';
import { StateContext } from '@ngxs/store';
import { iif, patch } from '@ngxs/store/operators';
import { FALLBACK_LANGUAGE } from '../../../common/config/config';
import { Alert, Alerts, UIMode, UIState } from '../../../common/models';
import { ActionDefinition, AppStateBranch } from '../../state-utils';
import {
    BreadcrumbLocation,
    DecreaseOverlayCountAction,
    IncreaseOverlayCountAction,
    ResetOverlayCountAction,
    SetAvailableUILanguageAction,
    SetBackendLanguageAction,
    SetBreadcrumbExpandedAction,
    SetBrokenLinksCountAction,
    SetCMPVersionAction,
    SetConstructFavourites,
    SetHideExtrasAction,
    SetNodesLoadedAction,
    SetTagEditorOpenAction,
    SetUILanguageAction,
    SetUIModeAction,
    SetUIOverridesAction,
    SetUIVersionAction,
    SetUsersnapSettingsAction,
    UI_STATE_KEY,
    UpdateIsAdminAction,
} from './ui.actions';

const INITIAL_UI_STATE: UIState = {
    mode: UIMode.EDIT,
    isAdmin: false,
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
    overlayCount: 0,
    constructFavourites: [],
    tagEditorOpen: false,
    nodesLoaded: false,
};

@AppStateBranch<UIState>({
    name: UI_STATE_KEY,
    defaults: INITIAL_UI_STATE,
})
@Injectable()
export class UIStateModule {

    @ActionDefinition(UpdateIsAdminAction)
    handleUpdateIsAdminAction(ctx: StateContext<UIState>, action: UpdateIsAdminAction): void {
        ctx.patchState({
            isAdmin: action.isAdmin,
        });
    }

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
        const state = ctx.getState();
        ctx.setState(patch<UIState>({
            alerts: patch<Alerts>({
                linkChecker: iif(state.alerts.linkChecker != null, patch<Alert>({
                    brokenLinksCount: action.count,
                }), {
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

    @ActionDefinition(SetConstructFavourites)
    handleSetConstructFavourites(ctx: StateContext<UIState>, action: SetConstructFavourites): void {
        ctx.patchState({
            constructFavourites: action.favourites,
        });
    }

    @ActionDefinition(IncreaseOverlayCountAction)
    handleIncreaseOverlayCountAction(ctx: StateContext<UIState>): void {
        ctx.patchState({
            overlayCount: ctx.getState().overlayCount + 1,
        });
    }

    @ActionDefinition(DecreaseOverlayCountAction)
    handleDecreaseOverlayCountAction(ctx: StateContext<UIState>): void {
        ctx.patchState({
            overlayCount: Math.max(0, ctx.getState().overlayCount - 1),
        });
    }

    @ActionDefinition(ResetOverlayCountAction)
    handleResetOverlayCountAction(ctx: StateContext<UIState>): void {
        ctx.patchState({
            overlayCount: 0,
        });
    }

    @ActionDefinition(SetTagEditorOpenAction)
    handleSetTagEditorOpenAction(ctx: StateContext<UIState>, action: SetTagEditorOpenAction): void {
        ctx.patchState({
            tagEditorOpen: action.isOpen,
        });
    }

    @ActionDefinition(SetNodesLoadedAction)
    handleSetNodesLoadedAction(ctx: StateContext<UIState>, action: SetNodesLoadedAction): void {
        ctx.patchState({
            nodesLoaded: action.loaded,
        });
    }
}
