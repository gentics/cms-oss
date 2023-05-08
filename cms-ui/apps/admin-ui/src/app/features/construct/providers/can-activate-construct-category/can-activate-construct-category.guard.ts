import { ConstructCategoryOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ConstructCategoryDataService } from '../../../../shared';
import { AbstractCanActivateEntityGuard } from '../../../../shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';

/**
 * A route guard checking for changes made by the user in an componennt with changable elements.
 */
@Injectable()
export class CanActivateConstructCategoryGuard extends AbstractCanActivateEntityGuard<'constructCategory', ConstructCategoryOperations> {
    constructor(
        constructCategoryData: ConstructCategoryDataService,
        appState: AppStateService,
    ) {
        super(
            'constructCategory',
            constructCategoryData,
            appState,
        );
    }

}
