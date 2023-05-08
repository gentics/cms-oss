import { detailLoading } from '@admin-ui/common';
import { EntityManagerService, GroupOperations, I18nNotificationService, I18nService, UserOperations } from '@admin-ui/core';
import { AppStateService, SelectState } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { Group, NormalizableEntityType, Normalized, Raw, User } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { combineLatest, Observable, OperatorFunction } from 'rxjs';
import { filter, map, mergeMap, switchMap } from 'rxjs/operators';
import { GroupDataService } from '../group-data/group-data.service';
import { UserDataService } from '../user-data/user-data.service';

@Injectable()
export class UserGroupDataService extends GroupDataService {

    @SelectState(state => state.ui.focusEntityType)
    focusEntityType$: Observable<NormalizableEntityType>;

    @SelectState(state => state.ui.focusEntityId)
    focusEntityId$: Observable<number>;

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: GroupOperations,
        userOperations: UserOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        modalService: ModalService,
        protected userData: UserDataService,
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
            map(([focusEntityType, focusEntityId]) => focusEntityType === 'user' ? focusEntityId : undefined),
            filter((id: number | undefined) => id != null),
        );
    }

    getEntitiesFromApi(): Observable<Group<Raw>[]> {
        return this.getParentEntityId().pipe(
            filter((parentUserId: number | undefined) => Number.isInteger(parentUserId)),
            switchMap((parentUserId: number) => this.userOperations.groups(parentUserId).pipe(this.getLoadingOperator())),
        );
    }

    getRawEntitiesFromState(): Observable<Group<Raw>[]> {
        return combineLatest([
            this.getParentEntityId().pipe(
                mergeMap((parentUserId: number) => this.userData.getEntityFromState(parentUserId)),
            ),
            super.getRawEntitiesFromState(),
        ]).pipe(
            filter(([parentUser]: [User<Normalized>, Group<Raw>[]]) => Array.isArray(parentUser?.groups)),
            map(([parentUser, groups]: [User<Normalized>, Group<Raw>[]]) => groups.filter(group => parentUser.groups.includes(group.id))),
        );
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return detailLoading(this.state);
    }
}
