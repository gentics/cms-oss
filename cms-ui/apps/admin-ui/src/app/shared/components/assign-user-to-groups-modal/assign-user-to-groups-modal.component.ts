import { GroupBO } from '@admin-ui/common';
import { ErrorHandler, GroupOperations, I18nNotificationService, I18nService, UserOperations } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { wasClosedByUser } from '@gentics/cms-integration-api-models';
import { Feature, Group, Raw, User } from '@gentics/cms-models';
import { BaseModal, CHECKBOX_STATE_INDETERMINATE, ModalService, TableAction, TableActionClickEvent, TableSelection, toSelectionArray } from '@gentics/ui-core';
import { combineLatest, forkJoin, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { AssignNodeRestrictionsToUsersModalComponent } from '../assign-node-restriction-to-users-modal/assign-node-restriction-to-users-modal.component';

const ACTION_NODE_RESTRICTIONS = 'restrict-by-nodes';

@Component({
    selector: 'gtx-assign-user-to-groups-modal',
    templateUrl: './assign-user-to-groups-modal.component.html',
    styleUrls: ['./assign-user-to-groups-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class AssignUserToGroupsModal extends BaseModal<boolean> implements OnInit, OnDestroy {

    /** IDs of users to be (un)assigned to/from groups */
    @Input()
    public userIds: number[] = [];

    public selected: TableSelection = {};
    public actions: TableAction<GroupBO>[] = [];

    public loading = false;

    protected users: Record<number, User<Raw>> = {};
    /**
     * @key groupId
     */
    protected currentAssignment: Record<number, Set<number>> = null;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: AppStateService,
        private userOps: UserOperations,
        private groupsOps: GroupOperations,
        private i18n: I18nService,
        private modalService: ModalService,
        private errorHandler: ErrorHandler,
        private notifications: I18nNotificationService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.subscriptions.push(combineLatest([
            this.groupsOps.getFlattned(),
            forkJoin(
                this.userIds.map(id => this.userOps.get(id)),
            ),
            forkJoin(
                this.userIds.map(id => this.userOps.groups(id).pipe(
                    map(groups => [id, groups] as [number, Group<Raw>[]]),
                )),
            ),
        ]).subscribe(([allGroups, loadedUsers, entries]) => {
            const assignment: Record<number, Set<number>> = {};
            const newSelection: TableSelection = {};

            for (const user of loadedUsers) {
                this.users[user.id] = user;
            }

            // Create a reverse mapping
            for (const [userId, userGroups] of entries) {
                for (const group of userGroups) {
                    if (!assignment[group.id]) {
                        assignment[group.id] = new Set();
                    }
                    assignment[group.id].add(userId);
                }
            }

            // Check each groups selection state
            for (const group of allGroups) {
                const userCount = assignment[group.id]?.size ?? 0;

                switch (userCount) {
                    case 0:
                        newSelection[group.id] = false;
                        break;

                    case this.userIds.length:
                        newSelection[group.id] = true;
                        break;

                    default:
                        newSelection[group.id] = CHECKBOX_STATE_INDETERMINATE;
                        break;
                }
            }

            // Apply new values
            this.selected = newSelection;
            this.currentAssignment = assignment;
            this.changeDetector.markForCheck();
        }));

        this.subscriptions.push(this.appState.select(state => state.features.global[Feature.MULTICHANNELLING]).subscribe(multiChanneling => {
            if (!multiChanneling || this.userIds.length !== 1) {
                this.actions = [];
                this.changeDetector.markForCheck();
                return;
            }

            this.actions = [
                {
                    id: ACTION_NODE_RESTRICTIONS,
                    label: this.i18n.instant('common.assign_user_node_restrictions'),
                    enabled: true,
                    icon: 'lock',
                    type: 'secondary',
                    single: true,
                },
            ];
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    /**
     * If user clicks "assign"
     */
    async handleConfirm(): Promise<void> {
        this.closeFn(await this.changeGroupsOfUsers());
    }

    handleActionClick(event: TableActionClickEvent<GroupBO>): void {
        switch (event.actionId) {
            case ACTION_NODE_RESTRICTIONS:
                this.getUserNodeRestrictions(event.item.id);
                break;
        }
    }

    async getUserNodeRestrictions(groupId: number): Promise<void> {
        this.loading = true;
        this.changeDetector.markForCheck();

        try {
            const dialog = await this.modalService.fromComponent(
                AssignNodeRestrictionsToUsersModalComponent,
                { closeOnOverlayClick: false, width: '50%' },
                { userId: this.userIds[0], groupId },
            );
            await dialog.open();

            this.loading = false;
            this.changeDetector.markForCheck();
        } catch (err) {
            if (!wasClosedByUser(err)) {
                this.errorHandler.catch(err);
            }
            this.loading = false;
            this.changeDetector.markForCheck();
        }
    }

    private async changeGroupsOfUsers(): Promise<boolean> {
        this.loading = true;
        this.changeDetector.markForCheck();
        let didChange = false;

        for (const userId of this.userIds) {
            const toAdd = new Set<number>(toSelectionArray(this.selected).map(Number));
            const toRemove = new Set<number>(toSelectionArray(this.selected, false).map(Number));

            for (const groupToAdd of toAdd) {
                if (this.currentAssignment[groupToAdd]?.has?.(userId)) {
                    continue;
                }

                try {
                    await this.userOps.addToGroup(userId, groupToAdd).toPromise();
                    didChange = true;
                } catch (err) {
                    this.errorHandler.catch(err);
                    // TODO: Handle error
                }
            }

            for (const groupToRemove of toRemove) {
                if (!this.currentAssignment[groupToRemove]?.has?.(userId)) {
                    continue;
                }

                try {
                    await this.userOps.removeFromGroup(userId, groupToRemove).toPromise();
                    didChange = true;
                } catch (err) {
                    this.errorHandler.catch(err);
                    // TODO: Handle error
                }
            }

            this.notifications.show({
                message: 'shared.assign_users_to_groups_success',
                translationParams: {
                    entityName: `${this.users[userId].firstName} ${this.users[userId].lastName}`,
                },
                type: 'success',
            });
        }

        this.loading = false;
        this.changeDetector.markForCheck();

        return didChange;
    }
}
