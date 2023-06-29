import { GroupBO } from '@admin-ui/common';
import { ErrorHandler, I18nService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { Feature, Normalized, Raw, User } from '@gentics/cms-models';
import { BaseModal, ModalService, TableAction, TableActionClickEvent } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { UserDataService } from '../../providers/user-data/user-data.service';
import { AssignNodeRestrictionsToUsersModalComponent } from '../assign-node-restriction-to-users-modal/assign-node-restriction-to-users-modal.component';

const ACTION_NODE_RESTRICTIONS = 'restrict-by-nodes';

@Component({
    selector: 'gtx-assign-user-to-groups-modal',
    templateUrl: './assign-user-to-groups-modal.component.html',
    styleUrls: ['./assign-user-to-groups-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignUserToGroupsModalComponent extends BaseModal<User<Raw>[] | boolean> implements OnInit, OnDestroy {

    /** IDs of users to be (un)assigned to/from groups */
    public userIds: number[] = [];

    /** IDs of groups to which the users shall finally be assigned to */
    public userGroupIds: number[] = [];

    public actions: TableAction<GroupBO>[] = [];

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private appState: AppStateService,
        private userData: UserDataService,
        private i18n: I18nService,
        private modalService: ModalService,
        private errorHandler: ErrorHandler,
    ) {
        super();
    }

    ngOnInit(): void {
        this.subscriptions.push(this.appState.select(state => state.features.global[Feature.MULTICHANNELLING]).subscribe(multiChanneling => {
            if (!multiChanneling && this.userIds.length === 1) {
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

        this.subscriptions.push()
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    /** Get form validity state */
    allIsValid(): boolean {
        return this.userGroupIds && this.userGroupIds.length > 0;
    }

    /**
     * If user clicks "assign"
     */
    buttonAssignUserToGroupsClicked(): void {
        this.changeGroupsOfUsers()
            .then(updatedUsers => this.closeFn(updatedUsers));
    }

    getModalTitle(): Observable<string> {
        if (this.userIds.length === 1) {
            return this.userData.getEntityFromState(this.userIds[0]).pipe(
                mergeMap((user: User<Normalized>) => this.i18n.get('shared.assign_user_to_groups_title', { entityName: user.login })),
            );
        } else {
            return this.i18n.get('shared.assign_users_to_groups');
        }
    }

    handleActionClick(event: TableActionClickEvent<GroupBO>): void {
        switch (event.actionId) {
            case ACTION_NODE_RESTRICTIONS:
                this.getUserNodeRestrictions(event.item.id);
                break;
        }
    }

    async getUserNodeRestrictions(groupId: number): Promise<void> {
        try {
            const dialog = await this.modalService.fromComponent(
                AssignNodeRestrictionsToUsersModalComponent,
                { closeOnOverlayClick: false, width: '50%' },
                { userId: this.userIds[0], groupId },
            );
            await dialog.open();
        } catch (err) {
            this.errorHandler.catch(err);
        }
    }


    /**
     * @param replace if the selected groupIds should replace the existing groups
     */
    private changeGroupsOfUsers(replace: boolean = false): Promise<User<Raw>[] | boolean> {
        return this.userData.changeGroupsOfUsers(this.userIds, this.userGroupIds, replace).toPromise();
    }

}
