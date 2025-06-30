import { BO_PERMISSIONS, TemplateBO } from '@admin-ui/common';
import {
    DevToolPackageTableLoaderService,
    I18nService,
    PermissionsService,
    TemplateTableLoaderOptions,
    TemplateTableLoaderService,
} from '@admin-ui/core';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges } from '@angular/core';
import { AnyModelType, GcmsPermission, NormalizableEntityTypesMap, Template } from '@gentics/cms-models';
import { ChangesOf, ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { ContextMenuService } from '../../providers/context-menu/context-menu.service';
import { DELETE_ACTION } from '../base-entity-table/base-entity-table.component';
import { BasePackageEntityTableComponent, UNASSIGN_FROM_PACKAGE_ACTION } from '../base-package-entity-table/base-package-entity-table.component';

@Component({
    selector: 'gtx-template-table',
    templateUrl: './template-table.component.html',
    styleUrls: ['./template-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TemplateTableComponent
    extends BasePackageEntityTableComponent<Template, TemplateBO, TemplateTableLoaderOptions>
    implements OnChanges {

    @Input()
    public nodeId: number | number[];

    protected rawColumns: TableColumn<TemplateBO>[] = [
        {
            id: 'name',
            label: 'template.name',
            fieldPath: 'name',
            sortable: true,
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'template';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: TemplateTableLoaderService,
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
        }
    }

    protected override createTableActionLoading(): Observable<TableAction<TemplateBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('contentadmin.updateContent').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canManagePackage]) => {
                const actions: TableAction<TemplateBO>[] = [];

                if (!this.packageName) {
                    actions.push({
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        type: 'alert',
                        enabled: (template) => template == null || template[BO_PERMISSIONS].includes(GcmsPermission.DELETE),
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

    protected override createAdditionalLoadOptions(): TemplateTableLoaderOptions {
        return {
            nodeId: this.nodeId,
            packageName: this.packageName,
        };
    }
}
