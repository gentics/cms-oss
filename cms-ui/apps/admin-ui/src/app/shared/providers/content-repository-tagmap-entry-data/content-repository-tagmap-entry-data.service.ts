import { detailLoading } from '@admin-ui/common';
import {
    ContentRepositoryFragmentTagmapEntryOperations,
    ContentRepositoryTagmapEntryOperations,
    EntityManagerService,
    I18nNotificationService,
    I18nService,
    PermissionsService,
} from '@admin-ui/core';
import { ChildGridDataProvider, Parent } from '@admin-ui/common';
import { AppStateService, IncrementDetailLoading, ResetDetailLoading, SelectState } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { NormalizableEntityType, NormalizableEntityTypesMapBO, Raw, TagmapEntryBO, TagmapEntryListOptions } from '@gentics/cms-models';
import { combineLatest, Observable, OperatorFunction } from 'rxjs';
import { filter, map, switchMap, tap } from 'rxjs/operators';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';

@Injectable()
export class ContentRepositoryTagmapEntryDataService
    extends ExtendedEntityDataServiceBase<'tagmapEntry', ContentRepositoryTagmapEntryOperations>
    implements ChildGridDataProvider<NormalizableEntityTypesMapBO<Raw>['tagmapEntry']> {

    @SelectState(state => state.ui.focusEntityType)
    focusEntityType$: Observable<NormalizableEntityType>;

    @SelectState(state => state.ui.focusEntityId)
    focusEntityId$: Observable<string>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        crOperations: ContentRepositoryTagmapEntryOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected fragmentOperations: ContentRepositoryFragmentTagmapEntryOperations,
        protected permissionsService: PermissionsService,
    ) {
        super(
            'tagmapEntry',
            state,
            entityManager,
            crOperations,
            notification,
            i18n,
        );
    }

    getParentEntity(): Observable<Parent> {
        return combineLatest([
            this.focusEntityType$,
            this.focusEntityId$,
        ]).pipe(
            map(([type, id]) => this.asParent(type, id)),
            filter((parent) => parent != null),
        );
    }

    getEntityId(entity: TagmapEntryBO<Raw>): string {
        return entity.id;
    }

    getEntitiesFromApi(options?: TagmapEntryListOptions): Observable<TagmapEntryBO<Raw>[]> {
        this.state.dispatch(new IncrementDetailLoading());
        return this.getParentEntity().pipe(
            switchMap(parent => {
                if (parent.type === 'contentRepository') {
                    return this.entityOperations.getAll(options, parent.id);
                } else {
                    return this.fragmentOperations.getAll(options, parent.id);
                }
            }),
            tap(() => this.state.dispatch(new ResetDetailLoading())),
        );
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return detailLoading(this.state);
    }

    private asParent(type: string, id: string): Parent | null {
        if (id == null || (type !== 'contentRepository' && type !== 'contentRepositoryFragment')) {
            return null;
        }

        return { type, id };
    }

}
