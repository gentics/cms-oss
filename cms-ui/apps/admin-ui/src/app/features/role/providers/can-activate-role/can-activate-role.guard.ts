import { RoleOperations } from '@admin-ui/core';
import { RoleDataService } from '@admin-ui/shared';
import { AbstractCanActivateEntityGuard } from '@admin-ui/shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';

/**
 * A route guard checking for changes made by the user in an component with changeable elements.
 */
@Injectable()
export class CanActivateRoleGuard extends AbstractCanActivateEntityGuard<'role', RoleOperations> {
    constructor(
        roleData: RoleDataService,
        appState: AppStateService,
    ) {
        super(
            'role',
            roleData,
            appState,
        );
    }

}
