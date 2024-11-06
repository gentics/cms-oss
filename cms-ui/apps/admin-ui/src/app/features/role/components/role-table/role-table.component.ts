import { RoleBO } from '@admin-ui/common';
import { I18nService, PermissionsService } from '@admin-ui/core';
import { BaseEntityTableComponent, DELETE_ACTION } from '@admin-ui/shared';
import { AppStateService } from '@admin-ui/state';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { AnyModelType, NormalizableEntityTypesMap, Raw, Role } from '@gentics/cms-models';
import { ModalService, TableAction, TableColumn } from '@gentics/ui-core';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';
import { RoleTableLoaderService } from '../../providers';

@Component({
    selector: 'gtx-role-table',
    templateUrl: './role-table.component.html',
    styleUrls: ['./role-table.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoleTableComponent extends BaseEntityTableComponent<Role<Raw>, RoleBO> {

    protected rawColumns: TableColumn<RoleBO>[] = [
        {
            id: 'name',
            label: 'role.name',
            fieldPath: 'name',
            sortable: true,
        },
        {
            id: 'description',
            label: 'role.description',
            fieldPath: 'description',
            sortable: false,
        },
    ];
    protected entityIdentifier: keyof NormalizableEntityTypesMap<AnyModelType> = 'role';

    constructor(
        changeDetector: ChangeDetectorRef,
        appState: AppStateService,
        i18n: I18nService,
        loader: RoleTableLoaderService,
        modalService: ModalService,
        protected permissions: PermissionsService,
    ) {
        super(
            changeDetector,
            appState,
            i18n,
            loader,
            modalService,
        );
    }

    protected override createTableActionLoading(): Observable<TableAction<RoleBO>[]> {
        return combineLatest([
            this.actionRebuildTrigger$,
            this.permissions.checkPermissions(this.permissions.getUserActionPermsForId('role.deleteRole').typePermissions),
        ]).pipe(
            map(([_, ...perms]) => perms),
            map(([canDelete]) => {
                const actions: TableAction<RoleBO>[] = [
                    {
                        id: DELETE_ACTION,
                        icon: 'delete',
                        label: this.i18n.instant('shared.delete'),
                        enabled: canDelete,
                        type: 'alert',
                        multiple: true,
                        single: true,
                    },
                ];

                return actions;
            }),
        );
    }
}
