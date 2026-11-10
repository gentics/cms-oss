import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { I18nService } from '@gentics/cms-components';
import { AnyModelType, NormalizableEntityTypesMap, PortalImplementation } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { PortalImplementationBO } from '../../../common';
import {
    DevToolPackageTableLoaderService,
    PermissionsService,
    PortalImplementationTableLoaderOptions,
    PortalImplementationTableLoaderService,
} from '../../../core';
import { AppStateService } from '../../../state';
import { ContextMenuService } from '../../providers/context-menu/context-menu.service';
import { BasePackageEntityTableComponent, UNASSIGN_FROM_PACKAGE_ACTION } from '../base-package-entity-table/base-package-entity-table.component';

@Component({
    selector: 'gtx-portal-implementation-table',
    templateUrl: './portal-implementation-table.component.html',
    styleUrls: ['./portal-implementation-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false,
})
export class PortalImplementationTableComponent
    extends BasePackageEntityTableComponent<PortalImplementation, PortalImplementationBO, PortalImplementationTableLoaderOptions> {

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

    protected override createTableActionLoading(): Observable<TableAction<PortalImplementationBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentadmin.updateContent').typePermissions),
        ]).pipe(
            map(([_, canManagePackage]) => {
                this.canManagePackage = canManagePackage;

                const actions: TableAction<PortalImplementationBO>[] = [];

                if (this.packageName) {
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

    protected override createAdditionalLoadOptions(): PortalImplementationTableLoaderOptions {
        return {
            packageName: this.packageName,
        };
    }
}
