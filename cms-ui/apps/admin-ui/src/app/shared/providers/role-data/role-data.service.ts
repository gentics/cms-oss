import { masterLoading } from '@admin-ui/common/utils/rxjs-loading-operators/master-loading.operator';
import { EntityManagerService, I18nNotificationService, I18nService, RoleOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ModelType, RoleBO } from '@gentics/cms-models';
import { OperatorFunction } from 'rxjs';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';


@Injectable()
export class RoleDataService extends ExtendedEntityDataServiceBase<'role', RoleOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: RoleOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(
            'role',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: RoleBO<ModelType.Raw>): string | number {
        return entity.id;
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return masterLoading(this.state);
    }
}
