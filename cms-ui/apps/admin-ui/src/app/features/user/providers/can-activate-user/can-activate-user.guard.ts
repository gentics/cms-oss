import { UserOperations } from '@admin-ui/core';
import { UserDataService } from '@admin-ui/shared';
import { AbstractCanActivateEntityGuard } from '@admin-ui/shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';

/**
 * A route guard checking for changes made by the user in an componennt with changable elements.
 */
@Injectable()
export class CanActivateUserGuard extends AbstractCanActivateEntityGuard<'user', UserOperations> {
    constructor(
        userData: UserDataService,
        appState: AppStateService,
    ) {
        super(
            'user',
            userData,
            appState,
        );
    }

}
