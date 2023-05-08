import { masterLoading } from '@admin-ui/common';
import { EntityManagerService, I18nNotificationService, I18nService, PackageOperations, PermissionsService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { PackageBO, Raw } from '@gentics/cms-models';
import { OperatorFunction } from 'rxjs';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class PackageDataService extends ExtendedEntityDataServiceBase<'package', PackageOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: PackageOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected permissionsService: PermissionsService,
    ) {
        super(
            'package',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: PackageBO<Raw>): string {
        return entity.id;
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return masterLoading(this.state);
    }

}
