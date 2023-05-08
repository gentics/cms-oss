import { Injectable } from '@angular/core';
import { StateContext } from '@ngxs/store';
import { MaintenanceModeState } from '../../../common/models';
import { ActionDefinition, AppStateBranch } from '../../state-utils';
import {
    MAINTENANCE_MODE_STATE_KEY,
    MaintenanceModeFetchErrorAction,
    MaintenanceModeFetchSuccessAction,
    StartMaintenanceModeFetchingAction,
} from './maintenance-mode.actions';

const INITIAL_MAINTENANCE_MODE_STATE: MaintenanceModeState = {
    active: false,
    fetching: false,
    message: '',
    reportedByServer: undefined,
    showBanner: false,
};

@AppStateBranch<MaintenanceModeState>({
    name: MAINTENANCE_MODE_STATE_KEY,
    defaults: INITIAL_MAINTENANCE_MODE_STATE,
})
@Injectable()
export class MaintenanceModeStateModule {

    @ActionDefinition(StartMaintenanceModeFetchingAction)
    handleStartMaintenanceModeFetchingAction(ctx: StateContext<MaintenanceModeState>, action: StartMaintenanceModeFetchingAction): void {
        ctx.patchState({
            fetching: true,
        });
    }

    @ActionDefinition(MaintenanceModeFetchSuccessAction)
    handleMaintenanceModeFetchSuccessAction(ctx: StateContext<MaintenanceModeState>, action: MaintenanceModeFetchSuccessAction): void {
        ctx.patchState({
            active: action.response.maintenance,
            fetching: false,
            message: action.response.message,
            reportedByServer: true,
            showBanner: action.response.banner,
        });
    }

    @ActionDefinition(MaintenanceModeFetchErrorAction)
    handleMaintenanceModeFetchErrorAction(ctx: StateContext<MaintenanceModeState>, action: MaintenanceModeFetchErrorAction): void {
        ctx.patchState({
            active: false,
            fetching: false,
            message: '',
            reportedByServer: false,
            showBanner: false,
        });
    }
}
