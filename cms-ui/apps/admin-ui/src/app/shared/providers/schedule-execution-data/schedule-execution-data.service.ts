import { ListId, listLoading } from '@admin-ui/common';
import { EntityManagerService, I18nNotificationService, I18nService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ModelType, ScheduleExecution } from '@gentics/cms-models';
import { Observable, OperatorFunction } from 'rxjs';
import { ScheduleExecutionOperations } from '../../../core';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class ScheduleExecutionDataService extends ExtendedEntityDataServiceBase<'scheduleExecution', ScheduleExecutionOperations>  {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: ScheduleExecutionOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(
            'scheduleExecution',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: ScheduleExecution<ModelType.Raw>): string | number {
        return entity.id;
    }

    override getEntitiesFromApi(options?: any): Observable<ScheduleExecution[]> {
        const { parentId, ...other } = options || {};

        return this.entityOperations.getAll(other, parentId).pipe(
            this.getLoadingOperator(),
        );
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return listLoading(this.state, ListId.SCHEDULE_EXECUTION);
    }

}
