import { masterLoading } from '@admin-ui/common/utils/rxjs-loading-operators/master-loading.operator';
import {
    EntityManagerService,
    MarkupLanguageOperations,
    PermissionsService,
} from '@admin-ui/core/providers';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { MarkupLanguage, Raw } from '@gentics/cms-models';
import { I18nService } from '@gentics/cms-components';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class MarkupLanguageDataService extends ExtendedEntityDataServiceBase<'markupLanguage', MarkupLanguageOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: MarkupLanguageOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected permissionsService: PermissionsService,
    ) {
        super(
            'markupLanguage',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: MarkupLanguage<Raw>): number {
        return entity.id;
    }

    protected getLoadingOperator<U>(): any {
        return masterLoading(this.state);
    }

}
