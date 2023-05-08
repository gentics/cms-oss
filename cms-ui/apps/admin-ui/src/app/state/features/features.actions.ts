import { Feature, NodeFeature } from '@gentics/cms-models';
import { AppState } from '../app-state';
import { ActionDeclaration } from '../utils';

const FEATURES: keyof AppState = 'features';

@ActionDeclaration(FEATURES)
export class SetGlobalFeature {
    static readonly type = 'SetGlobalFeature';
    constructor(public feature: Feature, public enabled: boolean) {}
}

@ActionDeclaration(FEATURES)
export class SetNodeFeatures {
    static readonly type = 'SetNodeFeatures';
    constructor(public nodeId: number, public enabledFeatures: NodeFeature[]) {}
}
