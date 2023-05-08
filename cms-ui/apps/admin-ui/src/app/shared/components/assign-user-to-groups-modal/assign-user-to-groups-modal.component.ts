import { I18nService } from '@admin-ui/core';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Normalized, Raw, User } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { mergeMap } from 'rxjs/operators';
import { UserDataService } from '../../providers/user-data/user-data.service';

@Component({
    selector: 'gtx-assign-user-to-groups-modal',
    templateUrl: './assign-user-to-groups-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignUserToGroupsModalComponent extends BaseModal<User<Raw>[] | boolean> {

    /** IDs of users to be (un)assigned to/from groups */
    userIds: number[] = [];

    /** IDs of groups to which the users shall finally be assigned to */
    userGroupIds: number[] = [];

    constructor(
        private userData: UserDataService,
        private i18n: I18nService,
    ) {
        super();
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

    /**
     * @param replace if the selected groupIds should replace the existing groups
     */
    private changeGroupsOfUsers(replace: boolean = false): Promise<User<Raw>[] | boolean> {
        return this.userData.changeGroupsOfUsers(this.userIds, this.userGroupIds, replace).toPromise();
    }

}
