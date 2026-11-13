import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { AnyModelType, GcmsPermission, NormalizableEntityTypesMap, PortalImplementation } from '@gentics/cms-models';
import { ChangesOf, ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { BO_PERMISSIONS, PortalImplementationBO } from '../../../common';
import {
    DevToolPackageTableLoaderService,
    PermissionsService,
    PortalImplementationTableLoaderOptions,
    PortalImplementationTableLoaderService,
} from '../../../core';
import { AppStateService } from '../../../state';
import { ContextMenuService } from '../../providers/context-menu/context-menu.service';
import { AssignNodesToPortalImplementationModalComponent } from '../assign-nodes-to-portal-implementation-modal/assign-nodes-to-portal-implementation-modal.component';
import { AssignPortalImplementationsToNodeModalComponent } from '../assign-portal-implementations-to-node-modal/assign-portal-implementations-to-node-modal.component';
import { DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { BasePackageEntityTableComponent, UNASSIGN_FROM_PACKAGE_ACTION } from '../base-package-entity-table/base-package-entity-table.component';

const UNASSIGN_FROM_NODE_ACTION = 'unassignFromNode';
const ASSIGN_TO_NODES_ACTION = 'assignToNodes';

@Component({
    selector: 'gtx-portal-implementation-table',
    templateUrl: './portal-implementation-table.component.html',
    styleUrls: ['./portal-implementation-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class PortalImplementationTableComponent
    extends BasePackageEntityTableComponent<PortalImplementation, PortalImplementationBO, PortalImplementationTableLoaderOptions>
    implements OnChanges {

    /** If set, the table shows the portal implementations assigned to the node and allows to change the assignment */
    @Input()
    public nodeId: number;

    /** Whether the node is a master node (assignments can only be changed for master nodes) */
    @Input()
    public isMasterNode = true;

    protected rawColumns: TableColumn<PortalImplementationBO>[] = [
        {
            id: 'keyword',
            label: 'portal_implementation.keyword',
            fieldPath: 'keyword',
            sortable: true,
        },
        {
            id: 'name',
            label: 'portal_implementation.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'type',
            label: 'portal_implementation.type',
            fieldPath: 'type',
            sortable: false,
        },
    ];

    public canManagePackage = false;
    public canCreate = false;

    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'portalImplementation';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: PortalImplementationTableLoaderService,
        modalService: ModalService,
        contextMenu: ContextMenuService,
        packageTableLoader: DevToolPackageTableLoaderService,
        protected permissions: PermissionsService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader as any,
            modalService,
            contextMenu,
            packageTableLoader,
        );
    }

    public override ngOnChanges(changes: ChangesOf<this>): void {
        super.ngOnChanges(changes);

        if (changes.nodeId) {
            this.loadTrigger.next();
            this.actionRebuildTrigger.next();
        }

        if (changes.isMasterNode) {
            this.actionRebuildTrigger.next();
        }
    }

    protected override createTableActionLoading(): Observable<TableAction<PortalImplementationBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentadmin.updateContent').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('portalImplementation.createPortalImplementation').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('portalImplementation.deletePortalImplementation').typePermissions),
            this.permissions.checkPermissions(
                this.permissions.getUserActionPermsForId('portalImplementation.assignPortalImplementationToNodes').typePermissions,
            ),
        ]).pipe(
            map(([_, canManagePackage, canCreate, canDelete, canAssignToNodes]) => {
                this.canManagePackage = canManagePackage;
                this.canCreate = canCreate;

                const actions: TableAction<PortalImplementationBO>[] = [];

                if (this.nodeId) {
                    actions.push({
                        id: UNASSIGN_FROM_NODE_ACTION,
                        icon: 'link_off',
                        type: 'alert',
                        label: this.i18n.instant('portal_implementation.remove_portalImplementations_from_node'),
                        enabled: this.isMasterNode,
                        single: true,
                        multiple: true,
                    });
                } else if (this.packageName) {
                    actions.push({
                        id: UNASSIGN_FROM_PACKAGE_ACTION,
                        icon: 'link_off',
                        type: 'alert',
                        label: this.i18n.instant('package.remove_from_package'),
                        enabled: canManagePackage,
                        single: true,
                        multiple: true,
                    });
                } else {
                    actions.push({
                        id: ASSIGN_TO_NODES_ACTION,
                        icon: 'device_hub',
                        type: 'primary',
                        label: this.i18n.instant('portal_implementation.assign_to_nodes'),
                        enabled: canAssignToNodes,
                        single: true,
                    }, {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        type: 'alert',
                        label: this.i18n.instant('shared.delete'),
                        // deleting requires the delete permission on the portal implementation
                        enabled: (item) => canDelete && (item == null || item[BO_PERMISSIONS].includes(GcmsPermission.DELETE)),
                        single: true,
                        multiple: true,
                    });
                }

                return actions;
            }),
        );
    }

    protected override createAdditionalLoadOptions(): PortalImplementationTableLoaderOptions {
        return {
            packageName: this.packageName,
            nodeId: this.nodeId,
        };
    }

    public override handleAction(event: TableActionClickEvent<PortalImplementationBO>): void {
        switch (event.actionId) {
            case UNASSIGN_FROM_NODE_ACTION:
                this.unassignFromNode(this.getAffectedEntityIds(event));
                return;

            case ASSIGN_TO_NODES_ACTION:
                this.assignToNodes(event.item);
                return;
        }

        super.handleAction(event);
    }

    public async handleAssignToNode(): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            AssignPortalImplementationsToNodeModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { nodeId: this.nodeId },
        );
        const didChange = await dialog.open();

        if (didChange) {
            this.loadTrigger.next();
        }
    }

    /**
     * Open the modal for changing the nodes, the portal implementation is assigned to
     */
    protected async assignToNodes(portalImplementation: PortalImplementationBO): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            AssignNodesToPortalImplementationModalComponent,
            { closeOnOverlayClick: false, width: '50%' },
            { portalImplementation },
        );
        await dialog.open();
    }

    protected async unassignFromNode(ids: string[]): Promise<void> {
        for (const id of ids) {
            await (this.loader as any as PortalImplementationTableLoaderService).removeFromNode(this.nodeId, id).toPromise();
            this.removeFromSelection(id);
        }

        this.loadTrigger.next();
    }
}
