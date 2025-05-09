import { GroupBO } from '@admin-ui/common';
import { ErrorHandler, I18nService } from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { wasClosedByUser } from '@gentics/cms-integration-api-models';
import { Feature, Raw, User } from '@gentics/cms-models';
import { BaseModal, ModalService, TableAction, TableActionClickEvent } from '@gentics/ui-core';
import { Subscription } from 'rxjs';
import { UserDataService } from '../../providers/user-data/user-data.service';
import { AssignNodeRestrictionsToUsersModalComponent } from '../assign-node-restriction-to-users-modal/assign-node-restriction-to-users-modal.component';

const ACTION_NODE_RESTRICTIONS = 'restrict-by-nodes';

@Component({
    selector: 'gtx-assign-user-to-groups-modal',
    templateUrl: './assign-user-to-groups-modal.component.html',
    styleUrls: ['./assign-user-to-groups-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignUserToGroupsModal extends BaseModal<User<Raw>[] | boolean> implements OnInit, OnDestroy {

    /** IDs of users to be (un)assigned to/from groups */
    @Input()
    public userIds: number[] = [];

    /** IDs of groups to which the users shall finally be assigned to */
    @Input()
    public userGroupIds: number[] = [];

    @Input()
    public user: User;

    public selected: string[] = [];
    public actions: TableAction<GroupBO>[] = [];

    public loading = false;

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
        this.selected = this.userGroupIds.map(id => `${id}`);
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
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    /**
     * If user clicks "assign"
     */
    async handleConfirm(): Promise<void> {
        this.loading = true;
        this.changeDetector.markForCheck();

        try {
            const updatedUsers = await this.changeGroupsOfUsers();

            this.closeFn(updatedUsers);
        } catch (err) {
            this.loading = false;
            this.changeDetector.markForCheck();
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

    private changeGroupsOfUsers(): Promise<User<Raw>[] | boolean> {
        return this.userData.changeGroupsOfUsers(this.userIds, this.selected.map(id => Number(id)), true).toPromise();
    }

}
