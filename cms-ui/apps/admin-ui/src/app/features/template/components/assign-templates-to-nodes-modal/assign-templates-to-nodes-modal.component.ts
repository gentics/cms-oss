import { TemplateBO } from '@admin-ui/common';
import { I18nNotificationService, NodeOperations, TemplateOperations } from '@admin-ui/core';
import { NodeDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { IndexById, Node, Raw } from '@gentics/cms-models';
import { BaseModal, CHECKBOX_STATE_INDETERMINATE, TableSelection, toSelectionArray } from '@gentics/ui-core';
import { combineLatest, forkJoin, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
    selector: 'gtx-assign-templates-to-nodes-modal',
    templateUrl: './assign-templates-to-nodes-modal.component.html',
    styleUrls: ['./assign-templates-to-nodes-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class AssignTemplatesToNodesModalComponent extends BaseModal<boolean> implements OnInit, OnDestroy {

    @Input()
    public templates: TemplateBO[] = [];

    public loading = false;
    public selected: TableSelection = {};

    protected nodes: IndexById<Node<Raw>> = {};
    /**
     * @key nodeId
     */
    protected currentAssignment: Record<number, Set<number>> = {};
    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected nodeData: NodeDataService,
        protected nodeOperations: NodeOperations,
        protected notification: I18nNotificationService,
        protected templateOperations: TemplateOperations,
    ) {
        super();
    }

    ngOnInit(): void {
        this.loading = true;
        this.changeDetector.markForCheck();

        this.subscriptions.push(combineLatest([
            this.nodeData.watchAllEntities(),
            forkJoin(this.templates.map(template => {
                // for every template, get the list of nodes to which the template is assigned
                return this.templateOperations.getLinkedNodes(template.id).pipe(
                    map(linkedNodes => [template, linkedNodes]),
                );
            })),
        ]).subscribe(([allNodes, templateData]: [Node[], [template: TemplateBO, linkedNodes: Node[]][]]) => {
            const assignment: Record<number, Set<number>> = {};
            const newSelection: TableSelection = {};
            this.nodes = {};

            // Create a reverse mapping
            for (const [template, linkedNodes] of templateData) {
                for (const node of linkedNodes) {
                    if (!assignment[node.id]) {
                        assignment[node.id] = new Set();
                    }
                    assignment[node.id].add(template.id);
                }
            }

            // Check each nodes selection state
            for (const node of allNodes) {
                this.nodes[node.id] = node;
                const templateCount = assignment[node.id]?.size ?? 0;

                switch (templateCount) {
                    case 0:
                        newSelection[node.id] = false;
                        break;

                    case this.templates.length:
                        newSelection[node.id] = true;
                        break;

                    default:
                        newSelection[node.id] = CHECKBOX_STATE_INDETERMINATE;
                        break;
                }
            }

            this.selected = newSelection;
            this.currentAssignment = assignment;

            this.loading = false;
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

        for (const template of this.templates) {
            const toAdd = new Set<number>(toSelectionArray(this.selected).map(Number));
            const toRemove = new Set<number>(toSelectionArray(this.selected, false).map(Number));

            for (const nodeToAdd of toAdd) {
                if (this.currentAssignment[nodeToAdd]?.has?.(template.id)) {
                    continue;
                }

                try {
                    await this.nodeOperations.linkTemplate(nodeToAdd, template.id).toPromise();
                    addSuccess.add(nodeToAdd);
                    didChange = true;
                } catch (err) {
                    this.notification.show({
                        type: 'alert',
                        message: 'template.assign_to_node_error',
                        delay: 10_000,
                        translationParams: {
                            templateName: template.name,
                            nodeName: this.nodes[nodeToAdd].name,
                            errorMessage: err.message,
                        },
                    });
                }
            }

            for (const nodeToRemove of toRemove) {
                if (!this.currentAssignment[nodeToRemove]?.has?.(template.id)) {
                    continue;
                }

                try {
                    await this.nodeOperations.unlinkTemplate(nodeToRemove, template.id).toPromise();
                    removeSucess.add(nodeToRemove);
                    didChange = true;
                } catch (err) {
                    this.notification.show({
                        type: 'alert',
                        message: 'template.unassign_from_node_error',
                        delay: 10_000,
                        translationParams: {
                            templateName: template.name,
                            nodeName: this.nodes[nodeToRemove].name,
                            errorMessage: err.message,
                        },
                    });
                }
            }
        }

        for (const nodeId of addSuccess) {
            if (this.templates.length === 1) {
                this.notification.show({
                    type: 'success',
                    message: 'template.assign_to_node_success',
                    translationParams: {
                        templateName: this.templates[0].name,
                        nodeName: this.nodes[nodeId].name,
                    },
                });
            } else {
                this.notification.show({
                    type: 'success',
                    message: 'template.assign_multiple_to_node_success',
                    translationParams: {
                        nodeName: this.nodes[nodeId].name,
                    },
                });
            }
        }

        for (const nodeId of removeSucess) {
            if (this.templates.length === 1) {
                this.notification.show({
                    type: 'success',
                    message: 'template.unassign_from_node_success',
                    translationParams: {
                        templateName: this.templates[0].name,
                        nodeName: this.nodes[nodeId].name,
                    },
                });
            } else {
                this.notification.show({
                    type: 'success',
                    message: 'template.unassign_multiple_from_node_success',
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
