import { detailLoading } from '@admin-ui/common/utils/rxjs-loading-operators/detail-loading.operator';
import { EntityManagerService, I18nNotificationService, I18nService } from '@admin-ui/core';
import { DataSourceEntryOperations } from '@admin-ui/core/providers/operations/datasource-entry';
import { AppStateService, IncrementDetailLoading, ResetDetailLoading, SelectState } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { DataSourceEntryBO, NormalizableEntityType, Raw } from '@gentics/cms-models';
import { BehaviorSubject, combineLatest, Observable, OperatorFunction, Subject } from 'rxjs';
import { filter, map, startWith, switchMap, tap } from 'rxjs/operators';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class DataSourceEntryDataService extends ExtendedEntityDataServiceBase<'dataSourceEntry', DataSourceEntryOperations> {

    @SelectState(state => state.ui.focusEntityType)
    focusEntityType$: Observable<NormalizableEntityType>;

    @SelectState(state => state.ui.focusEntityId)
    focusEntityId$: Observable<string>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: DataSourceEntryOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(
            'dataSourceEntry',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getParentEntityId(): Observable<string> {
        return combineLatest([
            this.focusEntityType$,
            this.focusEntityId$,
        ]).pipe(
            map(([focusEntityType, focusEntityId]) => focusEntityType === 'dataSource' ? focusEntityId : undefined),
            filter((id: string | undefined) => id != null),
        );
    }

    getEntityId(entity: DataSourceEntryBO<Raw>): string {
        return entity.id;
    }

    getEntity(id: string): DataSourceEntryBO<Raw> {
        return { id } as DataSourceEntryBO<Raw>;
    }

    getEntitiesFromApi(options?: null): Observable<DataSourceEntryBO<Raw>[]> {
        this.state.dispatch(new IncrementDetailLoading());
        return this.getParentEntityId().pipe(
            switchMap((parentId: string) => {
                return this.entityOperations.getAll(null, parentId).pipe(
                    tap(() => this.state.dispatch(new ResetDetailLoading())),
                );
            }),
        );
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return detailLoading(this.state);
    }
}
