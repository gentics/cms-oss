import { detailLoading } from '@admin-ui/common';
import { DataSourceConstructOperations, EntityManagerService, I18nNotificationService, I18nService } from '@admin-ui/core';
import { AppStateService, IncrementDetailLoading, ResetDetailLoading, SelectState } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { DataSourceConstructListOptions, NormalizableEntityType, Raw, TagTypeBO } from '@gentics/cms-models';
import { combineLatest, Observable, OperatorFunction } from 'rxjs';
import { filter, map, switchMap, tap } from 'rxjs/operators';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class DataSourceConstructDataService extends ExtendedEntityDataServiceBase<'construct', DataSourceConstructOperations> {

    @SelectState(state => state.ui.focusEntityType)
    focusEntityType$: Observable<NormalizableEntityType>;

    @SelectState(state => state.ui.focusEntityId)
    focusEntityId$: Observable<string>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: DataSourceConstructOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(
            'construct',
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

    getEntityId(entity: TagTypeBO<Raw>): string {
        return entity.id;
    }

    getEntity(id: string): TagTypeBO<Raw> {
        return { id } as TagTypeBO<Raw>;
    }

    getEntitiesFromApi(options?: DataSourceConstructListOptions): Observable<TagTypeBO<Raw>[]> {
        this.state.dispatch(new IncrementDetailLoading());
        return this.getParentEntityId().pipe(
            switchMap((parentId: string) => {
                return this.entityOperations.getAll(options, parentId).pipe(
                    tap(() => this.state.dispatch(new ResetDetailLoading())),
                );
            }),
        );
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return detailLoading(this.state);
    }
}
