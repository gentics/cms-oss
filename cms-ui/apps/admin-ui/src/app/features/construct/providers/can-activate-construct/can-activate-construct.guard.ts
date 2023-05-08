import { ConstructOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ConstructDataService } from '../../../../shared';
import { AbstractCanActivateEntityGuard } from '../../../../shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';

/**
 * A route guard checking for changes made by the user in an componennt with changable elements.
 */
@Injectable()
export class CanActivateConstructGuard extends AbstractCanActivateEntityGuard<'construct', ConstructOperations> {
    constructor(
        constructData: ConstructDataService,
        appState: AppStateService,
    ) {
        super(
            'construct',
            constructData,
            appState,
        );
    }

}
