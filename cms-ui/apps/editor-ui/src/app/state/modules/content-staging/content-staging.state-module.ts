import { Injectable } from '@angular/core';
import { StateContext } from '@ngxs/store';
import { iif, patch } from '@ngxs/store/operators';
import { omit } from 'lodash-es';
import { ContentStagingState } from '../../../common/models';
import { ActionDefinition, AppStateBranch } from '../../state-utils';
import {
    AddContentStagingEntryAction,
    AddContentStagingMapAction,
    CONTENT_STAGING_STATE_KEY,
    ClearContentStagingMapAction,
    ContentPackageErrorAction,
    ContentPackageSuccessAction,
    LoadContentPackagesAction,
    RemoveContentStagingEntryAction,
    SetActiveContentPackageAction,
} from './content-staging.actions';

const INITIAL_CONTENT_STATING_STATE: ContentStagingState = {
    activePackage: null,
    fetching: false,
    stagingMap: {},
};

@AppStateBranch<ContentStagingState>({
    name: CONTENT_STAGING_STATE_KEY,
    defaults: INITIAL_CONTENT_STATING_STATE,
    })
@Injectable()
export class ContentStagingStateModule {

    @ActionDefinition(LoadContentPackagesAction)
    handleLoadContentPackagesAction(ctx: StateContext<ContentStagingState>, action: LoadContentPackagesAction): void {
        ctx.patchState({
            fetching: true,
        });
    }

    @ActionDefinition(ContentPackageSuccessAction)
    handleContentPackageSuccessAction(ctx: StateContext<ContentStagingState>, action: ContentPackageSuccessAction): void {
        ctx.patchState({
            fetching: false,
        });
    }

    @ActionDefinition(ContentPackageErrorAction)
    handleContentPackageErrorAction(ctx: StateContext<ContentStagingState>, action: ContentPackageErrorAction): void {
        ctx.patchState({
            fetching: false,
            lastError: action.error,
        });
    }

    @ActionDefinition(SetActiveContentPackageAction)
    handleSetActiveContentPackageAction(ctx: StateContext<ContentStagingState>, action: SetActiveContentPackageAction): void {
        const state = ctx.getState();

        ctx.setState(patch({
            activePackage: action.name,
            stagingMap: iif(action.name !== state.activePackage, {}),
        }));
    }

    @ActionDefinition(ClearContentStagingMapAction)
    handleClearContentStagingMapAction(ctx: StateContext<ContentStagingState>, action: ClearContentStagingMapAction): void {
        ctx.patchState({ stagingMap: {} });
    }

    @ActionDefinition(RemoveContentStagingEntryAction)
    handleRemoveContentStagingEntryAction(ctx: StateContext<ContentStagingState>, action: RemoveContentStagingEntryAction): void {
        const state = ctx.getState();
        const newMap = omit(state.stagingMap, action.entityId);

        ctx.patchState({
            stagingMap: newMap,
        });
    }

    @ActionDefinition(AddContentStagingEntryAction)
    handleAddContentStagingEntryAction(ctx: StateContext<ContentStagingState>, action: AddContentStagingEntryAction): void {
        const state = ctx.getState();

        ctx.setState(patch({
            stagingMap: patch({
                [action.entityId]: iif(state.stagingMap[action.entityId] != null,
                    patch(action.status),
                    action.status,
                ),
            }),
        }));
    }

    @ActionDefinition(AddContentStagingMapAction)
    handleAddContentStagingMapAction(ctx: StateContext<ContentStagingState>, action: AddContentStagingMapAction): void {
        const state = ctx.getState();

        ctx.setState(patch({
            stagingMap: patch({
                ...state.stagingMap,
                ...action.map,
            }),
        }));
    }
}
