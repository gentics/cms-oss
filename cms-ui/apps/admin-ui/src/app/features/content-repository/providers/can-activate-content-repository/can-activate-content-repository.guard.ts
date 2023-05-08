import { ContentRepositoryOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ContentRepositoryDataService } from '../../../../shared';
import { AbstractCanActivateEntityGuard } from '../../../../shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';

/**
 * A route guard checking for changes made by the contentRepository in an componennt with changable elements.
 */
@Injectable()
export class CanActivateContentRepositoryGuard extends AbstractCanActivateEntityGuard<'contentRepository', ContentRepositoryOperations> {
    constructor(
        contentRepositoryData: ContentRepositoryDataService,
        appState: AppStateService,
    ) {
        super(
            'contentRepository',
            contentRepositoryData,
            appState,
        );
    }

}
