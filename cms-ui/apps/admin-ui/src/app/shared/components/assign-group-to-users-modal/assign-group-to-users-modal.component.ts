import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnInit } from '@angular/core';
import { EntityIdType, Group } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { GroupUserDataService } from '../../providers/group-user-data/group-user-data.service';

@Component({
    selector: 'gtx-assign-group-to-users-modal',
    templateUrl: './assign-group-to-users-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignGroupToUsersModal extends BaseModal<void> implements OnInit {

    /** The group the users should get assigned to. */
    @Input()
    public group: Group;

    /** Initially selected users. Can be left empty */
    @Input()
    public userIds: EntityIdType[] = [];

    public loading = false;
    public selected: string[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private groupUserData: GroupUserDataService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.selected = (this.userIds || []).map(id => `${id}`);
    }

    async handleConfirm(): Promise<void> {
        this.loading = false;
        this.changeDetector.markForCheck();

        try {
            const updatedUsers = await this.changeUsersOfGroup();
            this.closeFn(updatedUsers);
        } finally {
            this.loading = false;
            this.changeDetector.markForCheck();
        }
    }

    private changeUsersOfGroup(): Promise<void> {
        return this.groupUserData.changeGroupOfUsers(this.group.id, this.selected.map(id => Number(id))).toPromise();
    }

}
