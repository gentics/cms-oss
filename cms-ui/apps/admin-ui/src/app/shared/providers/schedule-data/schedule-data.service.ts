import { LIST_LOADER, ListId, listLoading } from '@admin-ui/common';
import { EntityManagerService } from '@admin-ui/core';
import { ScheduleOperations } from '@admin-ui/core/providers/operations/schedule/schedule.operations';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { ModelType, ScheduleBO } from '@gentics/cms-models';
import { I18nService } from '@gentics/cms-components';
import { OperatorFunction } from 'rxjs';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class ScheduleDataService extends ExtendedEntityDataServiceBase<'schedule', ScheduleOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: ScheduleOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(
            'schedule',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    override getEntityId(entity: ScheduleBO<ModelType.Raw>): string | number {
        return entity.id;
    }

    protected getLoadingOperator<U>(options?: any): OperatorFunction<U, U> {
        if (!options?.[LIST_LOADER]) {
            return (source) => source;
        }

        return listLoading(this.state, ListId.SCHEDULE);
    }
}
