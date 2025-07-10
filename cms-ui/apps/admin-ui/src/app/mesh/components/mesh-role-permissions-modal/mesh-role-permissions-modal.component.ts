import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { RoleReference } from '@gentics/mesh-models';
import { BaseModal } from '@gentics/ui-core';

@Component({
    selector: 'gtx-mesh-role-permissions-modal',
    templateUrl: './mesh-role-permissions-modal.component.html',
    styleUrls: ['./mesh-role-permissions-modal.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MeshRolePermissionsModal extends BaseModal<void> {

    @Input()
    public role: RoleReference;

}
