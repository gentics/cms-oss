import { ConstructBO } from '@admin-ui/common';
import { ConstructHandlerService, I18nNotificationService, NodeOperations } from '@admin-ui/core';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { IndexById, Node, Raw } from '@gentics/cms-models';
import { BaseModal, CHECKBOX_STATE_INDETERMINATE, TableSelection, toSelectionArray } from '@gentics/ui-core';
import { Subscription, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
    selector: 'gtx-assign-constructs-to-nodes-modal',
    templateUrl: './assign-constructs-to-nodes-modal.component.html',
    styleUrls: ['./assign-constructs-to-nodes-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class AssignConstructsToNodesModalComponent extends BaseModal<boolean> implements OnInit, OnDestroy {

    @Input()
    public constructs: ConstructBO[] = [];

    public loading = false;
    public selected: TableSelection = {};

    protected nodes: IndexById<Node<Raw>> = {};
    /**
     * @key nodeId
     */
    protected currentAssignment: Record<number, Set<number>> = null;

    private subscriptions: Subscription[] = [];

    constructor(
        private changeDetector: ChangeDetectorRef,
        private nodeOperations: NodeOperations,
        private handler: ConstructHandlerService,
        private notification: I18nNotificationService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.subscriptions.push(forkJoin([
            this.nodeOperations.getAll(),
            forkJoin(this.constructs.map(con => this.handler.getLinkedNodes(con.id).pipe(
                map(linked => [con.id, linked]),
            ))),
        ]).subscribe(([loadedNodes, links]: [Node[], [number, Node[]][]]) => {
            const assignment: Record<number, Set<number>> = {};
            const newSelection: TableSelection = {};

            // Create a reverse mapping
            for (const [constructId, linkedNodes] of links) {
                for (const linkedNode of linkedNodes) {
                    if (!assignment[linkedNode.id]) {
                        assignment[linkedNode.id] = new Set();
                    }
                    assignment[linkedNode.id].add(constructId);
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

                    case this.constructs.length:
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

    async okButtonClicked(): Promise<void> {
        this.closeFn(await this.applySelection());
    }

    async applySelection(): Promise<boolean> {
        this.loading = true;
        this.changeDetector.markForCheck();

        const addSuccess = new Set<number>();
        const removeSucess = new Set<number>();
        let didChange = false;

        for (const construct of this.constructs) {
            const toAdd = new Set<number>(toSelectionArray(this.selected).map(Number));
            const toRemove = new Set<number>(toSelectionArray(this.selected, false).map(Number));

            for (const nodeToAdd of toAdd) {
                if (this.currentAssignment[nodeToAdd]?.has?.(construct.id)) {
                    continue;
                }

                try {
                    await this.handler.linkToNode(construct.id, nodeToAdd).toPromise();
                    addSuccess.add(nodeToAdd);
                    didChange = true;
                } catch (err) {
                    this.notification.show({
                        type: 'alert',
                        message: 'construct.assign_to_node_error',
                        delay: 10_000,
                        translationParams: {
                            nodeName: this.nodes[nodeToAdd].name,
                            errorMessage: err.message,
                        },
                    });
                }
            }

            for (const nodeToRemove of toRemove) {
                if (!this.currentAssignment[nodeToRemove]?.has?.(construct.id)) {
                    continue;
                }

                try {
                    await this.handler.unlinkFromNode(construct.id, nodeToRemove).toPromise();
                    removeSucess.add(nodeToRemove);
                    didChange = true;
                } catch (err) {
                    this.notification.show({
                        type: 'alert',
                        message: 'construct.unassign_from_node_error',
                        delay: 10_000,
                        translationParams: {
                            nodeName: this.nodes[nodeToRemove].name,
                            errorMessage: err.message,
                        },
                    });
                }
            }
        }

        for (const nodeId of addSuccess) {
            if (this.constructs.length === 1) {
                this.notification.show({
                    type: 'success',
                    message: 'construct.assign_to_node_success',
                    translationParams: {
                        constructName: this.constructs[0].name,
                        nodeName: this.nodes[nodeId].name,
                    },
                });
            } else {
                this.notification.show({
                    type: 'success',
                    message: 'construct.assign_multiple_to_node_success',
                    translationParams: {
                        nodeName: this.nodes[nodeId].name,
                    },
                });
            }
        }

        for (const nodeId of removeSucess) {
            if (this.constructs.length === 1) {
                this.notification.show({
                    type: 'success',
                    message: 'construct.unassign_from_node_success',
                    translationParams: {
                        constructName: this.constructs[0].name,
                        nodeName: this.nodes[nodeId].name,
                    },
                });
            } else {
                this.notification.show({
                    type: 'success',
                    message: 'construct.unassign_multiple_from_node_success',
                    translationParams: {
                        nodeName: this.nodes[nodeId].name,
                    },
                });
            }
        }

        this.loading = false;
        this.changeDetector.markForCheck();

        return didChange;
    }
}
