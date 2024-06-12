import { DataSourceBO, EditableEntity } from '@admin-ui/common';
import {
    DataSourceTableLoaderOptions,
    DataSourceTableLoaderService,
    DevToolPackageTableLoaderService,
    I18nService,
    PermissionsService,
} from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, DataSource, NormalizableEntityTypesMap } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { ContextMenuService } from '../../providers/context-menu/context-menu.service';
import { DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { BasePackageEntityTableComponent, UNASSIGN_FROM_PACKAGE_ACTION } from '../base-package-entity-table/base-package-entity-table.component';

@Component({
    selector: 'gtx-data-source-table',
    templateUrl: './data-source-table.component.html',
    styleUrls: ['./data-source-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DataSourceTableComponent
    extends BasePackageEntityTableComponent<DataSource, DataSourceBO, DataSourceTableLoaderOptions> {

    protected rawColumns: TableColumn<DataSourceBO>[] = [
        {
            id: 'name',
            label: 'dataSource.name',
            fieldPath: 'name',
            sortable: true,
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'dataSource';
    protected focusEntityType = EditableEntity.DATA_SOURCE;

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: DataSourceTableLoaderService,
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

    protected override createTableActionLoading(): Observable<TableAction<DataSourceBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('dataSource.deleteDataSource').typePermissions),
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentadmin.updateContent').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canDelete, canUpdatePackages]) => {
                const actions: TableAction<DataSourceBO>[] = [];

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
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        enabled: canDelete,
                        type: 'alert',
                        single: true,
                        multiple: true,
                    });
                }

                return actions;
            }),
        );
    }

    protected override createAdditionalLoadOptions(): DataSourceTableLoaderOptions {
        return {
            packageName: this.packageName,
        };
    }
}
