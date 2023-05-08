import { masterLoading } from '@admin-ui/common';
import { ContentPackageOperations, EntityManagerService, I18nNotificationService, I18nService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ContentPackageBO, EntityIdType, ModelType } from '@gentics/cms-models';
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
