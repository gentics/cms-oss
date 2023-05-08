
import { ScheduleTaskOperations } from '@admin-ui/core';
import { ScheduleTaskDataService } from '@admin-ui/shared';
import { AbstractCanActivateEntityGuard } from '@admin-ui/shared/providers/abstract-can-activate-entity/abstract-can-activate-entity.guard';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';

/**
 * A route guard checking for changes made by the user in an componennt with changable elements.
 */
@Injectable()
export class CanActivateScheduleTaskGuard extends AbstractCanActivateEntityGuard<'scheduleTask', ScheduleTaskOperations> {

    constructor(
        entityData: ScheduleTaskDataService,
        appState: AppStateService,
    ) {
        super(
            'scheduleTask',
            entityData,
            appState,
        );
    }

}
