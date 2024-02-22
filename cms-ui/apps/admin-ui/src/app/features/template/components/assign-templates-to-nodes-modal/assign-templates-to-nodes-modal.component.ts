import { TemplateBO } from '@admin-ui/common';
import { I18nNotificationService, NodeOperations, NodeTableLoaderService, TemplateOperations } from '@admin-ui/core';
import { NodeDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { EntityIdType, IndexById, Node, Raw } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { combineLatest, Subscription, forkJoin } from 'rxjs';
import { first, map, switchMap } from 'rxjs/operators';

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
    public selectedIds: string[] = [];

    protected nodes: IndexById<Node<Raw>> = {};
    protected selectedPerTemplate: { [templateId: EntityIdType]: number[] } = {};
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

        this.subscriptions.push(combineLatest([this.nodeData.watchAllEntities(), forkJoin(this.templates.map(template => {
            // for every template, get the list of nodes to which the template is assigned
            return this.templateOperations.getLinkedNodes(template.id).pipe(
                map(linkedNodes => [template, linkedNodes]),
            );
        }))],
        ).subscribe(([nodes, templateData]: [Node[], [template: TemplateBO, linkedNodes: Node[]][]]) => {
            const nodeIds: number[] = [];
            this.nodes = {};
            nodes.forEach(node => {
                this.nodes[node.id] = node;
                nodeIds.push(node.id);
            });

            const newSelection = new Set<string>();
            const templateIds = this.templates.map(t => Number(t.id));

            // for every template, collect the node IDs to which the template is assigned
            this.selectedPerTemplate = {};
            templateData.forEach(([template, linkedNodes]) => {
                linkedNodes.forEach(node => {
                    const templateId = Number(template.id);
                    const nodeId = Number(node.id);
                    this.selectedPerTemplate[templateId] = this.selectedPerTemplate[templateId] ?? [];
                    this.selectedPerTemplate[templateId].push(nodeId);
                });
            });

            // for every node, check whether all given templates are assigned
            nodeIds.forEach(nodeId => {
                let nodeHasAllTemplates = true;
                for (const id of templateIds) {
                    if (!this.selectedPerTemplate[id] || !this.selectedPerTemplate[id].includes(nodeId)) {
                        nodeHasAllTemplates = false;
                        break;
                    }
                }

                // If a node contains all templates, then it should be marked as selected in the list
                if (nodeHasAllTemplates) {
                    newSelection.add(nodeId.toString());
                }
            });

            this.selectedIds = Array.from(newSelection);
            this.loading = false;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscriptions.forEach(s => s.unsubscribe());
    }

    selectionChange(newSelection: string[]): void {
        this.selectedIds = newSelection;
    }

    async okButtonClicked(): Promise<void> {
        this.loading = true;
        this.changeDetector.markForCheck();
        const addSuccess = new Set<string>();
        const removeSucess = new Set<string>();

        for (const template of this.templates) {
            const toAdd = new Set<string>(this.selectedIds);
            const nodeIds = this.selectedPerTemplate[template.id].map(id => id.toString())
            const toRemove = new Set<string>(nodeIds);

            for (const alreadAssigned of nodeIds) {
                toAdd.delete(alreadAssigned);
            }
            for (const stillAssigned of this.selectedIds) {
                toRemove.delete(stillAssigned);
            }

            for (const nodeToAdd of toAdd) {
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

        this.nodeTableLoader.reload();
        this.loading = false;
        this.changeDetector.markForCheck();
        this.closeFn();
    }
}
