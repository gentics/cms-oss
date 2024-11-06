import { SchemaContainer } from '@admin-ui/features/mesh-browser/models/mesh-browser-models';
import { Injectable } from '@angular/core';
import { StateContext } from '@ngxs/store';
import { patch } from '@ngxs/store/operators';
import { ActionDefinition, AppStateBranch, defineInitialState } from '../utils/state-utils';
import { SchemasLoaded } from './mesh.actions';


export interface MeshStateModel {
    schemas: SchemaContainer[];
}


export const INITIAL_MESH_STATE = defineInitialState<MeshStateModel>({
    schemas: [],
});

@AppStateBranch({
    name: 'mesh',
    defaults: INITIAL_MESH_STATE,
})
@Injectable()
export class MeshStateModule {

    @ActionDefinition(SchemasLoaded)
    setSchemas(ctx: StateContext<MeshStateModel>, action: SchemasLoaded): void {
        ctx.setState(patch({
            schemas: action.schemas,
        }));
    }
}
