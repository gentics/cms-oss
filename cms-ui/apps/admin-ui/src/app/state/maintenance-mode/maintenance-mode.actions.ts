import { MaintenanceModeResponse } from '@gentics/cms-models';
import { AppState } from '../app-state';
import { ActionDeclaration } from '../utils/state-utils';

const MAINTENANCE: keyof AppState = 'maintenanceMode';

@ActionDeclaration(MAINTENANCE)
export class FetchMaintenanceStatusStart {
    static readonly type = 'FetchMaintenanceStatusStart';
}

@ActionDeclaration(MAINTENANCE)
export class FetchMaintenanceStatusSuccess {
    static readonly type = 'FetchMaintenanceStatusSuccess';
    constructor(public response: MaintenanceModeResponse) {}
}

@ActionDeclaration(MAINTENANCE)
export class FetchMaintenanceStatusError {
    static readonly type = 'FetchMaintenanceStatusError';
}
