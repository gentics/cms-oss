import { PortalImplementationTableLoaderService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { BaseModal, IModalDialog } from '@gentics/ui-core';
import { firstValueFrom } from 'rxjs';

@Component({
    selector: 'gtx-assign-portal-implementations-to-node-modal',
    templateUrl: './assign-portal-implementations-to-node-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class AssignPortalImplementationsToNodeModalComponent extends BaseModal<boolean> implements IModalDialog, OnInit {

    /** ID of the node, to which the portal implementations are assigned */
    nodeId: number;

    /** IDs of the portal implementations assigned to the node, when the modal was opened */
    portalImplementationIdsInitial: string[] = [];

    /** IDs of the currently selected portal implementations */
    portalImplementationIdsCurrent: string[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private api: GcmsApi,
        private loader: PortalImplementationTableLoaderService,
    ) {
        super();
    }

    async ngOnInit(): Promise<void> {
        const assigned = await firstValueFrom(this.api.node.getNodePortalImplementations(this.nodeId, { pageSize: -1 }));
        // the IDs must match the IDs of the table rows (see PortalImplementationTableLoaderService.mapToBusinessObject)
        this.portalImplementationIdsInitial = assigned.items.map(item => String(item.id));
        this.portalImplementationIdsCurrent = [...this.portalImplementationIdsInitial];
        this.changeDetector.markForCheck();
    }

    /**
     * If user clicks "assign"
     */
    async buttonAssignPortalImplementationsToNodeClicked(): Promise<void> {
        const toAdd = this.portalImplementationIdsCurrent.filter(id => !this.portalImplementationIdsInitial.includes(id));
        const toRemove = this.portalImplementationIdsInitial.filter(id => !this.portalImplementationIdsCurrent.includes(id));

        for (const id of toAdd) {
            await firstValueFrom(this.loader.addToNode(this.nodeId, id));
        }
        for (const id of toRemove) {
            await firstValueFrom(this.loader.removeFromNode(this.nodeId, id));
        }

        this.closeFn(toAdd.length > 0 || toRemove.length > 0);
    }
}
