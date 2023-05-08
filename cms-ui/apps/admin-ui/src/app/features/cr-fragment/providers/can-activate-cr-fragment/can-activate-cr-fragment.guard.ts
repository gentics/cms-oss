import { ContentRepositoryFragmentOperations } from '@admin-ui/core';
import { ContentRepositoryFragmentDataService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { AbstractCanActivateEntityGuard } from '../../../../shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';

/**
 * A route guard checking for changes made by the user in an componennt with changable elements.
 */
@Injectable()
export class CanActivateCRFragmentGuard extends AbstractCanActivateEntityGuard<'contentRepositoryFragment', ContentRepositoryFragmentOperations> {
    constructor(
        dataService: ContentRepositoryFragmentDataService,
        appState: AppStateService,
    ) {
        super(
            'contentRepositoryFragment',
            dataService,
            appState,
        );
    }

}
