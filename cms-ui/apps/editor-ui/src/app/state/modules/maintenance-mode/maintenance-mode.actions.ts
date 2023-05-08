import { MaintenanceModeResponse } from '@gentics/cms-models';
import { AppState } from '../../../common/models';
import { ActionDeclaration } from '../../state-utils';

export const MAINTENANCE_MODE_STATE_KEY: keyof AppState = 'maintenanceMode';

@ActionDeclaration(MAINTENANCE_MODE_STATE_KEY)
export class StartMaintenanceModeFetchingAction {}

@ActionDeclaration(MAINTENANCE_MODE_STATE_KEY)
export class MaintenanceModeFetchSuccessAction {
    constructor(
        public response: MaintenanceModeResponse,
    ) {}
}

@ActionDeclaration(MAINTENANCE_MODE_STATE_KEY)
export class MaintenanceModeFetchErrorAction {}
