import { AppState } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';

export const NODE_SETTINGS_STATE_KEY: keyof AppState = 'nodeSettings';

@ActionDeclaration(NODE_SETTINGS_STATE_KEY)
export class NodeSettingsFetchingSuccessAction {
    constructor(
        public nodeId: number,
        // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
        public data: any,
        public global: boolean = false,
    ) {}
}
