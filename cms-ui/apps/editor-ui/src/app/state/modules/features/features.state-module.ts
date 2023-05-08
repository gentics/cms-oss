import { Injectable } from '@angular/core';
import { StateContext } from '@ngxs/store';
import { patch } from '@ngxs/store/operators';
import { FeaturesState } from '../../../common/models';
import { ActionDefinition, AppStateBranch } from '../../state-utils';
import { FEATURES_STATE_KEY, SetFeatureAction, SetNodeFeaturesAction } from './features.actions';

const INTIIAL_FEATURES_STATE: FeaturesState = {
    nice_urls: false,
    elasticsearch: false,
    always_localize: false,
    recent_items: false,
    focal_point_editing: false,
    hide_manual: false,
    imagemanipulation2: false,
    enable_image_upload_in_tagfill: false,
    tagfill_light: true,
    wastebin: false,
    pub_dir_segment: false,
    usersnap: false,
    autocomplete_folder_path: false,
    devtools: false,
    keycloak_signout: false,
    folder_based_template_selection: false,
    nodeFeatures: {},
    content_staging: false,
};

@AppStateBranch<FeaturesState>({
    name: FEATURES_STATE_KEY,
    defaults: INTIIAL_FEATURES_STATE,
    })
@Injectable()
export class FeaturesStateModule {

    @ActionDefinition(SetFeatureAction)
    handleSetFeatureAction(ctx: StateContext<FeaturesState>, action: SetFeatureAction): void {
        ctx.patchState({
            [action.feature]: action.enabled,
        });
    }

    @ActionDefinition(SetNodeFeaturesAction)
    handleSetNodeFeaturesAction(ctx: StateContext<FeaturesState>, action: SetNodeFeaturesAction): void {
        ctx.setState(patch({
            nodeFeatures: patch({
                [action.nodeId]: action.features,
            }),
        }));
    }
}
