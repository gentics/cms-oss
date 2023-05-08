import { EntityManagerService, GroupOperations, I18nNotificationService, I18nService, UserOperations } from '@admin-ui/core';
import { AppStateService, SelectState } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Group, NormalizableEntityType, Normalized, Raw } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { combineLatest, forkJoin, Observable, of, OperatorFunction } from 'rxjs';
import { filter, first, map, mergeMap, switchMap } from 'rxjs/operators';
import { detailLoading } from '../../../common/utils/rxjs-loading-operators/detail-loading.operator';
import { GroupDataService } from '../group-data/group-data.service';

@Injectable()
export class SubgroupDataService extends GroupDataService {

    @SelectState(state => state.ui.focusEntityType)
    focusEntityType$: Observable<NormalizableEntityType>;

    @SelectState(state => state.ui.focusEntityId)
    focusEntityId$: Observable<number>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: GroupOperations,
        userOperations: UserOperations,
        modalService: ModalService,
        notification: I18nNotificationService,
        i18n: I18nService,
    ) {
        super(
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
            userOperations,
            modalService,
        );
    }

    getParentEntityId(): Observable<number> {
        return combineLatest([
            this.focusEntityType$,
            this.focusEntityId$,
        ]).pipe(
            map(([focusEntityType, focusEntityId]) => focusEntityType === 'group' ? focusEntityId : undefined),
            filter((id: number | undefined) => id != null),
        );
    }

    getEntitiesFromApi(): Observable<Group<Raw>[]> {
        return this.getParentEntityId().pipe(
            filter((parentGroupId: number | undefined) => Number.isInteger(parentGroupId)),
            mergeMap((parentId: number) => {
                if (Number.isInteger(parentId)) {
                    return this.entityOperations.getSubgroups(parentId).pipe(this.getLoadingOperator());
                } else {
                    // if no parentGroupId is not provided, get all
                    return this.entityOperations.getAll().pipe(this.getLoadingOperator());
                }
            }),
        );
    }

    getRawEntitiesFromState(): Observable<Group<Raw>[]> {
        return this.getParentEntityId().pipe(
            filter((parentGroupId: number | undefined) => Number.isInteger(parentGroupId)),
            mergeMap((parentId: number) => {
                if (Number.isInteger(parentId)) {
                    return this.getSubGroupsFromState();
                } else {
                    // if no parentGroupId is not provided, get all
                    return super.getEntitiesFromApi();
                }
            }),
        );
    }

    private getSubGroupsFromState(): Observable<Group<Raw>[]> {
        return this.ensureEntitiesLoaded().pipe(
            switchMap(() => this.getParentEntityId()),
            filter((parentId: number | null) => Number.isInteger(parentId)),
            mergeMap((parentId: number) => this.entityManager.getEntity('group', parentId).pipe(
                map((parent: Group<Normalized> | undefined) => {
                    if (Array.isArray(parent?.children)) {
                        return parent.children;
                    } else {
                        // return empty array to refresh lists
                        return [];
                    }
                }),
            )),
            mergeMap((childrenIds: number[]) => {
                if (childrenIds.length > 0) {
                    return forkJoin(childrenIds.map(id => this.entityManager.getEntity('group', id).pipe(first())));
                } else {
                    // return empty array to refresh lists
                    return of([]);
                }
            }),
            map((children: Group<Normalized>[]) => {
                if (Array.isArray(children) && children.length > 0) {
                    return children.map(childGroup => this.entityManager.denormalizeEntity('group', childGroup));
                } else {
                    return [];
                }
            }),
        );
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return detailLoading(this.state);
    }
}
