import { Injectable } from '@angular/core';
import { Feature } from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import { patch } from '@ngxs/store/operators';
import { FeaturesState } from '../../../common/models';
import { ActionDefinition, AppStateBranch } from '../../state-utils';
import { FEATURES_STATE_KEY, SetFeatureAction, SetNodeFeaturesAction } from './features.actions';

/* eslint-disable @typescript-eslint/naming-convention */
const INTIIAL_FEATURES_STATE: FeaturesState = {
    [Feature.NICE_URLS]: false,
    [Feature.ELASTICSEARCH]: false,
    [Feature.ALWAYS_LOCALIZE]: false,
    [Feature.RECENT_ITEMS]: false,
    [Feature.FOCAL_POINT_EDITING]: false,
    [Feature.HIDE_MANUAL]: false,
    [Feature.IMAGE_MANIPULATION2]: false,
    [Feature.ENABLE_UPLOAD_IN_TAGFILL]: false,
    [Feature.TAGFILL_LIGHT]: true,
    [Feature.WASTEBIN]: false,
    [Feature.PUB_DIR_SEGMENT]: false,
    [Feature.USERSNAP]: false,
    [Feature.AUTOCOMPLETE_FOLDER_PATH]: false,
    [Feature.DEVTOOLS]: false,
    [Feature.KEYCLOAK]: false,
    [Feature.KEYCLOAK_SIGNOUT]: false,
    [Feature.FOLDER_BASED_TEMPLATE_SELECTION]: false,
    [Feature.CONTENT_STAGING]: false,
    [Feature.MULTICHANNELLING]: false,
    [Feature.MESH_CR]: false,
    [Feature.OBJECT_TAG_SYNC]: false,
    [Feature.LINK_CHECKER]: false,

    nodeFeatures: {},
};
/* eslint-enable @typescript-eslint/naming-convention */

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
