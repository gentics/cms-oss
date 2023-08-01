import { AdminUIEntityDetailRoutes, ContentRepositoryBO, ContentRepositoryDetailTabs } from '@admin-ui/common';
import {
    ContentRepositoryHandlerService,
    ContentRepositoryTableLoaderOptions,
    ContentRepositoryTableLoaderService,
    DevToolPackageTableLoaderService,
    I18nService,
    PackageOperations,
    PermissionsService,
} from '@admin-ui/core';
import { ContextMenuService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AnyModelType, ContentRepository, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import {
    AssignContentrepositoriesToNodesModalComponent,
} from '../assign-content-repositories-to-nodes-modal/assign-content-repositories-to-nodes-modal.component';
import {
    AssignCRFragmentsToContentRepositoryModal,
} from '../assign-cr-fragments-to-content-repository-modal/assign-cr-fragments-to-content-repository-modal.component';
import { DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { BasePackageEntityTableComponent, UNASSIGN_FROM_PACKAGE_ACTION } from '../base-package-entity-table/base-package-entity-table.component';

const ASSIGN_NODES_ACTION = 'assignNodes';
const ASSIGN_FRAGMENTS_ACTION = 'assignFragments';
const DATA_CHECK_ACTION = 'dataCheck';
const DATA_REPAIR_ACTION = 'dataRepair';
const STRUCTURE_CHECK_ACTION = 'structureCheck';
const STRUCTURE_REPAIR_ACTION = 'structureRepair';

export interface OpenCRDetailEvent {
    item: ContentRepositoryBO,
    tab: ContentRepositoryDetailTabs;
}

@Component({
    selector: 'gtx-content-repository-table',
    templateUrl: './content-repository-table.component.html',
    styleUrls: ['./content-repository-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ContentRepositoryTableComponent
    extends BasePackageEntityTableComponent<ContentRepository, ContentRepositoryBO, ContentRepositoryTableLoaderOptions>
    implements OnChanges {

    public readonly ContentRepositoryDetailTabs = ContentRepositoryDetailTabs;
    public readonly AdminUIEntityDetailRoutes = AdminUIEntityDetailRoutes;

    @Input()
    public linkDetails = false;

    protected rawColumns: TableColumn<ContentRepositoryBO>[] = [
        {
            id: 'name',
            label: 'contentRepository.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'crType',
            label: 'contentRepository.crType',
            fieldPath: 'crType',
            sortable: true,
        },
        {
            id: 'dbType',
            label: 'contentRepository.dbType',
            fieldPath: 'dbType',
            sortable: true,
        },
        {
            id: 'url',
            label: 'contentRepository.url',
            fieldPath: 'url',
            sortable: true,
        },
        {
            id: 'instantPublishing',
            label: 'contentRepository.instantPublishing',
            fieldPath: 'instantPublishing',
            align: 'center',
            sortable: true,
        },
        {
            id: 'dataStatus',
            label: 'contentRepository.dataStatus',
            fieldPath: 'dataStatus',
            sortable: true,
        },
        {
            id: 'checkStatus',
            label: 'contentRepository.checkStatus',
            fieldPath: 'checkStatus',
            sortable: true,
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'contentRepository';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: ContentRepositoryTableLoaderService,
        modalService: ModalService,
        contextMenu: ContextMenuService,
        packageOperations: PackageOperations,
        packageTableLoader: DevToolPackageTableLoaderService,
        protected router: Router,
        protected route: ActivatedRoute,
        protected permissions: PermissionsService,
        protected handler: ContentRepositoryHandlerService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader as any,
            modalService,
            contextMenu,
            packageOperations,
            packageTableLoader,
        );
    }

    protected override createTableActionLoading(): Observable<TableAction<ContentRepositoryBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentRepository.deleteContentRepository').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentadmin.updateContent').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canDelete, canManagePackage]) => {
                const actions: TableAction<ContentRepositoryBO>[] = [];

                if (!this.packageName) {
                    actions.push({
                        id: ASSIGN_NODES_ACTION,
                        type: 'primary',
                        icon: 'device_hub',
                        enabled: true,
                        single: true,
                        label: this.i18n.instant('shared.assign_contentrepositories_to_nodes'),
                    },
                    {
                        id: ASSIGN_FRAGMENTS_ACTION,
                        type: 'primary',
                        icon: 'dns',
                        enabled: true,
                        single: true,
                        label: this.i18n.instant('shared.assign_crfragments_to_contentrepositories'),
                    },
                    {
                        id: DATA_CHECK_ACTION,
                        type: 'secondary',
                        icon: 'find_in_page',
                        enabled: true,
                        single: true,
                        label: this.i18n.instant('contentRepository.btn_data_check'),
                    },
                    {
                        id: DATA_REPAIR_ACTION,
                        type: 'secondary',
                        icon: 'build',
                        enabled: true,
                        single: true,
                        label: this.i18n.instant('contentRepository.btn_data_repair'),
                    },
                    {
                        id: STRUCTURE_CHECK_ACTION,
                        type: 'secondary',
                        icon: 'image_search',
                        enabled: true,
                        single: true,
                        label: this.i18n.instant('contentRepository.btn_structure_check'),
                    },
                    {
                        id: STRUCTURE_REPAIR_ACTION,
                        type: 'secondary',
                        icon: 'offline_pin',
                        enabled: true,
                        single: true,
                        label: this.i18n.instant('contentRepository.btn_structure_repair'),
                    },
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        type: 'alert',
                        label: this.i18n.instant('shared.delete'),
                        enabled: canDelete,
                        multiple: true,
                        single: true,
                    });
                } else {
                    actions.push({
                        id: UNASSIGN_FROM_PACKAGE_ACTION,
                        icon: 'link_off',
                        type: 'alert',
                        label: this.i18n.instant('package.remove_from_package'),
                        enabled: canManagePackage,
                        single: true,
                        multiple: true,
                    });
                }

                return actions;
            }),
        );
    }

    protected override createAdditionalLoadOptions(): ContentRepositoryTableLoaderOptions {
        return {
            packageName: this.packageName,
        };
    }

    handleAction(event: TableActionClickEvent<ContentRepositoryBO>): void {
        switch (event.actionId) {
            case ASSIGN_NODES_ACTION:
                this.assignNodes(event.item.id);
                return;

            case ASSIGN_FRAGMENTS_ACTION:
                this.assignFragments(event.item.id);
                return;

            case DATA_CHECK_ACTION:
                this.handler.checkData(event.item.id).toPromise().then(() => {
                    this.reload();
                });
                return;

            case DATA_REPAIR_ACTION:
                this.handler.repairData(event.item.id).toPromise().then(() => {
                    this.reload();
                });
                return;

            case STRUCTURE_CHECK_ACTION:
                this.handler.checkStructure(event.item.id).toPromise().then(() => {
                    this.reload();
                });
                return;

            case STRUCTURE_REPAIR_ACTION:
                this.handler.repairStructure(event.item.id).toPromise().then(() => {
                    this.reload();
                });
                return;
        }

        super.handleAction(event);
    }

    protected async assignNodes(crId: string | number): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            AssignContentrepositoriesToNodesModalComponent,
            { closeOnOverlayClick: false, width: '100%' },
            { contentRepositoryId: String(crId) },
        );
        await dialog.open();

        this.loadTrigger.next();
    }

    protected async assignFragments(crId: string | number): Promise<void> {
        const dialog = await this.modalService.fromComponent(
            AssignCRFragmentsToContentRepositoryModal,
            { closeOnOverlayClick: false, width: '50%' },
            { contentRepositoryId: String(crId) },
        );
        await dialog.open();

        this.loadTrigger.next();
    }
}
