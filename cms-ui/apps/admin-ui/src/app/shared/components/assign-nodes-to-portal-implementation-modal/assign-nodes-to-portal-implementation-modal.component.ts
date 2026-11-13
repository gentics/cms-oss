import { PortalImplementationBO } from '@admin-ui/common';
import { PortalImplementationHandlerService, PortalImplementationOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { BaseModal, IModalDialog } from '@gentics/ui-core';
import { firstValueFrom } from 'rxjs';

/**
 * Modal for changing the nodes, a portal implementation is assigned to
 */
@Component({
    selector: 'gtx-assign-nodes-to-portal-implementation-modal',
    templateUrl: './assign-nodes-to-portal-implementation-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class AssignNodesToPortalImplementationModalComponent extends BaseModal<boolean> implements IModalDialog, OnInit {

    /** The portal implementation */
    portalImplementation: PortalImplementationBO;

    /** IDs of the nodes, the portal implementation was assigned to, when the modal was opened */
    nodeIdsInitial: string[] = [];

    /** IDs of the currently selected nodes */
    nodeIdsSelected: string[] = [];

    /** Will be set while loading or saving */
    loading = true;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private handler: PortalImplementationHandlerService,
        private operations: PortalImplementationOperations,
    ) {
        super();
    }

    async ngOnInit(): Promise<void> {
        const nodes = await firstValueFrom(this.handler.getAssignedNodes(this.portalImplementation.id));
        // the IDs must match the IDs of the rows in the node table
        this.nodeIdsInitial = nodes.map(node => String(node.id));
        this.nodeIdsSelected = [...this.nodeIdsInitial];
        this.loading = false;
        this.changeDetector.markForCheck();
    }

    /**
     * If the user clicks "assign": assign the newly selected nodes and remove the deselected nodes
     */
    async buttonAssignClicked(): Promise<void> {
        const toAdd = this.nodeIdsSelected.filter(id => !this.nodeIdsInitial.includes(id));
        const toRemove = this.nodeIdsInitial.filter(id => !this.nodeIdsSelected.includes(id));

        this.loading = true;
        this.changeDetector.markForCheck();

        try {
            for (const nodeId of toAdd) {
                await firstValueFrom(this.operations.addToNode(Number(nodeId), this.portalImplementation.id));
            }
            for (const nodeId of toRemove) {
                await firstValueFrom(this.operations.removeFromNode(Number(nodeId), this.portalImplementation.id));
            }
            this.closeFn(toAdd.length > 0 || toRemove.length > 0);
        } catch (err) {
            // errors are shown by the operations, the modal stays open
            this.loading = false;
            this.changeDetector.markForCheck();
        }
    }
}
