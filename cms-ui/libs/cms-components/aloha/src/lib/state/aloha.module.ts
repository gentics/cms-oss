import { Injectable } from '@angular/core';
import { ActionDefinition } from '@gentics/cms-components';
import { State, StateContext } from '@ngxs/store';
import { AlohaStateModel, INITIAL_ALOHA_STATE, SetAlohaResources } from '../models/state';

@State({
    name: 'aloha',
    defaults: INITIAL_ALOHA_STATE,
})
@Injectable()
export class AlohaStateModule {

    @ActionDefinition(SetAlohaResources)
    handleSetAlohaRessources(ctx: StateContext<AlohaStateModel>, action: SetAlohaResources): void {
        ctx.patchState({
            jsFiles: action.jsFiles,
            cssFiles: action.cssFiles,
        });
    }
}
