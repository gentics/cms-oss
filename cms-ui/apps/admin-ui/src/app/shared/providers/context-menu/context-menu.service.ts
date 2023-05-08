import { AppStateService } from '@admin-ui/state';
import { Injectable } from '@angular/core';
import { NormalizableEntityType } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { AssignEntityToPackageModalComponent } from '../../components/assign-entity-to-package-modal/assign-entity-to-package-modal.component';
import { AssignGroupToUsersModalComponent } from '../../components/assign-group-to-users-modal/assign-group-to-users-modal.component';
import { AssignPackagesToNodeModalComponent } from '../../components/assign-packages-to-node-modal/assign-packages-to-node-modal.component';
import { AssignUserToGroupsModalComponent } from '../../components/assign-user-to-groups-modal/assign-user-to-groups-modal.component';
import { NotificationService } from '../notification/notification.service';

@Injectable()
export class ContextMenuService {

    constructor(
        protected state: AppStateService,
        protected modalService: ModalService,
        protected notificationTools: NotificationService,
    ) { }

    changeGroupsOfUsersModalOpen(userIds: number[]): Promise<AssignUserToGroupsModalComponent> {
        // if no row is selected, display modal
        if (!userIds || userIds.length < 1) {
            this.notificationTools.notificationNoneSelected();
            return;
        }

        // if only one user, preselect groups
        let userGroupIds: number[] = [];
        if (userIds && userIds.length === 1) {
            const selectedUser = this.state.now.entity.user[userIds[0]];
            userGroupIds = [...selectedUser.groups];
        }

        // open modal
        return this.modalService.fromComponent(
            AssignUserToGroupsModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { userIds, userGroupIds },
        )
            .then(modal => modal.open());
    }

    changeUsersOfGroupModalOpen(groupId: number): Promise<AssignGroupToUsersModalComponent> {
        // Pre-collect users which has the group already
        const userIds = Object.values(this.state.now.entity.user)
            .filter(user => user.groups && user.groups.includes(groupId))
            .map(user => user.id);

        // open modal
        return this.modalService.fromComponent(
            AssignGroupToUsersModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { groupId, userIds },
        )
            .then(modal => modal.open());
    }

    assignEntityToPackageModalOpen(packageId: string, entityIdentifier: NormalizableEntityType): Promise<void> {
        // open modal
        return this.modalService.fromComponent(
            AssignEntityToPackageModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { packageId, entityIdentifier },
        )
            .then(modal => modal.open());
    }

    changePackagesOfNodeModalOpen(nodeId: number): Promise<AssignPackagesToNodeModalComponent> {
        // open modal
        return this.modalService.fromComponent(
            AssignPackagesToNodeModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { nodeId },
        )
            .then(modal => modal.open());
    }
}
