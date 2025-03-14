import { detailLoading, LOAD_FLATTENED, masterLoading } from '@admin-ui/common';
import { EntityManagerService, GroupOperations, I18nNotificationService, I18nService, UserOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import {
    Group,
    GroupSetPermissionsRequest,
    GroupUserCreateRequest,
    ModelType,
    Normalized,
    PermissionsSet,
    Raw,
    User,
} from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { forkJoin, Observable, of, OperatorFunction } from 'rxjs';
import { first, map, switchMap, tap } from 'rxjs/operators';
import { EditPermissionsModalComponent } from '../../components/edit-permissions-modal/edit-permissions-modal.component';
import { ExtendedEntityDataServiceBase } from '../extended-entity-data-service-base/extended-entity-data.service.base';
import { wasClosedByUser } from '@gentics/cms-integration-api-models';

@Injectable()
export class GroupDataService extends ExtendedEntityDataServiceBase<'group', GroupOperations> {

    constructor(
        state: AppStateService,
        entityManager: EntityManagerService,
        entityOperations: GroupOperations,
        notification: I18nNotificationService,
        i18n: I18nService,
        protected userOperations: UserOperations,
        protected modalService: ModalService,
    ) {
        super(
            'group',
            state,
            entityManager,
            entityOperations,
            notification,
            i18n,
        );
    }

    getEntityId(entity: Group<Raw>): number {
        return entity.id;
    }

    override getEntitiesFromApi(options?: any): Observable<Group<ModelType.Raw>[]> {
        if (options?.[LOAD_FLATTENED]) {
            return this.entityOperations.getFlattned().pipe(
                this.getLoadingOperator(),
            );
        }

        return this.entityOperations.getAll().pipe(
            this.getLoadingOperator(),
        );
    }

    createUser(user: GroupUserCreateRequest, userGroupIds: number[]): Observable<User<Normalized>> {
        // create in group and then assign to groups if provided more than one group id
        const groupIdsToBeAssigned = [...userGroupIds];
        const initialGroupId = groupIdsToBeAssigned.shift();
        return this.entityOperations.createUser(initialGroupId, user).pipe(
            switchMap((newUser: User<Raw>) => {
                // if additional groups to assign
                if (groupIdsToBeAssigned.length > 0) {
                    return forkJoin([
                        of(newUser),
                        forkJoin(groupIdsToBeAssigned.map(groupId => this.userOperations.addToGroup(newUser.id, groupId))),
                    ]).pipe(map(([newUserWithGroups]: [User<Raw>, Group<Raw>[]]) => newUserWithGroups));
                } else {
                    // just return user
                    return of(newUser);
                }
            }),
            // return created user with all assigned groups from state
            switchMap((newUser: User<Raw>) => this.entityManager.getEntity('user', newUser.id).pipe(first())),
        );
    }

    /**
     * Displays a modal for editing the specified `PermissionsSet` and saves the changes if the user clicks 'Save'.
     *
     * @returns An Observable that emits `true` if the user clicked 'Save' and the operation was successful or
     * `false` if the user clicked 'Cancel'.
     */
    async editGroupPermissions(group: Group, permSet: PermissionsSet, groupPermsByCategory: boolean = true): Promise<false | {
        subGroups: boolean;
        subObjects: boolean;
    }> {
        const denormalizedGroup = this.entityManager.denormalizeEntity('group', group);
        let changes: GroupSetPermissionsRequest;

        try {
            const modal = await this.modalService.fromComponent(EditPermissionsModalComponent, {}, {
                group: denormalizedGroup, permSet, groupPermsByCategory,
            });
            changes = await modal.open();
        } catch (err) {
            if (wasClosedByUser(err)) {
                return false;
            }
            throw err;
        }

        if (!changes) {
            return false;
        }

        let req$: Observable<Group<Normalized>>;
        if (typeof permSet.id === 'number') {
            req$ = this.entityOperations.setGroupInstancePermissions(group.id, permSet.type, permSet.id, changes);
        } else {
            req$ = this.entityOperations.setGroupTypePermissions(group.id, permSet.type, changes);
        }

        return await req$.pipe(
            // detailLoading(this.state, 'shared.loading_update_group_permissions'),
            map(() => ({
                subGroups: changes.subGroups,
                subObjects: changes.subObjects,
            })),
        )
            .toPromise();
    }

    protected getLoadingOperator<U>(): OperatorFunction<U, U> {
        return masterLoading(this.state);
    }

    public getGroupPermissions(): Observable<{[key: number]: string[]}> {
        return this.entityOperations.getGroupPermissions();
    }
}
