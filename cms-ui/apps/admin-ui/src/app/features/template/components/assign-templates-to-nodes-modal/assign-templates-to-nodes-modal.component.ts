import { TemplateBO } from '@admin-ui/common';
import { I18nNotificationService, NodeOperations, NodeTableLoaderService, TemplateOperations } from '@admin-ui/core';
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
})
export class AssignTemplatesToNodesModalComponent extends BaseModal<void> implements OnInit, OnDestroy {

    @Input()
    public templates: TemplateBO[] = [];

    public loading = false;
    public selected: TableSelection = {};

    protected nodes: IndexById<Node<Raw>> = {};
    /**
     * Record of template-id which node-ids have it assigned.
     */
    protected selectedPerTemplate: Record<number, Set<number>> = {};
    protected subscriptions: Subscription[] = [];

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected nodeData: NodeDataService,
        protected nodeOperations: NodeOperations,
        protected nodeTableLoader: NodeTableLoaderService,
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
        ]).subscribe(([nodes, templateData]: [Node[], [template: TemplateBO, linkedNodes: Node[]][]]) => {
            const nodeIds: number[] = [];
            this.nodes = {};
            this.selected = {};

            nodes.forEach(node => {
                this.nodes[node.id] = node;
                this.selected[node.id] = CHECKBOX_STATE_INDETERMINATE;
                nodeIds.push(node.id);
            });

            const templateIds = this.templates.map(t => Number(t.id));

            // for every template, collect the node IDs to which the template is assigned
            this.selectedPerTemplate = {};

            templateData.forEach(([template, linkedNodes]) => {
                linkedNodes.forEach(node => {
                    const templateId = Number(template.id);
                    const nodeId = Number(node.id);
                    if (!this.selectedPerTemplate[templateId]) {
                        this.selectedPerTemplate[templateId] = new Set();
                    }
                    this.selectedPerTemplate[templateId].add(nodeId);
                });
            });

            // for every node, check whether all given templates are assigned or unassigned
            nodeIds.forEach(nodeId => {
                let templateCount = 0;

                for (const id of templateIds) {
                    if (this.selectedPerTemplate[id] && this.selectedPerTemplate[id].has(nodeId)) {
                        templateCount++;
                    }
                }

                if (templateCount === 0) {
                    this.selected[nodeId] = false;
                } else if (templateCount === this.templates.length) {
                    this.selected[nodeId] = true;
                }
            });

            this.loading = false;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    selectionChange(newSelection: TableSelection): void {
        this.selected = newSelection;
    }

    async okButtonClicked(): Promise<void> {
        await this.applySelection();

        this.nodeTableLoader.reload();
        this.closeFn();
    }

    async applySelection(): Promise<void> {
        this.loading = true;
        this.changeDetector.markForCheck();
        const addSuccess = new Set<number>();
        const removeSucess = new Set<number>();

        for (const template of this.templates) {
            const toAdd = new Set<number>(toSelectionArray(this.selected).map(Number));
            const toRemove = new Set<number>(toSelectionArray(this.selected, false).map(Number));
            const previouslyAssignedIds = new Set(this.selectedPerTemplate[template.id]);

            for (const nodeToAdd of toAdd) {
                if (previouslyAssignedIds.has(nodeToAdd)) {
                    continue;
                }

                try {
                    await this.nodeOperations.linkTemplate(nodeToAdd, template.id).toPromise();
                    addSuccess.add(nodeToAdd);
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
                if (!previouslyAssignedIds.has(nodeToRemove)) {
                    continue;
                }

                try {
                    await this.nodeOperations.unlinkTemplate(nodeToRemove, template.id).toPromise();
                    removeSucess.add(nodeToRemove);
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
    }
}
