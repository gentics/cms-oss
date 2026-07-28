import { masterLoading } from '@admin-ui/common';
import { ContentPackageOperations, EntityManagerService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { ContentPackageBO, EntityIdType, ModelType } from '@gentics/cms-models';
import { I18nService } from '@gentics/cms-components';
import { OperatorFunction } from 'rxjs';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class ContentPackageDataService extends ExtendedEntityDataServiceBase<'contentPackage', ContentPackageOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: ContentPackageOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super('contentPackage', state, entityManager, entityOperations, notification, i18n);
    }

    override getEntityId(entity: ContentPackageBO<ModelType.Raw>): EntityIdType {
        return entity.id;
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return masterLoading(this.state);
    }
}
