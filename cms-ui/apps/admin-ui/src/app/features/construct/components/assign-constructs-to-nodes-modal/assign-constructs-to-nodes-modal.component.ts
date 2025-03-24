import { ConstructBO } from '@admin-ui/common';
import { I18nNotificationService, NodeOperations } from '@admin-ui/core';
import { ConstructHandlerService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { EntityIdType, IndexById, Node, Raw, TagTypeBO } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { intersection } from 'lodash-es';
import { Subscription, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
    selector: 'gtx-assign-constructs-to-nodes-modal',
    templateUrl: './assign-constructs-to-nodes-modal.component.html',
    styleUrls: ['./assign-constructs-to-nodes-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssignConstructsToNodesModalComponent extends BaseModal<void> implements OnInit, OnDestroy {

    @Input()
    public constructs: (TagTypeBO<Raw> | ConstructBO)[] = [];

    public loading = false;
    public selectedIds: string[] = [];

    protected nodes: IndexById<Node<Raw>> = {};
    protected selectedPerConstruct: { [constructId: EntityIdType]: string[] } = {};

    private subscription: Subscription = new Subscription();

    constructor(
        private changeDetector: ChangeDetectorRef,
        private nodeOperations: NodeOperations,
        private handler: ConstructHandlerService,
        private notification: I18nNotificationService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.loadNodes();
    }

    ngOnDestroy(): void {
        if (this.subscription) {
            this.subscription.unsubscribe();
        }
    }

    loadNodes(): void {
        this.loading = true;
        this.selectedPerConstruct = {};
        this.changeDetector.markForCheck();

        // Nodes are loaded here since this might be the first module which is being opened,
        // and the nodes are therefore not yet in the state. Would otherwise not display anything
        // in the list then.
        this.subscription.add(forkJoin([
            this.nodeOperations.getAll(),
            ...this.constructs.map(con => this.handler.getLinkedNodes(con.id).pipe(
                map(linked => ([con.id, linked])),
            )),
        ]).subscribe(([nodes, ...linkedNodesPerConstruct]) => {
            this.nodes = {};
            nodes.forEach(node => this.nodes[node.id] = node);

            const links = (linkedNodesPerConstruct as [EntityIdType, Node<Raw>[]][]).map(([constructId, linkedNodes]) => {
                const nodeIds = linkedNodes.map(node => `${node.id}`);
                this.selectedPerConstruct[constructId] = nodeIds;
                return nodeIds;
            });

            // Gives us a unique list of all nodes which are selected by all constructs
            this.selectedIds = Array.from(new Set<string>(intersection(...links)));
            this.loading = false;
            this.changeDetector.markForCheck();
        }, err => {
            console.error(err);
            this.notification.show({
                type: 'alert',
                message: 'common.loading_error',
            });
            this.cancelFn();
        }));
    }

    selectionChange(newSelection: string[]): void {
        this.selectedIds = newSelection;
    }

    async okButtonClicked(): Promise<void> {
        this.loading = true;
        this.changeDetector.markForCheck();
        const addSuccess = new Set<string>();
        const removeSucess = new Set<string>();

        for (const construct of this.constructs) {
            const toAdd = new Set<string>(this.selectedIds);
            const toRemove = new Set<string>(this.selectedPerConstruct[construct.id]);

            for (const alreadAssigned of this.selectedPerConstruct[construct.id]) {
                toAdd.delete(alreadAssigned);
            }
            for (const stillAssigned of this.selectedIds) {
                toRemove.delete(stillAssigned);
            }

            for (const nodeToAdd of toAdd) {
                try {
                    await this.handler.linkToNode(construct.id, Number(nodeToAdd)).toPromise();
                    addSuccess.add(nodeToAdd);
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
                try {
                    await this.handler.unlinkFromNode(construct.id, Number(nodeToRemove)).toPromise();
                    removeSucess.add(nodeToRemove);
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
        this.closeFn();
    }
}
