import { AuthStateModel } from '@gentics/cms-components/auth';
import { EntityStateModel } from './entity/entity.state';
import { FeaturesStateModel } from './features/features.state';
import { LoadingStateModel } from './loading/loading.state';
import { MaintenanceModeStateModel } from './maintenance-mode/maintenance-mode.state';
import { MessageStateModel } from './messages/message.state';
import { PermissionsStateModel } from './permissions/permissions.state';
import { UIStateModel } from './ui/ui.state';

// IMPORTANT: When adding a new state branch, the corresponding module
// needs to be added to the STATE_MODULES array in state.module.ts.

// IMPORTANT: When adding a new state branch, which should be cleared
// when a user logs out, add the clearing logic to the `LogoutCleanupService`.

/**
 * Defines the interface of the global app state.
 *
 * To access the AppState use either the `@SelectState` decorator
 * or inject the `AppStateService`.
 */
export interface AppState {
    auth: AuthStateModel;
    entity: EntityStateModel;
    features: FeaturesStateModel;
    loading: LoadingStateModel;
    maintenanceMode: MaintenanceModeStateModel;
    messages: MessageStateModel;
    permissions: PermissionsStateModel;
    ui: UIStateModel;
}
