import { Injectable } from '@angular/core';
import { StateContext } from '@ngxs/store';
import { append, compose, iif, patch, removeItem } from '@ngxs/store/operators';
import { isEqual } from 'lodash-es';
import { ToolsState } from '../../../common/models';
import { ActionDefinition, AppStateBranch } from '../../state-utils';
import {
    CloseToolAction,
    HideToolsAction,
    OpenToolAction,
    StartToolsFetchingAction,
    TOOLS_STATE_KEY,
    ToolBreadcrumbAction,
    ToolNavigationAction,
    ToolsFetchingErrorAction,
    ToolsFetchingSuccessAction,
} from './tools.actions';

const INITIAL_TOOLS_STATE: ToolsState = {
    active: [],
    available: [],
    breadcrumbs: {},
    fetching: false,
    received: false,
    subpath: {},
    visible: undefined,
};

@AppStateBranch<ToolsState>({
    name: TOOLS_STATE_KEY,
    defaults: INITIAL_TOOLS_STATE,
})
@Injectable()
export class ToolsStateModule {

    @ActionDefinition(StartToolsFetchingAction)
    handleStartToolsFetchingAction(ctx: StateContext<ToolsState>, action: StartToolsFetchingAction): void {
        ctx.patchState({
            fetching: true,
        });
    }

    @ActionDefinition(ToolsFetchingSuccessAction)
    handleToolsFetchingSuccessAction(ctx: StateContext<ToolsState>, action: ToolsFetchingSuccessAction): void {
        ctx.patchState({
            fetching: false,
            received: true,
            available: action.tools,
        });
    }

    @ActionDefinition(ToolsFetchingErrorAction)
    handleToolsFetchingErrorAction(ctx: StateContext<ToolsState>, action: ToolsFetchingErrorAction): void {
        ctx.patchState({
            fetching: false,
        });
    }

    @ActionDefinition(OpenToolAction)
    handleOpenToolAction(ctx: StateContext<ToolsState>, action: OpenToolAction): void {
        const state = ctx.getState();
        const tool = state.available.find(availableTool => availableTool.key === action.toolKey);

        if (!tool) {
            return;
        }

        const isActive = state.active.includes(action.toolKey);

        ctx.setState(compose<ToolsState>(
            iif<ToolsState>(!isActive, patch<ToolsState>({
                subpath: patch({
                    [action.toolKey]: action.subpath,
                }),
                active: append([action.toolKey]),
            })),
            patch<ToolsState>({
                visible: iif(!tool.newtab, action.toolKey),
            }),
        ));
    }

    @ActionDefinition(CloseToolAction)
    handleCloseToolAction(ctx: StateContext<ToolsState>, action: CloseToolAction): void {
        const state = ctx.getState();
        const isActive = state.active.includes(action.toolKey);
        const isVisible = state.visible === action.toolKey;

        ctx.setState(compose<ToolsState>(
            iif<ToolsState>(isActive, patch<ToolsState>({
                active: removeItem(key => key === action.toolKey),
                subpath: patch({
                    [action.toolKey]: '',
                }),
            })),
            iif<ToolsState>(isVisible, patch<ToolsState>({
                visible: undefined,
            })),
        ));
    }

    @ActionDefinition(ToolNavigationAction)
    handleToolNavigationAction(ctx: StateContext<ToolsState>, action: ToolNavigationAction): void {
        const state = ctx.getState();

        if (!state.active.includes(action.toolKey)) {
            return;
        }

        let subPath = action.subpath;

        if (subPath.charAt(0) === '/') {
            subPath = subPath.substring(1);
        }

        ctx.setState(patch<ToolsState>({
            subpath: patch({
                [action.toolKey]: subPath,
            }),
        }));
    }

    @ActionDefinition(HideToolsAction)
    handleHideToolsAction(ctx: StateContext<ToolsState>, action: HideToolsAction): void {
        ctx.patchState({
            visible: undefined,
        });
    }

    @ActionDefinition(ToolBreadcrumbAction)
    handleToolBreadcrumbAction(ctx: StateContext<ToolsState>, action: ToolBreadcrumbAction): void {
        const state = ctx.getState();
        const currentBreadcrumbs = state.breadcrumbs[action.toolKey];

        if (!currentBreadcrumbs || !isEqual(currentBreadcrumbs, action.breadcrumbs)) {
            ctx.setState(patch<ToolsState>({
                breadcrumbs: patch({
                    [action.toolKey]: action.breadcrumbs,
                }),
            }));
        }
    }

}
