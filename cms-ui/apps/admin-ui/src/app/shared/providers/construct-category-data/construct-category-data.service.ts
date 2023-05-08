import { masterLoading } from '@admin-ui/common';
import {
    ConstructCategoryOperations,
    EntityManagerService,
    I18nNotificationService,
    I18nService,
    PermissionsService
} from '@admin-ui/core/providers';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ConstructCategoryBO, Raw } from '@gentics/cms-models';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class ConstructCategoryDataService extends ExtendedEntityDataServiceBase<'constructCategory', ConstructCategoryOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: ConstructCategoryOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected permissionsService: PermissionsService,
    ) {
        super(
            'constructCategory',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: ConstructCategoryBO<Raw>): string {
        return entity.id;
    }

    protected getLoadingOperator(): any {
        return masterLoading(this.state);
    }

}
