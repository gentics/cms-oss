import { detailLoading, discard } from '@admin-ui/common';
import { EntityManagerService, I18nNotificationService, I18nService, TemplateTagOperations } from '@admin-ui/core';
import { AppStateService, IncrementDetailLoading, ResetDetailLoading, SelectState } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { EntityIdType, NormalizableEntityType, TemplateSaveRequest, TemplateTag } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { combineLatest, Observable, OperatorFunction } from 'rxjs';
import { filter, map, switchMap, tap } from 'rxjs/operators';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class TemplateTagDataService extends ExtendedEntityDataServiceBase<'templateTag', TemplateTagOperations> {

    @SelectState(state => state.ui.focusEntityType)
    focusEntityType$: Observable<NormalizableEntityType>;

    @SelectState(state => state.ui.focusEntityId)
    focusEntityId$: Observable<string>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: TemplateTagOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected api: GcmsApi,
    ) {
        super(
            'templateTag',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    entities: { [name: string]: TemplateTag } = {};

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
    getEntityId(entity: TemplateTag): EntityIdType {
        return entity.name;
    }

    /**
     * @returns The entity with the specified `id` or `undefined` if the ID
     * is unknown.
     */
    getEntity(id: EntityIdType): TemplateTag {
        return this.entities[id];
    }

    getEntitiesFromApi(options?: null): Observable<TemplateTag[]> {
        this.state.dispatch(new IncrementDetailLoading());
        return this.getParentEntityId().pipe(
            switchMap((parentId: string) => {
                return this.entityOperations.getAll(null, parentId).pipe(
                    tap(() => this.state.dispatch(new ResetDetailLoading())),
                );
            }),
        );
    }

    updateEntity(templateId: string | number, body: TemplateSaveRequest): Observable<void> {
        return this.entityOperations.update(templateId, body).pipe(
            discard(),
        );
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return detailLoading(this.state);
    }
}
