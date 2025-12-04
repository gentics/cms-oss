import { LIST_LOADER, ListId, listLoading } from '@admin-ui/common';
import { EntityManagerService, ScheduleTaskOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { ModelType, ScheduleTaskBO } from '@gentics/cms-models';
import { I18nService } from '@gentics/cms-components';
import { OperatorFunction } from 'rxjs';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class ScheduleTaskDataService extends ExtendedEntityDataServiceBase<'scheduleTask', ScheduleTaskOperations> {

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
