import { ChangeDetectionStrategy, Component } from '@angular/core';
import { EntityIdType, Raw, User } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { GroupUserDataService } from '../../providers/group-user-data/group-user-data.service';

@Component({
    selector: 'gtx-assign-group-to-users-modal',
    templateUrl: './assign-group-to-users-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignGroupToUsersModalComponent extends BaseModal<User<Raw>[] | boolean> {

    /** ID of group to be (un)assigned to/from users */
    groupId: number;

    /** IDs of users to be (un)assigned to/from groups */
    userIds: EntityIdType[];

    constructor(
        private groupUserData: GroupUserDataService,
    ) {
        super();
    }

    /** Get form validity state */
    allIsValid(): boolean {
        return this.userIds && this.userIds.length > 0;
    }

    /**
     * If user clicks "assign"
     */
    buttonAssignGroupToUsersClicked(): void {
        this.changeUsersOfGroup()
            .then(updatedUsers => this.closeFn(updatedUsers));
    }

    private changeUsersOfGroup(): Promise<User<Raw>[] | boolean> {
        return this.groupUserData.changeGroupOfUsers(this.groupId, this.userIds.map(id => Number(id))).toPromise();
    }

}
