import { Injectable } from '@angular/core';
import { wasClosedByUser } from '@gentics/cms-integration-api-models';
import { NormalizableEntityType } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';
import { AssignEntityToPackageModalComponent } from '../../components/assign-entity-to-package-modal/assign-entity-to-package-modal.component';
import { AssignPackagesToNodeModalComponent } from '../../components/assign-packages-to-node-modal/assign-packages-to-node-modal.component';
import { AssignUserToGroupsModal } from '../../components/assign-user-to-groups-modal/assign-user-to-groups-modal.component';
import { NotificationService } from '../notification/notification.service';

@Injectable()
export class ContextMenuService {

    constructor(
        protected modalService: ModalService,
        protected notificationTools: NotificationService,
    ) { }

    changeGroupsOfUsersModalOpen(userIds: number[]): Promise<boolean> {
        // if no row is selected, display modal
        if (!userIds || userIds.length < 1) {
            this.notificationTools.notificationNoneSelected();
            return;
        }

        // open modal
        return this.modalService.fromComponent(
            AssignUserToGroupsModal,
            { closeOnOverlayClick: false, width: '50%' },
            { userIds },
        )
            .then(modal => modal.open())
            .catch(err => {
                if (wasClosedByUser(err)) {
                    return false;
                }
                throw err;
            });
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

    changePackagesOfNodeModalOpen(nodeId: number): Promise<boolean> {
        // open modal
        return this.modalService.fromComponent(
            AssignPackagesToNodeModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { nodeId },
        )
            .then(modal => modal.open());
    }
}
