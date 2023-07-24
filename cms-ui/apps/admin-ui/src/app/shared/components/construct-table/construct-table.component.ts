import { AdminUIEntityDetailRoutes, AdminUIModuleRoutes, BO_PERMISSIONS, ConstructBO } from '@admin-ui/common';
import {
    ConstructOperations,
    ConstructTableLoaderOptions,
    ConstructTableLoaderService,
    DevToolPackageTableLoaderService,
    I18nNotificationService,
    I18nService,
    PackageOperations,
    PermissionsService
} from '@admin-ui/core';
import { ContextMenuService } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AnyModelType, GcmsPermission, NormalizableEntityTypesMap, TagType } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { BasePackageEntityTableComponent, UNASSIGN_FROM_PACKAGE_ACTION } from '../base-package-entity-table/base-package-entity-table.component';

export const ASSIGN_CONSTRUCT_TO_NODES_ACTION = 'assignConstructToNodes';
export const ASSIGN_CONSTRUCT_TO_CATEGORY_ACTION = 'assignConstructToCategory';
export const COPY_CONSTRUCT_ACTION = 'copyConstruct';

@Component({
    selector: 'gtx-construct-table',
    templateUrl: './construct-table.component.html',
    styleUrls: ['./construct-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructTableComponent
    extends BasePackageEntityTableComponent<TagType, ConstructBO, ConstructTableLoaderOptions>
    implements OnChanges {

    public readonly AdminUIModuleRoutes = AdminUIModuleRoutes;
    public readonly AdminUIEntityDetailRoutes = AdminUIEntityDetailRoutes;

    @Input()
    public dataSourceId: string | number;

    protected rawColumns: TableColumn<ConstructBO>[] = [
        {
            id: 'name',
            label: 'construct.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'keyword',
            label: 'construct.keyword',
            fieldPath: 'keyword',
            sortable: true,
        },
        {
            id: 'category',
            label: 'construct.category',
            fieldPath: 'categoryId',
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'construct';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: ConstructTableLoaderService,
        modalService: ModalService,
        contextMenu: ContextMenuService,
        packageOperations: PackageOperations,
        packageTableLoader: DevToolPackageTableLoaderService,
        protected permissions: PermissionsService,
        protected notification: I18nNotificationService,
        protected operations: ConstructOperations,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
            contextMenu,
            packageOperations,
            packageTableLoader,
        );
    }

    public override ngOnChanges(changes: SimpleChanges): void {
        super.ngOnChanges(changes);

        if (changes.dataSourceId) {
            this.loadTrigger.next();
        }
    }

    protected override createTableActionLoading(): Observable<TableAction<ConstructBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentadmin.updateContent').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canUpdatePackages]) => {
                const actions: TableAction<ConstructBO>[] = [];

                if (this.packageName) {
                    actions.push({
                        id: UNASSIGN_FROM_PACKAGE_ACTION,
                        icon: 'link_off',
                        label: this.i18n.instant('package.remove_from_package'),
                        enabled: canUpdatePackages,
                        type: 'alert',
                        single: true,
                        multiple: true,
                    })
                } else {
                    actions.push({
                        id: ASSIGN_CONSTRUCT_TO_NODES_ACTION,
                        icon: 'device_hub',
                        label: this.i18n.instant('construct.assign_to_nodes'),
                        type: 'primary',
                        enabled: (item) => item == null || item[BO_PERMISSIONS].includes(GcmsPermission.EDIT),
                        single: true,
                        multiple: true,
                    },
                    {
                        id: ASSIGN_CONSTRUCT_TO_CATEGORY_ACTION,
                        icon: 'category',
                        label: this.i18n.instant('construct.assign_to_category'),
                        type: 'secondary',
                        enabled: (item) => item == null || item[BO_PERMISSIONS].includes(GcmsPermission.EDIT),
                        multiple: true,
                    },
                    {
                        id: COPY_CONSTRUCT_ACTION,
                        icon: 'content_copy',
                        label: this.i18n.instant('construct.copy_construct'),
                        type: 'secondary',
                        enabled: (item) => item == null || item[BO_PERMISSIONS].includes(GcmsPermission.EDIT),
                        single: true,
                    },
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('construct.delete_construct_singular'),
                        enabled: (item) => item == null || item[BO_PERMISSIONS].includes(GcmsPermission.DELETE),
                        type: 'alert',
                        single: true,
                        multiple: true,
                    });
                }

                return actions;
            }),
        );
    }

    protected override createAdditionalLoadOptions(): ConstructTableLoaderOptions {
        return {
            packageName: this.packageName,
            dataSourceId: this.dataSourceId,
        };
    }
}
