import { detailLoading } from '@admin-ui/common';
import { EntityManagerService, TemplateTagStatusOperations } from '@admin-ui/core';
import { AppStateService, IncrementDetailLoading, ResetDetailLoading, SelectState } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { I18nNotificationService } from '@gentics/cms-components';
import { EntityIdType, NormalizableEntityType, TagStatusBO } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { I18nService } from '@gentics/cms-components';
import { combineLatest, Observable, OperatorFunction } from 'rxjs';
import { filter, map, switchMap, tap } from 'rxjs/operators';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class TemplateTagStatusDataService extends ExtendedEntityDataServiceBase<'templateTagStatus', TemplateTagStatusOperations> {

    @SelectState((state) => state.ui.focusEntityType)
    focusEntityType$: Observable<NormalizableEntityType>;

    @SelectState((state) => state.ui.focusEntityId)
    focusEntityId$: Observable<string>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: TemplateTagStatusOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected api: GcmsApi,
    ) {
        super(
            'templateTagStatus',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    entities: { [name: string]: TagStatusBO } = {};

    getParentEntityId(): Observable<string> {
        return combineLatest([
            this.focusEntityType$,
            this.focusEntityId$,
        ]).pipe(
            map(([focusEntityType, focusEntityId]) => focusEntityType === 'template' ? focusEntityId : undefined),
            filter((id: string | undefined) => id != null),
        );
    }

    /**
     * @returns The unique ID of the specified entity.
     */
    getEntityId(entity: TagStatusBO): EntityIdType {
        return entity.name;
    }

    /**
     * @returns The entity with the specified `id` or `undefined` if the ID
     * is unknown.
     */
    getEntity(id: EntityIdType): TagStatusBO {
        return this.entities[id];
    }

    getEntitiesFromApi(options?: null): Observable<TagStatusBO[]> {
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
