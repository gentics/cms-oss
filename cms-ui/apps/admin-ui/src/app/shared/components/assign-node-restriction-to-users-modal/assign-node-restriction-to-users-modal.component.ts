import { I18nService } from '@admin-ui/core';
import { GroupUserDataService } from '@admin-ui/shared/providers/group-user-data/group-user-data.service';
import { AfterViewInit, ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { UserGroupNodeRestrictionsResponse } from '@gentics/cms-models';
import { BaseModal, IModalDialog } from '@gentics/ui-core';
import { BehaviorSubject, Observable } from 'rxjs';
import { delay, tap } from 'rxjs/operators';

@Component({
    selector: 'gtx-assign-node-restriction-to-users-modal',
    templateUrl: './assign-node-restriction-to-users-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignNodeRestrictionsToUsersModalComponent extends BaseModal<UserGroupNodeRestrictionsResponse> implements OnInit {

    /** IDs of users to be (un)assigned to/from groups */
    userId: number;

    /** ID of group to be (un)assigned to/from users */
    groupId: number;

    /** IDs of nodes to be (un)assigned to/from groups */
    nodeIdsInitial: number[];
    nodeIdsSelected: number[];

    /** Is TRUE if a logged-in user examines Node restrictions of a user whose restricted Nodes they're not have permission to read. */
    hiddenNodeIdsExist$ = new BehaviorSubject<boolean>(false);

    constructor(
        private groupUserData: GroupUserDataService,
        private i18n: I18nService,
        protected changeDetector: ChangeDetectorRef,
    ) {
        super();
    }

    ngOnInit(): void {
        this.groupUserData.getUserNodeRestrictions(this.userId, this.groupId).pipe(
            delay(0),
        ).subscribe((res: UserGroupNodeRestrictionsResponse) => {
            this.nodeIdsSelected = res.nodeIds;
            this.nodeIdsInitial = res.nodeIds;
            this.changeDetector.markForCheck();
        });
    }

    /**
     * If user clicks "assign"
     */
    buttonAssignNodeRestrictonsClicked(): void {
        this.groupUserData.changeUserNodeRestrictions(this.userId, this.groupId, this.nodeIdsSelected, this.nodeIdsInitial)
            .toPromise()
            .then(restrictions => {
                this.closeFn(restrictions);
            });
    }
}
