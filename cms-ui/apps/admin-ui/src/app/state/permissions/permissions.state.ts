import { Injectable } from '@angular/core';
import { AccessControlledType, Index, PermissionsMapCollection } from '@gentics/cms-models';
import { StateContext } from '@ngxs/store';
import { ActionDefinition, AppStateBranch, defineInitialState } from '../utils';
import { AddTypePermissionsMap, ClearAllPermissions } from './permissions.actions';

/**
 * Maps each `AccessControlledType` to its `PermissionsMapCollection` if it has been loaded.
 */
export type TypePermissionsIndex = Partial<Index<AccessControlledType, PermissionsMapCollection>>;

/**
 * Contains the currently loaded permissions of the current user.
 *
 * For now this only contains type permissions.
 */
export interface PermissionsStateModel {

    /**
     * Maps each `AccessControlledType` to its `PermissionsMapCollection` if it has been loaded.
     */
    types: TypePermissionsIndex;

}

export const INITIAL_PERMISSIONS_STATE = defineInitialState<PermissionsStateModel>({
    types: {},
});

@AppStateBranch({
    name: 'permissions',
    defaults: INITIAL_PERMISSIONS_STATE,
})
@Injectable()
export class PermissionsStateModule {

    @ActionDefinition(AddTypePermissionsMap)
    addTypePermissionsMap(ctx: StateContext<PermissionsStateModel>, action: AddTypePermissionsMap): void {
        ctx.patchState({
            types: {
                ...ctx.getState().types,
                [action.type]: action.permissionsMapCollection,
            },
        });
    }

    @ActionDefinition(ClearAllPermissions)
    clearAllPermissions(ctx: StateContext<PermissionsStateModel>): void {
        ctx.setState(INITIAL_PERMISSIONS_STATE);
    }

}
