import { MBO_AVILABLE_PERMISSIONS, MBO_PERMISSION_PATH, MBO_ROLE_PERMISSIONS, MeshBusinessObject } from '@admin-ui/mesh/common';
import { MeshRolePermissionHandlerService } from '@admin-ui/mesh/providers/mesh-role-permission-handler/mesh-role-permission-handler.service';
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { Permission, PermissionInfo, RoleReference } from '@gentics/mesh-models';
import { BaseModal, FormProperties, setControlsEnabled } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-role-permissions-edit-modal',
    templateUrl: './mesh-role-permissions-edit-modal.component.html',
    styleUrls: ['./mesh-role-permissions-edit-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MeshRolePermissionsEditModal extends BaseModal<boolean> implements OnInit {

    @Input()
    public element: MeshBusinessObject;

    @Input()
    public role: RoleReference;

    public form: FormGroup<FormProperties<PermissionInfo>>;

    constructor(
        protected permissions: MeshRolePermissionHandlerService,
    ) {
        super();
    }

    ngOnInit(): void {
        this.form = new FormGroup<FormProperties<PermissionInfo>>(Object.values(Permission).reduce((acc: FormProperties<PermissionInfo>, val) => {
            acc[val] = new FormControl({
                value: this.element[MBO_ROLE_PERMISSIONS].includes(val),
                disabled: !this.element[MBO_AVILABLE_PERMISSIONS].includes(val),
            });
            return acc;
        }, {} as FormProperties<PermissionInfo>));
    }

    protected configureForm(): void {
        Object.values(Permission).forEach(perm => {
            setControlsEnabled(this.form, [perm], this.element[MBO_AVILABLE_PERMISSIONS].includes(perm));
        });
    }

    public async applyPermissions(recursive: boolean): Promise<void> {
        const perms = this.form.value;
        this.form.disable();

        try {
            await this.permissions.set(this.role, this.element[MBO_PERMISSION_PATH], {
                permissions: perms,
                recursive,
            });
            this.closeFn(true);
        } catch (err) {
            // Unlock the form again
            this.form.enable();
            this.configureForm();
        }
    }
}
