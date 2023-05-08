import { detailLoading, LOAD_FROM_PACKAGE } from '@admin-ui/common';
import { masterLoading } from '@admin-ui/common/utils/rxjs-loading-operators/master-loading.operator';
import { ContentRepositoryFragmentOperations, EntityManagerService, I18nNotificationService, I18nService } from '@admin-ui/core';
import { AppStateService, SelectState, UIStateModel } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { ContentRepositoryFragmentBO, Raw } from '@gentics/cms-models';
import { Observable, OperatorFunction } from 'rxjs';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class ContentRepositoryFragmentDataService
    extends ExtendedEntityDataServiceBase<'contentRepositoryFragment', ContentRepositoryFragmentOperations> {

    @SelectState(state => state.ui)
    stateUi$: Observable<UIStateModel>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: ContentRepositoryFragmentOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(
            'contentRepositoryFragment',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: ContentRepositoryFragmentBO<Raw>): string {
        return entity.id;
    }

    override getEntitiesFromApi(options?: any): Observable<ContentRepositoryFragmentBO<Raw>[]> {
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
