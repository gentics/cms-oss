import { detailLoading } from '@admin-ui/common';
import { EntityManagerService, I18nNotificationService, I18nService, ObjectPropertyCategoryOperations, PermissionsService } from '@admin-ui/core';
import { AppStateService, IncrementDetailLoading, ResetDetailLoading } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ObjectPropertyCategoryBO, ObjectPropertyCategoryListOptions, Raw } from '@gentics/cms-models';
import { Observable, OperatorFunction } from 'rxjs';
import { tap } from 'rxjs/operators';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class ObjectPropertyCategoryDataService
    extends ExtendedEntityDataServiceBase<'objectPropertyCategory', ObjectPropertyCategoryOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: ObjectPropertyCategoryOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected permissionsService: PermissionsService,
    ) {
        super(
            'objectPropertyCategory',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: ObjectPropertyCategoryBO<Raw>): string {
        return entity.id;
    }

    getEntitiesFromApi(options?: ObjectPropertyCategoryListOptions): Observable<ObjectPropertyCategoryBO<Raw>[]> {
        this.state.dispatch(new IncrementDetailLoading());
        return this.entityOperations.getAll(options).pipe(
            tap(() => this.state.dispatch(new ResetDetailLoading())),
        );
    }

    protected getLoadingOperator<U>(options?: any): OperatorFunction<U, U> {
        return detailLoading(this.state);
    }
}
