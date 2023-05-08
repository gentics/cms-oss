import { InitializableServiceBase } from '@admin-ui/shared/providers/initializable-service-base';
import {
    AppState,
    AppStateService,
    ClearAllEntities,
    ClearAllPermissions,
    ClearMessageState,
    selectLogoutEvent,
} from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Index } from '@gentics/cms-models';
import { takeUntil } from 'rxjs/operators';

/**
 * This service is responsible for cleaning up sensitive data from the AppState when a user logs out.
 */
@Injectable()
export class LogoutCleanupService extends InitializableServiceBase {

    /**
     * Defines the cleanup actions should be taken for each branch of the AppState.
     * Use `null` to indicate that the branch should not be cleaned up.
     *
     * Using such a map will trigger a compile error if we add a new AppState branch,
     * but forget to define whether it needs to be cleaned up.
     */
    private cleanupActions: Index<keyof AppState, () => void>  = {
        auth: null,
        entity: () => this.appState.dispatch(new ClearAllEntities()),
        features: null,
        loading: null,
        maintenanceMode: null,
        messages: () => this.appState.dispatch(new ClearMessageState()),
        permissions: () => this.appState.dispatch(new ClearAllPermissions()),
        ui: null,
    };

    constructor(
        private appState: AppStateService,
    ) {
        super();
    }

    protected onServiceInit(): void {
        selectLogoutEvent(this.appState).pipe(
            takeUntil(this.stopper.stopper$),
        ).subscribe(() => this.doCleanup());
    }

    private doCleanup(): void {
        Object.keys(this.cleanupActions).forEach(stateBranch => {
            const action = this.cleanupActions[stateBranch];
            if (action) {
                action();
            }
        });
    }

}
