import { ErrorHandler, NodeOperations, ObjectPropertyHandlerService } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { IndexById, Node, Raw } from '@gentics/cms-models';
import { BaseModal, CHECKBOX_STATE_INDETERMINATE, TableSelection, toSelectionArray } from '@gentics/ui-core';
import { forkJoin, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
    selector: 'gtx-assign-node-restriction-to-object-properties-modal',
    templateUrl: './assign-node-restriction-to-object-properties-modal.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignNodeRestrictionsToObjectPropertiesModalComponent extends BaseModal<boolean> implements OnInit, OnDestroy {

    /** IDs of objectproperties to be (un)assigned to/from groups */
    @Input()
    public objectProperties: number[] = [];

    public loading = false;
    public selected: TableSelection = {};

    protected nodes: IndexById<Node<Raw>> = {};
    /**
     * @key nodeId
     */
    protected currentAssignment: Record<number, Set<number>> = null;

    private subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        private handler: ObjectPropertyHandlerService,
        private nodeOps: NodeOperations,
        private errorHandler: ErrorHandler,
    ) {
        super();
    }

    ngOnInit(): void {
        this.subscriptions.push(forkJoin([
            this.nodeOps.getAll(),
            forkJoin(this.objectProperties.map(op => this.handler.getLinkedNodes(op).pipe(
                map(list => [op, list]),
            ))),
        ]).subscribe(([loadedNodes, links]: [Node[], [number, Node[]][]]) => {
            const assignment: Record<number, Set<number>> = {};
            const newSelection: TableSelection = {};

            // Create a reverse mapping
            for (const [opId, linkedNodes] of links) {
                for (const linkedNode of linkedNodes) {
                    if (!assignment[linkedNode.id]) {
                        assignment[linkedNode.id] = new Set();
                    }
                    assignment[linkedNode.id].add(opId);
                }
            }

            // Check each selection state
            for (const node of loadedNodes) {
                this.nodes[node.id] = node;
                const assignCount = assignment[node.id]?.size ?? 0;

                switch (assignCount) {
                    case 0:
                        newSelection[node.id] = false;
                        break;

                    case this.objectProperties.length:
                        newSelection[node.id] = true;
                        break;

                    default:
                        newSelection[node.id] = CHECKBOX_STATE_INDETERMINATE;
                        break;
                }
            }

            // Apply new values
            this.selected = newSelection;
            this.currentAssignment = assignment;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    /**
     * If user clicks "assign"
     */
    async buttonAssignNodeRestrictonsClicked(): Promise<void> {
        this.closeFn(await this.assignSelection());
    }

    private async assignSelection(): Promise<boolean> {
        this.loading = true;
        this.changeDetector.markForCheck();
        let didChange = false;

        for (const opId of this.objectProperties) {
            const toAdd = new Set<number>(toSelectionArray(this.selected).map(Number));
            const toRemove = new Set<number>(toSelectionArray(this.selected, false).map(Number));

            for (const nodeToAdd of toAdd) {
                if (this.currentAssignment[nodeToAdd]?.has?.(opId)) {
                    toAdd.delete(nodeToAdd);
                }
            }

            if (toAdd.size > 0) {
                try {
                    await this.handler.addNodeRestriction([opId], Array.from(toAdd)).toPromise();
                } catch (err) {
                    this.errorHandler.catch(err);
                }
            }

            for (const nodeToRemove of toRemove) {
                if (!this.currentAssignment[nodeToRemove]?.has?.(opId)) {
                    toRemove.delete(nodeToRemove);
                }
            }

            if (toRemove.size > 0) {
                try {
                    await this.handler.removeNodeRestriction([opId], Array.from(toRemove)).toPromise();
                    didChange = true;
                } catch (err) {
                    this.errorHandler.catch(err);
                }
            }
        }

        this.loading = false;
        this.changeDetector.markForCheck();

        return didChange;
    }
}
