import { TemplateBO } from '@admin-ui/common';
import { I18nNotificationService, NodeOperations } from '@admin-ui/core';
import { NodeDataService, TemplateDataService } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { EntityIdType, IndexById, Node, Raw, Template } from '@gentics/cms-models';
import { BaseModal } from '@gentics/ui-core';
import { forkJoin, Subscription } from 'rxjs';
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
    public selectedIds: number[] = [];

    protected nodes: IndexById<Node<Raw>> = {};
    protected selectedPerTemplate: { [templateId: EntityIdType]: number[] } = {};
    protected subscription = new Subscription();

    constructor(
        protected changeDetector: ChangeDetectorRef,
        protected nodeData: NodeDataService,
        protected nodeOperations: NodeOperations,
        protected templateData: TemplateDataService,
        protected notification: I18nNotificationService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.loading = true;
        this.changeDetector.markForCheck();

        this.subscription.add(this.nodeData.watchAllEntities().pipe(
            first(),
            switchMap(nodes => {
                this.nodes = {};
                nodes.forEach(node => this.nodes[node.id] = node);

                return forkJoin(nodes
                    .map(node => this.nodeOperations.getTemplates(node.id).pipe(
                        map(templateRes => ([node, templateRes.items])),
                    )),
                );
            }),
        ).subscribe((arr: [node: Node, templates: Template[]][]) => {
            const newSelection = new Set<number>();
            const templateIds = this.templates.map(t => Number(t.id));
            this.selectedPerTemplate = {};

            arr.forEach(([node, nodeTemplates]) => {
                const nodeTemplateIds = nodeTemplates.map(t => Number(t.id));
                let nodeHasAllTemplates = true;

                for (const id of templateIds) {
                    if (!nodeTemplateIds.includes(id)) {
                        nodeHasAllTemplates = false;
                    } else {
                        this.selectedPerTemplate[id] = this.selectedPerTemplate[id] ?? [];
                        this.selectedPerTemplate[id].push(node.id);
                    }
                }

                // If a node contains all templates, then it should be marked as selected in the list
                if (nodeHasAllTemplates) {
                    newSelection.add(node.id);
                }
            });

            this.selectedIds = Array.from(newSelection);
            this.loading = false;
            this.changeDetector.markForCheck();
        }));
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    selectionChange(newSelection: number[]): void {
        this.selectedIds = newSelection;
    }

    async okButtonClicked(): Promise<void> {
        this.loading = true;
        this.changeDetector.markForCheck();
        const addSuccess = new Set<number>();
        const removeSucess = new Set<number>();

        for (const template of this.templates) {
            const toAdd = new Set<number>(this.selectedIds);
            const toRemove = new Set<number>(this.selectedPerTemplate[template.id]);

            for (const alreadAssigned of this.selectedPerTemplate[template.id]) {
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
                    await this.nodeOperations.unlinkTemplate(nodeToRemove, template.id).toPromise();
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
                        constructName: this.templates[0].name,
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

        this.templateData.reloadEntities();
        this.loading = false;
        this.changeDetector.markForCheck();
        this.closeFn();
    }
}
