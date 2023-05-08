import { Injectable } from '@angular/core';
import { Feature, Index, IndexById, NodeFeature } from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import { patch } from '@ngxs/store/operators';
import { ActionDefinition, AppStateBranch, defineInitialState } from '../utils';
import { SetGlobalFeature, SetNodeFeatures } from './features.actions';

/**
 * A map that indicates if a certain global feature is enabled.
 */
export type GlobalFeaturesMap = Partial<Index<Feature, boolean>>;

/**
 * A map that indicates if a certain node feature is enabled.
 */
export type NodeFeaturesMap = Partial<Index<NodeFeature, boolean>>;

export interface FeaturesStateModel {

    /** Contains info about the global CMS features */
    global: GlobalFeaturesMap;

    /** Contains info about which node features are activated for a particular node. */
    node: IndexById<NodeFeaturesMap>;

}

export const INITIAL_FEATURES_STATE = defineInitialState<FeaturesStateModel>({
    global: {},
    node: {},
});

@AppStateBranch({
    name: 'features',
    defaults: INITIAL_FEATURES_STATE,
})
@Injectable()
export class FeaturesStateModule {

    @ActionDefinition(SetGlobalFeature)
    setGlobalFeature(ctx: StateContext<FeaturesStateModel>, action: SetGlobalFeature): void {
        ctx.setState(patch<FeaturesStateModel>({
            global: patch({ [action.feature]: action.enabled }),
        }));
    }

    @ActionDefinition(SetNodeFeatures)
    setNodeFeatures(ctx: StateContext<FeaturesStateModel>, action: SetNodeFeatures): void {
        const features = action.enabledFeatures.reduce<NodeFeaturesMap>((featuresMap, feature) => {
            featuresMap[feature] = true;
            return featuresMap;
        }, {});

        ctx.setState(patch<FeaturesStateModel>({
            node: patch({ [action.nodeId]: features }),
        }));
    }
}
