import { BO_DISPLAY_NAME, BO_ID } from '@admin-ui/common';
import { I18nService } from '@admin-ui/core';
import { MBO_AVILABLE_PERMISSIONS, MBO_PERMISSION_PATH, MBO_ROLE_PERMISSIONS, MeshBusinessObject } from '@admin-ui/mesh/common';
import { MeshRolePermissionHandlerService, MeshRolePermissionsTrableLoaderOptions, MeshRolePermissionsTrableLoaderService } from '@admin-ui/mesh/providers';
import { toPermissionInfo } from '@admin-ui/mesh/utils';
import { BaseEntityTrableComponent } from '@admin-ui/shared';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input } from '@angular/core';
import { Permission, RoleReference } from '@gentics/mesh-models';
import { ModalService, TableAction, TableActionClickEvent, TableColumn } from '@gentics/ui-core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MeshRolePermissionsEditModal } from '../mesh-role-permissions-edit-modal/mesh-role-permissions-edit-modal.component';

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

    public readonly BO_ID = BO_ID;
    public readonly MBO_AVILABLE_PERMISSIONS = MBO_AVILABLE_PERMISSIONS;
    public readonly MBO_ROLE_PERMISSIONS = MBO_ROLE_PERMISSIONS;
    public readonly Permission = Permission;

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
            label: 'common.name',
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
        protected modals: ModalService,
        protected permissions: MeshRolePermissionHandlerService,
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
                this.openEditModal(action.item);
                return;

            case APPLY_RECURSIVELY_ACTION:
                this.applyPermissionsRecursive(action.item);
                return;
        }

        super.handleActionClick(action);
    }

    protected async openEditModal(entity: MeshBusinessObject): Promise<void> {
        const dialog = await this.modals.fromComponent(MeshRolePermissionsEditModal, {
            closeOnEscape: false,
            closeOnOverlayClick: false,
        }, {
            element: entity,
            role: this.role,
        });
        const didChange = await dialog.open();
        if (didChange) {
            this.reloadRow(this.loader.flatStore[entity[BO_ID]]);
        }
    }

    protected async applyPermissionsRecursive(entity: MeshBusinessObject): Promise<void> {
        const dialog = await this.modals.dialog({
            title: this.i18n.instant('mesh.apply_permissions_recursive'),
            body: this.i18n.instant('mesh.apply_permissions_recursive_body'),
            buttons: [
                {
                    id: 'confirm',
                    label: this.i18n.instant('shared.confirm_button'),
                    type: 'default',
                    returnValue: true,
                },
                {
                    id: 'cancel',
                    label: this.i18n.instant('common.cancel_button'),
                    type: 'secondary',
                    returnValue: false,
                },
            ],
        });
        const shouldApply = await dialog.open();
        if (shouldApply) {
            await this.permissions.set(this.role, entity[MBO_PERMISSION_PATH], {
                permissions: toPermissionInfo(entity[MBO_ROLE_PERMISSIONS]),
                recursive: true,
            });
            const row = this.loader.flatStore[entity[BO_ID]];

            // Reset the row children data, as we have to fetch all of them again, but we do that lazyly.
            // Otherwise we'd need to do a lot of work here which we don't really need
            row.expanded = false;
            row.children = [];
            row.loaded = false;
            this.reloadRow(row);
        }
    }
}
