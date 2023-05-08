import { Injectable } from '@angular/core';
import { StateContext } from '@ngxs/store';
import { iif, patch } from '@ngxs/store/operators';
import { NodeSettingsState } from '../../../common/models';
import { ActionDefinition, AppStateBranch } from '../../state-utils';
import { NODE_SETTINGS_STATE_KEY, NodeSettingsFetchingSuccessAction } from './node-settings.actions';

const INITIAL_NODE_SETTINGS_STATE: NodeSettingsState = {
    node: {},
    global: {},
};

@AppStateBranch<NodeSettingsState>({
    name: NODE_SETTINGS_STATE_KEY,
    defaults: INITIAL_NODE_SETTINGS_STATE,
})
@Injectable()
export class NodeSettingsStateModule {

    @ActionDefinition(NodeSettingsFetchingSuccessAction)
    loaded(ctx: StateContext<NodeSettingsState>, action: NodeSettingsFetchingSuccessAction): void {
        ctx.setState(patch<NodeSettingsState>({
            global: iif(action.global, action.data),
            node: patch({
                [action.nodeId]: action.data,
            }),
        }));
    }
}
