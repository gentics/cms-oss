import { ObjectPropertyCategoryOperations } from '@admin-ui/core';
import { ObjectPropertyCategoryDataService } from '@admin-ui/shared';
import { AbstractCanActivateEntityGuard } from '@admin-ui/shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';

/**
 * A route guard checking for changes made by the user in an componennt with changable elements.
 */
@Injectable()
export class CanActivateObjectPropertyCategoryGuard extends AbstractCanActivateEntityGuard<'objectPropertyCategory', ObjectPropertyCategoryOperations> {
    constructor(
        objectPropertyCategoryData: ObjectPropertyCategoryDataService,
        appState: AppStateService,
    ) {
        super(
            'objectPropertyCategory',
            objectPropertyCategoryData,
            appState,
        );
    }

}
