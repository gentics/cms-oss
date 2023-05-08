import { detailLoading, LOAD_FROM_PACKAGE, masterLoading } from '@admin-ui/common';
import { DataSourceOperations, EntityManagerService, I18nNotificationService, I18nService, PermissionsService } from '@admin-ui/core';
import { AppStateService, SelectState, UIStateModel } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { DataSourceBO, DataSourceListOptions, Raw } from '@gentics/cms-models';
import { Observable, OperatorFunction } from 'rxjs';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class DataSourceDataService extends ExtendedEntityDataServiceBase<'dataSource', DataSourceOperations> {

    @SelectState(state => state.ui)
    stateUi$: Observable<UIStateModel>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: DataSourceOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected permissionsService: PermissionsService,
    ) {
        super(
            'dataSource',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: DataSourceBO<Raw>): string {
        return entity.id;
    }

    override getEntitiesFromApi(options?: DataSourceListOptions): Observable<DataSourceBO<Raw>[]> {
        if (options?.[LOAD_FROM_PACKAGE]) {
            return this.entityOperations.getAllFromPackage(options[LOAD_FROM_PACKAGE], options).pipe(
                detailLoading(this.state),
            );
        }

        return super.getEntitiesFromApi(options);
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return masterLoading(this.state);
    }

}
