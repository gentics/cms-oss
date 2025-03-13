import { BO_PERMISSIONS, EntityTableActionClickEvent, NodeBO } from '@admin-ui/common';
import { I18nNotificationService, I18nService, NodeOperations, NodeTableLoaderService, TranslatedNotificationOptions } from '@admin-ui/core';
import { WizardService } from '@admin-ui/shared';
import { BaseTableMasterComponent } from '@admin-ui/shared/components/base-table-master/base-table-master.component';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AnyModelType, GcmsPermission, Node, NodeCopyRequest, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableAction } from '@gentics/ui-core';
import { CopyNodesModalComponent } from '../copy-nodes-modal/copy-nodes-modal.component';
import { CreateNodeWizardComponent } from '../create-node-wizard/create-node-wizard.component';

const COPY_ACTION = 'copy';

@Component({
    selector: 'gtx-node-master',
    templateUrl: './node-master.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeMasterComponent extends BaseTableMasterComponent<Node, NodeBO> implements OnInit {

    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'node';

    public selected: string[] = [];
    public masterActions: TableAction<NodeBO>[] = [];

    constructor(
        changeDetector: ChangeDetectorRef,
        router: Router,
        route: ActivatedRoute,
        appState: AppStateService,
        protected wizardService: WizardService,
        protected tableLoader: NodeTableLoaderService,
        protected modalService: ModalService,
        protected notification: I18nNotificationService,
        protected operations: NodeOperations,
        protected i18n: I18nService,
    ) {
        super(
            changeDetector,
            router,
            route,
            appState,
        );
    }

    ngOnInit(): void {
        super.ngOnInit();

        this.masterActions = [
            {
                id: COPY_ACTION,
                icon: 'file_copy',
                label: this.i18n.instant('shared.copy'),
                type: 'secondary',
                single: true,
                multiple: true,
                enabled: (item) => {
                    if (this.tableLoader.isChannel(item)) {
                        return false;
                    }

                    if (!item) {
                        return true;
                    }

                    const perms = (item?.[BO_PERMISSIONS] || []);
                    return perms.includes(GcmsPermission.READ) && perms.includes(GcmsPermission.EDIT);
                },
            },
        ];
    }

    async handleCreateClick(): Promise<void> {
        const created = await this.wizardService.showWizard(CreateNodeWizardComponent);
        if (created) {
            this.tableLoader.reload();
        }
    }

    handleActionClick(event: EntityTableActionClickEvent<NodeBO>): void {
        switch (event.actionId) {
            case COPY_ACTION: {
                const nodes = event.selection ? event.selectedItems : [event.item];
                this.copyNodes(nodes);
                break;
            }
        }
    }

    async copyNodes(nodes: NodeBO[]): Promise<void> {
        if (nodes.length === 0) {
            return;
        }

        const dialog = await this.modalService.fromComponent(
            CopyNodesModalComponent,
            null,
            { nodesToBeCopied: nodes },
        );
        const userInput: { nodeId: number, requestPayload: NodeCopyRequest }[] = await dialog.open();

        if (userInput.length === 0) {
            const notificationOptions: TranslatedNotificationOptions = {
                delay: -1,
                dismissOnClick: false,
                type: 'warning',
                message: 'shared.no_row_selected_warning',
            };
            this.notification.show(notificationOptions);
            return;
        }

        for (const op of userInput) {
            await this.operations.copyNode(op.nodeId, op.requestPayload);
        }

        this.tableLoader.reload();
    }
}
