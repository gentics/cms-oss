import { BO_DISPLAY_NAME } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { MBO_AVILABLE_PERMISSIONS, MBO_ROLE_PERMISSIONS, MeshBusinessObject } from '@admin-ui/mesh/common';
import { MeshRolePermissionsTrableLoaderOptions, MeshRolePermissionsTrableLoaderService } from '@admin-ui/mesh/providers';
import { BaseEntityTrableComponent } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { RoleReference } from '@gentics/mesh-models';
import { TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

const EDIT_ACTION = 'edit';
const APPLY_RECURSIVELY_ACTION = 'applyRecursive';

@Component({
    selector: 'gtx-mesh-role-permissions-trable',
    templateUrl: './mesh-role-permissions-trable.component.html',
    styleUrls: ['./mesh-role-permissions-trable.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MeshRolePermissionsTrableComponent
    extends BaseEntityTrableComponent<MeshBusinessObject, MeshBusinessObject, MeshRolePermissionsTrableLoaderOptions> {

    public readonly MBO_AVILABLE_PERMISSIONS = MBO_AVILABLE_PERMISSIONS;
    public readonly MBO_ROLE_PERMISSIONS = MBO_ROLE_PERMISSIONS;

    @Input()
    public role: RoleReference;

    @Input()
    public inlineExpansion = true;

    @Input()
    public inlineSelection = true;

    protected rawColumns: TableColumn<MeshBusinessObject>[] = [
        {
            id: 'name',
            fieldPath: BO_DISPLAY_NAME,
            label: 'shared.name',
        },
        {
            id: 'permissions',
            label: 'mesh.permissions',
        },
    ];

    constructor(
        changeDetector: ChangeDetectorRef,
        i18n: I18nService,
        loader: MeshRolePermissionsTrableLoaderService,
    ) {
        super(
            changeDetector,
            i18n,
            loader,
        );
    }

    protected override createAdditionalLoadOptions(): MeshRolePermissionsTrableLoaderOptions {
        return {
            role: this.role.uuid,
        };
    }

    protected override createTableActionLoading(): Observable<TableAction<MeshBusinessObject>[]> {
        // Override me when needed
        return this.actionRebuildTrigger$.pipe(
            map(() => {
                const actions: TableAction<MeshBusinessObject>[] = [
                    {
                        id: EDIT_ACTION,
                        label: this.i18n.instant('common.edit'),
                        icon: 'edit',
                        type: 'primary',
                        enabled: true,
                        single: true,
                    },
                    {
                        id: APPLY_RECURSIVELY_ACTION,
                        label: this.i18n.instant('mesh.apply_permissions_recursive'),
                        icon: 'arrow_downward',
                        type: 'warning',
                        enabled: true,
                        single: true,
                    },
                ];
                return actions;
            }),
        );
    }

    public override handleActionClick(action: TableActionClickEvent<MeshBusinessObject>): void {
        switch (action.actionId) {
            case EDIT_ACTION:
                return;
            case APPLY_RECURSIVELY_ACTION:
                return;
        }

        super.handleActionClick(action);
    }
}
