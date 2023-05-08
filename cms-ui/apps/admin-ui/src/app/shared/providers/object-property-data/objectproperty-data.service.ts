import { detailLoading, LOAD_FROM_PACKAGE, masterLoading } from '@admin-ui/common';
import {
    EntityManagerService,
    I18nNotificationService,
    I18nService,
    ObjectPropertyOperations,
    PermissionsService,
} from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ObjectPropertyBO, ObjectPropertyListOptions, Raw } from '@gentics/cms-models';
import { Observable, OperatorFunction } from 'rxjs';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class ObjectPropertyDataService extends ExtendedEntityDataServiceBase<'objectProperty', ObjectPropertyOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: ObjectPropertyOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected permissionsService: PermissionsService,
    ) {
        super(
            'objectProperty',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: ObjectPropertyBO<Raw>): string {
        return entity.id;
    }

    override getEntitiesFromApi(options?: ObjectPropertyListOptions): Observable<ObjectPropertyBO<Raw>[]> {
        if (options?.[LOAD_FROM_PACKAGE]) {
            return this.entityOperations.getAllFromPackage(options[LOAD_FROM_PACKAGE], options).pipe(
                detailLoading(this.state),
            );
        }

        return super.getEntitiesFromApi(options);
    }

    protected getLoadingOperator<U>(options?: any): OperatorFunction<U, U> {
        return masterLoading(this.state);
    }
}
