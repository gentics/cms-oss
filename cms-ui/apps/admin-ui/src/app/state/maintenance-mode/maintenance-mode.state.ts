import { Injectable } from '@angular/core';
import { Action, StateContext } from '@ngxs/store';

import { ActionDefinition, AppStateBranch, defineInitialState } from '../utils/state-utils';
import { FetchMaintenanceStatusError, FetchMaintenanceStatusStart, FetchMaintenanceStatusSuccess } from './maintenance-mode.actions';

export interface MaintenanceModeStateModel {
    active: boolean;
    fetching: boolean;
    reportedByServer: boolean | undefined;
    showBanner: boolean;
    message: string;
}

export const INITIAL_MAINTENANCE_MODE_STATE = defineInitialState<MaintenanceModeStateModel>({
    active: false,
    fetching: false,
    message: '',
    reportedByServer: undefined,
    showBanner: false,
});

@AppStateBranch({
    name: 'maintenanceMode',
    defaults: INITIAL_MAINTENANCE_MODE_STATE,
})
@Injectable()
export class MaintenanceModeStateModule {

    @ActionDefinition(FetchMaintenanceStatusStart)
    fetchMaintenanceStatusStart(ctx: StateContext<MaintenanceModeStateModel>): void {
        ctx.patchState({
            fetching: true,
        });
    }

    @ActionDefinition(FetchMaintenanceStatusSuccess)
    fetchMaintenanceStatusSuccess(ctx: StateContext<MaintenanceModeStateModel>, action: FetchMaintenanceStatusSuccess): void {
        ctx.patchState({
            active: action.response.maintenance,
            fetching: false,
            message: action.response.message,
            reportedByServer: true,
            showBanner: action.response.banner,
        });
    }

    @ActionDefinition(FetchMaintenanceStatusError)
    fetchMaintenanceStatusError(ctx: StateContext<MaintenanceModeStateModel>): void {
        ctx.patchState({
            active: false,
            fetching: false,
            message: '',
            reportedByServer: false,
            showBanner: false,
        });
    }

}
