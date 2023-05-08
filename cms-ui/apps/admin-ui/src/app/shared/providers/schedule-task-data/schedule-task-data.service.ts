import { ListId, listLoading, LIST_LOADER } from '@admin-ui/common';
import { EntityManagerService, I18nNotificationService, I18nService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ModelType, ScheduleTaskBO } from '@gentics/cms-models';
import { OperatorFunction } from 'rxjs';
import { ScheduleTaskOperations } from '../../../core';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class ScheduleTaskDataService extends ExtendedEntityDataServiceBase<'scheduleTask', ScheduleTaskOperations>  {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: ScheduleTaskOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(
            'scheduleTask',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: ScheduleTaskBO<ModelType.Raw>): string | number {
        return entity.id;
    }

    protected getLoadingOperator<U>(options?: any): OperatorFunction<U, U> {
        if (!options?.[LIST_LOADER]) {
            return (source) => source;
        }

        return listLoading(this.state, ListId.SCHEDULE_TASK);
    }
}
