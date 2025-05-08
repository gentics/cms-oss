import { GroupUserDataService } from '@admin-ui/shared/providers/group-user-data/group-user-data.service';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { UserGroupNodeRestrictionsResponse } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Subscription } from 'rxjs';

@Component({
    selector: 'gtx-assign-node-restriction-to-users-modal',
    templateUrl: './assign-node-restriction-to-users-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class AssignNodeRestrictionsToUsersModalComponent
    extends BaseModal<UserGroupNodeRestrictionsResponse>
    implements OnInit, OnDestroy {

    /** IDs of users to be (un)assigned to/from groups */
    @Input()
    userId: number;

    /** ID of group to be (un)assigned to/from users */
    @Input()
    groupId: number;

    /** IDs of nodes to be (un)assigned to/from groups */
    nodeIdsInitial: number[];
    nodeIdsSelected: string[] = [];

    public loading = false;

    private subscriptions: Subscription[] = [];

    constructor(
        private groupUserData: GroupUserDataService,
        protected changeDetector: ChangeDetectorRef,
    ) {
        super();
    }

    ngOnInit(): void {
        this.subscriptions.push(this.groupUserData
            .getUserNodeRestrictions(this.userId, this.groupId)
            .subscribe((res: UserGroupNodeRestrictionsResponse) => {
                this.nodeIdsSelected = res.nodeIds.map(id => `${id}`);
                this.nodeIdsInitial = res.nodeIds;
                this.changeDetector.markForCheck();
            }),
        );
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    /**
     * If user clicks "assign"
     */
    async handleConfirm(): Promise<void> {
        this.loading = true;

        try {
            const restrictions = await this.groupUserData.changeUserNodeRestrictions(
                this.userId,
                this.groupId,
                this.nodeIdsSelected.map(id => Number(id)),
                this.nodeIdsInitial,
            ).toPromise();
            this.closeFn(restrictions);
        } catch (err) {
            this.loading = false;
            this.changeDetector.markForCheck();
        }
    }
}
