import { NodeFeature } from '@gentics/cms-models';
import { AppState } from '../../../common/models';
import { FeaturesState } from '../../../common/models/features-state';
import { ActionDeclaration } from '../../state-utils';

export const FEATURES_STATE_KEY: keyof AppState = 'features';

@ActionDeclaration(FEATURES_STATE_KEY)
export class SetFeatureAction {
    constructor(
        public feature: keyof FeaturesState,
        public enabled: boolean | { [id: number]: NodeFeature[]; },
    ) {}
}

@ActionDeclaration(FEATURES_STATE_KEY)
export class SetNodeFeaturesAction {
    constructor(
        public nodeId: number,
        public features: NodeFeature[],
    ) {}
}
